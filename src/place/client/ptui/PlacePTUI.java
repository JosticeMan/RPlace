package place.client.ptui;

import place.PlaceColor;
import place.PlaceTile;
import place.client.NetworkClient;
import place.client.model.ClientModel;
import place.test.Client;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class PlacePTUI extends ConsoleApplication implements Observer {

    private static final PlaceColor[] colors = {PlaceColor.BLACK, PlaceColor.GRAY, PlaceColor.SILVER, PlaceColor.WHITE,
                                                PlaceColor.MAROON, PlaceColor.RED, PlaceColor.OLIVE, PlaceColor.YELLOW,
                                                PlaceColor.GREEN, PlaceColor.LIME, PlaceColor.TEAL, PlaceColor.AQUA,
                                                PlaceColor.NAVY, PlaceColor.BLUE, PlaceColor.PURPLE, PlaceColor.FUCHSIA};

    private NetworkClient serverConn;
    private ClientModel model;
    private Scanner userIn;
    private PrintWriter userOut;
    private String userName;

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
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e);
            System.exit(0);
        }

    }

    private void updateTile(PlaceTile tile) {
        this.userOut.println("=== TILE CHANGED ===");
        this.userOut.println(this.model);
        this.userOut.println("=== Row: " + tile.getRow() + " Col: " + tile.getCol() + " ===");
        this.userOut.println("=== TILE CHANGED ===");
        if(this.model.canMakeMove()) {
            this.userOut.println("=== You can still change a tile! Enter: Row Col Color ===");
        }
    }

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
                        int color  = Integer.parseInt(this.userIn.next());
                        PlaceTile tileTBP = new PlaceTile(row, col, this.userName, colors[color], new Date().getTime());
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
                this.userOut.println(status.toString());
                break;
        }
    }

    public void stop() {
        this.userIn.close();
        this.userOut.close();
        this.serverConn.close();
    }

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
