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

public class KeyMap
{
	public static final short ENTER = 0x1c;
	public static final short BACKSPACE = 0x0e;
	public static final short ESC = 0x01;
	
	byte[][] map = {
	        {32, 0x39, 0},  //space
	        {33, 0x02, 1}, //!
	        {34, 0x28, 1}, //"
	        {35, 0x04, 1}, //#
	        {36, 0x05, 1}, //$
	        {37, 0x06, 1}, //%
	        {38, 0x08, 1}, //&
	        {39, 0x28, 0}, //'
	        {40, 0x0a, 1}, //(
	        {41, 0x0b, 1}, //)
	        {42, 0x09, 1}, //*
	        {43, 0x0d, 1}, //+
	        {44, 0x33, 0}, //,
	        {45, 0x0c, 0}, //-
	        {46, 0x34, 0}, //.
	        {47, 0x35, 0}, // /
	        {48, 0x0b, 0}, //0
	        {49, 0x02, 0}, //1
	        {50, 0x03, 0}, //2
	        {51, 0x04, 0}, //3
	        {52, 0x05, 0}, //4
	        {53, 0x06, 0}, //5
	        {54, 0x07, 0}, //6
	        {55, 0x08, 0}, //7
	        {56, 0x09, 0}, //8
	        {57, 0x0a, 0}, //9
	        {58, 0x27, 1}, //:
	        {59, 0x27, 0}, //;
	        {60, 0x33, 1}, //<
	        {61, 0x0d, 0}, //=
	        {62, 0x34, 1}, //>
	        {63, 0x35, 1}, //?
	        {64, 0x03, 1}, //@
	        {65, 0x1e, 1}, //A
	        {66, 0x30, 1}, //B
	        {67, 0x2e, 1}, //C
	        {68, 0x20, 1}, //D
	        {69, 0x12, 1}, //E
	        {70, 0x21, 1}, //F
	        {71, 0x22, 1}, //G
	        {72, 0x23, 1}, //H
	        {73, 0x17, 1}, //I
	        {74, 0x24, 1}, //J
	        {75, 0x25, 1}, //K
	        {76, 0x26, 1}, //L
	        {77, 0x32, 1}, //M
	        {78, 0x31, 1}, //N
	        {79, 0x18, 1}, //O
	        {80, 0x19, 1}, //P
	        {81, 0x10, 1}, //Q
	        {82, 0x13, 1}, //R
	        {83, 0x1f, 1}, //S
	        {84, 0x14, 1}, //T
	        {85, 0x16, 1}, //U
	        {86, 0x2f, 1}, //V
	        {87, 0x11, 1}, //W
	        {88, 0x2d, 1}, //X
	        {89, 0x15, 1}, //Y
	        {90, 0x2c, 1}, //Z
	        {91, 0x1a, 0}, //[
	        {92, 0x2b, 0}, //\
	        {93, 0x1b, 0}, //]
	        {94, 0x07, 1}, //^
	        {95, 0x0c, 1}, //_
	        {96, 0x29, 0}, //`
	        {97, 0x1e, 0}, //a
	        {98, 0x30, 0}, //b
	        {99, 0x2e, 0}, //c
	        {100, 0x20, 0}, //d
	        {101, 0x12, 0}, //e
	        {102, 0x21, 0}, //f
	        {103, 0x22, 0}, //g
	        {104, 0x23, 0}, //h
	        {105, 0x17, 0}, //i
	        {106, 0x24, 0}, //j
	        {107, 0x25, 0}, //k
	        {108, 0x26, 0}, //l
	        {109, 0x32, 0}, //m
	        {110, 0x31, 0}, //n
	        {111, 0x18, 0}, //o
	        {112, 0x19, 0}, //p
	        {113, 0x10, 0}, //q
	        {114, 0x13, 0}, //r
	        {115, 0x1f, 0}, //s
	        {116, 0x14, 0}, //t
	        {117, 0x16, 0}, //u
	        {118, 0x2f, 0}, //v
	        {119, 0x11, 0}, //w
	        {120, 0x2d, 0}, //x
	        {121, 0x15, 0}, //y
	        {122, 0x2c, 0}, //z
	        {123, 0x1a, 1}, //{
	        {124, 0x2b, 1}, //|
	        {125, 0x1b, 1}, //}
	        {126, 0x29, 1}, //~
	        {127, (byte) 0xd3, 0}, //DEL
	    };
}
