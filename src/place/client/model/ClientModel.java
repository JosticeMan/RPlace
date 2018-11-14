package place.client.model;

import place.PlaceBoard;
import place.PlaceColor;
import place.PlaceTile;

import java.util.Observable;

public class ClientModel extends Observable {

    /** The square dimension of the board */
    private int dim;
    /** The grid of tiles */
    private PlaceTile[][] board;
    /** Whether or not the user can pixel a spot*/
    private boolean makeMove;
    /** Status of the board */
    private Status status;

    public enum Status {
        ACTIVE, CLOSED , ERROR;

        private String message = null;

        public void setMessage( String msg ) {
            this.message = msg;
        }

        @Override
        public String toString() {
            return super.toString() +
                    this.message == null ? "" : ( '(' + this.message + ')' );
        }
    }

    /**
     * Replaces the current board with the inputted board and sets lets observers know
     * @param board - The board to replace the current one with
     */
    public void initialize(PlaceBoard board) {
        this.board = board.getBoard();
        this.dim = this.board.length;
        this.status = Status.ACTIVE;
        this.makeMove = true;
    }

    public Status getStatus() {
        return status;
    }

    public boolean canMakeMove() {
        return makeMove;
    }

    public void setMove(boolean move) {
        makeMove = move;
        super.setChanged();
        super.notifyObservers();
    }

    public void updatePixel(PlaceTile tile) {
        setTile(tile);
        super.setChanged();
        super.notifyObservers(tile);
    }

    public void close() {
        this.status = Status.CLOSED;
        super.setChanged();
        super.notifyObservers();
    }

    /**
     * Get the entire board.
     *
     * @return the board
     */
    public PlaceTile[][] getBoard() {
        return this.board;
    }

    /**
     * Get a tile on the board
     *
     * @param row row
     * @param col column
     * @rit.pre row and column constitute a valid board coordinate
     * @return the tile
     */
    public PlaceTile getTile(int row, int col){
        return this.board[row][col];
    }

    /**
     * Change a tile in the board.
     *
     * @param tile the new tile
     * @rit.pre row and column constitute a valid board coordinate
     */
    public void setTile(PlaceTile tile) {

        this.board[tile.getRow()][tile.getCol()] = tile;
    }

    /**
     * Tells whether the coordinates of the tile are valid or not
     * @param tile the tile
     * @return are the coordinates within the dimensions of the board?
     */
    public boolean isValid(PlaceTile tile) {
        return tile.getRow() >=0 &&
                tile.getRow() < this.dim &&
                tile.getCol() >= 0 &&
                tile.getCol() < this.dim;
    }

    public void error( String arguments ) {
        this.status = Status.ERROR;
        this.status.setMessage( arguments );
        super.setChanged();
        super.notifyObservers();
    }

    /**
     * Return a string representation of the board.  It displays the tile color as
     * a single character hex value in the range 0-F.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("");
        for (int row=0; row<this.dim; ++row) {
            builder.append("\n");
            for (int col=0; col<this.dim; ++col) {
                builder.append(this.board[row][col].getColor());
            }
        }
        return builder.toString();
    }

}
