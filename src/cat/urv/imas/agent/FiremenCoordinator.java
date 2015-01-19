/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import cat.urv.imas.behaviour.firemenCoordinator.RequestResponseBehaviour;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.behaviour.coordinator.RequesterBehaviour;
import cat.urv.imas.map.BuildingCell;
import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.utils.MessageType;
import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.*;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Domen
 */
public class FiremenCoordinator extends ImasAgent{

    /**
     * Game settings in use.
     */
    private GameSettings game;
    /**
     * Central agent id.
     */
    private AID coordinatorAgent;

    public FiremenCoordinator() {
        super(AgentType.FIREMEN_COORDINATOR);
    }
    
    private Map<BuildingCell, Integer> firesTakenCareOf;

    /*
     * Inform that it finish the process of the step
     */
    private void informStepCoordinator() {
        ACLMessage stepMsg = new ACLMessage(ACLMessage.INFORM);
        stepMsg.clearAllReceiver();
        stepMsg.addReceiver(this.coordinatorAgent);
        try {
            stepMsg.setContentObject(new MessageContent(MessageType.DONE, null));
        } catch (Exception e) {
            e.printStackTrace();
        }
        send(stepMsg);
    }
    
    @Override
    protected void setup() {

        /* ** Very Important Line (VIL) ************************************* */
        this.setEnabledO2ACommunication(true, 1);

        // 1. Register the agent to the DF
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType(AgentType.FIREMEN_COORDINATOR.toString());
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
        
        firesTakenCareOf = new HashMap<>();
        
        // search CoordinatorAgent
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.COORDINATOR.toString());
        this.coordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);

        addBehaviour(new CyclicBehaviour(this)
        {
            @Override
            public void action() {
                ACLMessage msg = receive();
                        if (msg != null) {
                            System.out.println( " - " +
                               myAgent.getLocalName() + " <- " );
                              // msg.getContent() );
                            AID sender = msg.getSender();
                            
                            if(sender.equals(coordinatorAgent)) {
                                try {
                                    MessageContent mc = (MessageContent)msg.getContentObject();
                                    switch(mc.getMessageType()) {
                                        case INFORM_CITY_STATUS:

                                            GameSettings game = (GameSettings)mc.getContent();
                                            ACLMessage initialRequest = new ACLMessage(ACLMessage.INFORM);
                                            initialRequest.clearAllReceiver();
                                            ServiceDescription searchCriterion = new ServiceDescription();
                                            searchCriterion.setType(AgentType.FIREMAN.toString());  
                                            Map<AgentType, List<Cell>> a = game.getAgentList();
                                            List<Cell> FIR = a.get(AgentType.FIREMAN);
                                            setGame(game); //we need to set game, so we can get fires (for now) 
                                            int i = 1;
                                            for (Cell FIR1 : FIR) {
                                                searchCriterion.setName("firemenAgent" + i);
                                                initialRequest.addReceiver(UtilsAgents.searchAgent(this.myAgent, searchCriterion));
                                                i++;
                                            }

                                           try {

                                               initialRequest.setContentObject(new MessageContent(MessageType.INFORM_CITY_STATUS, game));
                                              // log("Request message content:" + initialRequest.getContent());
                                           } catch (Exception e) {
                                               e.printStackTrace();
                                           }
                                           newFires(); // don't forget to delete
                                           this.myAgent.send(initialRequest);                                        
                                            break;
                                        case NEW_FIRES:
                                            newFires(); //we will send location of new fire or fires
                                            break;
                                        default:
                                            this.block();
                                    }


                                   //this.send(initialRequest);

                                } catch (UnreadableException ex) {
                                    Logger.getLogger(HospitalCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                                }

                                ((FiremenCoordinator)myAgent).informStepCoordinator();                                
                            }
                            if(msg.getPerformative()==ACLMessage.PROPOSE)
                            {
                                String protocol = msg.getProtocol();
                            }
                        }
                        else {
                            block();
                        }
            }

        }
        );

        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchProtocol(InteractionProtocol.FIPA_REQUEST), MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        this.addBehaviour(new RequestResponseBehaviour(this, mt));

    }

    private void newFires()
    {
        Map<BuildingCell, Integer> temporaryFiremap = new HashMap<>();
        List<BuildingCell> tmpList = new ArrayList<>();
        
        for(Entry<BuildingCell, Integer> entry : this.game.getFireList().entrySet()) // we new fires to temporary map 
        {
            if(!firesTakenCareOf.containsKey(entry.getKey()))
            {
                tmpList.add(entry.getKey());
                temporaryFiremap.put(entry.getKey(), entry.getValue());
            }
        }
        
        ACLMessage initialRequest = new ACLMessage(ACLMessage.CFP);
        initialRequest.setSender(getAID());
        initialRequest.clearAllReceiver();
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.FIREMAN.toString());  
        Map<AgentType, List<Cell>> a = game.getAgentList();
        List<Cell> FIR = a.get(AgentType.FIREMAN);
        int i = 1;
        for (Cell FIR1 : FIR) {
            searchCriterion.setName("firemenAgent" + i);
            initialRequest.addReceiver(UtilsAgents.searchAgent(this, searchCriterion));
            i++;
        }

       try {

           initialRequest.setContentObject(new MessageContent(MessageType.NEW_FIRES, temporaryFiremap));
          // log("Request message content:" + initialRequest.getContent());
       } catch (Exception e) {
           e.printStackTrace();
       }
       
       this.send(initialRequest); 
        
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
    
    public Map<BuildingCell, Integer> getFiresTakenCareOf() {
        return firesTakenCareOf;
    }

    public void setFiresTakenCareOf(Map<BuildingCell, Integer> firesTakenCareOf) {
        this.firesTakenCareOf = firesTakenCareOf;
    }

}
