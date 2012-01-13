package com.fasterxml.uuid.impl;

import java.util.UUID;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.MutableUUIDTimer;
import com.fasterxml.uuid.NoArgGenerator;
import com.fasterxml.uuid.UUIDType;
import com.fasterxml.uuid.impl.UUIDUtil;

public class UUID1Generator extends NoArgGenerator {

	protected final EthernetAddress _ethernetAddress;

	/**
	 * Object used for synchronizing access to timestamps, to guarantee that
	 * timestamps produced by this generator are unique and monotonically
	 * increasings. Some implementations offer even stronger guarantees, for
	 * example that same guarantee holds between instances running on different
	 * JVMs (or with native code).
	 */
	protected final MutableUUIDTimer _timer;

	/**
	 * Base values for the second long (last 8 bytes) of UUID to construct
	 */
	protected final long _uuidL2;

	public UUID1Generator(EthernetAddress ethAddr, MutableUUIDTimer timer) {
		/*
		 * UUID version 1 is a 128-bit (16 byte) value, split as: <br>
		 * 	32 bits time_low <br>
		 *  16 bits time_mid (offset 4 bytes) <br>
		 *  16 bits time_hi_and_version (offset 6 bytes) <br>
		 *  16 bits clock sequence (offset 8 bytes) <br>
		 *  48 bits node (offset 10 bytes)
		 */
		byte[] uuidBytes = new byte[16];
		if (ethAddr == null) {
			ethAddr = EthernetAddress.constructMulticastAddress();
		}

		_ethernetAddress = ethAddr;
		// The node field is a 48-bit (6 byte) value
		_ethernetAddress.toByteArray(uuidBytes, 10);

		// Add clock sequence
		int clockSeq = timer.getClockSequence();
		uuidBytes[UUIDUtil.BYTE_OFFSET_CLOCK_SEQUENCE] = (byte) (clockSeq >> 8);
		uuidBytes[UUIDUtil.BYTE_OFFSET_CLOCK_SEQUENCE + 1] = (byte) clockSeq;
		long l2 = UUIDUtil.gatherLong(uuidBytes, 8);
		_uuidL2 = UUIDUtil.initUUIDSecondLong(l2);
		_timer = timer;
	}
	
	public UUID generate(long timeInMillis) {
		return generateByTimestamp(_timer.getTimestamp(timeInMillis));
	}
	
	@Override
	public UUID generate() {
		return generateByTimestamp(_timer.getTimestamp());
        
	}

	@Override
	public UUIDType getType() {
		return UUIDType.TIME_BASED;
	}
	
	private UUID generateByTimestamp(long rawTimestamp) {
		// Time field components are kind of shuffled, need to slice:
        int clockHi = (int) (rawTimestamp >>> 32);
        int clockLo = (int) rawTimestamp;
        // and dice
        int midhi = (clockHi << 16) | (clockHi >>> 16);
        // need to squeeze in type (4 MSBs in byte 6, clock hi)
        midhi &= ~0xF000; // remove high nibble of 6th byte
        midhi |= 0x1000; // type 1
        long midhiL = (long) midhi;
        midhiL = ((midhiL << 32) >>> 32); // to get rid of sign extension
        // and reconstruct
        long l1 = (((long) clockLo) << 32) | midhiL;
        // last detail: must force 2 MSB to be '10'
        return new UUID(l1, _uuidL2);
	}
}
