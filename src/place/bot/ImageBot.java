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

public class ImageBot extends Bot {

    private LinkedList<PlaceColor> tilesToPlace;
    private int[] currentSpot;
    private int dim;

    @Override
    public void makeMove(Object arg) {
        NetworkClient serverConn = super.getConn();
        try {
            if(tilesToPlace.isEmpty()) {
                super.stop();
            }
            PlaceTile tile = new PlaceTile(currentSpot[0] , currentSpot[1], super.getUserName(), tilesToPlace.pop(),
                    System.currentTimeMillis());
            if(!nextSpot()) {
                super.stop();
            }
            serverConn.createTileChangeRequest(tile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init() {
        super.init();
        String imagePath = super.getArg().get(3);
        try {
            File f = new File(imagePath);
            BufferedImage img = ImageIO.read(f);
            this.dim = getModel().getDim();
            System.out.println(this.dim);
            LinkedList<Color> colorList = ImageUtils.pixelate(img, this.dim);
            System.out.println(colorList.size());
            tilesToPlace = new LinkedList<PlaceColor>();
            currentSpot = new int[2];
            currentSpot[0] = 0;
            currentSpot[1] = 0;
            while(!colorList.isEmpty()) {
                tilesToPlace.add(determineColor(colorList.pop()));
            }
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
