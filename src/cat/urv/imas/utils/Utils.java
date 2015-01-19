/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.utils;

import cat.urv.imas.agent.AgentType;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.CellType;
import cat.urv.imas.map.StreetCell;
import org.newdawn.slick.util.pathfinding.AStarPathFinder;
import org.newdawn.slick.util.pathfinding.Path;
import org.newdawn.slick.util.pathfinding.PathFindingContext;
import org.newdawn.slick.util.pathfinding.TileBasedMap;

/**
 *
 * @author mhj
 */
public final class Utils {
    public static Path getShortestPath(final Cell[][] map, final Cell source, final Cell target) {
        
        int maxSearchDistance = map.length * map[0].length;
        
        TileBasedMap tileMap = new TileBasedMap() {

            @Override
            public int getWidthInTiles() {
                return map[0].length;
            }

            @Override
            public int getHeightInTiles() {
                return map.length;
            }

            @Override
            public void pathFinderVisited(int x, int y) {
                
            }

            @Override
            public boolean blocked(PathFindingContext pfc, int x, int y) {
                if(map[x][y].getCellType() == CellType.STREET) {
                    StreetCell sc = (StreetCell)map[x][y];
                    if(sc.isThereAnAgent()) {
                        return sc.getAgent().getType() != AgentType.PRIVATE_VEHICLE;
                    }
                    else {
                        return false;
                    }
                }                
                return true;
            }

            @Override
            public float getCost(PathFindingContext pfc, int x, int y) {
                return 1.0F;
            }
        };
        
        AStarPathFinder pathFinder = new AStarPathFinder(tileMap, maxSearchDistance, false);
        Path p = pathFinder.findPath(null, source.getRow(), source.getCol(), target.getRow(), target.getCol());
        return p;
    }
}
