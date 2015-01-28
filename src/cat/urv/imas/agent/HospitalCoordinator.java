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
import cat.urv.imas.utils.MessageList;
import cat.urv.imas.utils.MessageType;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.proto.AchieveREInitiator;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.AchieveREResponder;
import jade.wrapper.AgentContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Domen
 */
public class HospitalCoordinator extends ImasAgent{

    
    public static final String REQUEST_STATE = "REQUEST_STATUS";
    public static final String INITIAL_SEND_TO_FIRMEN = "INITIAL_SEND_TO_FIRMEN";
    public static final String RECEIVE_MOVMENTS = "RECEIVE_MOVMENTS";
    public static final String FORWARD_MOVMENTS = "FORWARD_MOVMENTS";
    public static final String SEND_NEW_INFO = "SEND_NEW_INFO";
    public static final String PERFORM_AUCTION = "PERFORM_AUCTION";    
    
    private MessageList messageList;
    
    private ArrayList<Cell> movementsList = new ArrayList<>();    
    
    /**
     * The Coordinator agent with which interacts sharing game settings every
     * round.
     */
    private AID coordinatorAgent;

    /*
     * Game settings in use. So we can get city map 
     */
    private GameSettings game;
    
    
    public HospitalCoordinator() {
        super(AgentType.HOSPITAL_COORDINATOR);
        this.messageList = new MessageList(this);
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

        // Finite State Machine
        FSMBehaviour fsm = new FSMBehaviour(this) {
            public int onEnd() {
                System.out.println("FSM behaviour completed.");
		myAgent.doDelete();
		return super.onEnd();
            }
	};
        
        fsm.registerFirstState(new RequestStateInfo(), HospitalCoordinator.REQUEST_STATE);
        fsm.registerState(new SendInitialState(), HospitalCoordinator.INITIAL_SEND_TO_FIRMEN);
        fsm.registerState(new ReceiveMovement(), HospitalCoordinator.RECEIVE_MOVMENTS);
        fsm.registerState(new SendMovement(), HospitalCoordinator.FORWARD_MOVMENTS);
        fsm.registerState(new SendNewInfo(), HospitalCoordinator.SEND_NEW_INFO);
        fsm.registerState(new PerformAuction(), HospitalCoordinator.PERFORM_AUCTION);
        
        
        fsm.registerTransition(HospitalCoordinator.REQUEST_STATE, HospitalCoordinator.INITIAL_SEND_TO_FIRMEN, 1);
        fsm.registerDefaultTransition(HospitalCoordinator.INITIAL_SEND_TO_FIRMEN, HospitalCoordinator.REQUEST_STATE);
        fsm.registerTransition(HospitalCoordinator.REQUEST_STATE, HospitalCoordinator.SEND_NEW_INFO, 2);
        fsm.registerDefaultTransition(HospitalCoordinator.SEND_NEW_INFO, HospitalCoordinator.PERFORM_AUCTION);
        fsm.registerDefaultTransition(HospitalCoordinator.PERFORM_AUCTION, HospitalCoordinator.RECEIVE_MOVMENTS);
        
        
        fsm.registerTransition(HospitalCoordinator.RECEIVE_MOVMENTS, HospitalCoordinator.RECEIVE_MOVMENTS, 1);
        fsm.registerTransition(HospitalCoordinator.RECEIVE_MOVMENTS, HospitalCoordinator.FORWARD_MOVMENTS, 2);
        fsm.registerTransition(HospitalCoordinator.FORWARD_MOVMENTS, HospitalCoordinator.FORWARD_MOVMENTS, 1);
        fsm.registerTransition(HospitalCoordinator.FORWARD_MOVMENTS, HospitalCoordinator.REQUEST_STATE, 2);
        
        this.addBehaviour(fsm);
        
        /*
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
                                        ServiceDescription searchCriterion = new ServiceDescription();
                                        searchCriterion.setType(AgentType.HOSPITAL.toString());  
                                        Map<AgentType, List<Cell>> a = game.getAgentList();
                                        List<Cell> HOS = a.get(AgentType.HOSPITAL);

                                        int i = 1;
                                        for (Cell h : HOS) {
                                            searchCriterion.setName("hospitalAgent" + i);
                                            initialRequest.addReceiver(UtilsAgents.searchAgent(this.myAgent, searchCriterion));
                                            i++;
                                        }

                                       try {

                                           initialRequest.setContentObject(new MessageContent(MessageType.INFORM_CITY_STATUS, game));
                                          // log("Request message content:" + initialRequest.getContent());
                                       } catch (Exception e) {
                                           e.printStackTrace();
                                       }
                                       this.myAgent.send(initialRequest);                                        
                                        break;
                                    default:
                                        this.block();
                                }

                               
                               //this.send(initialRequest);

                            } catch (UnreadableException ex) {
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
        */
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

    
    //HospitalCoordinator Behaviours
    
    protected class RequestStateInfo extends SimpleBehaviour {

        private HospitalCoordinator hc = HospitalCoordinator.this;
        private boolean isInitialRequest = true;
        
        @Override
        public void action() {
            hc.log(HospitalCoordinator.REQUEST_STATE);
            // Make the request
            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.clearAllReceiver();
            request.addReceiver(hc.coordinatorAgent);
            request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            try {
		request.setContentObject(new MessageContent(MessageType.REQUEST_CITY_STATUS, null));
		hc.send(request);
		hc.log("Requesting game info to " + hc.coordinatorAgent.getLocalName());
            } catch (Exception e) {
		e.printStackTrace();
            }
            
            boolean isInfoOk = false;
            
            while(!isInfoOk) {
                ACLMessage reply = hc.messageList.getMessage();
                if(reply != null) {
                    switch(reply.getPerformative()) {
                        case ACLMessage.AGREE :
                            hc.log("Received AGREE from " + reply.getSender().getLocalName());
                            break;
                        case ACLMessage.INFORM:
                            try {
                                MessageContent mc = (MessageContent) reply.getContentObject();
                                if(mc.getMessageType() == MessageType.INFORM_CITY_STATUS) {
                                    isInfoOk = true;
                                    hc.log("Received Information from " + reply.getSender().getLocalName());
                                    hc.game = (GameSettings)mc.getContent();
                                }
                            }
                            catch(Exception ex) {
                                ex.printStackTrace();
                            }
                            break;
                        case ACLMessage.FAILURE:
                            break;
                        default:
                            hc.messageList.addMessage(reply);
                    }
                }
            }
            hc.messageList.endRetrieval();
        }

        @Override
        public boolean done() {
            return true;
        }

        @Override
        public int onEnd() {
            hc.log(HospitalCoordinator.REQUEST_STATE + " DONE!");
            if(this.isInitialRequest) {
                this.isInitialRequest = false;
                return 1;
            }
            return 2;
        }
        
        
        
    }
    
    protected class SendInitialState extends SimpleBehaviour {

        private HospitalCoordinator hc = HospitalCoordinator.this;
        
        @Override
        public void action() {
            
        }

        @Override
        public boolean done() {
            hc.log(HospitalCoordinator.INITIAL_SEND_TO_FIRMEN + " DONE!!");
            return true;
        }
        
    }

    protected class ReceiveMovement extends SimpleBehaviour {

        private HospitalCoordinator hc = HospitalCoordinator.this;
        private int movementsReceivedCount = 0;
        
        @Override
        public void action() {
            hc.log(HospitalCoordinator.RECEIVE_MOVMENTS);
            boolean isInfoReceived = false;
            while(!isInfoReceived) {
                ACLMessage msg = hc.messageList.getMessage();
                if(msg != null) {
                    switch(msg.getPerformative()) {
                        case ACLMessage.AGREE:
                            hc.log("Received AGREE from " + msg.getSender().getLocalName());
                            break;
                        case ACLMessage.INFORM:
                            try {
                                MessageContent mc = (MessageContent)msg.getContentObject();
                                if(mc.getMessageType() == MessageType.INFORM_NEW_FIRE_MOVE) {
                                    hc.log("New movement received from " + msg.getSender().getLocalName());
                                    isInfoReceived = true;
                                    movementsReceivedCount++;
                                    hc.movementsList.add((Cell)mc.getContent());
                                }
                                else {
                                    hc.messageList.addMessage(msg);
                                }
                            }
                            catch(Exception ex) {
                                ex.printStackTrace();
                            }
                            break;
                        case ACLMessage.FAILURE:
                            hc.log("Faild to receive new movement from " + msg.getSender().getLocalName());
                            break;
                        default:
                            hc.messageList.addMessage(msg);
                    }
                }
            }
            hc.messageList.endRetrieval();
        }

        @Override
        public boolean done() {
            return true;
        }

        @Override
        public int onEnd() {
            if(movementsReceivedCount < hc.game.getAgentList().get(AgentType.FIREMAN).size()) {
                return 1;
            }
            hc.log(HospitalCoordinator.RECEIVE_MOVMENTS + " DONE!!");
            movementsReceivedCount = 0;
            return 2;
        }
        
        
        
    }
    
    protected class SendMovement extends SimpleBehaviour {

        private HospitalCoordinator hc = HospitalCoordinator.this;
        
        @Override
        public void action() {
            
            hc.log(HospitalCoordinator.FORWARD_MOVMENTS);
            
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.clearAllReceiver();
            msg.addReceiver(hc.coordinatorAgent);
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            try {
                
                msg.setContentObject(new MessageContent(MessageType.INFORM_NEW_FIRE_MOVE, hc.movementsList.get(0)));
                hc.movementsList.remove(0);
                
            }
            catch(Exception ex) {
            }
        }

        @Override
        public boolean done() {
            return true;
        }

        @Override
        public int onEnd() {
            if(hc.movementsList.isEmpty()) {
                return 2;
            }
            hc.log(HospitalCoordinator.FORWARD_MOVMENTS + " DONE!!");
            return 1;
        }
        
        
        
    }
    
    protected class SendNewInfo extends SimpleBehaviour {

        private HospitalCoordinator hc = HospitalCoordinator.this;
        
        @Override
        public void action() {
            
        }

        @Override
        public boolean done() {
            hc.log(HospitalCoordinator.SEND_NEW_INFO + " DONE!!");
            return true;
        }
        
    }
    
    protected class PerformAuction extends SimpleBehaviour {

        private HospitalCoordinator hc = HospitalCoordinator.this;
        
        @Override
        public void action() {
            hc.log("Start Auction");
        }

        @Override
        public boolean done() {
            return true;
        }
        
    }    
    
}
