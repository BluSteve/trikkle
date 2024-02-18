import org.trikkle.Handler;

public class Handlers {
	@Handler(dataType = 't')
	public static void handleData(byte[] data) {
		System.out.println("data = " + new String(data));
	}
}
