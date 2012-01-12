package com.yourmediashelf.fedora.akubra;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Utility methods for IdMapper implementations.
 * 
 * @author Edwin Shin
 *
 */
public class IdMapperUtil {
	
	/**
	 * 
	 * @param uri
	 * @return the encoded URI, as a String
	 */
	public static String encode(String uri) {
        // encode char-by-char because we only want to borrow
        // URLEncoder.encode's behavior for some characters
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < uri.length(); i++) {
            char c = uri.charAt(i);
            if (c >= 'a' && c <= 'z') {
                out.append(c);
            } else if (c >= '0' && c <= '9') {
                out.append(c);
            } else if (c >= 'A' && c <= 'Z') {
                out.append(c);
            } else if (c == '-' || c == '=' || c == '(' || c == ')'
                    || c == '[' || c == ']' || c == ';') {
                out.append(c);
            } else if (c == ':') {
                out.append("%3A");
            } else if (c == ' ') {
                out.append("%20");
            } else if (c == '+') {
                out.append("%2B");
            } else if (c == '_') {
                out.append("%5F");
            } else if (c == '*') {
                out.append("%2A");
            } else if (c == '.') {
                if (i == uri.length() - 1) {
                    out.append("%2E");
                } else {
                    out.append(".");
                }
            } else {
                try {
                    out.append(URLEncoder.encode("" + c, "UTF-8"));
                } catch (UnsupportedEncodingException wontHappen) {
                    throw new RuntimeException(wontHappen);
                }
            }
        }
        return out.toString();
    }

	/**
	 * 
	 * @param encodedURI
	 * @return the decoded URI, as a String
	 */
    public static String decode(String encodedURI) {
        if (encodedURI.endsWith("%2E")) {
            encodedURI = encodedURI.substring(0, encodedURI.length() - 3) + ".";
        }
        try {
            return URLDecoder.decode(encodedURI, "UTF-8");
        } catch (UnsupportedEncodingException wontHappen) {
            throw new RuntimeException(wontHappen);
        }
    }
}
