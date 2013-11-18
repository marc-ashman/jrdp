package com.jrdp.core.remote.rdp;

import com.jrdp.core.util.BitManip;
import com.jrdp.core.util.InputByteStream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Vector;

public class NetworkManager
{
	private DataInputStream in;
	private DataOutputStream out;
	private Vector<NetworkManagerListener> listeners;
	private RdpServerInfo server;
	private Socket socket;
	
	public NetworkManager(String server, int port)
	{
		this.server = new RdpServerInfo(server, port);
	}
	
	public void connect() throws Exception
	{
		socket = new Socket(server.getServer(), server.getPort());
		in = new DataInputStream(socket.getInputStream());
		out = new DataOutputStream(socket.getOutputStream());
	}
	
	public synchronized InputByteStream getPacket()
	{
		byte[] data;
		try
		{
			byte[] packetStart = new byte[4];
			int read = 0;
			while(read != 4 && read != -1)
				read  += in.read(packetStart, 0, 4);
			if(read == -1)
			{
				return null;
			}
			short length = 0;
			if(packetStart[0] == 0x03)			//Slow path packet
			{
				length = BitManip.mergeToShort(packetStart[2], packetStart[3]);
			}
			else								//Fast path packet
			{
				if((packetStart[1] & 0x80) == 0x80)
				{
					length = BitManip.mergeToShort((byte) (packetStart[1] & 0x7f), packetStart[2]);
				}
				else
				{
					length = packetStart[1];
				}
			}
			
			data = new byte[length];
			System.arraycopy(packetStart, 0, data, 0, 4);
			int left = length - read;
			while(left != 0)
			{
				read += in.read(data, read, left);
				left = length - read;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
		
		InputByteStream stream = new InputByteStream(data);
		return stream;
	}
	
	public boolean send(RdpPacket packet)
	{
		byte[] data = packet.finalizePacket();
		try {
			out.write(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	private void notifyOfException(Exception e, int which)
	{
		int size = 0;
		for(int i=0; i < size; i++)
		{
			listeners.elementAt(i).onException(e, which);
		}
	}
	
	public RdpServerInfo getServerInfo()
	{
		return server;
	}
	
	public int available()
	{
		try {
			return in.available();
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}
}
