/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.GameSettings;
import org.newdawn.slick.util.pathfinding.Path;
import cat.urv.imas.utils.Utils;
/**
 *
 * @author mhj
 */
public abstract class NavigatorAgent extends ImasAgent {

    protected GameSettings game;
    
    protected Cell agentPosition;
    
    protected Cell targetPosition;
    
    protected Path shortestPath;
    
    public NavigatorAgent(AgentType type) {
        super(type);
    }
    
    protected boolean checkCollisions() {
        return false;
    }
    
    public float findShortestPath() {
        this.shortestPath = Utils.getShortestPath(this.game.getMap(), agentPosition, targetPosition);
        return this.shortestPath.getLength();
    }
    
    public Cell[] getPath() {
        if(this.shortestPath == null) {
            return null;
        }
        
        int n = shortestPath.getLength();
        Cell[] path = new Cell[n];
        for(int i = 0; i < n; i++) {
            path[i] = game.get(shortestPath.getX(i), shortestPath.getY(i));
        }
        return path;
    } 
    
    
    public float getPathCost() {
        if(this.shortestPath == null) {
            return 0;
        }
        return this.shortestPath.getLength();
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
