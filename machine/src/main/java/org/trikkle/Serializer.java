package org.trikkle;

import java.io.*;

public class Serializer {
	public static byte[] serialize(Serializable object) {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				 ObjectOutputStream out = new ObjectOutputStream(bos)) {
			out.writeObject(object);
			out.flush();
			return bos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Object deserialize(byte[] bytes) {
		try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
				 ObjectInputStream in = new ObjectInputStream(bis)) {
			return in.readObject();
		} catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
