package Lab0;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VectorTimeStamp extends TimeStamp{
	private HashMap<String, Integer> stamp;
	private int increaseRate;	
	
	public VectorTimeStamp(int seed, int increaseRate, List<String> processIds){
		stamp = new HashMap<String, Integer>();
		for(int i = 0; i < processIds.size(); i++){
			stamp.put(processIds.get(i), seed);
		}
		this.increaseRate = increaseRate;
	}
	public VectorTimeStamp(VectorTimeStamp v) {
		stamp = new HashMap<String, Integer>(v.stamp);
		increaseRate = v.increaseRate;
	}
	public VectorTimeStamp(List<String> processIds){
		stamp = new HashMap<String, Integer>();
		for(int i = 0; i < processIds.size(); i++){
			stamp.put(processIds.get(i), 0);
		}
		this.increaseRate = 1;
	}
	public void increase(String processId){
		Integer currValue = this.stamp.get(processId);
		this.stamp.put(processId,  currValue + this.increaseRate);
	}
	public void update(Object externalStamp){
		VectorTimeStamp extStamp = (VectorTimeStamp) externalStamp; 
		for(Map.Entry<String, Integer> item : stamp.entrySet()){
			if(extStamp.stamp.containsKey(item.getKey())){
				if(item.getValue() < extStamp.stamp.get(item.getKey())){
					item.setValue(extStamp.stamp.get(item.getKey()));
				}
			}
		}
	}
	
	public boolean isLess(Object rightOp) {
		VectorTimeStamp op = (VectorTimeStamp) rightOp; 
		if(this.isLessOrEqual(op) && !this.isEqual(op))
			return true;
		return false;
	}
	public boolean isEqual(Object rightOp) {
		VectorTimeStamp op = (VectorTimeStamp) rightOp; 
		for(Map.Entry<String, Integer> item : stamp.entrySet()){
			if(op.stamp.containsKey(item.getKey())){
				if(item.getValue() != op.stamp.get(item.getKey())){
					return false;
				}
			}
		}
		return true;
	}

	public boolean isLessOrEqual(Object rightOp) {
		VectorTimeStamp op = (VectorTimeStamp) rightOp; 
		for(Map.Entry<String, Integer> item : stamp.entrySet()){
			if(op.stamp.containsKey(item.getKey())){
				if(!(item.getValue() <= op.stamp.get(item.getKey()))){
					return false;
				}
			}
		}
		return true;
	}
	public boolean isConcurrent(Object rightOp) {
		VectorTimeStamp op = (VectorTimeStamp) rightOp; 
		if(!this.isLess(rightOp) && !op.isLess(this))
			return true;
		return false;
	}
	
	
	
	public String toString(){
		StringBuffer str = new StringBuffer("");
		for(Map.Entry<String, Integer> item : stamp.entrySet()){
			str.append(item.getKey());
			str.append(" : ");
			str.append(item.getValue());
			str.append("\t");
		}
		return str.toString();
	}
}
