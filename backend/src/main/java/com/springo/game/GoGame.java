package com.springo.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
* GoGame
* 
* 
* @author Eric Yager
* 
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

    public GoGame() {
        this.size = 19; // default board size is 19
        this.board = new short[size][size];
        this.history = new ArrayList<>();
        this.turn = 1;
        this.consecutivePasses = 0;
        this.piecesCaptured = new int[2];
        this.komi = 6.5; // defualt komi is 6.5
        history.add(board); //initialize history
    }

    public GoGame(int size, double komi) {
        this.size = size;
        this.board = new short[size][size];
        this.history = new ArrayList<>();
        this.turn = 1;
        this.consecutivePasses = 0;
        this.piecesCaptured = new int[2];
        this.komi = komi;
        history.add(board); //initialize history
    }

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

    public int getSize() {
        return this.size;
    }

    public short[][] getBoard() {
        return this.board;
    }

    public List<short[][]> getHistory() {
        return this.history;
    }

    public short getTurn() {
        return this.turn;
    }

    public int getConsecutivePasses() {
        return this.consecutivePasses;
    }

    public int[] getPiecesCaptured() {
        return this.piecesCaptured;
    }

    public double getKomi() {
        return this.komi;
    }

    public boolean isGameOver() {
        return this.gameOver;
    }

    public short getWinner() {
        if (this.gameOver) { // if the game is over, calculate the winner from the scores
            double[] scores = this.getScores();
            return (short) ((scores[0] > scores[1]) ? 1 : -1);
        }
        return 0; // if the game is not over, return 0, as there is no winner yet
    }

    public double[] getScores() {
        // initialize array for scores
        double[] scores = new double[2];
        // add the komi points for white
        scores[1] += this.komi;
        // add the pieces each player has captured
        scores[0] += (double) this.piecesCaptured[0];
        scores[1] += (double) this.piecesCaptured[1];
        // in the code block below, we copy the board and fill in any surrounded territory
        short[][] boardCopy = copyBoard();
        for (int i = 0; i < this.size; i++) {
            for (int j = 0; j < this.size; j++) {
                if (boardCopy[i][j] == 0) { // if there is an empty space
                    // find all adjacent empty spaces
                    ArrayList<int[]> block = this.getConnectedFriends(boardCopy, j, i, initializeFriendList(j, i));
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
        // loop over the board and give players points for all occupied spaces
        for (int i = 0; i < this.size; i++) {
            for (int j = 0; j < this.size; i++) {
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
        for (short[] row : this.board) {
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
        return boardString + ((this.turn == 1) ? "\u25CF" : "\u25CB") + " to move.\n";
    }

    public static String toString(GoGame game) {
        return game.toString();
    }

    public static short[][] copyBoard(short[][] board) {
        short[][] boardCopy = new short[board[0].length][board.length];
        for (int i = 0; i < board.length; i++) {
            // copy each row to the new array
            boardCopy[i] = Arrays.copyOf(board[i], board[i].length);
        }
        return boardCopy;
    }

    // FUNCTIONS FOR PLAYER ACTIONS

    public short pass() {
        if (this.gameOver) // passing is only allowed if the game is not over
            return this.getWinner();
        this.consecutivePasses++; // icrement the number of consecutive passes
        if (this.consecutivePasses > 1) { // if both players pass, the game ends
            this.gameOver = true;
            // determine winner
            double[] scores = this.getScores();
            if (scores[0] > scores[1])
                return 1; // black wins
            return -1; // white wins
        }
        this.turn *= -1; // change turn 
        return 0; // no one has won yet
    }

    public short resign() {
        if (this.gameOver) // resignation is only allowed if the game is not over
            return this.getWinner();
        this.gameOver = true;
        return this.turn *= -1; // other player wins
    }

    public short makeMove(int x, int y) throws GoGameException {
        if (this.gameOver)
            return this.getWinner();
        if (this.board[y][x] != 0) {
            throw new GoGameException(String.format("There is already a piece at %d, %d.", x, y));
        }
        short[][] boardCopy = this.copyBoard();
        boardCopy[y][x] = this.turn;
        if (y > 0 && boardCopy[y - 1][x] == -1 * this.turn) {
            ArrayList<int[]> friends = this.getConnectedFriends(boardCopy, x, y - 1, initializeFriendList(x, y-1));
            if (this.areFriendsSurrounded(boardCopy, friends)) {
                for (int[] f : friends) {
                    boardCopy[f[1]][f[0]] = 0;
                    if (this.turn == 1)
                        this.piecesCaptured[0]++;
                    else
                        this.piecesCaptured[1]++;
                }
            }
        }
        if (y + 1 < size && boardCopy[y + 1][x] == -1 * this.turn) {
            ArrayList<int[]> friends = this.getConnectedFriends(boardCopy, x, y + 1, initializeFriendList(x, y+1));
            if (this.areFriendsSurrounded(boardCopy, friends)) {
                for (int[] f : friends) {
                    boardCopy[f[1]][f[0]] = 0;
                    if (this.turn == 1)
                        this.piecesCaptured[0]++;
                    else
                        this.piecesCaptured[1]++;
                }
            }
        }
        if (x > 0 && boardCopy[y][x - 1] == -1 * this.turn) {
            ArrayList<int[]> friends = this.getConnectedFriends(boardCopy, x - 1, y, initializeFriendList(x-1, y));
            if (this.areFriendsSurrounded(boardCopy, friends)) {
                for (int[] f : friends) {
                    boardCopy[f[1]][f[0]] = 0;
                    if (this.turn == 1)
                        this.piecesCaptured[0]++;
                    else
                        this.piecesCaptured[1]++;
                }
            }
        }
        if (x + 1 < size && boardCopy[y][x + 1] == -1 * this.turn) {
            ArrayList<int[]> friends = this.getConnectedFriends(boardCopy, x + 1, y, initializeFriendList(x+1, y));
            if (this.areFriendsSurrounded(boardCopy, friends)) {
                for (int[] f : friends) {
                    boardCopy[f[1]][f[0]] = 0;
                    if (this.turn == 1)
                        this.piecesCaptured[0]++;
                    else
                        this.piecesCaptured[1]++;
                }
            }
        }
        ArrayList<int[]> friends = getConnectedFriends(boardCopy, x, y, initializeFriendList(x, y));
        if (areFriendsSurrounded(boardCopy, friends)) {
            for (int[] f : friends) {
                boardCopy[f[1]][f[0]] = 0;
                if (this.turn == 1)
                    this.piecesCaptured[1]++;
                else
                    this.piecesCaptured[0]++;
            }
        }
        if (inHistory(boardCopy)) {
            throw new GoGameException(
                    "This move violates the ko rule; the resulting game state has occurred previously.");
        }
        this.board = boardCopy;
        this.turn *= -1;
        this.history.add(boardCopy);
        this.consecutivePasses = 0;
        return 0;
    }

    // PRIVATE HELPER FUNCTIONS

    private short[][] copyBoard() {
        short[][] boardCopy = new short[this.size][this.size];
        for (int i = 0; i < this.size; i++) {
            // copy each row to the new array
            boardCopy[i] = Arrays.copyOf(this.board[i], size);
        }
        return boardCopy;
    }

    private ArrayList<int[]> getConnectedFriends(short[][] board, int x, int y, ArrayList<int[]> friends) {
        short piece = board[y][x];
        if (y > 0 && board[y - 1][x] == piece && !GoGame.isFriendInSet(x, y - 1, friends)) {
            int[] friend = { x, y - 1 };
            friends.add(friend);
            friends = getConnectedFriends(board, x, y - 1, friends);
        }
        if (y + 1 < size && board[y + 1][x] == piece && !GoGame.isFriendInSet(x, y + 1, friends)) {
            int[] friend = { x, y + 1 };
            friends.add(friend);
            friends = getConnectedFriends(board, x, y + 1, friends);
        }
        if (x > 0 && board[y][x - 1] == piece && !GoGame.isFriendInSet(x - 1, y, friends)) {
            int[] friend = { x - 1, y };
            friends.add(friend);
            friends = getConnectedFriends(board, x, y + 1, friends);
        }
        if (x + 1 < size && board[y][x + 1] == piece && !GoGame.isFriendInSet(x + 1, y, friends)) {
            int[] friend = { x + 1, y };
            friends.add(friend);
            friends = getConnectedFriends(board, x, y + 1, friends);
        }
        return friends;
    }

    private static boolean isFriendInSet(int x, int y, ArrayList<int[]> friends) {
        for (int[] f : friends) {
            if (f[0] == x && f[1] == y) {
                return true;
            }
        }
        return false;
    }

    private boolean areFriendsSurrounded(short[][] board, ArrayList<int[]> friends) {
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

    private boolean isSpaceEnclosed(short[][] board, ArrayList<int[]> block, short player) {
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

    private boolean inHistory(short[][] board) {
        for (short[][] b : this.history) {
            if (Arrays.deepEquals(b, board)) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<int[]> initializeFriendList(int x, int y) {
        ArrayList<int[]> friends = new ArrayList<>();
        int[] thisFriend = {x, y};
        friends.add(thisFriend);
        return friends;
    }

}
