package Lab0;

import java.io.Serializable;

public abstract class TimeStamp implements Serializable {	
	public TimeStamp(){}
	public abstract boolean isLess(Object rightOp);
	public abstract boolean isEqual(Object rightOp);
	public abstract boolean isLessOrEqual(Object rightOp);
	public abstract boolean isConcurrent(Object rightOp);
}
