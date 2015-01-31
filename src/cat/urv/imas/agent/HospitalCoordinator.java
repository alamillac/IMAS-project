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
public class HospitalCoordinator extends ImasAgent {

    /**
     * Game settings in use.
     */
    private GameSettings game;  
    
    
    private AID coordinatorAgent = null;
    
    private List<AID> ambulancesAgents;
    
    private List<Object[]> endTurnData;
    
    public HospitalCoordinator() {
        super(AgentType.HOSPITAL_COORDINATOR);
        
    }

    @Override
    protected void setup() {
        
        super.setup();
        
        //Register the agent to the DF service
        this.registerService(AgentType.HOSPITAL_COORDINATOR.toString());
        
        // search CoordinatorAgent
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.COORDINATOR.toString());
        this.coordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);  
        
        this.ambulancesAgents = new ArrayList<>();

        this.endTurnData = new ArrayList<>();
        
        this.addBehaviour(new CyclicBehaviour() {

            private HospitalCoordinator hc = HospitalCoordinator.this;
            
            @Override
            public void action() {
                ACLMessage msg = null;
                while((msg = hc.receive()) != null) {
                    switch(msg.getPerformative()) {
                        case ACLMessage.INFORM:
                            handleInform(msg);
                            break;
                        case ACLMessage.SUBSCRIBE:
                            if (msg.getSender().getLocalName().startsWith("amb")) {
                                AID sender = msg.getSender();
                                ambulancesAgents.add(sender);
                                log("Ambulance added " + sender.getLocalName());
                                if (hc.game != null) {
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
                            hc.log("Game state INFORM received from " +  msg.getSender().getLocalName());
                            hc.game = (GameSettings) mc.getContent();
                            this.sendGameStateInfo();
                            if(hc.game.getNewFire() != null) {
                                ACLMessage cfpMessage = new ACLMessage(ACLMessage.CFP);
                                cfpMessage.setContentObject(new MessageContent(MessageType.NEW_FIRES, game.getNewFire()));
                                //hc.addBehaviour(new AuctionManager(hc, cfpMessage));
                            }
                            break;
                        case TURN_IS_DONE:
                            if(msg.getSender().getLocalName().startsWith("amb")) {
                                if(hc.endTurnData.size() < hc.ambulancesAgents.size()) {
                                    hc.endTurnData.add((Object[]) mc.getContent());
                                    if(hc.endTurnData.size() == hc.ambulancesAgents.size()) {
                                        this.sendEndTurn();
                                        hc.endTurnData = new ArrayList<>();
                                        
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
                    msg.setContentObject(new MessageContent(MessageType.INFORM_CITY_STATUS, hc.game));
                }
                catch(Exception ex) {
                    ex.printStackTrace();
                    msg.setPerformative(ACLMessage.FAILURE);
                }
                
                hc.send(msg);
            }
            
            private void sendGameStateInfo() {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                hc.ambulancesAgents.forEach(agent -> {
                    msg.addReceiver(agent);
                });
                
                
                try {
                    msg.setContentObject(new MessageContent(MessageType.INFORM_CITY_STATUS, hc.game));
                }
                catch(Exception ex) {
                    ex.printStackTrace();
                    msg.setPerformative(ACLMessage.FAILURE);
                }
                
                hc.send(msg);         
            }
            
            private void sendEndTurn() {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(hc.coordinatorAgent);
                try {
                    msg.setContentObject(new MessageContent(MessageType.TURN_IS_DONE, hc.endTurnData));
                }
                catch(Exception ex) {
                    msg.setPerformative(ACLMessage.FAILURE);
                }
                hc.send(msg);
                
            }
            
        });
        
    }
    

    private class AuctionManager extends ContractNetInitiator {

        private HospitalCoordinator hc;
        
        public AuctionManager(Agent a, ACLMessage cfp) {
            super(a, cfp);
            hc = (HospitalCoordinator)a;
        }

        @Override
        protected Vector prepareCfps(ACLMessage cfp) {
            
            cfp.clearAllReceiver();
            
            hc.ambulancesAgents.forEach(agent -> {
                cfp.addReceiver(agent);
            });
            
            Vector v = new Vector();
            v.add(cfp);
            if (hc.ambulancesAgents.size() > 0)
               hc.log("Sent Call for Proposal to "+hc.ambulancesAgents.size()+" ambulances.");
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
