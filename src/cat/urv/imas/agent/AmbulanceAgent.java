/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;
import jade.core.*;

/**
 *
 * @author Domen
 */
public class AmbulanceAgent extends ImasAgent{

    private AID hospitalAgent;
    
    public AmbulanceAgent() {
        super(AgentType.AMBULANCE);
    }
    
}
