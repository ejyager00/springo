package com.springo.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
* GoGame is a class containing all the logic needed to play a game of Go. 
* 
* @author Eric Yager
*/
public class GoGame {

    private int size; // the side length of the board
    private short[][] board; // state of the board (0 = empty, 1 = black, -1 = white)
    private ArrayList<short[][]> history; // history of the board state
    private short turn; // whose turn it is currently (1 = black, -1 = white)
    private int consecutivePasses; // tracks whether both players have passed consecutively
    private int[] piecesCaptured; // the number of pieces each player has captured (0 index black, 1 index white)
    private double komi; // the number of points white gets for the disadvantage of playing second
    private boolean gameOver; // indicates whether the game is over or not

    // CONSTRUCTORS

    /**
     * Zero argument constructor, in which board size defaults to 19 by 19, 
     * and komi is 6.5 points.
     */
    public GoGame() {
        this.size = 19; // default board size is 19
        this.board = new short[size][size];
        this.history = new ArrayList<>();
        this.turn = 1;
        this.consecutivePasses = 0;
        this.piecesCaptured = new int[2];
        this.komi = 6.5; // defualt komi is 6.5
        this.history.add(board); //initialize history
    }

    /**
     * Two argument constructor setting board size and komi.
     * 
     * @param size the number of spaces on each side of the board.
     * @param komi the number of points added to white's score.
     */
    public GoGame(int size, double komi) {
        this.size = size;
        this.board = new short[size][size];
        this.history = new ArrayList<>();
        this.turn = 1;
        this.consecutivePasses = 0;
        this.piecesCaptured = new int[2];
        this.komi = komi;
        this.history.add(board); //initialize history
    }

    /**
     * The copy constructor, which copies all instance variables.
     * 
     * @param other the game to copy.
     */
    public GoGame(GoGame other) {
        this.size = other.getSize();
        this.turn = other.getTurn();
        this.consecutivePasses = other.getConsecutivePasses();
        this.komi = other.getKomi();
        this.board = GoGame.copyBoard(other.getBoard());
        int[] pc = {other.getPiecesCaptured()[0], other.getPiecesCaptured()[1]};
        this.piecesCaptured = pc;
        this.history = new ArrayList<>();
        for (short[][] b : other.getHistory()) {
            this.history.add(GoGame.copyBoard(b));
        }
    }

    // PUBLIC GETTERS

    /**
     * Get the side length (in spaces) of the board.
     * 
     * @return the board size.
     */
    public int getSize() {
        return size;
    }

    /**
     * Get the board as a 2d array. 
     * {@code 1} is a black piece, {@code -1} is a white piece, and {@code 0} 
     * is an empty space. Returns a copy.
     * 
     * @return a copy of the game board.
     */
    public short[][] getBoard() {
        return this.copyBoard();
    }

    /**
     * Get the history of the board as a List of boards. 
     * Creates a deep copy so that the history cannot be modified.
     * 
     * @return a copy of the game history.
     */
    public List<short[][]> getHistory() {
        ArrayList<short[][]> history = new ArrayList<>();
        for (short[][] b : this.history) {
            history.add(GoGame.copyBoard(b));
        }
        return history;
    }

    /**
     * Get the player whose turn it is. 
     * {@code 1} is black's turn, {@code -1} is white's turn.
     * 
     * @return the player whose turn it is.
     */
    public short getTurn() {
        return turn;
    }

    /**
     * Get the number of consecutive turns on which players have passed. 
     * A value of 2 indicates that white passed, and then black passed 
     * immediately, or vice versa.
     * 
     * @return the number of consecutive turns on which a player passed.
     */
    public int getConsecutivePasses() {
        return consecutivePasses;
    }

    /**
     * Get the number of pieces each player has captured. 
     * The result will always be an array of length 2. The {@code 0} index is 
     * the quantity of pieces captured by black, and the 1 index is the 
     * quantity of pieces captured by white.
     * 
     * @return length-2 array of captured piece quantities.
     */
    public int[] getPiecesCaptured() {
        return Arrays.copyOf(piecesCaptured, 2);
    }

    /**
     * Get the komi value, the number of additional points for white at the end 
     * of the game. 
     * This makes up for the disadvantage of going second.
     * 
     * @return komi value.
     */
    public double getKomi() {
        return komi;
    }

    /**
     * A boolean indicating whether or not this game as ended. 
     * True if the game has ended, false otherwise.
     * 
     * @return boolean for if the game is over.
     */
    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * Get the winner of the game. 
     * Returns zero if nobody has won; otherwise, {@code 1} is black, and 
     * {@code -1} is white.
     * 
     * @return the winner of the game.
     */
    public short getWinner() {
        if (gameOver) { // if the game is over, calculate the winner from the scores
            double[] scores = getScores();
            return (short) ((scores[0] > scores[1]) ? 1 : -1);
        }
        return 0; // if the game is not over, return 0, as there is no winner yet
    }

    /** 
     * Get the current scores of both players, black and white. 
     * The return value will always be a length two array. The {@code 0} index 
     * will be black's current score, and the {@code 1} index will be white's.
     * 
     * @return length-2 array of the players' scores.
     */
    public double[] getScores() {
        // initialize array for scores
        double[] scores = new double[2];
        // add the komi points for white
        scores[1] += komi;
        // add the pieces each player has captured
        scores[0] += (double) piecesCaptured[0];
        scores[1] += (double) piecesCaptured[1];
        // copy the board and fill in any surrounded territory
        short[][] boardCopy = fillSurroundedTerritory();
        // loop over the board and give players points for all occupied spaces
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; i++) {
                if (boardCopy[i][j]==1)
                    scores[0]++;
                else if (boardCopy[i][j]==-1)
                    scores[1]++;
            }
        }
        return scores;
    }

    @Override
    public String toString() {
        String boardString = "";
        for (short[] row : board) {
            for (short val : row) {
                if (val == 1) // black space
                    boardString += "\u25CF";
                else if (val == -1) // white space
                    boardString += "\u25CB";
                else // empty space
                    boardString += "+";
            }
            boardString += "\n";
        }
        // add whose turn it is
        return boardString + ((turn == 1) ? "\u25CF" : "\u25CB") + " to move.\n";
    }

    /**
     * A static method for converting a game to a {@code String} for display in 
     * ASCII text.
     * Simply calls the {@code toString()} method of the game that is passed.
     * 
     * @param game the game to represent as a string.
     * @return a string representation of the game.
     */
    public static String toString(GoGame game) {
        return game.toString();
    }

    /**
     * A public method for copying boards. 
     * Creates a deep copy of a 2d game board.
     * 
     * @param board the board to copy.
     * @return a copy of the supplied board.
     */
    public static short[][] copyBoard(short[][] board) {
        short[][] boardCopy = new short[board[0].length][board.length];
        for (int i = 0; i < board.length; i++) {
            // copy each row to the new array
            boardCopy[i] = Arrays.copyOf(board[i], board[i].length);
        }
        return boardCopy;
    }

    // FUNCTIONS FOR PLAYER ACTIONS

    /**
     * Indicates that the player whose turn it is currently would like to pass. 
     * This changes the turn to the next player without making a move. If both 
     * players pass, one after the other, the game will end and the score will 
     * be tallied. This will only impact the game if the the game is not over.
     * Returns the winner if the game is over, or {@code 0} otherwise.
     * 
     * @return the winner of the game.
     */
    public short pass() {
        if (gameOver) // passing is only allowed if the game is not over
            return getWinner();
        consecutivePasses++; // icrement the number of consecutive passes
        if (consecutivePasses > 1) { // if both players pass, the game ends
            gameOver = true;
            // determine winner
            double[] scores = getScores();
            if (scores[0] > scores[1])
                return 1; // black wins
            return -1; // white wins
        }
        turn *= -1; // change turn 
        return 0; // no one has won yet
    }

    /**
     * Indicates that the player whose turn it is currently would like to 
     * resign. 
     * This immediately ends the game, and the other player wins. This will 
     * only impact the game if the the game is not over. Returns the winner if 
     * the game is over, or {@code 0} otherwise.
     * 
     * @return the winner of the game.
     */
    public short resign() {
        if (gameOver) // resignation is only allowed if the game is not over
            return getWinner();
        gameOver = true;
        return turn *= -1; // other player wins
    }

    /** 
     * Place a piece at the coordinates {@code x}, {@code y} on the game board 
     * for the player whose turn it is.
     * {@code x} and {@code y} are both zero indexed from the top left corner 
     * of the board. If the attempted move is illegal, {@code makeMove} will 
     * throw a {@code GoGameException}. Although a move can never end the game, 
     * for consistency with {@code pass} and {@code resign}, a zero is always
     * returned to indicate that there is no winner, unless the game is over,
     * in which case the winner is returned.
     * 
     * @param x the x coordinate of the move.
     * @param y the y coordinate of the move.
     * @return the winner of the game.
     * @throws GoGameException the attempted move is illegal.
     */
    public short makeMove(int x, int y) throws GoGameException {
        if (gameOver) // moving is only allowed if the game is not over
            return getWinner();
        if (board[y][x] != 0) { // pieces can only be placed in empty spaces
            throw new GoGameException(String.format("There is already a piece at %d, %d.", x, y));
        }
        // make a copy of the board in case the ko rule is violated
        short[][] boardCopy = copyBoard();
        boardCopy[y][x] = turn;
        // check each adjacent piece to see if it is surrounded
        if (y > 0 && boardCopy[y - 1][x] == -1 * turn)
            boardCopy = checkPieceForCapture(boardCopy, x, y-1);
        if (y + 1 < size && boardCopy[y + 1][x] == -1 * turn) 
            boardCopy = checkPieceForCapture(boardCopy, x, y+1);
        if (x > 0 && boardCopy[y][x - 1] == -1 * turn)
            boardCopy = checkPieceForCapture(boardCopy, x-1, y);
        if (x + 1 < size && boardCopy[y][x + 1] == -1 * turn) 
            boardCopy = checkPieceForCapture(boardCopy, x+1, y);
        // check this piece to see if it is surrounded
        boardCopy = checkPieceForCapture(boardCopy, x, y);
        // if this exact board has appeared previously, the move is illegal
        if (inHistory(boardCopy)) {
            throw new GoGameException(
                    "This move violates the ko rule; the resulting game state has occurred previously.");
        }
        // update the board, the turn, the history, and the consecutive passes
        board = boardCopy;
        turn *= -1;
        history.add(boardCopy);
        consecutivePasses = 0;
        return 0;
    }

    // PRIVATE HELPER FUNCTIONS

    /**
     * Deep copies the current board of this game.
     * 
     * @return a copy of the board
     */
    private short[][] copyBoard() {
        short[][] boardCopy = new short[size][size];
        for (int i = 0; i < size; i++) {
            // copy each row to the new array
            boardCopy[i] = Arrays.copyOf(board[i], size);
        }
        return boardCopy;
    }

    /**
     * Given a piece at {@code board} location {@code x}, {@code y}, returns an 
     * {@code ArrayList} of pieces of the same color that form a chain.
     * The list returned will include all pieces in {@code friends}.
     * 
     * @param board the board on wich to check for chains.
     * @param x the starting x coordinate.
     * @param y the starting y coordinate.
     * @param friends already known members of the chain.
     * @return members of a chain of same-colored pieces.
     */
    private ArrayList<int[]> getConnectedFriends(short[][] board, int x, int y, ArrayList<int[]> friends) {
        short piece = board[y][x];
        if (y > 0 && board[y - 1][x] == piece && !isFriendInSet(x, y - 1, friends)) {
            int[] friend = { x, y - 1 };
            friends.add(friend);
            friends = getConnectedFriends(board, x, y - 1, friends);
        }
        if (y + 1 < size && board[y + 1][x] == piece && !isFriendInSet(x, y + 1, friends)) {
            int[] friend = { x, y + 1 };
            friends.add(friend);
            friends = getConnectedFriends(board, x, y + 1, friends);
        }
        if (x > 0 && board[y][x - 1] == piece && !isFriendInSet(x - 1, y, friends)) {
            int[] friend = { x - 1, y };
            friends.add(friend);
            friends = getConnectedFriends(board, x, y + 1, friends);
        }
        if (x + 1 < size && board[y][x + 1] == piece && !isFriendInSet(x + 1, y, friends)) {
            int[] friend = { x + 1, y };
            friends.add(friend);
            friends = getConnectedFriends(board, x, y + 1, friends);
        }
        return friends;
    }

    /**
     * Checks if the given {@code x}, {@code y} coordinate is already in 
     * {@code friends}.
     * 
     * @param x
     * @param y
     * @param friends
     * @return 
     */
    private static boolean isFriendInSet(int x, int y, ArrayList<int[]> friends) {
        for (int[] f : friends) {
            if (f[0] == x && f[1] == y) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the chain given in {@code friends} has no liberties.
     * 
     * @param board the board where the chain exists.
     * @param friends the chain of same-colored pieces.
     * @return {@code true} if there are no liberties, otherwise {@code false}
     */
    private boolean areFriendsSurrounded(short[][] board, ArrayList<int[]> friends) {
        // When looping over all pieces, if any adjacent empty spaces are found, that is a liberty
        for (int[] f : friends) {
            if (f[0] > 0 && board[f[1]][f[0] - 1] == 0)
                return false;
            if (f[0] + 1 < size && board[f[1]][f[0] + 1] == 0)
                return false;
            if (f[1] > 0 && board[f[1] - 1][f[0]] == 0)
                return false;
            if (f[1] + 1 < size && board[f[1] + 1][f[0]] == 0)
                return false;
        }
        return true;
    }

    /**
     * Checks if the group of adjacent empty spaces given in {@code block} is 
     * completely surrounded by pieces belonging to {@code player}.
     * 
     * @param board the board where the spaces exist.
     * @param block the group of connected empty spaces.
     * @param player the player potentially controlling the territory.
     * @return boolean indicating whether the player surrounds the territory.
     */
    private boolean isSpaceEnclosed(short[][] board, ArrayList<int[]> block, short player) {
        // When looping over all pieces, if any adjacent spaces are occupied by the opponent, the territory is not controlled
        for (int[] f : block) {
            if (f[0] > 0 && board[f[1]][f[0] - 1] == -1*player)
                return false;
            if (f[0] + 1 < size && board[f[1]][f[0] + 1] == -1*player)
                return false;
            if (f[1] > 0 && board[f[1] - 1][f[0]] == -1*player)
                return false;
            if (f[1] + 1 < size && board[f[1] + 1][f[0]] == -1*player)
                return false;
        }
        return true;
    }

    /**
     * Check if a board state has occurred previously in the game.
     * 
     * @param board the board state to search for in the history.
     * @return boolean indicating if {@code board} is in the game history.
     */
    private boolean inHistory(short[][] board) {
        for (short[][] b : history) {
            if (Arrays.deepEquals(b, board)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create an {@code ArrayList} of {@code int} arrays with one member 
     * containing {@code x} and {@code y}.
     * 
     * @param x the x value of the initial coordinate.
     * @param y the y value of the initial coordinate.
     * @return the ArrayList containing the given coordinate.
     */
    private ArrayList<int[]> initializeFriendList(int x, int y) {
        ArrayList<int[]> friends = new ArrayList<>();
        int[] thisFriend = {x, y};
        friends.add(thisFriend);
        return friends;
    }

    /**
     * Check if the piece in the coordinate at {@code x}, {@code y} is part of 
     * a group with no liberties.
     * 
     * @param boardCopy the board on which to check.
     * @param x the x value of the piece.
     * @param y the y value of the piece.
     * @return the new board after removing any groups with no liberties.
     */
    private short[][] checkPieceForCapture(short[][] boardCopy, int x, int y) {
        ArrayList<int[]> friends = getConnectedFriends(boardCopy, x, y, initializeFriendList(x, y));
        if (areFriendsSurrounded(boardCopy, friends)) {
            short player = boardCopy[y][x];
            for (int[] f : friends) {
                boardCopy[f[1]][f[0]] = 0;
                if (player == 1)
                    piecesCaptured[0]++;
                else
                    piecesCaptured[1]++;
            }
        }
        return boardCopy;
    }

    /**
     * Create a copy of the board with all territory surrounded by a single 
     * player filled in with that players pieces. 
     * 
     * @return board with controlled territory filled.
     */
    private short[][] fillSurroundedTerritory() {
        short[][] boardCopy = copyBoard();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (boardCopy[i][j] == 0) { // if there is an empty space
                    // find all adjacent empty spaces
                    ArrayList<int[]> block = getConnectedFriends(boardCopy, j, i, initializeFriendList(j, i));
                    if (isSpaceEnclosed(boardCopy, block, (short) 1)) {
                        // if black encloses the space, give the points to black
                        for (int[] s : block) {
                            boardCopy[s[1]][s[0]] = 1;
                        }
                    } else if (isSpaceEnclosed(boardCopy, block, (short) -1)) {
                        // if white encloses the space, give the points to white
                        for (int[] s : block) {
                            boardCopy[s[1]][s[0]] = -1;
                        }
                    }
                }
            }
        }
        return boardCopy;
    }

}
