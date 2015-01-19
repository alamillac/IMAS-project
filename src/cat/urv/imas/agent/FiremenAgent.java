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
 import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.AchieveREResponder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
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
                ACLMessage msg = receive();
                if (msg != null) {
                    try {

                        AID senderID = msg.getSender();
                        MessageContent mc = (MessageContent)msg.getContentObject();
                        if(senderID.equals(firemenCoordinator)) {
                            switch(msg.getPerformative()) {
                                case ACLMessage.PROPOSE :
                                    break;
                                case ACLMessage.CFP :
                                    Map<BuildingCell, Integer> tmp = (Map<BuildingCell, Integer>)mc.getContent();
                                    BuildingCell bc;
                                    
                                    for(Entry<BuildingCell, Integer> entry : tmp.entrySet()) // we new fires to temporary map 
                                    {
                                         bc = entry.getKey();
                                    }
                                    log("NEW FIREEEEEEEEEEEE");
                                    
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


    public AID getFiremenCoordinator() {
        return firemenCoordinator;
    }

    public void setFiremenCoordinator(AID firemenCoordinator) {
        this.firemenCoordinator = firemenCoordinator;
    }

}
