package org.trikkle.structs;

public class Pair<F, S> {
	public F fi;
	public S se;

	public Pair(F fi, S se) {
		this.fi = fi;
		this.se = se;
	}

	public F getFi() {
		return fi;
	}

	public void setFi(F fi) {
		this.fi = fi;
	}

	public S getSe() {
		return se;
	}

	public void setSe(S se) {
		this.se = se;
	}

	@Override
	public String toString() {
		return "Pair{" +
				"first=" + fi +
				", second=" + se +
				'}';
	}
}
