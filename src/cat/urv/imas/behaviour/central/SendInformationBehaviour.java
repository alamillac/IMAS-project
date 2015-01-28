/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.behaviour.central;

import cat.urv.imas.agent.CentralAgent;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.utils.MessageType;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.ACLMessage;

/**
 *
 * @author mhj
 */
public class SendInformationBehaviour extends OneShotBehaviour {
    private CentralAgent centralAgent = null;

    public SendInformationBehaviour(Agent a) {
        super(a);
        this.centralAgent = (CentralAgent)a;
    }
    
    
    @Override
    public void action() {
        this.centralAgent.log("Sending new informations to coord agent");				
        
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.clearAllReceiver();
	msg.addReceiver(this.centralAgent.getCoordinatorAgent());
	msg.setProtocol(InteractionProtocol.FIPA_REQUEST);
        try {
            msg.setContentObject(new MessageContent(MessageType.INFORM_NEW_STEP, new Object[] {this.centralAgent.getGame(), this.centralAgent.getNewFires()/*, Others*/  }));
        } catch (Exception e) {
            e.printStackTrace();
        }
					    
        this.centralAgent.send(msg);        
    }
    
}
