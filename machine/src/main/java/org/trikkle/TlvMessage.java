package org.trikkle;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public class TlvMessage {
	private static final KeyGenerator keyGen;

	static {
		try {
			keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(128);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

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
		char dataType = (char) dis.read(); // dis.readChar() doesn't work fsr
		int length = dis.readInt();
		byte[] data = new byte[length];
		dis.readFully(data);
		return new TlvMessage(dataType, data);
	}

	public static Cipher getEncryptCipher(PublicKey publicKey) {
		Cipher encryptCipher;
		try {
			encryptCipher = Cipher.getInstance("RSA");
			encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
			throw new RuntimeException(e);
		}
		return encryptCipher;
	}

	public void writeTo(OutputStream out) throws IOException {
		DataOutputStream dos = new DataOutputStream(out);
		dos.write(dataType);
		dos.writeInt(length);
		dos.write(data);
	}

	public TlvMessage encrypted(Cipher cipher) {
		SecretKey key = keyGen.generateKey();
		byte[] encryptedData;
		byte[] encryptedKey;
		try {
			Cipher aesCipher = Cipher.getInstance("AES");
			aesCipher.init(Cipher.ENCRYPT_MODE, key);
			encryptedData = aesCipher.doFinal(data);
			encryptedKey = cipher.doFinal(key.getEncoded());
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
			throw new RuntimeException(e);
		} catch (IllegalBlockSizeException | BadPaddingException e) { // todo handle these better. bad key leads to it
			throw new RuntimeException(e);
		}

		byte[] combined = new byte[encryptedKey.length + encryptedData.length];
		System.arraycopy(encryptedKey, 0, combined, 0, encryptedKey.length);
		System.arraycopy(encryptedData, 0, combined, encryptedKey.length, encryptedData.length);
		return new TlvMessage(dataType, combined);
	}

	public TlvMessage decrypted(Cipher cipher) {
		byte[] encryptedKey = new byte[256];
		byte[] encryptedData = new byte[data.length - 256];
		System.arraycopy(data, 0, encryptedKey, 0, 256);
		System.arraycopy(data, 256, encryptedData, 0, data.length - 256);

		byte[] decryptedData;
		try {
			SecretKey key = new SecretKeySpec(cipher.doFinal(encryptedKey), "AES");
			Cipher aesCipher = Cipher.getInstance("AES");
			aesCipher.init(Cipher.DECRYPT_MODE, key);
			decryptedData = aesCipher.doFinal(encryptedData);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
			throw new RuntimeException(e);
		} catch (IllegalBlockSizeException | BadPaddingException e) { // todo handle these better. bad key leads to it
			throw new RuntimeException(e);
		}
		return new TlvMessage(dataType, decryptedData);
	}

	@Override
	public String toString() {
		return "org.trikkle.TlvMessage{" +
				"dataType=" + dataType +
				", length=" + length +
				", data=" + new String(data, StandardCharsets.UTF_8) +
				'}';
	}
}
