package tracer;

import java.awt.Point;
import java.awt.image.BufferedImage;

/**
 *
 * @author Robert A. Cherry (MessiahWolf@gmail.com) You can use or sample if you
 * give original credit to me and don't distribute for monetary gains. Make sure
 * they give the credit to me. MUHAHAHAH!
 */
public class ImageTracer {

    // Variable Declaration
    // Project Nested Classes
    private final ColorPointPair[] cppArray;
    // Java Native Classes
    private final BufferedImage origImage;
    private Point curPos, initPos;
    // Data Types
    private boolean started = false;
    private int lastIndex = 0;
    // End of Variable Declaration

    public ImageTracer(BufferedImage origImage) {
        this.origImage = origImage;

        /*  
            So imagine a rectangle with a point in every 45degree angle
            We're going clockwise from 0degrees(Directly Right) to 315(Top Right)
        */
        cppArray = new ColorPointPair[8];
    }

    // Must set this to do anything with this class
    public void init(Point curPos) {
        this.curPos = curPos;
        this.initPos = new Point(curPos);
    }

    // Locates the next empty space, but it depends on which index was last used.
    public ColorPointPair getNextOpenSpace() {
        
        // Null case
        if (curPos == null  || initPos == null) {
            return null;
        }

        // Grab the nearby pixels.
        updatePixelSpace();

        // We're done.
        if (curPos.x == initPos.x && curPos.y == initPos.y && started == true) {
            return null;
        }
        
        // Updates how many indices we skipped so we don't leave any out.
        int unused = cppArray.length - lastIndex;
        
        /* 
            So this loop is going to go through the array which is in
            clockwise order of: 0-Bottom Right, 1- Bottom, 2-Bottom Left, 3-Left, etc
         */
        for (int i = lastIndex; i < cppArray.length + unused; i++) {

            // The pixel in the direction decided.
            final ColorPointPair p = cppArray[i % cppArray.length];

            // If the color code at index has a valid pixel to test.
            if (p.rgb != 0) {
                
                // Move the current position forward
                curPos = p.point;

                // @DEBUG
                //System.err.println("Point: " + p.point + " | Direction chosen:" + (i % arr.length));
                
                // @REMINDER COME HERE TO FIX ISSUES WITH DIRECTION CHANGES.
                /* 
                    This fixes the iterator trailing off to the right into
                    not transparent pixels by turning it back 45 degrees.
                */
                if (lastIndex == 0 && cppArray[7].rgb != 0 && cppArray[0].rgb != 0) {
                    lastIndex = 7;
                } else {
                    // Go back 45 degrees.
                    lastIndex = i - 1 < 0 ? 0 : i - 1;
                }

                // We've started
                started = true;

                // Return that point as the one we've chosen.
                return p;
            }
        }

        // The ones we didn't use in the above loop because lastIndex was changed from 0.
        unused = cppArray.length - lastIndex;

        //@Debug
        //System.out.println("CurPos: " + curPos + " Last Index: " + lastIndex);
        
        // This makes the tracer go back and detect shapes like spirals.
        if (lastIndex == 7) {
            lastIndex = 0;
        }

        /* 
           If we get to the end of the array and we haven't checked all the test points; move backwards towards 0
           Essentially we're going counter-clockwise.
         */
        for (int j = lastIndex; j < cppArray.length + unused; j++) {

            /* 
                Again, the next pixel decided. Mod will keep my int(j) within the bounds of arr(.length)
                So it will loop back on itself. Cool, right?
             */
            final ColorPointPair output = cppArray[j % cppArray.length];

            /* 
                As long as this test pixel isn't out of bounds
                this also tests for pixels that have a rgb of 0(red), 0(blue), 0(green), 0(alpha),
                but not pixels with a value greater than 1 in any of those color / alpha ranges.
             */
            if (output.rgb != 0) {

                // Again moving forward.
                curPos = output.point;

                /* 
                   Our new last index must include the ones we skipped so we start
                   off into those skipped directions. Then back one just in case
                   the previous direction is a fit.
                 */
                lastIndex = (j % (cppArray.length));
                
                // Return the point
                return output;
            }
        }

        // Then return nothing.
        return null;
    }

    // Accounts for 8 directions laid ordered in an array. This just updates it for the current position in the image.
    private void updatePixelSpace() {
        // We don't consider the center because we're already there.
        // Pixel right
        cppArray[0] = new ColorPointPair(isPixel(origImage, curPos) ? testPixel(origImage, curPos.x + 1, curPos.y) : 0, new Point(curPos.x + 1, curPos.y));
        // Pixel bottom right
        cppArray[1] = new ColorPointPair(isPixel(origImage, curPos) ? testPixel(origImage, curPos.x + 1, curPos.y + 1) : 0, new Point(curPos.x + 1, curPos.y + 1));
        // Pixel bottom
        cppArray[2] = new ColorPointPair(isPixel(origImage, curPos) ? testPixel(origImage, curPos.x, curPos.y + 1) : 0, new Point(curPos.x, curPos.y + 1));
        // Pixel bottom left
        cppArray[3] = new ColorPointPair(isPixel(origImage, curPos) ? testPixel(origImage, curPos.x - 1, curPos.y + 1) : 0, new Point(curPos.x - 1, curPos.y + 1));
        // Pixel left
        cppArray[4] = new ColorPointPair(isPixel(origImage, curPos) ? testPixel(origImage, curPos.x - 1, curPos.y) : 0, new Point(curPos.x - 1, curPos.y));
        // Pixel top left
        cppArray[5] = new ColorPointPair(isPixel(origImage, curPos) ? testPixel(origImage, curPos.x - 1, curPos.y - 1) : 0, new Point(curPos.x - 1, curPos.y - 1));
        // Pixel top
        cppArray[6] = new ColorPointPair(isPixel(origImage, curPos) ? testPixel(origImage, curPos.x, curPos.y - 1) : 0, new Point(curPos.x, curPos.y - 1));
        // Pixel top right
        cppArray[7] = new ColorPointPair(isPixel(origImage, curPos) ? testPixel(origImage, curPos.x + 1, curPos.y - 1) : 0, new Point(curPos.x + 1, curPos.y - 1));
    }

    // Checking if the test point is within bounds; convience method.
    private boolean isPixel(BufferedImage img, Point point) {
        return point.x < img.getWidth() && point.y < img.getHeight() && point.x >= 0 && point.y >= 0;
    }

    // This pixel must be within the image bounds ; returns the rgb at point as well.
    private int testPixel(BufferedImage img, int x, int y) {
        return x < img.getWidth() && y < img.getHeight() && x >= 0 && y >= 0 ? img.getRGB(x, y) : 0;
    }

    // Again another bastard nested class of mine.
    public class ColorPointPair {

        // Variable Declaration
        // Java Native Classes
        private final Point point;
        // Data Types
        private final int rgb;
        // End of Variable Delcaration

        // Simply pairs an rgb code and a point together in an object.
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
