package edu.pilin.test_agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Random;

public class ClubAgent extends Agent {

	void print_message(String msg) {
		System.out.println("ClubAgent " + getAID().getName() + " " + msg);
	}

	protected void setup() {
		print_message("is ready!");
		addBehaviour(new ClubBehaviour(this, 10000));
	}

	protected enum ClubStates {
		CLOSED, OPENING, OPENED, CLOSING
	};

	ArrayList<AID> visitors = new ArrayList<AID>();
	BouncerBehaviour bouncer = new BouncerBehaviour();
	DJBehaviour dj = new DJBehaviour(this, 2000);

	protected void takeDown() {
		closeClub();

		print_message("terminating");
	}

	void openClub() {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType(ClubConstants.CLUB_SERVICE_TYPE);
		sd.setName("JADE-clubbing");
		dfd.addServices(sd);

		try {
			DFService.register(this, dfd);
		} catch (FIPAException e) {
			e.printStackTrace();
		}

		print_message("club opened");

		addBehaviour(bouncer);
		addBehaviour(dj);
	}

	void closeClub() {

		removeBehaviour(bouncer);
		removeBehaviour(dj);

		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			e.printStackTrace();
		}

		print_message("club closed");

		kickVisitors();
	}

	void kickVisitors() {
		if (!visitors.isEmpty()) {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			for (AID aid : visitors) {
				msg.addReceiver(aid);
			}
			msg.setOntology(ClubConstants.CLUBBING_ONTOLOGY);
			msg.setContent(ClubConstants.CLUB_CLOSED);
			send(msg);

			visitors.clear();

			print_message("visitors kicked");
		}
	}

	public class ClubBehaviour extends TickerBehaviour {

		public ClubBehaviour(Agent a, long period) {
			super(a, period);
		}

		ClubStates states = ClubStates.CLOSED;
		Random r = new Random();

		final int changeStateProb = 10;

		Boolean needChangeState() {
			return r.nextInt(100) >= changeStateProb;
		}

		@Override
		protected void onTick() {

			switch (states) {
			case CLOSED:
				if (needChangeState())
					states = ClubStates.OPENING;
				openClub();

				break;

			case OPENING:
				states = ClubStates.OPENED;
				break;

			case OPENED:
				if (needChangeState())
					states = ClubStates.CLOSING;
				
				break;

			case CLOSING:
				states = ClubStates.CLOSED;
				closeClub();

				break;

			default:
				break;
			}
		}

	}

	class BouncerBehaviour extends CyclicBehaviour {

		@Override
		public void action() {
			MessageTemplate template = MessageTemplate.or(MessageTemplate
					.MatchPerformative(ACLMessage.CFP), MessageTemplate
					.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL));

			ACLMessage msg = getAgent().receive(template);

			if (msg != null) {
				
				print_message("got request");

				switch (msg.getPerformative()) {
				case ACLMessage.CFP:
					ACLMessage reply = msg.createReply();
					reply.setPerformative(ACLMessage.PROPOSE);
					getAgent().send(reply);
					print_message("you look good");
					break;
				case ACLMessage.ACCEPT_PROPOSAL:
					visitors.add(msg.getSender());
					print_message("come in, " + msg.getSender());
					print_message("have 2 visitors");
				}
			} else {
				block();
			}
		}
	}

	class DJBehaviour extends TickerBehaviour {

		public DJBehaviour(Agent a, long period) {
			super(a, period);
		}

		@Override
		protected void onTick() {

			print_message("people on dance floor: " + visitors.size());
			if (!visitors.isEmpty()) {
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				for (AID aid : visitors) {
					msg.addReceiver(aid);
				}

				msg.setOntology(ClubConstants.CLUBBING_ONTOLOGY);
				msg.setContent(ClubConstants.MUSIC_CHANGED);

				send(msg);
			}

			print_message("music changed");
		}

	}

}
