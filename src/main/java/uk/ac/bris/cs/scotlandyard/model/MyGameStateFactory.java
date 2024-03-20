package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;
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
		@Override  public ImmutableSet<Piece> getPlayers() {
			Set<Piece> players = new HashSet<Piece>();
			for (Player det:detectives){
				players.add(det.piece());
			}
			players.add(mrX.piece());
			final ImmutableSet<Piece> playersSet = ImmutableSet.copyOf(players);
			return playersSet;
		}
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
		@Override public ImmutableSet<Piece> getWinner(){ return winner; };
		@Override public ImmutableSet<Move> getAvailableMoves(){ return moves;};

		@Override public ImmutableList<LogEntry> getMrXTravelLog(){ return log;};
		@Override public GameState advance(Move move) {  return null;  }
	}
	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
			return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
		//throw new RuntimeException("Implement me!");

	}

}
