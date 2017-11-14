package org.trellisldp.camel.selector;

import static java.net.URLEncoder.encode;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_RESPONSE_CODE;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;
import static org.apache.jena.riot.WebContent.contentTypeNTriples;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.main.Main;
import org.apache.camel.main.MainListenerSupport;
import org.apache.camel.main.MainSupport;
import org.apache.commons.rdf.jena.JenaGraph;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.system.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trellisldp.api.IOService;
import org.trellisldp.io.JenaIOService;

public class Selector {
    public static void main(String[] args) throws Exception {
        Selector selector = new Selector();
        selector.init();
    }

    private void init() throws Exception {
        Main main = new Main();
        main.bind("resourceMessageList", new ResourceMessageList());
        main.addRouteBuilder(new SelectorRoute());
        main.addMainListener(new Events());
        main.run();
    }

    public static class Events extends MainListenerSupport {

        @Override
        public void afterStart(MainSupport main) {
            System.out.println("Selector is now started!");
        }

        @Override
        public void beforeStop(MainSupport main) {
            System.out.println("Selector is now being stopped!");
        }
    }

    public static class SelectorRoute extends RouteBuilder {
        private static final Logger LOGGER = LoggerFactory.getLogger(SelectorRoute.class);

        private static final String SPARQL_QUERY = "type";

        private static final String HTTP_ACCEPT = "Accept";

        private static final JenaRDF rdf = new JenaRDF();

        private static IOService service = new JenaIOService(null);

        public void configure() {

            final PropertiesComponent pc =
                    getContext().getComponent("properties", PropertiesComponent.class);
            pc.setLocation("classpath:application.properties");

            from("jetty:http://{{rest.host}}:{{rest.port}}{{rest.prefix}}?"
                    + "optionsEnabled=true&matchOnUriPrefix=true&sendServerVersion=false"
                    + "&httpMethodRestrict=GET,OPTIONS").routeId("Selector").choice()
                    .when(header(SPARQL_QUERY).isEqualTo("rdf:type")).setHeader(HTTP_METHOD)
                    .constant("POST").setHeader(CONTENT_TYPE)
                    .constant("application/x-www-form-urlencoded; " + "charset=utf-8")
                    .setHeader(HTTP_ACCEPT).constant(contentTypeNTriples).process(e -> {
                e.getIn().setBody(sparqlConstruct(
                        Query.getEventQuery("events-type.rq", Query.getSet(e))));
                LOGGER.info("Node type {}", e.getIn().getHeader("node").toString());
            }).to("http4:{{fuseki.base}}?useSystemProperties=true&bridgeEndpoint=true")
                    .filter(header(HTTP_RESPONSE_CODE).isEqualTo(200)).setHeader(CONTENT_TYPE)
                    .constant(contentTypeNTriples).convertBodyTo(String.class)
                    .log(INFO, LOGGER, "Getting query results as n-triples")
                    .to("direct:getEvents");

            from("direct:getEvents").routeId("EventProcessor")
                    .log(INFO, LOGGER, "Getting Subjects From Events").split()
                    .method("resourceMessageList", "getResources").to("direct:update.dataset");

            from("direct:update.dataset").routeId("DatasetUpdater").setHeader(HTTP_METHOD)
                    .constant("GET").setHeader(HTTP_ACCEPT).constant(contentTypeNTriples)
                    .setHeader("fuseki.base", constant("http://{{fuseki.base}}"))
                    .to("http4:trellis:8080")
                    .log(INFO, LOGGER, "Indexing Subject " + "${headers[CamelHttpUri]}")
                    .process(exchange -> {
                        final JenaGraph graph = rdf.createGraph();
                        service.read(exchange.getIn().getBody(InputStream.class), null,
                                NTRIPLES).forEachOrdered(graph::add);
                        try (RDFConnection conn = RDFConnectionFactory.connect(
                                exchange.getIn().getHeader("fuseki.base").toString())) {
                            Txn.executeWrite(conn, () -> {
                                conn.load(
                                        exchange.getIn().getHeader("named.graph").toString(),
                                        graph.asJenaModel());
                            });
                            conn.commit();
                        }
                        LOGGER.info("Committing Resource Body to Jena Dataset");
                    });
        }
    }

    private static String sparqlConstruct(final String command) {
        try {
            return "query=" + encode(command, "UTF-8");
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
