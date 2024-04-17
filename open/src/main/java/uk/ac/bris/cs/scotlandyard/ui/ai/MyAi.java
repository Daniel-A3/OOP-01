package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

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
							  int source, Set<Integer> detectives, int depth, boolean isMax, Board board) {
		if (depth == 0 || board.getWinner().isEmpty()) {
			return getScore(graph, source, detectives);
		}

		if (isMax) {
			int bestValue = Integer.MIN_VALUE;
			for (Move move : board.getAvailableMoves()) {
				bestValue = Math.max(bestValue, minimax(graph, move.source(), detectives, depth - 1, true, board));
			}
			return bestValue;
		} else {
			int bestValue = Integer.MAX_VALUE;
			for (Move move : board.getAvailableMoves()) {
				bestValue = Math.min(bestValue, minimax(graph, move.source(), detectives, depth - 1, false, board));
			}
			return bestValue;
		}
	}

	private static int getScore(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
										int source, Set<Integer> detectives) {
		// Evaluation function: Negative distance to the closest detective
		int shortestDistance = Integer.MAX_VALUE;
		for (Integer detective : detectives) {
			int distance = dijkstra(graph, source, detective);
			shortestDistance = Math.min(shortestDistance, distance);
		}
		return -shortestDistance;
	}


	@Nonnull @Override public String name() { return "Name me!"; }

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
