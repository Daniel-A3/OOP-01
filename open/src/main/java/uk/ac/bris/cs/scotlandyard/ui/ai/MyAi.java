package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;

public class MyAi implements Ai {


	public static Integer dijkstra(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
								   Integer source, Integer target) {
		Map<Integer, Integer> distance = new HashMap<>();
		PriorityQueue<Integer> pq = new PriorityQueue<>();
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

		return -1;
	}

	public static int minimax(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
							  int source, int depth, boolean isMax, Board board) {
		if (depth == 0) {
			return getScore(graph, source, board);
		}

		if (isMax) {
			int bestValue = Integer.MIN_VALUE;
			//Move bestMove;
			for (Move move : board.getAvailableMoves()) {
				int newValue = Math.max(bestValue, minimax(graph, move.source(),depth - 1, true, board));
				if (newValue > bestValue){
					bestValue = newValue;
					//bestMove = move;
				}
			}
			return bestValue;
		} else {
			int bestValue = Integer.MAX_VALUE;
			//Move bestMove;
			for (Move move : board.getAvailableMoves()) {
				int newValue = Math.min(bestValue, minimax(graph, move.source(),depth - 1, false, board));
				if (newValue < bestValue){
					bestValue = newValue;
					//bestMove = move;
				}
			}
			return bestValue;
		}
	}

	private static int getScore(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
										int source, Board board) {
		int score = 0;
        Set<Piece> detPieces = new HashSet<>(board.getPlayers().stream().filter(Piece::isDetective).toList());

		for (Piece detective : detPieces) {
			if (board.getDetectiveLocation((Detective) detective).isPresent()) {
				int distance = dijkstra(graph, source, board.getDetectiveLocation((Detective) detective).get());
				score += distance;
			}
		}

		return score;
	}


	@Nonnull @Override public String name() { return "Sophia"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// returns a random move, replace with your own implementation
		var moves = board.getAvailableMoves().asList();

		for (var move : moves) {

		}

		return moves.get(new Random().nextInt(moves.size()));
	}
}
