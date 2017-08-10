package polygoniterator;

import java.awt.Point;
import java.awt.image.BufferedImage;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Robert A. Cherry (MessiahWolf@gmail.com) You can use or sample if you
 * give original credit to me and don't distribute for monetary gains. Make sure
 * they give the credit to me. MUHAHAHAH!
 */
public class PIterator {

    // Variable Declaration
    // Project Nested Classes
    private final ColorPointPair[] arr;
    // Data Types
    private int lastIndex = 0;
    // Java Native Classes
    private final BufferedImage image;
    private Point curPos;
    // End of Variable Declaration

    public PIterator(BufferedImage image) {
        this.image = image;

        // So imagine a rectangle with a point in every 45degree angle
        // We're going clockwise from 0degrees(Directly Right) to 315(Top Right)
        arr = new ColorPointPair[8];
        //Arrays.fill(arr, new ColorPointPair((new Color(0, 0, 0, 0)).getRGB(), new Point()));
    }

    // Must set this to do anything with this class
    public void init(Point curPos) {
        this.curPos = curPos;
    }

    // Locates the next empty space, but it depends on which index was last used.
    public ColorPointPair getNextOpenSpace() {

        // Grab the nearby pixels.
        updatePixelSpace();

        // So this loop is going to go through the array which is in
        // clockwise order of: 0-Bottom Right, 1- Bottom, 2-Bottom Left, 3-Left, etc
        for (int i = lastIndex; i < arr.length; i++) {

            // The pixel in the direction decided.
            final ColorPointPair p = arr[i];

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

        // @DEBUG
        //System.out.println("Couldn't find anything from: " + curPos + "\nstarting from index: " + lastIndex);
        // The ones we didn't use in the above loop because lastIndex was changed from 0.
        final int unused = arr.length - lastIndex;

        // If we get to the end of the array and we haven't checked all the test points; move backwards towards 0
        // Essentially we're going counter-clockwise.
        for (int j = lastIndex; j < arr.length + unused; j++) {

            // @DEBUG
            //System.out.println("Backward: " + j + " A: " + (j % arr.length));
            // Again, the next pixel decided. Mod will keep my int(j) within the bounds of arr(.length)
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

                // @DEBUG
                //System.out.println("New Last Index: " + lastIndex);
                // @DEBUG This took forever.
                //System.out.println("Backwards Catch[" + j + "]: " + p.point);
                //main.paintAllThese(arr);
                return p;
            }// else {
            //System.err.println("Backward No Good[" + j + "]: " + p.rgb);
            //}
        }

        // Then return nothing.
        return null;
    }

    // Accounts for 8 directions layed ordered in an array. This just updates it for the current position in the image.
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

    // Checking if the test point is within bounds; convience method.
    private boolean isPixel(BufferedImage img, Point point) {
        return point.x <= img.getWidth() && point.y <= img.getHeight() && point.x >= 1 && point.y >= 1;
    }

    // This pixel must be within the image bounds ; returns the rgb at point as well.
    private int testPixel(BufferedImage img, int x, int y) {
        return x <= img.getWidth() - 1 && y <= img.getHeight() - 1 && x >= 1 && y >= 1 ? img.getRGB(x, y) : 0;
    }

    public boolean hasMore() {

        // First make sure we're up to date
        updatePixelSpace();

        // If even one is not 0 then we have more.
        for (ColorPointPair p : arr) {
            if (p.rgb != 0) {
                return true;
            }
        }

        // Otherwise we have none.
        return false;
    }

    // Again another bastard nested class of mine.
    public class ColorPointPair {

        // Variable Declaration
        // Data Types
        private final int rgb;
        // Java Native Classes
        private final Point point;
        // End of Variable Delcaration

        // Simply pairs an rgb code and a point in on object.
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
