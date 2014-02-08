package Lab0;

public class LogicalTimeStamp extends TimeStamp{
	private Integer stamp;
	private Integer increaseRate;
	public LogicalTimeStamp(){
		this.stamp = 0;
		this.increaseRate = 1;
	}
	public LogicalTimeStamp(LogicalTimeStamp l){
		this.stamp = new Integer(l.stamp);
		this.increaseRate = new Integer(l.increaseRate);
	}
	public LogicalTimeStamp(int seed, int increaseRate){
		this.stamp = seed;
		this.increaseRate = increaseRate;
	}
	public void increase(){
		this.stamp += this.increaseRate;
	}
	public void update(LogicalTimeStamp externalStamp){
		if(externalStamp.stamp >= this.stamp){
			this.stamp = externalStamp.stamp;
		}
	}
	public boolean isLess(Object rightOp) {
		LogicalTimeStamp op = (LogicalTimeStamp) rightOp;
		return stamp < op.stamp;
	}
	public boolean isEqual(Object rightOp) {
		LogicalTimeStamp op = (LogicalTimeStamp) rightOp;
		return stamp == op.stamp;
	}

	public boolean isLessOrEqual(Object rightOp) {
		LogicalTimeStamp op = (LogicalTimeStamp) rightOp;
		return stamp <= op.stamp;
	}
	public boolean isConcurrent(Object rightOp) {
		return this.isEqual(rightOp);
	}
	public String toString(){
		return String.valueOf(this.stamp);
	}

}
