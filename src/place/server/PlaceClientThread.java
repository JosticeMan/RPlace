package place.server;

import place.PlaceBoard;
import place.PlaceTile;
import place.network.PlaceRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class PlaceClientThread extends Thread {

    private static final boolean DEBUG = false;
    private static final int SLEEP_TIME = 500;

    private Socket socket = null;
    private PlaceServer server;
    private String username;
    private ObjectOutputStream out;
    private ObjectInputStream in;

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

    private static void dPrint( Object logMsg ) {
        if ( PlaceClientThread.DEBUG ) {
            System.out.println( logMsg );
        }
    }

    public void run() {
        try {
            PlaceRequest<?> req = (PlaceRequest<?>) in.readUnshared();
            if (req.getType() == PlaceRequest.RequestType.LOGIN) {
                if( server.addClient(username = (String) req.getData(), this.socket, this)) {
                    createLoginSuccess();
                    createBoardRequest();
                    go();
                } else {
                    createError(username + " already logged in! Try a different user!");
                }
            } else {
                createError("Expected login request first!");
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        this.dPrint(username + " Thread ended!");
    }

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

    public void processRequest(PlaceRequest<?> req) throws IOException, InterruptedException {
        PlaceRequest.RequestType type = req.getType();
        if(type == PlaceRequest.RequestType.CHANGE_TILE) {
            if( server.changeTile((PlaceTile) req.getData()) ) {
                this.sleep(SLEEP_TIME);
            }
        } else {
            createError("Expected change tile requests only!");
        }
    }

    public void createLoginSuccess() throws IOException {
        PlaceRequest<String> req = new PlaceRequest<>(PlaceRequest.RequestType.LOGIN_SUCCESS, socket.toString());
        this.out.writeUnshared(req);
        this.out.flush();
    }

    public void createBoardRequest() throws IOException {
        PlaceRequest<PlaceBoard> req = new PlaceRequest<>(PlaceRequest.RequestType.BOARD, server.getBoard());
        this.out.writeUnshared(req);
        this.out.flush();
    }

    public void createError(String errMsg) throws IOException {
        PlaceRequest<String> req = new PlaceRequest<>(PlaceRequest.RequestType.ERROR, errMsg);
        this.out.writeUnshared(req);
        this.out.flush();
    }

    public void createChangedTile(PlaceTile tile) throws IOException {
        PlaceRequest<PlaceTile> req = new PlaceRequest<>(PlaceRequest.RequestType.TILE_CHANGED, tile);
        this.out.writeUnshared(req);
        this.out.flush();
    }

}
