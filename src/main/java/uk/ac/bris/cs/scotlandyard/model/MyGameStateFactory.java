package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

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
			this.winner = ImmutableSet.of();

			this.moves = getAvailableMoves();

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
			for(Player player:detectives) {
				if (player.piece().equals(piece)) {
					return Optional.of(new DetectiveTicketBoardCreator(player).createTicketBoard());
				}
			}
			return Optional.empty();
		}
		@Override public ImmutableSet<Piece> getWinner(){ return winner; }

		// Puts all available moves of detectives and MrX in a set
		public Set<Move> combineAvailableMoves() {
			Set<Move> allAvailableMoves = new HashSet<>();
			for (Player player:detectives) {
				allAvailableMoves.addAll(makeSingleMoves(setup, detectives, player, player.location()));
			}
			allAvailableMoves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
			return allAvailableMoves;
		}

		@Override public ImmutableSet<Move> getAvailableMoves(){ return ImmutableSet.copyOf(combineAvailableMoves()); }

		@Override public ImmutableList<LogEntry> getMrXTravelLog(){ return log;};
		@Override public GameState advance(Move move){
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			return new MyGameState(setup, remaining, log, mrX, detectives);
		}
		private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

			// TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
			Set<SingleMove> SingleMoveSet = new HashSet<>();

			for(int destination : setup.graph.adjacentNodes(source)) {
				// TODO find out if destination is occupied by a detective
				//  if the location is occupied, don't add to the collection of moves to return
				if (detectives.stream().map(Player::location).noneMatch(location -> location.equals(destination))) {

					for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
						// TODO find out if the player has the required tickets
						//  if it does, construct a SingleMove and add it the collection of moves to return
						if (player.has(t.requiredTicket())) {
							SingleMoveSet.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
						}
					}
					// TODO consider the rules of secret moves here
					//  add moves to the destination via a secret ticket if there are any left with the player
					if (player.has(Ticket.SECRET)) {
						SingleMoveSet.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
					}
				}
			}

			// TODO return the collection of moves
			return SingleMoveSet;
		}
		private static Set<SingleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
			// Make sure detectives cant
			return null;
		}
	}
	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
			return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

}
