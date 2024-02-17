package org.trikkle.cluster;

import java.security.PublicKey;
import java.util.Objects;

// a machine has an ip address and some port that other machines can connect to it via.
// also has a rsa public key
public class Machine {
	public PublicKey publicKey;
	public String ip;
	public int port;

	public Machine(PublicKey publicKey, String ip, int port) {
		this.publicKey = publicKey;
		this.ip = ip;
		this.port = port;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Machine machine = (Machine) o;
		return Objects.equals(publicKey, machine.publicKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(publicKey);
	}

	@Override
	public String toString() {
		return "Machine{" +
				"publicKey=" + publicKey +
				", ip='" + ip + '\'' +
				", port=" + port +
				'}';
	}
}
