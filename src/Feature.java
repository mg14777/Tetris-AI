public class Feature {
	private final StateWrapper current;
	private final int move[];
	public static final int COLS = 10;
	public static final int ROWS = 21;

	public Feature(StateWrapper wrap,int[] move) {
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
		double landingHeight = top[move[State.SLOT]] +  ((double) pHeight[current.getNextPiece()][move[State.ORIENT]] - 1)/2.0;
		return landingHeight;
	}
	public int totalRowTransitions() {
		int row_transitions = 0;
		int[][] field = current.getField();
		int last_cell = 1;
		for(int i=0; i < ROWS; i++) {
			for(int j=0; j < COLS; j++) {
				int current_cell = field[i][j];
				if(current_cell != 0)
					current_cell = 1;
				if(current_cell != last_cell)
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
		for(int i=1; i < COLS-1; i++) {
			for(int j=ROWS -1; j >= 0; j--) {
				if((field[j][i] == 0) && (field[j][i-1] != 0) && (field[j][i+1] != 0)) {
					wells++;
					for(int k=j-1; k >= 0; k--)
						if(field[k][i] == 0)
							wells++;
						else 
							break;
				}
				if(field[j][i] != 0)
					break;
			}
		}

		// Wells in left-most column
		for(int j=ROWS -1; j >= 0; j--) {
			if((field[j][0] == 0) && (field[j][1] != 0)) {
				wells++;
				for(int k=j-1; k >= 0; k--)
					if(field[k][0] == 0)
						wells++;
					else 
						break;
			}
			if(field[j][0] != 0)
					break;
		}

		// Wells in right-most column
		for(int j=ROWS -1; j >= 0; j--) {
			if((field[j][COLS-1] == 0) && (field[j][COLS-2] != 0)) {
				wells++;
				for(int k=j-1; k >= 0; k--)
					if(field[k][COLS-1] == 0)
						wells++;
					else 
						break;
			}
			if(field[j][COLS-1] != 0)
					break;
		}
		return wells;

	}

	public int totalColumnTransitions() {
		int col_transitions = 0;
		int[][] field = current.getField();
		int last_cell = 1;
		for(int i=0; i < COLS; i++) {
			for(int j=0; j < ROWS; j++) {
				int current_cell = field[j][i];
				if(current_cell != 0)
					current_cell = 1;
				if(current_cell != last_cell)
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

	// returns bumpiness of grid aka sum of absolute difference between adjacent
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
