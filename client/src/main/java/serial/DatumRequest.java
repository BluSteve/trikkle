package serial;

import java.io.Serializable;
import java.security.PublicKey;

public class DatumRequest implements Serializable {
	public String datumName;
	public PublicKey publicKey; // need this to encrypt the response

	public DatumRequest() {
	}

	public DatumRequest(String datumName, PublicKey publicKey) {
		this.datumName = datumName;
		this.publicKey = publicKey;
	}
}
