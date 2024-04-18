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
		//
		// METHODS //
		//
		// returns setup
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
		// returns all player pieces registered in the game
		@Override  public ImmutableSet<Piece> getPlayers() { return allPlayersPresent(); }
		@Override public Optional<Integer> getDetectiveLocation(Detective detective){
			for(Player det:detectives) {
				if (det.piece().equals(detective)) {
					return Optional.of(det.location());
				}
			}
			return Optional.empty();
		}

		// Returns TicketBoard of a selected player
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

		// Determines winner(s) of the game if game is over
		public Set<Piece> determineWinner(){
			Set<Piece> winnerPieces = new HashSet<>();
			boolean detWon = false;
			boolean mrXWon = false;
			boolean AllDetHaveNotRunOutOfTickets = false;
			Set<Piece> detectivePieces = new HashSet<>();

			// Checks if detectives have caught mrX
			for (Player det : detectives) {
				if (det.location() == mrX.location()) { detWon = true; }

				// Checks that the detectives have not run out of tickets
				if (det.hasAtLeast(Ticket.TAXI, 1)
					|| det.hasAtLeast(Ticket.BUS, 1)
					|| det.hasAtLeast(Ticket.UNDERGROUND, 1)){
					AllDetHaveNotRunOutOfTickets = true;
				}
				detectivePieces.add(det.piece());
			}

			// Mr X wins if all detectives run out of tickets
			if (!AllDetHaveNotRunOutOfTickets) { mrXWon = true; }

			// Mr X wins if all detercives run out of legal moves
			if (combineAvailableMoves(detectivePieces).isEmpty() && !remaining.contains(mrX.piece())){
				mrXWon = true;
			}
			// Detectives win if Mr X is out of moves
			if (combineAvailableMoves(ImmutableSet.of(mrX.piece())).isEmpty() && remaining.contains(mrX.piece())) {
				detWon = true;
			}
			// Mr X wins if all log cells are used and Mr X hasn't been caught
			if (remaining.contains(mrX.piece())) {
				if (log.size() == setup.moves.size()) {
					mrXWon = true;
				}
			}

			// Adds all the detectives to the winner set if the detectives won
			if (detWon) {
				for (Player det : detectives) {
					winnerPieces.add(det.piece());
				}
				Set<Piece> remainingNew = new HashSet<>(Set.copyOf(remaining));
				remainingNew.remove(mrX.piece());
				remaining = ImmutableSet.copyOf(remaining);
			}
			// Adds Mr X to winner set if Mr X won
			else if (mrXWon) {
				winnerPieces.add(mrX.piece());
			}

			return winnerPieces;
		}

		// Returns set containing winner pieces if there are any, otherwise returns an empty set
		@Override public ImmutableSet<Piece> getWinner(){
			return ImmutableSet.copyOf(determineWinner());
		}

		// Puts all available moves of detectives if it's detectives turn in a set
		// Otherwise put all available moves of Mr X in a set
		public Set<Move> combineAvailableMoves(Set<Piece> remaining) {
			Set<Move> allAvailableMoves = new HashSet<>();

			for (Piece player : remaining) {
				if (mrX.piece().equals(player)) {
					allAvailableMoves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
					// mrX can't make a double move if there is only 1 log cell left (Otherwise game crashes)
					if (log.size() <= setup.moves.size()-2) {
						allAvailableMoves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
					}
				} else {
					for (Player det : detectives) {
						if (det.piece().equals(player)) {
							allAvailableMoves.addAll(makeSingleMoves(setup, detectives, det, det.location()));
						}
					}
				}
			}
			return allAvailableMoves;
		}

		// Returns all legal moves that can be made by detectives if it's their turn
		// Otherwise returns all legal moves that can be made by Mr X
		@Override public ImmutableSet<Move> getAvailableMoves(){
			if (!getWinner().isEmpty()) { return ImmutableSet.of(); }
			else { return ImmutableSet.copyOf(combineAvailableMoves(remaining)); }
		}

		// Returns Mr X Travel Log
		@Override public ImmutableList<LogEntry> getMrXTravelLog(){ return log; }

		// Returns the player that has made the move
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

		// Updates players location after they make a move
		public void updateLocation(Move move){
			// Uses visitor pattern to access private fields
			int newLocation = move.accept(new Visitor<Integer>(){
				// Check whether it's a single or double move
				@Override public Integer visit(SingleMove singleMove){ return singleMove.destination; }
				@Override public Integer visit(DoubleMove doubleMove){ return doubleMove.destination2; }
			});
			// Updates the location of the piece that has made a given move
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

		// Updates Tickets of the player that made a given move
		public void updateTickets(Move move, Player current){
			int doubleTicket  = 0;
			// Uses visitor pattern to access private fields
			Iterable<Ticket> tickets = move.accept(new Visitor<Iterable<Ticket>>() {
				// Check whether it's a single or double move
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
			// Takes used ticket(s) away from Mr X
			if (current.isMrX()){ mrX = mrX.use(tickets); }
			// Takes used ticket away form the detective and gives it to mrX
			else{
				mrX = mrX.give(tickets);
				List<Player> newDetectives = new ArrayList<>(detectives);
				newDetectives.set(newDetectives.indexOf(current), current.use(tickets));
				detectives = List.copyOf(newDetectives);
			}
		}

		// Updates Mr X's Travel Log
		public void updateLog(Move move) {
			List<LogEntry> newLog = new ArrayList<>(log);
			List<LogEntry> addLog = move.accept(new Visitor<List<LogEntry>>() {

				@Override public List<LogEntry> visit(SingleMove singleMove) {
					// Checks if it's a reveal move
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
					// Checks if it's a reveal move
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
			if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			if (!winner.isEmpty()) throw new IllegalArgumentException("Game is Over!");
			updateLocation(move);
			updateTickets(move, currentPlayer(move));
			remaining = ImmutableSet.copyOf(removeAfterMove(currentPlayer(move)));
			// Updates Mr X's Travel Log if the given move was made by him
			if(move.commencedBy() == mrX.piece()){ updateLog(move);}
			winner = ImmutableSet.copyOf(determineWinner());
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
