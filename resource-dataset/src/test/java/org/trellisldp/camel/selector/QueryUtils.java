package org.trellisldp.camel.selector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryUtils {
    private static String replaceNode(String query, String v1, String v2) {

        Pattern p = Pattern.compile("(\"\\?v1)\" \"(\\?v2)\"");
        Matcher m = p.matcher(query);
        StringBuffer sb = new StringBuffer(query.length());
        while (m.find()) {
            m.appendReplacement(sb, "\"" + replace(m.group(1), v1) + "\" \"" + replace(m.group(2), v2) + "\"");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String replace(String group, String value) {
        return Matcher.quoteReplacement(value);
    }

    static String getQuery(final String qname, final String V1, String V2) throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream(qname);
        String out = readFile(is);
        return replaceNode(out, V1, V2);
    }

    private static String readFile(InputStream in) throws IOException {
        StringBuilder inobj = new StringBuilder();
        try (BufferedReader buf = new BufferedReader(
                new InputStreamReader(in, "UTF-8"))) {
            String line;
            while ((line = buf.readLine()) != null) {
                inobj.append(line).append("\n");
            }
        }
        return inobj.toString();
    }
}
