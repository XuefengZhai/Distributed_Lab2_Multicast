package Lab0;

import java.util.List;

public class VectorClock extends Clock{
	private VectorTimeStamp currentTime;
	
	public VectorClock(List<String> processIds){
		currentTime = new VectorTimeStamp(processIds);
	}
	public VectorTimeStamp getTime(){
		return this.currentTime;
	}
	public void increase(String processId){
		currentTime.increase(processId);
	}
	public void update(TimeStamp externalStamp){
		VectorTimeStamp extStamp = (VectorTimeStamp) externalStamp;
		currentTime.update(extStamp);
	}
}
