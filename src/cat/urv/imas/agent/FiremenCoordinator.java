/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import cat.urv.imas.behaviour.firemenCoordinator.RequestResponseBehaviour;
import cat.urv.imas.onthology.GameSettings;
import java.util.Iterator;
import cat.urv.imas.behaviour.coordinator.RequesterBehaviour;
import cat.urv.imas.map.BuildingCell;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.CellType;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.utils.MessageType;
import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.*;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
    
    public boolean initStep = true; // temporary - init step is not same as others 
    
    private int numberOfFiremen; 
    
    private Map<BuildingCell, Integer> newFires;
    
    private Map<AID, List<Integer>> firemanResponses; 
    
    private Map<AID, Object[]> firemanMove;  

    
    public FiremenCoordinator() {
        super(AgentType.FIREMEN_COORDINATOR);
    }
    
    private Map<BuildingCell, Integer> firesTakenCareOf;

    
    
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
                               myAgent.getLocalName() + " <- "  );
                              // msg.getContent() );
                            AID sender = msg.getSender();
                            
                            if(sender.equals(coordinatorAgent)) {
                                try {
                                    MessageContent mc = (MessageContent)msg.getContentObject();
                                    switch(mc.getMessageType()) {
                                        case INFORM_CITY_STATUS:
                                            Object[] messageRecived = (Object[])mc.getContent();
                                            GameSettings game = (GameSettings)messageRecived[0];
                                            ACLMessage initialRequest = new ACLMessage(ACLMessage.INFORM);
                                            if((messageRecived[1]!=null)&&(!((List<BuildingCell>)messageRecived[1]).isEmpty()))
                                            {
                                                removeFire((List<BuildingCell>)messageRecived[1]);
                                            }
                                            initialRequest.clearAllReceiver();
                                            ServiceDescription searchCriterion = new ServiceDescription();
                                            searchCriterion.setType(AgentType.FIREMAN.toString());  
                                            Map<AgentType, List<Cell>> a = game.getAgentList();
                                            List<Cell> FIR = a.get(AgentType.FIREMAN);
                                            setNumberOfFiremen(FIR.size());
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
                                           //newFires(); // don't forget to delete
                                           this.myAgent.send(initialRequest);                                        
                                            break;
                                        case NEW_FIRES:
                                            int asdf = 2;
                                            //newFires(); //we will send location of new fire or fires
                                            break;
                                        default:
                                            this.block();
                                    }


                                   //this.send(initialRequest);

                                } catch (UnreadableException ex) {
                                    Logger.getLogger(HospitalCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                                }

                                //((FiremenCoordinator)myAgent).informStepCoordinator();    
                                if(initStep)
                                {
                                ((FiremenCoordinator)myAgent).informStepCoordinator();
                                log("INIT");
                                newFires = new HashMap<>();
                                }else
                                {
                                    newFires(); // with this we start new step 
                                }
                            }
                            
                            if((!sender.equals(coordinatorAgent))&&msg.getPerformative()==ACLMessage.PROPOSE) // we can't ask if sender is firemen agent because there is not just one firemen agent 
                            {
                                //System.out.println( " - " +
                                //myAgent.getLocalName() + " <- "  + msg.getSender().toString());
                                
                                //responses on auction 
                                MessageContent mc;
                                try {
                                     mc = (MessageContent)msg.getContentObject();
                                     firemanResponses.put(msg.getSender(), (List<Integer>)mc.getContent());
                                     
                                } catch (UnreadableException ex) {
                                    Logger.getLogger(FiremenCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                if(firemanResponses.size()==numberOfFiremen)
                                {
                                    selectWinners();
                                }
                            }
                            
                            if((!sender.equals(coordinatorAgent))&&(msg.getPerformative()==ACLMessage.INFORM)) // we can't ask if sender is firemen agent because there is not just one firemen agent 
                            {
                                //we need to recive all steps
                                MessageContent mc;
                                try {
                                     mc = (MessageContent)msg.getContentObject();
                                     firemanMove.put(msg.getSender(), (Object[])mc.getContent());
                                     
                                } catch (UnreadableException ex) {
                                    Logger.getLogger(FiremenCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                if(firemanMove.size()==numberOfFiremen)
                                {
                                    informStepCoordinator();
                                }
                            }
                        }
                        else {
                            block();
                        }
            }

        }
        );

        //MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchProtocol(InteractionProtocol.FIPA_REQUEST), MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        //this.addBehaviour(new RequestResponseBehaviour(this, mt));

    }

    private void removeFire(List<BuildingCell> list)
    {
        List<BuildingCell> removeList = new ArrayList<>();
        for(BuildingCell cl : list)
        {
            for(BuildingCell fireCell : firesTakenCareOf.keySet()) // we new fires to temporary map 
            {
                if(fireCell.getCol()==cl.getCol()&&fireCell.getRow()==cl.getRow())
                {
                    removeList.add(fireCell);
                }
            }
            
        }
        for(BuildingCell cl : removeList)
        {
            firesTakenCareOf.remove(cl);
        }
        list.clear();
        
    }
    /*
     * Inform that it finish the process of the step
     */
    private void informStepCoordinator() {
        ACLMessage stepMsg = new ACLMessage(ACLMessage.INFORM);
        stepMsg.clearAllReceiver();
        stepMsg.addReceiver(this.coordinatorAgent);
        try {
            if(!initStep)//in the case of initialization step we send message null
            {
                stepMsg.setContentObject(new MessageContent(MessageType.DONE, firemanMove));
            }else
            {
                stepMsg.setContentObject(new MessageContent(MessageType.DONE, null));
                initStep = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        send(stepMsg);
    }
    
    private void selectWinners()
    {
        ACLMessage initialRequest = new ACLMessage(ACLMessage.REQUEST);
        initialRequest.setSender(getAID());
        initialRequest.clearAllReceiver();
        Object[] dataToFiremen = new Object[3];
        
        int i = 0;
        int stopSwitch = 0; //if all agents are MAXINT that means that they are working so we don't select winner 
        Map<AID, Integer> temporaryMap; //we create new map for each fire
        for(BuildingCell fireCell : this.newFires.keySet()) // we new fires to temporary map 
        {
            temporaryMap = new HashMap<>();
            for(Entry<AID, List<Integer>> entry : this.firemanResponses.entrySet()) // we new fires to temporary map 
            {
                temporaryMap.put(entry.getKey(), entry.getValue().get(i));
            }
            
            temporaryMap = (HashMap)sortByValues((HashMap) temporaryMap); // we sort the values
           
            int j = 0;
            int equalFires = 0;
            List<Cell> listOfFreeSpaces = getFreeSpacesAroundBuilding(fireCell);
            if((firesTakenCareOf.isEmpty())&&(newFires.size()==1))//first time we send all firemen to same fire 
            {
                equalFires = numberOfFiremen;
            }else
            {
                equalFires = (int)(numberOfFiremen/(firesTakenCareOf.size()+newFires.size()));
            }
            for(AID entry : temporaryMap.keySet()) 
            {
                if((j<equalFires)&&(j<listOfFreeSpaces.size())) // we only sent limited number of firemen to fire (equal number of firemen to fire && not more firemen to fire as there is free spaces )
                {   
                   dataToFiremen = new Object[3];
                   dataToFiremen[0] = listOfFreeSpaces.get(j); // we send fire cell (one of free cells)
                   dataToFiremen[1] = j;        // and on which place they finish 
                   dataToFiremen[2] = fireCell; //we also send fire cell to fireman

                   initialRequest.addReceiver(entry); 
                      try {
                          initialRequest.setContentObject(new MessageContent(MessageType.GO_TO_THIS_FIRE, dataToFiremen));
                      } catch (IOException ex) {
                          Logger.getLogger(FiremenCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                      }

                   send(initialRequest);
                   initialRequest.clearAllReceiver();
                   j++;
                }
            }
            i++;
            firesTakenCareOf.put(fireCell, fireCell.getBurnedRatio()); //we add fire to fires that are taken care of 
            newFires.remove(fireCell); // remove from new fire;
        }
        
        firemanMove = new HashMap<>();
        firemanResponses= new HashMap<>();
        request_step(); // after auction we requst new step
        
    }
    
    private void request_step() // send request to firen to make step
    {
        
        ACLMessage initialRequest = new ACLMessage(ACLMessage.REQUEST);
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

           initialRequest.setContentObject(new MessageContent(MessageType.REQUEST_STEP, null));
          // log("Request message content:" + initialRequest.getContent());
       } catch (Exception e) {
           e.printStackTrace();
       }
       //newFires(); // don't forget to delete
       this.send(initialRequest);  
    }
    
    
    private List<Cell> getFreeSpacesAroundBuilding(BuildingCell targetCell)//function return list of free cells (street cells) surrounding building 
    { 
        List<Cell> listOfFreeSpaces = new ArrayList<>();
        
        for(int i = -1; i<2;i++) {
            for(int j = -1; j<2;j++) {
                try {
                    if(this.game.get(targetCell.getRow() + i, targetCell.getCol()+ j).getCellType().equals(CellType.STREET)) {
                        listOfFreeSpaces.add((Cell)this.game.get(targetCell.getRow()+i, targetCell.getCol()+j));
                    }
                } 
                catch (Exception e) {
                }
            }
        }
        
        return listOfFreeSpaces; 
    }
    
    private boolean isFireNew(Entry<BuildingCell, Integer> fire) //we can't compare keys
    {
        for(Entry<BuildingCell, Integer> oldFire : firesTakenCareOf.entrySet())
        {
            if((oldFire.getKey().getCol()==fire.getKey().getCol())&&(oldFire.getKey().getRow()==fire.getKey().getRow()))
            {
                return false;
            }
        }
        return true;
    }
    
    
    
    private void newFires()
    {
        //informStepCoordinator
        //newFires = new HashMap<>();
        
        //List<BuildingCell> tmpList = new ArrayList<>();
        
        for(Entry<BuildingCell, Integer> entry : this.game.getFireList().entrySet()) // we new fires to temporary map 
        {
            
            if(!firesTakenCareOf.isEmpty())
            {
                //tmpList.add(entry.getKey());
                if(isFireNew(entry)) // we check is fire is really new
                { 
                    newFires.put(entry.getKey(), entry.getValue());
                    log("New fire at" + entry.getKey().toString());
                }
            }
            else // if there is no exsisting fire we know that fire is new 
            {
                newFires.put(entry.getKey(), entry.getValue());
                log("New fire at" + entry.getKey().toString());
            }
        }
        
        if(!newFires.isEmpty())
        {
        
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

           initialRequest.setContentObject(new MessageContent(MessageType.NEW_FIRES, newFires));
          // log("Request message content:" + initialRequest.getContent());
       } catch (Exception e) {
           e.printStackTrace();
       }
       
       firemanResponses = new HashMap<>();
       
       this.send(initialRequest); 
        }else
        {
            //agent make steps if there is no new fires 
            firemanMove = new HashMap<>();
            request_step();
            //this.informStepCoordinator();  
        }
    }
    /**
     * Update the game settings.
     *
     * @param game current game settings.
     */
    public void setGame(GameSettings game) {
        this.game = game;
    }
    
    public int getNumberOfFiremen() {
        return numberOfFiremen;
    }

    public void setNumberOfFiremen(int numberOfFiremen) {
        this.numberOfFiremen = numberOfFiremen;
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
    
    private static HashMap sortByValues(HashMap map)
    {
        List tmp = new LinkedList(map.entrySet());
        
        Collections.sort(tmp, new Comparator(){

            @Override
            public int compare(Object o1, Object o2) {
                return ((Comparable)((Map.Entry)(o1)).getValue()).compareTo(((Map.Entry)(o2)).getValue());
               
            }
            
        });
        HashMap sorted = new LinkedHashMap();
        for(Iterator it = tmp.iterator(); it.hasNext();){
        Map.Entry entry= (Map.Entry)it.next();
        sorted.put(entry.getKey(), entry.getValue());
        }
        return sorted;
    }
}
