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
    private final HashMap<Point, Integer> pointList;
    private BufferedImage origImage;
    private BufferedImage traceImage;
    private BufferedImage progImage;
    private Point curPos;
    private Point initPos;
    private Point previousPoint;
    private Point lastStartPoint;
    private Polygon poly;
    // Data Types
    private int maxX;
    private int yPixel;
    private int transPixel = 8;
    private String message = null;
    // Data Types
    private boolean started = false;
    private boolean finished = false;
    private int lastIndex = 0;
    // End of Variable Declaration

    public AlphaTracer(BufferedImage image) {

        // Clear certain pixels that cause issues.
        origImage = destroyAllIslands(image);
        traceImage = bufferImage(origImage, null, BufferedImage.TYPE_INT_ARGB);
        progImage = bufferImage(origImage, null, BufferedImage.TYPE_INT_ARGB);

        /*  
            So imagine a rectangle with a point in every 45degree angle
            We're going clockwise from 0/360 degrees(Directly Right) to 315(Top Right)
         */
        cppArray = new ColorPointPair[8];

        // Initializing.
        polyList = new ArrayList();
        pointList = new HashMap();
        poly = new Polygon();

        // Gives us our initial maxX and yPixel.
        updateStartPoint();

        // Initial point
        this.curPos = new Point(new Point(maxX, yPixel));
        this.initPos = new Point(curPos);
    }

    public void addVerticalLine(Point pos) {

        //
        final int clear = (new Color(0, 0, 0, 0)).getRGB();

        //lineList.add(getVerticalLineTrace(img, pos));
        if (pos.x >= 0 && pos.y >= 0 && pos.x <= traceImage.getWidth() - 1 && pos.y <= traceImage.getHeight() - 1) {

            //
            final Point[] points = getVerticalLineTrace(traceImage, pos);

            //
            if (points[0] != null && points[1] != null) {

                //
                final int distY = points[1].y - points[0].y;

                //
                for (int i = 0; i <= distY; i++) {

                    //
                    traceImage.setRGB(pos.x, points[0].y + i, clear);
                }

                //
                pointList.put(pos, 1);
            }
        }
    }

    public void addHorizontalLine(Point pos) {

        //
        final int clear = (new Color(0, 0, 0, 0)).getRGB();

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
                    traceImage.setRGB(points[0].x + i, pos.y, clear);
                }

                //
                pointList.put(pos, 0);
            }
        }
    }

    public void reset(BufferedImage image) {

        //
        origImage = destroyAllIslands(image);

        //
        pointList.clear();

        //
        reset();
    }

    public void reset() {

        //
        maxX = -1;
        yPixel = -1;
        lastIndex = 0;

        //
        finished = false;
        started = false;
        
        // Previous and Curpoint reset
        curPos = new Point();
        initPos = new Point();
        previousPoint = null;
        lastStartPoint = null;

        //
        message = null;

        //
        poly.reset();
        polyList.clear();

        //
        traceImage = bufferImage(origImage, null, BufferedImage.TYPE_INT_ARGB);
        progImage = bufferImage(origImage, null, BufferedImage.TYPE_INT_ARGB);

        // Re add those lines you made on the trace image.
        for (Map.Entry<Point, Integer> map : pointList.entrySet()) {
            switch (map.getValue()) {
                case 0:
                    addHorizontalLine(map.getKey());
                    break;
                case 1:
                    addVerticalLine(map.getKey());
                    break;
            }
        }
    }

    public void flash() {

        //
        reset();

        //
        updateStartPoint();

        //
        while (!finished) {
            step();
        }
    }

    public void step() {

        //
        ColorPointPair cpp = null;

        // Adding points to create the polygon and drawing those points on progImage.
        if (maxX > -1) {

            // Grab the next (first) point.
            cpp = getNextOpenSpace();

            // While the AlphaTracer isn't in no-man's-land.
            while (cpp != null) {

                // We use more than once so a variable.
                final Point point = cpp.point();

                // In the case it's stuck on a recurring point.
                if (previousPoint != null) {
                    if (previousPoint.x == maxX && previousPoint.y == yPixel) {
                        break;
                    }
                }

                //
                previousPoint = point;

                // Detects pixels that aren't transparent.
                if (!isTransparent(cpp.rgb())) {

                    // Next Chosen point
                    if (progImage != null) {

                        // Draw a blue dot at the next point on the in-progress image.
                        progImage.setRGB(point.x, point.y, (new Color(0, 0, 255, 255)).getRGB());
                    }

                    // Add the polygon point.
                    poly.addPoint(point.x, point.y);

                    // Then break out.
                    break;
                }

                // The previous point has been processed. So onto the next one.
                // This allows cpp to be null at some point and exit the while loop.
                cpp = getNextOpenSpace();
            }
        }

        // Solution for very small shapes that have jagged edges.
        if (cpp != null && started) {

            // First get current input for the pixel data at curPos.
            updatePixelSpace();

            // For each point around curPos
            for (ColorPointPair p : cppArray) {

                // If the distance is less than 1 and the polygon isn't at the start point.
                if (p.point.distance(initPos) <= 1 && poly.npoints > 2) {

                    // End the polygon.
                    cpp = null;
                    break;
                }
            }
        }

        // Signifies the end of a polygon.
        if (cpp == null) {

            // Add the start point because usually, if not always, it's not added at the beginning.
            if (!poly.contains(maxX, yPixel)) {
                poly.addPoint(maxX, yPixel);
            }

            /* 
                We only process polygons with more than 1 point; usually those get swept up by
                destroyAllIslands();
             */
            if (poly.npoints > 1) {
                // Add to the list of polygons
                polyList.add(poly);
            }

            /*
                Clear the pixels for that polygon from the image that so we don't process that region of the image again
                over and over.
             */
            traceImage = destroyAllIslands(clearRegionFromImage(traceImage, poly));

            // Clearing the in progress's image data and setting it to the trace image.
            progImage.flush();
            progImage = bufferImage(traceImage, null, BufferedImage.TYPE_INT_ARGB);

            // Onto a new polygon.
            poly = new Polygon();

            // Find the next start point.
            updateStartPoint();
        }
    }

    private void updateStartPoint() {

        // Store some values for use
        final int iWidth = traceImage.getWidth();
        final int iHeight = traceImage.getHeight();

        // Always reset.
        maxX = -1;
        yPixel = 0;

        // Go down y-coord and sweep left for first non-trans pixel then right for first non-trans pixel.
        for (yPixel = 0; yPixel <= iHeight - 1; yPixel++) {

            // Now from the other side
            for (int x = iWidth - 1; x >= 0; x--) {

                // If the current pixel is not transparent.
                //System.out.println("Trans@("+ x + "," + yPixel + "): " + ((traceImage.getRGB(x, yPixel) >> 24 & 0xFF)));
                if (!isTransparent(traceImage.getRGB(x, yPixel))) {

                    // Very important; without this variable we have nothing.
                    maxX = x;

                    //
                    curPos = new Point(maxX, yPixel);

                    //
                    if (lastStartPoint != null && lastStartPoint.equals(curPos)) {
                        message = "Failed to acquire Polygon. Try again.";
                        finished = true;
                        return;
                    }

                    //
                    initPos = new Point(maxX, yPixel);
                    lastStartPoint = new Point(maxX, yPixel);

                    // Reset started to false everytime we start looking for a new polygon
                    started = false;
                    lastIndex = 0;

                    // @DEBUG
                    System.out.println("New MaxX: " + maxX + " Y: " + yPixel);
                    // Return
                    return;
                }
            }

            // When we're at the end of the image and have nothing.
            if (yPixel >= iHeight - 1 && maxX <= 0) {
                finished = true;
                return;
            }
        }
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
                started = true;

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
                As long as this test pixel isn't out of bounds
                this also tests for pixels that have a rgb of 0(red), 0(blue), 0(green), 0(alpha),
                but not pixels with a value greater than 1 in any of those color / alpha ranges.
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

        // Clear color
        final int clear = (new Color(0, 0, 0, 0)).getRGB();

        //
        for (int i = 0; i < img.getWidth(); i++) {

            //
            for (int j = 0; j < img.getHeight(); j++) {

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
                            img.setRGB(i, j, clear);
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

        // Clear color
        final int clear = (new Color(0, 0, 0, 0)).getRGB();
        int max = 3;

        // GET RID OF IT.
        for (int i = 0; i < img.getWidth(); i++) {

            // Going over every pixel.
            for (int j = 0; j < img.getHeight(); j++) {

                // If the image has a point that the polygon has (inefficient i know; will optimize later)
                if (region.contains(i, j)) {

                    /* 
                        WE NEED THIS. It's going to clear those pesky pixels that aren't really visible
                        but the isTransparent will still detect them because our detection algorithm
                        isn't the best.
                     */
                    // It's an eraser with a size of 1-3.
                    for (int size = 1; size < max; size++) {
                        try {

                            // Current Pixel
                            img.setRGB(i, j, clear);
                            // Pixel to the Right
                            img.setRGB(i + size, j, clear);
                            // Pixel Bottom Right
                            img.setRGB(i + size, j + size, clear);
                            // Pixel to the Bottom
                            img.setRGB(i, j + size, clear);
                            // Pixel to the Bottom Left
                            img.setRGB(i - size, j + size, clear);
                            // Pixel to the left.
                            img.setRGB(i - size, j, clear);
                            // Pixel Top Left
                            img.setRGB(i - size, j - size, clear);
                            // Pixel Above
                            img.setRGB(i, j - size, clear);
                            // Pixel Top Right
                            img.setRGB(i + size, j - size, clear);
                        } catch (ArrayIndexOutOfBoundsException aioobe) {
                            max--;
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
        int y1, mx, my;

        //
        mx = (mouse.x > img.getWidth() - 1 || mouse.x < 0) ? img.getWidth() - 1 : mouse.x;
        my = (mouse.y > img.getHeight() - 1 || mouse.y < 0) ? img.getHeight() - 1 : mouse.y;

        // Finding the top point.
        for (y1 = my; y1 >= 0; y1--) {

            // So essential go up from the mouse.y until we hit transparency
            if (isTransparent(img.getRGB(mx, y1))) {
                point[0] = new Point(mx, y1);
                break;
            }
        }

        // If we found a top point; the alternate case is that the image is all color on the y-axis at x.
        if (point[0] != null) {

            //
            for (int y2 = (y1 + 1) > img.getHeight() - 1 ? img.getHeight() - 1 : y1 + 1; y2 <= img.getHeight() - 1; y2++) {

                // Now go down from y1 until we hit transparency again.
                if (isTransparent(img.getRGB(mx, y2))) {
                    point[1] = new Point(mx, y2);
                    break;
                }

                //
                if (y2 == img.getHeight() - 1) {
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
        int x1, mx, my;

        //
        mx = (mouse.x > img.getWidth() - 1 || mouse.x < 0) ? img.getWidth() - 1 : mouse.x;
        my = (mouse.y > img.getHeight() - 1 || mouse.y < 0) ? img.getHeight() - 1 : mouse.y;

        //
        for (x1 = mx; x1 >= 0; x1--) {

            //
            if (isTransparent(img.getRGB(x1, my))) {
                point[0] = new Point(x1, my);
                break;
            }
        }
        
        //
        if (point[0] != null) {
            
            //
            for (int x2 = x1 + 1 > img.getWidth() - 1 ? img.getWidth() - 1 : x1 + 1; x2 <= img.getWidth() - 1; x2++) {
                if (isTransparent(img.getRGB(x2, my))) {
                    point[1] = new Point(x2, my);
                    break;
                }
            }
        }

        //
        return point;
    }

    // This pixel must be within the image bounds ; returns the rgb at point as well.
    private int testPixel(BufferedImage img, int x, int y) {
        return x <= img.getWidth()-1 && y <= img.getHeight()-1 && x >= 0 && y >= 0 ? img.getRGB(x, y) : 0;
    }

    // Checking if the test point is within bounds; convience method.
    private boolean isPixel(BufferedImage img, Point point) {
        return point.x <= img.getWidth()-1 && point.y <= img.getHeight()-1 && point.x >= 0 && point.y >= 0;
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

    public boolean hasStarted() {
        return started;
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
        private final int alpha;
        // End of Variable Delcaration

        // Simply pairs an rgb code and a point together in an object.
        public ColorPointPair(Integer rgb, Point point) {
            this.rgb = rgb;
            this.point = point;

            //
            alpha = ((rgb >> 24) & 0xFF);
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
    }
}
