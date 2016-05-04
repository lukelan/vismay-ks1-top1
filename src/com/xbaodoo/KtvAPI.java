package com.xbaodoo;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



public class KtvAPI {
	private String m_serverAddr;
	private int m_port;
	private Socket m_clientSock;
	private String KEY_STR = "HmcKaraoke";
	
	public static int BLOCK_SIZE = 1024*1024;
	public static final int WAIT_TIME = 200;
	
	public KtvAPI() {
		m_serverAddr = "";
		m_port = 0;
		m_clientSock = null;
	}
	
	public KtvAPI(String addr, int port) {
		m_serverAddr = addr;
		m_port = port;
		m_clientSock = null;
		
		System.out.println("Server addr: " + m_serverAddr);
		System.out.println("Server port: " + m_port);
		
		if (m_serverAddr == "" || m_port == 0 ) {
			System.out.println("Incorrect addr or port");
			return;
		}
	}
	
	public boolean createSocket() {
		try {		
			if (m_clientSock == null)
			{
				m_clientSock = new Socket(m_serverAddr, m_port);
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean closeSocket() {
		try {
			if (m_clientSock != null)
			{
				m_clientSock.close();
				m_clientSock = null;
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean connectWifi(String phoneNum) throws JSONException, IOException {
		DataOutputStream outStream = null;
		DataInputStream inStream = null;
		boolean result = false;
		
		try {
			if (m_clientSock != null)
			{
				outStream = new DataOutputStream(m_clientSock.getOutputStream());
				inStream = new DataInputStream(m_clientSock.getInputStream());
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		JSONObject js = new JSONObject();
		
		js.put("Message", phoneNum);
		
		///System.out.println("Json string:" + js.toString());
		
		int jsonSize = js.toString().length();
		int packetSize = jsonSize + 7;
		
		byte[] jsonArr = js.toString().getBytes();
		byte[] cryptoArr = new byte[jsonSize];
		
		cryptoArr = cryptoString(jsonArr, KEY_STR.getBytes(), jsonSize);
		
		byte[] sendData = new byte[packetSize];
		
		sendData[0] = 0x02;
		sendData[1] = (byte) 0xAA;
		sendData[2] = 0x01;
		sendData[3] = (byte) (jsonSize >> 8);
		sendData[4] = (byte) jsonSize;
		
		for(int i=0; i<jsonSize; i++)
		{
			sendData[5+i] = cryptoArr[i];
		}
		
		sendData[packetSize - 2] = 0x03;
		sendData[packetSize - 1] = (byte) 0xAA;
		
		outStream.write(sendData);
		
		// Check return packet
		int available = inStream.available();
		
		while (available == 0)
		{
			available = inStream.available();
		}
		
		byte[] response = new byte[available];
		
		int count = readBufferToByteArray(response, inStream, 0, available);
		
		if (count > 0)
		{
			for(int i=0; i<count; i++)
			{
				System.out.print(response[i] + " ");
			}
		}
		
		if(response[2] == 0x02)
		{
			if (response[5] == 0x00)
			{
				result = true;
			}
		}
		
		return result;
	}
	
	public boolean songReserve(int songNum, byte keyVal) throws JSONException, IOException {
		DataOutputStream outStream = null;
		DataInputStream inStream = null;
		boolean result = false;
		
		try {			
			if (m_clientSock != null)
			{
				outStream = new DataOutputStream(m_clientSock.getOutputStream());
				inStream = new DataInputStream(m_clientSock.getInputStream());
			}
		} catch (UnknownHostException e) {
			System.out.println("songReserve(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("songReserve(): IOException");
			e.printStackTrace();
		}
		
		byte[] sendData = new byte[12];
		
		sendData[0] = 0x02;
		sendData[1] = (byte) 0xAA;
		sendData[2] = 0x15;
		sendData[3] = 0x00;
		sendData[4] = 0x05;
		
		// song no + key value
		sendData[5] = (byte) (songNum >> 24);
		sendData[6] = (byte) (songNum >> 16);
		sendData[7] = (byte) (songNum >> 8);
		sendData[8] = (byte) songNum;
		sendData[9] = keyVal; 
		
		sendData[10] = 0x03;
		sendData[11] = (byte) 0xAA;
		
		outStream.write(sendData);
		
		// Check return packet
		int available = inStream.available();
		
		while (available == 0)
		{
			available = inStream.available();
		}
		
		byte[] response = new byte[available];
		
		int count = readBufferToByteArray(response, inStream, 0, available);
		
		if (count > 0)
		{
			for(int i=0; i<count; i++)
			{
				System.out.print(response[i] + " ");
			}
		}
		
		if(response[2] == 0x16)
		{
			if (response[5] == 0x00)
			{
				result = true;
			}
		}
		
		return result;
	}
	
	public JSONArray recList(int startNum, int endNum) throws JSONException, IOException {
		DataOutputStream outStream = null;
		DataInputStream inStream = null;
		JSONArray result = null;
		
		try {			
			if (m_clientSock != null)
			{
				outStream = new DataOutputStream(m_clientSock.getOutputStream());
				inStream = new DataInputStream(m_clientSock.getInputStream());
			}
		} catch (UnknownHostException e) {
			System.out.println("recList(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("recList(): IOException");
			e.printStackTrace();
		}
		
		byte[] sendData = new byte[15];
		
		sendData[0] = 0x02;
		sendData[1] = (byte) 0xAA;
		sendData[2] = 0x21;
		sendData[3] = 0x00;
		sendData[4] = 0x08;
		
		// start Num
		sendData[5] = (byte) (startNum >> 24);
		sendData[6] = (byte) (startNum >> 16);
		sendData[7] = (byte) (startNum >> 8);
		sendData[8] = (byte) startNum;
		
		// end Num
		sendData[9]  = (byte) (endNum >> 24);
		sendData[10] = (byte) (endNum >> 16);
		sendData[11] = (byte) (endNum >> 8);
		sendData[12] = (byte) endNum;
		
		sendData[13] = 0x03;
		sendData[14] = (byte) 0xAA;
		
		outStream.write(sendData);
		
		// Check return packet
		int available = inStream.available();
		
		while (available == 0)
		{
			available = inStream.available();
		}
		
		byte[] response = new byte[available];
		
		int count = readBufferToByteArray(response, inStream, 0, available);
		
		// Parse response
		if(response[2] == 0x22)
		{
			if (response[5] != 0x01)
			{
				int msgLen = response[3];
				msgLen = msgLen << 8;
				msgLen = msgLen | response[4];
				
				byte[] jsonArrEncode = new byte[msgLen];
				byte[] jsonArrDecode = new byte[msgLen];
				
				for (int i=0; i<msgLen; i++)
				{
					jsonArrEncode[i] = response[5 + i];
				}
				
				jsonArrDecode = cryptoString(jsonArrEncode, KEY_STR.getBytes(), msgLen);
				
				String jsonStr = new String(jsonArrDecode);		
			
				JSONArray jsArray = new JSONArray(jsonStr);
				
				for (int i=0; i < jsArray.length(); i++)
				{
					JSONObject jsObject = jsArray.getJSONObject(i);
					System.out.println("JSObject[" + i + "] = " + jsObject.toString());					
				}
				
				result = jsArray;
			}
		}
		
		return result;
	}
	
	public JSONArray reserveList() throws JSONException, IOException {
		DataOutputStream outStream = null;
		DataInputStream inStream = null;
		JSONArray result = null;
		
		try {			
			if (m_clientSock != null)
			{
				outStream = new DataOutputStream(m_clientSock.getOutputStream());
				inStream = new DataInputStream(m_clientSock.getInputStream());
			}
		} catch (UnknownHostException e) {
			System.out.println("recList(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("recList(): IOException");
			e.printStackTrace();
		}
		
		byte[] sendData = new byte[8];
		
		sendData[0] = 0x02;
		sendData[1] = (byte)0xAA;
		sendData[2] = (byte)0xA7;
		sendData[3] = 0x00;
		sendData[4] = 0x01;
		
		// dummy 
		sendData[5] = (byte)0;
		
		sendData[6] = 0x03;
		sendData[7] = (byte) 0xAA;
		
		outStream.write(sendData);
		
		// Check return packet
		int available = inStream.available();
		
		while (available == 0)
		{
			available = inStream.available();
		}
		
		byte[] response = new byte[available];
		
		int count = readBufferToByteArray(response, inStream, 0, available);
		
		// Parse response
		if(response[2] == (byte)0xA8)
		{
			if (response[5] != 0x01)
			{
				int msgLen = response[3];
				msgLen = msgLen << 8;
				msgLen = msgLen | response[4];
				
				byte[] jsonArrEncode = new byte[msgLen];
				byte[] jsonArrDecode = new byte[msgLen];
				
				for (int i=0; i<msgLen; i++)
				{
					jsonArrEncode[i] = response[5 + i];
				}
				
				jsonArrDecode = cryptoString(jsonArrEncode, KEY_STR.getBytes(), msgLen);
				
				String jsonStr = new String(jsonArrDecode);		
			
				JSONArray jsArray = new JSONArray(jsonStr);
				
				for (int i=0; i < jsArray.length(); i++)
				{
					JSONObject jsObject = jsArray.getJSONObject(i);
					System.out.println("JSObject[" + i + "] = " + jsObject.toString());					
				}
				
				result = jsArray;
			}
		}
		
		return result;
	}
	
	public JSONArray popularList(int startNum, int endNum) throws JSONException, IOException {
		DataOutputStream outStream = null;
		DataInputStream inStream = null;
		JSONArray result = null;
		
		try {			
			if (m_clientSock != null)
			{
				outStream = new DataOutputStream(m_clientSock.getOutputStream());
				inStream = new DataInputStream(m_clientSock.getInputStream());
			}
		} catch (UnknownHostException e) {
			System.out.println("popularList(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("popularList(): IOException");
			e.printStackTrace();
		}
		
		byte[] sendData = new byte[15];
		
		sendData[0] = 0x02;
		sendData[1] = (byte) 0xAA;
		sendData[2] = 0x27;
		sendData[3] = 0x00;
		sendData[4] = 0x08;
		
		// start Num
		sendData[5] = (byte) (startNum >> 24);
		sendData[6] = (byte) (startNum >> 16);
		sendData[7] = (byte) (startNum >> 8);
		sendData[8] = (byte) startNum;
		
		// end Num
		sendData[9]  = (byte) (endNum >> 24);
		sendData[10] = (byte) (endNum >> 16);
		sendData[11] = (byte) (endNum >> 8);
		sendData[12] = (byte) endNum;
		
		sendData[13] = 0x03;
		sendData[14] = (byte) 0xAA;
		
		outStream.write(sendData);
		
		// Check return packet
		int available = inStream.available();
		
		while (available == 0)
		{
			available = inStream.available();
		}
		
		byte[] response = new byte[available];
		
		int count = readBufferToByteArray(response, inStream, 0, available);
		
		// Parse response
		if(response[2] == 0x28)
		{
			if (response[5] != 0x01)
			{
				//int msgLen = response[3];
				//msgLen = msgLen << 8;
				//msgLen = msgLen | response[4];
				
				int msgLen = available - 7;
				
				byte[] jsonArrEncode = new byte[msgLen];
				byte[] jsonArrDecode = new byte[msgLen];
				
				for (int i=0; i<msgLen; i++)
				{
					jsonArrEncode[i] = response[5 + i];
				}
				
				jsonArrDecode = cryptoString(jsonArrEncode, KEY_STR.getBytes(), msgLen);
				
				String jsonStr = new String(jsonArrDecode);		
			
				JSONArray jsArray = new JSONArray(jsonStr);
				
				for (int i=0; i < jsArray.length(); i++)
				{
					JSONObject jsObject = jsArray.getJSONObject(i);
					System.out.println("JSObject[" + i + "] = " + jsObject.toString());					
				}
				
				result = jsArray;
			}
		}
		
		return result;
	}
	
	public boolean message(String msg) throws JSONException, IOException {
		DataOutputStream outStream = null;
		DataInputStream inStream = null;
		boolean result = false;
		
		try {			
			if (m_clientSock != null)
			{
				outStream = new DataOutputStream(m_clientSock.getOutputStream());
				inStream = new DataInputStream(m_clientSock.getInputStream());
			}
		} catch (UnknownHostException e) {
			System.out.println("message(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("message(): IOException");
			e.printStackTrace();
		}
		
		JSONObject js = new JSONObject();
		
		js.put("Message", msg);
		
		///System.out.println("Json string:" + js.toString());
		
		int jsonSize = js.toString().length();
		int packetSize = jsonSize + 7;
		
		byte[] jsonArr = js.toString().getBytes();
		byte[] cryptoArr = new byte[jsonSize];
		
		cryptoArr = cryptoString(jsonArr, KEY_STR.getBytes(), jsonSize);
		
		byte[] sendData = new byte[packetSize];
		
		sendData[0] = 0x02;
		sendData[1] = (byte) 0xAA;
		sendData[2] = 0x25;
		sendData[3] = (byte) (jsonSize >> 8);
		sendData[4] = (byte) jsonSize;
		
		for(int i=0; i<jsonSize; i++)
		{
			sendData[5+i] = cryptoArr[i];
		}
		
		sendData[packetSize - 2] = 0x03;
		sendData[packetSize - 1] = (byte) 0xAA;
		
		// Hack
		ktv_wait(300);
		
		outStream.write(sendData);
		
		// Check return packet
		int available = inStream.available();
		
		while (available == 0)
		{
			available = inStream.available();
		}
		
		byte[] response = new byte[available];
		
		int count = readBufferToByteArray(response, inStream, 0, available);
		
		if (count > 0)
		{
			for(int i=0; i<count; i++)
			{
				System.out.print(response[i] + " ");
			}
		}
		
		if(response[2] == 0x26)
		{
			if (response[5] == 0x00)
			{
				result = true;
			}
		}
		
		return result;
	}
	
	public boolean photo(String pic, String type) {
		DataOutputStream outStream = null;
		DataInputStream inStream = null;
		boolean result = false;
		
		try {			
			if (m_clientSock != null)
			{
				outStream = new DataOutputStream(m_clientSock.getOutputStream());
				inStream = new DataInputStream(m_clientSock.getInputStream());
			}
		} catch (UnknownHostException e) {
			System.out.println("photo(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("photo(): IOException");
			e.printStackTrace();
		}
		
		if (pic != null)
		{
			FileInputStream fis = null;
			DataInputStream inputFile = null;			
			
			// Read image file
			try {
				
				// Calculate md5 of original picture
				fis = new FileInputStream(pic);
				byte[] md5 = md5(fis);
				
				fis.close();
				
				// Re-read the file
				fis = new FileInputStream(pic);
				inputFile = new DataInputStream(fis);
				
				int fileSize = (int) fis.getChannel().size();
				String fileSizeStr = Integer.toString(fileSize);
				
				boolean ret1 = sendFileInfo(pic, type, fileSizeStr, inStream, outStream);
				
				if (ret1 == true)
				{
					int numBlock = fileSize/BLOCK_SIZE;
					
					int remainBytes;
					
					if (numBlock>0)
					{
						remainBytes = fileSize%(numBlock*BLOCK_SIZE);
					}
					else
					{
						remainBytes = fileSize%BLOCK_SIZE;
					}
					
					int highIndex = 0;
					int lowIndex = 0;
					boolean ret2 = true;
					
					if (remainBytes != 0)
					{
						if (numBlock > 0)
						{
							highIndex = numBlock + 1;
							lowIndex = 2;
						}
					}
					else
					{
						highIndex = numBlock - 1;
						lowIndex = 1;
					}
					
					if(numBlock > 0)
					{
						for (int i=highIndex; i>=lowIndex; i--)
						{
							byte[] imageData = new byte[BLOCK_SIZE];
							int byteRead = inputFile.read(imageData, 0, BLOCK_SIZE);
							
							if (byteRead == BLOCK_SIZE)
							{
								ret2 = transferPacket(imageData, i, BLOCK_SIZE, inStream, outStream);
								
								if (ret2 == false)
								{
									break;
								}
							}
						}
					}
					
					if (ret2 == true)
					{
						if (remainBytes != 0)
						{
							byte[] imageData = new byte[remainBytes];
							int byteRead = inputFile.read(imageData, 0, remainBytes);
							
							ret2 = transferPacket(imageData, 1, remainBytes, inStream, outStream);
						}
					}
					
					if (ret2 == true)
					{
						// Send transfer check										
						ret2 = sendTransferCheck(md5, inStream, outStream);
						
						if (ret2 == true)
						{
							result = true;
						}
					}
					
					try {
						fis.close();
						inputFile.close();
					} catch (IOException e) {
						e.printStackTrace();
						result = false;
					}
				}					
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return result;
	}
	
	public boolean recDownload(int index) throws IOException, JSONException {
		DataOutputStream outStream = null;
		DataInputStream inStream = null;
		boolean result = false;
		
		try {			
			if (m_clientSock != null)
			{
				outStream = new DataOutputStream(m_clientSock.getOutputStream());
				inStream = new DataInputStream(m_clientSock.getInputStream());
			}
		} catch (UnknownHostException e) {
			System.out.println("recDownload(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("recDownload(): IOException");
			e.printStackTrace();
		}
		
		byte[] sendData = new byte[11];
		
		sendData[0] = 0x02;
		sendData[1] = (byte) 0xAA;
		sendData[2] = (byte) 0xA1;
		sendData[3] = 0x00;
		sendData[4] = 0x04;
		
		// song no + key value
		sendData[5] = (byte) (index >> 24);
		sendData[6] = (byte) (index >> 16);
		sendData[7] = (byte) (index >> 8);
		sendData[8] = (byte) index;
		
		sendData[9] = 0x03;
		sendData[10] = (byte) 0xAA;
		
		outStream.write(sendData);
		
		// Check return packet
		int available = inStream.available();
		
		while (available == 0)
		{
			//System.out.println("Instream not available yet " + available);
			available = inStream.available();
		}
		
		byte[] response = new byte[available];
		
		int count = readBufferToByteArray(response, inStream, 0, available);

		if(response[2] == (byte)0xA2)
		{
			if (response[5] != 0x01)
			{
				int msgLen = response[3];
				msgLen = msgLen << 8;
				msgLen = msgLen | response[4];
				
				byte[] jsonArrEncode = new byte[msgLen];
				byte[] jsonArrDecode = new byte[msgLen];
				
				for (int i=0; i<msgLen; i++)
				{
					jsonArrEncode[i] = response[5 + i];
				}
				
				jsonArrDecode = cryptoString(jsonArrEncode, KEY_STR.getBytes(), msgLen);
				
				String jsonStr = new String(jsonArrDecode);
				
				JSONObject js = new JSONObject(jsonStr);
				
				System.out.println("Json string:" + js.toString());
				
				String fileName = js.getString("Name");
				int type = Integer.parseInt(js.getString("Type"));
				int length = Integer.parseInt(js.getString("Length"));			
				
				result = true;
			}
		}
		
		return result;
	}
	
	public boolean userDBDownload(String updateTime) {
		DataOutputStream outStream = null;
		DataInputStream inStream = null;
		boolean result = false;
		
		try {			
			if (m_clientSock != null)
			{
				outStream = new DataOutputStream(m_clientSock.getOutputStream());
				inStream = new DataInputStream(m_clientSock.getInputStream());
			}
		} catch (UnknownHostException e) {
			System.out.println("userDBDownload(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("userDBDownload(): IOException");
			e.printStackTrace();
		}
		
		result = sendDBRequest(updateTime, getCurrentDateTime(), (byte) 0xA3, (byte) 0xA4, inStream, outStream);
		
		if (result == true)
		{
			sendTransferWaiting(inStream, outStream);
			
			result = receiveFile((byte) 0xC3, (byte) 0xC4, inStream, outStream);
		}
		
		return result;
	}
	
	public boolean masterDBDownload(String updateTime) {
		DataOutputStream outStream = null;
		DataInputStream inStream = null;
		boolean result = false;
		
		try {			
			if (m_clientSock != null)
			{
				outStream = new DataOutputStream(m_clientSock.getOutputStream());
				inStream = new DataInputStream(m_clientSock.getInputStream());
			}
		} catch (UnknownHostException e) {
			System.out.println("masterDBDownload(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("masterDBDownload(): IOException");
			e.printStackTrace();
		}
		
		result = sendDBRequest(updateTime, getCurrentDateTime(), (byte) 0xA5, (byte) 0xA6, inStream, outStream);
		
		if (result == true)
		{
			sendTransferWaiting(inStream, outStream);
			
			result = receiveFile((byte) 0xC3, (byte) 0xC4, inStream, outStream);
		}
		
		return result;
	}
	
	public boolean remote(byte keyVal) throws IOException {
		DataOutputStream outStream = null;
		DataInputStream inStream = null;
		boolean result = false;
		
		try {			
			if (m_clientSock != null)
			{
				outStream = new DataOutputStream(m_clientSock.getOutputStream());
				inStream = new DataInputStream(m_clientSock.getInputStream());
			}
		} catch (UnknownHostException e) {
			System.out.println("remote(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("remote(): IOException");
			e.printStackTrace();
		}
				
		byte[] sendData = new byte[8];
		
		sendData[0] = 0x02;
		sendData[1] = (byte) 0xAA;
		sendData[2] = 0x13;
		sendData[3] = 0x00;
		sendData[4] = 0x01;
		sendData[5] = keyVal;
		sendData[6] = 0x03;
		sendData[7] = (byte) 0xAA;
		
		outStream.write(sendData);
		
		// Check return packet
		int available = inStream.available();
		
		while (available == 0)
		{
			available = inStream.available();
		}
		
		byte[] response = new byte[available];
		
		int count = readBufferToByteArray(response, inStream, 0, available);
		
		if (count > 0)
		{
			for(int i=0; i<count; i++)
			{
				System.out.print(response[i] + " ");
			}
		}
		
		if(response[2] == 0x14)
		{
			if (response[5] == 0x00)
			{
				result = true;
			}
		}
		
		return result;
	}
	
	public boolean keepConnection() throws IOException {
		DataOutputStream outStream = null;
		DataInputStream inStream = null;
		boolean result = false;
		
		try {			
			if (m_clientSock != null)
			{
				outStream = new DataOutputStream(m_clientSock.getOutputStream());
				inStream = new DataInputStream(m_clientSock.getInputStream());
			}
		} catch (UnknownHostException e) {
			System.out.println("keepConnection(): UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("keepConnection(): IOException");
			e.printStackTrace();
		}
				
		byte[] sendData = new byte[8];
		
		sendData[0] = 0x02;
		sendData[1] = (byte) 0xAA;
		sendData[2] = 0x03;
		sendData[3] = 0x00;
		sendData[4] = 0x01;
		sendData[5] = 0x01;
		sendData[6] = 0x03;
		sendData[7] = (byte) 0xAA;
		
		outStream.write(sendData);
		
		// Check return packet
		int available = inStream.available();
		
		while (available == 0)
		{
			available = inStream.available();
		}
		
		byte[] response = new byte[available];
		
		int count = readBufferToByteArray(response, inStream, 0, available);
		
		if (count > 0)
		{
			for(int i=0; i<count; i++)
			{
				System.out.print(response[i] + " ");
			}
		}
		
		if(response[2] == 0x04)
		{
			if (response[5] == 0x00)
			{
				result = true;
			}
		}
		
		return result;
	}
		
	
	private boolean sendFileInfo(String pic, String type, String length, DataInputStream inStream, DataOutputStream outStream) {
		
		try {
			JSONObject js = new JSONObject();
			
			js.put("Name", pic);
			js.put("Type", type);
			js.put("Length", length);
			
			///System.out.println("Json string:" + js.toString());
			
			int jsonSize = js.toString().length();
			int packetSize = jsonSize + 7;
			
			byte[] jsonArr = js.toString().getBytes();
			byte[] cryptoArr = new byte[jsonSize];
			
			cryptoArr = cryptoString(jsonArr, KEY_STR.getBytes(), jsonSize);
			
			byte[] sendData = new byte[packetSize];
			
			sendData[0] = 0x02;
			sendData[1] = (byte) 0xAA;
			sendData[2] = (byte) 0xB1;
			sendData[3] = (byte) (jsonSize >> 8);
			sendData[4] = (byte) jsonSize;
			
			for(int i=0; i<jsonSize; i++)
			{
				sendData[5+i] = cryptoArr[i];
			}
			
			sendData[packetSize - 2] = 0x03;
			sendData[packetSize - 1] = (byte) 0xAA;
			
			outStream.write(sendData);
			
			// Check return packet
			int available = inStream.available();
			
			while (available == 0)
			{
				available = inStream.available();
			}
			
			byte[] response = new byte[available];
			
			int count = readBufferToByteArray(response, inStream, 0, available);
			
			/*
			if (count > 0)
			{
				for(int i=0; i<count; i++)
				{
					System.out.print(response[i] + " ");
				}
			}
			*/
			
			if(response[2] == (byte)0xB2)
			{
				if (response[5] == 0x01)
				{
					return false;
				}
			}
				
		} catch (JSONException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		
		return true;
	}
	
	private boolean transferPacket(byte[] data, int blockCount, int size, DataInputStream inStream, DataOutputStream outStream) {
		
		try {
			int packetSize = 7 + size;
			byte[] sendData = new byte[packetSize];
			
			sendData[0] = 0x02;
			sendData[1] = (byte) 0xAA;
			sendData[2] = (byte) 0xC3;
			sendData[3] = (byte) (blockCount >> 8);
			sendData[4] = (byte) blockCount;
			
			for(int i=0; i<size; i++)
			{
				sendData[5+i] = data[i];
			}
			
			sendData[packetSize - 2] = 0x03;
			sendData[packetSize - 1] = (byte) 0xAA;
			
			outStream.write(sendData);
			
			// Check return packet
			int available = inStream.available();
			
			while (available == 0)
			{
				available = inStream.available();
			}
			
			byte[] response = new byte[available];
			
			int count = readBufferToByteArray(response, inStream, 0, available);
			
			/*
			if (count > 0)
			{
				for(int i=0; i<count; i++)
				{
					System.out.print(response[i] + " ");
				}
			}
			*/
			
			if(response[2] == (byte)0xC4)
			{
				if (response[5] == 0x00)
				{
					return true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return false;
	}
	
	private boolean sendTransferCheck(byte[] md5, DataInputStream inStream, DataOutputStream outStream) {
		
		try {
			int packetSize = 7 + 16;
			byte[] sendData = new byte[packetSize];
			
			sendData[0] = 0x02;
			sendData[1] = (byte) 0xAA;
			sendData[2] = (byte) 0xC5;
			sendData[3] = 0;
			sendData[4] = 0x10;
			
			for(int i=0; i<16; i++)
			{
				sendData[5+i] = md5[i];
			}
			
			sendData[packetSize - 2] = 0x03;
			sendData[packetSize - 1] = (byte) 0xAA;
			
			outStream.write(sendData);
			
			// Check return packet
			int available = inStream.available();
			
			while (available == 0)
			{
				available = inStream.available();
			}
			
			byte[] response = new byte[available];
			
			int count = readBufferToByteArray(response, inStream, 0, available);
			
			/*
			if (count > 0)
			{
				for(int i=0; i<count; i++)
				{
					System.out.print(response[i] + " ");
				}
			}
			*/
			
			if(response[2] == (byte)0xC6)
			{
				if (response[5] == 0x00)
				{
					return true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}
	
	private boolean responseTransferCheck(byte[] refMd5, DataInputStream inStream, DataOutputStream outStream) {
		boolean result = false;
		
		try {			
			//ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
			byte[] receivedPacket = new byte[BLOCK_SIZE + 7];
			byte[] receiveMd5 = new byte[16];
			
			/*
			while ((nread = inStream.read(buffer)) != -1) 
			{
				byteArray.write(buffer, 0, nread);
			};
			
			byte[] receivedPacket = byteArray.toByteArray();
			*/
			
			int count = inStream.read(receivedPacket, 0, BLOCK_SIZE + 7);
			
			//if (receivedPacket[2] == 0xC5) {
				
				for (int i=0; i<16; i++)
				{
					receiveMd5[i] = receivedPacket[5+i];
				}
				
				byte[] sendData = new byte[8];
				
				sendData[0] = 0x02;
				sendData[1] = (byte) 0xAA;
				sendData[2] = (byte) 0xC6;
				sendData[3] = 0x00;
				sendData[4] = 0x01;
				sendData[5] = 0x01;
				sendData[6] = 0x03;
				sendData[7] = (byte) 0xAA;				
								
				if (Arrays.equals(refMd5, receiveMd5))
				{
					sendData[5] = 0x00;
					result = true;
				}					
				
				outStream.write(sendData);
			//}
			
		} catch (IOException e) {
			// Nothing
			e.printStackTrace();
		}
		
		return result;
	}
	
	private byte[] cryptoString(byte [] org, byte[] key, int length) {
		byte[] output = new byte[length];
		
		int keySize = key.length;
		int keyCount = 0;
		
		for (int i=0; i<length; i++) {
			output[i] = (byte) (org[i] ^ key[keyCount++]);
			if (keyCount == keySize) keyCount = 0;
		}
		
		return output; 
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
	
	private byte[] md5(InputStream is) {
		try {
			int read = 0;
			byte[] bytes = new byte[BLOCK_SIZE];
			MessageDigest digest = MessageDigest.getInstance("MD5");
			
			while ((read = is.read(bytes, 0, BLOCK_SIZE)) != -1) {
				digest.update(bytes, 0, read);
			}
			
			byte [] md5 = digest.digest();
			
			return md5;
					
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}		
	}
	
	private boolean sendDBRequest(String updateTime, String currentTime, byte opCode, byte retOpCode, DataInputStream inStream, DataOutputStream outStream) {
		boolean result = false;
		
		JSONObject js = new JSONObject();
		
		try {
			js.put("Update", updateTime);
			js.put("Current", currentTime);
				
			int jsonSize = js.toString().length();
			int packetSize = jsonSize + 7;
			
			byte[] jsonArr = js.toString().getBytes();
			byte[] cryptoArr = new byte[jsonSize];
			
			cryptoArr = cryptoString(jsonArr, KEY_STR.getBytes(), jsonSize);
			
			byte[] sendData = new byte[packetSize];
			
			sendData[0] = 0x02;
			sendData[1] = (byte) 0xAA;
			sendData[2] = opCode;
			sendData[3] = (byte) (jsonSize >> 8);
			sendData[4] = (byte) jsonSize;
			
			for(int i=0; i<jsonSize; i++)
			{
				sendData[5+i] = cryptoArr[i];
			}
			
			sendData[packetSize - 2] = 0x03;
			sendData[packetSize - 1] = (byte) 0xAA;
			
			outStream.write(sendData);
			
			// Check return packet
			int available = inStream.available();
			
			while (available == 0)
			{
				available = inStream.available();
			}
			
			byte[] response = new byte[available];
			
			int count = readBufferToByteArray(response, inStream, 0, available);
			
			/*
			if (count > 0)
			{
				for(int i=0; i<count; i++)
				{
					System.out.print(response[i] + " ");
				}
			}
			*/
			
			// Parse response
			if(response[2] == retOpCode)
			{
				int msgLen = response[3];
				msgLen = msgLen << 8;
				msgLen = msgLen | response[4];
				
				if (msgLen != 0x01)
				{					
					byte[] jsonArrEncode = new byte[msgLen];
					byte[] jsonArrDecode = new byte[msgLen];
					
					for (int i=0; i<msgLen; i++)
					{
						jsonArrEncode[i] = response[5 + i];
					}
					
					jsonArrDecode = cryptoString(jsonArrEncode, KEY_STR.getBytes(), msgLen);
					
					String jsonStr = new String(jsonArrDecode);
					
					JSONObject js1 = new JSONObject(jsonStr);
					
					System.out.println("Json string:" + js1.toString());					
					
					result = true;
				}
				else
				{
					if (response[5] == 0x00)
					{
						result = true;
					}
				}
			}
		
		} catch (JSONException e) {
			result = false;
			e.printStackTrace();
		} catch (IOException e) {
			result = false;
			e.printStackTrace();
		}
		
		return result;
	}
	
	private void sendTransferWaiting(DataInputStream inStream, DataOutputStream outStream) {
		try {
			
			byte[] sendData = new byte[8];
			
			sendData[0] = 0x02;
			sendData[1] = (byte) 0xAA;
			sendData[2] = (byte) 0xC1;
			sendData[3] = 0x00;
			sendData[4] = 0x01;
			sendData[5] = 0x01;
			sendData[6] = 0x03;
			sendData[7] = (byte) 0xAA;
			
			outStream.write(sendData);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void sendTransferResponse(byte replyCode, byte response, DataInputStream inStream, DataOutputStream outStream) {
		try {
			
			byte[] sendData = new byte[8];
			
			sendData[0] = 0x02;
			sendData[1] = (byte) 0xAA;
			sendData[2] = replyCode;
			sendData[3] = 0x00;
			sendData[4] = 0x01;
			sendData[5] = response;
			sendData[6] = 0x03;
			sendData[7] = (byte) 0xAA;
			
			outStream.write(sendData);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private boolean receiveFile(byte receiveCode, byte replyCode, DataInputStream inStream, DataOutputStream outStream) {
		boolean result = false;
		
		try {
			DataOutputStream outputFile = new DataOutputStream(new FileOutputStream("update_userlist.db"));
			//ByteArrayOutputStream  byteArray = new ByteArrayOutputStream();			
			
			byte[] receivedPacket = new byte[BLOCK_SIZE + 7];
			
			/*
			while ((nread = inStream.read(buffer)) != -1) 
			{
				byteArray.write(buffer, 0, nread);
			};
			*/
			int nread = inStream.read(receivedPacket, 0, BLOCK_SIZE + 7);
			//inStream.readFully(receivedPacket, 0, BLOCK_SIZE + 7);
			
			//byte[] receivedPacket = byteArray.toByteArray();
			
			if (receivedPacket[2] == receiveCode) {
				
				//int dataSize = receivedPacket.length - 7;
				int dataSize = nread - 7;
				
				for (int i=0; i<dataSize; i++)
				{
					outputFile.writeByte(receivedPacket[5+i]);
				}
				
				sendTransferResponse(replyCode, (byte)0, inStream, outStream);
				
				// Read subsequence packets
				int blockCount = receivedPacket[3];
				blockCount = blockCount << 8;
				blockCount = blockCount | receivedPacket[4];
				
				if (blockCount > 1)
				{
					for (int i=blockCount - 1; i>=1; i--)
					{
						nread = inStream.read(receivedPacket, 0, BLOCK_SIZE + 7);
						
						//byteArray = new ByteArrayOutputStream();
						/*
						while ((nread = inStream.read(buffer)) != -1) 
						{
							byteArray.write(buffer, 0, nread);
						};
						*/
						
						//receivedPacket = byteArray.toByteArray();
						
						if (receivedPacket[2] == receiveCode) {
							//dataSize = receivedPacket.length - 7;
							dataSize = nread - 7;
							
							for (int j=0; j<dataSize; j++)
							{
								outputFile.writeByte(receivedPacket[5+j]);
							}
							
							sendTransferResponse(replyCode, (byte)0, inStream, outStream);
						}
					}
				}
				
				outputFile.close();
				
				// Receive Transfer Check
				FileInputStream fis = null;
				
				fis = new FileInputStream("update_userlist.db");
				
				int fileSize = (int) fis.getChannel().size();
				
				byte[] md5Ref = md5(fis);
				
				// Read data from Transfer Check
				result = responseTransferCheck(md5Ref, inStream, outStream);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			result = false;
		}		
		
		return result;
	}
	
	public void ktv_wait(int period) {
		try {
			Thread.sleep(period);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getCurrentDateTime() {
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		
		String date = dateFormat.format(Calendar.getInstance().getTime());
		
		return date;
	}
	
	public void testMD5() {
		FileInputStream fis = null;
		DataInputStream inputFile = null;
		
		// Read image file
		try {
			fis = new FileInputStream("pic000001.jpg");
			inputFile = new DataInputStream(fis);
			
			int fileSize = (int) fis.getChannel().size();
			
			byte[] imageData = new byte[fileSize];
			int byteRead = inputFile.read(imageData, 0, fileSize);
			
			byte[] md5 = md5(fis);
			
			System.out.println("Done");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
