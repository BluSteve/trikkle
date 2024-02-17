package org.trikkle;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class TlvMessage {
	public char dataType;
	public int length;
	public byte[] data;

	public TlvMessage(char dataType, byte[] data) {
		this.dataType = dataType;
		this.length = data.length;
		this.data = data;
	}

	public static TlvMessage readFrom(InputStream in) throws IOException {
		DataInputStream dis = new DataInputStream(new BufferedInputStream(in));
		char dataType = (char) dis.read();
		int length = dis.readInt();
		byte[] data = new byte[length];
		dis.readFully(data);
		return new TlvMessage(dataType, data);
	}

	public void writeTo(OutputStream out) throws IOException {
		DataOutputStream dos = new DataOutputStream(out);
		dos.write(dataType);
		dos.writeInt(length);
		dos.write(data);
	}

	@Override
	public String toString() {
		return "TlvMessage{" +
				"dataType=" + dataType +
				", length=" + length +
				", data=" + new String(data, StandardCharsets.UTF_8) +
				'}';
	}
}
