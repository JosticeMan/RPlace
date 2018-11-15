package place.client.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import javafx.util.Duration;
import place.PlaceColor;
import place.PlaceTile;
import place.client.NetworkClient;
import place.client.model.ClientModel;
import place.network.PlaceExchange;

import java.io.IOException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * CSCI-242 AP COMPUTER SCIENCE X
 * Project 2: Place
 *
 * GUI Interface for the Place Application
 *
 * @author Justin Yau
 */
public class PlaceGUI extends Application implements Observer {

    private double rectangleSize = 500; // The client
    private NetworkClient serverConn;               // The connection client

    private ClientModel model;                      // The model containing the state of the board
    private String username;                        // The username of the client
    private Rectangle[][] grid;                     // The current state of all the rectangles being stored on the gridPane
    private ToggleGroup canvas;                     // The current group of canvas buttons
    private HashMap<Rectangle, Tooltip> tips;       // Map of all the rectangles on the board and their tooltips, if any

    /***
     * Establishes connection with the server and sets up the model for communication
     */
    public void init() {
        try {
            // Get host info from command line
            List<String> args = getParameters().getRaw();

            // get host info and username from command line
            String host = args.get(0);
            int port = Integer.parseInt(args.get(1));
            this.username = args.get(2);

            this.tips = new HashMap<Rectangle, Tooltip>();
            // Create uninitialized board.
            this.model = new ClientModel();
            // Create the network connection.
            this.serverConn = new NetworkClient( host, port, this.username, this.model );

        } catch( ClassNotFoundException | ArrayIndexOutOfBoundsException | NumberFormatException | IOException e ) {
            System.out.println( e );
            System.exit(0);
        }
    }

    /***
     * Sets up the stage to display and adds observer
     * @param primaryStage - The stage to show
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        initalizeStage(primaryStage);
        primaryStage.show();
        this.model.addObserver(this);
    }

    /***
     * Loads up the starting board and populates a gridpane with tiles
     * @param dim - The dimensions of the board
     * @return - A gridpane populated with the appropriate tiles
     */
    public GridPane createBoard(int dim) {
        GridPane theBoard = new GridPane();
        PlaceTile[][] startBoard = model.getBoard();
        grid = new Rectangle[dim][dim];
        for(int row = 0; row < dim; row++) {
            for(int col = 0; col < dim; col++) {
                Rectangle r = new Rectangle(rectangleSize, rectangleSize);
                updateTooltip(r, startBoard[row][col]);
                PlaceColor color = startBoard[row][col].getColor();
                r.setFill(Color.rgb(color.getRed(), color.getGreen(), color.getBlue()));
                final int re = row;
                final int ce = col;
                r.setOnMousePressed( event -> {
                    buttonPressed(re, ce);
                });
                //r.setStroke(Color.BLACK);
                //r.setStrokeType(StrokeType.INSIDE);
                grid[row][col] = r;
                theBoard.add(r, col, row);
            }
        }
        theBoard.setSnapToPixel(false);
        return theBoard;
    }

    /***
     * This runnable is executed to hide the tooltip from the GUI
     */
    class hideTip implements Runnable {

        private Tooltip tip;

        public hideTip(Tooltip tip) {
            this.tip = tip;
        }

        @Override
        public void run() {
            tip.hide();
        }
    }

    /***
     * Updates the inputted rectangle with a tooltip containing updated info on the inputted tile
     * @param r - The rectangle to update a tooltip for
     * @param t - The tile containing updated information for the tooltip
     */
    public void updateTooltip(Rectangle r, PlaceTile t) {
        Date stamp = new Date(TimeUnit.MILLISECONDS.convert(t.getTime(), TimeUnit.MILLISECONDS));
        Tooltip info = new Tooltip("(" + t.getRow() + ", " + t.getCol() + ") \n" +
                t.getOwner() + "\n" +
                stamp.toString());
        //Tooltip.install(r.getStyleableNode(), info);
        if(tips.containsKey(r)) {
           Platform.runLater(new hideTip(tips.get(r)));
           tips.replace(r, info);
        } else {
            tips.put(r, info);
        }
        r.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Node node = (Node) event.getSource();
                info.show(node, event.getScreenX() + 10, event.getScreenY());
            }
        });
        r.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                info.hide();
            }
        });
    }

    /***
     * Creates a new canvas for the user to select a color to paint with
     * @param dim - The dimensions of the board
     * @return - A gridpane with toggles to select a color
     */
    public GridPane createCanvas(int dim) {
        GridPane canvasPane = new GridPane();

        double btnSize = (dim * rectangleSize)/PlaceColor.TOTAL_COLORS;
        canvas = new ToggleGroup();
        for(int i = 0; i < PlaceExchange.colors.length; ++i) {
            ToggleButton btn = new ToggleButton();
            if(i == 3) {
                btn.setSelected(true);
            }
            btn.setPrefSize(btnSize, btnSize);
            PlaceColor color = PlaceExchange.colors[i];
            String hex = String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
            btn.setStyle("-fx-background-color: " + hex + ";");
            btn.setText(String.valueOf(i));
            btn.setOnAction( (ActionEvent e) -> canvas.selectToggle(btn));
            btn.setToggleGroup(canvas);
            canvasPane.add(btn, i, 0);
        }
        return canvasPane;
    }

    /***
     * Populates the stage appropriately with initial stuff
     * @param primaryStage - The stage to show to the user
     * @throws Exception
     */
    public void initalizeStage(Stage primaryStage) throws Exception {
        if(model != null) {
            BorderPane mainPane = new BorderPane();

            int dim = model.getDim();
            rectangleSize = rectangleSize/dim;

            GridPane theBoard = createBoard(dim);
            GridPane canvasPane = createCanvas(dim);

            mainPane.setCenter(theBoard);
            mainPane.setBottom(canvasPane);

            primaryStage.setTitle("Place: " + this.username);
            primaryStage.setResizable(false);
            primaryStage.setScene(new Scene(mainPane));
        }
    }

    /***
     * Routine called when a button is pressed. Will fire a tile change request if user can make a move
     * @param row - The row of the button
     * @param col - The col of the button
     */
    public void buttonPressed(int row, int col) {
        PlaceTile tileTBP = new PlaceTile(row, col, this.username,
                PlaceExchange.colors[Integer.parseInt(((ToggleButton)canvas.getSelectedToggle()).getText())]
                , System.currentTimeMillis());
        try {
            if(this.model.canMakeMove()) {
                this.serverConn.createTileChangeRequest(tileTBP);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /***
     * Routine executed to update the GUI components
     */
    class refreshRun implements Runnable {

        private PlaceTile tile;
        private Rectangle r;

        public refreshRun(PlaceTile tile, Rectangle r) {
            this.tile = tile;
            this.r = r;
        }

        @Override
        public void run() {
            PlaceColor color = tile.getColor();
            r.setFill(Color.rgb(color.getRed(), color.getGreen(), color.getBlue()));
            updateTooltip(r, tile);
        }
    }

    /**
     * Update all GUI Nodes to match the state of the model.
     *
     * @param arg - The object that has been updated
     */
    private void refresh(Object arg) {
        if(arg instanceof PlaceTile) {
            PlaceTile tile = (PlaceTile) arg;
            Platform.runLater(new refreshRun(tile, this.grid[tile.getRow()][tile.getCol()]));
        }
    }

    @Override
    public void update(Observable o, Object arg) {

        assert o == this.model: "Update from non-model Observable";

        this.refresh(arg);

    }

    /***
     * Stops the current game when the window is closed
     * @throws Exception
     */
    @Override
    public void stop() throws Exception {
        super.stop();
        serverConn.close();
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java PlaceGUI host port username");
            System.exit(-1);
        } else {
            Application.launch(args);
        }
    }
}
