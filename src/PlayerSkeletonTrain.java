import java.util.ArrayList;
import java.util.List;

public class PlayerSkeletonTrain {
	
	private static final double WEIGHTS[] = {-2.76744786096722, 7.109539166236581, -2.2476784673295653, -7.486601242281183, -10.0, -2.76442687047878};

	private final double weightVector[];
	private final int nbGames;
	private final int move_cutoff_num;
	private final int numberOfProcessors;
	private final int gamePerCore;
	private final int gameForLastCore;
	private volatile double score;

	/**
	 * Default constructor, construct the Player Skeleton with the default
	 * weights to play 1 round
	 */
	public PlayerSkeletonTrain() {
		this(WEIGHTS, 1, Integer.MAX_VALUE);
	}

	/**
	 * Augmented constructor, takes the weights (size 4) and the number of game
	 * to play
	 * 
	 * @param weights
	 * @param nbGames
	 */
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

	/**
	 * Play the game for a known number of games, for the weights given during
	 * the construction
	 * 
	 * Note: It uses parrallelism if the number of game to play is bigger than
	 * MIN_NBGAMES
	 * 
	 * @return the average score over all the games
	 */
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

	/**
	 * Helper method that actually play a given number of games
	 * 
	 * @param nbGamesToPlay
	 *            the number of games to play
	 * @return the total score
	 */
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

	public static void main(String[] args) {
		System.out.println("iteration;score");
		double totalScore = 0;
		for(int i=0; i < 50; i++) {
			//State s = new State();
			// new TFrame(s);
			PlayerSkeletonTrain p = new PlayerSkeletonTrain();
			p.play();
			System.out.println(i + ";" + p.score);
			totalScore += p.score;
			/*while (!s.hasLost()) { 
				int move_no = p.pickMove(s,s.legalMoves());
				int[] move = s.legalMoves()[move_no];
				//System.out.println("Current piece(State): "+s.getNextPiece());
				StateWrapper wrap = new StateWrapper(s);
				s.makeMove(move);
				s.draw(); s.drawNext(0, 0);
			 	try { Thread.sleep(1); } 
			 	catch (InterruptedException e) {
			 		e.printStackTrace(); 
			 	}
			 	/*
			  	if(s.getTurnNumber() == 10) {
			  		wrap.makeMove(move_no);
			  		System.out.println("Current piece(StateWrapper): "+wrap.getNextPiece());
			  		System.out.println("Move: "+move[s.SLOT]+"  "+move[s.ORIENT]);
			  		
			  		Feature feature = new Feature(wrap,move);
			  		int landingHeight = feature.landingHeight();
					int completed = feature.completeLines();
					int row_transitions = feature.totalRowTransitions();
					int col_transitions = feature.totalColumnTransitions();
					int holes = feature.numberHoles();
					//int bumpiness = feature.bumpiness();
					int well_sums = feature.totalWells();
					p.debug(landingHeight,completed,row_transitions,col_transitions,holes,well_sums);
					int[][] field = s.getField();
					for(int i=s.ROWS - 1; i >= 0; i--) {
						for(int j=0; j < s.COLS; j++)
							System.out.print(field[i][j]+" ");
						System.out.println();
					}
					System.out.println();
					int[] top = s.getTop();
					for(int i=0; i < top.length;i++)
						System.out.print(top[i] + "  ");
			  		break;
			  	}
			  	
			}*/ 
			//System.out.println("You have completed " + s.getRowsCleared() +" rows.");
			//System.out.println("Moves made: "+s.getTurnNumber());
		}
		System.out.println("avg score;"+ totalScore / 50.);
		/*
		int nbGames = 1001;
		PlayerSkeleton p = new PlayerSkeleton(WEIGHTS, nbGames);
		System.out.println("Score avg " + p.play() + " rows in " + nbGames + " games");
		*/
	}

}
