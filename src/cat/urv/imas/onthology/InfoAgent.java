package cat.urv.imas.onthology;

import cat.urv.imas.agent.AgentType;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.CellType;
import cat.urv.imas.map.Coordinates;
import cat.urv.imas.map.StreetCell;
import java.util.Set;
import jade.core.AID;

/**
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013</p>
 * <p>
 * <b>Company:</b> Universitat Rovira i Virgili (<a
 * href="http://www.urv.cat">URV</a>)</p>
 */
public class InfoAgent implements java.io.Serializable {

    /**
     * Type of this agent.
     */
    private final AgentType type;
    /**
     * AID for the related agent.
     */
    private AID aid;

    /*
     * the direction to which point the agent
     */
    private Coordinates actualDirection;

    /**
     * Building new instance with only the type.
     *
     * @param type type of agent.
     */
    public InfoAgent(AgentType type) {
        this.type = type;
        this.aid = null;
    }

    /**
     * Building new instance specifying its type and its AID.
     *
     * @param type agent type.
     * @param aid agent id.
     */
    public InfoAgent(AgentType type, AID aid) {
        this.type = type;
        this.aid = aid;
    }

    /**
     *
     * @param a
     * @return
     */
    @Override
    public boolean equals(Object a) {
        if(this.aid == null) {
            //is a private vehicle
            return true;
        }

        if (a instanceof InfoAgent) {
            return ((InfoAgent) a).getAID().equals(this.aid);
        } else {
            return false;
        }
    }

    /**
     * Gets the hash code. To simplify it, just returns the hash code from its
     * type.
     *
     * @return
     */
    @Override
    public int hashCode() {
        return type.hashCode();
    }

    /**
     * Gets agent id.
     *
     * @return agent id.
     */
    public AID getAID() {
        return this.aid;
    }

    /**
     * Sets the agent id.
     *
     * @param aid agent id.
     */
    public void setAID(AID aid) {
        this.aid = aid;
    }

    /**
     * Type of agent.
     *
     * @return type of agent.
     */
    public AgentType getType() {
        return this.type;
    }

    public void setDirection(Coordinates actualDirection) {
        this.actualDirection = actualDirection;
    }

    public Coordinates getDirection() {
        return this.actualDirection;
    }

    /*
     * Random movement. Move the agent to another adyacente cell acording to:
     * +If a private vehicle is in any street, the private vehicle has to go straight on in the same direction.
     * +If a private vehicle is in any street, the private vehicle has to go straight on in the same direction.
     *      -An 80% of probability of going straight on, and
     *      -A 20% of turning right or left. Both directions will have the same probability of being chosen whenever they both are available.
     */
    public Cell randomMovement(Cell[][] map, Cell actualPosition, boolean tryTurn) throws Exception {
        Coordinates moveTo = null;
        Set<Coordinates> validDirections = ((StreetCell)actualPosition).getValidDirections();

        if(validDirections.size() > 1) {
            if(tryTurn) {
                //try to change direction
                for(Coordinates validDirection : validDirections) {
                    if(validDirection != actualDirection) {
                        moveTo = validDirection;
                    }
                }
            }
            else if(validDirections.contains(actualDirection)) {
                //try to go straight
                moveTo = actualDirection;
            }
        }

        if(moveTo == null) {
            //try to move
            moveTo = validDirections.iterator().next();
        }

        Cell newPosition = move(map, actualPosition,  moveTo);

        return newPosition;
    }

    /*
     * Try to move from actualPosition in direction toDirection
     */
    public Cell move(Cell[][] map, Cell actualPosition, Coordinates toDirection) throws Exception {
        Cell newPosition;

        int row = actualPosition.getRow();
        int col = actualPosition.getCol();

        if(toDirection == Coordinates.NORTH) {
            newPosition = map[row-1][col];
        }
        else if(toDirection == Coordinates.SOUTH) {
            newPosition = map[row+1][col];
        }
        else if(toDirection == Coordinates.EAST) {
            newPosition = map[row][col+1];
        }
        else if(toDirection == Coordinates.WEST) {
            newPosition = map[row][col-1];
        }
        else {
            newPosition = null;
        }

        if(newPosition != null && newPosition.getCellType() == CellType.STREET && ((StreetCell)newPosition).getAgent() == null) {
           // if the street is empty
            try {
                ((StreetCell)actualPosition).removeAgent(this);
            } catch (Exception e) {
                System.err.println("there was a problem by removing one agent");
                System.err.println(e.getMessage());
                return actualPosition;
            }

            try {
                ((StreetCell)newPosition).addAgent(this);
            } catch (Exception e) {
                System.err.println("there was a problem by adding one agent");
                System.err.println(e.getMessage());
                ((StreetCell)actualPosition).addAgent(this);
                return actualPosition;
            }
            setDirection(toDirection);
        }
        else {
            newPosition = actualPosition;
        }

        return newPosition;
    }

    /**
     * String representation of this isntance.
     *
     * @return string representation.
     */
    @Override
    public String toString() {
        return "(info-agent (agent-type " + this.getType() + ")"
                + ((null != this.aid) ? (" (aid " + this.aid + ")") : "")
                + ")";
    }

}
