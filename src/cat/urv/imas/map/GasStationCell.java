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
 * Cell map containing a gas station..
 */
public class GasStationCell extends Cell {

    public GasStationCell(int x, int y) {
        super(CellType.GAS_STATION, x, y);
    }
    
    /* ***************** Map visualization API ********************************/
    
    @Override
    public void draw(CellVisualizer visual) {
        visual.drawGasStation(this);
    }

}
