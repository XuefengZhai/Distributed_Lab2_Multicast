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
import java.util.Scanner;

/**
 *
 * @author Songzhe
 */
public class TestApplication 
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
            throws FileNotFoundException, IOException 
    {
    	String clock_type = " " ;
        System.out.println("Starting");
        MessagePasser mp = new MessagePasser(args[0],args[1], args[2]);
        Scanner scan=new Scanner(System.in);
        int choice=1;
        BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
        while(choice != 0)
        {
            System.out.println("What do you wanna do? 1.Send 2.Send Multicast 3.Receive 4.What is time? 0.End:");
            choice=scan.nextInt();
            if(choice<0 || choice>3)
                continue;
            switch(choice)
            {
                case 1: 
                    System.out.println("Enter destination:");
                    String d=br.readLine();
                    System.out.println("Enter kind:");
                    String k=br.readLine();
                    System.out.println("Enter data:");
                    String s=br.readLine();
                    Message m=new Message(d,k,(Object)s);
                    //System.out.println("Sending messages:");
                    mp.send(m);
                    break;
                
                case 3:
                    //System.out.println("Receiving message:");
                    TimeStampedMessage mem=mp.receive(); 
                    if(mem.destination.equalsIgnoreCase("NULL"))
                    {
                         System.out.println("No contents received");
                    }
                    else
                    {
                        System.out.println(mem.source+ ":" + mem.data+":"+mem.seqNum);
                        // test
                        System.out.println("  timestamp:" + mem.ts.toString());
                    }
                    break;
                case 4:
                	System.out.println("Time is: " + mp.clock.getTime().toString());
                	break;
                case 0: System.exit(1);
                        break;
                    
                default: continue;
            }
        }
    }
}