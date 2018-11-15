package place.server;

import place.PlaceBoard;
import place.PlaceTile;
import place.network.PlaceExchange;
import place.network.PlaceRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * CSCI-242 AP COMPUTER SCIENCE X
 * Project 2: Place
 *
 * A thread created by the server to handle requests sent by the connected client associated with this thread
 *
 * @author Justin Yau
 */
public class PlaceClientThread extends Thread {

    private Socket socket = null;   // The Client Connection
    private PlaceServer server;     // The server this thread was created by
    private String username;        // The username of the client
    private ObjectOutputStream out; // The output stream of the socket
    private ObjectInputStream in;   // The input stream of the socket

    /***
     * Creates a new thread that will handle requests from the socket
     * @param socket - The socket to handle requests from
     * @param server - The server that created this thread
     */
    public PlaceClientThread(Socket socket, PlaceServer server) {
        super("PlaceClientThread");
        this.socket = socket;
        this.server = server;
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /***
     * Used for debugging. Will print messages if debug variable is enabled
     * @param logMsg - The message to print
     */
    private static void dPrint( Object logMsg ) {
        if ( PlaceExchange.DEBUG ) {
            System.out.println( logMsg );
        }
    }

    /***
     * Handles the logging in of the user and any request following that
     */
    public void run() {
        try {
            PlaceRequest<?> req = (PlaceRequest<?>) in.readUnshared();
            if (req.getType() == PlaceRequest.RequestType.LOGIN) {
                if( server.addClient(username = (String) req.getData(), this.socket, this)) {
                    PlaceExchange.createLoginSuccess(this.out, socket.toString());
                    PlaceExchange.createBoardRequest(this.out, server.getBoard());
                    go();
                } else {
                    PlaceExchange.createError(this.out, username + " already logged in! Try a different user!");
                }
            } else {
                PlaceExchange.createError(this.out, "Expected login request first!");
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        this.dPrint(username + " Thread ended!");
    }

    /***
     * Handles any request made after the login request is made and successfully authorized
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void go() throws IOException, ClassNotFoundException {
        boolean loggedIn = true;
        try {
            while(loggedIn) {
                PlaceRequest<?> req = (PlaceRequest<?>) in.readUnshared();
                if(req.getType() == PlaceRequest.RequestType.ERROR) {
                    loggedIn = false;
                } else {
                    processRequest(req);
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
        server.removeClient(username, this.socket);
    }

    /***
     * Processes the inputted request and replies appropriately
     * @param req - The request sent to the socket
     * @throws IOException
     * @throws InterruptedException
     */
    public void processRequest(PlaceRequest<?> req) throws IOException, InterruptedException {
        PlaceRequest.RequestType type = req.getType();
        if(type == PlaceRequest.RequestType.CHANGE_TILE) {
            if( server.changeTile((PlaceTile) req.getData()) ) {
                this.sleep(PlaceExchange.SLEEP_TIME);
            }
        } else {
            PlaceExchange.createError(this.out, "Expected change tile requests only!");
        }
    }

    /***
     * Used by the server when shutting down
     * @param msg - Message to relay to the clients about this event
     */
    public void createError(String msg) throws IOException {
        PlaceExchange.createError(this.out, msg);
    }

    /***
     * Creates a request to let users know that a tile has been updated on the server
     * @param tile - The tile to be updated
     * @throws IOException
     */
    public void createChangedTile(PlaceTile tile) throws IOException {
        PlaceExchange.createChangedTile(this.out, tile);
    }

}
