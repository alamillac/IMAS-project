/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.utils;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;

/**
 *
 * @author mhj
 */
public class MessageList {
    
    private ArrayList<ACLMessage> messages = new ArrayList<>();
    private Agent agent;
    private int indexMessage = -1;
    private boolean isInList = false;

    public MessageList(Agent agent) {
        this.agent = agent;
    }
    
    public ACLMessage getMessage() {
        indexMessage++;
        ACLMessage reply;
        isInList = false;
        if(messages.size() <= indexMessage) {
            reply = agent.blockingReceive();
        }
        else {
            reply = messages.get(indexMessage);
            messages.remove(indexMessage);
            isInList = true;
        }
        return reply;
    }
    
    public void addMessage(ACLMessage msg) {
        if(!isInList) {
            messages.add(msg);
        }
        else {
            messages.set(indexMessage, msg);
        }
    }
    
    public void endRetrieval() {
        indexMessage = -1;
        isInList = false;
    }
    
    public int size() {
        return messages.size();
    }
    
}
