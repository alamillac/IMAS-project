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

import java.util.Map;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.behaviour.coordinator.RequesterBehaviour;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.utils.MessageType;
import cat.urv.imas.map.BuildingCell;
import cat.urv.imas.map.Cell;
import cat.urv.imas.utils.MessageList;
import jade.core.*;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main Coordinator agent.
 * TODO: This coordinator agent should get the game settings from the Central
 * agent every round and share the necessary information to other coordinators.
 */
public class CoordinatorAgent extends ImasAgent {

    public static final String INITIAL_REQUEST = "INITIAL_REQUEST";
    public static final String LISTEN_REQUEST = "LISTEN_TO_STATE_REQUEST";
    public static final String RECEIVE_MOVMENTS = "RECEIVE_MOVMENTS";
    public static final String SEND_MOVMENTS = "SEND_MOVMENTS";
    public static final String RECEIVE_NEW_INFO = "RECEIVE_NEW_INFO";
    public static final String SEND_NEW_INFO = "SEND_NEW_INFO";
    
    private ArrayList<Cell> movementsList = new ArrayList<>();
    
    /**
     * Game settings in use.
     */
    private GameSettings game;

    /**
     * Central agent id.
     */
    private AID centralAgent;

    /**
     * HospitalCoord agent id.
     */
    private AID hospitalCoord;

    /**
     * FiremenCoord agent id.
     */
    private AID firemenCoord;

    /*
     * Actual step
     */
    private int numStep = 0;

    /*
     * indicate if all the jobs related to hospital are done in the step
     */
    private boolean hospitalDone;

    /*
     * indicate if all the jobs related to firemen are done in the step
     */
    private boolean firemenDone;

    /*
     * New fires on step
     */
    public Map<BuildingCell, Integer> newFires;
    
    private MessageList messageList;
    
    private boolean isStateRequestedByFiremenCoordinator = false;
    private boolean isStateRequestedByHospitalCoordinator = false;
    

    public int getNumStep() {
        return numStep;
    }

    public void setNumStep(int numStep) {
        this.numStep = numStep;
    }

    public AID getCentralAgent() {
        return centralAgent;
    }

    public AID getHospitalCoordAgent() {
        return hospitalCoord;
    }

    public AID getFiremenCoordAgent() {
        return firemenCoord;
    }

    public boolean getHospitalDone() {
        return hospitalDone;
    }

    public boolean getFiremenDone() {
        return firemenDone;
    }

    public void setHospitalDone(boolean done) {
        hospitalDone = done;
    }

    public void setFiremenDone(boolean done) {
        firemenDone = done;
    }

    public Map<BuildingCell, Integer> getNewFires() {
        return newFires;
    }    
    
    /**
     * Builds the coordinator agent.
     */
    public CoordinatorAgent() {
        super(AgentType.COORDINATOR);
        this.messageList = new MessageList(this);
    }

    /**
     * Agent setup method - called when it first come on-line. Configuration of
     * language to use, ontology and initialization of behaviours.
     */
    @Override
    protected void setup() {

        /* ** Very Important Line (VIL) ***************************************/
        this.setEnabledO2ACommunication(true, 1);
        /* ********************************************************************/

        // Register the agent to the DF
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType(AgentType.COORDINATOR.toString());
        sd1.setName(getLocalName());
        sd1.setOwnership(OWNER);

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.addServices(sd1);
        dfd.setName(getAID());
        try {
            DFService.register(this, dfd);
            log("Registered to the DF");
        } catch (FIPAException e) {
            System.err.println(getLocalName() + " registration with DF unsucceeded. Reason: " + e.getMessage());
            doDelete();
        }

        // search CentralAgent
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.CENTRAL.toString());
        this.centralAgent = UtilsAgents.searchAgent(this, searchCriterion);
        // searchAgent is a blocking method, so we will obtain always a correct AID

        // search FiremenCoordinator
        searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.FIREMEN_COORDINATOR.toString());
        this.firemenCoord = UtilsAgents.searchAgent(this, searchCriterion);

        // search HospitalCoordinator
        searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.HOSPITAL_COORDINATOR.toString());
        this.hospitalCoord = UtilsAgents.searchAgent(this, searchCriterion);

        /* ********************************************************************/

        //this.requestCtyStatus();
        
        // Finite State Machine
        FSMBehaviour fsm = new FSMBehaviour(this) {
            public int onEnd() {
                System.out.println("FSM behaviour completed.");
                myAgent.doDelete();
                return super.onEnd();
            }
        };        
        
        fsm.registerFirstState(new RequestStateInfo(), CoordinatorAgent.INITIAL_REQUEST);
        fsm.registerState(new ListenStateInfo(), CoordinatorAgent.LISTEN_REQUEST);
        fsm.registerState(new ReceiveNewStateInfo(), CoordinatorAgent.RECEIVE_NEW_INFO);
        fsm.registerState(new SendNewStateInfo(), CoordinatorAgent.SEND_NEW_INFO);
        fsm.registerState(new ReceiveMovements(), CoordinatorAgent.RECEIVE_MOVMENTS);
        fsm.registerState(new SendMovements(), CoordinatorAgent.SEND_MOVMENTS);
        
        //fsm.registerLastState(new LastState(), CoordinatorAgent.SEND_NEW_INFO);
        
        fsm.registerDefaultTransition(CoordinatorAgent.INITIAL_REQUEST, CoordinatorAgent.LISTEN_REQUEST);
        
        fsm.registerTransition(CoordinatorAgent.LISTEN_REQUEST, CoordinatorAgent.LISTEN_REQUEST, 0);
        fsm.registerTransition(CoordinatorAgent.LISTEN_REQUEST, CoordinatorAgent.LISTEN_REQUEST, 1);
        
        fsm.registerTransition(CoordinatorAgent.LISTEN_REQUEST, CoordinatorAgent.RECEIVE_NEW_INFO, 2);
        fsm.registerDefaultTransition(CoordinatorAgent.RECEIVE_NEW_INFO, CoordinatorAgent.SEND_NEW_INFO);
        fsm.registerDefaultTransition(CoordinatorAgent.SEND_NEW_INFO, CoordinatorAgent.RECEIVE_MOVMENTS);
        fsm.registerTransition(CoordinatorAgent.RECEIVE_MOVMENTS, CoordinatorAgent.RECEIVE_MOVMENTS, 1);
        fsm.registerTransition(CoordinatorAgent.RECEIVE_MOVMENTS, CoordinatorAgent.SEND_MOVMENTS, 2);
        fsm.registerDefaultTransition(CoordinatorAgent.SEND_MOVMENTS, CoordinatorAgent.LISTEN_REQUEST);
        
        this.addBehaviour(fsm);
        // setup finished. When we receive the last inform, the agent itself will add
        // a behaviour to send/receive actions
    }
    
    // CoordinatorAgent Behaviours
    
    protected class RequestStateInfo extends SimpleBehaviour {

        private CoordinatorAgent ca = CoordinatorAgent.this;
        
        private RequestStateInfo() {
            //super(agent);
        }

        @Override
        public void action() {
            log(CoordinatorAgent.INITIAL_REQUEST);
            ACLMessage initialMessage = ca.buildInitialRequest();
            CoordinatorAgent.this.send(initialMessage);
            boolean isInofOk = false;
            while(!isInofOk) {
                ACLMessage msg = ca.messageList.getMessage();
                if(msg != null) {
                    switch(msg.getPerformative()) {
                        case ACLMessage.AGREE:
                            ca.log("AGREE receuved from " + msg.getSender().getLocalName());
                            break;
                        case ACLMessage.INFORM:
                            try {
                                MessageContent mc = (MessageContent)msg.getContentObject();
                                if(mc.getMessageType() == MessageType.INFORM_CITY_STATUS) {
                                    ca.game = (GameSettings)mc.getContent();
                                    isInofOk = true;
                                    ca.log("Game settings info received from " + msg.getSender().getLocalName() );
                                }
                            }
                            catch(Exception ex){
                                ex.printStackTrace();
                            }
                            break;
                        case ACLMessage.FAILURE:
                            ca.log("Faild to receive settings info from " + msg.getSender().getLocalName());
                            break;
                        default:
                            ca.messageList.addMessage(msg);
                    }
                }
            }
            ca.messageList.endRetrieval();
            
        }

        @Override
        public boolean done() {
            return true;
        }

        @Override
        public int onEnd() {
            ca.log(CoordinatorAgent.INITIAL_REQUEST + " DONE!!");
            return super.onEnd(); //To change body of generated methods, choose Tools | Templates.
        }
        
        
        
    }
    
    protected class ListenStateInfo extends SimpleBehaviour {

        private CoordinatorAgent agent = CoordinatorAgent.this;
        
        @Override
        public void action() {
            
            agent.log(CoordinatorAgent.LISTEN_REQUEST);
            
            boolean getMap = false;
            while(!getMap) {
                ACLMessage msg = messageList.getMessage();
                if(msg != null) {
                    try {
                        MessageContent mc = (MessageContent) msg.getContentObject();
                        
                        boolean receive = true;
                        
                        if(!agent.isStateRequestedByFiremenCoordinator &&
                                mc.getMessageType() == MessageType.REQUEST_CITY_STATUS 
                                && msg.getSender().getLocalName().equals(agent.firemenCoord.getLocalName())) {
                            isStateRequestedByFiremenCoordinator = true;
                        }
                        
                        else if(!agent.isStateRequestedByHospitalCoordinator &&
                                mc.getMessageType() == MessageType.REQUEST_CITY_STATUS 
                                && msg.getSender().getLocalName().equals(agent.hospitalCoord.getLocalName())) {
                            isStateRequestedByHospitalCoordinator = true;
                        }
                        else {
                            receive = false;
                        }
                        
                        if(receive) {
                            getMap = true;
                            agent.log("State request from " + msg.getSender().getLocalName() + " received.");
                            
                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.AGREE);
                            agent.send(reply);
                            
                            ACLMessage inf = msg.createReply();
                            inf.setPerformative(ACLMessage.INFORM);
                            
                            try {
                                inf.setContentObject(new MessageContent(MessageType.INFORM_CITY_STATUS, agent.game));
                            }
                            catch(Exception ex) {
                                inf.setPerformative(ACLMessage.FAILURE);
                                agent.log("Faild to inform city status to : " + msg.getSender().getLocalName());
                            }
                            
                            agent.send(inf);
                            
                        }
                        else {
                            messageList.addMessage(msg);
                        }
                    }
                    catch(Exception ex) {
                        messageList.addMessage(msg);
                    }
                }
            }
            
            messageList.endRetrieval();
        }

        @Override
        public boolean done() {
            return true;
        }

        @Override
        public int onEnd() {
            int rt = 0;
            if(agent.isStateRequestedByFiremenCoordinator) {
                rt++;
            }
            if(agent.isStateRequestedByHospitalCoordinator) {
                rt++;
            }
            
            agent.log("" + rt);
            if(rt == 2) {
                agent.isStateRequestedByFiremenCoordinator = agent.isStateRequestedByHospitalCoordinator = false;
            }
            return rt;
        }
        
        
        
    }
    
    protected class ReceiveNewStateInfo extends SimpleBehaviour {

        private CoordinatorAgent ca =  CoordinatorAgent.this;
        
        private void getResponse() {
            boolean isInfoReceivedOk = false;
            while(!isInfoReceivedOk) {
                ACLMessage response = ca.messageList.getMessage();
                if(response != null) {
                    switch(response.getPerformative()) {
                        case ACLMessage.AGREE:
                            ca.log("AGREE received from " + response.getSender().getLocalName());
                            break;
                        case ACLMessage.INFORM:
                            try {
                                MessageContent mc = (MessageContent) response.getContentObject();
                                if(mc.getMessageType() == MessageType.INFORM_NEW_STEP) {
                                    ca.log("New information of state are received from " + response.getSender().getLocalName());
                                    Object[] data = (Object[])mc.getContent();
                                    ca.game = (GameSettings) data[0];
                                    ca.newFires = (HashMap<BuildingCell, Integer>) data[1];
                                    isInfoReceivedOk = true;
                                }
                                else {
                                    ca.messageList.addMessage(response);
                                }
                            }
                            catch(Exception ex) {
                                ex.printStackTrace();
                                ca.messageList.addMessage(response);
                            }
                            break;
                        case ACLMessage.FAILURE:
                            break;
                        default:
                            ca.messageList.addMessage(response);
                    }
                }
            }
            ca.messageList.endRetrieval();
        }        
        
        @Override
        public void action() {
            getResponse();
        }

        @Override
        public boolean done() {
            return true;
        }
        
    }
    
    protected class SendNewStateInfo extends SimpleBehaviour {

        private CoordinatorAgent ca = CoordinatorAgent.this;
        
        @Override
        public void action() {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.clearAllReceiver();
            msg.addReceiver(ca.firemenCoord);
            msg.setProtocol(InteractionProtocol.FIPA_REQUEST);
            try {
                
                msg.setContentObject(new MessageContent(MessageType.INFORM_NEW_STEP, new Object[] {ca.getGame(), ca.getNewFires()/*, Others*/  }));
                
            }
            catch(Exception ex) {
                
            }
            ca.send(msg);
        }

        @Override
        public boolean done() {
            return true;
        }
        
    }
    
    protected class ReceiveMovements extends SimpleBehaviour {

        private CoordinatorAgent ca = CoordinatorAgent.this;
        
        @Override
        public void action() {
            ca.log(FiremenCoordinator.RECEIVE_MOVMENTS);
            boolean isInfoReceived = false;
            while(!isInfoReceived) {
                ACLMessage msg = ca.messageList.getMessage();
                if(msg != null) {
                    switch(msg.getPerformative()) {
                        case ACLMessage.AGREE:
                            ca.log("Received AGREE from " + msg.getSender().getLocalName());
                            break;
                        case ACLMessage.INFORM:
                            try {
                                MessageContent mc = (MessageContent)msg.getContentObject();
                                if(mc.getMessageType() == MessageType.REQUEST_MOVE) {
                                    ca.log("New movement received from " + msg.getSender().getLocalName());
                                    isInfoReceived = true;
                                    //movementsReceivedCount++;
                                    ca.movementsList.add((Cell)mc.getContent());
                                }
                                else {
                                    ca.messageList.addMessage(msg);
                                }
                            }
                            catch(Exception ex) {
                                ex.printStackTrace();
                            }
                            break;
                        case ACLMessage.FAILURE:
                            ca.log("Faild to receive new movement from " + msg.getSender().getLocalName());
                            break;
                        default:
                            ca.messageList.addMessage(msg);
                    }
                }
            }
            ca.messageList.endRetrieval();
        }
        

        @Override
        public boolean done() {
            return true;
        }

        @Override
        public int onEnd() {
           return 2;
        }
        
        
        
    }
    
    protected class SendMovements extends SimpleBehaviour {

        private CoordinatorAgent ca = CoordinatorAgent.this;
        
        @Override
        public void action() {
            ca.log(FiremenCoordinator.FORWARD_MOVMENTS);
            
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.clearAllReceiver();
            msg.addReceiver(ca.centralAgent);
            msg.setProtocol(InteractionProtocol.FIPA_REQUEST);
            try {
                
                msg.setContentObject(new MessageContent(MessageType.REQUEST_MOVE, ca.movementsList));
                ca.movementsList.remove(0);
                
            }
            catch(Exception ex) {
            }
            ca.send(msg);            
        }

        @Override
        public boolean done() {
            return true;
        }
        
    }
    

    /**
     * Update the game settings.
     *
     * @param game current game settings.
     */
    public void setGame(GameSettings game) {
        this.game = game;
    }

    public void setNewFires(Map<BuildingCell, Integer> newFires) {
        log("FIRES: "+Integer.toString(newFires.size()));
        this.newFires = newFires;
    }

    /**
     * Gets the current game settings.
     *
     * @return the current game settings.
     */
    public GameSettings getGame() {
        return this.game;
    }

    public void informHospitalCoordinator() {
        try {
            ACLMessage initialRequest = new ACLMessage(ACLMessage.INFORM);
            initialRequest.addReceiver(hospitalCoord);
            MessageContent mc = new MessageContent(MessageType.INFORM_CITY_STATUS, game);
            initialRequest.setContentObject(mc);
            this.send(initialRequest);
           // log("Request message content:" + initialRequest.getContent());
        } catch (Exception e) {
            log("Unable to inform HSC");
            e.printStackTrace();
        }

    }

    public void informFirmenCoordinator() {
        try {

            ACLMessage initialRequest = new ACLMessage(ACLMessage.INFORM);
            initialRequest.addReceiver(firemenCoord);
            MessageContent mc = new MessageContent(MessageType.INFORM_CITY_STATUS, game);
            initialRequest.setContentObject(mc);
            this.send(initialRequest);
           // log("Request message content:" + initialRequest.getContent());
        } catch (Exception e) {
            log("Unable to inform FMC");
            e.printStackTrace();
        }
    }

    
    protected ACLMessage buildInitialRequest() {
        ACLMessage initialRequest = new ACLMessage(ACLMessage.REQUEST);
        initialRequest.addReceiver(this.centralAgent);
        initialRequest.setProtocol(InteractionProtocol.FIPA_REQUEST);
        MessageContent mc = new MessageContent(MessageType.INITIAL_REQUEST, null);
        try {
            initialRequest.setContentObject(mc);
        } catch (IOException ex) {
            Logger.getLogger(CoordinatorAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        return initialRequest;
    }
    
    public void requestCtyStatus() {
        ACLMessage initialRequest = new ACLMessage(ACLMessage.REQUEST);
        initialRequest.clearAllReceiver();
        initialRequest.addReceiver(this.centralAgent);
        initialRequest.setProtocol(InteractionProtocol.FIPA_REQUEST);

        log("Request message to agent");
        try {
            MessageContent mc = new MessageContent(MessageType.REQUEST_CITY_STATUS, null);
            initialRequest.setContentObject(mc);
            log("Request message content:" + initialRequest.getContent());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //we add a behaviour that sends the message and waits for an answer
        this.addBehaviour(new RequesterBehaviour(this, initialRequest));
    }

}


