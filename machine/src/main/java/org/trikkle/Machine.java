package org.trikkle;

import org.trikkle.serial.MachineInfo;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

// a machine has an ip address and some port that other machines can connect to it via.
// also has a rsa public key
public class Machine {
	public MachineInfo info;
	public Cipher cipher;
	private final Socket socket;
	private final InputStream in;
	private final OutputStream out;

	public Machine(MachineInfo info, Socket socket) throws IOException {
		this.info = info;
		this.socket = socket;
		this.in = socket.getInputStream();
		this.out = socket.getOutputStream();
	}

	public Cipher getCipher() {
		if (cipher != null) {
			return cipher;
		} else {
			try {
				cipher = Cipher.getInstance("RSA");
				cipher.init(Cipher.ENCRYPT_MODE, info.publicKey);
				return cipher;
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void sendMessage(TlvMessage message) throws IOException {
		synchronized (out) {
			message.encrypted(getCipher()).writeTo(out);
		}
	}

	public void sendPlainMessage(TlvMessage message) throws IOException {
		synchronized (out) {
			message.writeTo(out);
		}
	}

	public TlvMessage receiveMessage() throws IOException {
		synchronized (in) {
			return TlvMessage.readFrom(in);
		}
	}

	@Override
	public String toString() {
		return info.toString();
	}
}
