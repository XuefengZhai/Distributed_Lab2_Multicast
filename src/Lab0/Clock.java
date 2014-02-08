package Lab0;


public abstract class Clock {
	public abstract TimeStamp getTime();
	public abstract void increase(String processId);
	public abstract void update(TimeStamp externalStamp);
}
