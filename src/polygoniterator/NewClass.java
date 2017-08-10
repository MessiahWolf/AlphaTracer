package polygoniterator;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author rcher
 */
public class NewClass {

    //
    private ColorPointPair[] arr;
    //
    private boolean swap = false;
    private int lastIndex = 0;
    private Point initPos, curPos;
    private final BufferedImage image;
    private Main main;

    public NewClass(Main main, BufferedImage image) {
        this.main = main;
        this.image = image;

        // So imagine a rectangle with a point in every 45degree angle
        // We're going clockwise from 0degrees(Directly Right) to 315(Top Right)
        arr = new ColorPointPair[8];
        Arrays.fill(arr, new ColorPointPair((new Color(0, 0, 0, 0)).getRGB(), new Point()));
    }

    public ColorPointPair getNextOpenSpace() {

        // Grab the nearby pixels.
        updatePixelSpace();

        // This variable will track which indexes we skipped with use of lastIndex
        int t = 0;

        // So this loop is going to go through the array which is in
        // clockwise order of: 0-Bottom Right, 1- Bottom, 2-Bottom Left, 3-Left, etc
        for (int i = lastIndex; i < arr.length; i++) {

            // The pixel in the direction decided.
            final ColorPointPair p = arr[i];

            // increment this little debug value.
            t++;

            // If the color code at index has a valid pixel to test.
            if (p.rgb != 0) {

                // Move the current position forward
                curPos = p.point;

                // Go back 45 degrees.
                lastIndex = i - 1 < 0 ? 0 : i - 1;

                // Return that point as the one we've chosen.
                return p;
            }
        }

        //
        System.out.println("Couldn't find anything from: " + curPos + "\nstarting from index: " + lastIndex);

        // The ones we didn't use in the above loop because lastIndex was changed from 0.
        final int unused = arr.length - lastIndex;

        // If we get to the end of the array and we haven't checked all the test points; move backwards towards 0
        // Essentially we're going counter-clockwise.
        for (int j = lastIndex; j < arr.length + unused; j++) {

            //
            System.out.println("Backward: " + j + " A: " + (j % arr.length));

            // Again, the next pixel decided. Mod will keep my iter(j) within the bounds of arr(length)
            // So it will loop back on itself. Cool, right?
            final ColorPointPair p = arr[j % arr.length];

            // As long as this test pixel isn't out of bounds
            // this also tests for pixels that have a rgb of 0(red), 0(blue), 0(green), 0(alpha),
            // but not pixels with a value greater than 1 in any of those color / alpha ranges.
            if (p.rgb != 0) {

                // Again moving forward.
                curPos = p.point;
                
                // Our new last index must include the ones we skipped so we start
                // off into those skipped directions. Then back one just in case
                // the previous direction is a fit.
                lastIndex = (j % (arr.length + unused)) - 1;
                System.out.println("New Last Index: " + lastIndex);

                //
                System.out.println("Backwards Catch[" + j + "]: " + p.point);
                //main.paintAllThese(arr);
                return p;
            } else {
                //System.err.println("Backward No Good[" + j + "]: " + p.rgb);
            }
        }

        // Then return nothing.
        return null;
    }

    private void updatePixelSpace() {

        // We don't consider the center because we're already there.
        // Pixel right
        arr[0] = new ColorPointPair(isPixel(image, curPos) ? testPixel(image, curPos.x + 1, curPos.y) : 0, new Point(curPos.x + 1, curPos.y));
        // Pixel bottom right
        arr[1] = new ColorPointPair(isPixel(image, curPos) ? testPixel(image, curPos.x + 1, curPos.y + 1) : 0, new Point(curPos.x + 1, curPos.y + 1));
        // Pixel bottom
        arr[2] = new ColorPointPair(isPixel(image, curPos) ? testPixel(image, curPos.x, curPos.y + 1) : 0, new Point(curPos.x, curPos.y + 1));
        // Pixel bottom left
        arr[3] = new ColorPointPair(isPixel(image, curPos) ? testPixel(image, curPos.x - 1, curPos.y + 1) : 0, new Point(curPos.x - 1, curPos.y + 1));
        // Pixel left
        arr[4] = new ColorPointPair(isPixel(image, curPos) ? testPixel(image, curPos.x - 1, curPos.y) : 0, new Point(curPos.x - 1, curPos.y));
        // Pixel top left
        arr[5] = new ColorPointPair(isPixel(image, curPos) ? testPixel(image, curPos.x - 1, curPos.y - 1) : 0, new Point(curPos.x - 1, curPos.y - 1));
        // Pixel top
        arr[6] = new ColorPointPair(isPixel(image, curPos) ? testPixel(image, curPos.x, curPos.y - 1) : 0, new Point(curPos.x, curPos.y - 1));
        // Pixel top right
        arr[7] = new ColorPointPair(isPixel(image, curPos) ? testPixel(image, curPos.x + 1, curPos.y - 1) : 0, new Point(curPos.x + 1, curPos.y - 1));
    }

    public void setStartPixel(Point initPos) {
        this.initPos = initPos;
        curPos = initPos;
    }

    private boolean isPixel(BufferedImage img, Point point) {
        return point.x <= img.getWidth() && point.y <= img.getHeight() && point.x >= 1 && point.y >= 1;
    }

    private boolean isTransparent(int testPixel) {
        return ((testPixel >> 24) & 0xFF) <= 8;
    }

    private int testPixel(BufferedImage img, int x, int y) {
        return x <= img.getWidth() - 1 && y <= img.getHeight() - 1 && x >= 1 && y >= 1 ? img.getRGB(x, y) : 0;
    }

    public boolean hasMore() {
        //
        updatePixelSpace();

        //
        for (ColorPointPair p : arr) {
            if (p.rgb != 0) {
                return true;
            }
        }

        //
        return false;
    }

    //
    public class ColorPointPair {

        //
        private final int rgb;
        private final Point point;

        //
        public ColorPointPair(Integer rgb, Point point) {
            this.rgb = rgb;
            this.point = point;
        }

        public Point point() {
            return point;
        }

        public int rgb() {
            return rgb;
        }
    }
}
