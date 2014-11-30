/**
 * IMAS base code for the practical work. 
 * Copyright (C) 2014 DEIM - URV
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cat.urv.imas.onthology;

import cat.urv.imas.agent.AgentType;
import cat.urv.imas.map.Cell;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Current game settings. Cell coordinates are zero based: row and column values
 * goes from [0..n-1], both included.
 * 
 * Use the GenerateGameSettings to build the game.settings configuration file.
 * 
 */
@XmlRootElement(name = "GameSettings")
public class GameSettings implements java.io.Serializable {    

    /* Default values set to all attributes, just in case. */
    /**
     * Seed for random numbers.
     */
    private float seed = 0.0f;
    /**
     * List of number of beds per hospital. Therefore, a value "{10, 10, 10}"
     * means there will be 3 hospitals with 10 beds each. The number of beds
     * means hospital capacity in number of people in the hospital at the same
     * simulation step.
     */
    private int[] hospitalCapacities = {10, 10, 10};
    /**
     * Number of steps a person needs to be in the hospital to health, before
     * the person leaves the hospital.
     */
    private int stepsToHealth = 3;
    /**
     * Capacity of ambulances, in number of people.
     */
    private int peoplePerAmbulance = 3;
    /**
     * Number of people loaded into an ambulance per simulation step.
     */
    private int ambulanceLoadingSpeed = 1;
    /**
     * Percentage of burning of a building without firemen. A value -fireSpeed
     * has to be applied when there are firemen surrounding the fire, at a total
     * ratio of: {number of surrounding firemen} * {- fireSpeed}.
     */
    private int fireSpeed = 5;
    /**
     * Number of gas stations. This is the optional part of the practice.
     * Develop it when you are sure the whole mandatory part is perfect.
     */
    private int gasStations = 0;
    /**
     * Total number of simulation steps.
     */
    private int simulationSteps = 100;
    /**
     * City map.
     */
    protected Cell[][] map;
    /**
     * Computed summary of the position of agents in the city. For each given
     * type of mobile agent, we get the list of their positions.
     */
    protected Map<AgentType, List<Cell>> agentList;
    /**
     * Computed summary of the list of fires. The integer value introduces
     * the burned ratio of the building.
     */
    protected Map<Cell, Integer> fireList;
    /**
     * Title to set to the GUI.
     */
    protected String title = "Demo title";
    

    public float getSeed() {
        return seed;
    }

    @XmlElement(required = true)
    public void setSeed(float seed) {
        this.seed = seed;
    }

    public int[] getHospitalCapacities() {
        return hospitalCapacities;
    }

    @XmlElement(required = true)
    public void setHospitalCapacities(int[] capacities) {
        this.hospitalCapacities = capacities;
    }

    public int getStepsToHealth() {
        return stepsToHealth;
    }

    @XmlElement(required = true)
    public void setStepsToHealth(int stepsToHealth) {
        this.stepsToHealth = stepsToHealth;
    }

    public int getPeoplePerAmbulance() {
        return peoplePerAmbulance;
    }

    @XmlElement(required = true)
    public void setPeoplePerAmbulance(int peoplePerAmbulance) {
        this.peoplePerAmbulance = peoplePerAmbulance;
    }

    public int getAmbulanceLoadingSpeed() {
        return ambulanceLoadingSpeed;
    }

    @XmlElement(required = true)
    public void setAmbulanceLoadingSpeed(int ambulanceLoadingSpeed) {
        this.ambulanceLoadingSpeed = ambulanceLoadingSpeed;
    }

    public int getFireSpeed() {
        return fireSpeed;
    }

    @XmlElement(required = true)
    public void setFireSpeed(int fireSpeed) {
        this.fireSpeed = fireSpeed;
    }

    public int getGasStations() {
        return gasStations;
    }

    @XmlElement(required = true)
    public void setGasStations(int gasStations) {
        this.gasStations = gasStations;
    }

    public int getSimulationSteps() {
        return simulationSteps;
    }

    @XmlElement(required = true)
    public void setSimulationSteps(int simulationSteps) {
        this.simulationSteps = simulationSteps;
    }

    public String getTitle() {
        return title;
    }

    @XmlElement(required=true)
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the full current city map.
     * @return the current city map.
     */
    @XmlTransient
    public Cell[][] getMap() {
        return map;
    }
    
    /**
     * Gets the cell given its coordinate.
     * @param row row number (zero based)
     * @param col column number (zero based).
     * @return a city's Cell.
     */
    public Cell get(int row, int col) {
        return map[row][col];
    }

    @XmlTransient
    public Map<AgentType, List<Cell>> getAgentList() {
        return agentList;
    }

    public void setAgentList(Map<AgentType, List<Cell>> agentList) {
        this.agentList = agentList;
    }

    @XmlTransient
    public Map<Cell, Integer> getFireList() {
        return fireList;
    }

    public void setFireList(Map<Cell, Integer> fireList) {
        this.fireList = fireList;
    }
    
    public String toString() {
        //TODO: show a human readable summary of the game settings.
        return "Game settings";
    }
    
    public String getShortString() {
        //TODO: list of agents, hospitals and gas stations (if any)
        return "Game settings: agent related string";
    }
    
}
