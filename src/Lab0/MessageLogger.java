/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Lab0;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static java.sql.Types.NULL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

/**
 *
 * @author Songzhe
 */
public class MessageLogger 
{
	static ArrayList<TimeStampedMessage> arr = new ArrayList<TimeStampedMessage>();
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
            throws FileNotFoundException, IOException 
    {
        System.out.println("Starting");
        MessagePasser mp = new MessagePasser(args[0],args[1], args[2]);
        Scanner scan=new Scanner(System.in);
        int choice=1;
        //BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
        while(choice != 0)
        {
            System.out.println("Logging... Input 1 to see log");
            choice=scan.nextInt();
            if(choice<0 || choice>1)
                continue;
            
            
            switch(choice)
            {   
                case 1:
                    System.out.println("Showing logs:");
                    TimeStampedMessage mem = (TimeStampedMessage) mp.receive();
                    
                    while (!mem.destination.equalsIgnoreCase("NULL"))
                    {
                        arr.add(mem);
                        mem=(TimeStampedMessage) mp.receive();
                    } 
                    
                    // do the sort
                    Collections.sort(arr, new MessageComparator());
                    
                    for (TimeStampedMessage m:arr) {
                    	System.out.println(m.source + ":" + m.seqNum + ": " + m.data);
                    	System.out.println("  timestamp:" + m.ts.toString());
                    	System.out.print("  concurrent messages: ");
                    	// print concurrent messages of every m
                    	for (TimeStampedMessage mm: arr) {
                    		if (!mm.ts.isLess(m.ts) && !m.ts.isLess(mm.ts) && !m.equals(mm))
                    			System.out.print(mm.source+ ":" + mm.seqNum);
                    	}
                    	System.out.println();
                    }
                    break;
                
                case 0:
                	scan.close();
                	System.exit(1);
                    break;
                    
                default:
                	continue;
            }
        }
    }
}