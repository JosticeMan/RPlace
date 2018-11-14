package place.client;

import place.PlaceBoard;
import place.PlaceTile;
import place.client.model.ClientModel;
import place.network.PlaceExchange;
import place.network.PlaceRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.NoSuchElementException;

/**
 * CSCI-242 AP COMPUTER SCIENCE X
 * Project 2: Place
 *
 * Essentially the client's connection to the server and acts sort of like the controller in the MVC model.
 *
 * @author Justin Yau
 */
public class NetworkClient {

    private Socket sock;                    // Client's connection to the server
    private String userName;                // Client's username
    private ObjectOutputStream networkOut;  // The output stream of the socket
    private ObjectInputStream networkIn;    // The input stream of the socket
    private ClientModel board;              // The current state of the board
    private boolean go;                     // Whether or not to handle requests or not
    private Thread netThread;               // The thread that this process runs under

    /***
     * Creates a new connection to the server and will handle any requests sent from the server and reply appropriately
     * @param hostname - The hostname of the server
     * @param portNumber - The port
     * @param userName - The username of the client
     * @param model - The current state of the board
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public NetworkClient(String hostname, int portNumber, String userName, ClientModel model) throws
                                                                                IOException, ClassNotFoundException {
            this.sock = new Socket( hostname, portNumber );
            this.networkIn = new ObjectInputStream( sock.getInputStream() );
            this.networkOut = new ObjectOutputStream( sock.getOutputStream() );
            this.userName = userName;
            this.board = model;
            this.go = true;

            PlaceExchange.createLoginRequest(networkOut, userName);
            //Block waiting for next request from the server
            PlaceRequest<?> req = (PlaceRequest<?>) networkIn.readUnshared();
            if (req.getType() == PlaceRequest.RequestType.LOGIN_SUCCESS) {
                NetworkClient.dPrint(((String) req.getData()));
                PlaceRequest<?> board = (PlaceRequest<?>) networkIn.readUnshared();
                if(board.getType() == PlaceRequest.RequestType.BOARD) {
                    this.board.initialize((PlaceBoard) board.getData());
                    NetworkClient.dPrint( this.board.toString());

                    // Run rest of client in separate thread.
                    // This threads stops on its own at the end of the game and
                    // does not need to rendez-vous with other software components.
                    netThread = new Thread( () -> this.run() );
                    netThread.start();
                }
            } else if (req.getType() == PlaceRequest.RequestType.ERROR){
                this.error((String) req.getData());
                NetworkClient.dPrint((String) req.getData());
            }
    }

    /***
     * Returns the client's connection to the server
     * @return - The client's connection to the server
     */
    public Socket getSock() {
        return sock;
    }

    /***
     * Debug method that will only print messages if the Debug variable is enabled
     * @param logMsg - The message to print
     */
    private static void dPrint( Object logMsg ) {
        if ( PlaceExchange.DEBUG ) {
            System.out.println( logMsg );
        }
    }

    /***
     * Returns whether or not this thread should keep handling requests
     * @return - Whether or not this thread should keep handling requests
     */
    private synchronized boolean goodToGo() {

        return this.go;
    }

    /***
     * Updates the state of this connection
     */
    private synchronized void stop() {

        this.go = false;
    }

    /***
     * Sends a error to the socket and makes it close. Also updates the state of the board
     */
    public void close() {
        try {
            PlaceExchange.createError(this.networkOut, "DISCONNECT");
        }
        catch( IOException ioe ) {
        }
        this.board.close();
    }

    /***
     * Routine used when an error has been received and will deal with it accordingly
     * @param arguments - Information regarding the error
     */
    public void error( String arguments ) {
        NetworkClient.dPrint( '!' + arguments );
        this.board.error(arguments);
        dPrint( "Fatal error: " + arguments );
        this.stop();
    }

    /***
     * Handles incoming requests
     */
    private void run() {
        while( this.goodToGo() ) {
            try {
                PlaceRequest<?> request = (PlaceRequest<?>) this.networkIn.readUnshared();
                NetworkClient.dPrint(request.getType() + " " + request.getData());
                if(request.getType() == PlaceRequest.RequestType.TILE_CHANGED) {
                    board.updatePixel((PlaceTile) request.getData());
                } else if(request.getType() == PlaceRequest.RequestType.ERROR){
                    this.error((String) request.getData());
                    this.stop();
                }
            }
            catch( NoSuchElementException nse ) {
                // Looks like the connection shut down.
                this.error( "Lost connection to server." );
                this.stop();
            }
            catch( Exception e ) {
                this.error( e.getMessage() + '?' );
                this.stop();
            }
        }
        this.close();
    }

    /***
     * Creates a request to change a tile and sleeps a required cooldown time
     * @param tile - The tile to be updated
     * @throws IOException
     */
    public void createTileChangeRequest(PlaceTile tile) throws IOException {
        PlaceExchange.createTileChangeRequest(this.networkOut, tile);
        try {
            this.board.setMove(false);
            netThread.sleep(PlaceExchange.SLEEP_TIME);
            this.board.setMove(true);
        } catch (InterruptedException e){

        }
    }

}
