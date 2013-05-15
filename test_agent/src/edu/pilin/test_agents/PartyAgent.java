package edu.pilin.test_agents;

import javax.swing.DefaultSingleSelectionModel;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class PartyAgent extends Agent {

	void print_message(String msg) {
		System.out.println("PartyAgent " + getAID().getName() + " " + msg);
	}

	protected void setup() {
		System.out.println("Hello! Party-agent " + getAID().getName()
				+ " is ready!");
		
		startSearch();
	}

	SearchClubBehaviour search = null;
	PartyBehaviour party = new PartyBehaviour();

	class SearchClubBehaviour extends TickerBehaviour {

		private MessageTemplate mt;
		private int step = 0;

		public SearchClubBehaviour(Agent a, long period) {
			super(a, period);
		}

		DFAgentDescription[] findClubs() throws FIPAException {
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription templateSd = new ServiceDescription();

			templateSd.setType(ClubConstants.CLUB_SERVICE_TYPE);
			template.addServices(templateSd);

			return DFService.search(getAgent(), template);
		}

		@Override
		protected void onTick() {

			switch (step) {
			case 0:
				;
				try {
					DFAgentDescription[] clubs = findClubs();
					if (clubs.length > 0) {
						print_message("clubs found");
						ACLMessage msg = new ACLMessage(ACLMessage.CFP);
						msg.setOntology(ClubConstants.CLUBBING_ONTOLOGY);
						msg.setReplyWith("cfp" + System.currentTimeMillis());

						for (DFAgentDescription descr : clubs) {
							AID club = descr.getName();
							msg.addReceiver(club);
						}

						getAgent().send(msg);

						mt = MessageTemplate.MatchInReplyTo(msg.getReplyWith());
						step = 1;
					}

				} catch (FIPAException e) {
					e.printStackTrace();
				}
				break;
			case 1:
				ACLMessage reply = getAgent().receive(mt);
				if (reply != null) {
					print_message("got reply");
					
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						ACLMessage message = reply.createReply();
						message.setPerformative(ACLMessage.ACCEPT_PROPOSAL);

						getAgent().send(message);

						startParty();

						// step = 0;
					}
				} else {
					block();
				}
			}

		}

	}

	class PartyBehaviour extends CyclicBehaviour {
		final MessageTemplate mt = MessageTemplate
				.MatchOntology(ClubConstants.CLUBBING_ONTOLOGY);

		@Override
		public void action() {
			ACLMessage message = receive(mt);
			if (message != null) {
				
				String content = message.getContent();
				print_message("got message from club: " + content);
				
				if (content.equals(ClubConstants.MUSIC_CHANGED)) {
					partyHard();
				}
				else if(content.equals(ClubConstants.CLUB_CLOSED))
				{
					print_message("club closed");
					startSearch();
				}
			} else {
				block();
			}
		}

	}

	public void startParty() {
		print_message("start party");
		
		removeBehaviour(search);
		addBehaviour(party);
	}

	public void partyHard() {
		
		print_message("party hard!");
		
	}

	public void startSearch() {
		print_message("starting search");
		
		search = new SearchClubBehaviour(this, 500);
		removeBehaviour(party);
		addBehaviour(search);
	}

}
