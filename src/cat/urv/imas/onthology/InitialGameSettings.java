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
import cat.urv.imas.map.StreetCell;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.Coordinates;
import cat.urv.imas.map.HospitalCell;
import cat.urv.imas.map.BuildingCell;
import cat.urv.imas.map.GasStationCell;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Initial game settings and automatic loading from file.
 *
 * Use the GenerateGameSettings to build the game.settings configuration file.
 */
@XmlRootElement(name = "InitialGameSettings")
public class InitialGameSettings extends GameSettings {

    /*
     * Constants that define the type of content into the initialMap.
     * Any other value in a cell means that a cell is a building and
     * the value is the number of people in it.
     *
     * Cells with mobile vehicles are street cells after vehicles
     * move around.
     */
    /**
     * Street cell.
     */
    public static final int S = 0;
    /**
     * Hospital cell.
     */
    public static final int H = -1;
    /**
     * Firemen cell.
     */
    public static final int F = -2;
    /**
     * Ambulance cell.
     */
    public static final int A = -3;
    /**
     * Private vehicle cell.
     */
    public static final int P = -4;
    /**
     * Gas station cell.
     */
    public static final int G = -5;

    /**
     * City initialMap. Each number is a cell. The type of each is expressed by a
     * constant (if a letter, see above), or a building (indicating the number
     * of people in that building).
     */
    private int[][] initialMap
            = {
                {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
                {10, S, S, S, S, S, S, S, S, S, S, P, S, S, S, S, S, S, S, 10},
                {10, S, A, S, S, S, S, F, S, S, S, S, S, S, S, S, S, S, F, 10},
                {10, S, S, 10, 10, 10, 10, 10, 10, S, S, 10, 10, 10, 10, 10, 10, 10, 10, 10},
                {10, S, S, 10, 10, 10, 10, 10, 10, S, S, 10, 10, 10, 10, 10, 10, 10, 10, 10},
                {10, F, S, 10, 10, S, S, S, S, S, S, 10, 10, S, S, S, S, S, S, 10},
                {10, S, S, 10, 10, S, S, S, S, S, S, 10, 10, S, S, S, S, S, S, 10},
                {10, S, S, 10, 10, S, S, 10, 10, S, S, 10, 10, S, S, 10, 10, S, S, 10},
                {10, S, S, 10, 10, S, S, H, 10, S, S, 10, 10, S, S, 10, 10, S, S, 10},
                {10, S, S, 10, 10, S, S, 10, 10, S, S, 10, 10, S, S, 10, 10, S, S, 10},
                {10, S, S, 10, 10, S, S, 10, 10, S, S, 10, 10, S, S, 10, 10, S, S, 10},
                {10, S, S, 10, 10, S, S, 10, 10, S, A, 10, 10, S, S, 10, 10, S, S, 10},
                {10, S, S, 10, 10, S, S, 10, 10, S, S, 10, 10, S, S, H, 10, S, S, 10},
                {10, S, S, 10, 10, S, S, 10, 10, S, S, 10, 10, S, S, 10, 10, F, S, 10},
                {10, S, S, 10, 10, S, S, 10, 10, S, S, 10, 10, S, S, 10, 10, F, S, 10},
                {10, S, S, 10, 10, S, S, 10, 10, S, S, 10, 10, S, S, 10, 10, S, S, 10},
                {10, S, S, H, 10, S, S, 10, 10, S, S, 10, 10, S, S, 10, 10, S, S, 10},
                {10, S, A, S, F, S, S, 10, 10, S, S, S, S, S, S, 10, 10, S, S, 10},
                {10, S, S, S, S, S, S, 10, 10, S, S, S, S, S, S, 10, 10, A, S, 10},
                {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},};

    public int[][] getInitialMap() {
        return initialMap;
    }

    @XmlElement(required = true)
    public void setInitialMap(int[][] initialMap) {
        this.initialMap = initialMap;
    }

    public static final GameSettings load(String filename) {
        if (filename == null) {
            filename = "game.settings";
        }
        try {
            // create JAXBContext which will be used to update writer
            JAXBContext context = JAXBContext.newInstance(InitialGameSettings.class);
            Unmarshaller u = context.createUnmarshaller();
            InitialGameSettings starter = (InitialGameSettings) u.unmarshal(new FileReader(filename));
            starter.initMap();
            return starter;
        } catch (Exception e) {
            System.err.println(filename);
            System.exit(-1);
        }
        return null;
    }

    /**
     * Initializes the cell map.
     * @throws Exception if some error occurs when adding agents.
     */
    private void initMap() throws Exception {
        int rows = this.initialMap.length;
        int cols = this.initialMap[0].length;
        map = new Cell[rows][cols];
        int hospitalIndex = 0;
        int hospitalCapacity;
        int[] hospitalCapacities = this.getHospitalCapacities();
        this.agentList = new HashMap();
        this.fireList = new HashMap();
        this.buildingList = new ArrayList();

        int cell;
        StreetCell c;
        Set<Coordinates> validDirections;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                cell = initialMap[row][col];
                switch (cell) {
                    case A:
                        validDirections = foundValidDirections(row, col);
                        c = new StreetCell(row, col, validDirections);
                        c.addAgent(new InfoAgent(AgentType.AMBULANCE));
                        map[row][col] = c;
                        addAgentToList(AgentType.AMBULANCE, c);
                        break;
                    case F:
                        validDirections = foundValidDirections(row, col);
                        c = new StreetCell(row, col, validDirections);
                        c.addAgent(new InfoAgent(AgentType.FIREMAN));
                        map[row][col] = c;
                        addAgentToList(AgentType.FIREMAN, c);
                        break;
                    case P:
                        validDirections = foundValidDirections(row, col);
                        c = new StreetCell(row, col, validDirections);
                        c.addAgent(new InfoAgent(AgentType.PRIVATE_VEHICLE));
                        map[row][col] = c;
                        addAgentToList(AgentType.PRIVATE_VEHICLE, c);
                        break;
                    case S:
                        validDirections = foundValidDirections(row, col);
                        c = new StreetCell(row, col, validDirections);
                        map[row][col] = c;
                        break;
                    case G:
                        map[row][col] = new GasStationCell(row, col);
                        break;
                    case H:
                        if (hospitalIndex < hospitalCapacities.length) {
                            hospitalCapacity = hospitalCapacities[hospitalIndex];
                            hospitalIndex++;
                            map[row][col] = new HospitalCell(row, col, hospitalCapacity);
                            addAgentToList(AgentType.HOSPITAL, map[row][col]);
                        } else {
                            throw new Error(getClass().getCanonicalName() + " : More hospitals in the map than given capacities");
                        }
                        break;
                    default: //positive value means number of citizens in a building.
                        BuildingCell building = new BuildingCell(cell, row, col);
                        map[row][col] = building;
                        this.buildingList.add(building);
                        break;
                }
            }
        }
        if (hospitalIndex != hospitalCapacities.length) {
            throw new Error(getClass().getCanonicalName() + " : Less hospitals in the map than given capacities.");
        }
    }

    /*
     * Return true if the value is an ambulance or fireman or personal vehicule or street
     */
    private boolean isStreet(int mapValue) {
        return mapValue == A || mapValue == F || mapValue == P || mapValue == S;
    }

    /**
     * Found all the valid directions of a cell
     */
    private Set<Coordinates> foundValidDirections(int row, int col) {
        Set<Coordinates> validDirections = new HashSet();

        if(isStreet(initialMap[row-1][col]) && (!isStreet(initialMap[row-1][col+1]) || !isStreet(initialMap[row][col+1]))) {
            validDirections.add(Coordinates.NORTH);
        }

        if(isStreet(initialMap[row+1][col]) && (!isStreet(initialMap[row+1][col-1]) || !isStreet(initialMap[row][col-1]))) {
            validDirections.add(Coordinates.SOUTH);
        }

        if(isStreet(initialMap[row][col+1]) && (!isStreet(initialMap[row+1][col+1]) || !isStreet(initialMap[row+1][col]))) {
            validDirections.add(Coordinates.EAST);
        }

        if(isStreet(initialMap[row][col-1]) && (!isStreet(initialMap[row-1][col-1]) || !isStreet(initialMap[row-1][col]))) {
            validDirections.add(Coordinates.WEST);
        }

        if(validDirections.size() == 0 && !isStreet(initialMap[row][col+1])) {
            validDirections.add(Coordinates.NORTH);
            validDirections.add(Coordinates.SOUTH);
            validDirections.add(Coordinates.EAST);
            validDirections.add(Coordinates.WEST);
        }

        return validDirections;
    }

    /**
     * Ensure agent list is correctly updated.
     *
     * @param type agent type.
     * @param cell cell where appears the agent.
     */
    private void addAgentToList(AgentType type, Cell cell) {
        List<Cell> list = this.agentList.get(type);
        if (list == null) {
            list = new ArrayList();
            this.agentList.put(type, list);
        }
        list.add(cell);
    }
}
