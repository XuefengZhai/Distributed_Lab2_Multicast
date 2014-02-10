/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Lab0;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWriteMode;

/**
 *
 * @author Songzhe
 */
public class MessagePasser implements Runnable
{ 
    LinkedList<TimeStampedMessage> delayed_send_queue=new LinkedList<TimeStampedMessage>();
    LinkedList<TimeStampedMessage> delayed_received_queue=new LinkedList<TimeStampedMessage>();
    LinkedList<TimeStampedMessage> received_queue=new LinkedList<TimeStampedMessage>();
    LinkedList<TimeStampedMessage> holdback_queue=new LinkedList<TimeStampedMessage>();
    Map<String,Socket> sendingMap;
    ArrayList<Message> send_delayed_queue;
    String conf_file,local_name, clock_type;
    int port_number;
    static int ISN;
    List<Map<String, Object>> conf;
    List<Map<String, Object>> sendRules;
    List<Map<String, Object>> recvRules;
    
    // Multicast specific parameters
    List<Map<String, Object>> groups;
    Map<String, Integer> selfSeqPerGroup;
    Map<String, Map<String, Integer>> maxSeqPerGroupPerMember;
    ArrayList<TimeStampedMessage> multicastRcvQueue; // Queue to hold the last 20 received messages
    
    Object lock = new Object();
    Clock clock;
    
    @SuppressWarnings("unchecked")
	public MessagePasser(String configuration_filename, String local_name, String clock_type) 
            throws FileNotFoundException
    {
        sendingMap = new HashMap<String,Socket>();
        ISN = 0;
        this.conf_file=configuration_filename;
        this.local_name=local_name;
        this.clock_type = clock_type;
        InputStream input = new FileInputStream(new File(conf_file));
        Yaml yaml = new Yaml();
        Map map = (Map) yaml.load(input);
        conf = (List<Map<String, Object>>) map.get("configuration");
        sendRules = (List<Map<String, Object>>) map.get("sendRules");
        recvRules = (List<Map<String, Object>>) map.get("receiveRules");
        
        // Configure multicast groups only on startup
        this.groups =  (List<Map<String, Object>>) map.get("groups");
        
        /* 
         * Initialize multicast parameters
         */
        this.selfSeqPerGroup = new HashMap<String, Integer>();
        this.maxSeqPerGroupPerMember = new HashMap<String, Map<String, Integer>>();
        for(Map<String, Object> group:this.groups){
        	// Initialize the sequence number for this process for each group
        	this.selfSeqPerGroup.put((String)group.get("name"), 0);
        	// Get all the members of the each group, and initialize the max seq. received from them to zero
        	List<String> members = (List<String>) group.get("members");
        	HashMap<String, Integer> maxSeqPerMember = new HashMap<String, Integer>();
        	for(String member:members){
        		maxSeqPerMember.put(member, 0);
        	}
        	String groupName = (String)group.get("name");
        	maxSeqPerGroupPerMember.put(groupName, maxSeqPerMember);
        }
        multicastRcvQueue = new ArrayList<TimeStampedMessage>();
        
        List<String> nameList = new ArrayList<String>();
        
        for (Map<String, Object> config:conf) 
        {
            if (config.get("name").equals(this.local_name)) 
            {
                port_number = (Integer) config.get("port");
            }
            nameList.add((String) config.get("name"));
        }
        
    	switch (clock_type) {
    	case "logical":
    	case "Logical":
    		clock = new LogicalClock();
    		break;
    	case "Vector":
    	case "vector":
    		clock = new VectorClock(nameList);
    		break;
    	default:
    		System.out.println("Wrong clock type");
    	}
        
        Socket_Listener sl = new Socket_Listener(port_number,this);
        Thread worker = new Thread(sl);
        worker.start();
        Thread t=new Thread(this);
        t.start();
    }
    /**
     * sendMulticast - send message to a Multicast group, defined in the config. file 
     * @param groupName
     * @param message
     */
    @SuppressWarnings("unchecked")
	void sendMulticast(String groupName, TimeStampedMessage multiMessage){
      	
    	// Get the group we want to send a message to
    	Map<String, Object> group = null;
    	for(Map<String, Object> groupIt:groups){
    		if (groupIt.containsKey("name") && groupIt.get("name").equals(groupName)){
    			group = groupIt;
    		}
    	}
    	// If no group matches, or the group has no members, return
    	if (group == null || !group.containsKey("members"))
    	{
    		return;
    	}
    	List<String> members = (List<String>) group.get("members");
    	if(members.size() <= 0)
    	{	
    		return;
    	}
    	
    	this.selfSeqPerGroup.put(groupName, this.selfSeqPerGroup.get(groupName) + 1); // Increase local seq for the group
    	System.out.println("Multicast Sequence: " + this.selfSeqPerGroup.get(groupName));
    	
    	for(String member:members){
    		multiMessage.destination = member;
    		// Set message as multicast and assign seq number (group&process specific)
    		multiMessage.isMulticast = true;
    		
    		multiMessage.multicastSeq = this.selfSeqPerGroup.get(groupName);
    		
    		multiMessage.lastSeqFromMembers = this.maxSeqPerGroupPerMember.get(groupName);
    		multiMessage.multicastGroup = groupName;
    		send(multiMessage);
    		
    	}
    	
    	
    }
    void resendMulticast(){
    	
    }
    void send(TimeStampedMessage m1)
    {
    	
    	TimeStampedMessage m = new TimeStampedMessage(m1.destination, m1.kind, m1.data, null, m1.isMulticast, m1.multicastGroup, m1.multicastSeq, m1.lastSeqFromMembers);
        try
        {
            //Code to check rules and send data
            //Use setters of Message class
            m.set_source(local_name);           
            String src = local_name, dest = m.destination, kind = m.kind;
            m.set_seqNum(ISN++);
            int seqNum=m.seqNum;
            m.set_duplicate(false);         
            String action = "send";
            Socket s;
            //System.out.println("About to create socket!");
            if(!sendingMap.containsKey(m.destination))
            {
                String IP = "";
                int port_no = -1;
                for (Map<String, Object> config:conf) 
                {
                        if (config.get("name").equals(m.destination)) 
                        {
                                IP = (String) config.get("ip");
                                port_no = (Integer) config.get("port");
                                break;
                        }
                }
                if(port_no == -1)
                {
                    System.out.println("IP of destination not reachable");
                    return;
                }
                s=new Socket(IP,port_no);
                sendingMap.put(m.destination, s);
            }
            else
            {
                s=sendingMap.get(m.destination);
            }
            
            clock.increase(local_name);
            
            switch (clock_type)
            {
            case "Vector":
            case "vector":
            	m.ts = new VectorTimeStamp((VectorTimeStamp) clock.getTime());
            	break;
            case "logical":
            case "Logical":
            	m.ts = new LogicalTimeStamp((LogicalTimeStamp) clock.getTime());
            	break;
            default:
            	break;
            }
            System.out.println("  timestamp:" + m.ts.toString());
            
            //System.out.println("Socket s initialized");
            ObjectOutputStream oos;
            /* check the rules for sending/recving */
            for (Map<String, Object> rule:sendRules) 
            {
                if (rule.containsKey("src") && !rule.get("src").equals(src) ||
                    rule.containsKey("kind") && !rule.get("kind").equals(kind)||
                    rule.containsKey("dest") && !rule.get("dest").equals(dest)||
                    rule.containsKey("seqNum") && 
                        !rule.get("seqNum").equals(seqNum))
                        continue;
                action = (String) rule.get("action");
                break;
            }       
            //System.out.println(action);
            try
            {
                if(action.equals("drop"))
                {
                    //System.out.println("Message dropped!");
                }
                    
                else if(action.equals("delay"))
                {
                    //System.out.println("Message delayed!");
                    delayed_send_queue.addLast(m);
                }
                    
                else if(action.equals("duplicate"))
                {
                    //System.out.println("Message duplicated!");
                    TimeStampedMessage duplicate_message = new TimeStampedMessage(m.destination,
                            m.kind,m.data, null, m.isMulticast, m.multicastGroup, m.multicastSeq, m.lastSeqFromMembers);
                    switch (clock_type)
                    {
                    case "Vector":
                    case "vector":
                    	duplicate_message.ts = new VectorTimeStamp((VectorTimeStamp) m.ts);
                    	break;
                    case "logical":
                    case "Logical":
                    	duplicate_message.ts = new LogicalTimeStamp((LogicalTimeStamp) m.ts);
                    	break;
                    default:
                    	break;
                    }
                    duplicate_message.set_source(src);
                    duplicate_message.set_seqNum(seqNum);
                    duplicate_message.set_duplicate(true);
                    oos = new ObjectOutputStream(s.getOutputStream());
                    oos.writeObject(m);
                    oos = new ObjectOutputStream(s.getOutputStream());
                    oos.writeObject(duplicate_message);
                    //System.out.println("2 Objects written");
                    while(!delayed_send_queue.isEmpty())
                    {
                        Message delayed = delayed_send_queue.pop();
                        s = sendingMap.get(delayed.destination);
                        oos = new ObjectOutputStream(s.getOutputStream());
                        oos.writeObject(delayed);
                        //System.out.println("1 Delayed message written");
                    }
                }
                    
                else if(action.equals("send"))
                {
                    //System.out.println("Message sent!");
                    oos=new ObjectOutputStream(s.getOutputStream());
                    oos.writeObject(m);
                    //System.out.println("Object written");
                    while(!delayed_send_queue.isEmpty())
                    {
                        Message delayed = delayed_send_queue.pop();
                        s = sendingMap.get(delayed.destination);
                        oos = new ObjectOutputStream(s.getOutputStream());
                        oos.writeObject(delayed);
                        //System.out.println("1 Delayed message written");
                    }
                }
                    
                else
                {
                    System.out.println("Invalid action specified! "
                            + "Cannot handle message");
                    return;
                }
            }
            catch(Exception e)
            {
                System.out.println("Exception in parsing action:" + e);
            }    
        }
        catch(Exception e)
        {
            //System.out.println("Excdeption in sending:" + e);
        	e.printStackTrace();
        }
    }
    
    void processMulticast(TimeStampedMessage m){
    	
    	if (m.kind.equals("NACK"))
    	{
    		System.out.println("processing NACK\n");
    	    	String groupName = m.multicastGroup;
    		String src = m.source;
    		ArrayList<String> data =(ArrayList<String>) m.data;
    		int groupSeq = Integer.parseInt(data.get(0));
    		String sender = data.get(1);
    		
    		
    		for(TimeStampedMessage resendMsg : multicastRcvQueue){
    			if(resendMsg.multicastGroup.equals(groupName) && resendMsg.multicastSeq == groupSeq && resendMsg.source.equals(sender)){
    					resendMsg.destination = src;
    					send(resendMsg);
    			}
    		}
	
    	}
    	else{
    		// Get the max seq number previously received by the current src
    		int maxSeqForGroupForSender = this.maxSeqPerGroupPerMember.get(m.multicastGroup).get(m.source);
    		
    		// If seq number is less than max, we already got that message, discard
    		if(m.multicastSeq <= maxSeqForGroupForSender){
    			return;
    		}
    		
    		// Message is reliable (seq = max + 1)
    		else if(m.multicastSeq == maxSeqForGroupForSender + 1){
    			if(multicastRcvQueue.size() > 20)
    				multicastRcvQueue.remove(0);
    			multicastRcvQueue.add(m);
    			
    			clock.update(m.ts);
            	clock.increase(local_name);
                received_queue.addLast(m);
                this.maxSeqPerGroupPerMember.get(m.multicastGroup).put(m.source, this.maxSeqPerGroupPerMember.get(m.multicastGroup).get(m.source) + 1); 
                // Check holdback queue
                
                 for(TimeStampedMessage checkedMsg: holdback_queue){
    			
                if(checkedMsg.multicastSeq == maxSeqForGroupForSender +1)
    				{
    				clock.update(checkedMsg.ts);
    				clock.increase(local_name);
    				received_queue.add(checkedMsg);
    				}
    			}
    		}
    		
    		
    		// Put in holdback queue and request 
    		else{
    			if(multicastRcvQueue.size() > 20)
    				multicastRcvQueue.remove(0);
    			multicastRcvQueue.add(m);
    			
    			this.holdback_queue.add(m);
    			Collections.sort(this.holdback_queue, new MessageComparator());
    			ArrayList<String> data = new ArrayList<String>();
    			
    			for(int i = maxSeqForGroupForSender + 1; i <  m.multicastSeq; i++){
    				data.add(0, String.valueOf(i));
    				data.add(1, m.source);
    				TimeStampedMessage nack = new TimeStampedMessage(m.multicastGroup, "NACK", data, 
        					this.clock.getTime(), true, m.multicastGroup, m.multicastSeq, null);
    				sendMulticast(m.multicastGroup, nack);
    			}
    		}
    		
    		
    	}
    }
    
    
    TimeStampedMessage receive( )
    {
        TimeStampedMessage message;
        synchronized(lock)
        {
            if(received_queue.isEmpty())
            {
                message = new TimeStampedMessage("NULL","NULL","NULL", null, false, null, -1, null);
            }
            else
            {
                message = (TimeStampedMessage) received_queue.pop();
            }
        }
        
        return message;
    }
    public void run()
    {	
    	try 
        {
		sync_conf();
	} 
        catch (DbxException | IOException e) 
        {
		e.printStackTrace();
	}
    }
    
    @SuppressWarnings("unchecked")
	public void sync_conf() throws DbxException, IOException
    {
	    Yaml yaml = new Yaml();
	    Map map;
        String conf_rev = null;
        FileInputStream input;
        DbxRequestConfig dbxconfig=new DbxRequestConfig("DistributedConf",Locale.getDefault().toString());
        String accessToken = "0MqvvF2iI4YAAAAAAAAAATtitP65UgYcVK2OKGue4CX_Zb4Dd3MC9lBtKi1IKVQc";
        DbxClient client = new DbxClient(dbxconfig, accessToken);
        System.out.println("Linked account: " + client.getAccountInfo().displayName);
        while (true) 
        {
            DbxEntry.WithChildren listing = 
                    client.getMetadataWithChildren("/");
            DbxEntry db_conf = listing.children.get(0);
            String conf_file_rev = db_conf.asFile().rev;
			try{
				Thread.sleep(500);
			} 
	        catch (InterruptedException e){
	        	e.printStackTrace();
			}
			if (!conf_file_rev.equals(conf_rev)){
				
				//System.out.println("Configuration file updated!");
                conf_rev = conf_file_rev;
			
                FileOutputStream outputStream = 
                        new FileOutputStream(conf_file);
                try 
                {
                	client.getFile("/config.yaml", null, outputStream);
                }
                finally 
                {
                    outputStream.close();
                }
			
                input = new FileInputStream(new File(conf_file));
                map = (Map) yaml.load(input);
                input.close();
			
                conf = (List<Map<String, Object>>) map.get("configuration");
                sendRules = (List<Map<String, Object>>)map.get("sendRules");
                recvRules = (List<Map<String, Object>>)map.get("receiveRules");
                
			}
        }
    }
}

class Socket_Listener implements Runnable
{
    int port_number;
    MessagePasser mp1;
    public Socket_Listener(int port_no, MessagePasser mp1)
    {
        this.port_number = port_no;
        this.mp1 = mp1;
    }
    public void run()
    {
        try 
        {
            ServerSocket ss = new ServerSocket(port_number);
            while(true)
            {
                    Socket s=ss.accept();
                    //System.out.println("Thread about to be created");
                    OurRunnable or = new OurRunnable(s,this.mp1);
                    Thread worker = new Thread(or);
                    worker.start();
            }
        }
        catch(Exception e)
        {
            
        }
    }
}

class OurRunnable implements Runnable
{
    Socket s;
    MessagePasser mp1;
    LinkedList<Message> delayed_received_queue;
    LinkedList<Message> received_queue;
    LinkedList<Message> holdback_queue;

    public OurRunnable(Socket ss,MessagePasser mp1)
    {
        s=ss;
        this.mp1=mp1;
    }
    public void run()
    {
        ObjectInputStream ois;
        try
        {
            while(true)
            {
            	
            	
                ois = new ObjectInputStream(s.getInputStream());
                TimeStampedMessage message = (TimeStampedMessage)ois.readObject();
                //System.out.println(message.data);
                String action="receive";
                String src = message.source;
                String kind = message.kind;
                String dest = message.destination;
                int seqNum = message.seqNum;
                
                /* check the rules for sending/receiving */
                for (Map<String, Object> rule:mp1.recvRules) 
                {         
                    if(rule.containsKey("src") && !rule.get("src").equals(src)||
                    rule.containsKey("kind") && !rule.get("kind").equals(kind)||
                    rule.containsKey("dest") && !rule.get("dest").equals(dest)||
                    rule.containsKey("seqNum") &&
                            !rule.get("seqNum").equals(seqNum) ||
                    rule.containsKey("duplicate") && 
                            !rule.get("duplicate").equals(message.duplicate))
                        continue;
                                
                    action = (String) rule.get("action");
                    break;
                }
                
                //System.out.println("Action is:"+action);
                try
                {
                    if(action.equals("drop"))
                    {
                        //System.out.println("Received message dropped!");
                    }

                    else if(action.equals("delay"))
                    {
                        //System.out.println("Received message delayed!");
                        synchronized(mp1.lock)
                        {
                            mp1.delayed_received_queue.addLast(message);
                        }
                    }

                    else if(action.equals("duplicate"))
                    {
                        //System.out.println("Received message duplicated!");
                        TimeStampedMessage duplicate_message = 
                                new TimeStampedMessage(message.destination,
						        message.kind,message.data, message.ts, message.isMulticast, message.multicastGroup, message.multicastSeq, message.lastSeqFromMembers);
                        duplicate_message.set_source(message.source);
                        duplicate_message.set_seqNum(message.seqNum);
                        duplicate_message.set_duplicate(true);
                        synchronized(mp1.lock)
                        {
                            
                            if(message.isMulticast){
                            	mp1.processMulticast(duplicate_message);
                            	mp1.processMulticast(message);
                            }
                            else{
                            	mp1.received_queue.addLast(message);
                                mp1.clock.update(message.ts);
                                mp1.clock.increase(mp1.local_name);
                            	mp1.received_queue.addLast(duplicate_message);
                            	mp1.clock.update(duplicate_message.ts);
                                mp1.clock.increase(mp1.local_name);
                               
                            }
                           
                            while(!mp1.delayed_received_queue.isEmpty())
                            {
                                //System.out.println("One delayed messaged received");
                                TimeStampedMessage m = mp1.delayed_received_queue.pop();
                                if(m.isMulticast){
                                	mp1.processMulticast(m);
                                }
                                else{
                                	mp1.received_queue.addLast(m);
                                }
                                
                            }
                        }
                    }

                    else if(action.equals("receive"))
                    {
                        synchronized(mp1.lock)
                        {
                        	if(message.isMulticast){
                        		mp1.processMulticast(message);
                        	}
                        	else{
	                        	mp1.clock.update(message.ts);
	                        	mp1.clock.increase(mp1.local_name);
	                            mp1.received_queue.addLast(message);
                        	}
                            while(!mp1.delayed_received_queue.isEmpty())
                            {
                                TimeStampedMessage m = (TimeStampedMessage) mp1.delayed_received_queue.pop();
                                if(m.isMulticast){
                            		mp1.processMulticast(m);
                            	}
                                else{
                                	mp1.clock.update(m.ts);
                                	mp1.clock.increase(mp1.local_name);
                                	mp1.received_queue.addLast(m);
                                }
                            }
                        }
                    }
                    else
                    {
                        System.out.println("Invalid action specified! "
                                + "Cannot handle message");
                    }
                }
                catch(Exception e)
                {
                	e.printStackTrace();
                    System.out.println("The receiver has terminated!");
                }
            }
            
            
        } 
        catch (Exception ex) 
        {
            System.out.println("The other user has terminated!");
            System.exit(1);
        }
    }
}
