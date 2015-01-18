/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.utils;

import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.InitialGameSettings;
import org.newdawn.slick.util.pathfinding.Path;

/**
 *
 * @author mhj
 */
public class ShortestPathTester {
    public static void main(String[] args) {
        GameSettings gs = InitialGameSettings.load("game.settings");
        Path path = Utils.getShortestPath(gs.getMap(), gs.get(2, 2), gs.get(2, 7));
        
        System.out.println(path.getLength());
        
        for(int i = 0; i < path.getLength(); i++) {
            System.out.print(gs.get(path.getX(i), path.getY(i)).toString());
        }
        System.out.println("");
    }
}
