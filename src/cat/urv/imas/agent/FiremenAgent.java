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
import cat.urv.imas.utils.MessageType;
import cat.urv.imas.utils.NavigatorStatus;
import cat.urv.imas.utils.Utils;
import com.sun.javafx.image.impl.IntArgb;
import com.sun.jmx.snmp.BerDecoder;
 import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.AchieveREResponder;
import java.util.ArrayList;

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



    private AID firemenCoordinator;


    public FiremenAgent() {
        super(AgentType.FIREMAN);
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

        addBehaviour(new CyclicBehaviour(this) {

            @Override
            public void action() {
                //Check if the game agent is shared?
                /*Map<AgentType, List<Cell>> agentList = game.getAgentList();
                List<Cell> privateVehiclesPositions = agentList.get(AgentType.PRIVATE_VEHICLE);
                for(Cell privateVehiclePosition : privateVehiclesPositions) {
                    System.out.println("Check : " + privateVehiclePosition);
                }
                System.out.println("=====================================================");*/
                //
                ACLMessage msg = receive();
                if (msg != null) {
                    try {

                        AID senderID = msg.getSender();
                        MessageContent mc = (MessageContent)msg.getContentObject();
                        if(senderID.equals(firemenCoordinator)) {
                            switch(msg.getPerformative()) {
                                case ACLMessage.REQUEST :
                                    switch(mc.getMessageType()) {
                                        case GO_TO_THIS_FIRE:
                                            mc = (MessageContent)msg.getContentObject();//order from coordinator
                                            Object[] order = (Object[])mc.getContent();
                                            setTargetPosition((Cell)order[0]);
                                            this.myAgent.getAID();
                                            findShortestPath((Cell)order[0]);
                                            if((int)order[1]==0)
                                            {
                                                setStatus(NavigatorStatus.FIRST_WINNER);
                                            }else
                                            {
                                                setStatus(NavigatorStatus.IN_JOB);
                                            }
                                            
                                            setTargetBuilding((Cell)order[2]);
                                            break;
                                        case REQUEST_STEP:
                                            makeStep();
                                           // log("INFORMMMMM");
                                            break;
                                    }
                                    
                                    break;
                                case ACLMessage.CFP :
                                    checkIfFireIsOn();//we check if we can change agent status
                                    responseOnAuction((Map<BuildingCell, Integer>)mc.getContent());
                                    break;
                                case ACLMessage.INFORM :
                                    switch(mc.getMessageType()) {
                                        case INFORM_CITY_STATUS:
                                           // log("INFORMMMMM");
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
    
    private void checkIfFireIsOn() //we check if fire is on -> if not we change status to WAITING
    {
        if(targetBuilding!=null)
        {
            
        Map<BuildingCell, Integer> firemap = game.getFireList();
        
        Cell tmpBuilding = (Cell)targetBuilding;
        BuildingCell Building = (BuildingCell)game.get(tmpBuilding.getRow(), tmpBuilding.getCol());
        
        if(Building.getBurnedRatio()<=0)
        {
            //setTargetBuilding(null);
            //targetBuilding = null;
            setStatus(NavigatorStatus.FREE);
          //  targetPosition = null;
            currentStep = -1;
        }
        
    }
    }
    
    
    
    private void makeStep()
    {
        Cell oldPosition = this.agentPosition;
        String move = moveStep();
        Object[] moveMessage = new Object[4];
        switch(move){
            case "PATH_DONT_EXIST": // this fireman is free
                moveMessage[0] = MessageType.WAITING;
                break;
            case "ON_CELL":
                moveMessage[0] = MessageType.ON_BUILDING;
                moveMessage[1] = oldPosition;
                moveMessage[2] = this.agentPosition;
                moveMessage[3] = this.targetBuilding;
                break;
            case "OK":
                moveMessage[0] = MessageType.MOVE;
                moveMessage[1] = oldPosition;
                moveMessage[2] = this.agentPosition;
                moveMessage[3] = this.targetBuilding;
                break;
        }
        
        ACLMessage response = new ACLMessage(ACLMessage.INFORM);
        response.clearAllReceiver();
        response.addReceiver(firemenCoordinator);

       try {
          response.setContentObject(new MessageContent(MessageType.MAKE_STEP, moveMessage));
           
          // log("Request message content:" + initialRequest.getContent());
       } catch (Exception e) {
           e.printStackTrace();
       }
        
       this.send(response);
           
    }

    private void responseOnAuction(Map<BuildingCell, Integer> tmp)
    {
        BuildingCell bc;
        List<Integer> steps = new ArrayList<>();
        
        if(this.getLocalName().equals("firemenAgent3"))
        {
       log("aaa" +  this.getLocalName());
       }
        for(Entry<BuildingCell, Integer> entry : tmp.entrySet()) // we new fires to temporary map 
        {
            if(this.status!=NavigatorStatus.FIRST_WINNER) //if agent is not first winner 
            {
             bc = entry.getKey();
             Cell tmpCell = (this.findFreeCell((Cell)bc));
             log("AGENT : ROW"+String.valueOf(this.agentPosition.getRow()) + " COL"+String.valueOf(this.agentPosition.getCol()));
             log("BUILDING : ROW"+String.valueOf(this.findFreeCell((Cell)bc).getRow()) + " COL"+String.valueOf(this.findFreeCell((Cell)bc).getCol()));
             Path p = Utils.getShortestPath(this.game.getMap(), this.agentPosition, tmpCell);
             //steps.add((int)findShortestPath((Cell)bc));
             //log(String.valueOf(p.getLength()));
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
        log("aaa");
       this.send(response);
        
    }

    public AID getFiremenCoordinator() {
        return firemenCoordinator;
    }

    public void setFiremenCoordinator(AID firemenCoordinator) {
        this.firemenCoordinator = firemenCoordinator;
    }

}
