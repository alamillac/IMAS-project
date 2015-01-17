/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;
import static cat.urv.imas.agent.ImasAgent.OWNER;
import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.GameSettings;
import jade.core.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

/**
 *
 * @author Domen
 */
public class AmbulanceAgent extends ImasAgent{

    /**
     * Game settings in use. So we can get city map 
     */
    private GameSettings game;
    
    private int ambulanceLoadingSpeed;

    private int peoplePerAmbulance;
    
    private Cell ambulanceCell;
    
    
    private AID hospitalAgent;
    
    public AmbulanceAgent() {
        super(AgentType.AMBULANCE);
    }
    
    @Override
    protected void setup() {
        
        
         /* ** Very Important Line (VIL) ************************************* */
        this.setEnabledO2ACommunication(true, 1);

        // 1. Register the agent to the DF
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType(AgentType.AMBULANCE.toString());
        sd1.setName(getLocalName());
        sd1.setOwnership(OWNER);
        
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.addServices(sd1);
        dfd.setName(getAID());
        try {
            DFService.register(this, dfd);
            log("Registered to the DF");
        } catch (FIPAException e) {
            System.err.println(getLocalName() + " failed registration to DF [ko]. Reason: " + e.getMessage());
            doDelete();
        }
        
        //Set the arguments we get from central agent
        Object[] arg = this.getArguments();
        this.setGame((GameSettings)arg[0]);
        this.setAmbulanceLoadingSpeed((int)arg[1]);
        this.setPeoplePerAmbulance((int)arg[2]);
        this.setAmbulanceCell((Cell)arg[2]);
        
    }
    
    /**
     * Update the game settings.
     *
     * @param game current game settings.
     */
    public void setGame(GameSettings game) {
        this.game = game;
    }

    /**
     * Gets the current game settings.
     *
     * @return the current game settings.
     */
    public GameSettings getGame() {
        return this.game;
    }
    
    public int getAmbulanceLoadingSpeed() {
        return ambulanceLoadingSpeed;
    }

    public void setAmbulanceLoadingSpeed(int ambulanceLoadingSpeed) {
        this.ambulanceLoadingSpeed = ambulanceLoadingSpeed;
    }

    public int getPeoplePerAmbulance() {
        return peoplePerAmbulance;
    }

    public void setPeoplePerAmbulance(int peoplePerAmbulance) {
        this.peoplePerAmbulance = peoplePerAmbulance;
    }

    public Cell getAmbulanceCell() {
        return ambulanceCell;
    }

    public void setAmbulanceCell(Cell ambulanceCell) {
        this.ambulanceCell = ambulanceCell;
    }

    public AID getHospitalAgent() {
        return hospitalAgent;
    }

    public void setHospitalAgent(AID hospitalAgent) {
        this.hospitalAgent = hospitalAgent;
    }
    
}
