/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import cat.urv.imas.behaviour.firemenCoordinator.RequestResponseBehaviour;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.behaviour.coordinator.RequesterBehaviour;
import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.MessageContent;
import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.*;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Domen
 */
public class FiremenCoordinator extends ImasAgent{
    
    /**
     * Game settings in use.
     */
    private GameSettings game;
    /**
     * Central agent id.
     */
    private AID coordinatorAgent;

    public FiremenCoordinator() {
        super(AgentType.FIREMEN_COORDINATOR);
    }
    
    @Override
    protected void setup() {
        
        /* ** Very Important Line (VIL) ************************************* */
        this.setEnabledO2ACommunication(true, 1);

        // 1. Register the agent to the DF
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType(AgentType.FIREMEN_COORDINATOR.toString());
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
        
        addBehaviour(new CyclicBehaviour(this)
        {
            @Override
            public void action() {
                ACLMessage msg= receive();
                        if (msg!=null){
                            System.out.println( " - " +
                               myAgent.getLocalName() + " <- " );
                              // msg.getContent() );
                        
                            try {
                                GameSettings game = (GameSettings) msg.getContentObject();
                                ACLMessage initialRequest = new ACLMessage(ACLMessage.INFORM);
                                initialRequest.clearAllReceiver();
                                ServiceDescription searchCriterion = new ServiceDescription();
                                searchCriterion.setType(AgentType.FIREMAN.toString());
                               
                                
                                Map<AgentType, List<Cell>> a = game.getAgentList();
                                List<Cell> FIR = a.get(AgentType.FIREMAN);
                                
                                int i = 1;
                                for (Cell FIR1 : FIR) {
                                    searchCriterion.setName("firemenAgent" + i);
                                    initialRequest.addReceiver(UtilsAgents.searchAgent(this.myAgent, searchCriterion));
                                    i++;
                                }
                                
                               try {

                                   initialRequest.setContent("Message recive!!");
                                  // log("Request message content:" + initialRequest.getContent());
                               } catch (Exception e) {
                                   e.printStackTrace();
                               }
                               this.myAgent.send(initialRequest);
                               //this.send(initialRequest);
                            
                            } catch (UnreadableException ex) {
                                Logger.getLogger(HospitalCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            
                        }
            }
            
        }
        );
        
        //MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchProtocol(InteractionProtocol.FIPA_REQUEST), MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        //this.addBehaviour(new RequestResponseBehaviour(this, mt));
        
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
}
