/**********************************************************************
Copyright (c) 2004 Ralf Ullrich and others. All rights reserved. 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 

Contributors:
...
**********************************************************************/
package org.datanucleus.store.valuegenerator;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;

import org.datanucleus.util.NucleusLogger;

/**
 * An almost unique ID.
 * 
 * This class represents a best possible derivate of a DCE UUID as is possible
 * with the standard Java API, which provides no way to determine the IEEE 802
 * address of a system and only provides a time with a granularity of
 * milliseconds instead of the required 100nanoseconds.
 * 
 * It therefore uses random node values and also does not use non-volatile
 * storage for internal state fields.
 * 
 * The uuid is internally stored as two 64 bit long values. This allows a fast
 * implementation of the <code>equals</code> method, but provides possibly
 * poor performance for the <code>compare</code> method due to the required
 * unpacking of the data.
 * 
 * To create "real" UUIDs the default implementations of <code>loadState</code>
 * and <code>saveState</code> have to be overridden to provide hardware
 * derived node values and non-volatile generator state storage. The first is
 * not possible with Java's standard API and there is no standard way to do the
 * latter, therefore this class was called AUID = "almost unique id", as it
 * tries to circumvent these short comings by random initializations.
 * 
 * Note: Due to the construction of the time stamp included in an AUID as a
 * 60-bit value of 100-nanosecond intervals since 00:00:00.00, 15 October 1582
 * (the date of Gregorian reform to the Christian Calendar), this implementation
 * will break due to field overflow on 23:21:00 CEST of March 31st in the year
 * 5236, that is when we are long gone and became ashes.
 */
class AUID implements Comparable
{
    /**
     * This class represents the current state of the AUID generator.
     */
    protected static class State
    {
        /** The last time stamp used to create a AUID. */
        private long lastTime;
        /**
         * The time adjustment to be added to the last time stamp to create the
         * next AUID.
         */
        private long adjustTime;
        /** The current clock sequence. */
        private int clockSequence;
        /** The node value. */
        private long node;
        /** The version to use when constructing new AUIDs. */
        private int version;
        /** The variant to use when constructing new AUIDs. */
        private int variant;
        /** A random generator to be use for random initialization of fields. */
        private Random random;
        /**
         * A flag indicating if security attributes should be included in time
         * low field.
         */
        private boolean includeSecurityAttributes;

        /**
         * Sets the last time stamp used to create an AUID.
         * 
         * @param lastTime
         *            the last time stamp used to create an AUID.
         */
        public void setLastTime(long lastTime)
        {
            this.lastTime = lastTime;
        }

        /**
         * Returns the last time stamp used to create an AUID.
         * 
         * @return the last time stamp used to create an AUID.
         */
        public long getLastTime()
        {
            return lastTime;
        }

        /**
         * Sets the time adjustment to be added to the last time stamp to create
         * the next AUID.
         * 
         * @param adjustTime
         *            The time adjustment to be added to the last time stamp to
         *            create the next AUID.
         */
        public void setAdjustTime(long adjustTime)
        {
            this.adjustTime = adjustTime;
        }

        /**
         * Returns the time adjustment to be added to the last time stamp to
         * create the next AUID.
         * 
         * @return The time adjustment to be added to the last time stamp to
         *         create the next AUID.
         */
        public long getAdjustTime()
        {
            return adjustTime;
        }

        /**
         * Returns the time adjustment to be added to the last time stamp to
         * create the next AUID and increments it.
         * 
         * @return The time adjustment to be added to the last time stamp to
         *         create the next AUID before incrementation.
         */
        public long incrementAdjustTime()
        {
            return adjustTime++;
        }

        /**
         * Sets the current clock sequence.
         * 
         * @param clockSequence
         *            the current clock sequence.
         */
        public void setClockSequence(int clockSequence)
        {
            this.clockSequence = clockSequence;
        }

        /**
         * Returns the current clock sequence.
         * 
         * @return the current clock sequence.
         */
        public int getClockSequence()
        {
            return clockSequence;
        }

        /**
         * Set the node value.
         * 
         * @param node
         *            the node value.
         */
        public void setNode(long node)
        {
            this.node = node;
        }

        /**
         * Returns the node value.
         * 
         * @return the node value.
         */
        public long getNode()
        {
            return node;
        }

        /**
         * Sets the version to use when constructing new AUIDs.
         * 
         * @param version
         *            the version to use when constructing new AUIDs.
         */
        public void setVersion(int version)
        {
            this.version = version;
        }

        /**
         * Returns the version to use when constructing new AUIDs.
         * 
         * @return the version to use when constructing new AUIDs.
         */
        public int getVersion()
        {
            return version;
        }

        /**
         * Sets the variant to use when constructing new AUIDs.
         * 
         * @param variant
         *            the variant to use when constructing new AUIDs.
         */
        public void setVariant(int variant)
        {
            this.variant = variant;
        }

        /**
         * Returns the variant to use when constructing new AUIDs.
         * 
         * @return the variant to use when constructing new AUIDs.
         */
        public int getVariant()
        {
            return variant;
        }

        /**
         * Sets the random generator used for initialization of fields.
         * 
         * @param random
         *            the random generator to use for initialization of fields.
         */
        public void setRandom(Random random)
        {
            this.random = random;
        }

        /**
         * Returns the random generator used for initialization of fields.
         * 
         * @return the random generator used for initialization of fields.
         */
        public Random getRandom()
        {
            return random;
        }

        /**
         * Sets if security attributes have to be included in time low field.
         * 
         * @param includeSecurityAttributes
         *            if <code>true</code> security attributes will included.
         */
        public void setIncludeSecurityAttributes(
                boolean includeSecurityAttributes)
        {
            this.includeSecurityAttributes = includeSecurityAttributes;
        }

        /**
         * Returns wether security attribute have to be included.
         * 
         * @return <code>true</code> if security attributes have to be
         *         included.
         */
        public boolean getIncludeSecurityAttributes()
        {
            return includeSecurityAttributes;
        }
    }

    // These two are commented out since not enabled in the code
//  private static final int VERSION_DCE = 1;
//  private static final int VERSION_DCE_SECURE = 2;

    private static final int VERSION_RANDOM_NODE = 3;
    private static final int VARIANT_NCS = 0x0000;
    private static final int VARIANT_DCE = 0x8000;
    private static final int VARIANT_MICROSOFT = 0xc000;
    private static final int VARIANT_RESERVED = 0xe000;
    private static final int CS_MASK_NCS = 0x7fff;
    private static final int CS_MASK_DCE = 0x3fff;
    private static final int CS_MASK_MICROSOFT = 0x1fff;
    private static final int CS_MASK_RESERVED = 0x1fff;
    private static final long MAXIMUM_ENTROPIC_TIME_MS = 5000;

    /**
     * Scale from System.currentTimeMillis() granularity to that of AUID.
     */
    private static final long TIME_SCALE = 10000;

    /**
     * The offset between UTC and the AUID timestamps in 100-nanosecond
     * intervals since 00:00:00.00, 15 October 1582 (the date of Gregorian
     * reform to the Christian Calendar).
     */
    private static final long UTC_OFFSET = new GregorianCalendar()
            .getGregorianChange().getTime()
            * TIME_SCALE;
    /**
     * An array of chars for Hex conversion.
     */
    private static final char[] HEX_CHARS = {'0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * The state of the AUID generator.
     */
    private static State auidState = null;

    /**
     * The first 64 bits of the uuid value.
     */
    private long firstHalf;

    /**
     * The last 64 bits of the uuid value.
     */
    private long secondHalf;

    /**
     * Constructs an AUID with new values.
     */
    public AUID()
    {
        makeUnique(0, false);
    }

    /**
     * Constructs an AUID with new values and the specified security attributes.
     * The subclass which calls this constructor MUST set the state accordingly.
     * 
     * @param securityAttributes
     *            the security attributes to include in the new AUID.
     */
    protected AUID(int securityAttributes)
    {
        makeUnique(securityAttributes, true);
    }

    /**
     * Constructs an AUID from the specified values. This constructor is for
     * subclasses only and should not be made available to API users.
     * 
     * @param time
     *            the time field of the new AUID
     * @param version
     *            the version of the new AUID
     * @param clockSeq
     *            the clock sequence of the new AUID
     * @param variant
     *            the variant of the new AUID
     * @param node
     *            the node of the new AUID
     */
    protected AUID(long time, int version, int clockSeq, int variant, long node)
    {
        packFirstHalf(time, version);
        packSecondHalf(clockSeq, variant, node);
    }

    /**
     * Constructs an AUID from the specified original DCE field values. This
     * constructor is for subclasses only and should not be made available to
     * API users.
     * 
     * @param timeLow
     *            the time low field of the new AUID
     * @param timeMid
     *            the time mid field of the new AUID
     * @param timeHiAndVersion
     *            the packed time high and version field of the new AUID
     * @param clockSeqHiAndVariant
     *            the packed clock sequence high and variant field of the new
     *            AUID
     * @param clockSeqLow
     *            the clock sequence low field of the new AUID
     * @param node
     *            the node field of the new AUID
     */
    protected AUID(long timeLow, long timeMid, long timeHiAndVersion,
            int clockSeqHiAndVariant, int clockSeqLow, long node)
    {
        packDCEFieldsFirstHalf(timeLow, timeMid, timeHiAndVersion);
        packDCEFieldsSecondHalf(clockSeqHiAndVariant, clockSeqLow, node);
    }

    /**
     * Constructs an AUID with the same values as the specified AUID.
     * 
     * @param auid
     *            the AUID to clone
     */
    protected AUID(AUID auid)
    {
        firstHalf = auid.firstHalf;
        secondHalf = auid.secondHalf;
    }

    /**
     * Constructs an AUID from the specified string representation.
     * 
     * @param auid string in the format "LLLLLLLL-MMMM-HHHH-CCCC-NNNNNNNNNNNN"
     * @throws NumberFormatException
     *             if <code>auid</code> does not match the pattern
     *             "LLLLLLLL-MMMM-HHHH-CCCC-NNNNNNNNNNNN" where 'L', 'M', 'H',
     *             'C' and 'N' are time low, mid, high, clock sequence and node
     *             fields respectively.
     *            a string representation of an AUID
     */
    public AUID(String auid)
    {
        try
        {
            firstHalf = parseFirstHalf(auid.subSequence(0, 18));
            secondHalf = parseSecondHalf(auid.subSequence(18, 36));
        }
        catch (IndexOutOfBoundsException ioobe)
        {
            throw new NumberFormatException();
        }
        catch (NumberFormatException nfe)
        {
            throw new NumberFormatException();
        }
    }

    /**
     * Constructs an AUID from the specified string representation in
     * CharSequence.
     * 
     * @param auid
     *            a string representation of an AUID
     * @throws NumberFormatException
     *             if <code>auid</code> does not match the pattern
     *             "LLLLLLLL-MMMM-HHHH-CCCC-NNNNNNNNNNNN" where 'L', 'M', 'H',
     *             'C' and 'N' are time low, mid, high, clock sequence and node
     *             fields respectively.
     */
    public AUID(CharSequence auid)
    {
        try
        {
            firstHalf = parseFirstHalf(auid.subSequence(0, 18));
            secondHalf = parseSecondHalf(auid.subSequence(18, 36));
        }
        catch (IndexOutOfBoundsException ioobe)
        {
            throw new NumberFormatException();
        }
        catch (NumberFormatException nfe)
        {
            throw new NumberFormatException();
        }
    }

    /**
     * Constructs an AUID from the specified byte array representation, which is
     * in the same format as returned by <code>getBytes(dst, dstBegin)</code>.
     * This constructor is equal to <code>AUID(bytes, 0)</code>.
     * 
     * @param bytes
     *            a byte array representation of an AUID
     */
    public AUID(byte[] bytes)
    {
        this(bytes, 0);
    }

    /**
     * Constructs an AUID from the specified byte array representation, which is
     * in the same format as returned by <code>getBytes(dst, dstBegin)</code>.
     * 
     * @param bytes
     *            a byte array representation of an AUID
     * @param offset
     *            the offset at which the byte array representation is located
     *            in the byte array.
     */
    public AUID(byte[] bytes, int offset)
    {
        long timeLow = getOctets(4, bytes, 0 + offset, true);
        long timeMid = getOctets(2, bytes, 4 + offset, true);
        long timeHAV = getOctets(2, bytes, 6 + offset, true);
        int csHAV = (int) getOctets(1, bytes, 8 + offset, true);
        int csLow = (int) getOctets(1, bytes, 9 + offset, true);
        long node = getOctets(6, bytes, 10 + offset, true);
        packDCEFieldsFirstHalf(timeLow, timeMid, timeHAV);
        packDCEFieldsSecondHalf(csHAV, csLow, node);
    }

    /**
     * Parse a String representation of an AUID. Equal to
     * <code>new AUID(auid)</code>.
     * 
     * @param auid
     *            a string representation of an AUID
     * @return the AUID represented by <code>auid</code>.
     * @throws NumberFormatException
     *             if <code>auid</code> does not match the pattern
     *             "LLLLLLLL-MMMM-HHHH-CCCC-NNNNNNNNNNNN" where 'L', 'M', 'H',
     *             'C' and 'N' are time low, mid, high, clock sequence and node
     *             fields respectively.
     */
    public static AUID parse(String auid)
    {
        return new AUID(auid);
    }

    /**
     * Parse a String representation of an AUID. Equal to
     * <code>new AUID(auid)</code>.
     * 
     * @param auid string in the format "LLLLLLLL-MMMM-HHHH-CCCC-NNNNNNNNNNNN"
     * @return the AUID represented by <code>auid</code>.
     * @throws NumberFormatException
     *             if <code>auid</code> does not match the pattern
     *             "LLLLLLLL-MMMM-HHHH-CCCC-NNNNNNNNNNNN" where 'L', 'M', 'H',
     *             'C' and 'N' are time low, mid, high, clock sequence and node
     *             fields respectively.
     *            a string representation of an AUID
     */
    public static AUID parse(CharSequence auid)
    {
        return new AUID(auid);
    }

    /**
     * Identifies the variant from the combined clock sequence and variant
     * value, as it is returned by <code>getClockSeqAndVariant()</code>.
     * 
     * May be overridden to support new variants.
     * 
     * @param clockSeqAndVariant
     *            the combined clock sequence and variant value, as it is
     *            returned by <code>getClockSeqAndVariant()</code>.
     * @return the variant identified in the specified combined value.
     * @throws IllegalArgumentException
     *             if variant could not be identified.
     */
    protected int identifyVariant(int clockSeqAndVariant)
    {
        if ((clockSeqAndVariant & ~CS_MASK_NCS) == VARIANT_NCS)
        {
            return VARIANT_NCS;
        }
        if ((clockSeqAndVariant & ~CS_MASK_DCE) == VARIANT_DCE)
        {
            return VARIANT_DCE;
        }
        if ((clockSeqAndVariant & ~CS_MASK_MICROSOFT) == VARIANT_MICROSOFT)
        {
            return VARIANT_NCS;
        }
        if ((clockSeqAndVariant & ~CS_MASK_RESERVED) == VARIANT_RESERVED)
        {
            return VARIANT_RESERVED;
        }
        // should not occur
        throw new IllegalArgumentException();
    }

    /**
     * This returns the bit mask to be applied to a clock sequence before it is
     * ORed with the variant value.
     * 
     * May be overridden to support new variants.
     * 
     * @param variant
     *            the variant for which the mask shall be returned.
     * @return the mask to apply to a sequence when combining values.
     */
    public int getClockSeqMaskForVariant(int variant)
    {
        switch (variant)
        {
            case VARIANT_NCS :
                return CS_MASK_NCS;
            case VARIANT_DCE :
                return CS_MASK_DCE;
            case VARIANT_MICROSOFT :
                return CS_MASK_MICROSOFT;
            case VARIANT_RESERVED :
                return CS_MASK_RESERVED;
            default :
                throw new IllegalArgumentException();
        }
    }

    /**
     * Returns the current time as 100-nanosecond intervals since 00:00:00.00,
     * 15 October 1582 (the date of Gregorian reform to the Christian Calendar).
     * This may be overridden to support high resolution time devices.
     * 
     * @return a time stamp for use in an AUID
     */
    protected long getCurrentTime()
    {
        return System.currentTimeMillis() * TIME_SCALE - UTC_OFFSET;
    }

    /**
     * Loads the generator state from non-volatile storage into the provided
     * State object or creates a new one if <code>null</code>.
     * 
     * Overriding this method allows to actually add persistent storage
     * facilities for the generator state and may also support non-random node
     * that have been retrieved from the hardware. <br />
     * To override this method follow this pattern:
     * 
     * <pre>
     * protected State loadState(State state)
     * {
     *     State loadInto = state;
     *     if (loadInto == null)
     *     {
     *         if (staticStateField == null)
     *         {
     *             loadInto = staticStateField = new State();
     *         }
     *         state = staticStateField;
     *     }
     *     if (loadInto != null)
     *     {
     *         if (loadInto.random == null)
     *         {
     *             // set loadInto.random to a customized Random generator if possible
     *             // YOUR CODE TO SET RANDOM GENERATOR GOES HERE (OPTIONAL)
     *         }
     *         // call super implementation to initialize with defaults.
     *         super(loadInto);
     *         // load state into loadInto (consider to use getClass() to distinguish
     *         // between multiple stored versions).
     *         // always load lastTime and adjustTime
     *         // load clock sequence only if node value is retrieved from hardware that
     *         // cannot be moved to another system and is guaranteed to be unique between
     *         // different systems
     *         // Do not modify the above values if loading failed (for example due to 
     *         // I/O failure).
     *         // YOUR CODE TO LOAD CLOCK RELATED VALUES GOES HERE (REQUIRED)
     *         // Set node, version, variant and security attribute inclusion as required.
     *         // YOUR CODE TO SET NODE, VERSION, VARIANT VALUES GOES HERE (RECOMMENDED)
     *     }
     *     return state;
     * }
     * </pre>
     * 
     * @param state
     *            the State object into which the state has to be loaded.
     * @return an initialized State object
     */
    protected State loadState(State state)
    {
        State loadInto = state;
        if (loadInto == null)
        {
            if (auidState == null)
            {
                loadInto = auidState = new State();
            }
            state = auidState;
        }
        if (loadInto != null)
        {
            if (loadInto.getRandom() == null)
            {
                // set random generator
                //loadInto.setRandom(new Random(System.currentTimeMillis()));
                //loadInto.setRandom(new Random(EntropicSeed.calculate(32)));
                loadInto.setRandom(new Random(entropicSeed(32, System
                        .currentTimeMillis())));
            }
            // no super implementation to call
            // initialize clock related
            loadInto.setLastTime(getCurrentTime());
            loadInto.setAdjustTime(0);
            loadInto.setClockSequence(loadInto.getRandom().nextInt());
            // initialize attribute fields
            loadInto
                    .setNode(loadInto.getRandom().nextLong() & 0x0000ffffffffffffL);
            loadInto.setVersion(VERSION_RANDOM_NODE);
            loadInto.setVariant(VARIANT_DCE);
            loadInto.setIncludeSecurityAttributes(false);
        }
        return state;
    }

    /**
     * Can be overridden together with <code>loadState</code> to provide
     * persistent storage for the auid generator state. The default
     * implementation does nothing.
     * 
     * @param state
     *            the State object to persist.
     */
    protected void saveState(State state)
    {
    }

    /**
     * This is the implementation of <code>getBytes(dst, dstBegin)</code>,
     * however, the endianess can be specified through the boolean argument.
     * Subclasses can use this to provide little endian byte array
     * representations. If this is done, there should also be a constructor
     * using this byte array order. If this method is overridden to provide a
     * different field order, the new field order will also be used by the
     * <code>getBytes(dst, dstBegin)</code> method.
     * 
     * @param dst
     *            the destination byte array, if <code>null</code> a new array
     *            will be created
     * @param dstBegin
     *            the offset to use when writing into the destination byte
     *            array, ignored if dst is <code>null</code>
     * @param bigendian
     *            if <code>true</code> big endian byte order is used
     * @return a byte array
     */
    protected byte[] getBytes(byte[] dst, int dstBegin, boolean bigendian)
    {
        if (dst == null)
        {
            dst = new byte[16];
            dstBegin = 0;
        }
        putOctets(getTimeLow(), 4, dst, dstBegin, bigendian);
        putOctets(getTimeMid(), 2, dst, dstBegin + 4, bigendian);
        putOctets(getTimeHighAndVersion(), 2, dst, dstBegin + 6, bigendian);
        putOctets(getClockSeqHighAndVariant(), 1, dst, dstBegin + 8, bigendian);
        putOctets(getClockSeqLow(), 1, dst, dstBegin + 9, bigendian);
        putOctets(getNode(), 6, dst, dstBegin + 10, bigendian);
        return dst;
    }

    /**
     * Returns the AUID value as byte array. The array will be filled with the
     * original DCE fields in big endian order, that is each field starts with
     * the most significant byte:
     * 
     * <pre>
     * 
     *  
     *   +---------------------------+---------+---+---+-------+-------+
     *   ! FIELD DESCRIPTION         ! OCTETS  !    LENGTH in bits     !
     *   +---------------------------+---------+---------------+-------+
     *   ! time low                  !  0 -  3 !       32      !
     *   +---------------------------+---------+-------+-------+
     *   ! time mid                  !  4 -  5 !   16  !
     *   +---------------------------+---------+-------+
     *   ! time hi and version       !  6 -  7 !   16  !
     *   +---------------------------+---------+---+---+
     *   ! clock seq hi and reserved !    8    ! 8 !
     *   +---------------------------+---------+---+
     *   ! clock seq low             !    9    ! 8 !
     *   +---------------------------+---------+---+-------------------+
     *   ! node                      ! 10 - 15 !           48          !
     *   +---------------------------+---------+-----------------------+
     *   
     *  
     * </pre>
     * 
     * This implementation just returns
     * <code>getBytes(dst, dstBegin, true)</code>.
     * @param dst
     *            the destination byte array, if <code>null</code> a new array
     *            will be created
     * @param dstBegin
     *            the offset to use when writing into the destination byte
     *            array, ignored if dst is <code>null</code>
     * @return a byte array     
     */
    public byte[] getBytes(byte[] dst, int dstBegin)
    {
        return getBytes(dst, dstBegin, true);
    }

    /**
     * Appends the String representation of this AUID to the specified
     * StringBuffer or if null to a new created StringBuffer and returns the
     * StringBuffer. This method is called by <code>toString</code>,
     * therefore it is sufficient to override this method together with
     * providing new parse methods to create a new string representation of an
     * AUID.
     * 
     * @param sb
     *            the StringBuffer to use
     * @return a StringBuffer to which this AUID has been appended
     */
    public StringBuffer toStringBuffer(StringBuffer sb)
    {
        if (sb == null)
        {
            sb = new StringBuffer();
        }
        toHex(sb, getTimeLow(), 8);
        sb.append('-');
        toHex(sb, getTimeMid(), 4);
        sb.append('-');
        toHex(sb, getTimeHighAndVersion(), 4);
        sb.append('-');
        toHex(sb, getClockSeqAndVariant(), 4);
        sb.append('-');
        toHex(sb, getNode(), 12);
        return sb;
    }

    /**
     * Packs time and version into the first 64 bits (octets 0-7).
     */
    private void packFirstHalf(long time, int version)
    {
        firstHalf = ((long) version << 60) | (time & 0x0fffffffffffffffL);
    }

    /**
     * Packs the original DCE fields time low, mid and hiAndVersion into the
     * first 64 bits (octets 0-7).
     */
    private void packDCEFieldsFirstHalf(long timeLow, long timeMid,
            long timeHiAndVersion)
    {
        firstHalf = (timeHiAndVersion << 48) | (timeMid << 32) | timeLow;
    }

    /**
     * Packs clock sequence, variant and node into the second 64 bits (octets
     * 8-15).
     */
    private void packSecondHalf(int clockSeq, int variant, long node)
    {
        int csMasked = clockSeq & getClockSeqMaskForVariant(variant);
        int csLow = csMasked & 0xff;
        int csHigh = (variant | csMasked) >>> 8;
        secondHalf = (node << 16) | (csLow << 8) | csHigh;
    }

    /**
     * Packs the original DCE fields clockSeqHiAndVariant, clockSeqLow and node
     * into the second 64 bits (octets 8-15).
     */
    private void packDCEFieldsSecondHalf(int clockSeqHiAndVariant,
            int clockSeqLow, long node)
    {
        secondHalf = (node << 16) | (clockSeqLow << 8) | clockSeqHiAndVariant;
    }

    /**
     * Contains the algorithm to create an AUID.
     */
    private void makeUnique(int securityAttributes,
            boolean hasSecurityAttributes)
    {
        synchronized (AUID.class)
        {
            // prepare generation
            State state = loadState(null);
            // DCE algorithm to generate UUID:
            // 1. determine time stamp and clock sequence
            long now = getCurrentTime();
            if (now < state.getLastTime())
            {
                state.setClockSequence(state.getClockSequence() + 1);
                state.setAdjustTime(0);
                state.setLastTime(now);
            }
            else if (now != state.getLastTime())
            {
                if (now < (state.getLastTime() + state.getAdjustTime()))
                {
                    throw new IllegalStateException("Clock overrun occured.");
                }
                state.setAdjustTime(0);
                state.setLastTime(now);
            }
            now += state.incrementAdjustTime();
            // 2a. replace time-low with security attributes if version is
            // DCE_SECURE
            if (state.getIncludeSecurityAttributes())
            {
                if (hasSecurityAttributes)
                {
                    now = (now & 0xffffffff00000000L) | securityAttributes;
                }
                else
                {
                    throw new IllegalArgumentException(
                            "Required to include security attributes as declared in state.");
                }
            }
            else
            {
                if (hasSecurityAttributes)
                {
                    throw new IllegalArgumentException(
                            "Cannot include security attributes if not declared in state.");
                }
            }
            // 2b., 3., 4., 5. set time low, mid high and version fields
            packFirstHalf(now, state.getVersion());
            // 6., 7., 8. set clock sequence and variant fields
            packSecondHalf(state.getClockSequence(), state.getVariant(), state
                    .getNode());
            saveState(state);
        }
    }

    /**
     * Converts <code>nibbles</code> least significant nibbles of
     * <code>value</code> into a hex representation and appends it to
     * <code>result</code>.
     */
    private void toHex(StringBuffer result, long value, int nibbles)
    {
        if (nibbles > 0)
        {
            toHex(result, value >>> 4, nibbles - 1);
            result.append(HEX_CHARS[(int) value & 0xf]);
        }
    }

    /**
     * Converts a hex character into the corresponding nibble value.
     * 
     * @return the nibble value.
     * @throws NumberFormatException
     *             if <code>c</code> is not a valid hex character.
     */
    private long parseNibble(char c)
    {
        switch (c)
        {
            case '0' :
                return 0;
            case '1' :
                return 1;
            case '2' :
                return 2;
            case '3' :
                return 3;
            case '4' :
                return 4;
            case '5' :
                return 5;
            case '6' :
                return 6;
            case '7' :
                return 7;
            case '8' :
                return 8;
            case '9' :
                return 9;
            case 'a' :
            case 'A' :
                return 10;
            case 'b' :
            case 'B' :
                return 11;
            case 'c' :
            case 'C' :
                return 12;
            case 'd' :
            case 'D' :
                return 13;
            case 'e' :
            case 'E' :
                return 14;
            case 'f' :
            case 'F' :
                return 15;
            default :
                throw new NumberFormatException();
        }
    }

    /**
     * Tests wether <code>c</code> is a hyphen '-' character.
     * 
     * @throws NumberFormatException
     *             if <code>c</code> is not a hyphen '-' character.
     */
    private void parseHyphen(char c)
    {
        if (c != '-')
        {
            throw new NumberFormatException();
        }
    }

    /**
     * Parses the character sequence <code>cs</code> from begin to end as hex
     * string.
     * 
     * @throws NumberFormatException
     *             if <code>cs</code> contains non-hex characters.
     */
    private long parseHex(CharSequence cs)
    {
        long retval = 0;
        for (int i = 0; i < cs.length(); i++)
        {
            retval = (retval << 4) + parseNibble(cs.charAt(i));
        }
        return retval;
    }

    /**
     * Parses the character sequence <code>cs</code> from begin to end as the
     * first 18 charcters of a string representation of an AUID.
     * 
     * @return the first 64 bits represented by <code>cs</code>.
     * @throws NumberFormatException
     *             if <code>cs</code> does no match the pattern
     *             "LLLLLLLL-MMMM-HHHH" where 'L', 'M', 'H' are time low, mid
     *             and high fields respectively.
     */
    private long parseFirstHalf(CharSequence charSequence)
    {
        long timeLow = parseHex(charSequence.subSequence(0, 8));
        parseHyphen(charSequence.charAt(8));
        long timeMid = parseHex(charSequence.subSequence(9, 13));
        parseHyphen(charSequence.charAt(13));
        long timeHi = parseHex(charSequence.subSequence(14, 18));
        return (timeHi << 48) | (timeMid << 32) | timeLow;
    }

    /**
     * Parses the character sequence <code>cs</code> from begin to end as the
     * second 18 charcters of a string representation of an AUID.
     * 
     * @return the second 64 bits represented by <code>cs</code>.
     * @throws NumberFormatException
     *             if <code>cs</code> does no match the pattern
     *             "-CCCC-NNNNNNNNNNNN" where 'C', 'N' are clock sequence and
     *             node fields respectively.
     */
    private long parseSecondHalf(CharSequence charSequence)
    {
        parseHyphen(charSequence.charAt(0));
        long clockSeq = parseHex(charSequence.subSequence(1, 5));
        parseHyphen(charSequence.charAt(5));
        long node = parseHex(charSequence.subSequence(6, 18));
        return (node << 16) | ((clockSeq & 0xff) << 8) | (clockSeq >>> 8);
    }

    /**
     * Gets a value from the specified byte array starting at begin with the
     * specified number of octets and endianess.
     * @param octets
     *            the number of octets
     * @param bytes
     *            the array to get the value from
     * @param begin
     *            the offset to use when writing into the destination byte
     *            array, ignored if dst is <code>null</code>
     * @param bigendian
     *            if <code>true</code> big endian byte order is used
     * @return the octet
     */
    protected static final long getOctets(int octets, byte[] bytes, int begin,
            boolean bigendian)
    {
        if (octets > 1)
        {
            if (bigendian)
            {
                return ((bytes[begin] & 0xffL) << (8 * (octets - 1)))
                        | getOctets(octets - 1, bytes, begin + 1, bigendian);
            }
            else
            {
                return getOctets(octets - 1, bytes, begin, bigendian)
                        | ((bytes[begin + octets - 1] & 0xffL) << (8 * (octets - 1)));
            }
        }
        else
        {
            return bytes[begin] & 0xffL;
        }
    }

    /**
     * Puts the specified value into the byte array in the specified endianess.
     * @param value
     *            the value
     * @param octets
     *            the number of octets
     * @param dst
     *            the destination array
     * @param dstBegin
     *            the offset to use when writing into the destination byte
     *            array, ignored if dst is <code>null</code>
     * @param bigendian
     *            if <code>true</code> big endian byte order is used
     */
    protected static final void putOctets(long value, int octets, byte[] dst,
            int dstBegin, boolean bigendian)
    {
        if (bigendian)
        {
            if (octets > 1)
            {
                putOctets(value >>> 8, octets - 1, dst, dstBegin, bigendian);
            }
            dst[dstBegin + octets - 1] = (byte) (value & 0xff);
        }
        else
        {
            dst[dstBegin] = (byte) (value & 0xff);
            if (octets > 1)
            {
                putOctets(value >>> 8, octets - 1, dst, dstBegin + 1, bigendian);
            }
        }
    }

    /**
     * Returns time low octets 0-3 (unsigned) int in a signed long.
     * 
     * @return the time low field (original DCE field).
     */
    public final long getTimeLow()
    {
        return firstHalf & 0xffffffff;
    }

    /**
     * Returns time mid octets 4-5 (unsigned) int in a signed long.
     * 
     * @return the time mid field (original DCE field).
     */
    public final long getTimeMid()
    {
        return (firstHalf >>> 32) & 0xffff;
    }

    /**
     * Returns time high octets 6-7 (unsigned) int in a signed long.
     * 
     * @return the time high field with version masked out.
     */
    public final long getTimeHigh()
    {
        return (firstHalf >>> 48) & 0x0fff;
    }

    /**
     * Returns octets 6-7 (unsigned) int in a signed long.
     * 
     * @return the time high and version field (original DCE field).
     */
    public final long getTimeHighAndVersion()
    {
        return (firstHalf >>> 48);
    }

    /**
     * Returns octets 0-7 time only.
     * 
     * @return the complete time value.
     */
    public final long getTime()
    {
        return firstHalf & 0x0fffffffffffffffL;
    }

    /**
     * Returns the time of the AUID as Date. This is a narrowing conversion
     * because Date only supports a granularity of milliseconds, while the time
     * value represents 100 nanosecond intervals. Use <code>getNanos</code> to
     * retrieve the truncated nanoseconds.
     * 
     * @return the complete time value as Date truncated to the next
     *         millisecond.
     */
    public final Date getDate()
    {
        return new Date((getTime() + UTC_OFFSET) / TIME_SCALE);
    }

    /**
     * Returns the nanoseconds truncated from the time of the AUID by
     * <code>getDate</code>.
     * 
     * @return the nanoseconds truncated from the time of the AUID by
     *         <code>getDate</code>.
     */
    public final long getNanos()
    {
        return (getTime() + UTC_OFFSET) % TIME_SCALE;
    }

    /**
     * Returns octets 6-7 version only.
     * 
     * @return the version field.
     */
    public final int getVersion()
    {
        return (int) (firstHalf >>> 60);
    }

    /**
     * Returns clock sequence high and variant octet 8 (unsigned) small in a
     * signed int.
     * 
     * @return the clock seq hi and reserved (variant) field (original DCE
     *         field).
     */
    public final int getClockSeqHighAndVariant()
    {
        return (int) (secondHalf & 0xff);
    }

    /**
     * Returns clock sequence low octet 9 (unsigned) small in a signed int.
     * 
     * @return the clock seq low field (original DCE field).
     */
    public final int getClockSeqLow()
    {
        return (int) ((secondHalf >>> 8) & 0xff);
    }

    /**
     * Returns clock sequence and variant octets 8-9 (unsigned) short in a
     * signed int.
     * 
     * @return the clock sequence and variant field.
     */
    public final int getClockSeqAndVariant()
    {
        return (getClockSeqHighAndVariant() << 8) | getClockSeqLow();
    }

    /**
     * Returns clock sequence.
     * 
     * @return the clock sequence field.
     */
    public final int getClockSeq()
    {
        int csv = getClockSeqAndVariant();
        return csv & getClockSeqMaskForVariant(identifyVariant(csv));
    }

    /**
     * Returns the variant.
     * 
     * @return the variant field.
     */
    public final int getVariant()
    {
        return identifyVariant(getClockSeqAndVariant());
    }

    /**
     * Returns node value octets 10-15 (unsigned) 48-bit in a signed long.
     * 
     * @return the node field (original DCE field).
     */
    public final long getNode()
    {
        return secondHalf >>> 16;
    }

    /**
     * Conveniance method just returns <code>getBytes(null, 0)</code>.
     * 
     * @return <code>getBytes(null, 0)</code>.
     */
    public final byte[] getBytes()
    {
        return getBytes(null, 0);
    }

    /*
     * Object methods
     */
    public boolean equals(Object obj)
    {
        if (obj instanceof AUID)
        {
            AUID other = (AUID) obj;
            return (firstHalf == other.firstHalf)
                    && (secondHalf == other.secondHalf);
        }
        return false;
    }

    public int hashCode()
    {
        return (int) (firstHalf ^ (firstHalf >>> 32) ^ secondHalf ^ (secondHalf >>> 32));
    }

    public String toString()
    {
        return toStringBuffer(null).toString();
    }

    /*
     * Comparable methods
     */
    public int compareTo(Object o)
    {
        AUID other = (AUID) o;
        long cmp = getTimeLow() - other.getTimeLow();
        if (cmp != 0)
        {
            cmp = getTimeMid() - other.getTimeMid();
            if (cmp != 0)
            {
                cmp = getTimeHighAndVersion() - other.getTimeHighAndVersion();
                if (cmp != 0)
                {
                    cmp = getClockSeqHighAndVariant()
                            - other.getClockSeqHighAndVariant();
                    if (cmp != 0)
                    {
                        cmp = getClockSeqLow() - other.getClockSeqLow();
                        if (cmp != 0)
                        {
                            cmp = getNode() - other.getNode();
                        }
                    }
                }
            }
        }
        return (cmp == 0 ? 0 : (cmp < 0 ? -1 : 1));
    }

    /*
     * Utility method for "real" random seed
     */
    private static long entropicSeed(int bits, long initialSeed)
    {
        if (bits > 63)
        {
            bits = 63;
        }
        else if (bits < 1)
        {
            bits = 1;
        }
        final long startTime = System.currentTimeMillis();
        final int[] counters = new int[bits + 1];
        final Random[] randoms = new Random[bits];
        final Thread[] threads = new Thread[bits];
        final int endvalue = bits * 128;
        final int lastindex = bits;
        // create threads
        Random random = new Random(initialSeed);
        for (int i = 0; i < bits; i++)
        {
            final int thisindex = i;
            long nextSeed = random.nextLong();
            randoms[i] = new Random(nextSeed);
            threads[i] = new Thread()
            {
                public void run()
                {
                    try
                    {
                        while (counters[lastindex] < endvalue)
                        {
                            long value = randoms[thisindex].nextLong();
                            int loop = ((int) (value & 255)) + 16;
                            for (int a = 0; a < loop; a++)
                            {
                                randoms[thisindex].nextLong();
                                if (System.currentTimeMillis() - startTime > MAXIMUM_ENTROPIC_TIME_MS)
                                {
                                    break;
                                }
                            }
                            counters[thisindex]++;
                            if (System.currentTimeMillis() - startTime > MAXIMUM_ENTROPIC_TIME_MS)
                            {
                                break;
                            }
                        }
                    }
                    catch (Throwable t)
                    {
                        NucleusLogger.VALUEGENERATION.error(t);
                        counters[thisindex] = endvalue;
                    }
                    finally
                    {
                        threads[thisindex] = null;
                    }
                }
            };
            threads[i].start();
        }
        // check if all threads revolved at least bits times
        for (int i = 0; i < bits; i++)
        {
            while (counters[i] < bits)
            {
                Thread.yield();
                if (System.currentTimeMillis() - startTime > MAXIMUM_ENTROPIC_TIME_MS)
                {
                    break;
                }
            }
        }
        // check if all threads together revolved often enough
        while (counters[lastindex] < endvalue)
        {
            Thread.yield();
            int sum = 0;
            for (int i = 0; i < bits; i++)
            {
                sum += counters[i];
            }
            counters[lastindex] = sum;
            if (System.currentTimeMillis() - startTime > MAXIMUM_ENTROPIC_TIME_MS)
            {
                break;
            }
        }
        // check if all threads stopped
        for (int i = 0; i < bits; i++)
        {
            while (threads[i] != null)
            {
                Thread.yield();
            }
        }
        // create a new seed
        long seed = 0;
        for (int i = 0; i < bits; i++)
        {
            //seed = (seed << 1) + (randoms[i].nextLong() & 1);
            seed += randoms[i].nextLong();
        }
        return seed;
    }
}