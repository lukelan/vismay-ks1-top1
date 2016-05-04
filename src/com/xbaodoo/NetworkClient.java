package com.xbaodoo;

import java.io.IOException;
//import java.net.InetAddress;
//import java.net.UnknownHostException;
//import java.net.InetAddress;

import org.json.JSONException;

public class NetworkClient {

	public static void main(String[] args) throws IOException, JSONException {
						
		KtvAPI ktv = new KtvAPI("192.168.43.1",20158);
		ktv.createSocket();
		
		ktv.connectWifi("0903933209");
		ktv.ktv_wait(300);
		//ktv.remote((byte)0x12);
		
		//ktv.songReserve(1, (byte)0x13); // OK
		//ktv.recList(0, 9);      // OK
		//ktv.popularList(0, 99);  // OK		
		//ktv.message("Test Message"); // OK
		//ktv.keepConnection(); // OK
		//ktv.recDownload(0);  // OK
		//ktv.photo("IMG_0286.JPG", "3"); // OK
		//ktv.photo("ico_like.jpg", "3"); // OK
		//ktv.userDBDownload("0"); // OK
		//ktv.reserveList();
		ktv.masterDBDownload("0"); // Not OK
		
		ktv.closeSocket();
	}
}