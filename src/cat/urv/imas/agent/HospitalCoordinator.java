/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import static cat.urv.imas.agent.ImasAgent.OWNER;
import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.GameSettings;
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
import jade.wrapper.AgentContainer;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Domen
 */
public class HospitalCoordinator extends ImasAgent{

    public HospitalCoordinator() {
        super(AgentType.HOSPITAL_COORDINATOR);
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
        
        addBehaviour(new CyclicBehaviour(this)
        {
            @Override
            public void action() {
                ACLMessage msg= receive();
                        if (msg!=null){
                            System.out.println( " - " +
                               myAgent.getLocalName() + " <- " + "game settings rrecived");
                               //msg.getContent() );
                    
                            try {
                                GameSettings game = (GameSettings) msg.getContentObject();
                                ACLMessage initialRequest = new ACLMessage(ACLMessage.INFORM);
                                initialRequest.clearAllReceiver();
                                ServiceDescription searchCriterion = new ServiceDescription();
                                searchCriterion.setType(AgentType.HOSPITAL.toString());
                               
                                
                                Map<AgentType, List<Cell>> a = game.getAgentList();
                                List<Cell> HOS = a.get(AgentType.HOSPITAL);
                                
                                int i = 1;
                                for (Cell HOS1 : HOS) {
                                    searchCriterion.setName("hospitalAgent" + i);
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
    }

}
