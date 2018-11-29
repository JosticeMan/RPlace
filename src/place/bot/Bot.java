package place.bot;

import place.client.NetworkClient;
import place.client.model.ClientModel;
import place.client.ptui.ConsoleApplication;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;

/**
 * CSCI-242 AP COMPUTER SCIENCE X
 * Project 2: Place
 *
 *  Abstract Bot that lays out all the components needed for a bot
 *
 * @author Justin Yau
 */
public abstract class Bot extends ConsoleApplication implements Observer{

    private NetworkClient serverConn; // The client's connection to the server
    private ClientModel model;        // The model of this PTUI
    private Scanner userIn;           // Scanner reading user input
    private PrintWriter userOut;      // System.out stream
    private String userName;          // Username of this client
    private List<String> args;        // The list of args passed into the command line

    /***
     * Establishes a connection with the server
     */
    public void init() {
        this.args = super.getArguments();

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
            System.err.println(e);
            System.exit(0);
        }

    }

    /***
     * Returns the list of args passed into the command line
     * @return - The list of args passed into the command line
     */
    public List<String> getArg() {
        return args;
    }

    /***
     * Returns the current state of the model
     * @return - The current state of the model
     */
    public ClientModel getModel() {
        return model;
    }

    /***
     * Returns the current connection to the server
     * @return - The current connection to the server
     */
    public NetworkClient getConn() {
        return serverConn;
    }

    /***
     * Returns the username of the Bot
     * @return - The username of the Bot
     */
    public String getUserName() {
        return userName;
    }

    /***
     * Routine called when the model is active. Will display appropriate messages to the user
     * @param arg - Object that has been passed through notifyObservers(), if any.
     */
    public abstract void makeMove(Object arg);

    /***
     * Updates the current state of the view that the user sees
     * @param arg - Object that has been passed through notifyObservers(), if any.
     */
    private void refresh(Object arg) {
        ClientModel.Status status = this.model.getStatus();
        switch(status) {
            case ACTIVE:
                if(this.model.canMakeMove()) {
                    makeMove(arg);
                }
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
     * Generates a random number between the min and max
     * @param min - The min number to be chosen
     * @param max - The max number to be chosen
     * @return - Random number between min and max
     */
    public static int randomWithRange(int min, int max) {
        int range = (max - min) + 1;
        return (int)(Math.random() * range) + min;
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
    public synchronized void endSession() {
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
            ConsoleApplication.launch(Bot.class, args);
        }
    }

}
