package com.xbaodoo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class NetworkServer {

	public static final String PS_INFOR = "SP00000034INFOFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000";
	public static final String PS_ACLRR = "SP00000008AOK!ACLR";
	public static final String PS_ASTRR = "SP00000008AOK!ASTR";
	public static final String PS_ARAWR = "SP00000008AOK!ARAW";
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("Server is running...");
		
		ServerSocket serverSock = new ServerSocket();
		serverSock.bind(new InetSocketAddress(InetAddress.getLocalHost(),9011));
		
		System.out.println(serverSock.getInetAddress().toString());
		System.out.println(serverSock.getLocalSocketAddress().toString());
				
		try {
			
			//while (true) {
				Socket sock = serverSock.accept();
				
				try {
					/*
					BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
					
					System.out.println("Received message from client: ");
					System.out.println(in.readLine());
					
					// Send INFO packet to SOKA
					PrintWriter out = new PrintWriter(sock.getOutputStream(),true);
					System.out.println("Send message to client: " + PS_INFOR);
					out.println(PS_INFOR);
					*/
					// first message
					while(true)
					{
					BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
					System.out.println("Received message from client: ");
					System.out.println(in.readLine());
					
					PrintWriter out = new PrintWriter(sock.getOutputStream(),true);
					System.out.println("Send message to client: " + PS_ACLRR);
					out.println(PS_ACLRR);
					
					// second message
					System.out.println("Received message from client: ");
					System.out.println(in.readLine());
					
					System.out.println("Send message to client: " + PS_ASTRR);
					out.println(PS_ASTRR);
					
					// Third message
					System.out.println("Received message from client: ");
					System.out.println(in.readLine());
					
					System.out.println("Send message to client: " + PS_ARAWR);
					out.println(PS_ARAWR);
					}
				} finally {
					sock.close();
				}
			//}
			
		} finally {
			serverSock.close();
		}
	}

}
