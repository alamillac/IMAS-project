package cat.urv.imas.behaviour.coordinator;

import cat.urv.imas.agent.CoordinatorAgent;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.utils.MessageType;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.MessageTemplate;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StepBehaviour extends CyclicBehaviour {

    public void action() {
        //MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchContent(MessageContent.NEW_STEP), MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
        ACLMessage msg = myAgent.receive(mt);
        
        if (msg != null) {

            System.out.println("Message received: new Step");

            try {
                Object content = msg.getContentObject();
                if (content instanceof MessageContent) {
                    MessageContent mc = (MessageContent)content;
                    switch(mc.getMessageType()) {
                        case INFORM_NEW_STEP:
                            System.out.println("Message received: new Step");
                            ((CoordinatorAgent)this.myAgent).setGame((GameSettings)mc.getContent());
                            break;
                        default:
                            block();
                    }
                }
                else {
                    block();
                }
            } catch (UnreadableException ex) {
                Logger.getLogger(StepBehaviour.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else {
            block();
        }
    }
}
