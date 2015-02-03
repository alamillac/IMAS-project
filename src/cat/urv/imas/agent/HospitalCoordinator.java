/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import static cat.urv.imas.agent.ImasAgent.OWNER;
import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.utils.MessageType;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.proto.AchieveREInitiator;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.AchieveREResponder;
import jade.proto.ContractNetInitiator;
import jade.wrapper.AgentContainer;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Domen
 */
public class HospitalCoordinator extends ImasAgent{

    /**
     * The Coordinator agent with which interacts sharing game settings every
     * round.
     */
    private AID coordinatorAgent;

    /*
     * Game settings in use. So we can get city map 
     */
    private GameSettings game;
    
    private List<AID> hospitalAgents;
    
    
    public HospitalCoordinator() {
        super(AgentType.HOSPITAL_COORDINATOR);
    }

    /*
     * Inform that it finish the process of the step
     */
    private void informStepCoordinator() {
        ACLMessage stepMsg = new ACLMessage(ACLMessage.INFORM);
        stepMsg.clearAllReceiver();
        stepMsg.addReceiver(this.coordinatorAgent);
        try {
            MessageContent mc = new MessageContent(MessageType.DONE, null);
            stepMsg.setContentObject(mc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        send(stepMsg);
    }

    @Override
    protected void setup() {

        /* ** Very Important Line (VIL) ************************************* */
        this.setEnabledO2ACommunication(true, 1);

        // 1. Register the agent to the DF
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType(AgentType.HOSPITAL_COORDINATOR.toString());
        sd1.setName(getLocalName());
        sd1.setOwnership(OWNER);

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.addServices(sd1);
        dfd.setName(getAID());
        try {
            DFService.register(this, dfd);
            log("Registered to the DF");
        } catch (FIPAException e) {
            System.err.println(getLocalName() + " failed registration to DF [ko]. Reason: " + e.getMessage());
            doDelete();
        }

        // search CoordinatorAgent
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.COORDINATOR.toString());
        this.coordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);

        searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.HOSPITAL.toString());
        this.hospitalAgents = UtilsAgents.searchAgents(this, searchCriterion);
        
        addBehaviour(new CyclicBehaviour(this)
        {
            @Override
            public void action() {
                ACLMessage msg = receive();
                        if (msg!=null){
                            System.out.println( " - " +
                               myAgent.getLocalName() + " <- " + "game settings rrecived");
                               //msg.getContent() );

                            try {
                                MessageContent mc = (MessageContent)msg.getContentObject();
                                switch(mc.getMessageType()) {
                                    case INFORM_CITY_STATUS:
                                        GameSettings game = (GameSettings)mc.getContent();
                                        ACLMessage initialRequest = new ACLMessage(ACLMessage.INFORM);
                                        initialRequest.clearAllReceiver();
                                        
                                        hospitalAgents.forEach(ha ->{
                                            initialRequest.addReceiver(ha);
                                        });
                                        
                                       try {

                                           initialRequest.setContentObject(new MessageContent(MessageType.INFORM_CITY_STATUS, game));
                                          // log("Request message content:" + initialRequest.getContent());
                                       } catch (Exception e) {
                                           e.printStackTrace();
                                       }
                                       this.myAgent.send(initialRequest);                                        
                                       
                                       if(game.isNewFireAppeard()) {
                                           ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                                           cfp.setContentObject(new MessageContent(MessageType.NEW_FIRES, game.getCurrentBuildingFire()));
                                           this.myAgent.addBehaviour(new AuctionManager(myAgent, cfp, game.getCurrentBuildingFire()));
                                       }
                                       
                                        break;
                                    default:
                                        this.block();
                                }

                               
                               //this.send(initialRequest);

                            } catch (UnreadableException ex) {
                                Logger.getLogger(HospitalCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (IOException ex) {
                        Logger.getLogger(HospitalCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                    }



                        ((HospitalCoordinator)myAgent).informStepCoordinator();
                        }
                        else {
                            block();
                        }
            }

        }
        );
    }
    
    /**
     * Update the game settings.
     *
     * @param game current game settings.
     */
    public void setGame(GameSettings game) {
        this.game = game;
    }

    /**
     * Gets the current game settings.
     *
     * @return the current game settings.
     */
    public GameSettings getGame() {
        return this.game;
    }
    
    private class AuctionManager extends ContractNetInitiator {

        
        private HospitalCoordinator hc = HospitalCoordinator.this;
        
        private Cell auctionItem = null;
        
        public AuctionManager(Agent a, ACLMessage cfp, Cell auctionItem) {
            super(a, cfp);
            this.auctionItem = auctionItem;
        }

        @Override
        protected Vector prepareCfps(ACLMessage cfp) {
            cfp.clearAllReceiver();
            
            hc.hospitalAgents.forEach(agent -> {
                cfp.addReceiver(agent);
            });
            
            Vector v = new Vector();
            v.add(cfp);
            if (hc.hospitalAgents.size() > 0)
               hc.log("Sent Call for Proposal to " + hc.hospitalAgents.size()+" hospitals.");
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
                          accept.setContentObject(new MessageContent(MessageType.TAKE_INJURED, this.auctionItem));
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
