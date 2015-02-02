/**
 *  IMAS base code for the practical work.
 *  Copyright (C) 2014 DEIM - URV
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cat.urv.imas.agent;

import java.util.ArrayList;
import cat.urv.imas.onthology.InitialGameSettings;
import cat.urv.imas.onthology.InfoAgent;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.gui.GraphicInterface;
import cat.urv.imas.behaviour.central.RequestResponseBehaviour;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.StreetCell;
import cat.urv.imas.map.BuildingCell;
import cat.urv.imas.map.Coordinates;
import cat.urv.imas.utils.MessageType;
import jade.core.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.*;
import jade.wrapper.AgentContainer;
import java.util.List;
import java.util.Map;
import jade.core.behaviours.TickerBehaviour;
import java.io.IOException;
import java.util.Random;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;


/**
 * Central agent that controls the GUI and loads initial configuration settings.
 * TODO: You have to decide the onthology and protocol when interacting among
 * the Coordinator agent.
 */
public class CentralAgent extends ImasAgent {

    /**
     * GUI with the map, central agent log and statistics.
     */
    private GraphicInterface gui;
    
    private List<BuildingCell> firemoveFire ;
    
    private List<Cell> agentCellsToRemove;//old agents positions 

    /**
     * Game settings. At the very beginning, it will contain the loaded
     * initial configuration settings.
     */
    private GameSettings game;

    /**
     * The Coordinator agent with which interacts sharing game settings every
     * round.
     */
    private AID coordinatorAgent;

    /*
     * Random generator
     */
    private Random random;

    /*
     * Total number of citizens killed by fire
     */
    private int totalDeadCitizens = 0;

    /**
     * Builds the Central agent.
     */
    public CentralAgent() {
        super(AgentType.CENTRAL);
    }
    
    public List<BuildingCell> firemenOnFire;
    

    /**
     * A message is shown in the log area of the GUI, as well as in the
     * stantard output.
     *
     * @param log String to show
     */
    @Override
    public void log(String log) {
        if (gui != null) {
            gui.log(getLocalName()+ ": " + log + "\n");
        }
        super.log(log);
    }

    /**
     * An error message is shown in the log area of the GUI, as well as in the
     * error output.
     *
     * @param error Error to show
     */
    @Override
    public void errorLog(String error) {
        if (gui != null) {
            gui.log("ERROR: " + getLocalName()+ ": " + error + "\n");
        }
        super.errorLog(error);
    }

    /**
     * Gets the game settings.
     *
     * @return game settings.
     */
    public GameSettings getGame() {
        return this.game;
    }

    /*
     * Init the random variable with a seed
     */
    protected void initRandom(long seed) {
        random = new Random(seed);
    }

    /*
     * Return a Building cell randomly
     */
    protected BuildingCell getRandomBuilding() {
        List<BuildingCell> buildings = game.getBuildingList();
        int num_buildings = buildings.size();
        BuildingCell building;
        if(num_buildings > 0) {
            int building_id = random.nextInt(num_buildings);
            building = buildings.get(building_id);
        }
        else {
            building = null;
        }
        return building;
    }
    
    public Coordinates getDirectionOfMovement(Cell oldPos, Cell newPos)
    {
        if(oldPos.getCol()<newPos.getCol()){return Coordinates.EAST;}
        if(oldPos.getCol()>newPos.getCol()){return Coordinates.WEST;}
        if(oldPos.getRow()<newPos.getRow()){return Coordinates.SOUTH;}
        if(oldPos.getRow()>newPos.getRow()){return Coordinates.NORTH;}
        
        return null; 
    }

    //update map 
    public void updateMoves(Map<AID, Object[]> moves)
    {
        agentCellsToRemove = new ArrayList<>();
        
        
        Map<AgentType, List<Cell>> agentList = game.getAgentList();
        Map<BuildingCell, Integer> firemap = game.getFireList();
        List<Cell> FiremanPositions = agentList.get(AgentType.FIREMAN);
        List<Cell> newFiremanPositions = new ArrayList();
        firemoveFire = new ArrayList<>();
        //Cell[][] currentMap = this.game.getMap();
        
       
        
        for (Map.Entry<AID, Object[]> entrySet : moves.entrySet()) {
            AID key = entrySet.getKey();
            Object[] value = entrySet.getValue();
            
            switch((MessageType)value[0])
            {
                case WAITING:
                    break;
                case MOVE:
                    
                   /* for (Cell[] cl : currentMap) {
                        for (Cell c : cl) {
                            if (c instanceof StreetCell) {
                                StreetCell sc = (StreetCell)c;
                                try {
                                    if (sc.isThereAnAgent()) {
                                        
                                        
                                        
                                        sc.removeAgentt();
                                    }
                                } catch (Exception ex) {
                                    Logger.getLogger(CentralAgent.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    }*/
                    
                    
                    for(int i = 0; i < FiremanPositions.size(); i++) // we find that agent in list (there must be faster way)
                    {
                        Cell tmpOld = (Cell)value[1];
                        Cell tmpNew = (Cell)value[2];
                        if((FiremanPositions.get(i).getCol()==tmpOld.getCol())&&(FiremanPositions.get(i).getRow()==tmpOld.getRow())){
                            
                            InfoAgent firemen = ((StreetCell)FiremanPositions.get(i)).getAgent();
                            agentCellsToRemove.add(FiremanPositions.get(i));
                            /*StreetCell oldCell = ((StreetCell)FiremanPositions.get(i));
                            
                            
                            try {
                                oldCell.removeAgentt();
                               // ((StreetCell)FiremanPositions.get(i)).removeAgent(firemen);
                            } catch (Exception ex) {
                                Logger.getLogger(CentralAgent.class.getName()).log(Level.SEVERE, null, ex);
                            }*/
                            
                            Cell newPosition = null;//(StreetCell)game.get(tmpNew.getRow(), tmpNew.getCol());
                            //InfoAgent newfiremen = ((StreetCell)FiremanPositions.get(i)).getAgent();
                            try {
                                
                                //(StreetCell)newPosition).addAgent(firemen);
                                newPosition = firemen.move(game.getMap(), tmpOld, getDirectionOfMovement(tmpOld, tmpNew));
                               // newPosition.addAgent(new InfoAgent(AgentType.FIREMAN, key));
                            } catch (Exception ex) {
                                Logger.getLogger(CentralAgent.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            

                            FiremanPositions.remove(i);
                            FiremanPositions.add(newPosition);

                            i=FiremanPositions.size(); // out of loop
                        }
                      //  if(FiremanPositions.get(i).)
                    }
                    break;
                case ON_BUILDING:
                     
                    Cell tmpBuilding = (Cell)value[3];
                    for(BuildingCell building : firemap.keySet()) {
                    
                    if((building.getCol()==tmpBuilding.getCol())&&(building.getRow()==tmpBuilding.getRow()))
                    {
                        if(!firemenOnFire.contains(building)) // we add building to list so we don't increse fire in updateFireRatio
                        {
                            firemenOnFire.add(building);
                        }
                        building.updateBurnedRatio(-game.getFireSpeed());
                        
                        if(building.getBurnedRatio()<=0)//if we saved building we can remove from fire from firemap
                        {
                          if(!firemoveFire.contains(building))//we dont want duplicats
                          {
                              firemoveFire.add(building);
                          }
                          
                        }
                    }
                    }
                    break;
            }
            
        }
        
        
        if(!firemoveFire.isEmpty()) //remove building from firemap
        {
            for(int i = 0; i<firemoveFire.size(); i++)
            {
                firemap.remove(firemoveFire.get(i));
                firemenOnFire.remove(firemoveFire.get(i));
                
              
            }
            
        }
        
        
        agentList.put(AgentType.FIREMAN, FiremanPositions);
        
        for(Cell toremove : agentCellsToRemove)
        {
            StreetCell tmpCell = (StreetCell)game.get(toremove.getRow(), toremove.getCol());
            try {
                tmpCell.removeAgentt();
            } catch (Exception ex) {
                Logger.getLogger(CentralAgent.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        
        agentCellsToRemove.clear();
        
        
        /*long randomSeed = game.getSeed();

        List<Cell> privateVehiclesPositions = agentList.get(AgentType.PRIVATE_VEHICLE);
        List<Cell> newPrivateVehiclesPositions = new ArrayList();
        //iterate in private vehicules
        for(Cell privateVehiclePosition : privateVehiclesPositions) {
            InfoAgent privateVehicle = ((StreetCell)privateVehiclePosition).getAgent();

            boolean tryTurn = randomCoin(20); //a probability of turn right or left
            try {
                Cell newPosition = privateVehicle.randomMovement(game.getMap(), privateVehiclePosition, tryTurn);
                
                newPrivateVehiclesPositions.add(newPosition);
            } catch (Exception e) {
                newPrivateVehiclesPositions.add(privateVehiclePosition);
                System.err.println("Error moving vehicle");
            }
        }
        agentList.put(AgentType.PRIVATE_VEHICLE, newPrivateVehiclesPositions);*/
    }
    
    
    /*
     * Update the fires ratio of burning buildings
     */
    protected void updateFiresRatio() {
        //get all the burning fires
        Map<BuildingCell, Integer> firemap = game.getFireList();
       
                
        for(BuildingCell building : firemap.keySet()) {
            if(!firemenOnFire.contains(building)){
          
            int fireSpeed = firemap.get(building);
            building.updateBurnedRatio(fireSpeed);

            //logging the burning build
            int row = building.getRow();
            int col = building.getCol();
            log("Building in (" + Integer.toString(col) + "," + Integer.toString(row) + ") is being burned with burnratio " + Integer.toString(fireSpeed));
            }
            
        }
        
        
    }

    /*
     * Set a fire in a random building
     */
    protected Map<BuildingCell, Integer> setFire() {
        Map<BuildingCell, Integer> newFire = new HashMap();
        //get a building to put fire on it
        int fireSpeed = game.getFireSpeed();
        BuildingCell building = getRandomBuilding();

        //building on fire if it don't have fire
        if(building != null && ! building.isOnFire()) {
            Map<BuildingCell, Integer> firemap = game.getFireList();
            firemap.put(building, fireSpeed);
            newFire.put(building, fireSpeed);
        }
        return newFire;
    }

    /*
     * Get a threshold probability
     * trueProbability should be between 0 and 100
     */
    protected boolean randomCoin(int trueProbability) {
        int randomNum = random.nextInt(100) + 1;

        return randomNum < trueProbability;
    }

    /*
     * Set fires on the city
     * There is a probability that a fire occur in a building
     */
    protected Map<BuildingCell, Integer> addNewFire() {
        boolean fireProb = randomCoin(10); //a probability of add a new fire
        Map<BuildingCell, Integer> newFire;

        //add a fire with a fireProb
        if(fireProb) {
            log("setting a fire");
            newFire = setFire();
        }
        else {
            newFire = new HashMap();
        }

        return newFire;
    }

    /*
     * Print all the statistics of the step.
     * +Percentage of fires that had firemen trying to put it out.
     * +Percentage of fires that were totally put out.
     * +Average of burned ratio of buildings when the first fireman arrived.
     * +Percentage of people in risk due to fires.
     * +Percentage of people brought to hospitals.
     * +Percentage of dead people due to fires.
     * +Percentage of occupied beds by simulation steps, and their average.
     * +The number of buildings destroyed. This value is gathered by the SystemAgent and it is only zero if all fires are put out and no building is destroyed.
     * +The number of dead citizens. This value is also gathered by the SystemAgent and it will show the number of fire victims. Each building on fire has an amount of citizens in it. This statistic value will be only zero when no citizens have died due to the fires.
     */
    protected void showStatistics() {
        //TODO
    }

    /*
     * Search for destroyed buildings (burned ratio = 100) and "kill" all the citizen on those buildings.
     * Return the number of citizens dead on the step.
     */
    protected int updateDeaths() {
        //get all the burning fires
        Map<BuildingCell, Integer> firemap = game.getFireList();

        Iterator<Map.Entry<BuildingCell, Integer>> iter = firemap.entrySet().iterator();

        int stepDeadCitizens = 0;
        while(iter.hasNext()) {
            Map.Entry<BuildingCell, Integer> building_bratio = iter.next();

            BuildingCell building = building_bratio.getKey();
            if(building.isDestroyed()) {
                //kill all the citizen on the building
                int deadCitizen = building.killCitizens();
                stepDeadCitizens += deadCitizen;
                totalDeadCitizens += deadCitizen;

                //remove the building from the fireList
                iter.remove();

                //logging the destroyed building
                int row = building.getRow();
                int col = building.getCol();
                log("Building in (" + Integer.toString(col) + "," + Integer.toString(row) + ") was destroyed by fire. " + deadCitizen + " citizens died in the building");
            }
        }

        return stepDeadCitizens;
    }

    /*
     * Move all the private vehicles on the city with the following rules:
     * -If a private vehicle is in any street, the private vehicle has to go straight on in the same direction.
     *  -If a private vehicle arrives at a street cell where the vehicle can turn right and/or left (like in a street cross), the private vehicle will have:
     *      +An 80% of probability of going straight on, and
     *      +A 20% of turning right or left. Both directions will have the same probability of being chosen whenever they both are available.
     *  -If the private vehicle arrives at a street cell from which cannot go straight on, it will have the same probability of turning right or left if both directions are available.
     */
    protected void movePrivateVehicles() {
        //get the list of all agents
        Map<AgentType, List<Cell>> agentList = game.getAgentList();

        long randomSeed = game.getSeed();

        List<Cell> privateVehiclesPositions = agentList.get(AgentType.PRIVATE_VEHICLE);
        List<Cell> newPrivateVehiclesPositions = new ArrayList();
        //iterate in private vehicules
        for(Cell privateVehiclePosition : privateVehiclesPositions) {
            InfoAgent privateVehicle = ((StreetCell)privateVehiclePosition).getAgent();

            boolean tryTurn = randomCoin(20); //a probability of turn right or left
            try {
                Cell newPosition = privateVehicle.randomMovement(game.getMap(), privateVehiclePosition, tryTurn);
                newPrivateVehiclesPositions.add(newPosition);
            } catch (Exception e) {
                newPrivateVehiclesPositions.add(privateVehiclePosition);
                System.err.println("Error moving vehicle");
            }
        }
        agentList.put(AgentType.PRIVATE_VEHICLE, newPrivateVehiclesPositions);
    }

    /*
     * This method is executed on each step
     */
    public Map<String, Object> simulationStep() {
        //update fires
        updateFiresRatio();
        //add fires
        Map<BuildingCell, Integer> newFire = addNewFire();
        //vehicles Movement
        movePrivateVehicles();
        //kill all the citizens of destroyed buildings
        
        
        
        int stepDeads = updateDeaths();
        //Show the statistics of the step
        showStatistics();
        Map<String, Object> stepData = new HashMap();
        stepData.put("new_fires", newFire);
        stepData.put("remove_fire", firemoveFire);
        firemoveFire = new ArrayList<>();
        return stepData;
    }

    private void createAgents()
    {
        Map<AgentType, List<Cell>> a = this.game.getAgentList();
        AgentContainer ac = this.getContainerController();
        List<Cell> FIR = a.get(AgentType.FIREMAN);
        List<Cell> AMB = a.get(AgentType.AMBULANCE);
        List<Cell> HOS = a.get(AgentType.HOSPITAL);
       //properties for hospital
        /*Object[] property = new Object[3];
        property[0] = this.game;
        property[1] = this.game.getStepsToHealth();

        */
        int i = 1;
        for (Cell HOS1 : HOS) {
            //here was the mistake, take in your account that poperty now is one object,
            //and you assign this object to all agents, when the last agent created,
            //the position value for all agents will be same
            //property[2]= HOS1;
            UtilsAgents.createAgent(ac, "hospitalAgent" + i, "cat.urv.imas.agent.HospitalAgent", new Object[] {this.game, this.game.getStepsToHealth(), HOS1});
            i++;
        }


        //properties for ambulance 
        /*
        //properties for ambulance
        property = new Object[4];

        property[1] = this.game;
        property[2]= this.game.getAmbulanceLoadingSpeed();
        property[3]= this.game.getPeoplePerAmbulance();
        */

        i = 1;
        for (Cell AMB1 : AMB) {

            
            //property[0]= AMB1;
            UtilsAgents.createAgent(ac, "ambulanceAgent" + i, "cat.urv.imas.agent.AmbulanceAgent", new Object[]{ AMB1, this.game, this.game.getAmbulanceLoadingSpeed(), this.game.getPeoplePerAmbulance() });

            i++;
        }

        i = 1;
        for (Cell FIR1 : FIR) {
            
            //property[0]= FIR1;
            UtilsAgents.createAgent(ac, "firemenAgent" + i, "cat.urv.imas.agent.FiremenAgent", new Object[]{ FIR1, this.game });
            i++;
        }        
    }

    /**
     * Agent setup method - called when it first come on-line. Configuration of
     * language to use, ontology and initialization of behaviours.
     */
    @Override
    protected void setup() {

        /* ** Very Important Line (VIL) ************************************* */
        this.setEnabledO2ACommunication(true, 1);

        // 1. Register the agent to the DF
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType(AgentType.CENTRAL.toString());
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

        // 2. Load game settings.
        this.game = InitialGameSettings.load("game.settings");
        log("Initial configuration settings loaded");

        // 3. Load GUI
        try {
            this.gui = new GraphicInterface(game);
            gui.setVisible(true);
            log("GUI loaded");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // search CoordinatorAgent
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.COORDINATOR.toString());
        this.coordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);
        // searchAgent is a blocking method, so we will obtain always a correct AID

        // add behaviours
        // we wait for the initialization of the game
        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchProtocol(InteractionProtocol.FIPA_REQUEST), MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

        this.addBehaviour(new RequestResponseBehaviour(this, mt));

        // Setup finished. When the last inform is received, the agent itself will add
        // a behaviour to send/receive actions
        createAgents();
        
        firemenOnFire = new ArrayList<>();
        initRandom(game.getSeed());
    }

    public void updateGUI() {
        System.out.println("CENTRAL AGENT:" + this.game.get(2, 2).toString());
        this.gui.updateGame();
    }

}
