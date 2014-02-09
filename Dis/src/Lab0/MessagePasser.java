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

public class MessagePasser implements Runnable
{ 
    LinkedList<Message> delayed_send_queue=new LinkedList<Message>();
    LinkedList<Message> delayed_received_queue=new LinkedList<Message>();
    LinkedList<Message> received_queue=new LinkedList<Message>();
    LinkedList<Message> holdback_queue = new LinkedList<Message>();
    Map<String,Socket> sendingMap;
    ArrayList<Message> send_delayed_queue;
    String conf_file,local_name, clock_type;
    int port_number;
    static int ISN;
    List<Map<String, Object>> conf;
    List<Map<String, Object>> sendRules;
    List<Map<String, Object>> recvRules;
    List<Map<String, Object>> groups;
    
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
        groups = (List<Map<String, Object>>) map.get("groups");
        
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
    void send(Message m1)
    {
    	TimeStampedMessage m = new TimeStampedMessage(m1.destination, m1.kind, m1.data, null);
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
        	List<String> members = null;

            //System.out.println("About to create socket!");
            
            
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
            System.out.println(action);
            
            
            if(kind.equals("multicast")){
                for (Map<String, Object> group:groups) 
                {
                	if(group.containsKey("name") && (group.get("name").equals(dest))){
                		if(group.containsKey("members")){
                		members = (List<String>) group.get("members");}
                	}
                	
                	else
                	{
                	continue;
                	}
                }
                
                
                for(String member:members){
                	TimeStampedMessage readyMsg = new TimeStampedMessage(member, kind, m1.data,m.ts);
                    
                    if(!sendingMap.containsKey(readyMsg.destination))
                    {
                        String IP = "";
                        int port_no = -1;
                        for (Map<String, Object> config:conf) 
                        {
                                if (config.get("name").equals(readyMsg.destination)) 
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
                        sendingMap.put(readyMsg.destination, s);
                    }
                    else
                    {
                        s=sendingMap.get(readyMsg.destination);
                    }
                    oos=new ObjectOutputStream(s.getOutputStream());
                    oos.writeObject(readyMsg);
                    
                    
                }
            }
            
            else{
            
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
            
            try
            {
                if(action.equals("drop"))
                {
                    System.out.println("Message dropped!");
                }
                    
                else if(action.equals("delay"))
                {
                    System.out.println("Message delayed!");
                    delayed_send_queue.addLast(m);
                }
                    
                else if(action.equals("duplicate"))
                {
                    System.out.println("Message duplicated!");
                    TimeStampedMessage duplicate_message = new TimeStampedMessage(m.destination,
                            m.kind,m.data, null);
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
                        //("1 Delayed message written");
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
        }
        catch(Exception e)
        {
            System.out.println("Excdeption in sending:" + e);
        }
    }
    
    void sendMulticast(Message m2){
    	
    	/*
    	List<String> members = null;
        for (Map<String, Object> group:groups) 
        {
        	if(group.containsKey("name") && (group.get("name").equals(m2.destination))){
        		if(group.containsKey("members")){
        		members = (List<String>) group.get("members");}
        	}
        	
        	else
        	{
        	continue;
        	}
        }
        
        
        for(String member:members){
            Message readyMsg = new Message(member, m2.kind, m2.data);
            */
        	send(m2);
        //}

    	
    }
    
    TimeStampedMessage receive( )
    {
        TimeStampedMessage message;
        synchronized(lock)
        {
            if(received_queue.isEmpty())
            {
                message = new TimeStampedMessage("NULL","NULL","NULL", null);
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
       
    }
    
    public void sync_conf() 
    {
	    Yaml yaml = new Yaml();
	    Map map;
        String conf_rev = null;
        FileInputStream input;
	
        DbxRequestConfig dbxconfig=new DbxRequestConfig("DistributedConf",
                Locale.getDefault().toString());
        
        /*DbxAppInfo appInfo = new DbxAppInfo("1gim5r5ec6v0ll4", "172qwxwjic68jyc");
        DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(dbxconfig, appInfo);
        String authorizeUrl = webAuth.start();
        System.out.println("1. Go to: " + authorizeUrl);
        System.out.println("2. Click \"Allow\" (you might have to log in first)");
        System.out.println("3. Copy the authorization code.");
        String code = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();
        DbxAuthFinish authFinish = webAuth.finish(code);
        System.out.println(authFinish.accessToken.toString());*/
        
        String accessToken = "0MqvvF2iI4YAAAAAAAAAATtitP65UgYcVK2OKGue4CX_Zb4Dd3MC9lBtKi1IKVQc";
        DbxClient client = new DbxClient(dbxconfig, accessToken);
        //System.out.println("Linked account: " + client.getAccountInfo().displayName);
        //DbxClient client = new DbxClient(dbxconfig, accessToken);
        
//        File inputFile = new File("config.yaml");
//        FileInputStream inputStream = new FileInputStream(inputFile);
//        try {
//            DbxEntry.File uploadedFile = client.uploadFile("/config.yaml",
//                DbxWriteMode.add(), inputFile.length(), inputStream);
//            System.out.println("Uploaded: " + uploadedFile.toString());
//        } finally {
//            inputStream.close();
//        }
        
        while (true) 
        {
            //DbxEntry.WithChildren listing = 
                   // client.getMetadataWithChildren("/");
	
            //DbxEntry db_conf = listing.children.get(0);
		//String conf_file_rev = db_conf.asFile().rev;
			
		try 
                {
                    Thread.sleep(500);
		} 
                catch (InterruptedException e) 
                {
                    e.printStackTrace();
		}
			/*
		if (!conf_file_rev.equals(conf_rev)) 
                {
				
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
                    groups =  (List<Map<String, Object>>) map.get("groups");
                    */
                    /*
                    for (Map<String, Object> group:groups) 
                    {
                    	if(group.containsKey("name"))
                    		System.out.println(group.get("name"));
                    	if(group.containsKey("members")){
                    		List<String> members = (List<String>) group.get("members");
                    		for(String member:members){
                    			System.out.println(member);
                    		}
                    	}
                    }*/
		}
            }
      }
//}

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
                //System.out.println(message.kind);
                
                String action="receive";
                String src = message.source;
                String kind = message.kind;
                String dest = message.destination;
                int seqNum = message.seqNum;
                
                /* check the rules for sending/recving */
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
                
                
                if(kind.equals("multicast"))
                {
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
						        message.kind,message.data, message.ts);
                        duplicate_message.set_source(message.source);
                        duplicate_message.set_seqNum(message.seqNum);
                        duplicate_message.set_duplicate(true);
                        synchronized(mp1.lock)
                        {
                            mp1.received_queue.addLast(message);
                            mp1.clock.update(message.ts);
                            mp1.clock.increase(mp1.local_name);
                            mp1.received_queue.addLast(duplicate_message);
                            mp1.clock.update(duplicate_message.ts);
                            mp1.clock.increase(mp1.local_name);
                            while(!mp1.delayed_received_queue.isEmpty())
                            {
                                //System.out.println("One delayed messaged received");
                                Message m = mp1.delayed_received_queue.pop();
                                mp1.received_queue.addLast(m);
                            }
                        }
                    }

                    else if(action.equals("receive"))
                    {
                        synchronized(mp1.lock)
                        {
                        	mp1.clock.update(message.ts);
                        	mp1.clock.increase(mp1.local_name);
                            mp1.received_queue.addLast(message);
                            while(!mp1.delayed_received_queue.isEmpty())
                            {
                                TimeStampedMessage m = (TimeStampedMessage) mp1.delayed_received_queue.pop();
                                mp1.clock.update(m.ts);
                                mp1.clock.increase(mp1.local_name);
                                mp1.received_queue.addLast(m);
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
                    System.out.println("The receiver has terminated!");
                }
            }
                else //Not Multicast 
                {
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
    						        message.kind,message.data, message.ts);
                            duplicate_message.set_source(message.source);
                            duplicate_message.set_seqNum(message.seqNum);
                            duplicate_message.set_duplicate(true);
                            synchronized(mp1.lock)
                            {
                                mp1.received_queue.addLast(message);
                                mp1.clock.update(message.ts);
                                mp1.clock.increase(mp1.local_name);
                                mp1.received_queue.addLast(duplicate_message);
                                mp1.clock.update(duplicate_message.ts);
                                mp1.clock.increase(mp1.local_name);
                                while(!mp1.delayed_received_queue.isEmpty())
                                {
                                    //System.out.println("One delayed messaged received");
                                    Message m = mp1.delayed_received_queue.pop();
                                    mp1.received_queue.addLast(m);
                                }
                            }
                        }

                        else if(action.equals("receive"))
                        {
                            synchronized(mp1.lock)
                            {
                            	mp1.clock.update(message.ts);
                            	mp1.clock.increase(mp1.local_name);
                                mp1.received_queue.addLast(message);
                                while(!mp1.delayed_received_queue.isEmpty())
                                {
                                    TimeStampedMessage m = (TimeStampedMessage) mp1.delayed_received_queue.pop();
                                    mp1.clock.update(m.ts);
                                    mp1.clock.increase(mp1.local_name);
                                    mp1.received_queue.addLast(m);
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
                        System.out.println("The receiver has terminated!");
                    }                
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