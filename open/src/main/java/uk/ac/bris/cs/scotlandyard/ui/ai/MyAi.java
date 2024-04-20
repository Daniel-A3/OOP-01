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
		// Target can't be reached
		return -1;
	}

	public static int minimax(int source, int depth, boolean isMax, Board board) {
		ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph = board.getSetup().graph;

		if (depth == 0) {
			return getScore(graph, source, board);
		}

		if (isMax) {
			int bestValue = Integer.MIN_VALUE;
			Move bestMove;
			for (Move move : board.getAvailableMoves()) {
				int target = move.accept(new Move.Visitor<>() {
                    @Override
                    public Integer visit(Move.SingleMove move) {
                        return move.destination;
                    }

                    @Override
                    public Integer visit(Move.DoubleMove move) {
                        return move.destination2;
                    }
                });
				int newValue = Math.max(bestValue, minimax(target,depth - 1, false, board));
				if (newValue > bestValue){
					bestValue = newValue;
					bestMove = move;
				}
			}
			return bestValue;
		} else {
			int bestValue = Integer.MAX_VALUE;
			//Move bestMove;
			for (Move move : board.getAvailableMoves()) {
				int target = move.accept(new Move.Visitor<>() {
                    @Override
                    public Integer visit(Move.SingleMove move) {
                        return move.destination;
                    }

                    @Override
                    public Integer visit(Move.DoubleMove move) {
                        return move.destination2;
                    }
                });
				int newValue = Math.min(bestValue, minimax(,target,depth - 1, true, board));
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
		int score = Integer.MAX_VALUE;
        Set<Piece> detPieces = new HashSet<>(board.getPlayers().stream().filter(Piece::isDetective).toList());
		Set<List<Integer>> detDestinations;

		for (Piece detective : detPieces) {
			if (board.getDetectiveLocation((Detective) detective).isPresent()) {
				int distance = dijkstra(graph, source, board.getDetectiveLocation((Detective) detective).get());
				if ( distance<score ) score = distance;
			}
		}

		return score;
	}


	@Nonnull @Override public String name() { return "Sophia"; }

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
			if (player.isDetective()){
				Optional<Integer> detectiveLocation = board.getDetectiveLocation((Piece.Detective)player);
				Player newPlayer = new Player(player, (ImmutableMap<ScotlandYard.Ticket, Integer>) specificTicket, detectiveLocation.get());
				allDetectives.add(newPlayer);
			} else {
				int mrXLocation = moves.stream().findFirst().get().source();
				Player newPlayer = new Player(player, (ImmutableMap<ScotlandYard.Ticket, Integer>) specificTicket, mrXLocation);
				mrX = Optional.of(newPlayer);
			}
		}
		Board.GameState gameState = new MyGameStateFactory().build(board.getSetup(), mrX.get(), (ImmutableList<Player>) allDetectives);
		int bestValue = Integer.MIN_VALUE;
		Move bestMove = null;
		for (Move move : moves) {
			int target = move.accept(new Move.Visitor<>() {
				@Override
				public Integer visit(Move.SingleMove move) {
					return move.destination;
				}

				@Override
				public Integer visit(Move.DoubleMove move) {
					return move.destination2;
				}
			});
			int newValue = Math.max(bestValue, minimax(target, depth - 1, true, board));
			if (newValue > bestValue) {
				bestValue = newValue;
				bestMove = move;
			}
		}

		if (bestMove != null) { return bestMove; }
		//return moves.get(new Random().nextInt(moves.size()));
		else { return moves.get(new Random().nextInt(moves.size())); }
	}
}
