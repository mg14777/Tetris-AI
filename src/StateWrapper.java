import java.util.Arrays;

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
