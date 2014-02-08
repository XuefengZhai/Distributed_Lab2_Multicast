package Lab0;

public class TimeStampedMessage extends Message {

	TimeStamp ts;
	public TimeStampedMessage(String dest, String kind, Object data, TimeStamp t) {
		super(dest, kind, data);
		
		this.ts = t;
		
	}

}
