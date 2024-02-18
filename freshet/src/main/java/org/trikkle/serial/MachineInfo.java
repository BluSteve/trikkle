package org.trikkle.serial;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Objects;

// a machine has an ip address and some port that other machines can connect to it via.
// also has a rsa public key
public class MachineInfo implements Serializable {
	public PublicKey publicKey;
	public String ip;
	public int port;

	public MachineInfo(PublicKey publicKey, String ip, int port) {
		this.publicKey = publicKey;
		this.ip = ip;
		this.port = port;
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
