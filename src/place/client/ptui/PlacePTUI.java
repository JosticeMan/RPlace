package place.client.ptui;

import place.PlaceTile;
import place.client.NetworkClient;
import place.client.model.ClientModel;
import place.network.PlaceExchange;

import java.io.*;
import java.util.*;

/**
 * CSCI-242 AP COMPUTER SCIENCE X
 * Project 2: Place
 *
 * PTUI for the textual user interface that clients can opt for to use in PLACE
 *
 * @author Justin Yau
 */
public class PlacePTUI extends ConsoleApplication implements Observer {

    private NetworkClient serverConn; // The client's connection to the server
    private ClientModel model;        // The model of this PTUI
    private Scanner userIn;           // Scanner reading user input
    private PrintWriter userOut;      // System.out stream
    private String userName;          // Username of this client

    /***
     * Establishes a connection with the server
     */
    public void init() {
        List< String > args = super.getArguments();

        // Get host info from command line
        String host = args.get( 0 );
        int port = Integer.parseInt( args.get( 1 ) );
        this.userName = args.get( 2 );

        // Create uninitialized board.
        this.model = new ClientModel();
        // Create the network connection.
        try {
            this.serverConn = new NetworkClient( host, port, this.userName, this.model );
            System.out.println(this.serverConn.getSock().toString());
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e);
            System.exit(0);
        }

    }

    /***
     * Routine called when a tile is updated by the server
     * @param tile - The tile that has been updated
     */
    private void updateTile(PlaceTile tile) {
        this.userOut.println("=== TILE CHANGED ===");
        this.userOut.println(this.model);
        this.userOut.println("=== Row: " + tile.getRow() + " Col: " + tile.getCol() + " ===");
        this.userOut.println("=== TILE CHANGED ===");
        if(this.model.canMakeMove()) {
            this.userOut.println("=== You can still change a tile! Enter: Row Col Color ===");
        }
    }

    /***
     * Routine called when the model is active. Will display appropriate messages to the user
     * @param arg - Object that has been passed through notifyObservers(), if any.
     */
    private void handleActive(Object arg) {
        if(arg instanceof PlaceTile) {
            updateTile((PlaceTile) arg);
        } else {
            if(this.model.canMakeMove()) {
                boolean done = false;
                do {
                    try {
                        this.userOut.println("=== Current Board ===");
                        this.userOut.println(this.model);
                        this.userOut.println("=== You can now change a tile! Enter: Row Col Color ===");
                        this.userOut.flush();
                        int row = this.userIn.nextInt();
                        if(row == -1) {
                            this.model.close();
                            this.userOut.println("=== DISCONNECTING ===");
                            return;
                        }
                        int col = this.userIn.nextInt();
                        int color  = Integer.parseInt(this.userIn.next(), 16);
                        PlaceTile tileTBP = new PlaceTile(row, col, this.userName, PlaceExchange.colors[color], new Date().getTime());
                        if(this.model.isValid(tileTBP)) {
                            this.serverConn.createTileChangeRequest(tileTBP);
                            done = true;
                        }
                    } catch (IOException | IllegalStateException e) {
                        done = true;
                    }
                } while (!done);
            }
        }
    }

    /***
     * Updates the current state of the view that the user sees
     * @param arg - Object that has been passed through notifyObservers(), if any.
     */
    private void refresh(Object arg) {
        ClientModel.Status status = this.model.getStatus();
        switch(status) {
            case ACTIVE:
                handleActive(arg);
                break;
            case CLOSED:
                this.endSession();
                break;
            case ERROR:
                this.userOut.println("ERROR: " + status.toString());
                //DISCLAIMER: THIS WILL NOT EXIT THE APPLICATION IF THE SERVER CONNECTION IS LOST AS SCANNER NEXTINT IS STILL WAITING
                //I HAVE NO REAL EFFICIENT WAY OF UNBLOCKING IT WITHOUT SOMETHING LIKE JUNIT
                //PS YOUR REVERSI PROGRAM IS ALSO LIKE THIS
                this.endSession();
                break;
        }
    }

    /***
     * Closes all stream connections and the connection the server
     */
    public void stop() {
        this.userIn.close();
        this.userOut.close();
        this.serverConn.close();
    }

    /***
     * Notifies the thread for this application to shut down
     */
    private synchronized void endSession() {
        this.notify();
    }

    @Override
    public void update(Observable o, Object arg) {

        assert o == this.model: "Update from non-model Observable";

        this.refresh(arg);
    }

    @Override
    public synchronized void go(Scanner consoleIn, PrintWriter consoleOut) {
        this.userIn = consoleIn;
        this.userOut = consoleOut;

        // Connect UI to model. Can't do it sooner because streams not set up.
        this.model.addObserver( this );
        // Manually force a display of all board state, since it's too late
        // to trigger update().
        this.refresh(null);
        while ( this.model.getStatus() == ClientModel.Status.ACTIVE ) {
            try {
                this.wait();
            }
            catch( InterruptedException ie ) {}
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java PlaceClient host port username");
        } else {
            ConsoleApplication.launch(PlacePTUI.class, args);
        }
    }

}
