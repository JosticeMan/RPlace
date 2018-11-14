package place.client;

import place.PlaceBoard;
import place.PlaceTile;
import place.client.model.ClientModel;
import place.network.PlaceRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.NoSuchElementException;

public class NetworkClient {

    private static final boolean DEBUG = false;
    private static final int SLEEP_TIME = 500;

    private Socket sock;
    private String userName;
    private ObjectOutputStream networkOut;
    private ObjectInputStream networkIn;
    private ClientModel board;
    private boolean go;
    private Thread netThread;

    public NetworkClient(String hostname, int portNumber, String userName, ClientModel model) throws
                                                                                IOException, ClassNotFoundException {
            this.sock = new Socket( hostname, portNumber );
            this.networkIn = new ObjectInputStream( sock.getInputStream() );
            this.networkOut = new ObjectOutputStream( sock.getOutputStream() );
            this.userName = userName;
            this.board = model;
            this.go = true;

            createLoginRequest(userName);
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

    private static void dPrint( Object logMsg ) {
        if ( NetworkClient.DEBUG ) {
            System.out.println( logMsg );
        }
    }

    private synchronized boolean goodToGo() {

        return this.go;
    }

    private synchronized void stop() {

        this.go = false;
    }

    public void close() {
        try {
            createError("DISCONNECT");
        }
        catch( IOException ioe ) {
        }
    }

    public void error( String arguments ) {
        NetworkClient.dPrint( '!' + arguments );
        this.board.error(arguments);
        dPrint( "Fatal error: " + arguments );
        this.stop();
    }

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

    public void createTileChangeRequest(PlaceTile tile) throws IOException {
        PlaceRequest<PlaceTile> req = new PlaceRequest<>(PlaceRequest.RequestType.CHANGE_TILE, tile);
        this.networkOut.writeUnshared(req);
        this.networkOut.flush();
        try {
            this.board.setMove(false);
            netThread.sleep(SLEEP_TIME);
            this.board.setMove(true);
        } catch (InterruptedException e){

        }
    }

    public void createLoginRequest(String username) throws IOException {
        PlaceRequest<String> req = new PlaceRequest<>(PlaceRequest.RequestType.LOGIN, username);
        this.networkOut.writeUnshared(req);
        this.networkOut.flush();
    }

    public void createError(String errMsg) throws IOException {
        PlaceRequest<String> req = new PlaceRequest<>(PlaceRequest.RequestType.ERROR, errMsg);
        this.networkOut.writeUnshared(req);
        this.networkOut.flush();
    }


}
