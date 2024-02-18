package org.trikkle.serial;

import java.io.Serializable;
import java.security.PublicKey;

public class InitialData implements Serializable {
	public String password;
	public PublicKey publicKey;
	public String ip;
	public int port;
}
