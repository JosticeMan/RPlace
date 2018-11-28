package place.network;

import place.PlaceBoard;
import place.PlaceColor;
import place.PlaceTile;

import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * CSCI-242 AP COMPUTER SCIENCE X
 * Project 2: Place
 *
 * Package that enables easy access to package sending or receiving. Also contains constants for easy access by classes.
 *
 * @author Justin Yau
 */
public class PlaceExchange {

    /**
     * The colors with their appropriate hexdecimal equivalent as indices
     */
    public static final PlaceColor[] colors = {PlaceColor.BLACK, PlaceColor.GRAY, PlaceColor.SILVER, PlaceColor.WHITE,
            PlaceColor.MAROON, PlaceColor.RED, PlaceColor.OLIVE, PlaceColor.YELLOW,
            PlaceColor.GREEN, PlaceColor.LIME, PlaceColor.TEAL, PlaceColor.AQUA,
            PlaceColor.NAVY, PlaceColor.BLUE, PlaceColor.PURPLE, PlaceColor.FUCHSIA};

    /**
     * The time in-between each TILE CHANGE request per user
     */
    public static final int SLEEP_TIME = 0;

    /**
     * Whether or not to print debug messages
     */
    public static final boolean DEBUG = false;

    /***
     * Creates a login request and sends it to the inputted output stream
     * @param out - The output stream to send the request to
     * @param username - The username of who sent the request
     * @throws IOException
     */
    public static void createLoginRequest(ObjectOutputStream out, String username) throws IOException {
        PlaceRequest<String> req = new PlaceRequest<>(PlaceRequest.RequestType.LOGIN, username);
        out.writeUnshared(req);
        out.flush();
    }

    /***
     * Creates a request to change a tile and sends it to the inputted output stream (The server)
     * @param tile - The tile to be changed
     * @throws IOException
     */
    public static void createTileChangeRequest(ObjectOutputStream out, PlaceTile tile) throws IOException {
        PlaceRequest<PlaceTile> req = new PlaceRequest<>(PlaceRequest.RequestType.CHANGE_TILE, tile);
        out.writeUnshared(req);
        out.flush();
    }

    /***
     * Creates a request to let the user know they logged in successfully
     * @param out - The output stream to the user
     * @param arg - Preferably information regarding the user and the connection
     * @throws IOException
     */
    public static void createLoginSuccess(ObjectOutputStream out, String arg) throws IOException {
        PlaceRequest<String> req = new PlaceRequest<>(PlaceRequest.RequestType.LOGIN_SUCCESS, arg);
        out.writeUnshared(req);
        out.flush();
    }

    /***
     * Creates a request to let the user know the current state of the board
     * @param out - The output stream to the user
     * @param board - The current state of the board
     * @throws IOException
     */
    public static void createBoardRequest(ObjectOutputStream out, PlaceBoard board) throws IOException {
        PlaceRequest<PlaceBoard> req = new PlaceRequest<>(PlaceRequest.RequestType.BOARD, board);
        out.writeUnshared(req);
        out.flush();
    }

    /***
     * Creates a request to let users know that a tile has been updated on the server
     * @param out - The output stream to the user
     * @param tile - The tile that has been updated
     * @throws IOException
     */
    public static void createChangedTile(ObjectOutputStream out, PlaceTile tile) throws IOException {
        PlaceRequest<PlaceTile> req = new PlaceRequest<>(PlaceRequest.RequestType.TILE_CHANGED, tile);
        out.writeUnshared(req);
        out.flush();
    }

    /***
     * Creates a request to let the output stream know that an error has occurred
     * @param out - The output stream to send the request to
     * @param errMsg - A message specifying the error
     * @throws IOException
     */
    public static void createError(ObjectOutputStream out, String errMsg) throws IOException {
        PlaceRequest<String> req = new PlaceRequest<>(PlaceRequest.RequestType.ERROR, errMsg);
        out.writeUnshared(req);
        out.flush();
    }

}
