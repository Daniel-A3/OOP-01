package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
			Set<Move> mrXMoves = new HashSet<>();
			for (Move singleMove : gameState.getAvailableMoves()) {
				if (checkIfDouble(singleMove) == 1){
					mrXMoves.add(singleMove);
				}
			}
			if (mrXMoves.isEmpty()) { mrXMoves = gameState.getAvailableMoves(); }
			for (Move move : mrXMoves) {
				int newValue = minimax(getMoveDestination(move), depth - 1, alpha, beta, false, gameState.advance(move));
				bestValue = Math.max(newValue, bestValue);
				alpha = Math.max(alpha, bestValue);
				if (beta <= alpha) {
					break; // Beta cutoff
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

	private static int checkIfDouble (Move move){
		return move.accept(new Move.Visitor<>() {
			@Override
			public Integer visit(Move.SingleMove move) {

				return 1;
			}

			@Override
			public Integer visit(Move.DoubleMove move) {
				return 2;
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
				Optional<Integer> detectiveLocation = board.getDetectiveLocation((Detective)player);
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

		Set<Move> mrXMoves = new HashSet<>();
		if ((dijkstraFirstDetective(gameState, mrX.get().location(), detectivePieces(gameState)) > 2) &&
				(moves.stream().anyMatch(move -> checkIfDouble(move) == 2))) {
			for (Move move : moves) {
				if (checkIfDouble(move) == 1){
					mrXMoves.add(move);
				}
			}
		} else { mrXMoves = new HashSet<>(moves); }
		// Finds the best scored moves for MrX to take
		// Adds all the best moves to a list
		for (Move move : mrXMoves) {
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

			System.out.println(bestMoves);
            return bestMoves.get(new Random().nextInt(bestMoves.size()));

			//return bestMoves.get(new Random().nextInt(bestMoves.size()));
		}

		else { return moves.get(new Random().nextInt(moves.size())); }
	}
}
