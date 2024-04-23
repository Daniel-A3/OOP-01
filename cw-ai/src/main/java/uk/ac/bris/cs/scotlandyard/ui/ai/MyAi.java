package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import io.atlassian.fugue.Pair;
import org.controlsfx.control.tableview2.filter.filtereditor.SouthFilter;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;

public class MyAi implements Ai {


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

	public static int minimax(int mrXLocation , int depth, int alpha, int beta, boolean isMax, Board.GameState gameState) {
		List<Detective> detectives = detectivePieces(gameState);
		if (depth == 0) {
			return getScore(mrXLocation, detectives, gameState);
		}
		List<List<Move>> detectiveMovesCombinations = detectiveMoveCombination(gameState, mrXLocation);
		Board.GameState newGameState;
		if (isMax) {
			int bestValue = Integer.MIN_VALUE;
			for (Move move : gameState.getAvailableMoves()) {
				int newValue = minimax(mrXLocation, depth - 1, alpha, beta,  false, gameState.advance(move));
				bestValue = Math.max(newValue, bestValue);
				alpha = Math.max(alpha, bestValue);
				if (beta <= alpha) {
					break; // Beta cutoff
				}
			}
			return bestValue;
		} else {
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

	private static List<List<Move>> detectiveMoveCombination(Board.GameState gameState, int mrXLocation){
		ImmutableSet<Move> moves = gameState.getAvailableMoves();
		Piece detective = null;
		boolean last = false;
		List<List<Move>> combinedMoves = new ArrayList<>();
		int source = 0;
		for (Move move : moves){
			if (detective == null){
				detective = move.commencedBy();
				Piece finalDetective = detective;
				last = moves.stream().allMatch(move1 -> move1.commencedBy() == finalDetective);
				source = dijkstra(gameState.getSetup().graph, mrXLocation, getMoveSource(move));
			}
			if (move.commencedBy() != detective){ continue; }
			if (dijkstra(gameState.getSetup().graph, mrXLocation, getMoveDestination(move)) > source){ continue; }
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
		return combinedMoves;
	}
	private static int getScore(int mrXLocation, List<Detective> detectives, Board.GameState gameState) {
		int score = Integer.MAX_VALUE;
		for (Detective detective : detectives) {
			if (gameState.getDetectiveLocation(detective).isPresent()) {
				int distance = dijkstra(gameState.getSetup().graph, mrXLocation, gameState.getDetectiveLocation(detective).get());
				if (distance < score) score = distance;
			}
		}

		return score;
	}

	private static List<Detective> detectivePieces(Board.GameState gameState){
		ImmutableSet<Piece> players = gameState.getPlayers();
		// returns list of detective pieces
        return players.stream()
				.filter(Piece::isDetective)
				.map(piece -> (Detective) piece)
				.toList();
	}

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

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		var moves = board.getAvailableMoves().asList();
		int depth = 1;

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
		int bestValue = Integer.MIN_VALUE;
		Move bestMove = null;
		int alpha = Integer.MIN_VALUE;
		int beta = Integer.MAX_VALUE;
		for (Move move : moves) {
			int mrXDestination = getMoveDestination(move);
			int newValue = minimax(mrXDestination, depth, alpha, beta, false, gameState.advance(move));
			alpha = Math.max(alpha, newValue);
			if (newValue > bestValue) {
				bestValue = newValue;
				bestMove = move;
			}
			System.out.println(move+ ", " + newValue);
		}

		if (bestMove != null) { return bestMove; }
		//return moves.get(new Random().nextInt(moves.size()));
		else { return moves.get(new Random().nextInt(moves.size())); }
	}
}
