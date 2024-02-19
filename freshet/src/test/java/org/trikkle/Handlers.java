package org.trikkle;

import java.net.Socket;

public class Handlers {
	@Handler(dataType = 't')
	public static void handleData(byte[] data, Socket socket) {
		System.out.println("data = " + new String(data));
	}
}
