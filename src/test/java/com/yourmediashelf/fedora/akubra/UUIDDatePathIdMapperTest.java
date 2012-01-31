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

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

import org.akubraproject.map.IdMapper;
import org.junit.Test;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.MutableUUIDTimer;
import com.fasterxml.uuid.impl.UUID1Generator;

/**
 * @author Edwin Shin
 * 
 */
public class UUIDDatePathIdMapperTest {
	
	/*
	 * PID containing version 1 UUID (25f814ce-f5ac-11e0-b139-2837370107a5) 
	 * with timestamp value 2011-10-13T15:00:54Z.
	 */
	private String upid1 = "info:fedora/cellar:25f814ce-f5ac-11e0-b139-2837370107a5";
	private String upid2 = "info:fedora/cellar:25f814ce-f5ac-11e0-b139-2837370107a5.0001";
	private String upid3 = "info:fedora/cellar:25f814ce-f5ac-11e0-b139-2837370107a5.0002.01";
	
	String[] upids = {upid1, upid2, upid3};
	
	private String pid1 = "info:fedora/demo:1";
	
	@Test
	public void testType1UUIDs() throws Exception {
		IdMapper m = new UUIDDatePathIdMapper();
		URI internalId, externalId;

		for (String pid : upids) {
			internalId = m.getInternalId(new URI(pid));
			assertEquals("file:2011/10/13/" + IdMapperUtil.encode(pid), internalId.toString());
			externalId = m.getExternalId(internalId);
			assertEquals(pid, externalId.toString());
		}
	}
	
	/**
	 * Test that non-type 1 UUIDs use the default fallback IdMapper
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNonType1UUIDs() throws Exception {
		IdMapper m = new UUIDDatePathIdMapper();
		URI internalId = m.getInternalId(new URI(pid1));
		assertEquals("file:" + IdMapperUtil.encode(pid1), internalId.toString());
		
		URI externalId = m.getExternalId(internalId);
		assertEquals(pid1, externalId.toString());
	}
	
	@Test
	public void testDateFormats() throws Exception {
		IdMapper m = new UUIDDatePathIdMapper("yy/HH");
		URI internalId, externalId;

		for (String pid : upids) {
			internalId = m.getInternalId(new URI(pid));
			assertEquals("file:11/15/" + IdMapperUtil.encode(pid), internalId.toString());
			externalId = m.getExternalId(internalId);
			assertEquals(pid, externalId.toString());
		}
	}

	/**
	 * Test use of IdMapperPrefixer
	 *
	 * @throws Exception
	 */
	@Test
	public void testFedoraNamespacePrefixer() throws Exception {
	    IdMapperPrefixer prefixer = new FedoraNamespacePrefixer();
	    IdMapper fallback = new PrefixingHashPathIdMapper("##", prefixer);
	    IdMapper m = new UUIDDatePathIdMapper(null, fallback, prefixer);
        URI internalId, externalId;

        for (String pid : upids) {
            internalId = m.getInternalId(new URI(pid));
            assertEquals("file:cellar/2011/10/13/" + IdMapperUtil.encode(pid), internalId.toString());
            externalId = m.getExternalId(internalId);
            assertEquals(pid, externalId.toString());
        }

        // Using a non type 1 UUID, so should use PrefixingHashPathIdMapper
        // The MD5 of fallbackId is "f5b3783ee48db3b87a008eace01b0060"
        String fallbackId = "info:fedora/test:123";
        internalId = m.getInternalId(new URI(fallbackId));
        assertEquals("file:test/f5/" + IdMapperUtil.encode(fallbackId), internalId.toString());
        externalId = m.getExternalId(internalId);
        assertEquals(fallbackId, externalId.toString());
	}

	/**
	 * Convert a Java Date into a ISO-8601 UTC date string
	 * 
	 * @param date
	 * @return
	 */
	protected static String convertDateToString(Date date) {
        if (date == null) {
            return null;
        } else {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));

            return df.format(date);
        }
    }
	
	protected static Date parseDate(String dateString) throws ParseException {
        if (dateString == null) {
            throw new ParseException("Argument cannot be null.", 0);
        } else if (dateString.isEmpty()) {
            throw new ParseException("Argument cannot be empty.", 0);
        } else if (dateString.endsWith(".")) {
            throw new ParseException("dateString ends with invalid character.", dateString.length() - 1);
        }
        SimpleDateFormat formatter = new SimpleDateFormat();
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        int length = dateString.length();
        if (dateString.startsWith("-")) {
            length--;
        }
        if (dateString.endsWith("Z")) {
            if (length == 11) {
                formatter.applyPattern("yyyy-MM-dd'Z'");
            } else if (length == 20) {
                formatter.applyPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            } else if (length > 21 && length < 24) {
                // right-pad the milliseconds with 0s up to three places
                StringBuilder sb = new StringBuilder(dateString.substring(0, dateString.length() - 1));
                int dotIndex = sb.lastIndexOf(".");
                int endIndex = sb.length() - 1;
                int padding = 3 - (endIndex - dotIndex);
                for (int i = 0; i < padding; i++) {
                    sb.append("0");
                }
                sb.append("Z");
                dateString = sb.toString();
                formatter.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            } else if (length == 24) {
                formatter.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            }
        } else {
            if (length == 10) {
                formatter.applyPattern("yyyy-MM-dd");
            } else if (length == 19) {
                formatter.applyPattern("yyyy-MM-dd'T'HH:mm:ss");
            } else if (length > 20 && length < 23) {
                // right-pad millis with 0s
                StringBuilder sb = new StringBuilder(dateString);
                int dotIndex = sb.lastIndexOf(".");
                int endIndex = sb.length() - 1;
                int padding = 3 - (endIndex - dotIndex);
                for (int i = 0; i < padding; i++) {
                    sb.append("0");
                }
                dateString = sb.toString();
                formatter.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
            } else if (length == 23) {
                formatter.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
            } else if (dateString.endsWith("GMT") || dateString.endsWith("UTC")) {
                formatter.applyPattern("EEE, dd MMMM yyyyy HH:mm:ss z");
            }
        }
        return formatter.parse(dateString);
    }
	
	@Test
	public void testUUIDGeneration() throws Exception {
		EthernetAddress addr = new EthernetAddress("01:aa:75:ed:71:a1");
		MutableUUIDTimer timer = new MutableUUIDTimer(new Random(System.currentTimeMillis()), null);
		UUID1Generator gen = new UUID1Generator(addr, timer);
		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date d = df.parse("2011-12-31");
		UUID uuid = gen.generate(d.getTime());
		
		assertEquals(d.getTime(), getDate(uuid).getTime());
		assertEquals(addr.toString(), getNode(uuid));
	}
	
	private Date getDate(UUID uuid) {
    	if (uuid.version() != 1) {
    		throw new IllegalArgumentException("Wrong UUID version: " + uuid.version());
    	}
    	
    	long NUM_100NS_INTERVALS_BETWEEN_UUID_AND_UNIX_EPOCHS = 0x01b21dd213814000L;
		long t1 = uuid.timestamp() - NUM_100NS_INTERVALS_BETWEEN_UUID_AND_UNIX_EPOCHS;
		return new Date(t1/10000);
    }
	
	private String getNode(UUID uuid) {
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.putLong(uuid.getLeastSignificantBits());
		// node is the last 6 bytes
		byte[] node = Arrays.copyOfRange(buffer.array(), 2, 8);
		EthernetAddress addr = new EthernetAddress(node);
		return addr.toString();
	}
}
