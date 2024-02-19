package org.trikkle.serial;

import org.trikkle.TlvMessage;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Objects;

// a machine has an ip address and some port that other machines can connect to it via.
// also has a rsa public key
public class MachineInfo implements Serializable {
	public PublicKey publicKey;
	public String ip;
	public int port;
	public transient Cipher cipher;

	public MachineInfo(PublicKey publicKey, String ip, int port) {
		this.publicKey = publicKey;
		this.ip = ip;
		this.port = port;
	}

	public Cipher getCipher() {
		if (cipher != null) {
			return cipher;
		} else {
			try {
				cipher = Cipher.getInstance("RSA");
				cipher.init(Cipher.ENCRYPT_MODE, publicKey);
				return cipher;
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void sendMessage(TlvMessage message) {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(ip, port));

			message.encrypted(getCipher()).writeTo(socket.getOutputStream());
		} catch (IOException e) {
			System.err.println("Could not send message to " + this);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MachineInfo machine = (MachineInfo) o;
		return Objects.equals(publicKey, machine.publicKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(publicKey);
	}

	@Override
	public String toString() {
		return "Machine{" + Integer.toHexString(publicKey.hashCode()) + "}(" + ip + ":" + port + ")";
	}
}
