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
/**
 * 
 */
package com.yourmediashelf.fedora.akubra;

import java.net.URI;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.akubraproject.map.IdMapper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.uuid.UUIDType;
import com.fasterxml.uuid.impl.UUIDUtil;

/**
 * <p>An Akubra IdMapper that maps version 1 UUIDs (i.e., time-based UUIDs) to 
 * datetime-based paths.
 * 
 * <p>This IdMapper should be configured to use a fallbackMapper, to handle ids 
 * which are not version 1 UUIDs.
 * 
 * @author Edwin Shin
 *
 */
public class UUIDDatePathIdMapper implements IdMapper {

	private static final String internalScheme = "file";
	private IdMapper fallbackMapper;
	private Pattern pattern;
	private DateTimeFormatter fmt;
	
	/**
	 * The number of 100-ns intervals between the UUID epoch 1582-10-15 00:00:00 
	 * and the Unix epoch 1970-01-01 00:00:00.
	 */
	private final static long NUM_100NS_INTERVALS_SINCE_UUID_EPOCH = 0x01b21dd213814000L;
	
	/**
	 * <p>Convenience constructor that uses the default dateFormat and fallbackMapper.
	 * 
	 * <p>Note: the default fallbackMapper (TrivialIdMapper) is probably not 
	 * what you should be using.
	 * 
	 */
	public UUIDDatePathIdMapper() {
		this(null, null);
	}
	
	/**
	 * <p>Convenience constructor that uses the default fallbackMapper.
	 * 
	 * <p>Note: the default fallbackMapper (TrivialIdMapper) is probably not 
	 * what you should be using.
	 * 
	 * @param dateFormat
	 * 
	 * @see http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html#forPattern%28java.lang.String%29
	 */
	public UUIDDatePathIdMapper(String dateFormat) {
		this(dateFormat, null);
	}
	
	/**
	 * Convenience constructor that uses the default dateFormat.
	 * 
	 * @param fallbackMapper
	 */
	public UUIDDatePathIdMapper(IdMapper fallbackMapper) {
		this(null, fallbackMapper);
	} 
	
	/**
	 * 
	 * @param dateFormat The format pattern used to generate the path. If null or empty, 
	 * defaults to "yyyy/MM/dd".
	 * @param fallbackMapper The IdMapper to use if/when we encounter an id that 
	 * is not a version 1 UUID. If null, defaults to TrivialIdMapper.
	 * @see http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html#forPattern%28java.lang.String%29
	 */
	public UUIDDatePathIdMapper(String dateFormat, IdMapper fallbackMapper) {
		if (fallbackMapper == null) {
			this.fallbackMapper = new TrivialIdMapper();
		} else {
			this.fallbackMapper = fallbackMapper;
		}
		
		// minor performance optimization since we only support UTC
		System.setProperty("org.joda.time.DateTimeZone.Provider", "org.joda.time.tz.UTCProvider");
		
		if (dateFormat == null || dateFormat.isEmpty()) {
			dateFormat = "yyyy/MM/dd";
		}
		fmt = DateTimeFormat.forPattern(dateFormat);
		
		// regex Pattern for canonical UUIDs (32 hexadecimal digits, separated in 5 groups by hyphens)
    	pattern = Pattern.compile(".*([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}).*");	
	}
	
	/* (non-Javadoc)
	 * @see org.akubraproject.map.IdMapper#getExternalId(java.net.URI)
	 */
	public URI getExternalId(URI internalId) throws NullPointerException {
		String uri = internalId.toString();

		try {
			extractUUID(uri);
		} catch (IllegalArgumentException e) {
			return fallbackMapper.getExternalId(internalId);
		}
		
		
		String fullPath = internalId.toString().substring(
                internalScheme.length() + 1);
        int i = fullPath.lastIndexOf('/'); // e.g. everything after "info:fedora/"
        String encodedURI;
        if (i == -1)
            encodedURI = fullPath;
        else
            encodedURI = fullPath.substring(i + 1);
        return URI.create(IdMapperUtil.decode(encodedURI));
	}

	/*
	 * (non-Javadoc)
	 * @see org.akubraproject.map.IdMapper#getInternalId(java.net.URI)
	 */
	public URI getInternalId(URI externalId) throws NullPointerException {
		if (externalId == null) {
            throw new NullPointerException();
        }
        String uri = externalId.toString();
        
        // if not version 1 UUID, use fallback id mapper
        try {
        	return URI.create(internalScheme + ":" + getPath(uri) + IdMapperUtil.encode(uri));
        } catch(IllegalArgumentException e) {
        	return fallbackMapper.getInternalId(externalId);
        }
	}

	/* (non-Javadoc)
	 * @see org.akubraproject.map.IdMapper#getInternalPrefix(java.lang.String)
	 */
	public String getInternalPrefix(String externalPrefix) throws NullPointerException {
		if (externalPrefix == null) {
            throw new NullPointerException();
        }
		//TODO
        return null;
	}
	
	private String getPath(String uri) {
		UUID uuid = extractUUID(uri);
		UUIDType uuidType = UUIDUtil.typeOf(uuid);
		if (uuidType.equals(UUIDType.TIME_BASED)) {
			return getDateTime(uuid).toString(fmt) + '/';
	        
		} else {
			throw new IllegalArgumentException("Wrong type of UUID. " + uuid.toString() + " is " + uuidType);
		}
	}
    
    /**
     * Searches for and returns a UUID embedded in the supplied string.
     * 
     * @param s Any string that contains a UUID in canonical form.
     * @return UUID
     * @throws NullPointerException
     */
	protected UUID extractUUID(String s) throws NullPointerException {
    	if (s == null) {
    		throw new NullPointerException();
    	}
    	
    	if (s.length() < 36) {
    		throw new IllegalArgumentException("String \"" + s + "\" is too short to be a UUID");
    	}
    	
    	Matcher m = pattern.matcher(s);
    	if (m.matches()) {
    		return UUIDUtil.uuid(m.group(1));
    	} else {
    		throw new IllegalArgumentException("\"" + s + "\" is not a UUID");
    	}
    }
    
    /**
     * Get the DateTime from a version 1 UUID.
     * 
     * @param uuid a version 1 UUID
     * @return
     */
    protected DateTime getDateTime(UUID uuid) {
    	if (uuid.version() != 1) {
    		throw new IllegalArgumentException("Wrong UUID version: " + uuid.version());
    	}
    	
		long t1 = uuid.timestamp() - NUM_100NS_INTERVALS_SINCE_UUID_EPOCH;
    		
		return new DateTime(t1/10000, DateTimeZone.UTC);
    }
}
