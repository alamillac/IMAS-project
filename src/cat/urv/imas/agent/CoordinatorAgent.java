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
package cat.urv.imas.agent;

import java.util.Map;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.behaviour.coordinator.RequesterBehaviour;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.utils.MessageType;
import cat.urv.imas.map.BuildingCell;
import jade.core.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.*;
import java.util.HashMap;
import java.util.List;

/**
 * The main Coordinator agent.
 * TODO: This coordinator agent should get the game settings from the Central
 * agent every round and share the necessary information to other coordinators.
 */
public class CoordinatorAgent extends ImasAgent {

    /**
     * Game settings in use.
     */
    private GameSettings game;

    /**
     * Central agent id.
     */
    private AID centralAgent;

    /**
     * HospitalCoord agent id.
     */
    private AID hospitalCoord;

    /**
     * FiremenCoord agent id.
     */
    private AID firemenCoord;

    /*
     * Actual step
     */
    private int numStep = 0;

    /*
     * indicate if all the jobs related to hospital are done in the step
     */
    private boolean hospitalDone;

    /*
     * indicate if all the jobs related to firemen are done in the step
     */
    private boolean firemenDone;

    /*
     * New fires on step
     */
    public Map<BuildingCell, Integer> newFires;

    public int getNumStep() {
        return numStep;
    }

    public void setNumStep(int numStep) {
        this.numStep = numStep;
    }

    public AID getCentralAgent() {
        return centralAgent;
    }

    public AID getHospitalCoordAgent() {
        return hospitalCoord;
    }

    public AID getFiremenCoordAgent() {
        return firemenCoord;
    }

    public boolean getHospitalDone() {
        return hospitalDone;
    }

    public boolean getFiremenDone() {
        return firemenDone;
    }

    public void setHospitalDone(boolean done) {
        hospitalDone = done;
    }

    public void setFiremenDone(boolean done) {
        firemenDone = done;
    }

    /**
     * Builds the coordinator agent.
     */
    public CoordinatorAgent() {
        super(AgentType.COORDINATOR);
    }

    /**
     * Agent setup method - called when it first come on-line. Configuration of
     * language to use, ontology and initialization of behaviours.
     */
    @Override
    protected void setup() {

        /* ** Very Important Line (VIL) ***************************************/
        this.setEnabledO2ACommunication(true, 1);
        /* ********************************************************************/

        // Register the agent to the DF
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType(AgentType.COORDINATOR.toString());
        sd1.setName(getLocalName());
        sd1.setOwnership(OWNER);

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.addServices(sd1);
        dfd.setName(getAID());
        try {
            DFService.register(this, dfd);
            log("Registered to the DF");
        } catch (FIPAException e) {
            System.err.println(getLocalName() + " registration with DF unsucceeded. Reason: " + e.getMessage());
            doDelete();
        }

        // search CentralAgent
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.CENTRAL.toString());
        this.centralAgent = UtilsAgents.searchAgent(this, searchCriterion);
        // searchAgent is a blocking method, so we will obtain always a correct AID

        // search FiremenCoordinator
        searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.FIREMEN_COORDINATOR.toString());
        this.firemenCoord = UtilsAgents.searchAgent(this, searchCriterion);

        // search HospitalCoordinator
        searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.HOSPITAL_COORDINATOR.toString());
        this.hospitalCoord = UtilsAgents.searchAgent(this, searchCriterion);

        /* ********************************************************************/

        this.requestCtyStatus(null, null);
        // setup finished. When we receive the last inform, the agent itself will add
        // a behaviour to send/receive actions
    }

    /**
     * Update the game settings.
     *
     * @param game current game settings.
     */
    public void setGame(GameSettings game) {
        this.game = game;
    }

    public void setNewFires(Map<BuildingCell, Integer> newFires) {
        log("FIRES: "+Integer.toString(newFires.size()));
        this.newFires = newFires;
    }

    /**
     * Gets the current game settings.
     *
     * @return the current game settings.
     */
    public GameSettings getGame() {
        return this.game;
    }

    public void informHospitalCoordinator() {
        try {
            ACLMessage initialRequest = new ACLMessage(ACLMessage.INFORM);
            initialRequest.addReceiver(hospitalCoord);
            MessageContent mc = new MessageContent(MessageType.INFORM_CITY_STATUS, game);
            initialRequest.setContentObject(mc);
            this.send(initialRequest);
           // log("Request message content:" + initialRequest.getContent());
        } catch (Exception e) {
            log("Unable to inform HSC");
            e.printStackTrace();
        }

    }

    public void informFirmenCoordinator(List<BuildingCell> removeFire) {
        try {
            Object[] messageToFiremen= new Object[3]; //game, removefire, and burned buildings
            messageToFiremen[0] = game;
            messageToFiremen[1] = removeFire;
            ACLMessage initialRequest = new ACLMessage(ACLMessage.INFORM);
            initialRequest.addReceiver(firemenCoord);
            MessageContent mc = new MessageContent(MessageType.INFORM_CITY_STATUS, messageToFiremen);
            initialRequest.setContentObject(mc);
            this.send(initialRequest);
           // log("Request message content:" + initialRequest.getContent());
        } catch (Exception e) {
            log("Unable to inform FMC");
            e.printStackTrace();
        }
    }
    public void requestCtyStatus(Map<AID, Object[]> fMoves, Map<AID, Object[]> aMoves) {
        ACLMessage initialRequest = new ACLMessage(ACLMessage.REQUEST);
        initialRequest.clearAllReceiver();
        initialRequest.addReceiver(this.centralAgent);
        initialRequest.setProtocol(InteractionProtocol.FIPA_REQUEST);

        log("Request message to agent");
        try {
            HashMap<AID, Object[]> moves = new HashMap<>();
            if(fMoves != null) { 
                moves.putAll(fMoves);
            }
            if(aMoves != null) {
                moves.putAll(aMoves);
            }
            MessageContent mc = new MessageContent(MessageType.REQUEST_CITY_STATUS, moves);
            initialRequest.setContentObject(mc);
            //log("Request message content:" + initialRequest.getContent());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //we add a behaviour that sends the message and waits for an answer
        this.addBehaviour(new RequesterBehaviour(this, initialRequest));
    }

}
