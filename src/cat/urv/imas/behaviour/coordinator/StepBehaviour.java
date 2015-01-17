package cat.urv.imas.behaviour.coordinator;

import cat.urv.imas.onthology.MessageContent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.MessageTemplate;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class StepBehaviour extends CyclicBehaviour {

    public void action() {
        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchContent(MessageContent.NEW_STEP), MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        ACLMessage msg = myAgent.receive(mt);

        if (msg != null) {
            System.out.println("Message received: new Step");
        }
        else {
            block();
        }
    }
}
