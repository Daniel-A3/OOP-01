package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;


public class MyAi implements Ai {

// Dijkstra algorithm to find the distance from mrX location to a given detective location
	public static Integer dijkstra(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
								   Integer source, Integer target) {
		Map<Integer, Integer> distance = new HashMap<>();
		Queue<Integer> pq = new LinkedList<>();
		Set<Integer> visited = new HashSet<>();

		for (Integer node : graph.nodes()) {
			distance.put(node, Integer.MAX_VALUE);
		}
		distance.put(source, 0);

		pq.add(source);

		while (!pq.isEmpty()) {
			Integer current = pq.poll();
			if (!visited.contains(current)) {
				if (current.equals(target)) {
					return distance.get(target);
				}
				visited.add(current);
				for (Integer adjacent : graph.adjacentNodes(current)) {
					int newDistance = distance.get(current) + 1;
					if (newDistance < distance.get(adjacent)) {
						distance.put(adjacent, newDistance);
						pq.add(adjacent);
					}
				}
			}
		}
		// Target can't be reached
		return -1;
	}

	public static Integer dijkstraFirstDetective(Board.GameState gameState,
												 Integer source, List<Detective> detectives) {
		Map<Integer, Integer> distance = new HashMap<>();
		Queue<Integer> pq = new LinkedList<>();
		Set<Integer> visited = new HashSet<>();
		ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph = gameState.getSetup().graph;
		Set<Integer> targets = new HashSet<>();
		for (Detective detective : detectives) {
			targets.add(gameState.getDetectiveLocation(detective).get());
		}

		for (Integer node : graph.nodes()) {
			distance.put(node, Integer.MAX_VALUE);
		}
		distance.put(source, 0);
		pq.add(source);

		while (!pq.isEmpty()) {
			Integer current = pq.poll();
			if (!visited.contains(current)) {
				if (targets.contains(current)) {
					//System.out.println("Distance " + distance.get(current));
					return distance.get(current);
				}
				visited.add(current);
				for (Integer adjacent : graph.adjacentNodes(current)) {
					int newDistance = distance.get(current) + 1;
					if (newDistance < distance.get(adjacent)) {
						distance.put(adjacent, newDistance);
						pq.add(adjacent);
					}
				}
			}
		}
		// Target can't be reached
		return -1;
	}

	// Minimax algorithm to pick the best board score for Mrx/Detectives
	public static int minimax(int mrXLocation , int depth, int alpha, int beta, boolean isMax, Board.GameState gameState) {
		if (depth == 0) {
			return getScore(mrXLocation, detectivePieces(gameState), gameState);
		}
		Board.GameState newGameState;
		if (isMax) {
			int bestValue = Integer.MIN_VALUE;
			Set<Move> bestMrXMoves = bestMrxMoves(gameState, mrXLocation);
			for (Move move : bestMrXMoves) {
				if (dijkstraFirstDetective(gameState.advance(move), getMoveDestination(move), detectivePieces(gameState.advance(move)))
				    > dijkstraFirstDetective(gameState, mrXLocation, detectivePieces(gameState))) {

					int newValue = minimax(mrXLocation, depth - 1, alpha, beta, false, gameState.advance(move));
					bestValue = Math.max(newValue, bestValue);
					alpha = Math.max(alpha, bestValue);
					if (beta <= alpha) {
						break; // Beta cutoff
					}
				}
			}
			return bestValue;
		} else {
			List<List<Move>> detectiveMovesCombinations = detectiveMoveCombination(gameState, mrXLocation);
			int bestValue = Integer.MAX_VALUE;
			for (List<Move> moves : detectiveMovesCombinations) {
				newGameState = gameState;
				for (Move move : moves) {
					newGameState = newGameState.advance(move);
					}
				int newValue = minimax(mrXLocation, depth - 1, alpha, beta, true, newGameState);
				bestValue = Math.min(newValue, bestValue);
				if (beta <= alpha) {
					break; // Alpha cutoff
				}
			}
			return bestValue;
		}
	}

	private static Set<Move> bestMrxMoves(Board.GameState gameState, int mrXLocation){
		Set<Move> bestMoves = new HashSet<>();
		List<Detective> detectives = detectivePieces(gameState);
		for (Move move : gameState.getAvailableMoves()) {
			if (dijkstraFirstDetective(gameState, mrXLocation, detectives) <=
			dijkstraFirstDetective(gameState.advance(move), getMoveDestination(move), detectives)){
				bestMoves.add(move);
			}
		}
		return bestMoves;
	}
	// Creates list of combinations of detective moves
	private static List<List<Move>> detectiveMoveCombination(Board.GameState gameState, int mrXLocation){
		ImmutableSet<Move> moves = gameState.getAvailableMoves();
		Piece detective = null;
		boolean last = false;
		List<List<Move>> combinedMoves = new ArrayList<>();
		int source = 0;
		for (Move move : moves){

			if (combinedMoves.size() < 2){
				if (detective == null) {
					detective = move.commencedBy();
					Piece finalDetective = detective;
					last = moves.stream().allMatch(move1 -> move1.commencedBy() == finalDetective);
					source = dijkstra(gameState.getSetup().graph, mrXLocation, getMoveSource(move));
				}
				if (move.commencedBy() == detective) {
					if (!(dijkstra(gameState.getSetup().graph, mrXLocation, getMoveDestination(move)) > source)) {
						Board.GameState advancedGameState = gameState.advance(move);
						if (last || !advancedGameState.getWinner().isEmpty()) {
							List<Move> newCombination = new ArrayList<>();
							newCombination.add(move);
							combinedMoves.add(newCombination);
						} else {
							List<List<Move>> newCombination = detectiveMoveCombination(gameState.advance(move), mrXLocation);
							for (List<Move> combinations : newCombination) {
								combinations.add(0, move);
								combinedMoves.add(combinations);
							}
						}
					}
				}
			}
		}
		return combinedMoves;
	}
	// Returns the board score
	private static int getScore(int mrXLocation, List<Detective> detectives, Board.GameState gameState) {
		//System.out.println("Score " + dijkstraFirstDetective(gameState, mrXLocation, detectives));
		return dijkstraFirstDetective(gameState, mrXLocation, detectives);
	}

	// Returns list of detectives (pieces)
	private static List<Detective> detectivePieces(Board.GameState gameState){
		ImmutableSet<Piece> players = gameState.getPlayers();
		// returns list of detective pieces
        return players.stream()
				.filter(Piece::isDetective)
				.map(piece -> (Detective) piece)
				.toList();
	}

	// Returns the location from which the move was made
	private static int getMoveSource(Move move){
		return move.accept(new Move.Visitor<>() {
            @Override
            public Integer visit(Move.SingleMove move) {
                return move.source();
            }

            @Override
            public Integer visit(Move.DoubleMove move) {
                return move.source();
            }
        });
	}

	// Returns the ticket/s used for the move made
	private static ScotlandYard.Ticket getMoveTicket (Move move){
		return move.accept(new Move.Visitor<>() {
			@Override
			public ScotlandYard.Ticket visit(Move.SingleMove move) {
				return move.ticket;
			}

			@Override
			public ScotlandYard.Ticket visit(Move.DoubleMove move) {
				return move.ticket2;
			}
		});
	}

	private static Set<ScotlandYard.Ticket> checkIfDouble (Move move){
		return move.accept(new Move.Visitor<>() {
			@Override
			public Set<ScotlandYard.Ticket> visit(Move.SingleMove move) {
				Set<ScotlandYard.Ticket> tickets = new HashSet<>();
				tickets.add(move.ticket);
				return tickets;
			}

			@Override
			public Set<ScotlandYard.Ticket> visit(Move.DoubleMove move) {
				Set<ScotlandYard.Ticket> tickets = new HashSet<>();
				tickets.add(move.ticket1);
				tickets.add(move.ticket2);
				return tickets;
			}
		});
	}

	// Returns the destination of the move
	private static int getMoveDestination(Move move){
		return move.accept(new Move.Visitor<>() {
            @Override
            public Integer visit(Move.SingleMove move) {
                return move.destination;
            }

            @Override
            public Integer visit(Move.DoubleMove move) {
                return move.destination2;
            }
        });
	}


	/*
	// Implementing the decorator design pattern to add functionality to the move class (a score attribute)
	public static class MoveDecorator implements Move {
		private Move move;
		private int score;

		public MoveDecorator(Move move) {
			this.move = move;
		}

		public int getScore() { return score; }

		public void setScore(int score) { this.score = score; }

		@Nonnull @Override public Piece commencedBy() { return move.commencedBy(); }

		@Nonnull @Override public Iterable<ScotlandYard.Ticket> tickets() { return move.tickets(); }

		@Override public int source() { return move.source(); }

		@Override public <T> T accept(Visitor<T> visitor) { return move.accept(visitor); }
	}
	*/

	@Nonnull @Override public String name() { return "Robot Sophia"; }

	// Picks the best move for MrX to take
	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		var moves = board.getAvailableMoves().asList();
		int depth = 3;

		List<Player> allDetectives = new ArrayList<>();
		Set<Piece> players = new HashSet<>(board.getPlayers());
		Optional<Player> mrX = Optional.empty();

		for (Piece player : players){
			Optional<Board.TicketBoard> tickets = board.getPlayerTickets(player);
			Map<ScotlandYard.Ticket, Integer> specificTicket = new HashMap<>();
			for (ScotlandYard.Ticket ticket : ScotlandYard.Ticket.values()) {
				specificTicket.put(ticket, tickets.get().getCount(ticket));
			}
			ImmutableMap<ScotlandYard.Ticket, Integer> immutableSpecificTicket = ImmutableMap.<ScotlandYard.Ticket, Integer>builder()
					.putAll(specificTicket)
					.build();
			if (player.isDetective()){
				Optional<Integer> detectiveLocation = board.getDetectiveLocation((Piece.Detective)player);
				Player newPlayer = new Player(player, immutableSpecificTicket, detectiveLocation.get());
				allDetectives.add(newPlayer);
			} else {
				int mrXLocation = moves.stream().findFirst().get().source();
				Player newPlayer = new Player(player, immutableSpecificTicket, mrXLocation);
				mrX = Optional.of(newPlayer);
			}
		}

		ImmutableList<Player> detectives = ImmutableList.copyOf(allDetectives);
		Board.GameState gameState = new MyGameStateFactory().build(board.getSetup(), mrX.get(), detectives);
		int bestScore = Integer.MIN_VALUE;
		List<Move> bestMoves = new ArrayList<>();
		int alpha = Integer.MIN_VALUE;
		int beta = Integer.MAX_VALUE;

		// Finds the best scored moves for MrX to take
		// Adds all the best moves to a list
		for (Move move : moves) {
			int mrXDestination = getMoveDestination(move);
			int newScore = minimax(mrXDestination, depth, alpha, beta, false, gameState.advance(move));
			alpha = Math.max(alpha, newScore);

			if (newScore > bestScore) {
				bestMoves = new ArrayList<>();
				bestScore = newScore;
				bestMoves.add(move);
			}
			else if (newScore == bestScore) {
				bestMoves.add(move);
			}
			System.out.println("Best score " + bestScore);
		}

		if (!bestMoves.isEmpty()) {


//				Set<ScotlandYard.Ticket> moveTickets = getMoveTicket(move);
//				 Checks if a move is a double move
//				 Decreases the score of double moves so that MrX is incentivised to only use them when necessary
//				Set<ScotlandYard.Ticket> moveTickets = getMoveTicket(move);
//				if (moveTickets.size() == 2) {
//					if (!gameState.getSetup().moves.get(gameState.getMrXTravelLog().size())) {
//
//					}
//				}
//			}
//			if (!bestMoves.stream().allMatch(move1 -> checkIfDouble(move1).size() == 2)){
//				bestMoves = bestMoves.stream().filter(move1 -> checkIfDouble(move1).size() == 1).toList();
//			}
//			if (bestMoves.stream().anyMatch(move1 -> gameState.getSetup()
//					.graph.edgeValue(getMoveSource(move1), getMoveDestination(move1))
//					.stream().anyMatch(set -> set.contains(ScotlandYard.Transport.UNDERGROUND)))){
//				bestMoves = bestMoves.stream().filter(move1 -> ((List<ScotlandYard.Ticket>) move1.tickets()).contains(ScotlandYard.Ticket.UNDERGROUND)).toList();
//			}

			System.out.println(bestMoves);
            return bestMoves.get(new Random().nextInt(bestMoves.size()));

			//return bestMoves.get(new Random().nextInt(bestMoves.size()));
		}

		else { return moves.get(new Random().nextInt(moves.size())); }
	}
}
