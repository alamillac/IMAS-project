/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import cat.urv.imas.map.Cell;
import cat.urv.imas.map.StreetCell;
import cat.urv.imas.onthology.GameSettings;
import org.newdawn.slick.util.pathfinding.Path;
import cat.urv.imas.utils.Utils;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.utils.MessageType;
import cat.urv.imas.map.CellType;
import cat.urv.imas.utils.NavigatorStatus;

import jade.core.AID;
import jade.domain.FIPANames.InteractionProtocol;

/**
 *
 * @author mhj
 */
public abstract class NavigatorAgent extends ImasAgent {

    protected GameSettings game;

    protected Cell agentPosition;

    protected Cell targetPosition;
    
    protected Cell targetBuilding;

    

    protected Path shortestPath;

    protected int currentStep = -1;

    protected NavigatorStatus status;

    

    public NavigatorAgent(AgentType type) {
        super(type);
    }

    protected boolean checkCollisions() {
        return false;
    }

    public float findShortestPath(Cell tPosition) {
        //tPosition = findFreeCell(tPosition);
        this.shortestPath = Utils.getShortestPath(this.game.getMap(), this.agentPosition, tPosition);
        this.currentStep = 0;
        if(this.shortestPath == null)
            return -1;
        return this.shortestPath.getLength();        
    }
    
    public Cell findFreeCell(Cell tPosition)
    {
        for(int i = -1; i<2;i++)
        {
            for(int j = -1; j<2;j++)
            {
                try {
                    if(this.game.get(tPosition.getRow() + i, tPosition.getCol()+ j).getCellType().equals(CellType.STREET))
                    {
                        return this.game.get(tPosition.getRow()+i, tPosition.getCol()+j);
                    }
                } catch (Exception e) {
                }
            }
        }
        return null; 
    }
    
    public float findShortestPath() {
        return findShortestPath(this.targetPosition);
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
            return -1;
        }
        return this.shortestPath.getLength();
    }

    protected String moveStep() {
        if(this.shortestPath == null) {
            return "PATH_DONT_EXIST";
        }

        //Already on the target
        if(this.currentStep == this.shortestPath.getLength() - 1 ||
                (this.agentPosition.getRow() == this.targetPosition.getRow() &&
                this.agentPosition.getCol() == this.targetPosition.getCol())) {

            return "ON_CELL";
        }
        int s = this.currentStep + 1;
        Path.Step step = shortestPath.getStep(s);
        StreetCell cell =(StreetCell) this.game.get(step.getX(), step.getY());
        //if(cell.isThereAnAgent()) {Working on this
            //tryToFindNewWay(cell);
           // this.findShortestPath(); //
            //return "OK";
        //}
       //else {
            this.currentStep = s;
            this.agentPosition = cell;
            return "OK";
        //}
    }
    
    private void tryToFindNewWay(StreetCell blockedCell)
    {
        int moveX = agentPosition.getCol() - blockedCell.getCol();
        int moveY = agentPosition.getRow()- blockedCell.getRow();
        Cell newCell;
        StreetCell newStreetCell;
        
        if(moveX!=0)
        {
            
            newCell = game.get(agentPosition.getRow()+1, agentPosition.getCol());
            if(newCell.getCellType().equals(CellType.STREET))
            {
                newStreetCell = (StreetCell)newCell;
                if(!newStreetCell.isThereAnAgent()) {
                 
                }
            }
            
            newCell = game.get(agentPosition.getRow()-1, agentPosition.getCol());
            if(newCell.getCellType().equals(CellType.STREET))
            {
                newStreetCell = (StreetCell)newCell;
                if(!newStreetCell.isThereAnAgent()) {

                }
            }
            
        }
        if(moveY!=0)
        {
            
        }
    }

    @Override
    protected void setup() {
        /* ** Very Important Line (VIL) ************************************* */
        this.setEnabledO2ACommunication(true, 1);

         Object[] args = this.getArguments();

         this.agentPosition = (Cell)args[0];
         this.game = (GameSettings)args[1];
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
    
    public NavigatorStatus getStatus() {
        return status;
    }

    public void setStatus(NavigatorStatus status) {
        this.status = status;
    }
    
    public Cell getTargetBuilding() {
        return targetBuilding;
    }

    public void setTargetBuilding(Cell targetBuilding) {
        this.targetBuilding = targetBuilding;
    }
    
}
