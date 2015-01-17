package cat.urv.imas.behaviour.coordinator;

import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.agent.CoordinatorAgent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.MessageTemplate;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.FIPANames.InteractionProtocol;
import jade.core.AID;

public class DoneBehaviour extends CyclicBehaviour {

    public void action() {
        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchContent(MessageContent.DONE), MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        ACLMessage msg = myAgent.receive(mt);

        if (msg != null) {
            CoordinatorAgent agent = (CoordinatorAgent) this.getAgent();
            GameSettings game = agent.getGame();
            AID sender = msg.getSender();

            if(sender.equals(agent.getHospitalCoordAgent())) {
                agent.log("Message received from hospital: Done");
                agent.setHospitalDone(true);
            }
            else if(sender.equals(agent.getFiremenCoordAgent())) {
                agent.log("Message received from firemen: Done");
                agent.setFiremenDone(true);
            }

            if(agent.getHospitalDone() && agent.getFiremenDone()) {
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
                        Thread.sleep(4000);
                    } catch (Exception e) {
                        agent.errorLog(e.toString());
                    }

                    ACLMessage initialRequest = new ACLMessage(ACLMessage.REQUEST);
                    initialRequest.clearAllReceiver();
                    initialRequest.addReceiver(agent.getCentralAgent());
                    initialRequest.setProtocol(InteractionProtocol.FIPA_REQUEST);
                    try {
                        initialRequest.setContent(MessageContent.GET_MAP);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //we add a behaviour that sends the message and waits for an answer
                    agent.addBehaviour(new RequesterBehaviour(agent, initialRequest));
                }
                else {
                    //we don't send more messages
                    agent.log("Simulation completed");
                }
                agent.removeBehaviour(this);
                /**********************************************************************/
            }
        }
        else {
            block();
        }
    }
}
