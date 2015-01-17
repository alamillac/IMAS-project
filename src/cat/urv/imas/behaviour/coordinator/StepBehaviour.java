package cat.urv.imas.behaviour.coordinator;

import cat.urv.imas.agent.CoordinatorAgent;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
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
<<<<<<< HEAD
            System.out.println("Message received: new Step");
=======
            try {
                Object content = msg.getContentObject();
                if (content instanceof MessageContent) {
                    MessageContent mc = (MessageContent)content;
                    if(mc.getMessageType().equals(MessageContent.NEW_STEP)) {
                        System.out.println("Message received: new Step");

                        ((CoordinatorAgent)this.myAgent).setGame((GameSettings)mc.getContent());
                    }
                }
                else {
                    block();
                }
            } catch (UnreadableException ex) {
                Logger.getLogger(StepBehaviour.class.getName()).log(Level.SEVERE, null, ex);
            }
            
>>>>>>> 738dc78b4b277c87bc0679e7423e61360dabbb3b
            
        }
        else {
            block();
        }
    }
}
