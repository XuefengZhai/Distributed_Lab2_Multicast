package Lab0;

import java.util.Map;

public class TimeStampedMessage extends Message {

	TimeStamp ts;
	public boolean isMulticast;
	public Map<String,Integer> lastSeqFromMembers;
	public int multicastSeq;
	
	public TimeStampedMessage(String dest, String kind, Object data, TimeStamp t) {
		super(dest, kind, data);
		this.ts = t;
		
		this.isMulticast = false;
		this.multicastSeq = -1;
		this.lastSeqFromMembers = null;
		
	}
	public TimeStampedMessage(String dest, String kind, Object data, TimeStamp t, boolean isMulticast, int multicastSeq, Map<String, Integer> lastSeqFromMembers) {
		super(dest, kind, data);
		
		this.ts = t;
		this.isMulticast = isMulticast;
		this.multicastSeq = multicastSeq;
		this.lastSeqFromMembers = lastSeqFromMembers;
	}

}
