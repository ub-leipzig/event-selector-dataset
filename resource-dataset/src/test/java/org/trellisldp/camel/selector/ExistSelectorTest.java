/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.trellisldp.camel.selector;

import static java.net.URLEncoder.encode;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_RESPONSE_CODE;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.jena.riot.WebContent.contentTypeNTriples;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Hashtable;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.util.IOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExistSelectorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistSelectorTest.class);

    private static final String SPARQL_QUERY = "type";

    private static final String HTTP_ACCEPT = "Accept";

    public static void main(final String[] args) throws Exception {
        LOGGER.info("About to run Camel integration...");

        JndiRegistry registry = new JndiRegistry(createInitialContext());
        registry.bind("resourceMessageList", new ResourceMessageList());
        final CamelContext camelContext = new DefaultCamelContext(registry);

        camelContext.addRoutes(new RouteBuilder() {
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
                    e.getIn().setHeader("named.graph", e.getIn().getHeader("node").toString());
                }).to("http4:{{activitystream.base}}?useSystemProperties=true&bridgeEndpoint=true")
                        .filter(header(HTTP_RESPONSE_CODE).isEqualTo(200)).setHeader(CONTENT_TYPE)
                        .constant(contentTypeNTriples)
                        .log(INFO, LOGGER, "Getting query results as n-triples")
                        .to("direct:getEvents");

                from("direct:getEvents").routeId("EventProcessor")
                        .log(INFO, LOGGER, "Getting Subjects From Events").split()
                        .method("resourceMessageList", "getResources").to("direct:getBody");

                from("direct:getBody").routeId("DatasetUpdater").setHeader(HTTP_METHOD)
                        .constant("GET")
                        .to("http4:trellis:8080")
                        .log(INFO, LOGGER, "Indexing Subject " + "${headers[CamelHttpUri]}")
                        .filter(header(HTTP_RESPONSE_CODE).isEqualTo(200))
                        .to("direct:putBody");
                from("direct:putBody")
                        .convertBodyTo(InputStream.class)
                        .setHeader(
                                Exchange.HTTP_METHOD,
                                constant(org.apache.camel.component.http4.HttpMethods.PUT))
                        .process(e -> {
                            String messageURI = e.getIn().getHeader(Exchange.HTTP_URI).toString();
                            String path = "/exist/rest/db/apps/metsmods/data/test/"
                                    + messageURI.substring(messageURI.lastIndexOf('/') + 1);
                            e.getIn().setHeader(Exchange.HTTP_PATH, path);
                        })
                        .removeHeader(Exchange.HTTP_URI)
                        .to("http4://localhost:8900?authUsername=admin&authPassword=khatru"
                                + "&authenticationPreemptive=true");
            }
        });
        camelContext.start();

        Thread.sleep(5 * 60 * 1000);

        camelContext.stop();
    }

    private static String sparqlConstruct(final String command) {
        try {
            return "query=" + encode(command, "UTF-8");
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static Context createInitialContext() throws Exception {
        InputStream in = ExistSelectorTest.class.getClassLoader().getResourceAsStream(
                "jndi.properties");
        try {
            Properties properties = new Properties();
            properties.load(in);
            return new InitialContext(new Hashtable<>(properties));
        } finally {
            IOHelper.close(in);
        }
    }
}

