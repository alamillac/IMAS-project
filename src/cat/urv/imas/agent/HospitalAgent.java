/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import static cat.urv.imas.agent.ImasAgent.OWNER;
import cat.urv.imas.behaviour.central.RequestResponseBehaviour;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.HospitalCell;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.utils.MessageType;
import cat.urv.imas.utils.Utils;
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
import jade.lang.acl.UnreadableException;
import jade.proto.AchieveREResponder;
import jade.proto.ContractNetResponder;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.newdawn.slick.util.pathfinding.Path;

/**
 *
 * @author Domen
 */
public class HospitalAgent extends ImasAgent{

    /**
     * Game settings in use. So we can get city map 
     */
    private GameSettings game;
    
    private HospitalCell hospitalCell;
    
    private int stepsToHealth;
    
    private int maxCapacity;
    
    private AID hospitalCoordinator;
    
    private List<AID> ambulanceAgents;
    
    
    
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
        searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.AMBULANCE.toString());
        this.ambulanceAgents = UtilsAgents.searchAgents(this, searchCriterion);
        
        //Set the arguments we get from central agent
        Object[] arg = this.getArguments();
        this.setGame((GameSettings)arg[0]);
        this.setStepsToHealth((int)arg[1]);
        this.hospitalCell = (HospitalCell) arg[2];
        
        addBehaviour(new CyclicBehaviour(this) {
            
            private HospitalAgent ha = HospitalAgent.this;
            private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchSender(hospitalCoordinator));
            
            @Override
            public void action() {
                ACLMessage msg = null;
                while ((msg = receive(mt)) != null) {
                    try {
                        
                        MessageContent mc = (MessageContent) msg.getContentObject();
                        
                        switch(mc.getMessageType()) {
                            case INFORM_CITY_STATUS:
                                ha.game = (GameSettings) mc.getContent();
                                break;    
                        }
                        
                    } catch (UnreadableException ex) {
                        Logger.getLogger(HospitalAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    System.out.println( " - " +myAgent.getLocalName() + " <- " + msg.getContent() );
                }
                this.block();
            }
            
        });
        
        this.addBehaviour(new ContractNetResponder(this, MessageTemplate.MatchSender(this.hospitalCoordinator)) {

            private AID ambulanceWinner;
            private float bestBid;
            private HospitalAgent ha = HospitalAgent.this;
            
            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {
                
                ACLMessage reply = cfp.createReply();
                reply.setPerformative(ACLMessage.PROPOSE);
                
                try {
                    MessageContent mc = (MessageContent) cfp.getContentObject();
                    Cell cell = (Cell) mc.getContent();
                    
                    ACLMessage cfpA = new ACLMessage(ACLMessage.CFP);
                    cfpA.setContentObject(new MessageContent(MessageType.TAKE_INJURED, new Object[]{ mc.getContent(), ha.hospitalCell }));
                    ha.ambulanceAgents.forEach(agent -> {
                        cfpA.addReceiver(agent);
                    });
                    
                    ha.send(cfpA);
                    
                    MessageTemplate prpsMt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                    int responsesCount = 0;
                    bestBid = -1;
                    while(responsesCount <  ha.ambulanceAgents.size()) {
                        
                        ACLMessage rspns = null;
                        while((rspns = receive(prpsMt)) != null ) {
                            if(rspns.getSender().getLocalName().startsWith("amb")) {
                                responsesCount++;
                                float bid = Float.parseFloat(rspns.getContent());
                                if(bid > bestBid) {
                                    bestBid = bid;
                                    ambulanceWinner = rspns.getSender();
                                }
                            }
                        }
                        this.block();
                        
                    }
                    
                    float hBid = 1 / ha.hospitalCell.getAvaliableBeds() + bestBid;
                    
                    reply.setContent(hBid + "");
                }
                catch(Exception ex) {

                }

                return reply;                
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
    
    public HospitalCell getHospitalCell() {
        return hospitalCell;
    }

    public void setHospitalCell(HospitalCell hospitalCell) {
        this.hospitalCell = hospitalCell;
    }

    public int getStepsToHealth() {
        return stepsToHealth;
    }

    public void setStepsToHealth(int stepsToHealth) {
        this.stepsToHealth = stepsToHealth;
    }
}


