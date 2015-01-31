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
import cat.urv.imas.map.BuildingCell;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.CellType;
import cat.urv.imas.map.HospitalCell;
import java.util.ArrayList;
import java.util.*;
import java.util.stream.Collectors;
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
     * Number of people inside each ambulance
     */
    private int[] ambulanceCurrentLoad;
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
    
    /**
     * Keeps track of new fires appearing
     */
    private Cell newFire;
    

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
    
    @Override
    public String toString() {
        //TODO: show a human readable summary of the game settings.
        List<Cell> listAmbulance = agentList.get(AgentType.AMBULANCE);
        List<Cell> listFireman = agentList.get(AgentType.FIREMAN);
        List<Cell> listPrivate = agentList.get(AgentType.PRIVATE_VEHICLE);
        String retstr = "\n\n-------Game information-------\nPositions of mobile agents: \n";
        int i = 0;
        for(Cell cell: listAmbulance){
            retstr += "\t Ambulance " + i + ": (" + cell.getRow() + "," + cell.getCol() + ")\n";
            i++;
        }
        i = 0;
        for(Cell cell: listFireman){
            retstr += "\t Fireman " + i + ": (" + cell.getRow() + "," + cell.getCol() + ")\n";
            i++;
        }
        i = 0;
        for(Cell cell: listPrivate){
            retstr += "\t Private_vehicle " + i + ": (" + cell.getRow() + "," + cell.getCol() + ")\n";
            i++;
        }
        retstr += "Occupancy of Hospitals: \n";
        for(i=0; i<this.hospitalCapacities.length; i++){
            retstr += "\t Hospital " + i + ": " + this.hospitalCapacities[i] + "\n";
        }
        retstr += "Fires: \n";
        if (this.fireList != null){
            i = 0;
            for(Cell cell: this.fireList.keySet()){
                retstr += "\t Fire " + i + ": " + this.fireList.get(cell) + "\n";
                i++;
            }
        }else{
            retstr += "\t No fires... \n";
        }
        return retstr;
    }
    
    public String getShortString() {
        List<Cell> listAmbulance = agentList.get(AgentType.AMBULANCE);
        List<Cell> listFireman = agentList.get(AgentType.FIREMAN);
        List<Cell> listPrivate = agentList.get(AgentType.PRIVATE_VEHICLE);
        String retstr = "\n\n-------Game information-------\nPositions of mobile agents: \n";
        int i = 0;
        for(Cell cell: listAmbulance){
            retstr += "\t Ambulance " + i + ": (" + cell.getRow() + "," + cell.getCol() + ")\n";
            i++;
        }
        i = 0;
        for(Cell cell: listFireman){
            retstr += "\t Fireman " + i + ": (" + cell.getRow() + "," + cell.getCol() + ")\n";
            i++;
        }
        i = 0;
        for(Cell cell: listPrivate){
            retstr += "\t Private_vehicle " + i + ": (" + cell.getRow() + "," + cell.getCol() + ")\n";
            i++;
        }
        return retstr;
    }
    
    public void setNewFire(Cell fireCell) {
        this.newFire = fireCell;
    }
    
    public Cell getNewFire() {
        return this.newFire;
    }
    
    public List<Cell> getBuildingsOnFire() {
        
        List<Cell> buildingsOnFire = new ArrayList<>();
        
        Arrays.asList(map).forEach(cells -> {
            buildingsOnFire.addAll(Arrays.asList(cells)
                    .stream()
                    .filter(cell -> cell.getCellType() == CellType.BUILDING)
                    .filter(cell -> ((BuildingCell) cell).isOnFire())
                    .collect(Collectors.toList()));
        });                

        return buildingsOnFire;
    }
    
    public List<Cell> getClearBuildings() {
        
        List<Cell> clearBuildings = new ArrayList<>();
        Arrays.asList(map).forEach(cells -> {
            clearBuildings.addAll(Arrays.asList(cells)
                    .stream()
                    .filter(cell -> cell.getCellType() == CellType.BUILDING)
                    .filter(cell -> !((BuildingCell) cell).isOnFire())
                    .collect(Collectors.toList()));
        });

        return clearBuildings;
    }
    
    public static Boolean isBurned(Cell c) {

        if (((BuildingCell)c).getBurnedRatio() == 100) {
            return ((BuildingCell)c).isDestroyed();
        }
        return false;
    }
    
    public void initializeAmbulanceCapacities() {
        int nAmbulances = this.agentList.get(AgentType.AMBULANCE).size();
        int[] aCapacities = new int[nAmbulances];
        for (int i : aCapacities) {
            i = this.peoplePerAmbulance;
        }
        
        this.ambulanceCurrentLoad = aCapacities;
    }
    
    public int getAmbulanceCurrentLoad(int ambulance) {
        return this.ambulanceCurrentLoad[ambulance];
    }
    
    public void updateAmbulanceCurrentLoad(int ambulance, int load) {
        this.ambulanceCurrentLoad[ambulance] += load;
        if (this.ambulanceCurrentLoad[ambulance] > this.peoplePerAmbulance) {
            this.ambulanceCurrentLoad[ambulance] = this.peoplePerAmbulance;
        } else if (this.ambulanceCurrentLoad[ambulance] < 0 ) {
            this.ambulanceCurrentLoad[ambulance] = 0;
        }
    }
    

    
    public void advanceTurn() {
        for (Cell c : this.agentList.get(AgentType.HOSPITAL)) {
            HospitalCell hc = (HospitalCell)c;
            //hc.newTurn();
        }
    }
}
