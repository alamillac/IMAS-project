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
import cat.urv.imas.utils.Utils;
import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.newdawn.slick.util.pathfinding.Path;

/**
 *
 * @author Domen
 */
public class AmbulanceAgent extends NavigatorAgent {

    private int ambulanceLoadingSpeed;

    private int peoplePerAmbulance;

    private Cell ambulanceCell;

    private AID hospitalAgent;
    
    private AID hospitalCoordinator;
    
    public AmbulanceAgent() {
        super(AgentType.AMBULANCE);
    }

    @Override
    protected void setup() {
        super.setup();

        // 1. Register the agent to the DF
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType(AgentType.AMBULANCE.toString());
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
        searchCriterion.setType(AgentType.HOSPITAL_COORDINATOR.toString());
        this.hospitalCoordinator = UtilsAgents.searchAgent(this, searchCriterion);        

        //Set the arguments we get from central agent
        Object[] arg = this.getArguments();
        //this.setGame((GameSettings)arg[1]);
        this.setAmbulanceLoadingSpeed((int)arg[2]);
        this.setPeoplePerAmbulance((int)arg[3]);
        
        this.addBehaviour(new CyclicBehaviour() {

            private AmbulanceAgent aa = AmbulanceAgent.this;
            
            @Override
            public void action() {
                ACLMessage msg = null;
                while((msg = receive()) != null) {
                    if(msg.getSender().getLocalName().startsWith("hospital")) {
                        switch(msg.getPerformative()) {
                            
                            case ACLMessage.INFORM:
                                aa.makeStep();
                                break;
                            
                            case ACLMessage.CFP:
                                
                                ACLMessage reply = msg.createReply();
                                reply.setPerformative(ACLMessage.PROPOSE);
                                
                        
                                try {
                                    MessageContent mc = (MessageContent) msg.getContentObject();
                                    Object[] args = (Object[]) mc.getContent();
                                    Cell targetCell = aa.findFreeCell((Cell)args[0]);
                                    Path pathToBuild = Utils.getShortestPath(aa.game.getMap(), aa.agentPosition, targetCell);
                                    Path pathFromBuildToHospital = Utils.getShortestPath(aa.game.getMap(), targetCell, aa.findFreeCell((Cell)args[1]));
                                    
                                    float bid = -1;
                                    if(pathToBuild != null && pathFromBuildToHospital != null) {
                                        bid = 1 / (float)pathToBuild.getLength() + 1 / (float) pathFromBuildToHospital.getLength();
                                    }
                                    
                                    reply.setContent(bid + "");
                                    
                                    aa.send(reply);
                                    
                                } catch (UnreadableException ex) {
                                    Logger.getLogger(AmbulanceAgent.class.getName()).log(Level.SEVERE, null, ex);
                                }
                        
                                
                                break;
                        }
                    }
                    else {
                        this.block();
                    }
                }
                this.block();
            }
        });

    }
    
    
    private void makeStep() {
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
        response.addReceiver(hospitalCoordinator);

       try {
          response.setContentObject(new MessageContent(MessageType.MAKE_STEP, moveMessage));
           
          // log("Request message content:" + initialRequest.getContent());
       } catch (Exception e) {
           e.printStackTrace();
       }
        
       this.send(response);
           
    }    

    public int getAmbulanceLoadingSpeed() {
        return ambulanceLoadingSpeed;
    }

    public void setAmbulanceLoadingSpeed(int ambulanceLoadingSpeed) {
        this.ambulanceLoadingSpeed = ambulanceLoadingSpeed;
    }

    public int getPeoplePerAmbulance() {
        return peoplePerAmbulance;
    }

    public void setPeoplePerAmbulance(int peoplePerAmbulance) {
        this.peoplePerAmbulance = peoplePerAmbulance;
    }

    public AID getHospitalAgent() {
        return hospitalAgent;
    }

    public void setHospitalAgent(AID hospitalAgent) {
        this.hospitalAgent = hospitalAgent;
    }

}
