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
package cat.urv.imas.behaviour.coordinator;

import java.util.Map;
import cat.urv.imas.agent.AgentType;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import cat.urv.imas.agent.CoordinatorAgent;
import cat.urv.imas.agent.FiremenCoordinator;
import cat.urv.imas.agent.HospitalCoordinator;
import cat.urv.imas.agent.UtilsAgents;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.behaviour.coordinator.DoneBehaviour;
import cat.urv.imas.utils.MessageType;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames;
import cat.urv.imas.map.BuildingCell;
import java.util.List;

/**
 * Behaviour for the Coordinator agent to deal with AGREE messages.
 * The Coordinator Agent sends a REQUEST for the
 * information of the game settings. The Central Agent sends an AGREE and
 * then it informs of this information which is stored by the Coordinator Agent.
 *
 * NOTE: The game is processed by another behaviour that we add after the
 * INFORM has been processed.
 */
public class RequesterBehaviour extends AchieveREInitiator {

    public RequesterBehaviour(CoordinatorAgent agent, ACLMessage requestMsg) {
        super(agent, requestMsg);
        agent.log("Started behaviour to deal with AGREEs");
    }

    /**
     * Handle AGREE messages
     *
     * @param msg Message to handle
     */
    @Override
    protected void handleAgree(ACLMessage msg) {
        CoordinatorAgent agent = (CoordinatorAgent) this.getAgent();
        agent.log("AGREE received from " + ((AID) msg.getSender()).getLocalName());
    }

    /**
     * Handle INFORM messages
     *
     * @param msg Message
     */
    @Override
    protected void handleInform(ACLMessage msg) {
        CoordinatorAgent agent = (CoordinatorAgent) this.getAgent();
        agent.log("INFORM received from " + ((AID) msg.getSender()).getLocalName());

        /**********************************************************************/
        //Handle msgs
        //send Game to the others coordinators

        try {
            MessageContent mc = (MessageContent)msg.getContentObject();
            switch(mc.getMessageType()) {
                case INFORM_CITY_STATUS:
                    //get Game settings
                    Map<String, Object> stepData = (Map<String, Object>) mc.getContent();

                    GameSettings game = (GameSettings) stepData.get("game");  //delete this?
                    Map<BuildingCell, Integer> newFires = (Map<BuildingCell, Integer>) stepData.get("new_fires");
                    List<BuildingCell> removeFire = (List<BuildingCell>) stepData.get("remove_fire");
                    
                    
                    
                    agent.setNewFires(newFires);
                    agent.setGame(game); 
                    agent.log(game.getShortString());
                    agent.informFirmenCoordinator(removeFire);
                    agent.informHospitalCoordinator();
                    break;
            }
        } catch (Exception e) {
            agent.errorLog("Incorrect content: " + e.toString());
            e.printStackTrace();
        }

        /**********************************************************************/

        agent.addBehaviour(new DoneBehaviour());

    }

    /**
     * Handle NOT-UNDERSTOOD messages
     *
     * @param msg Message
     */
    @Override
    protected void handleNotUnderstood(ACLMessage msg) {
        CoordinatorAgent agent = (CoordinatorAgent) this.getAgent();
        agent.log("This message NOT UNDERSTOOD.");
    }

    /**
     * Handle FAILURE messages
     *
     * @param msg Message
     */
    @Override
    protected void handleFailure(ACLMessage msg) {
        CoordinatorAgent agent = (CoordinatorAgent) this.getAgent();
        agent.log("The action has failed.");

    } //End of handleFailure

    /**
     * Handle REFUSE messages
     *
     * @param msg Message
     */
    @Override
    protected void handleRefuse(ACLMessage msg) {
        CoordinatorAgent agent = (CoordinatorAgent) this.getAgent();
        agent.log("Action refused.");
    }

}
