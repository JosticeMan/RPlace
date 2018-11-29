package place.bot;

import place.PlaceColor;
import place.PlaceTile;
import place.bot.ImageUtil.ImageUtils;
import place.client.NetworkClient;
import place.client.model.ClientModel;
import place.client.ptui.ConsoleApplication;
import place.network.PlaceExchange;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

/**
 * CSCI-242 AP COMPUTER SCIENCE X
 * Project 2: Place
 *
 * ImageBot that can draw out inputted images after using an algorithm to pixelate them
 *
 * @author Justin Yau
 */
public class ImageBot extends Bot {

    private LinkedList<PlaceColor> tilesToPlace; // The colors to place that have been generated by the pixelation algorithm
    private int[] currentSpot;                   // Allows the bot to know where it is at on the board
    private int dim;                             // The dimensions of the board

    /***
     * Selects the next color to place, if any, and creates a request for it
     * @param arg - Object that has been passed through notifyObservers(), if any.
     */
    @Override
    public synchronized void makeMove(Object arg) {
        ClientModel model = super.getModel();
        NetworkClient serverConn = super.getConn();
        if(tilesToPlace.isEmpty()) {
            model.close();
            return;
        }
        try {
            PlaceTile tile = new PlaceTile(currentSpot[0] , currentSpot[1], super.getUserName(), tilesToPlace.pop(),
                    System.currentTimeMillis());
            boolean contin = nextSpot();
            serverConn.createTileChangeRequest(tile);
            if(!contin) {
                model.close();
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /***
     * Sets up the connection between the server and the client. Then proceeds to pixelate the inputted image.
     */
    @Override
    public void init() {
        super.init();
        String imagePath = super.getArg().get(3);
        try {
            File f = new File(imagePath);
            BufferedImage img = ImageIO.read(f);
            this.dim = getModel().getDim();
            LinkedList<Color> colorList = ImageUtils.pixelate(img, this.dim);
            tilesToPlace = new LinkedList<PlaceColor>();
            currentSpot = new int[2];
            currentSpot[0] = 0;
            currentSpot[1] = 0;
            while(!colorList.isEmpty()) {
                tilesToPlace.add(determineColor(colorList.pop()));
            }
            //System.out.println(tilesToPlace.size());
        } catch (IOException e) {
            System.err.println(e);
            System.exit(0);
        }
    }

    /***
     * Determines which place color the inputted color is closesst to
     * @param col - The color to match a place color to
     * @return - A place color that the color is closest to
     */
    public PlaceColor determineColor(Color col) {
        int red = col.getRed();
        int green = col.getGreen();
        int blue = col.getBlue();
        int[] proximity = new int[PlaceColor.TOTAL_COLORS];
        PlaceColor[] colors = PlaceExchange.colors;
        for(int i = 0; i < colors.length; ++i) {
            int[] diff = { Math.abs(colors[i].getRed() - red),
                           Math.abs(colors[i].getGreen() - green),
                           Math.abs(colors[i].getBlue())};
            proximity[i] = ImageBot.avg(diff);
        }
        return colors[smallest(proximity)];
    }

    /***
     * Determines the smallest number in the inputted array
     * @param num - The number array to find the smallest of
     * @return - The smallest number in the inputted array
     */
    public static int smallest(int[] num) {
        int ind = 0;
        int min = num[0];
        for(int i = 1; i < num.length; ++i) {
            if(min > num[i]) {
                min = num[i];
                ind = i;
            }
        }
        return ind;
    }

    /***
     * Moves the bot to the next spot in the grid and returns whether or not it has navigated through all the spots already
     * @return - Whether or not the bot has navigated through all the spots already
     */
    public boolean nextSpot() {
        currentSpot[1]++;
        if(currentSpot[1] >= dim) {
            currentSpot[0]++;
            currentSpot[1] = 0;
        }
        if(currentSpot[0] >= dim) {
            return false;
        }
        return true;
    }

    /***
     * Finds the average based on the inputted numerical array
     * @param num - The numerical array to find the average of
     * @return - The average of the inputted numerical array
     */
    public static int avg(int[] num) {
        int sum = 0;
        for(int i = 0; i < num.length; ++i) {
            sum += num[i];
        }
        return sum/num.length;
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Usage: java PlaceClient host port username imgpath");
        } else {
            ConsoleApplication.launch(ImageBot.class, args);
        }
    }
}
