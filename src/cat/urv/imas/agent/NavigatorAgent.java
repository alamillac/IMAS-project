/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.GameSettings;

/**
 *
 * @author mhj
 */
public abstract class NavigatorAgent extends ImasAgent {

    protected GameSettings game;
    
    protected Cell agentPosition;
    
    protected Cell targetPosition;
    
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
        
        this.setEnabledO2ACommunication(true, 1);
        
         Object[] args = this.getArguments();
         this.agentPosition = (Cell)args[0];
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
    
    
    
}
