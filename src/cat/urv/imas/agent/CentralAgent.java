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

import cat.urv.imas.onthology.InitialGameSettings;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.gui.GraphicInterface;
import cat.urv.imas.behaviour.central.RequestResponseBehaviour;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.BuildingCell;
import jade.core.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.*;
import jade.wrapper.AgentContainer;
import java.util.List;
import java.util.Map;
import jade.core.behaviours.TickerBehaviour;
import java.util.Random;


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
     * Actual step
     */
    private int numStep = 0;

    /**
     * Builds the Central agent.
     */
    public CentralAgent() {
        super(AgentType.CENTRAL);
    }

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
            int building_id = random.nextInt(num_buildings) + 1;
            building = buildings.get(building_id);
        }
        else {
            building = null;
        }
        return building;
    }

    /*
     * Update the fires ratio of burning buildings
     */
    protected void updateFiresRatio() {
        //get all the burning fires
        Map<BuildingCell, Integer> firemap = game.getFireList();

        for(BuildingCell building : firemap.keySet()) {
            int fireSpeed = firemap.get(building);
            building.updateBurnedRatio(fireSpeed);

            //logging the burning build
            int row = building.getRow();
            int col = building.getCol();
            log("Building in (" + Integer.toString(row) + "," + Integer.toString(col) + ") is being burned with burnratio " + Integer.toString(fireSpeed));
        }
    }

    /*
     * Set a fire in a building
     */
    protected void setFire() {
        //get a building to put fire on it
        int fireSpeed = game.getFireSpeed();
        BuildingCell building = getRandomBuilding();

        //building on fire if it don't have fire
        if(building != null && ! building.isOnFire()) {
            Map<BuildingCell, Integer> firemap = game.getFireList();
            firemap.put(building, fireSpeed);
        }
    }

    /*
     * Set fires on the city
     */
    protected void addNewFire() {
        int randomNum = random.nextInt(100) + 1;
        int fireProb = 70; //a probability of add a new fire

        //add a fire with a fireProb
        if(randomNum < fireProb) {
            log("setting a fire");
            setFire();
        }
    }

    /*
     * This method is executed on each step
     */
    protected void simulationStep() {
        //update fires
        updateFiresRatio();
        //add fires
        addNewFire();
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


        Map<AgentType, List<Cell>> a = this.game.getAgentList();
        AgentContainer ac = this.getContainerController();
        List<Cell> FIR = a.get(AgentType.FIREMAN);
        List<Cell> AMB = a.get(AgentType.AMBULANCE);
        List<Cell> HOS = a.get(AgentType.HOSPITAL);

        int i = 1;
        for (Cell HOS1 : HOS) {
            UtilsAgents.createAgent(ac, "hospitalAgent" + i, "cat.urv.imas.agent.HospitalAgent", null);
            i++;
        }

        i = 1;
        for (Cell AMB1 : AMB) {
            UtilsAgents.createAgent(ac, "ambulanceAgent" + i, "cat.urv.imas.agent.AmbulanceAgent", null);
            i++;
        }

        i = 1;
        for (Cell FIR1 : FIR) {
            UtilsAgents.createAgent(ac, "firemenAgent" + i, "cat.urv.imas.agent.FiremenAgent", null);
            i++;
        }

        // Start the simulation. SimulationStep will be executed every 500 milsec
        initRandom(game.getSeed());
        final int maxSteps = game.getSimulationSteps();
        log("Simulation start. Running " + Integer.toString(maxSteps) + " steps");
        addBehaviour(new TickerBehaviour(this, 500) {
            protected void onTick() {
                if(numStep < maxSteps) {
                    //log the current step
                    log("Step " + Integer.toString(numStep));

                    simulationStep();

                    //redraw the map
                    updateGUI();
                    numStep ++;
                }
                else {
                    removeBehaviour(this);
                    log("Simulation completed");
                }
            }
        });
    }

    public void updateGUI() {
        System.out.println("CENTRAL AGENT:" + this.game.get(2, 2).toString());
        this.gui.updateGame();
    }

}
