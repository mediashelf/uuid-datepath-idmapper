/**
 * 
 */
package com.yourmediashelf.fedora.akubra;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.akubraproject.map.IdMapper;
import org.junit.Test;

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
	 * Convert a Java Date into a ISO-8601 UTC date string
	 * 
	 * @param date
	 * @return
	 */
	public static String convertDateToString(Date date) {
        if (date == null) {
            return null;
        } else {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));

            return df.format(date);
        }
    }
	
	public static Date parseDate(String dateString) throws ParseException {
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
}