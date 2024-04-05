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
		private final Board.GameState currentBoard;

		private MyModel(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
			this.currentBoard = new MyGameStateFactory().build(setup, mrX, detectives);
		}

		@Nonnull @Override public Board getCurrentBoard() { return currentBoard; }

		@Override public void registerObserver(@Nonnull Observer observer) {
//            Set<Observer> observerSet = new HashSet<Observer>(observers);
//			observerSet.add(observer);
//			observers = ImmutableSet.copyOf(observerSet);
			if (!observers.contains(observer)) {
				observers.add(observer);
			}
		}

		@Override public void unregisterObserver(@Nonnull Observer observer) {
//			Set<Observer> observerSet = new HashSet<Observer>(observers);
//			observerSet.remove(observer);
//			observers = ImmutableSet.copyOf(observerSet);
			if (observers.contains(observer)) {
				observers.remove(observer);
			}
		}

		@Nonnull @Override public ImmutableSet<Observer> getObservers() { return ImmutableSet.copyOf(observers); }

		@Override public void chooseMove(@Nonnull Move move) {
			// TODO Advance the model with move, then notify all observers of what what just happened.
			//  you may want to use getWinner() to determine whether to send out Event.MOVE_MADE or Event.GAME_OVER
			if (currentBoard.getWinner().isEmpty()) {
				currentBoard.advance(move);
				for (Observer observer : observers) {
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
