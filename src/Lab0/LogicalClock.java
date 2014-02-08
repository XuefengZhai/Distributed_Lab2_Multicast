package Lab0;

public class LogicalClock extends Clock{
	private LogicalTimeStamp currentTime;
	
	public LogicalClock(){
		currentTime = new LogicalTimeStamp();
	}
	public LogicalTimeStamp getTime(){
		return this.currentTime;
	}
	public void increase(String processId){
		currentTime.increase();
	}
	public void update(TimeStamp externalStamp){
		LogicalTimeStamp stamp = (LogicalTimeStamp) externalStamp;
		currentTime.update(stamp);
	}
}
