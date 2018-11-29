package place.bot.ImageUtil;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * CSCI-242 AP COMPUTER SCIENCE X
 * Project 2: Place
 *
 * Contains utility methods to pixelate images and convert them to something we can use in our application.
 * NOTE: NOT ALL OF THIS IS MY CODE. I ONLY MODIFIED THE PIXELATE METHOD TO SUIT MY NEEDS
 *
 * @author Justin Yau
 * @see "https://stackoverflow.com/questions/15777821/how-can-i-pixelate-a-jpg-with-java' - THIS CODE IS A MODIFIED VERSION
 * OF THE ONE PROVIDED AT THIS LINK. CREDITS TO THE PERSON WHO GAVE THE ANSWER WITH THIS CODE.
 */
public class ImageUtils {

    /***
     * Pixelates the inputted image and renders it to the inputted dimension size
     * @param imageToPixelate - The image to pixelate
     * @param dim - The dimension to render the image at
     * @return - The pixelated image rendered at the inputted dimension
     */
    public static LinkedList<Color> pixelate(BufferedImage imageToPixelate, int dim) {
        LinkedList<Color> list = new LinkedList<Color>();
        int heightPixel = imageToPixelate.getHeight()/dim;
        int widthPixel = imageToPixelate.getWidth()/dim;
        for (int y = 0; y < imageToPixelate.getHeight(); y += heightPixel) {
            for (int x = 0; x < imageToPixelate.getWidth(); x += widthPixel) {
                BufferedImage croppedImage = getCroppedImage(imageToPixelate, x, y, widthPixel, heightPixel);
                Color dominantColor = getDominantColor(croppedImage);
                list.add(dominantColor);
            }
        }
        return list;
    }

    /***
     * Adjusts the parameters to prevent errors and crops the inputted image to return it
     * @param image - The image to crop
     * @param startx - The x coord to start the crop at
     * @param starty - The y coord to start the crop at
     * @param width - The width of the crop
     * @param height - The height of the crop
     * @return - The cropped image
     */
    public static BufferedImage getCroppedImage(BufferedImage image, int startx, int starty, int width, int height) {
        if (startx < 0) startx = 0;
        if (starty < 0) starty = 0;
        if (startx > image.getWidth()) startx = image.getWidth();
        if (starty > image.getHeight()) starty = image.getHeight();
        if (startx + width > image.getWidth()) width = image.getWidth() - startx;
        if (starty + height > image.getHeight()) height = image.getHeight() - starty;
        return image.getSubimage(startx, starty, width, height);
    }

    /***
     * Determines the most dominant color in the bufferedImage
     * @param image - The image to determine what color is most dominant in it
     * @return - The most dominant color in the inputted bufferedImage
     */
    public static Color getDominantColor(BufferedImage image) {
        Map<Integer, Integer> colorCounter = new HashMap<>(100);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int currentRGB = image.getRGB(x, y);
                int count = colorCounter.getOrDefault(currentRGB, 0);
                colorCounter.put(currentRGB, count + 1);
            }
        }
        return getDominantColor(colorCounter);
    }

    /***
     * Determines the most dominant color in the bufferedImage
     * @param colorCounter - A map to keep track how many times each color appeared
     * @return - The most dominant color in the inputted bufferedImage
     */
    @SuppressWarnings("unchecked")
    private static Color getDominantColor(Map<Integer, Integer> colorCounter) {
        int dominantRGB = colorCounter.entrySet().stream()
                .max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1)
                .get()
                .getKey();
        return new Color(dominantRGB);
    }

}
