/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.utils.MessageType;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 *
 * @author Mohammed
 */
public class FiremenCoordinator extends ImasAgent {
    
    
    /**
     * Game settings in use.
     */
    private GameSettings game;  
    
    
    private AID coordinatorAgent = null;
    
    private List<AID> firemenAgents;
    
    private List<Object[]> endTurnData;
    
    public FiremenCoordinator() {
        super(AgentType.FIREMEN_COORDINATOR);
        
    }

    @Override
    protected void setup() {
        
        super.setup();
        
        //Register the agent to the DF service
        this.registerService(AgentType.FIREMEN_COORDINATOR.toString());
        
        // search CoordinatorAgent
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.COORDINATOR.toString());
        this.coordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);  
        
        this.firemenAgents = new ArrayList<>();

        this.endTurnData = new ArrayList<>();
        
        this.addBehaviour(new CyclicBehaviour() {

            private FiremenCoordinator fc = FiremenCoordinator.this;
            
            @Override
            public void action() {
                ACLMessage msg;
                while((msg = fc.receive()) != null) {
                    switch(msg.getPerformative()) {
                        case ACLMessage.INFORM:
                            handleInform(msg);
                            break;
                        case ACLMessage.SUBSCRIBE:
                            if (msg.getSender().getLocalName().startsWith("fireman")) {
                                AID sender = msg.getSender();
                                firemenAgents.add(sender);
                                log("Fireman added " + sender.getLocalName());
                                if (fc.game != null) {
                                    sendGameStateInfo(sender);
                                }                                 
                            }
                            break;
                    }
                    
                    
                }
                this.block();
                
            }
            
            private void handleInform(ACLMessage msg) {
                try {
                    MessageContent mc = (MessageContent) msg.getContentObject();
                    switch(mc.getMessageType()) {
                        case INFORM_CITY_STATUS:
                            fc.log("Game state INFORM received from " +  msg.getSender().getLocalName());
                            //Update game Info
                            fc.game = (GameSettings) mc.getContent();
                            //Forward game info to firemen
                            this.sendGameStateInfo();
                            //If there is a new fire, start the auction
                            if(fc.game.getNewFire() != null) {
                                ACLMessage cfpMessage = new ACLMessage(ACLMessage.CFP);
                                cfpMessage.setContentObject(new MessageContent(MessageType.NEW_FIRES, game.getNewFire()));
                                fc.addBehaviour(new AuctionManager(fc, cfpMessage));
                            }
                            break;
                        case TURN_IS_DONE:
                            if(msg.getSender().getLocalName().startsWith("fireman")) {
                                if(fc.endTurnData.size() < fc.firemenAgents.size()) {
                                    fc.endTurnData.add((Object[]) mc.getContent());
                                    if(fc.endTurnData.size() == fc.firemenAgents.size()) {
                                        this.sendEndTurn();
                                        fc.endTurnData = new ArrayList<>();
                                    }
                                }
                            }
                            break;
                    }
                }
                catch(Exception ex) {
                    
                }
            }
            
            private void sendGameStateInfo(AID receiver) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(receiver);
                
                try {
                    msg.setContentObject(new MessageContent(MessageType.INFORM_CITY_STATUS, fc.game));
                }
                catch(Exception ex) {
                    ex.printStackTrace();
                    msg.setPerformative(ACLMessage.FAILURE);
                }
                
                fc.send(msg);
            }
            
            private void sendGameStateInfo() {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                fc.firemenAgents.forEach(agent -> {
                    msg.addReceiver(agent);
                });
                
                
                try {
                    msg.setContentObject(new MessageContent(MessageType.INFORM_CITY_STATUS, fc.game));
                }
                catch(Exception ex) {
                    ex.printStackTrace();
                    msg.setPerformative(ACLMessage.FAILURE);
                }
                
                fc.send(msg);         
            }
            
            private void sendEndTurn() {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(fc.coordinatorAgent);
                try {
                    msg.setContentObject(new MessageContent(MessageType.TURN_IS_DONE, fc.endTurnData));
                }
                catch(Exception ex) {
                    msg.setPerformative(ACLMessage.FAILURE);
                }
                fc.send(msg);
                
            }
            
        });
        
    }
    

    private class AuctionManager extends ContractNetInitiator {

        private FiremenCoordinator fc;
        
        public AuctionManager(Agent a, ACLMessage cfp) {
            super(a, cfp);
            fc = (FiremenCoordinator)a;
        }

        @Override
        protected Vector prepareCfps(ACLMessage cfp) {
            
            cfp.clearAllReceiver();
            
            fc.firemenAgents.forEach(agent -> {
                cfp.addReceiver(agent);
            });
            
            Vector v = new Vector();
            v.add(cfp);
            if (fc.firemenAgents.size() > 0)
               fc.log("Sent Call for Proposal to " + fc.firemenAgents.size()+" firemen.");
            return v;            
        }

        @Override
        protected void handleAllResponses(Vector responses, Vector acceptances) {
            ACLMessage[] bestOffers = new ACLMessage[] { null };
            int[] bestBids = {Integer.MAX_VALUE};
            responses.forEach(rsp -> {
                ACLMessage rspMsg = (ACLMessage)rsp;
                if(rspMsg.getPerformative() == ACLMessage.PROPOSE) {
                    try {
                        
                        int bid = Integer.parseInt(rspMsg.getContent());
                        if(bid < bestBids[0] && bid > -1) {
                            bestBids[0] = bid;
                            bestOffers[0] = rspMsg;
                        }
                    }
                    catch(Exception ex) {
                        
                    }
                }
            });
            
            responses.forEach(rsp -> {
                  ACLMessage rspMsg = (ACLMessage)rsp;
                  ACLMessage accept = rspMsg.createReply();
                  if(rspMsg == bestOffers[0]) {
                      accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                      try {
                          accept.setContentObject(new MessageContent(MessageType.GO_TO_THIS_FIRE, null));
                      }
                      catch(Exception ex) {
                          
                      }
                  }
                  else {
                      accept.setPerformative(ACLMessage.REJECT_PROPOSAL);
                  }
                  
                  acceptances.add(accept);
            });
                        
        }

        @Override
        protected void handleInform(ACLMessage inform) {
            super.handleInform(inform); //To change body of generated methods, choose Tools | Templates.
        }
        
        
        
    }
    

}
