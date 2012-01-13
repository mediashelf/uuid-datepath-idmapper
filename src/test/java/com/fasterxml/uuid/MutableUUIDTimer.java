package com.fasterxml.uuid;

import java.io.IOException;
import java.util.Random;

import com.fasterxml.uuid.Logger;
import com.fasterxml.uuid.TimestampSynchronizer;
import com.fasterxml.uuid.impl.UUIDUtil;

public class MutableUUIDTimer {

	/**
	 * Since System.longTimeMillis() returns time from 1 January 1970, and
	 * UUIDs need time from the beginning of Gregorian calendar (15 October 1582),
	 * need to apply the offset:
	 */
	private final static long kClockOffset = 0x01b21dd213814000L;
	
	/**
	 * Also, instead of getting time in units of 100nsecs, we get something with
	 * max resolution of 1 msec... and need the multiplier as well
	 */
	private final static int kClockMultiplier = 10000;

	private final static long kClockMultiplierL = 10000L;

	/**
	 * Let's allow "virtual" system time to advance at most 100 milliseconds
	 * beyond actual physical system time, before adding delays.
	 */
	private final static long kMaxClockAdvance = 100L;

	// Configuration

	/**
	 * Object used to reliably ensure that no multiple JVMs generate UUIDs, and
	 * also that the time stamp value used for generating time-based UUIDs is
	 * monotonically increasing even if system clock moves backwards over a
	 * reboot (usually due to some system level problem).
	 * <p>
	 * See {@link TimestampSynchronizer} for details.
	 */
	protected final TimestampSynchronizer _syncer;

	/**
	 * Random number generator used to generate additional information to
	 * further reduce probability of collisions.
	 */
	protected final Random _random;

	// Clock state:

	/**
	 * Additional state information used to protect against anomalous cases
	 * (clock time going backwards, node id getting mixed up). Third byte is
	 * actually used for seeding counter on counter overflow. Note that only
	 * lowermost 16 bits are actually used as sequence
	 */
	private int _clockSequence;

	/**
	 * Last physical timestamp value <code>System.currentTimeMillis()</code>
	 * returned: used to catch (and report) cases where system clock goes
	 * backwards. Is also used to limit "drifting", that is, amount timestamps
	 * used can differ from the system time value. This value is not guaranteed
	 * to be monotonically increasing.
	 */
	private long _lastSystemTimestamp = 0L;

	/**
	 * Timestamp value last used for generating a UUID (along with
	 * {@link #_clockCounter}. Usually the same as {@link #_lastSystemTimestamp}
	 * , but not always (system clock moved backwards). Note that this value is
	 * guaranteed to be monotonically increasing; that is, at given absolute
	 * time points t1 and t2 (where t2 is after t1), t1 <= t2 will always hold
	 * true.
	 */
	private long _lastUsedTimestamp = 0L;

	/**
	 * First timestamp that can NOT be used without synchronizing using
	 * synchronization object ({@link #_syncer}). Only used when external
	 * timestamp synchronization (and persistence) is used, ie. when
	 * {@link #_syncer} is not null.
	 */
	private long _firstUnsafeTimestamp = Long.MAX_VALUE;

	/**
	 * Counter used to compensate inadequate resolution of JDK system timer.
	 */
	private int _clockCounter = 0;

	public MutableUUIDTimer(Random rnd, TimestampSynchronizer sync) throws IOException {
		_random = rnd;
		_syncer = sync;
		initCounters(rnd);
		_lastSystemTimestamp = 0L;
		// This may get overwritten by the synchronizer
		_lastUsedTimestamp = 0L;

		/*
		 * Ok, now; synchronizer can tell us what is the first timestamp value
		 * that definitely was NOT used by the previous incarnation. This can
		 * serve as the last used time stamp, assuming it is not less than value
		 * we are using now.
		 */
		if (sync != null) {
			long lastSaved = sync.initialize();
			if (lastSaved > _lastUsedTimestamp) {
				_lastUsedTimestamp = lastSaved;
			}
		}

		/*
		 * Also, we need to make sure there are now no safe values (since
		 * synchronizer is not yet requested to allocate any):
		 */
		_firstUnsafeTimestamp = 0L; // ie. will always trigger sync.update()
	}

	private void initCounters(Random rnd) {
		/*
		 * Let's generate the clock sequence field now; as with counter, this
		 * reduces likelihood of collisions (as explained in UUID specs)
		 */
		_clockSequence = rnd.nextInt();
		/*
		 * Ok, let's also initialize the counter... Counter is used to make it
		 * slightly less likely that two instances of UUIDGenerator (from
		 * separate JVMs as no more than one can be created in one JVM) would
		 * produce colliding time-based UUIDs. The practice of using multiple
		 * generators, is strongly discouraged, of course, but just in case...
		 */
		_clockCounter = (_clockSequence >> 16) & 0xFF;
	}

	public int getClockSequence() {
		return (_clockSequence & 0xFFFF);
	}

	public synchronized long getTimestamp(long systime) {
		/*
		 * Let's first verify that the system time is not going backwards;
		 * independent of whether we can use it:
		 */
		if (systime < _lastSystemTimestamp) {
			Logger.logWarning("System time going backwards! (got value "
					+ systime + ", last " + _lastSystemTimestamp);
			// Let's write it down, still
			_lastSystemTimestamp = systime;
		}

		/*
		 * But even without it going backwards, it may be less than the last one
		 * used (when generating UUIDs fast with coarse clock resolution; or if
		 * clock has gone backwards over reboot etc).
		 */
		if (systime <= _lastUsedTimestamp) {
			/*
			 * Can we just use the last time stamp (ok if the counter hasn't hit
			 * max yet)
			 */
			if (_clockCounter < kClockMultiplier) { // yup, still have room
				systime = _lastUsedTimestamp;
			} else { // nope, have to roll over to next value and maybe wait
				long actDiff = _lastUsedTimestamp - systime;
				long origTime = systime;
				systime = _lastUsedTimestamp + 1L;

				Logger.logWarning("Timestamp over-run: need to reinitialize random sequence");

				/*
				 * Clock counter is now at exactly the multiplier; no use just
				 * anding its value. So, we better get some random numbers
				 * instead...
				 */
				initCounters(_random);

				/*
				 * But do we also need to slow down? (to try to keep virtual
				 * time close to physical time; i.e. either catch up when system
				 * clock has been moved backwards, or when coarse clock
				 * resolution has forced us to advance virtual timer too far)
				 */
				if (actDiff >= kMaxClockAdvance) {
					slowDown(origTime, actDiff);
				}
			}
		} else {
			/*
			 * Clock has advanced normally; just need to make sure counter is
			 * reset to a low value (need not be 0; good to leave a small
			 * residual to further decrease collisions)
			 */
			_clockCounter &= 0xFF;
		}

		_lastUsedTimestamp = systime;

		/*
		 * Ok, we have consistent clock (virtual or physical) value that we can
		 * and should use. But do we need to check external syncing now?
		 */
		if (_syncer != null && systime >= _firstUnsafeTimestamp) {
			try {
				_firstUnsafeTimestamp = _syncer.update(systime);
			} catch (IOException ioe) {
				throw new RuntimeException("Failed to synchronize timestamp: "
						+ ioe);
			}
		}

		/*
		 * Now, let's translate the timestamp to one UUID needs, 100ns unit
		 * offset from the beginning of Gregorian calendar...
		 */
		systime *= kClockMultiplierL;
		systime += kClockOffset;

		// Plus add the clock counter:
		systime += _clockCounter;
		// and then increase
		++_clockCounter;
		return systime;
	}
	
	/**
	 * Method that constructs timestamp unique and suitable to use for
	 * constructing UUIDs. Default implementation just calls
	 * {@link #getTimestampSynchronized}, which is fully synchronized;
	 * sub-classes may choose to implemented alternate strategies
	 * 
	 * @return 64-bit timestamp to use for constructing UUID
	 */
	public final synchronized long getTimestamp() {
		return getTimestamp(System.currentTimeMillis());	
	}

	/*
	 * /**********************************************************************
	 * /* Test-support methods
	 * /**********************************************************************
	 */

	/*
	 * Method for accessing timestamp to use for creating UUIDs. Used ONLY by
	 * unit tests, hence protected.
	 */
	protected final void getAndSetTimestamp(byte[] uuidBytes) {
		long timestamp = getTimestamp();

		uuidBytes[UUIDUtil.BYTE_OFFSET_CLOCK_SEQUENCE] = (byte) _clockSequence;
		uuidBytes[UUIDUtil.BYTE_OFFSET_CLOCK_SEQUENCE + 1] = (byte) (_clockSequence >> 8);

		// Time fields aren't nicely split across the UUID, so can't just
		// linearly dump the stamp:
		int clockHi = (int) (timestamp >>> 32);
		int clockLo = (int) timestamp;

		uuidBytes[UUIDUtil.BYTE_OFFSET_CLOCK_HI] = (byte) (clockHi >>> 24);
		uuidBytes[UUIDUtil.BYTE_OFFSET_CLOCK_HI + 1] = (byte) (clockHi >>> 16);
		uuidBytes[UUIDUtil.BYTE_OFFSET_CLOCK_MID] = (byte) (clockHi >>> 8);
		uuidBytes[UUIDUtil.BYTE_OFFSET_CLOCK_MID + 1] = (byte) clockHi;

		uuidBytes[UUIDUtil.BYTE_OFFSET_CLOCK_LO] = (byte) (clockLo >>> 24);
		uuidBytes[UUIDUtil.BYTE_OFFSET_CLOCK_LO + 1] = (byte) (clockLo >>> 16);
		uuidBytes[UUIDUtil.BYTE_OFFSET_CLOCK_LO + 2] = (byte) (clockLo >>> 8);
		uuidBytes[UUIDUtil.BYTE_OFFSET_CLOCK_LO + 3] = (byte) clockLo;
	}

	/*
	 * /**********************************************************************
	 * /* Private methods
	 * /**********************************************************************
	 */

	private final static int MAX_WAIT_COUNT = 50;

	/**
	 * Simple utility method to use to wait for couple of milliseconds, to let
	 * system clock hopefully advance closer to the virtual timestamps used.
	 * Delay is kept to just a millisecond or two, to prevent excessive
	 * blocking; but that should be enough to eventually synchronize physical
	 * clock with virtual clock values used for UUIDs.
	 * 
	 * @param msecs
	 *            Number of milliseconds to wait for from current time point
	 */
	private final static void slowDown(long startTime, long actDiff) {
		/*
		 * First, let's determine how long we'd like to wait. This is based on
		 * how far ahead are we as of now.
		 */
		long ratio = actDiff / kMaxClockAdvance;
		long delay;

		if (ratio < 2L) { // 200 msecs or less
			delay = 1L;
		} else if (ratio < 10L) { // 1 second or less
			delay = 2L;
		} else if (ratio < 600L) { // 1 minute or less
			delay = 3L;
		} else {
			delay = 5L;
		}
		Logger.logWarning("Need to wait for " + delay
				+ " milliseconds; virtual clock advanced too far in the future");
		long waitUntil = startTime + delay;
		int counter = 0;
		do {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException ie) {
			}
			delay = 1L;
			/*
			 * This is just a sanity check: don't want an "infinite" loop if
			 * clock happened to be moved backwards by, say, an hour...
			 */
			if (++counter > MAX_WAIT_COUNT) {
				break;
			}
		} while (System.currentTimeMillis() < waitUntil);
	}

}
