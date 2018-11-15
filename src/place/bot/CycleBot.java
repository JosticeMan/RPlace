package place.bot;

import place.PlaceColor;
import place.PlaceTile;
import place.client.NetworkClient;
import place.client.model.ClientModel;
import place.client.ptui.ConsoleApplication;
import place.network.PlaceExchange;

import java.io.IOException;

/**
 * CSCI-242 AP COMPUTER SCIENCE X
 * Project 2: Place
 *
 *  Bot that spams the living heck out of the server with random change tile request to make it theirs
 *
 * @author Justin Yau
 */
public class CycleBot extends Bot {

    /***
     * Picks a random tile and creates a change tile request for it
     * @param arg - Object that has been passed through notifyObservers(), if any.
     */
    @Override
    public void makeMove(Object arg) {
        ClientModel model = super.getModel();
        NetworkClient serverConn = super.getConn();
        try {
            serverConn.createTileChangeRequest(pickTile(model));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /***
     * Picks a random tile from the model and saves it to make it theirs
     * @param model - The model from the server
     */
    private PlaceTile pickTile(ClientModel model) {
        int dim = model.getDim();
        int row = Bot.randomWithRange(0, dim);
        int col = Bot.randomWithRange(0, dim);
        int currentColor = Bot.randomWithRange(0, PlaceColor.TOTAL_COLORS - 1);
        return new PlaceTile(row, col, super.getUserName(), PlaceExchange.colors[currentColor],
                System.currentTimeMillis());
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java PlaceClient host port username");
        } else {
            ConsoleApplication.launch(CycleBot.class, args);
        }
    }
}
