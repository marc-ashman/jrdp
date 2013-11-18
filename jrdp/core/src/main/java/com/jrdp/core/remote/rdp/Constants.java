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

package com.jrdp.core.remote.rdp;

public class Constants
{
	public static final int ENCRYPTION_NONE = 0x00000000;
	public static final int ENCRYPTION_40BIT = 0x00000001;
	public static final int ENCRYPTION_128BIT = 0x00000002;
	public static final int ENCRYPTION_56BIT = 0x00000008;
	public static final int ENCRYPTION_FIPS = 0x00000010;
	public static final byte CONNECTION_TYPE_MODEM = 0x01;
	public static final byte CONNECTION_TYPE_BROADBAND_LOW = 0x02;
	public static final byte CONNECTION_TYPE_SATELLITE = 0x03;
	public static final byte CONNECTION_TYPE_BROADBAND_HIGH = 0x04;
	public static final byte CONNECTION_TYPE_WAN = 0x05;
	public static final byte CONNECTION_TYPE_LAN = 0x06;
	public static final int MD5_DIGEST_LENGTH = 16;
	public static final int SHA1_DIGEST_LENGTH = 20;
	
	public static final byte[] pad36 = { 	0x36, 0x36, 0x36, 0x36, 0x36, 0x36, 0x36, 0x36, 0x36, 0x36, 
											0x36, 0x36, 0x36, 0x36, 0x36, 0x36, 0x36, 0x36, 0x36, 0x36,
											0x36, 0x36, 0x36, 0x36, 0x36, 0x36, 0x36, 0x36, 0x36, 0x36, 
											0x36, 0x36, 0x36, 0x36, 0x36, 0x36, 0x36, 0x36, 0x36, 0x36 };
	public static final byte[] pad5c = {	0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c,
											0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c,
											0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c,
											0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c,
											0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c, 0x5c };
	
	static final String PRODUCT_NAME = "Ashman RDP";
	static final String PRODUCT_ID = "702568-373573-6363-436739009";
	static final int CLIENT_BUILD = 1;
}
