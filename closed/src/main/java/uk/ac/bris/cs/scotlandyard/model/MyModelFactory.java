package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.HashSet;
import java.util.Set;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	private final class MyModel implements Model {
		// Set to contain all current observers
		Set<Observer> observers = new HashSet<Observer>();
		private Board.GameState currentBoard;

		private MyModel(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
			this.currentBoard = new MyGameStateFactory().build(setup, mrX, detectives);
		}

		@Nonnull @Override public Board getCurrentBoard() { return currentBoard; }
		// Registers new observer (adds them to the set)
		@Override public void registerObserver(@Nonnull Observer observer) {
			if (observers.contains(observer)) {
				throw new IllegalArgumentException("Can't register the same observer twice");
			} else if (observer == null) {
				throw new NullPointerException("Observer can't be null");
			} else { observers.add(observer); }
		}
		// Unregisters existing observer (removes them from the set)
		@Override public void unregisterObserver(@Nonnull Observer observer) {
			if (observer == null) {
				throw new NullPointerException("Observer can't be null");
			}
			else if (observers.contains(observer)) {
				observers.remove(observer);
			} else {
				throw new IllegalArgumentException("Can't unregister an observer who has not been registered yet");
			}
		}
		// Returns all current observers
		@Nonnull @Override public ImmutableSet<Observer> getObservers() { return ImmutableSet.copyOf(observers); }

		@Override public void chooseMove(@Nonnull Move move) {
			// Checks if given move is legal (Present in all available moves)
			if (!currentBoard.getAvailableMoves().contains(move)) throw new IllegalArgumentException("Move is not available!");
			// Updates game state according to the given move
			currentBoard = currentBoard.advance(move);
			// Check if game is over by checking if there are any winners
            boolean gameOver = !currentBoard.getWinner().isEmpty();
			// Goes over all observers and notifies them about new ingame changes
			for (Observer observer : observers) {
				// Check if game is over
				// If not - notifies observers about the move made
				// If over - notifies observers that game is over, doesn't notify them about the move since its not possible
				// after the end of the game
				if (gameOver) {
					observer.onModelChanged(currentBoard, Observer.Event.GAME_OVER);
				} else {
					observer.onModelChanged(currentBoard, Observer.Event.MOVE_MADE);
				}
			}
		}
	}

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		return new MyModel(setup, mrX, detectives);
	}
}
