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
		Set<Observer> observers = new HashSet<Observer>();
		private Board.GameState currentBoard;

		private MyModel(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
			this.currentBoard = new MyGameStateFactory().build(setup, mrX, detectives);
		}

		@Nonnull @Override public Board getCurrentBoard() { return currentBoard; }

		@Override public void registerObserver(@Nonnull Observer observer) {
			if (observers.contains(observer)) {
				throw new IllegalArgumentException("Can't register the same observer twice");
			} else if (observer == null) {
				throw new NullPointerException("Observer can't be null");
			} else { observers.add(observer); }
		}

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

		@Nonnull @Override public ImmutableSet<Observer> getObservers() { return ImmutableSet.copyOf(observers); }

		@Override public void chooseMove(@Nonnull Move move) {
			// TODO Advance the model with move, then notify all observers of what what just happened.
			//  you may want to use getWinner() to determine whether to send out Event.MOVE_MADE or Event.GAME_OVER
			if (!currentBoard.getAvailableMoves().contains(move)) throw new IllegalArgumentException("Game is Over!");
			currentBoard = currentBoard.advance(move);
            boolean gameOver = !currentBoard.getWinner().isEmpty();

			for (Observer observer : observers) {
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
		// TODO
		return new MyModel(setup, mrX, detectives);
	}
}