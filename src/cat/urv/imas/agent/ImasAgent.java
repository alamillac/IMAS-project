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

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

/**
 * Agent abstraction used in this practical work.
 * It gathers common attributes and functionality from all agents.
 */
public class ImasAgent extends Agent {
    
    /**
     * Type of this agent.
     */
    protected AgentType type;
    
    /**
     * Agents' owner.
     */
    public static final String OWNER = "urv";
    /**
     * Language used for communication.
     */
    public static final String LANGUAGE = "serialized-object";
    /**
     * Onthology used in the communication.
     */
    public static final String ONTOLOGY = "serialized-object";
    
    /**
     * Creates the agent.
     * @param type type of agent to set.
     */
    public ImasAgent(AgentType type) {
        super();
        this.type = type;
    }
    
    /**
     * Informs the type of agent.
     * @return the type of agent.
     */
    public AgentType getType() {
        return this.type;
    }
    
    /**
     * Add a new message to the log.
     *
     * @param str message to show
     */
    public void log(String str) {
        System.out.println(getLocalName() + ": " + str);
    }
    
    /**
     * Add a new message to the error log.
     *
     * @param str message to show
     */
    public void errorLog(String str) {
        System.err.println(getLocalName() + ": " + str);
    }
    
    protected boolean registerService(String type) {
        // 1. Register the agent to the DF
        ServiceDescription sd = new ServiceDescription();
        sd.setType(type);
        sd.setName(this.getLocalName());
        sd.setOwnership(OWNER);

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.addServices(sd);
        dfd.setName(this.getAID());
        try {
            DFService.register(this, dfd);
            log("Registered to the DF");
            return true;
        } catch (FIPAException e) {
            System.err.println(getLocalName() + " failed registration to DF [ko]. Reason: " + e.getMessage());
            doDelete();
        }        
        return false;
    }

    @Override
    protected void setup() {
        super.setup(); //To change body of generated methods, choose Tools | Templates.
                /* ** Very Important Line (VIL) ************************************* */
        this.setEnabledO2ACommunication(true, 1);
    }
    
    
    
}
