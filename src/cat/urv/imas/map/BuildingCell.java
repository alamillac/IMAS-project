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
 * Building cell.
 */
public class BuildingCell extends Cell {

    /**
     * Initial number of citizens.
     */
    private int initialNumberOfCitizens = 0;
    /**
     * Current number of citizens.
     */
    private int numberOfCitizens = 0;
    /**
     * This ratio shows wether this building is on fire and its percentage. If
     * 0, means 0% burned, and no fire. Any other positive value means that the
     * building is on fire and the corresponding burned ratio. If this value is
     * 100, menas 100% burned and building is destroyed.
     */
    private int burnedRatio = 0;

    /**
     * Builds a cell corresponding to a building with the given initial number
     * of citizens.
     *
     * @param initialNumberOfCitizens Number of citizens in this building.
     * @param row row number.
     * @param col column number.
     */
    public BuildingCell(int initialNumberOfCitizens, int row, int col) {
        super(CellType.BUILDING, row, col);
        this.initialNumberOfCitizens = initialNumberOfCitizens;
        this.numberOfCitizens = initialNumberOfCitizens;
    }

    /**
     * Gets the initial number of citizens in the building.
     *
     * @return the initial number of citizens.
     */
    public int getInitialNumberOfCitizens() {
        return this.initialNumberOfCitizens;
    }

    /**
     * Gets the current number of citizens in the building.
     *
     * @return the current number of citizens in the building.
     */
    public int getNumberOfCitizens() {
        return this.numberOfCitizens;
    }

    /**
     * Tries to take up to the number of "citizens" from the building, and
     * informs the real taken number of citizens if there are less than
     * "citizens" people in the building.
     *
     * @param citizens Number of citizens to take from the building.
     * @return the actual number of citizens taken from the building.
     */
    public int take(int citizens) {
        int min = Math.min(numberOfCitizens, citizens);
        numberOfCitizens -= min;
        return min;
    }

    /**
     * Gets the current burned ratio of the building.
     *
     * @return the current burned ratio of the building.
     */
    public int getBurnedRatio() {
        return this.burnedRatio;
    }

    /**
     * Updates the current burned ratio of the building with the given ratio. If
     * the ratio is positive, the burned ratio will increment. Otherwise, if
     * negative, fire is being put out.
     *
     * @param ratio Ratio of burning of increment (if positive) or decrement (if
     * negative).
     */
    public void updateBurnedRatio(int ratio) {
        this.burnedRatio -= ratio;
        if (burnedRatio < 0) {
            burnedRatio = 0;
        } else if (burnedRatio > 100) {
            burnedRatio = 100;
        }
    }

    /**
     * Tells whether the bulding is on fire.
     *
     * @return true if there is fire on the building. false otherwise.
     */
    public boolean isOnFire() {
        return this.burnedRatio != 0;
    }

    /**
     * Tells whether the building was destroyed by fire.
     *
     * @return true if burnedRatio is at 100%; false otherwise.
     */
    public boolean isDestroyed() {
        return this.burnedRatio == 100;
    }

    /* ***************** Map visualization API ********************************/
    
    @Override
    public void draw(CellVisualizer visual) {
        visual.drawBuilding(this);
    }

    @Override
    public String getMapMessage() {
        String message = String.valueOf(getNumberOfCitizens());
        if (isOnFire()) {
            message = String.valueOf(getBurnedRatio()) + "/" + message;
        }
        return message;
    }
}
