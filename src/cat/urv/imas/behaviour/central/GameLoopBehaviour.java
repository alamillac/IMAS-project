/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.behaviour.central;

import cat.urv.imas.agent.CentralAgent;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

/**
 *
 * @author mhj
 */
public class GameLoopBehaviour extends TickerBehaviour {

    private CentralAgent centralAgent = null;
    
    public GameLoopBehaviour(Agent a, long period) {
        super(a, period);
        this.centralAgent = (CentralAgent) a;
    }

    @Override
    protected void onTick() {
        boolean isGameFinished = false;
        this.centralAgent.incGameStep();
        if(!this.centralAgent.isGameFinished()) {
            
            Object newFire = this.centralAgent.addNewFire();
            
            this.centralAgent.movePrivateVehicles();
            
            // Send NEW informations
            this.centralAgent.addBehaviour(new SendInformationBehaviour(this.centralAgent));
            
            // wait for the new positions
            //this.myAgent.addBehaviour(new RequestResponseBehaviour(this.centralAgent));
            
            //update fires
            this.centralAgent.updateFiresRatio();
            this.centralAgent.updateGUI();
        }
        else {
            this.stop();
        }
    }
    
}
