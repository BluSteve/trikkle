package org.trikkle.serial;

import java.io.Serializable;

public class JarInfo implements Serializable {
	public String className;
	public byte[] jar;

	public JarInfo(String className, byte[] jar) {
		this.className = className;
		this.jar = jar;
	}
}
