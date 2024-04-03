package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	private final class MyGameState implements GameState {
		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;
		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives){
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;

			this.moves = getAvailableMoves();
			this.winner = getWinner();

			// Constructor input validation
			//
			// Checks that mrX is the black piece
			if (!mrX.piece().webColour().equals("#000")) throw new IllegalArgumentException("MrX is not the black piece!");

			// Checks that graph is not empty
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty!");

			// Checks that all detectives have different locations
			if (detectives.stream().map(Player::location).distinct().count() != detectives.size())
				throw new IllegalArgumentException("Detectives locations overlap");

			// Check that there are no duplicate detective game pieces
			if (detectives.stream().map(Player::piece).distinct().count() != detectives.size())
				throw new IllegalArgumentException("Duplicate game piece!");

			// Checks that the detectives are indeed detective pieces
			if (detectives.stream().map(Player::piece).anyMatch(Piece::isMrX))
				throw new IllegalArgumentException("Detective player is not a detective piece!");

			// Checks that the detective does not have double or secret tickets
			if (detectives.stream().anyMatch(player -> player.has(Ticket.SECRET) || player.has(Ticket.DOUBLE)))
				throw new IllegalArgumentException("Detectives can't have secret or double tickets");

			// Checks if moves are empty
			if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
		}

		// Implementation of TicketBoard using factory pattern
		// Allows for different implementations of ticket board for mrX and detectives

		// Common product interface for ticket boards already defined in Board.TicketBoard

		// Concrete product #1
		public class MrXTicketBoard implements TicketBoard{
			Player MrX;
			MrXTicketBoard(Player Mrx){
				this.MrX = Mrx;
			}
			@Override
			public int getCount(@Nonnull Ticket ticket) {
				return MrX.tickets().get(ticket);
			}
		}

		// Concrete product #2
		public class DetectiveTicketBoard implements TicketBoard{
			Player Detective;
			DetectiveTicketBoard(Player Detective){
				this.Detective = Detective;
			}

			@Override
			public int getCount(@Nonnull Ticket ticket) {
				return Detective.tickets().get(ticket);
			}
		}

		// Base Creator
		public abstract class TicketBoardCreator{
			public abstract TicketBoard createTicketBoard();
		}

		// Concrete Creator #1
		public class MrXTicketBoardCreator extends TicketBoardCreator{
			Player MrX;
			MrXTicketBoardCreator(Player Mrx){
				this.MrX = Mrx;
			}
			@Override public TicketBoard createTicketBoard() {
				return new MrXTicketBoard(this.MrX);
			}
		}

		// Concrete Creator #2
		public class DetectiveTicketBoardCreator extends TicketBoardCreator{
			Player Detective;
			DetectiveTicketBoardCreator(Player Detective){
				this.Detective = Detective;
			}
			@Override public TicketBoard createTicketBoard() {
				return new DetectiveTicketBoard(this.Detective);
			}
		}

		// Methods //
		@Override public GameSetup getSetup() {  return setup; }

		// Creates an immutable set of all pieces taking a part in the game
		public ImmutableSet<Piece> allPlayersPresent() {
			Set<Piece> players = new HashSet<Piece>();
			for (Player det:detectives){
				players.add(det.piece());
			}
			players.add(mrX.piece());
			final ImmutableSet<Piece> playersSet = ImmutableSet.copyOf(players);
			return playersSet;
		}

		@Override  public ImmutableSet<Piece> getPlayers() { return allPlayersPresent(); }
		@Override public Optional<Integer> getDetectiveLocation(Detective detective){
			for(Player det:detectives) {
				if (det.piece().equals(detective)) {
					return Optional.of(det.location());
				}
			}
			return Optional.empty();
		}

		@Override public Optional<TicketBoard> getPlayerTickets(Piece piece){
			if (mrX.piece().equals(piece)){
				return Optional.of(new MrXTicketBoardCreator(mrX).createTicketBoard());
			}
			for (Player player:detectives) {
				if (player.piece().equals(piece)) {
					return Optional.of(new DetectiveTicketBoardCreator(player).createTicketBoard());
				}
			}
			return Optional.empty();
		}

		public Set<Piece> determineWinner(){
			Set<Piece> winnerPieces = new HashSet<>();
			boolean detWon = false;
			//boolean detHaveMove = false;
			boolean AllDetHaveNotRunOutOfTickets = false;

			//boolean mrXHasMove = false;
			boolean mrXWon = false;

			// Checks if detectives have caught mrX
			for (Player det : detectives) {
				if (det.location() == mrX.location()) { detWon = true; }

				// Checks that the detectives have not run out of tickets
				if (det.hasAtLeast(Ticket.TAXI, 1)
					|| det.hasAtLeast(Ticket.BUS, 1)
					|| det.hasAtLeast(Ticket.UNDERGROUND, 1)){
					AllDetHaveNotRunOutOfTickets = true;
				}
			}
			// Checks if MrX has any available moves, if not, then the detectives win
			//if (setup.moves.contains(mrX.piece()))

			// TODO something is wrong with the logic here, causes most of the tests in GameStateGameOverTest to fail...
//			if (this.moves != null) {
//				for (Move availableMove : moves) {
//					if (availableMove.commencedBy() == mrX.piece()) {
//						mrXHasMove = true;
//					}
//				}
//			}
//			if (!mrXHasMove) { detWon = true; }

			// Mr X won if he has filled out his log and the detectives have not caught
			// him with their final moves
			//if (log.size())

			// Second implementation attempt
			// java.lang.NullPointerException: Cannot invoke "com.google.common.collect.ImmutableSet.iterator()" because "this.moves" is null
			// TODO why is this.moves null????!!!
//			if (!(moves == null)) {
//				if (!moves.isEmpty()) {
//					for (Move move : moves) {
//						if (move.commencedBy() == mrX.piece()) {
//							mrXHasMove = true;
//						} else {
//							detHaveMove = true;
//						}
//					}
//					// Detectives win if mrX has no moves
//					if (!mrXHasMove) {
//						detWon = true;
//					}
//					// Mr X wins if the detectives have no more moves left
//					if (!detHaveMove) {
//						mrXWon = true;
//					}
//				}
//			}

			// Checks if MrX is out of tickets
//			if (!(mrX.hasAtLeast(Ticket.TAXI, 1)
//					|| mrX.hasAtLeast(Ticket.BUS, 1)
//					|| mrX.hasAtLeast(Ticket.UNDERGROUND, 1)
//					|| mrX.hasAtLeast(Ticket.SECRET, 1))) {
//				detWon = true;
//			}

			// Mr X wins if all detectives run out of tickets
			if (!AllDetHaveNotRunOutOfTickets) { mrXWon = true; }

			// Adds all the detectives to the winner set if the detectives won
			if (detWon) {
				for (Player det : detectives) {
					winnerPieces.add(det.piece());
				}
				Set<Piece> remainingNew = new HashSet<>(Set.copyOf(remaining));
				remainingNew.remove(mrX.piece());
				remaining = ImmutableSet.copyOf(remaining);
			}
			else if (mrXWon) {
				winnerPieces.add(mrX.piece());
			}

			return winnerPieces;
		}

		@Override public ImmutableSet<Piece> getWinner(){
			return ImmutableSet.copyOf(determineWinner());
		}

		// Puts all available moves of detectives and MrX in a set
		public Set<Move> combineAvailableMoves(Set<Piece> remaining) {
			Set<Move> allAvailableMoves = new HashSet<>();

			if (getWinner().isEmpty()) {
				for (Piece player : remaining) {
					if (mrX.piece().equals(player)) {
						allAvailableMoves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
						allAvailableMoves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
					} else {
						for (Player det : detectives) {
							if (det.piece().equals(player)) {
								allAvailableMoves.addAll(makeSingleMoves(setup, detectives, det, det.location()));
							}
						}
					}
				}
			}
			return allAvailableMoves;
		}

		@Override public ImmutableSet<Move> getAvailableMoves(){  return ImmutableSet.copyOf(combineAvailableMoves(remaining));  }

		@Override public ImmutableList<LogEntry> getMrXTravelLog(){ return log; }

		@Override public void chooseMove(@Nonnull Move move){
			// TODO Advance the model with move, then notify all observers of what what just happened.
			//  you may want to use getWinner() to determine whether to send out Event.MOVE_MADE or Event.GAME_OVER
			
		}

		// returns the player that has made the move and updates their location and tickets
		public Player currentPlayer(Move move){
			if (move.commencedBy().isMrX()){
				return mrX;
			}
			for (Player det : detectives){
				if (det.piece().equals(move.commencedBy())) {
					return det;
				}
			}
			throw new IllegalArgumentException("Illegal move");
		}
		// Remove player that has made move from remaining list
		public ImmutableSet<Piece> removeAfterMove(Player current){
            Set<Piece> newRemaining = new HashSet<Piece>(remaining);
			if (current.isMrX()){
				newRemaining.remove(mrX.piece());
				// Adds all detectives with legal moves to the remaining
				// after mrX does his move
				newRemaining.addAll(combineAvailableMoves(detectives.stream().map(Player::piece).collect(Collectors.toCollection(HashSet::new)))
						.stream()
						.map(Move::commencedBy)
						.collect(Collectors.toCollection(HashSet::new)));
			}
			else{
				// Removes detective after their move
				// and adds mrX if all detectives have done their moves
				newRemaining.remove(current.piece());
				if (newRemaining.isEmpty()){ newRemaining.add(mrX.piece());}
			}
			remaining = ImmutableSet.copyOf(newRemaining);
			return remaining;
		}
		public void updateLocation(Move move){
			int newLocation = move.accept(new Visitor<Integer>(){
				@Override public Integer visit(SingleMove singleMove){ return singleMove.destination; }
				@Override public Integer visit(DoubleMove doubleMove){ return doubleMove.destination2; }
			});
			if (move.commencedBy().isMrX()){ mrX = mrX.at(newLocation);}
			else{
				List<Player> newDetectives = new ArrayList<>(detectives);
				for (Player det : newDetectives) {
					if (det.piece().equals(move.commencedBy())) {
						newDetectives.set(newDetectives.indexOf(det), det.at(newLocation));
						detectives = List.copyOf(newDetectives);
					}
				}
			}
		}
		public void updateTickets(Move move, Player current){
			int doubleTicket  = 0;
			Iterable<Ticket> tickets = move.accept(new Visitor<Iterable<Ticket>>() {

				@Override public Iterable<Ticket> visit(SingleMove singleMove) {
					List<Ticket> ticket = new ArrayList<>();
					ticket.add(singleMove.ticket);
					return ticket;
				}
				@Override public Iterable<Ticket> visit(DoubleMove doubleMove) {
					mrX = mrX.use(Ticket.DOUBLE);
					List<Ticket> tickets = new ArrayList<>();
					tickets.add(doubleMove.ticket1);
					tickets.add(doubleMove.ticket2);
					return tickets;
				}
			});
			// Takes used tickets away from mrx
			if (current.isMrX()){ mrX = mrX.use(tickets); }
			// Takes used ticket away form the detective and gives it to mrX
			else{
				mrX = mrX.give(tickets);
				List<Player> newDetectives = new ArrayList<>(detectives);
				newDetectives.set(newDetectives.indexOf(current), current.use(tickets));
				detectives = List.copyOf(newDetectives);
			}
		}
		public void updateLog(Move move) {
			List<LogEntry> newLog = new ArrayList<>(log);
			List<LogEntry> addLog = move.accept(new Visitor<List<LogEntry>>() {

				@Override public List<LogEntry> visit(SingleMove singleMove) {
					if (!setup.moves.get(log.size())) {
						List<LogEntry> unrevealed = new ArrayList<>();
						unrevealed.add(LogEntry.hidden(singleMove.ticket));
						return unrevealed;
					} else {
						List<LogEntry> revealed = new ArrayList<>();
						revealed.add(LogEntry.reveal(singleMove.ticket, singleMove.destination));
						return revealed;
					}
				}
				@Override public List<LogEntry> visit(DoubleMove doubleMove) {
					List<LogEntry> doubleLog = new ArrayList<>();
					if(setup.moves.get(log.size())){
						doubleLog.add(LogEntry.reveal(doubleMove.ticket1, doubleMove.destination1));
					}
					else{ doubleLog.add(LogEntry.hidden(doubleMove.ticket1)); }
					if(setup.moves.get(log.size()+1)){
						doubleLog.add(LogEntry.reveal(doubleMove.ticket2, doubleMove.destination2));
					} else { doubleLog.add(LogEntry.hidden(doubleMove.ticket2)); }
					return doubleLog;
				}
			});
			newLog.addAll(addLog);
			log = ImmutableList.copyOf(newLog);
		}
		@Override public GameState advance(Move move){
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			updateLocation(move);
			updateTickets(move, currentPlayer(move));
			remaining = ImmutableSet.copyOf(removeAfterMove(currentPlayer(move)));
			if(move.commencedBy() == mrX.piece()){ updateLog(move);}
			return new MyGameState(setup, remaining, log, mrX, detectives);
		}
		private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

			// Create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
			Set<SingleMove> SingleMoveSet = new HashSet<>();

			for (int destination : setup.graph.adjacentNodes(source)) {
				// Find out if destination is occupied by a detective
				//  If the location is occupied, don't add to the collection of moves to return
				if (detectives.stream().map(Player::location).noneMatch(location -> location.equals(destination))) {

					for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
						// Find out if the player has the required tickets
						//  If it does, construct a SingleMove and add it the collection of moves to return
						if (player.has(t.requiredTicket())) {
							SingleMoveSet.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
						}
					}
					// Consider the rules of secret moves here
					// Add moves to the destination via a secret ticket if there are any left with the player
					if (player.has(Ticket.SECRET)) {
						SingleMoveSet.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
					}
				}
			}

			// Return the collection of moves
			return SingleMoveSet;
		}
		private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player mrX, int source){

			if (mrX.isDetective()) throw new IllegalArgumentException("Only MrX can do double move");

			// Set containing all the available double moves
			Set<DoubleMove> DoubleMoveSet = new HashSet<>();
			// Set containing all the available single moves from mr X's starting location
			Set<SingleMove> SingleMoveSetInitial = new HashSet<>();
			// Set containing all the available single moves, from each available single move from SingleMoveSetInitial
			Set<SingleMove> SingleMoveSetSecond = new HashSet<>();

			SingleMoveSetInitial = makeSingleMoves(setup, detectives, mrX, source);

			// Iterates through the available single moves from the detectives location, and for each find all the possible
			// double moves.
			for (SingleMove singleMove1 : SingleMoveSetInitial) {
				SingleMoveSetSecond = makeSingleMoves(setup, detectives, mrX, singleMove1.destination);
				for (SingleMove singleMove2 : SingleMoveSetSecond) {
					if (mrX.has(Ticket.DOUBLE)) {
						// sets condition to check has enough tickets and it's not a reveal move to produce a double move
						if (((singleMove1.ticket != singleMove2.ticket)
						|| (singleMove1.ticket == singleMove2.ticket && mrX.hasAtLeast(singleMove2.ticket, 2)))
						&& (setup.moves.size()>=2)){
							DoubleMoveSet.add(new DoubleMove(mrX.piece(), source, singleMove1.ticket, singleMove1.destination,
								singleMove2.ticket, singleMove2.destination));
						}
					}
				}
			}
			return DoubleMoveSet;
		}
	}
	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
			return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

}
