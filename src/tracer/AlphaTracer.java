/*
    Copyright 2017 Robert Cherry

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package tracer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;

/*
 * @author Robert Cherry (MessiahWolf)
 */
public class AlphaTracer {

    // Variable Declaration
    // Project Nested Classes
    private final ColorPointPair[] cppArray;
    // Java Native Classes
    private final ArrayList<Polygon> polyList;
    private BufferedImage origImage;
    private BufferedImage traceImage;
    private BufferedImage progImage;
    private final HashMap<Point, Integer> pointMap;
    private Point curPos;
    private Point initPos;
    private Polygon polygon;
    // Data Types
    private boolean finished;
    private boolean running;
    private final int MIN_POLYGON_POINTS = 3;
    private final int COLOR_CLEAR = (new Color(0, 0, 0, 0)).getRGB();
    private int lastIndex;
    private int transPixel = 8;
    private String message;
    // End of Variable Declaration

    public AlphaTracer(BufferedImage image) {

        // Clear certain pixels that cause issues.
        origImage = destroyAllIslands(image);
        traceImage = bufferImage(origImage, null, BufferedImage.TYPE_INT_ARGB);
        progImage = bufferImage(origImage, null, BufferedImage.TYPE_INT_ARGB);

        // Array of surrounding points, about curPos.
        cppArray = new ColorPointPair[8];

        // Initializing.
        polyList = new ArrayList();
        pointMap = new HashMap();
        polygon = new Polygon();

        // Initial point
        this.initPos = updateStartPoint();
        this.curPos = new Point(initPos);

        // @TODO there is an error where getNextOpenSpace
        // will wander into colors and end early even though it
        // should skate along the outside. 
        // This can clearly be observed in example8.png near the
        // Fifth polygon if you slow down the timer and watch it while
        // zoomed in.
        // Possibly cppArray[] needs to fall back on itself once if the selected
        // pixel is not transparent.
    }

    public void addVerticalLine(Point pos) {

        // If we're within the bounds of the image.
        if (pos.x >= 0 && pos.y >= 0 && pos.x <= traceImage.getWidth() - 1 && pos.y <= traceImage.getHeight() - 1) {

            // Get the start and end points for our line
            final Point[] points = getVerticalLineTrace(traceImage, pos);

            // If the points exist
            if (points[0] != null && points[1] != null) {

                // Determine the distance between them.
                final int distY = points[1].y - points[0].y;

                // Clear those pixels.
                for (int i = 0; i <= distY; i++) {

                    // Setting the rgb to clear which will get ignored by the tracer
                    traceImage.setRGB(pos.x, points[0].y + i, COLOR_CLEAR);
                }

                // Clearing the in progress's image data and setting it to the trace image.
                progImage.flush();
                progImage = bufferImage(traceImage, null, BufferedImage.TYPE_INT_ARGB);

                // Make sure we can recreate this line later.
                pointMap.put(pos, 1);
            }
        }
    }

    public void addHorizontalLine(Point pos) {

        //lineList.add(getVerticalLineTrace(img, pos));
        if (pos.x >= 0 && pos.y >= 0 && pos.x < traceImage.getWidth() - 1 && pos.y < traceImage.getHeight() - 1) {

            //
            final Point[] points = getHorizontalLineTrace(traceImage, pos);

            //
            if (points[0] != null && points[1] != null) {

                //
                final int distX = points[1].x - points[0].x;

                //
                for (int i = 0; i < distX; i++) {

                    //
                    traceImage.setRGB(points[0].x + i, pos.y, COLOR_CLEAR);
                }

                // Clearing the in progress's image data and setting it to the trace image.
                progImage.flush();
                progImage = bufferImage(traceImage, null, BufferedImage.TYPE_INT_ARGB);

                // Put in the map.
                pointMap.put(pos, 0);
            }
        }
    }

    public void reset(BufferedImage image) {

        //
        origImage = destroyAllIslands(image);

        // Clear the pointmap when you set a new image.
        pointMap.clear();

        //
        reset();
    }

    public void reset() {

        // Makes it so that on each new polygon we start
        // going to the right (0 degrees) first
        lastIndex = 0;

        // Our stop variables.
        finished = false;
        running = false;

        // Error message resets too.
        message = null;

        // Clear that polygon and the list of them.
        polygon.reset();
        polyList.clear();

        // Reset the images to the original image, blah blah.
        traceImage = bufferImage(origImage, null, BufferedImage.TYPE_INT_ARGB);
        progImage = bufferImage(origImage, null, BufferedImage.TYPE_INT_ARGB);

        // Previous and Curpoint reset
        initPos = updateStartPoint();
        curPos = new Point(initPos);
        
        // Re add those lines you made on the trace image.
        for (Map.Entry<Point, Integer> map : pointMap.entrySet()) {
            switch (map.getValue()) {
                case 0:
                    addHorizontalLine(map.getKey());
                    break;
                case 1:
                    addVerticalLine(map.getKey());
                    break;
                case 2:
                    //addCustomLine(map.getKey());
                    break;
            }
        }
    }

    public void flash() {

        // Always reset the variables.
        reset();

        // Goes as fast as your processor can process.
        while (!finished) {

            // Running, sprinting, tripping and falling.
            run();
        }
    }

    public void run() {

        // CPP should be null each cycle in the case it fails
        // to acquire a non-null value in the getNextOpenSpace.
        ColorPointPair cpp = null;

        // If we're calling run, we're running.
        running = true;

        // Adding points to create the polygon and drawing those points on progImage.
        if (initPos.x > -1 && initPos.y > -1) {

            // Grab the next (first) point.
            cpp = getNextOpenSpace();

            // While the AlphaTracer isn't in no-man's-land.
            while (cpp != null) {

                // We use more than once so a variable.
                final Point point = cpp.point();

                // In the case it's stuck on a recurring point.
                // So this will happen when the current point 
                // has gone all the way around and is near the start
                // This is where we say stop running now.
                if (initPos.equals(point)) {
                    running = false;
                    finished = true;
                    break;
                }

                // Otherwise detects pixels that aren't transparent.
                if (!isTransparent(cpp.rgb())) {

                    // As always, if we're calling from an image it must exist.
                    if (progImage != null) {

                        //
                        final Color color = new Color(progImage.getRGB(point.x, point.y));

                        // Draw a dot of the inverse color at the next point on the in-progress image.
                        progImage.setRGB(point.x, point.y, (new Color(255 - color.getRed(), 255 - color.getGreen(), 255 - color.getBlue(), 255)).getRGB());
                    }

                    // Add the polygon point.
                    polygon.addPoint(point.x, point.y);

                    // Then break out.
                    break;
                }

                // The previous point has been processed. So onto the next one.
                // This allows cpp to be null at some point and exit the while loop.
                cpp = getNextOpenSpace();
            }
        }

        // Solution for very small shapes that have jagged edges.
        if (cpp != null && running) {

            // First get current input for the pixel data at curPos.
            updatePixelSpace();

            // For each point around curPos
            for (ColorPointPair p : cppArray) {

                // If curPos is really close to initPos and we've at least processed
                // 3 points then close the polygon.
                if (p.point.distance(initPos) <= 1 && polygon.npoints >= MIN_POLYGON_POINTS) {

                    // End the polygon.
                    cpp = null;

                    // Break the loop to save processing time.
                    break;
                }
            }
        }

        // Signifies the end of a polygon.
        if (cpp == null) {

            // Add the start point because usually, if not always, it's not added at the beginning.
            if (!polygon.contains(initPos)) {
                polygon.addPoint(initPos.x, initPos.y);
            }

            // Polygon must have a certain number of points to be added.
            if (polygon.npoints >= MIN_POLYGON_POINTS) {
                // Add to the list of polygons
                polyList.add(polygon);
            }

            // Renew the trace image with pixels with no connection to other pixels removed.
            traceImage = destroyAllIslands(clearRegionFromImage(traceImage, polygon));

            // Clearing the in progress's image data and setting it to the trace image.
            progImage.flush();
            progImage = bufferImage(traceImage, null, BufferedImage.TYPE_INT_ARGB);

            // Onto a new polygon.
            polygon = new Polygon();

            // Find the next start point.
            initPos = updateStartPoint();
            curPos = new Point(initPos);

            // If after updating init pos it's equals to -1,-1 then the image is cleared.
            if (initPos.x == -1 || initPos.y == -1) {
                running = false;
                finished = true;
                message = null;
            }
        }
    }

    private Point updateStartPoint() {

        // Go down y-coord and sweep left for first non-trans pixel.
        for (int y = 0; y <= traceImage.getHeight() - 1; y++) {

            // Until we hit 0.
            for (int x = traceImage.getWidth() - 1; x >= 0; x--) {

                // If the current pixel is not transparent.
                if (!isTransparent(traceImage.getRGB(x, y))) {

                    // Stop running but we're not finished; run will reset this to true 
                    running = false;
                    
                    // New polygon, then restart by cpp preferring to move to the right first.
                    lastIndex = 0;

                    // @DEBUG
                    //System.out.println("New MaxX: " + x + " Y: " + y);

                    // Return
                    return new Point(x, y);
                }
            }
        }

        // Always send to -1, -1 if we didnt find above. Image is completely transparent.
        return new Point(-1, -1);
    }

    // Accounts for 8 directions laid ordered in an array. This just updates it for the current position in the image.
    private void updatePixelSpace() {
        // We don't consider the center because we're already there.
        // Pixel right
        cppArray[0] = new ColorPointPair(isPixel(traceImage, curPos) ? testPixel(traceImage, curPos.x + 1, curPos.y) : 0, new Point(curPos.x + 1, curPos.y));
        // Pixel bottom right
        cppArray[1] = new ColorPointPair(isPixel(traceImage, curPos) ? testPixel(traceImage, curPos.x + 1, curPos.y + 1) : 0, new Point(curPos.x + 1, curPos.y + 1));
        // Pixel bottom
        cppArray[2] = new ColorPointPair(isPixel(traceImage, curPos) ? testPixel(traceImage, curPos.x, curPos.y + 1) : 0, new Point(curPos.x, curPos.y + 1));
        // Pixel bottom left
        cppArray[3] = new ColorPointPair(isPixel(traceImage, curPos) ? testPixel(traceImage, curPos.x - 1, curPos.y + 1) : 0, new Point(curPos.x - 1, curPos.y + 1));
        // Pixel left
        cppArray[4] = new ColorPointPair(isPixel(traceImage, curPos) ? testPixel(traceImage, curPos.x - 1, curPos.y) : 0, new Point(curPos.x - 1, curPos.y));
        // Pixel top left
        cppArray[5] = new ColorPointPair(isPixel(traceImage, curPos) ? testPixel(traceImage, curPos.x - 1, curPos.y - 1) : 0, new Point(curPos.x - 1, curPos.y - 1));
        // Pixel top
        cppArray[6] = new ColorPointPair(isPixel(traceImage, curPos) ? testPixel(traceImage, curPos.x, curPos.y - 1) : 0, new Point(curPos.x, curPos.y - 1));
        // Pixel top right
        cppArray[7] = new ColorPointPair(isPixel(traceImage, curPos) ? testPixel(traceImage, curPos.x + 1, curPos.y - 1) : 0, new Point(curPos.x + 1, curPos.y - 1));
    }

    // Locates the next empty space, but it depends on which index was last used.
    public ColorPointPair getNextOpenSpace() {

        // Null case
        if (curPos == null || initPos == null) {
            return null;
        }

        // Grab the nearby pixels.
        updatePixelSpace();

        // We're done.
        if (curPos.equals(initPos) && polygon.npoints >= MIN_POLYGON_POINTS) {
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
            if (p.alpha() > transPixel) {

                // Move the current position forward
                curPos = p.point;

                // @DEBUG
                //System.err.println("Point: " + p.point + " | Direction chosen:" + (i % arr.length));
                // @REMINDER COME HERE TO FIX ISSUES WITH DIRECTION CHANGES.
                /* 
                    This fixes the iterator trailing off to the right into
                    not transparent pixels by turning it back 45 degrees.
                 */
                if (lastIndex == 0 && cppArray[7].alpha() > transPixel && cppArray[0].alpha() > transPixel) {
                    lastIndex = 7;
                } else {
                    // Go back 45 degrees.
                    lastIndex = i - 1 < 0 ? 0 : i - 1;
                }

                // We've started
                //running = true;
                // Return that point as the one we've chosen.
                return p;
            }
        }

        // The ones we didn't use in the above loop because lastIndex was changed from 0.
        unused = cppArray.length - lastIndex;

        //@Debug
        //System.out.println("CurPos: " + curPos + " | InitPos: " + initPos + " Last Index: " + lastIndex);
        // This makes the tracer go back and detect shapes like spirals.
        if (lastIndex >= 7) {
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
                As long as this test pixel isn't out of range.
             */
            if (output.alpha() > transPixel) {

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

    public Polygon getSelectedPolygon(Point pos) {

        // Grab the first polygon detected to contains the Point::pos
        for (Polygon p : polyList) {
            if (p.contains(pos)) {
                return p;
            }
        }

        // Otherwise return nothing
        return null;
    }

    /**
     * Buffer those normal Images.
     *
     * @param source The Source Image
     * @param obs Typical Image Observer, you can just use 'null' usually.
     * @param type BufferedImage type.
     * @return The image with buffered capabilities.
     */
    public static BufferedImage bufferImage(Image source, ImageObserver obs, int type) {

        //
        int iWidth = source.getWidth(obs);
        int iHeight = source.getHeight(obs);

        // Try to force it this way
        if (iWidth <= 0 || iHeight <= 0) {
            final ImageIcon icon = new ImageIcon(source);
            iWidth = icon.getIconWidth();
            iHeight = icon.getIconHeight();
        }

        // Shell for the new BufferedImage
        final BufferedImage output = new BufferedImage(iWidth, iHeight, type);

        // Create the raster space
        final Graphics2D manet = output.createGraphics();

        // Draw the image and then Close it
        manet.drawImage(source, 0, 0, obs);
        manet.dispose();

        //
        return output;
    }

    /**
     * Removes pixels that would keep the tracer from completing it's cycle. Any
     * pixels that are just 1x1 in space or a little bigger or just hanging off
     * the edge awkwardly.
     *
     * @param img The source Image
     * @return the source image with hanging pixels removed.
     */
    private BufferedImage destroyAllIslands(BufferedImage img) {

        //
        for (int i = 0; i <= img.getWidth() - 1; i++) {

            //
            for (int j = 0; j <= img.getHeight() - 1; j++) {

                //
                int p = img.getRGB(i, j);
                int point = 0;

                // Question the pixel
                if (!isTransparent(p)) {
                    try {

                        // Current Pixel
                        if (isTransparent(img.getRGB(i, j))) {
                            point++;
                        }
                        // Pixel to the Right
                        if (isTransparent(img.getRGB(i + 1, j))) {
                            point++;
                        }
                        // Pixel Bottom Right
                        if (isTransparent(img.getRGB(i + 1, j + 1))) {
                            point++;
                        }
                        // Pixel to the Bottom
                        if (isTransparent(img.getRGB(i, j + 1))) {
                            point++;
                        }
                        // Pixel to the Bottom Left
                        if (isTransparent(img.getRGB(i - 1, j + 1))) {
                            point++;
                        }
                        // Pixel to the left.
                        if (isTransparent(img.getRGB(i - 1, j))) {
                            point++;
                        }
                        // Pixel Top Left
                        if (isTransparent(img.getRGB(i - 1, j - 1))) {
                            point++;
                        }
                        // Pixel Above
                        if (isTransparent(img.getRGB(i, j - 1))) {
                            point++;
                        }
                        // Pixel Top Right
                        if (isTransparent(img.getRGB(i + 1, j - 1))) {
                            point++;
                        }

                        // if 75% are invisible then remove the pixel
                        if (point >= 6) {
                            img.setRGB(i, j, COLOR_CLEAR);
                        }
                    } catch (ArrayIndexOutOfBoundsException aioobe) {
                        //
                    }
                }
            }
        }

        //
        return img;
    }

    /**
     * Clears the shape of the given polygon from the image
     *
     * @param img The source image
     * @param region The polygon shape to use
     * @return The source image with the pixels inside the bounds of the
     * polygon, removed.
     */
    private BufferedImage clearRegionFromImage(BufferedImage img, Polygon region) {

        //
        int max = 2;

        // GET RID OF IT.
        for (int i = 0; i <= img.getWidth()-1; i++) {

            // Going over every pixel.
            for (int j = 0; j <= img.getHeight()-1; j++) {

                // If the image has a point that the polygon has (inefficient i know; will optimize later)
                if (region.contains(i, j)) {

                    /* 
                        WE NEED THIS. It's going to clear those pesky pixels that aren't really visible
                        but the isTransparent will still detect them because our detection algorithm
                        isn't the best.
                     */
                    // It's an eraser with a size of 1-3.
                    for (int size = 0; size <= max; size++) {
                        try {

                            // Current Pixel
                            img.setRGB(i, j, COLOR_CLEAR);
                            // Pixel to the Right
                            img.setRGB(i + size, j, COLOR_CLEAR);
                            // Pixel Bottom Right
                            img.setRGB(i + size, j + size, COLOR_CLEAR);
                            // Pixel to the Bottom
                            img.setRGB(i, j + size, COLOR_CLEAR);
                            // Pixel to the Bottom Left
                            img.setRGB(i - size, j + size, COLOR_CLEAR);
                            // Pixel to the left.
                            img.setRGB(i - size, j, COLOR_CLEAR);
                            // Pixel Top Left
                            img.setRGB(i - size, j - size, COLOR_CLEAR);
                            // Pixel Above
                            img.setRGB(i, j - size, COLOR_CLEAR);
                            // Pixel Top Right
                            img.setRGB(i + size, j - size, COLOR_CLEAR);
                        } catch (ArrayIndexOutOfBoundsException aioobe) {
                            
                            // If it errors out then just stop.
                            break;
                            // @DEBUG
                            //System.out.println("Eraser Size: " + max);
                        }
                    }
                }
            }
        }

        // Return the new image.
        return img;
    }

    public Point[] getVerticalLineTrace(BufferedImage img, Point mouse) {

        //
        final Point[] point = new Point[2];

        //
        int y1, mx, my, width, height;

        // Used a lot so variables for them
        width = img.getWidth();
        height = img.getHeight();

        //
        mx = (mouse.x > width - 1 || mouse.x < 0) ? width - 1 : mouse.x;
        my = (mouse.y > height - 1 || mouse.y < 0) ? height - 1 : mouse.y;

        // Finding the top point.
        for (y1 = my; y1 >= 0; y1--) {

            // So essentially go up from the mouse.y until we hit transparency
            if (isTransparent(img.getRGB(mx, y1))) {
                point[0] = new Point(mx, y1);
                break;
            }

            if (y1 == 0 && !isTransparent(img.getRGB(mx, 0))) {
                point[0] = new Point(mx, 0);
            }
        }

        // If we found a top point; the alternate case is that the image is all color on the y-axis at x.
        if (point[0] != null) {

            //
            for (int y2 = (y1 + 1) > height - 1 ? height - 1 : y1 + 1; y2 <= height - 1; y2++) {

                // Now go down from y1 until we hit transparency again.
                if (isTransparent(img.getRGB(mx, y2))) {
                    point[1] = new Point(mx, y2);
                    break;
                }

                //
                if (y2 == height - 1 && !isTransparent(img.getRGB(mx, height - 1))) {
                    point[1] = new Point(mx, y2);
                }
            }
        }

        //
        return point;
    }

    public Point[] getHorizontalLineTrace(BufferedImage img, Point mouse) {

        //
        final Point[] point = new Point[2];

        //
        int x1, mx, my, width, height;

        //
        width = img.getWidth();
        height = img.getHeight();

        //
        mx = (mouse.x > width - 1 || mouse.x < 0) ? width - 1 : mouse.x;
        my = (mouse.y > height - 1 || mouse.y < 0) ? height - 1 : mouse.y;

        //
        for (x1 = mx; x1 >= 0; x1--) {

            //
            if (isTransparent(img.getRGB(x1, my))) {
                point[0] = new Point(x1, my);
                break;
            }

            //
            if (x1 == 0 && !isTransparent(img.getRGB(0, my))) {
                point[0] = new Point(0, my);
            }
        }

        // Going to the right.
        if (point[0] != null) {

            // Start at previous acquired point plus one if we can.
            for (int x2 = x1 + 1 > width - 1 ? width - 1 : x1 + 1; x2 <= width - 1; x2++) {

                //
                if (isTransparent(img.getRGB(x2, my))) {
                    point[1] = new Point(x2, my);
                    break;
                }

                // 
                if (x2 == width - 1 && !isTransparent(img.getRGB(0, my))) {
                    point[1] = new Point(width, my);
                }
            }
        }

        //
        return point;
    }

    // This pixel must be within the image bounds ; returns the rgb at point as well.
    private int testPixel(BufferedImage img, int x, int y) {
        return x <= img.getWidth() - 1 && y <= img.getHeight() - 1 && x >= 0 && y >= 0 ? img.getRGB(x, y) : 0;
    }

    // Checking if the test point is within bounds; convience method.
    private boolean isPixel(BufferedImage img, Point point) {
        return point.x <= img.getWidth() - 1 && point.y <= img.getHeight() - 1 && point.x >= 0 && point.y >= 0;
    }

    private boolean isTransparent(int testPixel) {
        return isTransparent(testPixel, transPixel);
    }

    private boolean isTransparent(int testPixel, int thresh) {
        return ((testPixel >> 24) & 0xFF) <= thresh;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isRunning() {
        return running;
    }

    public Point getCurrentPosition() {
        return curPos;
    }

    public BufferedImage getOriginalImage() {
        return origImage;
    }

    public BufferedImage getTraceImage() {
        return traceImage;
    }

    public BufferedImage getProgressImage() {
        return progImage;
    }

    // Our would-be export method.
    public ArrayList<Polygon> getPolygonList() {
        return polyList;
    }

    public String getMessage() {
        return message;
    }

    public int getTransparencyThreshold() {
        return transPixel;
    }

    public void setTransparencyThreshold(int transPixel) {
        this.transPixel = transPixel;
    }

    // Again another bastard nested class of mine.
    public class ColorPointPair {

        // Variable Declaration
        // Java Native Classes
        private final Point point;
        // Data Types
        private final int rgb;
        private final int red;
        private final int green;
        private final int blue;
        private final int alpha;
        // End of Variable Delcaration

        // Simply pairs an rgb code and a point together in an object.
        public ColorPointPair(Integer rgb, Point point) {
            this.rgb = rgb;
            this.point = point;

            // Parse out the ARGB values in case I want to turn this into a color chooser.
            alpha = ((rgb >> 24) & 0xFF);
            red = ((rgb >> 16) & 0xFF);
            green = ((rgb >> 8) & 0xFF);
            blue = ((rgb) & 0xFF);
        }

        public Point point() {
            return point;
        }

        public int rgb() {
            return rgb;
        }

        public int alpha() {
            return alpha;
        }

        public int red() {
            return red;
        }

        public int blue() {
            return blue;
        }

        public int green() {
            return green;
        }
    }
}
