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

import static org.slf4j.LoggerFactory.getLogger;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;

class FromRdf {
    private static final Logger log = getLogger(FromRdf.class);

    static String toJsonLd(String ntriples, String contextUri, String frameUri) throws IOException, JsonLdError {
        Object ctxobj;
        Object frame;
        Object outobj;
        Object compactobj;
        Object frameobj;
        final JsonLdOptions opts = new JsonLdOptions("");
        opts.setUseNativeTypes(true);
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream(contextUri);
        ctxobj = JsonUtils.fromInputStream(is);
        if (Deskolemize.isNotEmpty(ntriples)) {
            final String graph = Deskolemize.convertSkolem(ntriples);
            outobj = JsonLdProcessor.fromRDF(graph, opts);
            compactobj = JsonLdProcessor.compact(outobj, ctxobj, opts);
            System.out.println(JsonUtils.toPrettyString(compactobj));
            InputStream fs = classloader.getResourceAsStream(frameUri);
            frame = JsonUtils.fromInputStream(fs);
            frameobj = JsonLdProcessor.frame(compactobj, frame, opts);
            //System.out.println(JsonUtils.toPrettyString(frameobj));
            //Files.write(Paths.get("output.json"), JsonUtils.toPrettyString(compactobj).getBytes
            // ());
            return JsonUtils.toPrettyString(frameobj);
        } else {
            String empty = "empty SPARQL result set";
            log.error(empty);
            throw new IOException();
        }
    }
}
