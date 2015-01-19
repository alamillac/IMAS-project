/**
 *  IMAS base code for the practical work.
 *  Copyright (C) 2014 DEIM - URV
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cat.urv.imas.behaviour.navigator;

import cat.urv.imas.agent.AgentType;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import cat.urv.imas.agent.UtilsAgents;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.utils.MessageType;
import cat.urv.imas.agent.NavigatorAgent;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames;

/**
 * Behaviour for the Navigator agent to deal with AGREE messages.
 * The Navigator Agent sends a REQUEST for change his current position.
 * The Central Agent sends an AGREE or a refuse.
 */
public class MovementBehaviour extends AchieveREInitiator {

    public MovementBehaviour(NavigatorAgent agent, ACLMessage requestMsg) {
        super(agent, requestMsg);
        agent.log("Asked a movement");
    }

    /**
     * Handle AGREE messages
     *
     * @param msg Message to handle
     */
    @Override
    protected void handleAgree(ACLMessage msg) {
        NavigatorAgent agent = (NavigatorAgent) this.getAgent();
        //set the new position
        //agent.setAgentPosition();
        agent.log("Movement AGREE received from " + ((AID) msg.getSender()).getLocalName());
    }

    /**
     * Handle INFORM messages
     *
     * @param msg Message
     */
    @Override
    protected void handleInform(ACLMessage msg) {
        NavigatorAgent agent = (NavigatorAgent) this.getAgent();
        agent.log("Movement INFORM received from " + ((AID) msg.getSender()).getLocalName());

        /**********************************************************************/
        //Handle msgs
        //send Game to the others coordinators

        /*
        try {
            MessageContent mc = (MessageContent)msg.getContentObject();
            switch(mc.getMessageType()) {
                case INFORM_CITY_STATUS:
                    //get Game settings
                    GameSettings game = (GameSettings) mc.getContent();
                    agent.setGame(game);
                    agent.log(game.getShortString());
                    agent.informFirmenCoordinator();
                    agent.informHospitalCoordinator();
                    break;
            }
        } catch (Exception e) {
            agent.errorLog("Incorrect content: " + e.toString());
            e.printStackTrace();
        }
        */

        /**********************************************************************/

    }

    /**
     * Handle NOT-UNDERSTOOD messages
     *
     * @param msg Message
     */
    @Override
    protected void handleNotUnderstood(ACLMessage msg) {
        NavigatorAgent agent = (NavigatorAgent) this.getAgent();
        agent.log("This message NOT UNDERSTOOD.");
    }

    /**
     * Handle FAILURE messages
     *
     * @param msg Message
     */
    @Override
    protected void handleFailure(ACLMessage msg) {
        NavigatorAgent agent = (NavigatorAgent) this.getAgent();
        agent.log("The action has failed.");

    } //End of handleFailure

    /**
     * Handle REFUSE messages
     *
     * @param msg Message
     */
    @Override
    protected void handleRefuse(ACLMessage msg) {
        NavigatorAgent agent = (NavigatorAgent) this.getAgent();
        agent.log("Movement refused.");
    }

}
