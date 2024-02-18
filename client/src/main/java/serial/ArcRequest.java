package serial;

import java.io.Serializable;

public class ArcRequest implements Serializable {
	public int index;
	public long argument;

	public ArcRequest() {
	}

	public ArcRequest(int index, long argument) {
		this.index = index;
		this.argument = argument;
	}
}
