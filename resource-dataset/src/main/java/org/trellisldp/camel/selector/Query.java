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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.camel.Exchange;

public class Query {

    private static final String NODE_SET = "node";

    private static String replaceNode(String query, String node) {
        Pattern p = Pattern.compile("\\?node");
        Matcher m = p.matcher(query);
        StringBuffer sb = new StringBuffer(query.length());
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(node));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    static String getEventQuery(final String qname, final String setName) throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream(qname);
        String out = readFile(is);
        return replaceNode(out, setName);
    }

    private static String readFile(InputStream in) throws IOException {
        StringBuilder inobj = new StringBuilder();
        try (BufferedReader buf = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            String line;
            while ((line = buf.readLine()) != null) {
                inobj.append(line).append("\n");
            }
        }
        return inobj.toString();
    }

    @SuppressWarnings("unchecked")
    public static String getSet(final Exchange e) {
        final Object optHdr = e.getIn().getHeader(NODE_SET);
        return (String) optHdr;
    }
}
