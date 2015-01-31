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
import cat.urv.imas.map.BuildingCell;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.HospitalCell;
import cat.urv.imas.map.StreetCell;
import cat.urv.imas.onthology.InfoAgent;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.utils.MessageType;
import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.*;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.stream.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central agent that controls the GUI and loads initial configuration settings.
 * TODO: You have to decide the onthology and protocol when interacting among
 * the Coordinator agent.
 */
public class CentralAgent extends ImasAgent {

    public static final String AGENT_PACKAGE = "cat.urv.imas.agent.";
    
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
    
    // List of fires
    
    private List<Cell> fires;    
    
    // Simulation step counter
    
    private int simulationStep;

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
    
    /**
     * Agent setup method - called when it first come on-line. Configuration of
     * language to use, ontology and initialization of behaviours.
     */
    @Override
    protected void setup() {


        super.setup();

        // 1. Register the agent to the DF
        if(this.registerService(AgentType.CENTRAL.toString())) {
            this.log("Service registered");
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

        //4. Create the agents
        
        this.createAgents();
        
        // search CoordinatorAgent
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.COORDINATOR.toString());
        this.coordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);
        
        // add behaviours
        // we wait for the initialization of the game
        //MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchProtocol(InteractionProtocol.FIPA_REQUEST), MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

        //this.addBehaviour(new RequestResponseBehaviour(this, mt));

        // Setup finished. When the last inform is received, the agent itself will add
        // a behaviour to send/receive actions
        
        this.fires = new ArrayList<>();
        
        this.addBehaviour(new CyclicBehaviour() { 
            
            private CentralAgent ca = CentralAgent.this;
            private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchProtocol(InteractionProtocol.FIPA_REQUEST), MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            @Override
            public void action() {
                ACLMessage msg = null;
                while((msg = ca.receive(mt)) != null) {
                    try {
                        MessageContent mc = (MessageContent) msg.getContentObject();
                        

                        switch(mc.getMessageType()) {
                            case TURN_IS_DONE:
                                List<Object[]> endSimulationData = (List<Object[]>) mc.getContent();
                                
                                ca.endSimulationTurn(endSimulationData);
                                break;
                            default:
                                ca.log("Message Content not understood");
                                break;
                        }
                    } catch (UnreadableException ex) {
                        Logger.getLogger(CoordinatorAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }                    
                }
                block();
                
            }
        });
        
        this.startSimulationStep();
    }
    
    private void performActions(List<Object[]> actions) {

    }
    
    private void performMovements(List<Object[]> endSimulationData) {
        Cell[][] currentMap = this.game.getMap();
        
        for (Cell[] cl : currentMap) {
            for (Cell c : cl) {
                if (c instanceof StreetCell) {
                    StreetCell sc = (StreetCell)c;
                    try {
                        if (sc.isThereAnAgent()) {
                            sc.removeAgent();
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(CentralAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        
        Map<AgentType, List<Cell>> content = new HashMap<>();
        
        for (Object[] sData : endSimulationData) {
            
            int row = Array.getInt(sData[1], 0);
            int col = Array.getInt(sData[1], 1);
            Cell position = new StreetCell(row, col);
            String agentName = sData[0].toString();
            if (agentName.startsWith("fireman")) {
                if (content.get(AgentType.FIREMAN) == null) {
                    List<Cell> positions = new ArrayList<>();
                    positions.add(position);
                    content.put(AgentType.FIREMAN, positions);
                } else {
                    List<Cell> positions = new ArrayList<>();
                    positions.addAll(content.get(AgentType.FIREMAN));
                    positions.add(position);
                    content.put(AgentType.FIREMAN, positions);
                }
                int agentIdx = Integer.valueOf(agentName.substring(agentName.length() - 1)) - 1;
                this.game.getAgentList().get(AgentType.FIREMAN).set(agentIdx, position);
            } else {
                if (content.get(AgentType.AMBULANCE) == null) {
                    List<Cell> positions = new ArrayList<>();
                    positions.add(position);
                    content.put(AgentType.AMBULANCE, positions);
                } else {
                    List<Cell> positions = new ArrayList<>();
                    positions.addAll(content.get(AgentType.AMBULANCE));
                    positions.add(position);
                    content.put(AgentType.AMBULANCE, positions);
                }
                int agentIdx = Integer.valueOf(agentName.substring(agentName.length() - 1)) - 1;
                this.game.getAgentList().get(AgentType.AMBULANCE).set(agentIdx, position);
            }
        }
        
        content.entrySet().stream().forEach((entry) -> {
            entry.getValue().stream().map((c) -> (StreetCell)currentMap[c.getRow()][c.getCol()]).forEach((sc) -> {
                try {
                    if (sc.isThereAnAgent()) {
                        sc.removeAgent();
                    }
                    sc.addAgent(new InfoAgent(entry.getKey()));
                } catch (Exception ex) {
                    Logger.getLogger(CentralAgent.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        });
        
    }
    
    private void createAgents() {
        
        jade.wrapper.AgentContainer container = this.getContainerController();
        // Create the FiremenCoordinatorAgent, HospitalCoordinatorAgent
        UtilsAgents.createAgent(container, "firemenCoord", CentralAgent.AGENT_PACKAGE + "FiremenCoordinator", new Object[] { this.game});
        UtilsAgents.createAgent(container, "hospCoord", CentralAgent.AGENT_PACKAGE + "HospitalCoordinator", new Object[] { this.game});
        
        // Create other agents
        Map<AgentType, List<Cell>> agentList = this.game.getAgentList();
        
        agentList
                .entrySet()
                .stream()
                .filter(es -> !es.getKey().toString().equalsIgnoreCase("private_vehicle"))
                .forEach(es -> {
                    String agentPrefix = es.getKey().toString().toLowerCase();
                    String agentClassName = agentPrefix.substring(0, 1).toUpperCase() + agentPrefix.substring(1);
                    
                    int[] idx =  { 1 };
                    
                    es.getValue().forEach(cell -> {
                        UtilsAgents.createAgent(container, 
                                String.format("%s%d", agentPrefix, idx[0]), 
                                String.format("%s%sAgent", CentralAgent.AGENT_PACKAGE, agentClassName),
                                new Object[] { CentralAgent.this.game, cell});
                        idx[0]++;
                    });                    
                });

        
    }
    
    private void startSimulationStep() {
        this.simulationStep++;
        
        if(true) {
            addNewFire();
        }
        this.sendNewGameInfo();
    }
    
    private void endSimulationTurn(List<Object[]> endSimulationData) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(CentralAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        
        this.performMovements(endSimulationData);
        this.updateFiresRatio();

        this.gui.updateGame();
     
        this.startSimulationStep();
    }
    
    private void addNewFire() {
        
        List<Cell> buildings = this.game.getClearBuildings();
        Random  random = new Random((int)this.game.getSeed());
        if (buildings.size() > 0) {
            int reandIdx = random.nextInt(buildings.size());
            ((BuildingCell)buildings.get(reandIdx)).updateBurnedRatio(this.game.getFireSpeed());
            this.game.setNewFire(buildings.get(reandIdx));
            
        }
        else {
            this.game.setNewFire(null);
        }
        
    }
    
    /*
     * Update the fires ratio of burning buildings
     */
    public void updateFiresRatio() {
        //get all the burning fires
        List<Cell> firemap = game.getBuildingsOnFire();

        firemap.forEach((building) -> {
            // check if there are some firemen agents closest to it
            int fCount = getFiremenArroundFire((BuildingCell) building);
            int fireSpeed = this.game.getFireSpeed();
            if(fCount > 0) {
                //if there? reduce the fire at aratio fireSpeed% for each fireman agent
                fireSpeed *= -fCount;
            }
            
            ((BuildingCell)building).updateBurnedRatio(fireSpeed);
            //logging the burning build
            int row = building.getRow();
            int col = building.getCol();
            log("Building in (" + Integer.toString(col) + "," + Integer.toString(row) + ") is being burned with burnratio " + Integer.toString(fireSpeed));
        }); //if the bulding is on fire
    }
    
    protected int getFiremenArroundFire(BuildingCell cell) {
        int[] f = { 0 };
        List<Cell> fCells  = this.game.getAgentList().get(AgentType.FIREMAN);
        fCells.stream().forEach((c) -> {
            for(int x = -1; x <= 1; x++) {
                for(int y = -1; y <= 1; y ++) {
                    if(x == 0 && y == 0) continue;
                    if(c.getRow() == cell.getRow() + x && c.getCol() == cell.getCol() + y ) {
                        f[0]++;
                    }
                }
            }
        });
        return f[0];
    }    
    
    private void sendNewGameInfo() {
        
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(this.coordinatorAgent);
        msg.setProtocol(InteractionProtocol.FIPA_REQUEST);
        log("Inform message to " + this.coordinatorAgent.getLocalName());
        try {
            msg.setContentObject(new MessageContent(MessageType.INFORM_CITY_STATUS, this.game));
            log("Inform message content: " + MessageType.INFORM_CITY_STATUS.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        this.send(msg);
    }
    
    public void updateGUI() {
        System.out.println("CENTRAL AGENT:" + this.game.get(2, 2).toString());
        this.gui.updateGame();
    }

}
