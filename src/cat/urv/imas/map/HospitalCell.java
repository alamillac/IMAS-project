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
package cat.urv.imas.map;

import cat.urv.imas.gui.CellVisualizer;

/**
 * Cell that represents a hospital.
 */
public class HospitalCell extends Cell {

    /**
     * Total capacity of the hospital, in number of beds.
     */
    private final int capacity;
    /**
     * Number of beds used by people. When usedBeds = capacity, hospital cannot
     * accept more citizens.
     */
    private int usedBeds;

    /**
     * Initializes a cell with a hospital.
     *
     * @param row row number (zero based).
     * @param col col number (zero based).
     * @param capacity total capacity of the hospital, in number of beds.
     */
    public HospitalCell(int row, int col, int capacity) {
        super(CellType.HOSPITAL, row, col);
        this.capacity = capacity;
        usedBeds = 0;
    }

    /**
     * This puts the given number of people into the hospital.
     *
     * @param people number of people to put into the hospital.
     */
    public void put(int people) {
        //TODO: how we take into account when these people can leave the hospital?
        usedBeds += people;
    }

    /**
     * Tells whether the hospital is full of healthing people.
     *
     * @return true if full; false if can accept at least another person.
     */
    public boolean isFull() {
        return usedBeds == capacity;
    }

    /**
     * Tells the ratio (from 0 to 100) of the number of beds in use.
     *
     * @return ratio of the number of used beds.
     */
    public int useRatio() {
        return 0;
    }

    /* ***************** Map visualization API ********************************/
    @Override
    public void draw(CellVisualizer visual) {
        visual.drawHospital(this);
    }
    
    @Override
    public String getMapMessage() {
        return String.valueOf(useRatio());
    }
    
}
