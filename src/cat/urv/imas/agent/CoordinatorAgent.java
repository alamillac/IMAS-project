/**
 *  IMAS base code for the practical work.
 *  Copyright (C) 2014 DEIM - URV
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cat.urv.imas.agent;

import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.utils.MessageType;
import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The main Coordinator agent. 
 * TODO: This coordinator agent should get the game settings from the Central
 * agent every round and share the necessary information to other coordinators.
 */
public class CoordinatorAgent extends ImasAgent {
    
    public static final String INITIAL_REQUEST_OF_STATE = "INITIAL_REQUEST_OF_STATE";
    public static final String INITIAL_LISTEN_OF_STATE = "INITIAL_LISTEN_OF_STATE";    
    public static final String RECEIVE_NEW_INFO = "RECEIVE_NEW_INFO";
    /**
     * Game settings in use.
     */
    private GameSettings game;
    /**
     * Central agent id.
     */
    private AID centralAgent;
    
    /**
     * HospitalCoordinator agent id.
     */
    private AID hospitalCoordinator;

    /**
     * FiremenCoordinator agent id.
     */
    private AID firemenCoordinator;  
    
    private boolean isTurnEndReceivedFormForemenCoordinator = false;
    private boolean isTurnEndReceivedFormHospitalCoordinator = false;
    
    private List<Object[]> endTurnData = null;

    /**
     * Builds the coordinator agent.
     */
    public CoordinatorAgent() {
        super(AgentType.COORDINATOR);
        endTurnData = new ArrayList<>();
    }

    /**
     * Agent setup method - called when it first come on-line. Configuration of
     * language to use, ontology and initialization of behaviours.
     */
    @Override
    protected void setup() {
        
        super.setup();

        // Register the agent to the DF

        this.registerService(AgentType.COORDINATOR.toString());
        
        // search CentralAgent
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.CENTRAL.toString());
        this.centralAgent = UtilsAgents.searchAgent(this, searchCriterion);
        
        // search HospitalCoordinator
        searchCriterion.setType(AgentType.HOSPITAL_COORDINATOR.toString());
        this.hospitalCoordinator = UtilsAgents.searchAgent(this, searchCriterion);
        // searchAgent is a blocking method, so we will obtain always a correct AID
        
        // search FiremenCoordinator
        searchCriterion.setType(AgentType.FIREMEN_COORDINATOR.toString());
        this.firemenCoordinator = UtilsAgents.searchAgent(this, searchCriterion);
        // searchAgent is a blocking method, so we will obtain always a correct AID        
        
        // searchAgent is a blocking method, so we will obtain always a correct AID

        /* ********************************************************************/
        ACLMessage initialRequest = new ACLMessage(ACLMessage.REQUEST);
        initialRequest.clearAllReceiver();
        initialRequest.addReceiver(this.centralAgent);
        initialRequest.setProtocol(InteractionProtocol.FIPA_REQUEST);
        log("Request message to agent");
        try {
            initialRequest.setContent(MessageContent.GET_MAP);
            log("Request message content:" + initialRequest.getContent());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        this.addBehaviour(new CyclicBehaviour() {

            private CoordinatorAgent ca = CoordinatorAgent.this;
            
            @Override
            public void action() {
                ACLMessage msg = null;
                while((msg = ca.receive()) != null) {
                    switch(msg.getPerformative()) {
                        case ACLMessage.INFORM:
                            try {
                                MessageContent mc = (MessageContent)msg.getContentObject();
                                if(mc.getMessageType() == MessageType.INFORM_CITY_STATUS) {
                                    ca.log("INFORM received from " + msg.getSender().getLocalName());
                                    ca.game = (GameSettings) mc.getContent();
                                    this.forwardGameInfo();
                                }
                                else if(mc.getMessageType() == MessageType.TURN_IS_DONE) {
                                    switch (msg.getSender().getLocalName()) {
                                        case "firemenCoord":
                                            if(!isTurnEndReceivedFormForemenCoordinator) {
                                                endTurnData.addAll((List<Object[]>) mc.getContent());
                                                isTurnEndReceivedFormForemenCoordinator = true;
                                            }
                                            break;
                                        case "hospCoord":
                                            if(!isTurnEndReceivedFormHospitalCoordinator) {
                                                isTurnEndReceivedFormHospitalCoordinator = true;
                                                endTurnData.addAll((List<Object[]>) mc.getContent());
                                            }
                                            break;
                                    }
                                    
                                    
                                    if(isTurnEndReceivedFormForemenCoordinator && isTurnEndReceivedFormHospitalCoordinator) {
                                        this.forwardEndSimulationStepInfo();
                                        ca.endTurnData = new ArrayList<>();
                                        ca.isTurnEndReceivedFormHospitalCoordinator = false;
                                        ca.isTurnEndReceivedFormForemenCoordinator = false;                                        
                                    }
                                }
                            }
                            catch(Exception ex) {
                                
                            }
                            break;
                    }
                }
                block();
                
            }
            
            private void forwardGameInfo() {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(ca.firemenCoordinator);
                msg.addReceiver(ca.hospitalCoordinator);
                try {
                    msg.setContentObject(new MessageContent(MessageType.INFORM_CITY_STATUS, ca.game));
                }
                catch(Exception ex) {
                    
                }
                ca.send(msg);
            }
            
            private void forwardEndSimulationStepInfo() {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(ca.centralAgent);
                msg.setProtocol(InteractionProtocol.FIPA_REQUEST);
                try {
                    msg.setContentObject(new MessageContent(MessageType.TURN_IS_DONE, ca.endTurnData));
                }
                catch(Exception ex) {
                    
                }
                ca.send(msg); 
                

            }
            
        });
       
        

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
