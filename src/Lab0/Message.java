/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Lab0;

import java.io.Serializable;

/**
 *
 * @author Songzhe
 */
public class Message implements Serializable 
{ 
    String destination, kind, source;
    int seqNum;
    Object data;
    Boolean duplicate;
    public Message(String dest, String kind, Object data)
    {
        //Build the message
        this.destination=dest;
        this.kind=kind;
        this.data=data;
    }
    // These settors are used by MessagePasser.send, not our app
    public void set_source(String source)
    {
        this.source = source;
    }
    public void set_seqNum(int sequenceNumber)
    {
        //MessagePasser should generate this seq num
        this.seqNum = sequenceNumber;
    }
    public void set_duplicate(Boolean dupe)
    {
        //Used only by the MessagePasser
        this.duplicate = dupe;
    }
}