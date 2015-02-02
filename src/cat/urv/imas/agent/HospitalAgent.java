/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import static cat.urv.imas.agent.ImasAgent.OWNER;
import cat.urv.imas.behaviour.central.RequestResponseBehaviour;
import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import jade.proto.ContractNetResponder;

/**
 *
 * @author Domen
 */
public class HospitalAgent extends ImasAgent{

    /**
     * Game settings in use. So we can get city map 
     */
    private GameSettings game;
    
    private Cell hospitalCell;
    
    private int stepsToHealth;
    private int maxCapacity;
    private AID hospitalCoordinator;

    
    
    
    public HospitalAgent() {
        super(AgentType.HOSPITAL);
    }
    
    @Override
    protected void setup() {
        
        /* ** Very Important Line (VIL) ************************************* */
        this.setEnabledO2ACommunication(true, 1);

        // 1. Register the agent to the DF
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType(AgentType.HOSPITAL.toString());
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
        
        // search CoordinatorAgent
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.COORDINATOR.toString());
        this.hospitalCoordinator = UtilsAgents.searchAgent(this, searchCriterion);        
        
        //Set the arguments we get from central agent
        Object[] arg = this.getArguments();
        this.setGame((GameSettings)arg[0]);
        this.setStepsToHealth((int)arg[1]);
        this.setHospitalCell((Cell)arg[2]);
        this.maxCapacity = (int) arg[3];
        
        //addBehaviour(new CyclicBehaviour(this)
        //{
        //    @Override
        //    public void action() {
        //        ACLMessage msg= receive();
        //                if (msg!=null){
        //                    System.out.println( " - " +myAgent.getLocalName() + " <- " + msg.getContent() );
        //                    
        //                    
        //                }
        //    }
        //    
        //}
        //);
        
        this.addBehaviour(new ContractNetResponder(this, MessageTemplate.MatchSender(this.hospitalCoordinator)) {

            private AID ambulanceWinner;
            
            
            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {
                return super.handleCfp(cfp); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
                return super.handleAcceptProposal(cfp, propose, accept); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
                super.handleRejectProposal(cfp, propose, reject); //To change body of generated methods, choose Tools | Templates.
            }
            
            
            
        });
        
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
    
    public Cell getHospitalCell() {
        return hospitalCell;
    }

    public void setHospitalCell(Cell hospitalCell) {
        this.hospitalCell = hospitalCell;
    }

    public int getStepsToHealth() {
        return stepsToHealth;
    }

    public void setStepsToHealth(int stepsToHealth) {
        this.stepsToHealth = stepsToHealth;
    }
}


