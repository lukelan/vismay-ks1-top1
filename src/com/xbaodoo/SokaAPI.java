package com.xbaodoo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;

public class SokaAPI {
	private String m_serverAddr;
	private int m_port;
	private int m_dbSize;
	private int m_checksum;
	private String m_dbSizeStr;
	
	public static final String PS_INFO = "PS00000004INFO";
	public static final String PS_LIST = "PS00000012LIST00000000";
	public static final String PS_PICT = "PS0000000APICT";
	public static final String PS_RESV = "PS0000000ARESV";
	public static final String PS_PL   = "PS00000004PLAY";
	public static final String PS_1STR = "PS0000000A1STR";
	public static final String PS_PCAN = "PS0000000APCAN";
	public static final String PS_P1ST = "PS0000000AP1ST";
	public static final String PS_REMO = "PS00000008REMO";
	public static final String PS_CPSG = "PS00000004CPSG";
	
	// IMG
	public static final String PS_ACLR = "PS00000004ACLR";
	public static final String PS_AIMG = "PS00000014AIMG";
	public static final String PS_AIME = "PS00000006AIME";
	public static final String PS_ARAW = "PS00000004ARAW";
	
	// Chatting
	public static final String PS_HCLR = "PS00000004HCLR";
	public static final String PS_HIMG = "PS00000011HIMG";
	public static final String PS_HIME = "PS00000006HIME";
	public static final String PS_HRAW = "PS00000004HRAW";
	
	// Wait time between packet sent
	public static final int WAIT_TIME = 100;
	
	public SokaAPI() {
		m_serverAddr = "";
		m_port = 0;
	}
	
	public SokaAPI(String addr, int port) {
		m_serverAddr = addr;
		m_port = port;
		
		System.out.println("Server addr: " + m_serverAddr);
		System.out.println("Server port: " + m_port);
		
		if (m_serverAddr == "" || m_port == 0 ) {
			System.out.println("Incorrect addr or port");
			return;
		}
	}
	
	/* Get info */	
	public void getInfo() throws IOException {
		
		Socket clientSock = null;
		
		try {			
			clientSock = new Socket(m_serverAddr, m_port);
		} catch (UnknownHostException e) {
			System.out.println("getInfo(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("getInfo(): IOException");
			e.printStackTrace();
		}
		
		DataInputStream inStream = getDataFromSOKA(PS_INFO,clientSock);
		
		// Read direction
		String direction = readDirection(inStream);
		
		if (direction != null)
		{
			System.out.println("Direction: " + direction);
		}
		
		// Read No 8 byte
		int nobyte = readNoByte(inStream);
		
		if (nobyte != 0)
		{
			System.out.println("No byte: " + nobyte);
		}
		
		// Get payload
		int size = nobyte - 4;
		byte[] payload = new byte[size];
		
		payload = readPayload(inStream, size);
		
		// Read DB size
		byte[] sizedb = new byte[8];
		for (int i=0; i<8; i++)
		{
			sizedb[i] = payload[i+32];
		}
		
		m_dbSizeStr = new String(sizedb);
		m_dbSize = Integer.parseInt(m_dbSizeStr,16) - 1;
		m_dbSizeStr = Integer.toHexString(m_dbSize);
		
		// Padding to 6 bytes
		int temp = m_dbSizeStr.length();
		
		if (temp<6)
		{
			for (int i=0; i<(6-temp); i++)
			{
				m_dbSizeStr = '0' + m_dbSizeStr;
			}
		}
		
		// Read checksum
		byte[] checksum = new byte[8];
		for (int i=0; i<8; i++)
		{
			checksum[i] = payload[i+40];				
		}
		
		m_checksum = Integer.parseInt(new String(checksum),16);
		
		System.out.println("Db size: " + m_dbSize);
		System.out.println("Checksum: " + m_checksum);
		
		try {
			clientSock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void getSongList() throws IOException {
		if (m_dbSizeStr == null) return;
		
		String command = PS_LIST + m_dbSizeStr;
		
		Socket clientSock = null;
		
		try {			
			clientSock = new Socket(m_serverAddr, m_port);
		} catch (UnknownHostException e) {
			System.out.println("getSongList(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("getSongList(): IOException");
			e.printStackTrace();
		}
		
		DataInputStream inStream = getDataFromSOKA(command, clientSock);
		
		// Read direction
		String direction = readDirection(inStream);
		
		if (direction != null)
		{
			System.out.println("Direction: " + direction);
		}
		
		// Read No 8 byte
		int nobyte = readNoByte(inStream);
		
		if (nobyte != 0)
		{
			System.out.println("No byte: " + nobyte);
		}
		
		// Get payload
		int size = nobyte - 4;
		byte[] payload = new byte[size];
		
		payload = readPayload(inStream, size);
		
		// Read Offset
		byte[] s = new byte[8];
		for (int i=0; i<8; i++)
		{
			s[i] = payload[i];
		}
		
		int offset = Integer.parseInt(new String(s),16);
		
		System.out.println("Offset: " + offset);
		
		// Read size
		byte[] sz = new byte[6];
		for (int i=0; i<6; i++)
		{
			sz[i] = payload[i+8];
		}
		
		int dbsize = Integer.parseInt(new String(sz),16);
		
		// Write db file
		DataOutputStream outputFile = new DataOutputStream(new FileOutputStream("sokadb.db3"));
		
		for (int i=0; i<dbsize+1; i++)
		{
			outputFile.writeByte(payload[i+14]);
		}
		
		outputFile.close();
		
		try {
			clientSock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<Integer> getPlaylist() throws IOException {
		Socket clientSock = null;
		ArrayList<Integer> arrList = new ArrayList<Integer>();
		
		try {			
			clientSock = new Socket(m_serverAddr, m_port);
		} catch (UnknownHostException e) {
			System.out.println("getPlaylist(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("getPlaylist(): IOException");
			e.printStackTrace();
		}
		
		DataInputStream inStream = getDataFromSOKA(PS_PL, clientSock);
		
		// Read direction
		String direction = readDirection(inStream);
		
		if (direction != null)
		{
			System.out.println("Direction: " + direction);
		}
		
		// Read No 8 byte
		int nobyte = readNoByte(inStream);
		
		if (nobyte != 0)
		{
			System.out.println("No byte: " + nobyte);
		}
		
		// Get payload
		int size = nobyte - 4;
		byte[] payload = new byte[size];
		
		payload = readPayload(inStream, size);
		
		// Read COUNT
		byte[] s = new byte[4];
		for (int i=0; i<4; i++)
		{
			s[i] = payload[i];
		}
		
		int count = Integer.parseInt(new String(s),16);
		
		for (int i=0; i<count; i++)
		{
			byte[] songNumStr = new byte[6];
			for (int j=0; j<6; j++)
			{
				songNumStr[j] = payload[i*6 + j + 4];
			}
			
			int songNum = Integer.parseInt(new String(songNumStr),16);
			
			arrList.add(songNum);
		}
		
		try {
			clientSock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return arrList;
	}
	
	public void getSingerPic(int picNum) throws IOException {
		Socket clientSock = null;
				
		try {			
			clientSock = new Socket(m_serverAddr, m_port);
		} catch (UnknownHostException e) {
			System.out.println("createSocket(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("createSocket(): IOException");
			e.printStackTrace();
		}
		
		String hex = Integer.toHexString(picNum);
		
		int temp = hex.length();
		
		if (temp<6)
		{
			for (int i=0; i<(6-temp); i++)
			{
				hex = '0' + hex;
			}
		}	
		
		DataInputStream inStream = getDataFromSOKA(PS_PICT + hex, clientSock);
		
		// Read direction
		String direction = readDirection(inStream);
		
		if (direction != null)
		{
			System.out.println("Direction: " + direction);
		}
		
		// Read No 8 byte
		int nobyte = readNoByte(inStream);
		
		if (nobyte != 0)
		{
			System.out.println("No byte: " + nobyte);
		}
		
		// Get payload
		int size = nobyte - 4;
		byte[] payload = new byte[size];
		
		payload = readPayload(inStream, size);
		
		// Read COUNT
		byte[] s = new byte[6];
		for (int i=0; i<6; i++)
		{
			s[i] = payload[i];
		}
		
		int picSize = Integer.parseInt(new String(s),16);
		
		System.out.println("picSize: " + picSize);
		
		// Write db file
		DataOutputStream outputFile = new DataOutputStream(new FileOutputStream(hex + ".jpg"));
		
		for (int i=0; i<picSize; i++)
		{
			outputFile.writeByte(payload[i+6]);
		}
		
		outputFile.close();
		
		try {
			clientSock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Remote Control */
	public void reserve(int songNum) {
		Socket clientSock = null;
		
		try {			
			clientSock = new Socket(m_serverAddr, m_port);
		} catch (UnknownHostException e) {
			System.out.println("reserve(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("reserve(): IOException");
			e.printStackTrace();
		}
		
		String hex = Integer.toHexString(songNum);
		
		int temp = hex.length();
		
		if (temp<6)
		{
			for (int i=0; i<(6-temp); i++)
			{
				hex = '0' + hex;
			}
		}	
		
		sendDataToSOKA(PS_RESV + hex, clientSock);
		
		try {
			clientSock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void firstReserve(int songNum) {
		Socket clientSock = null;
		
		try {			
			clientSock = new Socket(m_serverAddr, m_port);
		} catch (UnknownHostException e) {
			System.out.println("createSocket(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("createSocket(): IOException");
			e.printStackTrace();
		}
		
		String hex = Integer.toHexString(songNum);
		
		int temp = hex.length();
		
		if (temp<6)
		{
			for (int i=0; i<(6-temp); i++)
			{
				hex = '0' + hex;
			}
		}	
		
		sendDataToSOKA(PS_1STR + hex, clientSock);
		
		try {
			clientSock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void plCancel(int songNum) {
		Socket clientSock = null;
		
		try {			
			clientSock = new Socket(m_serverAddr, m_port);
		} catch (UnknownHostException e) {
			System.out.println("plCancel(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("plCancel(): IOException");
			e.printStackTrace();
		}
		
		String hex = Integer.toHexString(songNum);
		
		int temp = hex.length();
		
		if (temp<6)
		{
			for (int i=0; i<(6-temp); i++)
			{
				hex = '0' + hex;
			}
		}	
		
		sendDataToSOKA(PS_PCAN + hex, clientSock);
		
		try {
			clientSock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void plFirstReserve(int songNum) {
		Socket clientSock = null;
		
		try {			
			clientSock = new Socket(m_serverAddr, m_port);
		} catch (UnknownHostException e) {
			System.out.println("plFirstReserve(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("plFirstReserve(): IOException");
			e.printStackTrace();
		}
		
		String hex = Integer.toHexString(songNum);
		
		int temp = hex.length();
		
		if (temp<6)
		{
			for (int i=0; i<(6-temp); i++)
			{
				hex = '0' + hex;
			}
		}	
		
		sendDataToSOKA(PS_P1ST + hex, clientSock);
		
		try {
			clientSock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int getCurrentSongNumber() throws IOException {
		int curSong = 0;
		
		Socket clientSock = null;
		DataOutputStream outStream = null;
		DataInputStream inStream = null;
		
		try {			
			clientSock = new Socket(m_serverAddr, m_port);
			outStream = new DataOutputStream(clientSock.getOutputStream());
			inStream = new DataInputStream(clientSock.getInputStream());
		} catch (UnknownHostException e) {
			System.out.println("drawImageText(): UnknownHostException");
			e.printStackTrace();
			return 0;
		} catch (IOException e) {
			System.out.println("drawImageText(): IOException");
			e.printStackTrace();
			return 0;
		}
		
		try {
			outStream.writeBytes(PS_CPSG);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			clientSock.close();
			return 0;
		}
						
		soka_wait(WAIT_TIME);
		
		int available = inStream.available();
		while (available == 0)
		{
			//System.out.println("Instream not available yet " + available);
			available = inStream.available();
		}
		
		System.out.println("getCurrentSongNumber Available = " + available);
		
		byte[] byteArr = new byte[available];
		
		int count;
		count = readBufferToByteArray(byteArr, inStream, 0, available);
		
		if (count > 0)
		{
			String direction = readFieldString(byteArr,	0, 2);
			
			if (!direction.equalsIgnoreCase("SP"))
			{
				clientSock.close();
				return 0;
			}
			
			// Get return code and command
			String command = readFieldString(byteArr, 10, 4);
						
			if(command.equalsIgnoreCase("CPSG"))
			{
				String songNumStr = readFieldString(byteArr, 14, 6);
				curSong = Integer.parseInt(songNumStr,16);;
			}	
		}
		
		try {
			clientSock.close();
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
		
		return curSong;
	}
	
	public void remo(String keyName) throws IOException {
		Socket clientSock = null;
		DataOutputStream outStream = null;
		
		try {			
			clientSock = new Socket(m_serverAddr, m_port);
			outStream = new DataOutputStream(clientSock.getOutputStream());
		} catch (UnknownHostException e) {
			System.out.println("remo(): UnknownHostException");
			e.printStackTrace();
			return;
		} catch (IOException e) {
			System.out.println("remo(): IOException");
			e.printStackTrace();
			return;
		}
		
		//sendDataToSOKA(PS_REMO + keyName, clientSock);
		String commandStr = PS_REMO + keyName;
		commandStr = commandStr.toUpperCase();
		
		outStream.writeBytes(commandStr);
		
		soka_wait(WAIT_TIME);
		
		try {
			clientSock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Draw Image + Text */
	public boolean drawImageText(int xImage, int yImage, String imageFile, int xText, int yText, int width, int height, String alignment, int r, int g, int b, String text) throws IOException {
		boolean ret = false;
		
		Socket clientSock = null;
		DataOutputStream outStream = null;
		DataInputStream inStream = null;
		
		try {			
			clientSock = new Socket(m_serverAddr, m_port);
			outStream = new DataOutputStream(clientSock.getOutputStream());
			inStream = new DataInputStream(clientSock.getInputStream());
		} catch (UnknownHostException e) {
			System.out.println("drawImageText(): UnknownHostException");
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			System.out.println("drawImageText(): IOException");
			e.printStackTrace();
			return false;
		}
		
		// Clear screen
		ret = clearScreen(inStream, outStream);
		
		soka_wait(WAIT_TIME);
		
		if (ret == false) 
		{
			try {
				clientSock.close();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return false;
		} 
		
		// Draw image
		if (imageFile != null)
		{
			// Send image data
			FileInputStream fis = null;
			DataInputStream inputFile = null;
			
			// Read image file
			try {
				fis = new FileInputStream(imageFile);
				inputFile = new DataInputStream(fis);
				
				int fileSize = (int) fis.getChannel().size();
							
				ret = setImagePosition(0, xImage, yImage, fileSize, inStream, outStream);
				
				if (ret == false) 
				{
					try {
						fis.close();
						inputFile.close();
						clientSock.close();
					} catch (IOException e) {
						e.printStackTrace();
						return false;
					}
					return false;
				} 
				
				soka_wait(WAIT_TIME);
				
				byte[] imageData = new byte[0x10000];
				int offset = 0;
				int byteRead = inputFile.read(imageData, 0, 0x10000);
				
				while (byteRead == 0x10000) {
					ret = sendImageData(0, offset, byteRead-1, imageData, inStream, outStream);
					
					if (ret == false) 
					{
						try {
							fis.close();
							inputFile.close();
							clientSock.close();
						} catch (IOException e) {
							e.printStackTrace();
							return false;
						}
						
						return false;
					} 
					
					soka_wait(WAIT_TIME);
					
					offset += byteRead;
					byteRead = inputFile.read(imageData, 0, 0x10000);
				}
				
				if (byteRead > 0)
				{
					ret = sendImageData(0, offset, byteRead-1, imageData, inStream, outStream);
					if (ret == false) 
					{
						try {
							inputFile.close();
							clientSock.close();
						} catch (IOException e) {
							e.printStackTrace();
							return false;
						}
						return false;
					}
					
					soka_wait(WAIT_TIME);
				}
				
				ret = endImageData(0, inStream, outStream);
				
				if (ret == false) 
				{
					try {
						inputFile.close();
						clientSock.close();
					} catch (IOException e) {
						e.printStackTrace();
						return false;
					}
					return false;
				}
				
				soka_wait(WAIT_TIME);
				
				fis.close();
				inputFile.close();
									
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		// Draw text
		if (text != null)
		{
			byte[] utf = text.getBytes("UTF-8");
			
			int packetSize = 4 + 19 + 3 + utf.length;
			String packetSizeStr = Integer.toHexString(packetSize);
			
			int temp = packetSizeStr.length();
			
			if (temp<8)
			{
				for (int i=0; i<(8-temp); i++)
				{
					packetSizeStr = '0' + packetSizeStr;
				}
			} 
			
			// Convert x, y, width, height to 3 bytes hexadecimal string
			String xStr = Integer.toHexString(xText);
			String yStr = Integer.toHexString(yText);
			String widthStr = Integer.toHexString(width);
			String heightStr = Integer.toHexString(height);
			
			temp = xStr.length();
			
			if (temp<3)
			{
				for (int i=0; i<(3-temp); i++)
				{
					xStr = '0' + xStr;
				}
			}
			
			temp = yStr.length();
			
			if (temp<3)
			{
				for (int i=0; i<(3-temp); i++)
				{
					yStr = '0' + yStr;
				}
			}
			
			temp = widthStr.length();
			
			if (temp<3)
			{
				for (int i=0; i<(3-temp); i++)
				{
					widthStr = '0' + widthStr;
				}
			}
			
			temp = heightStr.length();
			
			if (temp<3)
			{
				for (int i=0; i<(3-temp); i++)
				{
					heightStr = '0' + heightStr;
				}
			}
			
			// Convert R,G,B to 2 bytes hexadecimal string
			String rStr = Integer.toHexString(r);
			String gStr = Integer.toHexString(g);
			String bStr = Integer.toHexString(b);
			
			temp = rStr.length();
			
			if (temp<2)
			{
				for (int i=0; i<(2-temp); i++)
				{
					rStr = '0' + rStr;
				}
			}
			
			temp = gStr.length();
			
			if (temp<2)
			{
				for (int i=0; i<(2-temp); i++)
				{
					gStr = '0' + gStr;
				}
			}
			
			temp = bStr.length();
			
			if (temp<2)
			{
				for (int i=0; i<(2-temp); i++)
				{
					bStr = '0' + bStr;
				}
			}
			
			
			String sizeStr = Integer.toHexString(utf.length);
			
			temp = sizeStr.length();
			
			if (temp<3)
			{
				for (int i=0; i<(3-temp); i++)
				{
					sizeStr = '0' + sizeStr;
				}
			}
	
			String sendString = "PS" + packetSizeStr + "ASTR" + xStr + yStr + widthStr + heightStr + alignment + rStr + gStr + bStr + sizeStr;
			sendString = sendString.toUpperCase();
			
			outStream.writeBytes(sendString);
			outStream.write(utf);
			
			int available = inStream.available();
			
			while (available == 0)
			{
				//System.out.println("Instream not available yet " + available);
				available = inStream.available();
			}
			
			System.out.println("Send text data Available = " + available);
			
			byte[] byteArr = new byte[available];
			
			int count = readBufferToByteArray(byteArr, inStream, 0, available);
			
			if (count > 0)
			{
				String direction = readFieldString(byteArr,	0, 2);
				
				if (!direction.equalsIgnoreCase("SP"))
				{
					try {
						clientSock.close();
					} catch (IOException e) {
						e.printStackTrace();
						return false;
					}
					return false;
				}
				
				// Get return code and command
				String errCode = readFieldString(byteArr, 10, 4);
				String command = readFieldString(byteArr, 14, 4);
				
				if(command.equalsIgnoreCase("ASTR"))
				{
					if (!errCode.equalsIgnoreCase("AOK!"))
					{
						try {
							clientSock.close();
						} catch (IOException e) {
							e.printStackTrace();
							return false;
						}
						return false;
					}
				}
				else
				{
					try {
						clientSock.close();
					} catch (IOException e) {
						e.printStackTrace();
						return false;
					}
					return false;
				}
			}
			
			soka_wait(WAIT_TIME);
		}
		
		// Draw screen
		ret = drawScreen(inStream, outStream);
		
		if (ret == false) 
		{
			try {
				clientSock.close();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return false;
		} 
		
		try {
			clientSock.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public boolean clearScreen(DataInputStream inStream, DataOutputStream outStream) throws IOException {
		boolean result = false;
		
		//outStream.print(PS_ACLR);
		//outStream.flush();
		outStream.writeBytes(PS_ACLR);
		
		int available = inStream.available();
		while (available == 0)
		{
			//System.out.println("Instream not available yet " + available);
			available = inStream.available();
		}
		
		System.out.println("Clearscreen Available = " + available);
		
		byte[] byteArr = new byte[available];
		
		int count = readBufferToByteArray(byteArr, inStream, 0, available);
		
		if (count > 0)
		{
			String direction = readFieldString(byteArr,	0, 2);
			
			if (!direction.equalsIgnoreCase("SP"))
			{
				result = false;
				return result;
			}
			
			// Get return code and command
			String errCode = readFieldString(byteArr, 10, 4);
			String command = readFieldString(byteArr, 14, 4);
			
			if(command.equalsIgnoreCase("ACLR"))
			{
				if (!errCode.equalsIgnoreCase("AOK!"))
				{
					result = false;
					return result;
				}
				
				result = true;
			}
			else
			{
				result = false;
				return result;
			}			
		}
	
		return result;
	}
	
	public boolean setImagePosition(int imgNo, int x, int y, int size, DataInputStream inStream, DataOutputStream outStream) throws IOException {
		
		boolean result = false;
		
		// Convert image number to 2 bytes hexadecimal string
		String imgNoStr = Integer.toHexString(imgNo);
		
		int temp = imgNoStr.length();
		
		if (temp<2)
		{
			for (int i=0; i<(2-temp); i++)
			{
				imgNoStr = '0' + imgNoStr;
			}
		}
		
		// Convert y and y to 3 bytes hexadecimal string
		String xStr = Integer.toHexString(x);
		String yStr = Integer.toHexString(y);
		
		temp = xStr.length();
		
		if (temp<3)
		{
			for (int i=0; i<(3-temp); i++)
			{
				xStr = '0' + xStr;
			}
		}
		
		temp = yStr.length();
		
		if (temp<3)
		{
			for (int i=0; i<(3-temp); i++)
			{
				yStr = '0' + yStr;
			}
		}
		
		// Convert size to 8 bytes hexadecimal string
		String sizeStr = Integer.toHexString(size);
		
		temp = sizeStr.length();
		
		if (temp<8)
		{
			for (int i=0; i<(8-temp); i++)
			{
				sizeStr = '0' + sizeStr;
			}
		}
		
		String commandStr = PS_AIMG + imgNoStr + xStr + yStr + sizeStr;
		commandStr = commandStr.toUpperCase();
		//outStream.print(commandStr);
		//outStream.flush();
		outStream.writeBytes(commandStr);
		
		// Check return packet
		int available = inStream.available();
		while (available == 0)
		{
			//System.out.println("Instream not available yet " + available);
			available = inStream.available();
		}
		
		System.out.println("SetPosition Available = " + available);
		
		byte[] byteArr = new byte[available];
		
		int count = readBufferToByteArray(byteArr, inStream, 0, available);
		
		if (count > 0)
		{
			String direction = readFieldString(byteArr,	0, 2);
			
			if (!direction.equalsIgnoreCase("SP"))
			{
				result = false;
				return result;
			}
			
			// Get return code and command
			String errCode = readFieldString(byteArr, 10, 4);
			String command = readFieldString(byteArr, 14, 4);
			
			if(command.equalsIgnoreCase("AIMG"))
			{
				if (!errCode.equalsIgnoreCase("AOK!"))
				{
					result = false;
					return result;
				}
				
				result = true;
			}
			else
			{
				result = false;
				return result;
			}			
		}
	
		return result;
	}
	
	public boolean sendImageData(int imgNo, int offset, int size, byte[] data, DataInputStream inStream, DataOutputStream outStream) throws IOException {
		boolean result = false;
		
		// Calculate No8 bytes from size
		int packetSize = (size+1) + 4 + 2 + 8 + 4;
		
		String packetSizeStr = Integer.toHexString(packetSize);
		
		int temp = packetSizeStr.length();
		
		if (temp<8)
		{
			for (int i=0; i<(8-temp); i++)
			{
				packetSizeStr = '0' + packetSizeStr;
			}
		}
		
		// Convert image number to 2 bytes hexadecimal string
		String imgNoStr = Integer.toHexString(imgNo);
		
		temp = imgNoStr.length();
		
		if (temp<2)
		{
			for (int i=0; i<(2-temp); i++)
			{
				imgNoStr = '0' + imgNoStr;
			}
		}
		
		// Convert offset to 8 bytes hexadecimal string		
		String offsetStr = Integer.toHexString(offset);
		
		temp = offsetStr.length();
		
		if (temp<8)
		{
			for (int i=0; i<(8-temp); i++)
			{
				offsetStr = '0' + offsetStr;
			}
		}
		
		// Convert size to 4 bytes hexadecimal string
		String sizeStr = Integer.toHexString(size);
		
		temp = sizeStr.length();
		
		if (temp<4)
		{
			for (int i=0; i<(4-temp); i++)
			{
				sizeStr = '0' + sizeStr;
			}
		}
		
		String commandStr = "PS" + packetSizeStr + "AIMD" + imgNoStr + offsetStr + sizeStr;
		commandStr = commandStr.toUpperCase();
		
		outStream.writeBytes(commandStr);
		outStream.write(data);
		
		// Check return packet
		int available = inStream.available();
		while (available == 0)
		{
			//System.out.println("Instream not available yet " + available);
			available = inStream.available();
		}
		
		System.out.println("SetImageData Available = " + available);
		
		byte[] byteArr = new byte[available];
		
		int count = readBufferToByteArray(byteArr, inStream, 0, available);
		
		if (count > 0)
		{
			String direction = readFieldString(byteArr,	0, 2);
			
			if (!direction.equalsIgnoreCase("SP"))
			{
				result = false;
				return result;
			}
			
			// Get return code and command
			String errCode = readFieldString(byteArr, 10, 4);
			String command = readFieldString(byteArr, 14, 4);
			
			if(command.equalsIgnoreCase("AIMD"))
			{
				if (!errCode.equalsIgnoreCase("AOK!"))
				{
					result = false;
					return result;
				}
				
				result = true;
			}
			else
			{
				result = false;
				return result;
			}			
		}
	
		return result;		
	}
	
	public boolean endImageData(int imgNo, DataInputStream inStream, DataOutputStream outStream) throws IOException {
		boolean result = false;
		
		// Convert image number to 2 bytes hexadecimal string
		String imgNoStr = Integer.toHexString(imgNo);
		
		int temp = imgNoStr.length();
		
		if (temp<2)
		{
			for (int i=0; i<(2-temp); i++)
			{
				imgNoStr = '0' + imgNoStr;
			}
		}
		
		String commandStr = PS_AIME + imgNoStr;
		commandStr = commandStr.toUpperCase();
		
		outStream.writeBytes(commandStr);
		
		// Check return packet
		int available = inStream.available();
		while (available == 0)
		{
			//System.out.println("Instream not available yet " + available);
			available = inStream.available();
		}
		
		System.out.println("EndImageData Available = " + available);
		
		byte[] byteArr = new byte[available];
		
		int count = readBufferToByteArray(byteArr, inStream, 0, available);
		
		if (count > 0)
		{
			String direction = readFieldString(byteArr,	0, 2);
			
			if (!direction.equalsIgnoreCase("SP"))
			{
				result = false;
				return result;
			}
			
			// Get return code and command
			String errCode = readFieldString(byteArr, 10, 4);
			String command = readFieldString(byteArr, 14, 4);
			
			if(command.equalsIgnoreCase("AIME"))
			{
				if (!errCode.equalsIgnoreCase("AOK!"))
				{
					result = false;
					return result;
				}
				
				result = true;
			}
			else
			{
				result = false;
				return result;
			}			
		}
	
		return result;
	}
	
	public boolean drawScreen(DataInputStream inStream, DataOutputStream outStream) throws IOException {
		boolean result = false;
		
		outStream.writeBytes(PS_ARAW);
		
		int available = inStream.available();
		while (available == 0)
		{
			//System.out.println("Instream not available yet " + available);
			available = inStream.available();
		}
		
		System.out.println("Draw screen Available = " + available);
		
		byte[] byteArr = new byte[available];
		
		int count = readBufferToByteArray(byteArr, inStream, 0, available);
		
		if (count > 0)
		{
			String direction = readFieldString(byteArr,	0, 2);
			
			if (!direction.equalsIgnoreCase("SP"))
			{
				result = false;
				return result;
			}
			
			// Get return code and command
			String errCode = readFieldString(byteArr, 10, 4);
			String command = readFieldString(byteArr, 14, 4);
			
			if(command.equalsIgnoreCase("ARAW"))
			{
				if (!errCode.equalsIgnoreCase("AOK!"))
				{
					result = false;
					return result;
				}
				
				result = true;
			}
			else
			{
				result = false;
				return result;
			}			
		}
	
		return result;
	}
	
	/* Draw Image + Text */
	public boolean drawChatImageText(int xImage, String imageFile, int xText, int r, int g, int b, String text) throws IOException {
		boolean ret = false;
		
		Socket clientSock = null;
		DataOutputStream outStream = null;
		DataInputStream inStream = null;
		
		try {			
			clientSock = new Socket(m_serverAddr, m_port);
			outStream = new DataOutputStream(clientSock.getOutputStream());
			inStream = new DataInputStream(clientSock.getInputStream());
		} catch (UnknownHostException e) {
			System.out.println("drawChatImageText(): UnknownHostException");
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			System.out.println("drawChatImageText(): IOException");
			e.printStackTrace();
			return false;
		}
		
		// Clear screen
		ret = clearChatScreen(inStream, outStream);
		
		if (ret == false) 
		{
			try {
				clientSock.close();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return false;
		} 
		
		soka_wait(WAIT_TIME);
		
		// Draw image
		if (imageFile != null)
		{
			// Send image data
			FileInputStream fis = null;
			DataInputStream inputFile = null;
			
			// Read image file
			try {
				fis = new FileInputStream(imageFile);
				inputFile = new DataInputStream(fis);
				
				int fileSize = (int) fis.getChannel().size();
							
				ret = setChatImagePosition(0, xImage, fileSize, inStream, outStream);
				
				if (ret == false) 
				{
					try {
						fis.close();
						inputFile.close();
						clientSock.close();
					} catch (IOException e) {
						e.printStackTrace();
						return false;
					}
					return false;
				} 
				
				soka_wait(WAIT_TIME);
				
				byte[] imageData = new byte[0x10000];
				int offset = 0;
				int byteRead = inputFile.read(imageData, 0, 0x10000);
				
				while (byteRead == 0x10000) {
					ret = sendChatImageData(0, offset, byteRead-1, imageData, inStream, outStream);
					
					if (ret == false) 
					{
						try {
							fis.close();
							inputFile.close();
							clientSock.close();
						} catch (IOException e) {
							e.printStackTrace();
							return false;
						}
						return false;
					}
					
					soka_wait(WAIT_TIME);
					
					offset += byteRead;
					byteRead = inputFile.read(imageData, 0, 0x10000);
				}
				
				if (byteRead > 0)
				{
					ret = sendChatImageData(0, offset, byteRead-1, imageData, inStream, outStream);
					if (ret == false) 
					{
						try {
							inputFile.close();
							clientSock.close();
						} catch (IOException e) {
							e.printStackTrace();
							return false;
						}
						return false;
					} 
					
					soka_wait(WAIT_TIME);
				}
				
				ret = endChatImageData(0, inStream, outStream);
				
				if (ret == false) 
				{
					try {
						inputFile.close();
						clientSock.close();
					} catch (IOException e) {
						e.printStackTrace();
						return false;
					}
					return false;
				} 
				
				soka_wait(WAIT_TIME);
				
				fis.close();
				inputFile.close();
									
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		// Draw text
		if (text != null)
		{
			byte[] utf = text.getBytes("UTF-8");
			
			int packetSize = 4 + 9 + 3 + utf.length;
			String packetSizeStr = Integer.toHexString(packetSize);
			
			int temp = packetSizeStr.length();
			
			if (temp<8)
			{
				for (int i=0; i<(8-temp); i++)
				{
					packetSizeStr = '0' + packetSizeStr;
				}
			} 
			
			// Convert x, y, width, height to 3 bytes hexadecimal string
			String xStr = Integer.toHexString(xText);
			
			temp = xStr.length();
			
			if (temp<3)
			{
				for (int i=0; i<(3-temp); i++)
				{
					xStr = '0' + xStr;
				}
			}
			
			// Convert R,G,B to 2 bytes hexadecimal string
			String rStr = Integer.toHexString(r);
			String gStr = Integer.toHexString(g);
			String bStr = Integer.toHexString(b);
			
			temp = rStr.length();
			
			if (temp<2)
			{
				for (int i=0; i<(2-temp); i++)
				{
					rStr = '0' + rStr;
				}
			}
			
			temp = gStr.length();
			
			if (temp<2)
			{
				for (int i=0; i<(2-temp); i++)
				{
					gStr = '0' + gStr;
				}
			}
			
			temp = bStr.length();
			
			if (temp<2)
			{
				for (int i=0; i<(2-temp); i++)
				{
					bStr = '0' + bStr;
				}
			}
			
			
			String sizeStr = Integer.toHexString(utf.length);
			
			temp = sizeStr.length();
			
			if (temp<3)
			{
				for (int i=0; i<(3-temp); i++)
				{
					sizeStr = '0' + sizeStr;
				}
			}
	
			String sendString = "PS" + packetSizeStr + "HSTR" + xStr + rStr + gStr + bStr + sizeStr;
			sendString = sendString.toUpperCase();
			
			outStream.writeBytes(sendString);
			outStream.write(utf);
			
			int available = inStream.available();
			
			while (available == 0)
			{
				//System.out.println("Instream not available yet " + available);
				available = inStream.available();
			}
			
			System.out.println("Send text data Available = " + available);
			
			byte[] byteArr = new byte[available];
			
			int count = readBufferToByteArray(byteArr, inStream, 0, available);
			
			if (count > 0)
			{
				String direction = readFieldString(byteArr,	0, 2);
				
				if (!direction.equalsIgnoreCase("SP"))
				{
					try {
						clientSock.close();
					} catch (IOException e) {
						e.printStackTrace();
						return false;
					}
					return false;
				}
				
				// Get return code and command
				String errCode = readFieldString(byteArr, 10, 4);
				String command = readFieldString(byteArr, 14, 4);
				
				if(command.equalsIgnoreCase("HSTR"))
				{
					if (!errCode.equalsIgnoreCase("HOK!"))
					{
						try {
							clientSock.close();
						} catch (IOException e) {
							e.printStackTrace();
							return false;
						}
						return false;
					}
				}
				else
				{
					try {
						clientSock.close();
					} catch (IOException e) {
						e.printStackTrace();
						return false;
					}
					return false;
				}
			}
			
			soka_wait(WAIT_TIME);
		}
		
		// Draw screen
		ret = drawChatScreen(inStream, outStream);
		
		if (ret == false) 
		{
			try {
				clientSock.close();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return false;
		} 
		
		try {
			clientSock.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}	
	
	public boolean clearChatScreen(DataInputStream inStream, DataOutputStream outStream) throws IOException {
		boolean result = false;

		outStream.writeBytes(PS_HCLR);
		
		int available = inStream.available();
		while (available == 0)
		{
			//System.out.println("Instream not available yet " + available);
			available = inStream.available();
		}
		
		System.out.println("clearChatScreen Available = " + available);
		
		byte[] byteArr = new byte[available];
		
		int count = readBufferToByteArray(byteArr, inStream, 0, available);
		
		if (count > 0)
		{
			String direction = readFieldString(byteArr,	0, 2);
			
			if (!direction.equalsIgnoreCase("SP"))
			{
				result = false;
				return result;
			}
			
			// Get return code and command
			String errCode = readFieldString(byteArr, 10, 4);
			String command = readFieldString(byteArr, 14, 4);
			
			if(command.equalsIgnoreCase("HCLR"))
			{
				if (!errCode.equalsIgnoreCase("HOK!"))
				{
					result = false;
					return result;
				}
				
				result = true;
			}
			else
			{
				result = false;
				return result;
			}			
		}
	
		return result;
	}
	
	public boolean setChatImagePosition(int imgNo, int x, int size, DataInputStream inStream, DataOutputStream outStream) throws IOException {
		
		boolean result = false;
		
		// Convert image number to 2 bytes hexadecimal string
		String imgNoStr = Integer.toHexString(imgNo);
		
		int temp = imgNoStr.length();
		
		if (temp<2)
		{
			for (int i=0; i<(2-temp); i++)
			{
				imgNoStr = '0' + imgNoStr;
			}
		}
		
		// Convert x to 3 bytes hexadecimal string
		String xStr = Integer.toHexString(x);
				
		temp = xStr.length();
		
		if (temp<3)
		{
			for (int i=0; i<(3-temp); i++)
			{
				xStr = '0' + xStr;
			}
		}
		
		// Convert size to 8 bytes hexadecimal string
		String sizeStr = Integer.toHexString(size);
		
		temp = sizeStr.length();
		
		if (temp<8)
		{
			for (int i=0; i<(8-temp); i++)
			{
				sizeStr = '0' + sizeStr;
			}
		}
		
		String commandStr = PS_HIMG + imgNoStr + xStr + sizeStr;
		commandStr = commandStr.toUpperCase();
		outStream.writeBytes(commandStr);
		
		// Check return packet
		int available = inStream.available();
		while (available == 0)
		{
			//System.out.println("Instream not available yet " + available);
			available = inStream.available();
		}
		
		System.out.println("SetChatPosition Available = " + available);
		
		byte[] byteArr = new byte[available];
		
		int count = readBufferToByteArray(byteArr, inStream, 0, available);
		
		if (count > 0)
		{
			String direction = readFieldString(byteArr,	0, 2);
			
			if (!direction.equalsIgnoreCase("SP"))
			{
				result = false;
				return result;
			}
			
			// Get return code and command
			String errCode = readFieldString(byteArr, 10, 4);
			String command = readFieldString(byteArr, 14, 4);
			
			if(command.equalsIgnoreCase("HIMG"))
			{
				if (!errCode.equalsIgnoreCase("HOK!"))
				{
					result = false;
					return result;
				}
				
				result = true;
			}
			else
			{
				result = false;
				return result;
			}			
		}
	
		return result;
	}
	
	public boolean sendChatImageData(int imgNo, int offset, int size, byte[] data, DataInputStream inStream, DataOutputStream outStream) throws IOException {
		boolean result = false;
		
		// Calculate No8 bytes from size
		int packetSize = (size+1) + 4 + 2 + 8 + 4;
		
		String packetSizeStr = Integer.toHexString(packetSize);
		
		int temp = packetSizeStr.length();
		
		if (temp<8)
		{
			for (int i=0; i<(8-temp); i++)
			{
				packetSizeStr = '0' + packetSizeStr;
			}
		}
		
		// Convert image number to 2 bytes hexadecimal string
		String imgNoStr = Integer.toHexString(imgNo);
		
		temp = imgNoStr.length();
		
		if (temp<2)
		{
			for (int i=0; i<(2-temp); i++)
			{
				imgNoStr = '0' + imgNoStr;
			}
		}
		
		// Convert offset to 8 bytes hexadecimal string		
		String offsetStr = Integer.toHexString(offset);
		
		temp = offsetStr.length();
		
		if (temp<8)
		{
			for (int i=0; i<(8-temp); i++)
			{
				offsetStr = '0' + offsetStr;
			}
		}
		
		// Convert size to 4 bytes hexadecimal string
		String sizeStr = Integer.toHexString(size);
		
		temp = sizeStr.length();
		
		if (temp<4)
		{
			for (int i=0; i<(4-temp); i++)
			{
				sizeStr = '0' + sizeStr;
			}
		}
		
		String commandStr = "PS" + packetSizeStr + "HIMD" + imgNoStr + offsetStr + sizeStr;
		commandStr = commandStr.toUpperCase();
		
		outStream.writeBytes(commandStr);
		outStream.write(data);
		
		// Check return packet
		int available = inStream.available();
		while (available == 0)
		{
			//System.out.println("Instream not available yet " + available);
			available = inStream.available();
		}
		
		System.out.println("sendChatImageData Available = " + available);
		
		byte[] byteArr = new byte[available];
		
		int count = readBufferToByteArray(byteArr, inStream, 0, available);
		
		if (count > 0)
		{
			String direction = readFieldString(byteArr,	0, 2);
			
			if (!direction.equalsIgnoreCase("SP"))
			{
				result = false;
				return result;
			}
			
			// Get return code and command
			String errCode = readFieldString(byteArr, 10, 4);
			String command = readFieldString(byteArr, 14, 4);
			
			if(command.equalsIgnoreCase("HIMD"))
			{
				if (!errCode.equalsIgnoreCase("HOK!"))
				{
					result = false;
					return result;
				}
				
				result = true;
			}
			else
			{
				result = false;
				return result;
			}			
		}
	
		return result;		
	}
	
	public boolean endChatImageData(int imgNo, DataInputStream inStream, DataOutputStream outStream) throws IOException {
		boolean result = false;
		
		// Convert image number to 2 bytes hexadecimal string
		String imgNoStr = Integer.toHexString(imgNo);
		
		int temp = imgNoStr.length();
		
		if (temp<2)
		{
			for (int i=0; i<(2-temp); i++)
			{
				imgNoStr = '0' + imgNoStr;
			}
		}
		
		String commandStr = PS_HIME + imgNoStr;
		commandStr = commandStr.toUpperCase();
		
		outStream.writeBytes(commandStr);
		
		// Check return packet
		int available = inStream.available();
		while (available == 0)
		{
			//System.out.println("Instream not available yet " + available);
			available = inStream.available();
		}
		
		System.out.println("endChatImageData Available = " + available);
		
		byte[] byteArr = new byte[available];
		
		int count = readBufferToByteArray(byteArr, inStream, 0, available);		
		
		if (count > 0)
		{
			String direction = readFieldString(byteArr,	0, 2);
			
			if (!direction.equalsIgnoreCase("SP"))
			{
				result = false;
				return result;
			}
			
			// Get return code and command
			String errCode = readFieldString(byteArr, 10, 4);
			String command = readFieldString(byteArr, 14, 4);
			
			if(command.equalsIgnoreCase("HIME"))
			{
				if (!errCode.equalsIgnoreCase("HOK!"))
				{
					result = false;
					return result;
				}
				
				result = true;
			}
			else
			{
				result = false;
				return result;
			}			
		}
	
		return result;
	}
	
	public boolean drawChatScreen(DataInputStream inStream, DataOutputStream outStream) throws IOException {
		boolean result = false;
		
		outStream.writeBytes(PS_HRAW);
		
		int available = inStream.available();
		while (available == 0)
		{
			//System.out.println("Instream not available yet " + available);
			available = inStream.available();
		}
		
		System.out.println("drawChatScreen Available = " + available);
		
		byte[] byteArr = new byte[available];
		
		int count = readBufferToByteArray(byteArr, inStream, 0, available);
		
		if (count > 0)
		{
			String direction = readFieldString(byteArr,	0, 2);
			
			if (!direction.equalsIgnoreCase("SP"))
			{
				result = false;
				return result;
			}
			
			// Get return code and command
			String errCode = readFieldString(byteArr, 10, 4);
			String command = readFieldString(byteArr, 14, 4);
			
			if(command.equalsIgnoreCase("HRAW"))
			{
				if (!errCode.equalsIgnoreCase("HOK!"))
				{
					result = false;
					return result;
				}
				
				result = true;
			}
			else
			{
				result = false;
				return result;
			}			
		}
	
		return result;
	}
	
	private void soka_wait(int period) {
		try {
			Thread.sleep(period);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
	private DataInputStream getDataFromSOKA(String command, Socket clientSock) {
		
		try {			
			// Send INFO packet to SOKA
			PrintWriter outStream = new PrintWriter(clientSock.getOutputStream(),true);
			System.out.println("Send to SOKA: " + command);
			outStream.println(command);
			
			DataInputStream inStream = new DataInputStream(clientSock.getInputStream());
			
			return inStream;
			
		} catch (UnknownHostException e) {
			System.out.println("getDataFromSOKA(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("getDataFromSOKA(): IOException");
			e.printStackTrace();
		} 
		
		return null;
	}
	
		
	private void sendDataToSOKA(String command, Socket clientSock) {
		
		try {			
			// Send INFO packet to SOKA
			PrintWriter outStream = new PrintWriter(clientSock.getOutputStream(),true);
			System.out.println("Send to SOKA: " + command);
			outStream.println(command);
			
		} catch (UnknownHostException e) {
			System.out.println("sendDataToSOKA(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("sendDataToSOKA(): IOException");
			e.printStackTrace();
		} 
	}
	
	private String readDirection(DataInputStream in) {
		byte[] dir = new byte[2];
		
		try {
			int count = readBufferToByteArray(dir, in, 0, 2);
			
			if(count>0)
			{
				String direction = new String(dir);
				return direction;
			}
		} catch (IOException e){
			System.out.println("Exception ");
			e.printStackTrace();
		} 		
		return null;
	}

	private int readNoByte(DataInputStream in) throws IOException {
		byte[] nobyte = new byte[8];
		int count = readBufferToByteArray(nobyte, in, 0, 8);
		
		if(count>0)
		{
			int packet_size = Integer.parseInt(new String(nobyte),16);
			return packet_size;			
		}
		
		return 0;
	}

	private byte[] readPayload(DataInputStream in, int length) throws IOException {
		byte[] payload = new byte[length];
		
		in.skip(4);
		int count = readBufferToByteArray(payload, in, 0, length);
		
		if (count>0)
		{
			System.out.println("Read payload " + count + " bytes!");
			return payload;
		}
		
		return payload;
	}
	
	private int readBufferToByteArray(byte[] out, DataInputStream in, int offset, int length) throws IOException {		
		try {
			//int count = in.readFully(out,offset,length);
			in.readFully(out,offset,length);
			return length;		
		}
		catch (IOException e){			
			e.printStackTrace();
		} 
		return 0;
	}
	
	private byte[] readFieldByte(byte[] arr, int offset, int length)
	{
		byte[] data = new byte[length];
		
		for (int i=0; i<length; i++)
		{
			data[i] = arr[i+offset];
		}
		
		return data;
	}
	
	private String readFieldString(byte[] arr, int offset, int length)
	{
		byte[] data = new byte[length];
		
		for (int i=0; i<length; i++)
		{
			data[i] = arr[i+offset];
		}
		
		String out = new String(data);
		
		return out;
	}
}
