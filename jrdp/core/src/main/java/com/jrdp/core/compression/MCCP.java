/*
 * Copyright (C) 2013 JRDP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jrdp.core.compression;

import java.util.Arrays;

public class MCCP
{
	private static final int MCCP_HISTORY_BUFFER_SIZE = 65536;
	private static final int RDP_MPPC_DICT_SIZE = 65536;
	private static final byte MCCP_PACKET_AT_FRONT = 0x40;
	private static final byte MCCP_FLUSHED = (byte) 0x80;
	private static final byte MCCP_COMPRESSION_TYPE_8K = 0x00;
	private static final byte MCCP_COMPRESSION_TYPE_64K = 0x01;
	public static final byte MCCP_COMPRESSED = 0x20;
	
	private byte[] historyBuffer;
	//private int historyOffset;
	private int historyroff;

	private int resultOffset;
	private int resultLength;
	private int roff;
	private int rlen;
	
	public MCCP()
	{
		historyBuffer = new byte[MCCP_HISTORY_BUFFER_SIZE];
	}
	
	public byte[] getHistoryBuffer()
	{
		return historyBuffer;
	}
	
	public int getResultOffset()
	{
		//return resultOffset;
		return roff;
	}
	
	public int getResultLength()
	{
		//return resultLength;
		return rlen;
	}
	
	public int mppc_expand(byte[] data, int clen, byte ctype)
	{
	    int k, walker_len = 0, walker;
	    int i = 0;
	    int next_offset, match_off;
	    int match_len;
	    int old_offset, match_bits;
	    boolean big = (ctype & MCCP_COMPRESSION_TYPE_64K) == MCCP_COMPRESSION_TYPE_64K;

	    if ((ctype & MCCP_COMPRESSED) == 0)
	    {
	        roff = 0;
	        rlen = clen;
	        return 0;
	    }

	    if ((ctype & MCCP_PACKET_AT_FRONT) != 0)
	    {
	        historyroff = 0;
	    }

	    if ((ctype & MCCP_FLUSHED) != 0)
	    {
			Arrays.fill(historyBuffer, (byte) 0);
	        historyroff = 0;
	    }

	    roff = 0;
	    rlen = 0;

	    walker = historyroff;

	    next_offset = walker;
	    old_offset = next_offset;
	    roff = old_offset;
	    if (clen == 0)
	        return 0;
	    clen += i;

	    do
	    {
	        if (walker_len == 0)
	        {
	            if (i >= clen)
	                break;
	            walker = data[i++] << 24;
	            walker_len = 8;
	//printf("walker: %i\n", walker);
	        }
	        if (walker >= 0)
	        {
	            if (walker_len < 8)
	            {
	                if (i >= clen)
	                {
	                    if (walker != 0)
	                        return -1;
	                    break;
	                }
	                walker |= (data[i++] & 0xff) << (24 - walker_len);
	                walker_len += 8;
	            }
	            if (next_offset >= RDP_MPPC_DICT_SIZE)
	                return -1;
	            historyBuffer[next_offset++] = (byte) (walker >>> 24);
	            walker <<= 8;
	            walker_len -= 8;
	            continue;
	        }
	        walker <<= 1;
	        /* fetch next 8-bits */
	        if (--walker_len == 0)
	        {
	            if (i >= clen)
	                return -1;
	            walker = data[i++] << 24;
	            walker_len = 8;
	        }
	        /* literal decoding */
	        if (walker >= 0)
	        {
	            if (walker_len < 8)
	            {
	                if (i >= clen)
	                    return -1;
	                walker |= (data[i++] & 0xff) << (24 - walker_len);
	                walker_len += 8;
	            }
	            if (next_offset >= RDP_MPPC_DICT_SIZE)
	                return -1;
	            historyBuffer[next_offset++] = (byte) (walker >> 24 | 0x80);
	            walker <<= 8;
	            walker_len -= 8;
	            continue;
	        }

	        /* decode offset  */
	        /* length pair    */
	        walker <<= 1;
	        if (--walker_len < (big ? 3 : 2))
	        {
	            if (i >= clen)
	                return -1;
	            walker |= (data[i++] & 0xff) << (24 - walker_len);
	            walker_len += 8;
	        }

	        if (big)
	        {
	            /* offset decoding where offset len is:
	               -63: 11111 followed by the lower 6 bits of the value
	               64-319: 11110 followed by the lower 8 bits of the value ( value - 64 )
	               320-2367: 1110 followed by lower 11 bits of the value ( value - 320 )
	               2368-65535: 110 followed by lower 16 bits of the value ( value - 2368 )
	             */
	            switch (walker >>> 29)
	            {
	                case 7:    /* - 63 */
	                    for (; walker_len < 9; walker_len += 8)
	                    {
	                        if (i >= clen)
	                            return -1;
	                        walker |= (data[i++] & 0xff) << (24 - walker_len);
	                    }
	                    walker <<= 3;
	                    match_off = (walker >>> 26);
	                    walker <<= 6;
	                    walker_len -= 9;
	                    break;

	                case 6:    /* 64 - 319 */
	                    for (; walker_len < 11; walker_len += 8)
	                    {
	                        if (i >= clen)
	                            return -1;
	                        walker |= (data[i++] & 0xff) << (24 - walker_len);
	                    }

	                    walker <<= 3;
	                    match_off = (walker >>> 24) + 64;
	                    walker <<= 8;
	                    walker_len -= 11;
	                    break;

	                case 5:
	                case 4:    /* 320 - 2367 */
	                    for (; walker_len < 13; walker_len += 8)
	                    {
	                        if (i >= clen)
	                            return -1;
	                        walker |= (data[i++] & 0xff) << (24 - walker_len);
	                    }

	                    walker <<= 2;
	                    match_off = (walker >>> 21) + 320;
	                    walker <<= 11;
	                    walker_len -= 13;
	                    break;

	                default:    /* 2368 - 65535 */
	                    for (; walker_len < 17; walker_len += 8)
	                    {
	                        if (i >= clen)
	                            return -1;
	                        walker |= (data[i++] & 0xff) << (24 - walker_len);
	                    }

	                    walker <<= 1;
	                    match_off = (walker >>> 16) + 2368;
	                    walker <<= 16;
	                    walker_len -= 17;
	                    break;
	            }
	        }
	        else
	        {
	            /* offset decoding where offset len is:
	               -63: 1111 followed by the lower 6 bits of the value
	               64-319: 1110 followed by the lower 8 bits of the value ( value - 64 )
	               320-8191: 110 followed by the lower 13 bits of the value ( value - 320 )
	             */
	            switch (walker >>> 30)
	            {
	                case 3:    /* - 63 */
	                    if (walker_len < 8)
	                    {
	                        if (i >= clen)
	                            return -1;
	                        walker |= (data[i++] & 0xff) << (24 - walker_len);
	                        walker_len += 8;
	                    }
	                    walker <<= 2;
	                    match_off = (walker >>> 26);
	                    walker <<= 6;
	                    walker_len -= 8;
	                    break;

	                case 2:    /* 64 - 319 */
	                    for (; walker_len < 10; walker_len += 8)
	                    {
	                        if (i >= clen)
	                            return -1;
	                        walker |= (data[i++] & 0xff) << (24 - walker_len);
	                    }

	                    walker <<= 2;
	                    match_off = (walker >>> 24) + 64;
	                    walker <<= 8;
	                    walker_len -= 10;
	                    break;

	                default:    /* 320 - 8191 */
	                    for (; walker_len < 14; walker_len += 8)
	                    {
	                        if (i >= clen)
	                            return -1;
	                        walker |= (data[i++] & 0xff) << (24 - walker_len);
	                    }

	                    match_off = (walker >> 18) + 320;
	                    walker <<= 14;
	                    walker_len -= 14;
	                    break;
	            }
	        }
	        if (walker_len == 0)
	        {
	            if (i >= clen)
	                return -1;
	            walker = data[i++] << 24;
	            walker_len = 8;
	        }

	        /* decode length of match */
	        match_len = 0;
	        if (walker >= 0)
	        {        /* special case - length of 3 is in bit 0 */
	            match_len = 3;
	            walker <<= 1;
	            walker_len--;
	        }
	        else
	        {
	            /* this is how it works len of:
	               4-7: 10 followed by 2 bits of the value
	               8-15: 110 followed by 3 bits of the value
	               16-31: 1110 followed by 4 bits of the value
	               32-63: .... and so forth
	               64-127:
	               128-255:
	               256-511:
	               512-1023:
	               1024-2047:
	               2048-4095:
	               4096-8191:

	               i.e. 4097 is encoded as: 111111111110 000000000001
	               meaning 4096 + 1...
	             */
	            match_bits = big ? 14 : 11;    /* 11 or 14 bits of value at most */
	            do
	            {
	                walker <<= 1;
	                if (--walker_len == 0)
	                {
	                    if (i >= clen)
	                        return -1;
	                    walker = data[i++] << 24;
	                    walker_len = 8;
	                }
	                if (walker >= 0)
	                    break;
	                if (--match_bits == 0)
	                {
	                    return -1;
	                }
	            }
	            while (true);
	            match_len = (big ? 16 : 13) - match_bits;
	            walker <<= 1;
	            if (--walker_len < match_len)
	            {
	                for (; walker_len < match_len; walker_len += 8)
	                {
	                    if (i >= clen)
	                    {
	                        return -1;
	                    }
	                    walker |= (data[i++] & 0xff) << (24 - walker_len);
	                }
	            }

	            match_bits = match_len;
	            match_len =
	                ((walker >> (32 - match_bits)) & (~(-1 << match_bits))) | (1 <<
	                                               match_bits);
	            walker <<= match_bits;
	            walker_len -= match_bits;
	        }
	        if (next_offset + match_len >= RDP_MPPC_DICT_SIZE)
	        {
	            return -1;
	        }
	        /* memory areas can overlap - meaning we can't use memXXX functions */
	        k = (next_offset - match_off) & (big ? 65535 : 8191);
	        do
	        {
	        	historyBuffer[next_offset++] = historyBuffer[k++];
	        }
	        while (--match_len != 0);
	    }
	    while (true);

	    /* store history offset */
	    historyroff = next_offset;

	    roff = old_offset;
	    rlen = next_offset - old_offset;

	    //printf("roff: %d rlen: %d\n", *roff, *rlen);

	    return 0;
	}
	
	/*
	public boolean decompress(InputByteStream packet, byte compressedType, int compressedLength)
	{
		if((compressedType & MCCP_COMPRESSED) != MCCP_COMPRESSED)
			return true;
		if((compressedType & MCCP_PACKET_AT_FRONT) != MCCP_PACKET_AT_FRONT)
			historyOffset = 0;
		if((compressedType & MCCP_FLUSHED) != MCCP_FLUSHED)
		{
			historyOffset = 0;
			Arrays.fill(historyBuffer, (byte) 0);
		}
		if(compressedLength == 0)
			return true;
		
		int offset, nextOffset, oldOffset;
		offset = nextOffset = oldOffset = historyOffset;
		int matchOffset = 0;
		int matchLength = 0;
		int matchBits = 0;
		int offsetLength = 0;
		int i = 0;
		
		byte[] data = packet.getByteArray(compressedLength);
		
		boolean is64kDecompression = (compressedType & MCCP_COMPRESSION_TYPE_64K) == MCCP_COMPRESSION_TYPE_64K;
		
		while(true)
		{
			if(offsetLength == 0)
			{
				if(i >= compressedLength)
					break;
				System.out.println("offset before: " + Integer.toHexString(data[i] & 0xff));
				offset = (data[i++] & 0xff) << 24;
				offsetLength = 8;
				//System.out.println("offset after: " + Long.toHexString(offset));
			}
			if(offset >= 0)
			{
				if(offsetLength < 8)
				{
					if(i >= compressedLength)
					{
						if(offset != 0)
							return false;
						break;
					}
					offset |= (data[i++] & 0xff) << (24 - offsetLength);
					offsetLength += 8;
				}
				if(nextOffset >= MCCP_HISTORY_BUFFER_SIZE)
					return false;
				historyBuffer[(int) nextOffset++] = (byte) (offset >> 24);
				offset <<= 8;
				offsetLength -= 8;
				continue;
			}
			//get next byte of data
			offset <<= 1;
			if(--offsetLength == 0)
			{
				if(i >= compressedLength)
					return false;
				offset = data[i++] << 24;
				offsetLength = 8;
			}
			//decode litteral
			if(offset >= 0)
			{
				if(offsetLength < 8)
				{
					if(i <= compressedLength)
						return false;
					offset |= (data[i++] & 0xff) << (24 - offsetLength);
					offsetLength += 8;
				}
				if(nextOffset >= MCCP_HISTORY_BUFFER_SIZE)
					return false;
				historyBuffer[(int) nextOffset++] = (byte) ((offset >> 24) | 0x80);
				offset <<= 8;
				offsetLength -= 8;
				continue;
			}
			
			//decode offset
			offset <<= 1;
			if(--offsetLength < (is64kDecompression ? 3 : 2))
			{
				if(i >= compressedLength)
					return false;
				offset |= (data[i++] & 0xff) << (24 - offsetLength);
				offsetLength += 8;
			}
			
			if(is64kDecompression)
			{
				//offset decoding... 3.1.8.4.2.2.1 Copy-Offset Encoding
				//http://msdn.microsoft.com/en-us/library/cc240851%28v=PROT.10%29.aspx
				switch((int) offset >> 29)
				{
				case 7:				// 0 - 63
					while(offsetLength < 9)
					{
						if(i >= compressedLength)
							return false;
						offset |= (data[i++] & 0xff) << (24 - offsetLength);
						offsetLength += 8;
					}
					offset <<= 3;
					matchOffset = offset >> 26;
					offset <<= 6;
					offsetLength -= 9;
					break;
				case 6:				// 64 - 319
					while(offsetLength < 11)
					{
						if(i >= compressedLength)
							return false;
						offset |= (data[i++] & 0xff) << (24 - offsetLength);
						offsetLength += 8;
					}
					offset <<= 3;
					matchOffset = (offset >> 24) + 64;
					offset <<= 8;
					offsetLength -= 11;
					break;
				case 5:	case 4:		// 320 - 2367
					while(offsetLength < 13)
					{
						if(i >= compressedLength)
							return false;
						offset |= (data[i++] & 0xff) << (24 - offsetLength);
						offsetLength += 8;
					}
					offset <<= 2;
					matchOffset = (offset >> 21) + 320;
					offset <<= 11;
					offsetLength -= 13;
					break;
				default:			// 2368+
					while(offsetLength < 17)
					{
						if(i >= compressedLength)
							return false;
						offset |= (data[i++] & 0xff) << (24 - offsetLength);
						offsetLength += 8;
					}
					offset <<= 1;
					matchOffset = (offset >> 16) + 2368;
					offset <<= 16;
					offsetLength -= 17;
				}
			}
			else
			{
				switch((int) offset >> 30)
				{
				case 3:				// 0 - 63
					while(offsetLength < 9)
					{
						if(i >= compressedLength)
							return false;
						offset |= (data[i++] & 0xff) << (24 - offsetLength);
						offsetLength += 8;
					}
					offset <<= 3;
					matchOffset = offset >> 26;
					offset <<= 6;
					offsetLength -= 9;
					break;
				case 2:				// 64 - 319
					while(offsetLength < 10)
					{
						if(i >= compressedLength)
							return false;
						offset |= (data[i++] & 0xff) << (24 - offsetLength);
						offsetLength += 8;
					}
					offset <<= 2;
					matchOffset = (offset >> 24) + 64;
					offset <<= 8;
					offsetLength -= 10;
					break;
				default:			// 320+
					while(offsetLength < 14)
					{
						if(i >= compressedLength)
							return false;
						offset |= (data[i++] & 0xff) << (24 - offsetLength);
						offsetLength += 8;
					}
					matchOffset = (offset >> 18) + 320;
					offset <<= 14;
					offsetLength -= 14;
					break;
				}
			}
			if(offset == 0)
			{
				if(i >= compressedLength)
					return false;
				offset = data[i++] << 24;
				offsetLength = 8;
			}
			
			matchLength = 0;
			if(offset >= 0)
			{
				matchLength = 3;
				offset <<= 1;
				offsetLength--;
			}
			else
			{
				matchBits = is64kDecompression ? 14 : 11;
				while(true)
				{
					offset <<= 1;
					if(--offsetLength == 0)
					{
						if(i >= compressedLength)
							return false;
						offset = data[i++] << 24;
						offsetLength = 8;
					}
					if(offset >= 0)
						break;
					if(--matchBits == 0)
						return false;
				}
				matchLength = (is64kDecompression ? 16 : 13) - matchBits;
				offset <<= 1;
				if(--offsetLength < matchLength)
				{
					while(offsetLength < matchLength)
					{
						if(i >= compressedLength)
							return false;
						offset |= (data[i++] & 0xff) << (24 << offsetLength);
						offsetLength += 8;
					}
				}
				matchBits = matchLength;
				matchLength = ((offset >> (32 - matchBits)) & (~(-1 << matchBits))) | (1 << matchBits);
				offset <<= matchBits;
				offsetLength -= matchBits;
			}
			if(nextOffset + matchLength >= MCCP_HISTORY_BUFFER_SIZE)
				return false;
			
			long next = (nextOffset - matchOffset) & (is64kDecompression ? 65535 : 8191);
			do
			{
				historyBuffer[(int) nextOffset++] = historyBuffer[(int) next++];
			}
			while(--matchLength != 0);
		}
		
		historyOffset = nextOffset;
		
		resultOffset = oldOffset;
		resultLength = nextOffset - oldOffset;
		
		return true;
	}*/
}
