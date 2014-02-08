package Lab0;

import java.util.ArrayList;
import java.util.List;

public class ClockTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		int total = 5;
		LogicalClock[] clocks;
		clocks = new LogicalClock[5];
		
		// Initialize
		for(int i = 0; i < total; i++)
			clocks[i] = new LogicalClock();
		
		
		// Increase values
		clocks[4].increase(null);
		clocks[3].increase(null);
		clocks[2].increase(null);
		clocks[1].increase(null);clocks[1].increase(null);clocks[1].increase(null);clocks[1].increase(null);
		clocks[0].increase(null);clocks[0].increase(null);clocks[0].increase(null);clocks[0].increase(null);clocks[0].increase(null);
		
		// Print values
		System.out.println("Clock values: ");
		for(int i = 0; i < total; i++)
			System.out.printf("Clock %d value: %s\n", i, clocks[i].getTime().toString());
		
		// Update
		clocks[4].update(clocks[0].getTime());
		// Clock 4 after updating with a higher value
		System.out.printf("Clock 4 value (after updating with clock 0): %s\n", clocks[4].getTime().toString());
		System.out.println();
		
		// Compare
		for(int i = 1; i < total; i++){
			System.out.printf("Is clock %d less than clock %d?: %b\n", i, i-1, clocks[i].getTime().isLess(clocks[i-1].getTime()));
			System.out.printf("Is clock %d equal than clock %d?: %b\n", i, i-1, clocks[i].getTime().isEqual(clocks[i-1].getTime()));
			System.out.printf("Is clock %d less or equal than clock %d?: %b\n", i, i-1, clocks[i].getTime().isLessOrEqual(clocks[i-1].getTime()));
		}
		System.out.println();
		// Compare
		for(int i = 0; i < total-1; i++){
			System.out.printf("Is clock %d less than clock %d?: %b\n", i, i+1, clocks[i].getTime().isLess(clocks[i+1].getTime()));
			System.out.printf("Is clock %d equal than clock %d?: %b\n", i, i+1, clocks[i].getTime().isEqual(clocks[i+1].getTime()));
			System.out.printf("Is clock %d less or equal than clock %d?: %b\n", i, i+1, clocks[i].getTime().isLessOrEqual(clocks[i+1].getTime()));
		}
		
		/*
		 * Test Vector Clock
		 */
		System.out.println("------- VECTOR CLOCK TEST --------\n");
		List<String> processIds = new ArrayList<String>();
		processIds.add("Alice");
		processIds.add("Bob");
		processIds.add("Charlie");
		
		VectorClock alicesClock = new VectorClock(processIds);
		VectorClock bobsClock = new VectorClock(processIds);
		VectorClock charliesClock = new VectorClock(processIds);
		
		alicesClock.increase("Alice");alicesClock.increase("Alice");alicesClock.increase("Alice");
		bobsClock.increase("Bob");bobsClock.increase("Bob");
		System.out.println("\n------- Increase clocks --------\n");
		// Print the clocks' values 
		System.out.println("Alice's Clock\n" + alicesClock.getTime().toString() + "\n");
		System.out.println("Bob's Clock\n" + bobsClock.getTime().toString() + "\n");
		System.out.println("Charlie's Clock\n" + charliesClock.getTime().toString() + "\n");
		
		// Update Clocks and print values
		System.out.println("\n------- Update Charlie<-Alice, Bob<-Charlie, Alice<-Bob, Alice<-Alice --------\n");
		charliesClock.update(alicesClock.getTime());
		charliesClock.increase("Charlie");
		System.out.println("Charlie's Clock\n" + charliesClock.getTime().toString() + "\n");
		
		bobsClock.update(charliesClock.getTime());
		bobsClock.increase("Bob");
		System.out.println("Bob's Clock\n" + bobsClock.getTime().toString() + "\n");
		
		alicesClock.update(bobsClock.getTime());
		alicesClock.increase("Alice");
		System.out.println("Alice's Clock\n" + alicesClock.getTime().toString() + "\n");
		
		alicesClock.update(alicesClock.getTime());
		alicesClock.increase("Alice");
		System.out.println("Alice's Clock\n" + alicesClock.getTime().toString() + "\n");
		
			
		// Compare
		System.out.println("Alice < Bob? : " + alicesClock.getTime().isLess(bobsClock.getTime()) + "\n");
		
		System.out.println("Bob <= Alice? : " + bobsClock.getTime().isLessOrEqual(alicesClock.getTime()) + "\n");
		System.out.println("Bob = Alice? : " + bobsClock.getTime().isEqual(alicesClock.getTime()) + "\n");
		System.out.println("Bob < Alice? : " + bobsClock.getTime().isLess(alicesClock.getTime()) + "\n");
		
		// (1,2,3) vs (1,9,2)
		VectorTimeStamp t1 = new VectorTimeStamp(processIds);
		VectorTimeStamp t2 = new VectorTimeStamp(processIds);
		t1.increase("Alice");
		t1.increase("Bob");t1.increase("Bob");
		t1.increase("Charlie");t1.increase("Charlie");t1.increase("Charlie");
		System.out.println("\nT1: \n" + t1.toString() + "\n");
		
		t2.increase("Alice");
		t2.increase("Bob");t2.increase("Bob");t2.increase("Bob");t2.increase("Bob");t2.increase("Bob");t2.increase("Bob");
		t2.increase("Bob");t2.increase("Bob");t2.increase("Bob");
		t2.increase("Charlie");t2.increase("Charlie");
		System.out.println("\nT2: \n" + t2.toString() + "\n");
		
		System.out.println("T1 <= T2? : " + t1.isLessOrEqual(t2) + "\n");
		System.out.println("T1 = T2? : " + t1.isEqual(t2) + "\n");
		System.out.println("T1 < T2? : " + t1.isLess(t2) + "\n");
	}

}
