package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;
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

			// Constructor input validation
			//
			// Checks that mrX is the black piece
			if(!mrX.piece().webColour().equals("#000")) throw new IllegalArgumentException("MrX is not the black piece!");

			List<Player> seen = new ArrayList<Player>();
			for (Player det:detectives){
				// Checks that all detectives have different locations
				for (Player ref : seen){
					if (det.location() == ref.location()) throw new IllegalArgumentException("Detectives have the same location!");
				}

				// Check that there are no duplicate game pieces
				if (!seen.contains(det)){
					seen.add(det);
				} else throw new IllegalArgumentException("Duplicate game piece!");

				// Checks that the detectives are indeed detective pieces
				if (!det.piece().isDetective()) throw new IllegalArgumentException("Detective player is not a detective piece!");
			}

			if(setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
		}
		@Override public GameSetup getSetup() {  return setup; }
		@Override  public ImmutableSet<Piece> getPlayers() { return remaining; }
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
				TicketBoard tBoard = new TicketBoard() {
					@Override
					public int getCount(@Nonnull Ticket ticket) {
						return mrX.tickets().get(ticket);
					}
				};
				return Optional.of(tBoard);
			}
			for(Player player:detectives){
				TicketBoard tBoard = new TicketBoard() {
					@Override
					public int getCount(@Nonnull Ticket ticket) {
						return player.tickets().get(ticket);
					}
				};
				if (player.piece().equals(piece)){
					return Optional.of(tBoard);
				}
			}

			return Optional.empty();
		};
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
