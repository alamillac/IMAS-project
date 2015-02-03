package cat.urv.imas.behaviour.coordinator;

import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.agent.CoordinatorAgent;
import cat.urv.imas.utils.MessageType;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.MessageTemplate;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.FIPANames.InteractionProtocol;
import jade.core.AID;
import jade.lang.acl.UnreadableException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DoneBehaviour extends CyclicBehaviour {

    private Map<AID, Object[]> firemanMove;
    private Map<AID, Object[]> ambMove;
    private Map<AID, Object[]> moves;
    
    
    public void action() {
        MessageTemplate mt =  MessageTemplate.MatchPerformative(ACLMessage.INFORM);
        ACLMessage msg = myAgent.receive(mt);

        
        
        if (msg != null) {
            MessageContent mc = null;
            try {
                mc = (MessageContent) msg.getContentObject();
            } catch (UnreadableException ex) {
                Logger.getLogger(DoneBehaviour.class.getName()).log(Level.SEVERE, null, ex);
            }
            if(mc != null && mc.getMessageType() == MessageType.DONE) {
                CoordinatorAgent agent = (CoordinatorAgent) this.getAgent();
                GameSettings game = agent.getGame();
                AID sender = msg.getSender();

                if(sender.equals(agent.getHospitalCoordAgent())) {
                    agent.log("Message received from hospital: Done");
                    if(mc.getContent()!= null) // because in initial state we send null
                    {
                        ambMove = new HashMap<>();
                        ambMove = (Map<AID, Object[]>)mc.getContent();
                        
                    }else
                    {
                        ambMove = null;
                    }                    
                    agent.setHospitalDone(true);
                }
                else if(sender.equals(agent.getFiremenCoordAgent())) {
                    agent.log("Message received from firemen: Done");
                    
                    if(mc.getContent()!= null) // because in initial state we send null
                    {
                        firemanMove = new HashMap<>();
                        firemanMove = (Map<AID, Object[]>)mc.getContent();
                        
                    }else
                    {
                        firemanMove = null;
                    }
                    agent.setFiremenDone(true);
                }


                if(agent.getHospitalDone() && agent.getFiremenDone()) {
                    agent.setHospitalDone(false);
                    agent.setFiremenDone(false);

                    /**********************************************************************/
                    //Set a newBehaviour until steps are done
                    //This message will be send to central agent asking to update the game

                    int maxSteps = game.getSimulationSteps();
                    int numStep = agent.getNumStep();

                    if(numStep < maxSteps) {
                        agent.log("Step " + Integer.toString(numStep));
                        numStep ++;
                        agent.setNumStep(numStep);

                        //delay
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {
                            agent.errorLog(e.toString());
                        }

                        
                        
                        agent.requestCtyStatus(firemanMove, ambMove);
                    }
                    else {
                        //we don't send more messages
                        agent.log("Simulation completed");
                    }
                    agent.removeBehaviour(this);
                    /**********************************************************************/
                }
            }
        }
        else {
            block();
        }
    }
}
