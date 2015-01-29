/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;
import static cat.urv.imas.agent.ImasAgent.OWNER;
import cat.urv.imas.map.BuildingCell;
import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.utils.MessageList;
import cat.urv.imas.utils.MessageType;
import cat.urv.imas.utils.NavigatorStatus;
import cat.urv.imas.utils.Utils;
import com.sun.javafx.image.impl.IntArgb;
import com.sun.jmx.snmp.BerDecoder;
 import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.AchieveREResponder;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.Map;
import java.util.Map.Entry;

import java.util.List;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.newdawn.slick.util.pathfinding.Path;
/**
 *
 * @author Domen
 */
public class FiremenAgent extends NavigatorAgent {


    public static final String INITIAL_REQUEST = "INITIAL_REQUEST";
    public static final String REQUEST_STATE_INFO = "REQUEST_STATE_INFO";
    public static final String BID_IN_AUCTION = "BID_IN_AUCTION";
    public static final String PERFORME_MOVE = "PERFORME_MOVE";
    
    private MessageList messageList;
    

    private AID firemenCoordinator;


    public FiremenAgent() {
        super(AgentType.FIREMAN);
        this.messageList = new MessageList(this);
    }
    
    @Override
    protected void setup() {
        super.setup();

        // 1. Register the agent to the DF
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType(AgentType.FIREMAN.toString());
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
        searchCriterion.setType(AgentType.FIREMEN_COORDINATOR.toString());
        this.firemenCoordinator = UtilsAgents.searchAgent(this, searchCriterion);

        FSMBehaviour fsmb = new FSMBehaviour(this) {

            @Override
            public int onEnd() {
                FiremenAgent.this.log("FSM behaviour completed.");
		myAgent.doDelete();                
                return super.onEnd(); //To change body of generated methods, choose Tools | Templates.
            }
            
        };
        
        fsmb.registerFirstState(new InitialRequest(), FiremenAgent.INITIAL_REQUEST);
        fsmb.registerState(new RequestStateInfo(), FiremenAgent.REQUEST_STATE_INFO);
        fsmb.registerState(new BidInAuction(), FiremenAgent.BID_IN_AUCTION);
        fsmb.registerState(new PerformeMove(), FiremenAgent.PERFORME_MOVE);
        
        fsmb.registerDefaultTransition(FiremenAgent.INITIAL_REQUEST, FiremenAgent.BID_IN_AUCTION);
        fsmb.registerDefaultTransition(FiremenAgent.BID_IN_AUCTION, FiremenAgent.PERFORME_MOVE);
        fsmb.registerDefaultTransition(FiremenAgent.PERFORME_MOVE, FiremenAgent.REQUEST_STATE_INFO);
        fsmb.registerDefaultTransition(FiremenAgent.REQUEST_STATE_INFO, FiremenAgent.BID_IN_AUCTION);
        
        this.addBehaviour(fsmb);
        
        /*
        addBehaviour(new CyclicBehaviour(this) {

            @Override
            public void action() {
                //Check if the game agent is shared?
                /*Map<AgentType, List<Cell>> agentList = game.getAgentList();
                List<Cell> privateVehiclesPositions = agentList.get(AgentType.PRIVATE_VEHICLE);
                for(Cell privateVehiclePosition : privateVehiclesPositions) {
                    System.out.println("Check : " + privateVehiclePosition);
                }
                System.out.println("=====================================================");
                //
                ACLMessage msg = receive();
                if (msg != null) {
                    try {

                        AID senderID = msg.getSender();
                        MessageContent mc = (MessageContent)msg.getContentObject();
                        if(senderID.equals(firemenCoordinator)) {
                            switch(msg.getPerformative()) {
                                case ACLMessage.REQUEST :
                                    mc = (MessageContent)msg.getContentObject();//order from coordinator
                                    Object[] order = (Object[])mc.getContent();
                                    setTargetPosition((Cell)order[0]);
                                    if((int)order[1]==0)
                                    {
                                        FiremenAgent.this.setStatus(NavigatorStatus.FIRST_WINNER);
                                    }else
                                    {
                                        FiremenAgent.this.setStatus(NavigatorStatus.IN_JOB);
                                    }
                                    break;
                                case ACLMessage.CFP :
                                    responseOnAuction((Map<BuildingCell, Integer>)mc.getContent());
                                    break;
                                case ACLMessage.INFORM :
                                    switch(mc.getMessageType()) {
                                        case INFORM_CITY_STATUS:
                                            log("INFORMMMMM");
                                            break;
                                    }
                                    break;
                            }


                        }
                    } catch (UnreadableException ex) {
                        Logger.getLogger(FiremenAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    catch(NullPointerException npex) {
                        //npex.printStackTrace();
                    }
                    catch(Exception ex) {

                    }
                }
                else {
                    block();
                }
            }
        });

        */
        //addBehaviour(new CyclicBehaviour(this)
        //{
        //    @Override
        //    public void action() {
        //        ACLMessage msg= receive();
        //                if (msg!=null) {
        //                    System.out.println( " - " +
        //                       myAgent.getLocalName() + " <- " +
        //                       msg.getContent() );
        //                }
        //    }
        //
        //}
        //);

       // addBehaviour(new AchieveREResponder );

    }

    private void responseOnAuction(Map<BuildingCell, Integer> tmp) {
        BuildingCell bc;
        List<Integer> steps = new ArrayList<>();
        for(Entry<BuildingCell, Integer> entry : tmp.entrySet()) // we new fires to temporary map 
        {
            if(this.status!=NavigatorStatus.FIRST_WINNER) //if agent is not first winner 
            {
             bc = entry.getKey();
             this.log(bc.toString());
             Path p = Utils.getShortestPath(this.game.getMap(), this.agentPosition, this.findFreeCell((Cell)bc));
             //steps.add((int)findShortestPath((Cell)bc));
             if(p != null) {
                 steps.add(p.getLength());
             }
             else {
                 //Thats mean the agent can not go to this position, we can add negative value to refer to this 
                 steps.add(-1);
             }
            }else
            {
              steps.add(Integer.MAX_VALUE);  
            }
                 
        }
        
        ACLMessage response = new ACLMessage(ACLMessage.PROPOSE);
        response.clearAllReceiver();
        response.addReceiver(firemenCoordinator);

       try {
               response.setContentObject(new MessageContent(MessageType.AUCTION_PROPOSAL, steps));
          
          // log("Request message content:" + initialRequest.getContent());
       } catch (Exception e) {
           e.printStackTrace();
       }
       
       this.send(response);
        
        
    }

    public AID getFiremenCoordinator() {
        return firemenCoordinator;
    }

    public void setFiremenCoordinator(AID firemenCoordinator) {
        this.firemenCoordinator = firemenCoordinator;
    }
    
    protected class InitialRequest extends SimpleBehaviour {

        private FiremenAgent fa = FiremenAgent.this;
        
        @Override
        public void action() {
            
            //ACLMessage request = new ACLMessage(D_MIN)
            
            boolean isInfoReceivedOk = false;
            
            while(!isInfoReceivedOk) {
                ACLMessage msg = fa.messageList.getMessage();
                if(msg != null) {
                    switch(msg.getPerformative()) {
                        case ACLMessage.AGREE:
                            fa.log("AGREE received from " + msg.getSender().getLocalName());
                            break;
                        case ACLMessage.INFORM:
                            try {
                                MessageContent mc = (MessageContent) msg.getContentObject();
                                if(mc.getMessageType() == MessageType.INFORM_CITY_STATUS) {
                                    fa.log("State Information received from " + msg.getSender().getLocalName());
                                    Object[] data = (Object[]) mc.getContent();
                                    fa.game = (GameSettings)data[1];
                                    fa.agentPosition = (Cell)data[0];
                                    fa.log( fa.agentPosition + " ");
                                    isInfoReceivedOk = true;
                                }
                                else {
                                    fa.messageList.addMessage(msg);
                                }
                            }
                            catch(Exception ex) {
                                ex.printStackTrace();
                            }
                            break;
                        case ACLMessage.FAILURE:
                            fa.log("FAILD to receive information form " + msg.getSender().getLocalName());
                            break;
                        default:
                            fa.messageList.addMessage(msg);
                                
                    }
                }
            }
            fa.messageList.endRetrieval();
        }

        @Override
        public boolean done() {
            return true;
        }
        
    }
    
    protected class RequestStateInfo extends SimpleBehaviour {

        private FiremenAgent fa = FiremenAgent.this;
        
        private void sendRequest() {
            
            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.clearAllReceiver();
            request.addReceiver(fa.firemenCoordinator);
            request.setProtocol(InteractionProtocol.FIPA_REQUEST);
            try {
		request.setContentObject(new MessageContent(MessageType.REQUEST_CITY_STATUS, null));
		send(request);
		fa.log("Requesting new state info to " + fa.firemenCoordinator.getLocalName());
            } catch (Exception e) {
		e.printStackTrace();
            }            
        }
        
        private void getResponse() {
            boolean isInfoReceivedOk = false;
            while(!isInfoReceivedOk) {
                ACLMessage response = fa.messageList.getMessage();
                if(response != null) {
                    switch(response.getPerformative()) {
                        case ACLMessage.AGREE:
                            fa.log("AGREE received from " + response.getSender().getLocalName());
                            break;
                        case ACLMessage.INFORM:
                            try {
                                MessageContent mc = (MessageContent) response.getContentObject();
                                if(mc.getMessageType() == MessageType.INFORM_CITY_STATUS) {
                                    fa.log("State new information received from " + response.getSender().getLocalName());
                                    Object[] data = (Object[])mc.getContent();
                                    fa.game = (GameSettings) data[1];
                                    isInfoReceivedOk = true;
                                }
                                else {
                                    fa.messageList.addMessage(response);
                                }
                            }
                            catch(Exception ex) {
                                ex.printStackTrace();
                                fa.messageList.addMessage(response);
                            }
                            break;
                        case ACLMessage.FAILURE:
                            break;
                        default:
                            fa.messageList.addMessage(response);
                    }
                }
            }
            fa.messageList.endRetrieval();
        }
        
        @Override
        public void action() {
            //this.sendRequest();
            this.getResponse();
            
        }

        @Override
        public boolean done() {
             return true;
        }
        
    }    
    
    protected class BidInAuction extends SimpleBehaviour {

        private FiremenAgent fa = FiremenAgent.this;
        
        @Override
        public void action() {
            
            boolean isAuctionInfoReceived = false;
            boolean isThereAuctionInfo = true;
            HashMap<BuildingCell, Integer> auctionInfo = null;
            while(!isAuctionInfoReceived && isThereAuctionInfo) {
                ACLMessage msg = fa.messageList.getMessage();
                
                if(msg != null) {
                    switch(msg.getPerformative()) {
                        case ACLMessage.CFP:
                            try {
                                MessageContent mc = (MessageContent) msg.getContentObject();
                                if(mc != null) {
                                    auctionInfo = (HashMap<BuildingCell, Integer>) mc.getContent();
                                    if(auctionInfo == null) {
                                        fa.log("There is no fires");
                                        isThereAuctionInfo = false;
                                    }
                                    else {
                                        fa.log("receive new fires");
                                        isAuctionInfoReceived = true;
                                    }
                                }
                            }
                            catch(Exception ex) {
                                fa.messageList.addMessage(msg);
                                ex.printStackTrace();
                            }
                            break;
                        case ACLMessage.FAILURE:
                            fa.log("Failure to recive auction information ");
                            break;
                        default:
                            fa.messageList.addMessage(msg);
                    }
                }
            }
            
            fa.messageList.endRetrieval();
            if(isThereAuctionInfo) {
                fa.responseOnAuction(auctionInfo);
                fa.targetPosition = auctionInfo.keySet().iterator().next();
                boolean isWinnerReceived = false;
                
                while(!isWinnerReceived) {
                    ACLMessage winnerMsg = fa.messageList.getMessage();
                    if(winnerMsg != null) {
                        switch(winnerMsg.getPerformative()) {
                            case ACLMessage.AGREE:
                                fa.log("bid AGREE received from " + winnerMsg.getSender().getLocalName());
                                break;
                            case ACLMessage.INFORM:
                                try {
                                    MessageContent mc = (MessageContent) winnerMsg.getContentObject();
                                    if(mc != null) {
                                        if(mc.getMessageType() == MessageType.GO_TO_THIS_FIRE) {
                                            Object[] order = (Object[]) mc.getContent();
                                            fa.targetPosition = (Cell) order[0];
                                            if((int) order[1] == 0) {
                                                fa.status = NavigatorStatus.FIRST_WINNER;
                                                fa.findShortestPath();
                                            }
                                            else {
                                                fa.status = NavigatorStatus.IN_JOB;
                                            } 
                                            isWinnerReceived = true;
                                        }
                                        else {
                                            fa.messageList.addMessage(winnerMsg);
                                        }
                                    }
                                    else {
                                        fa.messageList.addMessage(winnerMsg);
                                    }
                                }
                                
                                catch(Exception ex) {
                                    fa.messageList.addMessage(winnerMsg);
                                    ex.printStackTrace();
                                }
                                break;
                        }
                    }
                }
                
                fa.messageList.endRetrieval();
            }
            
        }

        @Override
        public boolean done() {
            return true;
        }
        
    }    
    
    protected class PerformeMove extends SimpleBehaviour {

        private FiremenAgent fa = FiremenAgent.this;
        
        @Override
        public void action() {
            
            
            String step = fa.moveStep();
            switch (step) {
                case "OK":    // fireman can make a new step, we send new location to coordinator
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.addReceiver(fa.firemenCoordinator);
                    try {
                        msg.setContentObject(new MessageContent(MessageType.REQUEST_MOVE, fa.agentPosition));
                    }
                    catch(Exception ex) {
                        ex.printStackTrace();
                        msg.setPerformative(ACLMessage.FAILURE);
                    }
                    fa.send(msg);
                    fa.log("Sending new position to " + fa.firemenCoordinator.getLocalName());
                         break;
                case "ON_CELL":  // wen need to send that fireman in on fire so system agent can reduce fire for 5 %
                        //TO DO 
                        break;
                case "PATH_DONT_EXIST":  // almost imposible, but if this happens we need to wait one step 
                         break;
                default: 
                         break;
            
            }
        }

        @Override
        public boolean done() {
             return true;
        }
        
    }    
    
    

}
