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

package com.jrdp.client.awt;

import com.jrdp.core.remote.Canvas;
import com.jrdp.core.remote.rdp.Cursor;
import com.jrdp.core.remote.rdp.KeyMap;
import com.jrdp.core.remote.rdp.NetworkManager;
import com.jrdp.core.remote.rdp.Rdp;
import com.jrdp.core.remote.rdp.RdpConnectionInfo;
import com.jrdp.core.util.Logger;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class AwtClient implements KeyListener, MouseListener
{
	private Rdp client;
	private JFrame frame;
	private BitmapPanel panel;

	public AwtClient()
	{
		RdpConnectionInfo info = new RdpConnectionInfo("", "", "", "192.168.1.192", -5, 0,
                (short) 800, (short) 600, (short) 16, Rdp.CONNECTION_TYPE_BROADBAND_LOW, false);
		panel = new BitmapPanel();
        client = new Rdp(new NetworkManager("192.168.0.192", 3389), info, panel);

        frame = new JFrame();
        frame.addKeyListener(this);
        frame.addMouseListener(this);
        frame.setTitle("JRDP Awt Client");
        frame.setBounds(100, 100, 816, 638);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.add(panel);

        Logger.setLogger(new Logger.LoggerInterface() {
            @Override
            public void log(int type, String message) {
                System.out.println(message);
            }

            @Override
            public void log(String message) {
                System.out.println(message);
            }

            @Override
            public void log(Exception e) {
                e.printStackTrace();
            }

            @Override
            public void log(Exception e, String message) {
                System.out.println(message);
                e.printStackTrace();
            }
        });
	}

	public void start()
	{
		client.connect();
	}

	public static void main(String [] args)
	{
		AwtClient client = new AwtClient();
		client.start();
	}

	class BitmapPanel extends JPanel implements Canvas
	{
		private static final long serialVersionUID = -5427556578665059620L;
		private BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_USHORT_565_RGB);
        private Cursor cursor;
        private boolean isCursorVisible;
        private int cursorX;
        private int cursorY;

	    public void paintComponent(Graphics g) {
            Logger.log("paint()");
			g.drawImage(image, 0, 0, null);
			if(cursor != null && isCursorVisible)
			{
				BufferedImage cur = new BufferedImage(cursor.getWidth(), cursor.getHeight(),
						BufferedImage.TYPE_INT_ARGB);
				cur.setRGB(0, 0, cursor.getWidth(), cursor.getWidth(), cursor.getCursorBitmap(),
						0, cursor.getWidth());
				g.drawImage(cur, cursorX, cursorY, null);
			}
	    }

		@Override
		public void setCanvas(int[] img, int width, int height, int x, int y,
				int clippingWidth, int clippingHeight) {
            Logger.log("setCanvas(" + img.length + ", " + width + ", " + height + ")");
			image.setRGB(x, y, clippingWidth, clippingHeight, img, 0, width);
            repaint();
		}

        @Override
        public void setCanvasBottomUp(int[] img, int width, int height, int x,
                                      int y, int clippingWidth, int clippingHeight) {
            Logger.log("setCanvas(" + img.length + ", " + width + ", " + height + ")");
            int offset = width * (clippingHeight - 1);
            image.setRGB(x, y, clippingWidth, clippingHeight, img, offset, -width);
            repaint();
        }

        @Override
        public void cursorPositionChanged(int x, int y) {
            cursorX = x;
            cursorY = x;
            repaint();
        }

        @Override
        public void setCursor(Cursor cursor) {
            this.cursor = cursor;
            repaint();
        }

        @Override
        public void hideCursor() {
            isCursorVisible = false;
            repaint();
        }

        @Override
        public void showCursor() {
            isCursorVisible = true;
            repaint();
        }

        @Override
        public int getCursorX() {
            return cursorX;
        }

        @Override
        public int getCursorY() {
            return cursorY;
        }
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		int code = e.getKeyCode();
		switch(code)
		{
		case KeyEvent.VK_DOWN:
			client.moveMouse((short) 0, (short) -10);
			break;
		case KeyEvent.VK_UP:
			client.moveMouse((short) 0, (short) 10);
			break;
		case KeyEvent.VK_RIGHT:
			client.moveMouse((short) 10, (short) 0);
			break;
		case KeyEvent.VK_LEFT:
			client.moveMouse((short) -10, (short) 0);
			break;
		case KeyEvent.VK_ENTER:
			client.sendSpecialKeyboardKey(KeyMap.ENTER);
			break;
		default:
			//client.sendKeyDown(new String(e.getKeyChar() + ""));
			char key = e.getKeyChar();
			if(key != KeyEvent.CHAR_UNDEFINED)
				client.sendKeyboardKey(key);
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		int code = e.getKeyCode();

		switch(code)
		{
		case KeyEvent.VK_DOWN:
		case KeyEvent.VK_UP:
		case KeyEvent.VK_RIGHT:
		case KeyEvent.VK_LEFT:
			break;
		case KeyEvent.VK_ENTER:
			//client.sendSpecialKey(KeyMap.ENTER);
		default:
			//client.sendKeyUp(new String(e.getKeyChar() + ""));
			break;
		}
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
		//client.sendKeyDown(e.getKeyChar());
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		client.setMouse((short) (e.getX() - 16), (short) (e.getY() - 38));
		client.clickMouseLeft();
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}
}
