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

import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.camel.Body;
import org.apache.camel.CamelContext;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultMessage;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.jena.JenaGraph;
import org.apache.commons.rdf.jena.JenaRDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trellisldp.api.IOService;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.vocabulary.RDF;

public class ResourceMessageList {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceMessageList.class);

    private static final JenaRDF rdf = new JenaRDF();

    private static IOService service = new JenaIOService(null);

    public List<Message> getResources(@Body InputStream body, CamelContext camelContext) {

        List<Message> resList = new ArrayList<>();
        final JenaGraph graph = rdf.createGraph();
        service.read(body, null, NTRIPLES).forEachOrdered(graph::add);
        graph.stream().forEach(t -> {
            DefaultMessage message = new DefaultMessage(camelContext);
            String uri = ((IRI) t.getSubject()).getIRIString();
            message.setHeader("CamelHttpUri", uri);
            resList.add(message);
        });
        return resList;
    }
}
