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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deskolemize.
 *
 * @author christopher-johnson
 */
public class Deskolemize {

    public boolean isWellKnownUri(String v) {
        Pattern p = Pattern.compile("^.+?trellis:bnode.+?$");
        Matcher m = p.matcher(v);
        return m.find();
    }

    /**
     * Converts a SkolemIRI to a BNode.
     *
     * @param input the SkolemIRI to convert.
     * @return a BNode.
     */
    static String convertSkolem(String input) {
        Pattern p = Pattern.compile("trellis:bnode/([0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{44})");
        Matcher m = p.matcher(input);
        StringBuffer sb = new StringBuffer(input.length());
        while (m.find()) {
            String id = m.group(1);
            String bnode = "_:b" + id;
            m.appendReplacement(sb, Matcher.quoteReplacement(bnode));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Converts a trellis:data node to a concrete hostname.
     *
     * @param input the dataset to convert.
     * @param hostname the hostname.
     * @return a BNode.
     */
    static String convertHostname(String input, String hostname) {
        Pattern p = Pattern.compile("(trellis:data/)(.*)");
        Matcher m = p.matcher(input);
        StringBuffer sb = new StringBuffer(input.length());
        while (m.find()) {
            String path = m.group(2);
            String node = hostname + "/" + path;
            m.appendReplacement(sb, Matcher.quoteReplacement(node));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    static boolean isNotEmpty(String input) {
        Pattern p = Pattern.compile("^<");
        Matcher m = p.matcher(input);
        return m.find();
    }
}
