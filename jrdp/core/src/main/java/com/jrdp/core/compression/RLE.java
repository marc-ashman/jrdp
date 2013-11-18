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


public class RLE {
	public static final int[] decompressInt(int width, int height, int size, byte[] compressed_pixel, int Bpp, int bitsPerPixel) throws Exception {

		int previous = -1, line = 0;
		int input = 0, output = 0, end = size;
		int opcode = 0, count = 0, offset = 0, x = width;
		int lastopcode = -1, fom_mask = 0;
		int code = 0, color1 = 0, color2 = 0;
		byte mixmask = 0;
		int mask = 0;
		int mix = 0xffffffff;

		boolean insertmix = false, bicolor = false, isfillormix = false;

		int[] pixel = new int[width * height];
		while (input < end)
		{
			fom_mask = 0;
			code = (compressed_pixel[input++] & 0x000000ff);
			opcode = code >> 4;

			switch(opcode){
			case 0xc:
			case 0xd:			//Light
			case 0xe:
				opcode -= 6;
				count = code & 0xf;
				offset = 16;
				break;

			case 0xf:			//MegaMega
				opcode = code & 0xf;
				if(opcode < 9)
				{
					count = (compressed_pixel[input++] & 0xff);
					count |= ((compressed_pixel[input++] & 0xff) << 8);
				}
				else
				{
					count = (opcode < 0xb) ? 8 : 1;
				}
				offset = 0;
				break;

			default:			//Regular
				opcode >>= 1;
				count = code & 0x1f;
				offset = 32;
				break;
			}

			if(offset != 0)
			{
				isfillormix = ((opcode == 2) || (opcode == 7));

				if(count == 0)
				{
					if(isfillormix)
						count = (compressed_pixel[input++] & 0x000000ff) + 1;
					else
						count = (compressed_pixel[input++] & 0x000000ff)
								+ offset;
				}
				else if (isfillormix)
				{
					count <<= 3;
				}
			}

			switch(opcode)
			{
			case 0:
				if((lastopcode == opcode) && !((x == width) && (previous == -1)))
					insertmix = true;
				break;
			case 8:
				color1 = cvalx(compressed_pixel, input, Bpp, bitsPerPixel);
				input += Bpp;
			case 3:
				color2 = cvalx(compressed_pixel, input, Bpp, bitsPerPixel);
				input += Bpp;
				break;
			case 6:
			case 7:
				mix = cvalx(compressed_pixel, input, Bpp, bitsPerPixel);
				input += Bpp;
				opcode -= 5;
				break;
			case 9:
				mask = 0x03;
				opcode = 0x02;
				fom_mask = 3;
				break;
			case 0x0a:
				mask = 0x05;
				opcode = 0x02;
				fom_mask = 5;
				break;

			}

			lastopcode = opcode;
			mixmask = 0;

			while(count > 0)
			{
               if(x >= width)
               {
                   if (height <= 0)
                	   throw new Exception("Decompressing bitmap failed! Height = " + height);

					x = 0;
					height--;

					previous = line;
					line = output + height * width;
				}

				switch(opcode)
				{
				case 0:
					if(insertmix)
					{
						if(previous == -1)
						{
							pixel[line+x] = mix;
						}
						else
						{
							pixel[line+x] = (pixel[previous+x] ^ mix);
						}

						insertmix = false;
						count--;
						x++;
					}

					if(previous == -1)
					{
						while(((count & ~0x7) != 0) && ((x + 8) < width))
						{
							for(int i = 0; i < 8; i++)
							{
								pixel[line+x] = 0;
								count--;
								x++;
							}
						}
						while((count > 0) && (x < width))
						{
							pixel[line+x] = 0;
							count--;
							x++;
						}
					}else
					{
						while(((count & ~0x7) != 0) && ((x + 8) < width))
						{
							for(int i = 0; i < 8; i++)
							{
								pixel[line + x] = pixel[previous + x];
								count--;
								x++;
							}
						}
						while((count > 0) && (x < width))
						{
							pixel[line + x] = pixel[previous + x];
							count--;
							x++;
						}
					}
					break;

				case 1:
					if(previous == -1)
					{
						while(((count & ~0x7) != 0) && ((x + 8) < width))
						{
							for(int i = 0; i < 8; i++)
							{
								pixel[line + x] = mix;
								count--;
								x++;
							}
						}
						while((count > 0) && (x < width))
						{
							pixel[line + x] = mix;
							count--;
							x++;
						}
					}
					else
					{
						while(((count & ~0x7) != 0) && ((x + 8) < width))
						{
							for(int i = 0; i < 8; i++)
							{
								pixel[line + x] = pixel[previous + x] ^ mix;
								count--;
								x++;
							}
						}
						while((count > 0) && (x < width))
						{
							pixel[line + x] = pixel[previous + x] ^ mix;
							count--;
							x++;
						}
					}
					break;
				case 2:
					if(previous == -1)
					{
						while(((count & ~0x7) != 0) && ((x + 8) < width))
						{
							for(int i = 0; i < 8; i++)
							{
								mixmask <<= 1;
								if (mixmask == 0)
								{
									mask = (fom_mask != 0) ? (byte) fom_mask
											: compressed_pixel[input++];
									mixmask = 1;
								}
								if((mask & mixmask) != 0)
									pixel[line + x] = (byte) mix;
								else
									pixel[line + x] = 0;
								count--;
								x++;
							}
						}
						while((count > 0) && (x < width))
						{
							mixmask <<= 1;
							if(mixmask == 0)
							{
								mask = (fom_mask != 0) ? (byte) fom_mask
										: compressed_pixel[input++];
								mixmask = 1;
							}
							if((mask & mixmask) != 0)
								pixel[line + x] = mix;
							else
								pixel[line + x] = 0;
							count--;
							x++;
						}
					}
					else
					{
						while(((count & ~0x7) != 0) && ((x + 8) < width))
						{
							for(int i = 0; i < 8; i++)
							{
								mixmask <<= 1;
								if(mixmask == 0)
								{
									mask = (fom_mask != 0) ? (byte) fom_mask
											: compressed_pixel[input++];
									mixmask = 1;
								}
								if((mask & mixmask) != 0)
									pixel[line + x] = (pixel[previous + x] ^ mix);
								else
									pixel[line + x] = pixel[previous + x];
								count--;
								x++;
							}
						}
						while((count > 0) && (x < width))
						{
							mixmask <<= 1;
							if(mixmask == 0)
							{
								mask = (fom_mask != 0) ? (byte) fom_mask
										: compressed_pixel[input++];
								mixmask = 1;
							}
							if((mask & mixmask) != 0)
								pixel[line + x] = (pixel[previous + x] ^ mix);
							else
								pixel[line + x] = pixel[previous + x];
							count--;
							x++;
						}

					}
					break;

				case 3:
					while(((count & ~0x7) != 0) && ((x + 8) < width))
					{
						for(int i = 0; i < 8; i++)
						{
							pixel[line + x] = color2;
							count--;
							x++;
						}
					}
					while((count > 0) && (x < width))
					{
						pixel[line + x] = color2;
						count--;
						x++;
					}

					break;

				case 4:
					while(((count & ~0x7) != 0) && ((x + 8) < width))
					{
						for(int i = 0; i < 8; i++)
						{
							pixel[line + x] = cvalx(compressed_pixel, input, Bpp, bitsPerPixel);
							input += Bpp;
							count--;
							x++;
						}
					}
					while((count > 0) && (x < width))
					{
						pixel[line + x] = cvalx(compressed_pixel, input, Bpp, bitsPerPixel);
						input += Bpp;
						count--;
						x++;
					}
					break;

				case 8:
					while(((count & ~0x7) != 0) && ((x + 8) < width))
					{
						for(int i = 0; i < 8; i++)
						{
							if(bicolor)
							{
								pixel[line + x] = color2;
								bicolor = false;
							}
							else
							{
								pixel[line + x] = color1;
								bicolor = true;
								count++;
							}
							count--;
							x++;
						}
					}
					while((count > 0) && (x < width))
					{
						if(bicolor)
						{
							pixel[line + x] = color2;
							bicolor = false;
						}
						else
						{
							pixel[line + x] = color1;
							bicolor = true;
							count++;
						}
						count--;
						x++;
					}

					break;

				case 0xd:
					while(((count & ~0x7) != 0) && ((x + 8) < width))
					{
						for(int i = 0; i < 8; i++)
						{
							pixel[line + x] = 0xffffff;
							count--;
							x++;
						}
					}
					while((count > 0) && (x < width))
					{
						pixel[line + x] = 0xffffff;
						count--;
						x++;
					}
					break;

				case 0xe:
					while(((count & ~0x7) != 0) && ((x + 8) < width))
					{
						for(int i = 0; i < 8; i++)
						{
							pixel[line + x] = 0x00;
							count--;
							x++;
						}
					}
					while((count > 0) && (x < width))
					{
						pixel[line + x] = 0x00;
						count--;
						x++;
					}

					break;
				default:
					throw new Exception(
							"Unimplemented decompress opcode " + opcode);// ;
				}
			}
		}
		return pixel;
	}
	
	public static final int[] convertToInt(byte[] data, int Bpp, int bpp)
	{
		int size = data.length / Bpp;
		int img[] = new int[size];
		int n = 0;
		for(int i=0; i < size; i++)
		{
			img[i] = cvalx(data, n, Bpp, bpp);
			n += Bpp;
		}
		return img;
	}
	
	static final int cvalx(byte[] data, int offset, int Bpp, int bpp) {
		int rv = 0;
		int red, green, blue, lower, full;
		switch(bpp)
		{
		case 15:
			lower = data[offset] & 0xFF;
			full = (data[offset + 1] & 0xFF) << 8 | lower;     

			red = (full >> 7) & 0xF8;
			red |= red >> 5;
			green = (full >> 2) & 0xF8;
			green |= green >> 5;
			blue = (lower << 3) & 0xFF;
			blue |= blue >> 5;

			return (0xff << 24) | (red << 16) | (green << 8) | blue;
		case 16:
			lower = data[offset] & 0xFF;
			full = (data[offset + 1] & 0xFF) << 8 | lower;          

			red = (full >> 8) & 0xF8;
			red |= red >> 5;
			green = (full >> 3) & 0xFC;
			green |= green >> 6;
			blue = (lower << 3) & 0xFF;
			blue |= blue >> 5;

			return (0xff << 24) | (red << 16) | (green << 8) | blue;
		case 24:
			return (0xff << 24) | (data[offset] << 16) | (data[offset + 1] << 8) | data[offset + 2];
		case 32:
			return (data[offset] << 24) | (data[offset + 1] << 16) | (data[offset + 2] << 8) | data[offset + 3]; 
		default:
			for (int i = (Bpp - 1); i >= 0; i--) {
				rv = rv << 8;
				rv |= data[offset + i] & 0xFF;
			}
		}

		return rv;
	}
}
