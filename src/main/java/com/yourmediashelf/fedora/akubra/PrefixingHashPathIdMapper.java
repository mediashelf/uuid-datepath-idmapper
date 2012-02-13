/**
 * Copyright (C) 2012 MediaShelf <http://www.yourmediashelf.com/>
 *
 * This file is part of uuid-datepath-idmapper.
 *
 * uuid-datepath-idmapper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * uuid-datepath-idmapper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with uuid-datepath-idmapper.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.yourmediashelf.fedora.akubra;

import java.net.URI;

import org.akubraproject.map.IdMapper;

import com.twmacinta.util.MD5;


/**
 * Implementation of org.fcrepo.server.storage.lowlevel.akubra.HashPathIdMapper 
 * that takes an {@link IdMapperPrefixer} as a constructor argument.
 * 
 * Provides a hash-based <code>file:</code> mapping for any URI.
 * <p>
 * The path component of each internal URI is derived from an MD5 hash of
 * the external URI. The filename component is a reversible encoding of the
 * external URI that is safe to use as a filename on modern filesystems.
 * <p>
 * <h2>Hash Path Patterns</h2>
 * The pattern given at construction time determines how the path component
 * of each internal URI will be composed. Within the pattern, the # character
 * is a stand-in for a hexadecimal [0-f] digit from the MD5 hash of the
 * external id.
 * <p>
 * Patterns:
 * <ul>
 *   <li> must consist only of # and / characters.</li>
 *   <li> must contain between 1 and 32 # characters.</li>
 *   <li> must not begin or end with the / character.</li>
 *   <li> must not contain consecutive / characters.</li>
 * </ul>
 * <p>
 * Example patterns:
 * <ul>
 *   <li> Good: #</li>
 *   <li> Good: ##/#</li>
 *   <li> Good: ##/##/##</li>
 *   <li> Bad: a</li>
 *   <li> Bad: ##/</li>
 *   <li> Bad: ##//##</li>
 * </ul>
 * <p>
 * <h2>Filesystem-Safe Encoding</h2>
 * The last part of the internal URI is a "filesystem-safe" encoding of the
 * external URI. All characters will be UTF-8 percent-encoded ("URI escaped")
 * except for the following: <code>a-z A-Z 0-9 = ( ) [ ] -</code>
 * In addition, <code>.</code> (period) will be escaped as <code>%2E</code> when
 * it occurs as the last character of the URI.
 * <p>
 * <h2>Example Mappings</h2>
 * With pattern <em>#/#</em>:
 * <ul>
 *   <li> <code>urn:example1</code> becomes <code>file:0/8/urn%3Aexample1</code></li>
 *   <li> <code>http://tinyurl.com/cxzzf</code> becomes <code>file:6/2/http%3A%2F%2Ftinyurl.com%2Fcxzzf</code></li>
 * </ul>
 * With pattern <em>##/##</em>:
 * <ul>
 *   <li> <code>urn:example1</code> becomes <code>file:08/86/urn%3Aexample1</code></li>
 *   <li> <code>http://tinyurl.com/cxzzf</code> becomes <code>file:62/ca/http%3A%2F%2Ftinyurl.com%2Fcxzzf</code></li>
 * </ul>
 *
 * @author Chris Wilper
 * @author Edwin Shin
 */
public class PrefixingHashPathIdMapper
        implements IdMapper {

    private static final String internalScheme = "file";

    private final String pattern;
    
    private final IdMapperPrefixer prefixer;
    
    static {
        MD5.initNativeLibrary(true); // don't attempt to use the native libs, ever.
    }

    /**
     * Creates an instance that will use the given pattern.
     *
     * @param pattern the path pattern to use, possibly <code>null</code> or "".
     * @param prefixer The {@link IdMapperPrefixer} to use, or <code>null</code>.
     * 
     * @throws IllegalArgumentException if the pattern is invalid.
     */
    public PrefixingHashPathIdMapper(String pattern, IdMapperPrefixer prefixer) {
        this.pattern = validatePattern(pattern);
        this.prefixer = prefixer;
    }
    
    /**
     * Convenience constructor that uses a <code>null</null> {@link IdMapperPrefixer} 
     * and therefore behaves identically to 
     * <code>org.fcrepo.server.storage.lowlevel.akubra.HashPathIdMapper</code>.
     *
     * @param pattern the path pattern to use, possibly <code>null</code> or "".
     */
    public PrefixingHashPathIdMapper(String pattern) {
        this(pattern, null);
    }

    public URI getExternalId(URI internalId) throws NullPointerException {
        String fullPath = internalId.toString().substring(
                internalScheme.length() + 1);
        int i = fullPath.lastIndexOf('/');
        String encodedURI;
        if (i == -1)
            encodedURI = fullPath;
        else
            encodedURI = fullPath.substring(i + 1);
        return URI.create(IdMapperUtil.decode(encodedURI));
    }

    public URI getInternalId(URI externalId) throws NullPointerException {
        if (externalId == null) {
            throw new NullPointerException();
        }
        String uri = externalId.toString();
        return URI.create(internalScheme + ":" + getPath(uri) + IdMapperUtil.encode(uri));
    }

    public String getInternalPrefix(String externalPrefix)
            throws NullPointerException {
        if (externalPrefix == null) {
            throw new NullPointerException();
        }
        // we can only do this if pattern is ""
        if (pattern.length() == 0) {
            return internalScheme + ":" + IdMapperUtil.encode(externalPrefix);
        } else {
            return null;
        }
    }

    // gets the path based on the hash of the uri, or "" if the pattern is empty
    private String getPath(String uri) {
        String nsPrefix = "";
        if (prefixer != null) {
            nsPrefix = prefixer.getPrefix(uri);
            if (!nsPrefix.isEmpty()) {
                nsPrefix = nsPrefix + '/';
            }
        }
        
        if (pattern.length() == 0) {
            return nsPrefix;
        }
        
        StringBuilder builder = new StringBuilder(nsPrefix);
        String hash = getHash(uri);
        int hashPos = 0;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '#') {
                builder.append(hash.charAt(hashPos++));
            } else {
                builder.append(c);
            }
        }
        builder.append('/');
        return builder.toString();
    }

    // computes the md5 and returns a 32-char lowercase hex string
    private static String getHash(String uri) {
        return MD5.asHex(new MD5(uri).Final());
    }

    private static String validatePattern(String pattern) {
        if (pattern == null) {
            return "";
        }
        int count = 0;
        boolean prevWasSlash = false;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '#') {
                count++;
                prevWasSlash = false;
            } else if (c == '/') {
                if (i == 0 || i == pattern.length() - 1) {
                    throw new IllegalArgumentException("Pattern must not begin"
                            + " or end with '/'");
                } else if (prevWasSlash) {
                    throw new IllegalArgumentException("Pattern must not"
                            + " contain consecutive '/' characters");
                } else {
                    prevWasSlash = true;
                }
            } else {
                throw new IllegalArgumentException("Illegal character in"
                        + " pattern: " + c);
            }
        }
        if (count > 32) {
            throw new IllegalArgumentException("Pattern must not contain more"
                    + " than 32 '#' characters");
        }
        return pattern;
    }
}
