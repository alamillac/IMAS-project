/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.GameSettings;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.utils.MessageType;
import jade.core.AID;

/**
 *
 * @author mhj
 */
public abstract class NavigatorAgent extends ImasAgent {

    protected GameSettings game;

    protected Cell agentPosition;

    protected Cell targetPosition;

    /**
     * Central agent id.
     */
    private AID centralAgent;

    public NavigatorAgent(AgentType type) {
        super(type);
    }

    protected boolean checkCollisions() {
        return false;
    }

    protected float findShortestPath() {
        return 0;
    }

    @Override
    protected void setup() {
        // search CentralAgent
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.CENTRAL.toString());
        this.centralAgent = UtilsAgents.searchAgent(this, searchCriterion);
    }

    public void setGame(GameSettings game) {
        this.game = game;
    }

    public GameSettings getGame() {
        return game;
    }

    public void setAgentPosition(Cell agentPosition) {
        this.agentPosition = agentPosition;
    }

    public Cell getAgentPosition() {
        return agentPosition;
    }

    public void setTargetPosition(Cell targetPosition) {
        this.targetPosition = targetPosition;
    }

    public Cell getTargetPosition() {
        return targetPosition;
    }

    protected void ask_moveToCentralAgent(Cell newPosition) {
        try {
            ACLMessage moveMsg = new ACLMessage(ACLMessage.REQUEST);
            moveMsg.addReceiver(centralAgent);
            MessageContent mc = new MessageContent(MessageType.REQUEST_MOVE, null);
            moveMsg.setContentObject(mc);
            this.send(moveMsg);
           // log("Request message content:" + initialRequest.getContent());
        } catch (Exception e) {
            log("Unable to move");
            e.printStackTrace();
        }
    }

}
