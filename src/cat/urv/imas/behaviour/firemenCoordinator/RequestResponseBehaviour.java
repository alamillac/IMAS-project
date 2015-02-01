/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.behaviour.firemenCoordinator;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;


import cat.urv.imas.agent.FiremenCoordinator;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.StreetCell;
import cat.urv.imas.onthology.MessageContent;


/**
 *
 * @author Domen
 */
public class RequestResponseBehaviour extends AchieveREResponder{
    
    public RequestResponseBehaviour(FiremenCoordinator agent, MessageTemplate mt) {
        super(agent, mt);
       
    }
    
   @Override
    protected ACLMessage prepareResponse(ACLMessage msg) {
        FiremenCoordinator agent = (FiremenCoordinator)this.getAgent();
        ACLMessage reply = msg.createReply();
        try {
            Object content = (Object) msg.getContent();
            if (content.equals("qrac")) {
                agent.log("Request received");
                reply.setPerformative(ACLMessage.AGREE);
            }
        } catch (Exception e) {
            reply.setPerformative(ACLMessage.FAILURE);
            agent.errorLog(e.getMessage());
            e.printStackTrace();
        }
        agent.log("Response being prepared");
        return reply;
    }
    
    @Override
    protected ACLMessage prepareResultNotification(ACLMessage msg, ACLMessage response) {

        // it is important to make the createReply in order to keep the same context of
        // the conversation
        FiremenCoordinator agent = (FiremenCoordinator)this.getAgent();
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);

        try {
            reply.setContentObject(agent.getGame());
        } catch (Exception e) {
            reply.setPerformative(ACLMessage.FAILURE);
            agent.errorLog(e.toString());
            e.printStackTrace();
        }
        agent.log("Game settings sent");
        return reply;

    }

    /**
     * No need for any specific action to reset this behaviour
     */
    @Override
    public void reset() {
    }
}
