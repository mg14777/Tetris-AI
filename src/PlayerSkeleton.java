import java.util.Arrays;

public class PlayerSkeleton {
	private static final double WEIGHTS[] = { -2.76744786096722, 7.109539166236581, -2.2476784673295653,
			-7.486601242281183, -10.0, -2.76442687047878 };
	private final double weightVector[];

	public class StateWrapper {
		// private final State mutableState;
		private int[][] field;
		private int[] top;
		private int rowsCleared = 0;
		private final int nextPiece;

		// Public constructor, for the first call, copying the outer state
		public StateWrapper(State state) {
			field = Arrays.stream(state.getField()).map(j -> j.clone()).toArray(int[][]::new);
			top = Arrays.copyOf(state.getTop(), State.COLS);
			nextPiece = state.getNextPiece();
		}

		public boolean makeMove(int move) {
			return makeMove(State.legalMoves[nextPiece][move]);
		}

		// make a move based on an array of orient and slot
		public boolean makeMove(int[] move) {
			return makeMove(move[State.ORIENT], move[State.SLOT]);
		}

		// returns false if you lose - true otherwise and make the move in the
		// "fake" state
		public boolean makeMove(int orient, int slot) {
			// height if the first column makes contact
			int height = top[slot] - State.getpBottom()[nextPiece][orient][0];
			// for each column beyond the first in the piece
			for (int c = 1; c < State.getpWidth()[nextPiece][orient]; c++) {
				height = Math.max(height, top[slot + c] - State.getpBottom()[nextPiece][orient][c]);
			}

			// check if game ended
			if (height + State.getpHeight()[nextPiece][orient] >= State.ROWS + rowsCleared) {
				return false;
			}

			// for each column in the piece - fill in the appropriate blocks
			for (int i = 0; i < State.getpWidth()[nextPiece][orient]; i++) {

				// from bottom to top of brick
				for (int h = height + State.getpBottom()[nextPiece][orient][i]; h < height
						+ State.getpTop()[nextPiece][orient][i]; h++) {
					field[h][i + slot] = 1;
				}
			}

			// adjust top
			for (int c = 0; c < State.getpWidth()[nextPiece][orient]; c++) {
				top[slot + c] = height + State.getpTop()[nextPiece][orient][c];
			}

			// check for full rows - starting at the top
			for (int r = height + State.getpHeight()[nextPiece][orient] - 1; r >= height; r--) {
				// check all columns in the row
				boolean full = true;
				for (int c = 0; c < State.COLS; c++) {
					if (field[r][c] == 0) {
						full = false;
						break;
					}
				}
				// if the row was full - remove it and slide above stuff down
				if (full)
					rowsCleared++;

			}
			return true;
		}

		// Return the Field array
		public int[][] getField() {
			return field;
		}

		// Return the Top array
		public int[] getTop() {
			return top;
		}

		// Return the number of cleared row (to use after makeMove !)
		public int getRowsCleared() {
			return rowsCleared;
		}

		public int getNextPiece() {
			return nextPiece;
		}

		// For simulating multi-ply, inner call (use getnNextState)
		private StateWrapper(StateWrapper state, int piece) {
			field = Arrays.stream(state.getField()).map(int[]::clone).toArray(int[][]::new);
			top = Arrays.copyOf(state.getTop(), State.COLS);
			// Remove previous full line
			// check for full rows - starting at the top
			rowsCleared = state.rowsCleared;
			nextPiece = piece;
		}

		// Create another wrapper to simulate the next ply
		public StateWrapper generateNextState(int piece) {
			return new StateWrapper(this, piece);
		}

		// return the set of legal moves for the next piece
		public int[][] legalMoves() {
			return State.legalMoves[nextPiece];
		}
	}

	public static class Feature {
		private final StateWrapper current;
		private final int move[];
		public static final int COLS = 10;
		public static final int ROWS = 21;

		public Feature(StateWrapper wrap, int[] move) {
			this.current = wrap;
			this.move = move;
		}

		// returns aggregate height of the grid
		public int aggregateHeight() {
			int aggregateHeight = 0;
			int[] top = current.getTop();
			for (int i = 0; i < top.length; i++)
				aggregateHeight += top[i];
			return aggregateHeight;
		}

		// returns number of holes (unfilled places) beneath top of grid
		public int numberHoles() {
			int holesCounter = 0;
			int[][] field = current.getField();
			int[] top = current.getTop();
			for (int i = 0; i < top.length; i++) {
				for (int j = 0; j < top[i]; j++) {
					if (field[j][i] == 0)
						holesCounter++;
				}
			}
			return holesCounter;
		}

		public double landingHeight() {
			int[][] pHeight = State.getpHeight();
			int[] top = current.getTop();
			double landingHeight = top[move[State.SLOT]]
					+ ((double) pHeight[current.getNextPiece()][move[State.ORIENT]] - 1) / 2.0;
			return landingHeight;
		}

		public int totalRowTransitions() {
			int row_transitions = 0;
			int[][] field = current.getField();
			int last_cell = 1;
			for (int i = 0; i < ROWS; i++) {
				for (int j = 0; j < COLS; j++) {
					int current_cell = field[i][j];
					if (current_cell != 0)
						current_cell = 1;
					if (current_cell != last_cell)
						row_transitions++;
					last_cell = current_cell;
				}
				last_cell = 1;
			}
			return row_transitions;
		}

		public int totalWells() {
			int wells = 0;
			int[][] field = current.getField();
			// Wells in inner columns
			for (int i = 1; i < COLS - 1; i++) {
				for (int j = ROWS - 1; j >= 0; j--) {
					if ((field[j][i] == 0) && (field[j][i - 1] != 0) && (field[j][i + 1] != 0)) {
						wells++;
						for (int k = j - 1; k >= 0; k--)
							if (field[k][i] == 0)
								wells++;
							else
								break;
					}
					if (field[j][i] != 0)
						break;
				}
			}

			// Wells in left-most column
			for (int j = ROWS - 1; j >= 0; j--) {
				if ((field[j][0] == 0) && (field[j][1] != 0)) {
					wells++;
					for (int k = j - 1; k >= 0; k--)
						if (field[k][0] == 0)
							wells++;
						else
							break;
				}
				if (field[j][0] != 0)
					break;
			}

			// Wells in right-most column
			for (int j = ROWS - 1; j >= 0; j--) {
				if ((field[j][COLS - 1] == 0) && (field[j][COLS - 2] != 0)) {
					wells++;
					for (int k = j - 1; k >= 0; k--)
						if (field[k][COLS - 1] == 0)
							wells++;
						else
							break;
				}
				if (field[j][COLS - 1] != 0)
					break;
			}
			return wells;

		}

		public int totalColumnTransitions() {
			int col_transitions = 0;
			int[][] field = current.getField();
			int last_cell = 1;
			for (int i = 0; i < COLS; i++) {
				for (int j = 0; j < ROWS; j++) {
					int current_cell = field[j][i];
					if (current_cell != 0)
						current_cell = 1;
					if (current_cell != last_cell)
						col_transitions++;
					last_cell = current_cell;
				}
				last_cell = 1;
			}
			return col_transitions;
		}

		// returns number of rows cleared so far
		public int completeLines() {
			return current.getRowsCleared();
		}

		// returns bumpiness of grid aka sum of absolute difference between
		// adjacent
		// column heights
		public int bumpiness() {
			int bumpiness = 0;
			int[] top = current.getTop();
			for (int i = 0; i < top.length - 1; i++) {
				bumpiness += Math.abs(top[i + 1] - top[i]);
			}
			return bumpiness;
		}

		// returns column with max height in current state
		public int maxHeight() {
			int max_col = 0, max_height = 0;
			int[] top = current.getTop();
			for (int i = 0; i < top.length; i++) {
				if (max_height < top[i]) {
					max_height = top[i];
					max_col = i;
				}
			}
			return max_col;
		}

		// returns column with min height in current state
		public int minHeight() {
			int min_col = 0, min_height = State.ROWS;
			int[] top = current.getTop();
			for (int i = 0; i < top.length; i++) {
				if (min_height > top[i]) {
					min_height = top[i];
					min_col = i;
				}
			}
			return min_col;
		}

	}

	/**
	 * Default constructor, construct the Player Skeleton with the default
	 * weights to play 1 round
	 */
	public PlayerSkeleton() {
		weightVector = WEIGHTS;
	}

	/**
	 * Select the best possible move based on linear combination heuristic for a
	 * given state and set of legal moves
	 * 
	 * @param s
	 *            The current state
	 * @param legalMoves
	 *            The set of legal moves in the current state
	 * @return The index of the best move to do based on the heuristic
	 */
	public int pickMove(State s, int[][] legalMoves) {
		int bestIndex = 0;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < legalMoves.length; ++i) {
			StateWrapper wrap = new StateWrapper(s);
			boolean validMove = wrap.makeMove(i);
			if (validMove) {
				Feature feature = new Feature(wrap, legalMoves[i]);
				double landingHeight = feature.landingHeight();
				int completed = feature.completeLines();
				int row_transitions = feature.totalRowTransitions();
				int col_transitions = feature.totalColumnTransitions();
				int holes = feature.numberHoles();
				int well_sums = feature.totalWells();

				double score = weightVector[0] * landingHeight + weightVector[1] * completed
						+ weightVector[2] * row_transitions + weightVector[3] * col_transitions
						+ weightVector[4] * holes + weightVector[5] * well_sums;
				if (score > bestScore) {
					bestIndex = i;
					bestScore = score;
				}
			}
		}
		return bestIndex;
	}

	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while (!s.hasLost()) {
			s.makeMove(p.pickMove(s, s.legalMoves()));
			s.draw();
			s.drawNext(0, 0);
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("You have completed " + s.getRowsCleared() + " rows.");
	}

}


/**
 * TRAINING CODE 
 */

/*
 
// Very similar to Player Skeleton, simply use parallelism if it needs to play many games (that are usually LONG) 
public class PlayerSkeletonTrain {
	
	private static final double WEIGHTS[] = {-2.76744786096722, 7.109539166236581, -2.2476784673295653, -7.486601242281183, -10.0, -2.76442687047878};

	private final double weightVector[];
	private final int nbGames;
	private final int move_cutoff_num;
	private final int numberOfProcessors;
	private final int gamePerCore;
	private final int gameForLastCore;
	private volatile double score;

	public PlayerSkeletonTrain() {
		this(WEIGHTS, 1, Integer.MAX_VALUE);
	}

	public PlayerSkeletonTrain(double[] weights, int nbGames,int move_cutoff_num) {
		weightVector = weights;
		this.nbGames = nbGames;
		this.move_cutoff_num = move_cutoff_num;
		numberOfProcessors = Runtime.getRuntime().availableProcessors();
		// System.out.println("nb Processor " + numberOfProcessors);
		gamePerCore = nbGames / numberOfProcessors;
		gameForLastCore = nbGames - (numberOfProcessors - 1) * gamePerCore;
	}
	public void debug(int landingHeight,int completed,int row_transitions,int col_transitions,int holes,int well_sums) {
		System.out.print("Landing Height: "+landingHeight+"   ");
		System.out.print("Completed: "+completed+"   ");
		System.out.print("Row Trans: "+row_transitions+"   ");
		System.out.print("Col Trans: "+col_transitions+"   ");
		System.out.print("Holes: "+holes+"   ");
		System.out.print("Wells: "+well_sums+"   ");
		System.out.println();

	}
	
	public int pickMove(State s, int[][] legalMoves) {
		int bestIndex = 0;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < legalMoves.length; ++i) {
			StateWrapper wrap = new StateWrapper(s);
			boolean validMove = wrap.makeMove(i);
			if (validMove) {
				Feature feature = new Feature(wrap,legalMoves[i]);
				int aggregateHeight = feature.aggregateHeight();
				double landingHeight = feature.landingHeight();
				int completed = feature.completeLines();
				int row_transitions = feature.totalRowTransitions();
				int col_transitions = feature.totalColumnTransitions();
				int holes = feature.numberHoles();
				int bumpiness = feature.bumpiness();
				int well_sums = feature.totalWells();
				
				double score = weightVector[0] * landingHeight + weightVector[1] * completed
						+ weightVector[2] * row_transitions + weightVector[3] * col_transitions + weightVector[4]*holes + weightVector[5]*well_sums;
						//+ weightVector[6] * aggregateHeight + weightVector[7] * bumpiness;
				if (score > bestScore) {
					bestIndex = i;
					bestScore = score;
				}
			}
		}
		return bestIndex;
	}

	public double play() {
		//double score = playHelper(nbGames);
		if (nbGames <= numberOfProcessors) {
			score = playHelper(nbGames);
		} else {
			List<Thread> threads = new ArrayList<>(numberOfProcessors);
			for (int i = 0; i < numberOfProcessors; ++i) {
				Thread t = i == numberOfProcessors - 1 ? new Thread(() -> {
					long local = playHelper(gameForLastCore);
					score += local;
				}) : new Thread(() -> {
					long local = playHelper(gamePerCore);
					score += local;
				});
				t.start();
				threads.add(t);
			}
			for (int i = 0; i < numberOfProcessors; ++i) {
				try {
					threads.get(i).join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		// System.out.println(score);
		return score / nbGames;
	}

	private long playHelper(int nbGamesToPlay) {
		long sum = 0;
		for (int i = 0; i < nbGamesToPlay; ++i) {
			State s = new State();
			int moves = 0;
			while (!s.hasLost() && (moves < move_cutoff_num)) {
				s.makeMove(pickMove(s, s.legalMoves()));
				moves++;
			}
			//if (!s.hasLost()) System.out.println("cutoff " + Arrays.toString(weightVector));
			//System.out.println("You have completed " + s.getRowsCleared() +" rows.");
			//System.out.println("Moves made: "+s.getTurnNumber());
			sum += s.getRowsCleared();
		}
		return sum;
	}
}


// We decided to use Particle Swarm optimization as the final solution to train the AI.
// We also use the open source library [JSwarm PSO](http://jswarm-pso.sourceforge.net/)
// in order to simplify the task
import net.sourceforge.jswarm_pso.*;

public class PSO {
	public static final int NB_FEATURES = 6;//8;
	public static final int GAMES = 16;
	public static final int MOVE_CUTOFF_NUM = 10_000_000;//Integer.MAX_VALUE;
	public static final int NB_PARTICULES = 25;
	public static final int NB_EVOLUTION = 20;
	private final Swarm swarm;

	public static class TetrisFitnessFunction extends FitnessFunction {

		@Override
		public double evaluate(double[] weights) {
			PlayerSkeletonTrain player = new PlayerSkeletonTrain(weights, GAMES, MOVE_CUTOFF_NUM);
			return player.play();
		}

	}

	public static class TetrisParticle extends Particle {
		public TetrisParticle() {
			super(NB_FEATURES);
		}
	}

	public PSO() {
		swarm = new Swarm(NB_PARTICULES, new TetrisParticle(), new TetrisFitnessFunction());
		//double max[] = {-2, 9, -1, -7, -9.5, -1.5};
		double max[] = {0, 10, 0, 0 , 0, 0};
		//double min[] = {-3, 6.5, -3, -8.5, -10, -3};
		double min[] = {-10, 5, -10, -10, -10, -10};
		swarm.setMaxPosition(max);
		swarm.setMinPosition(min);
		swarm.setMaxMinVelocity(0.5);
	}

	private void evolve(int i) {
		for (int j = 0; j < i; ++j) {
			System.out.println("Iteration " + (j + 1));
			swarm.evolve();
			Particle particles[] = swarm.getParticles();
			String str = "";
			for (int k = 0; k < particles.length; k++) {
				str += k + ";" + particles[k].getFitness() + ";" + Arrays.toString(particles[k].getPosition()) + "\n";
			}
			System.out.println(str);
		}
		System.out.println(swarm.toStringStats());
	}

	public static void main(String[] args) {
		PSO trainer = new PSO();
		trainer.evolve(NB_EVOLUTION);
	}
}

*/
