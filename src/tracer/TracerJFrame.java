/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tracer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import tracer.ImageTracer.ColorPointPair;

/**
 *
 * @author Robert A. Cherry (MessiahWolf@gmail.com) You can use or sample if you
 * give original credit to me and don't distribute for monetary gains. Make sure
 * they give the credit to me. MUHAHAHAH! In memory of Manny.
 */
public class TracerJFrame extends javax.swing.JFrame implements ActionListener {

    // Variable Declaration
    // Project Classes
    private ImageTracer tracer;
    // Java Swing Classes
    private JPanel panel;
    private Timer timer;
    // Java Native Classes
    private ArrayList<Polygon> polyList;
    private BufferedImage origImage;
    private BufferedImage traceImage;
    private BufferedImage progImage;
    private Image imageFrame16;
    private Image imageFrame32;
    private ImageIcon iconContinuous;
    private ImageIcon iconPause;
    private ImageIcon iconPlay;
    private ImageIcon iconPolygon;
    private ImageIcon iconRefresh;
    private ImageIcon iconRefreshOver;
    private ImageIcon iconSingle;
    private Polygon poly;
    // Data Types
    private int maxX;
    private int yPixel;
    private int transPixel;
    // End of Variable Declaration

    public static void main(String[] args) {
        TracerJFrame main = new TracerJFrame();
        main.setVisible(true);
    }

    public TracerJFrame() {

        // Attempt to set the look and feel of the application
        try {

            // Set to native look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            //UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException cnfe) {
            System.err.println(cnfe);
        }

        //
        initComponents();
        init();

        //
        importSamples();
    }

    private void init() {

        // Our rendering panel.
        panel = new JPanel() {
            @Override
            public void paint(Graphics monet) {

                // Cast to 2D for easier polygon rendering.
                final Graphics2D manet = (Graphics2D) monet;

                // Draw the image under
                manet.drawImage(progImage == null ? traceImage : progImage, 0, 0, this);
                manet.setColor(Color.BLACK);

                // Draw those polygons over.
                for (Polygon poly : polyList) {
                    manet.fill(poly);
                }
            }
        };

        // The timer; can set as low as you want.
        timer = new Timer(19, this);

        // Initializing.
        polyList = new ArrayList();
        poly = new Polygon();

        // My probably inefficient way of getting images that will soon be deprecated.
        final Toolkit kit = Toolkit.getDefaultToolkit();
        final Class closs = getClass();

        // Original.
        origImage = copyImage(kit.createImage(closs.getResource("/samples/example2.png")), this, BufferedImage.TYPE_INT_ARGB);

        // Icons.
        iconContinuous = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-continuous24.png")));
        iconPolygon = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-body24.png")));
        iconPlay = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-play24.png")));
        iconPause = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-pause24.png")));
        iconRefresh = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-refresh24.png")));
        iconRefreshOver = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-refresh-roll24.png")));
        iconSingle = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-single24.png")));
        imageFrame16 = kit.createImage(closs.getResource("/icons/icon-frame16.png"));
        imageFrame32 = kit.createImage(closs.getResource("/icons/icon-frame32.png"));
        
        // Our working copy.
        traceImage = copyImage(origImage, this, BufferedImage.TYPE_INT_ARGB);

        // The magic itself.
        tracer = new ImageTracer(traceImage);
        
        //
        final ArrayList<Image> frameList = new ArrayList();
        frameList.add(imageFrame16);
        frameList.add(imageFrame32);
        
        // Change frame images.
        setIconImages(frameList);

        // Icon setting
        nextJButton.setIcon(iconPlay);
        nextJButton.setEnabled(false);
        acquireJButton.setIcon(iconPolygon);
        refreshJButton.setIcon(iconRefreshOver);
        refreshJButton.setRolloverIcon(iconRefresh);
        contJToggle.setSelectedIcon(iconContinuous);
        contJToggle.setIcon(iconSingle);

        // Setting up the viewport for scrolling if nessecary.
        mainJScrollPane.setViewportView(panel);

        // Set the title
        setTitle("Image Tracer");
    }

    private void importSamples() {

        //
        final Toolkit kit = Toolkit.getDefaultToolkit();
        final Class closs = getClass();

        //
        final File directory = new File(closs.getResource("/samples").getFile());

        //
        final File[] files = directory.listFiles();

        // If it's there.
        if (files != null) {

            // Max of eight samples for now. I know wrong way of ever implementing 
            // something like this.
            for (int i = 0; i < (files.length > 8 ? 8 : files.length); i++) {

                //
                final File file = files[i];

                // Null check
                if (file == null) {
                    break;
                }

                // Question it first
                if (file.getAbsolutePath().toLowerCase().endsWith(".png")) {

                    //
                    final BufferedImage image = copyImage(kit.createImage(file.getAbsolutePath()), this, BufferedImage.TYPE_INT_ARGB);

                    // Attempt to create image from it.
                    final ImageButton button = new ImageButton(this, image, file.getName());

                    //
                    button.setPreferredSize(new Dimension(32, 32));
                    button.setMaximumSize(new Dimension(32, 32));
                    button.setMinimumSize(new Dimension(32, 32));

                    //
                    imageJPanel.add(button);
                }
            }
        }
    }

    private int findStartPoint() {

        // Store some values for use
        final int iWidth = traceImage.getWidth(this);
        final int iHeight = traceImage.getHeight(this);

        // Always reset.
        maxX = -1;
        yPixel = 0;

        // Go down y-coord and sweep left for first non-trans pixel then right for first non-trans pixel.
        for (yPixel = 0; yPixel < iHeight - 1; yPixel++) {

            // Now from the other side
            for (int x = iWidth - 1; x >= 0; x--) {

                // If the current pixel is not transparent.
                if (!isTransparent(traceImage.getRGB(x, yPixel))) {

                    // Very important; without this variable we have nothing.
                    maxX = x;

                    // The magic.
                    tracer = new ImageTracer(traceImage);
                    tracer.init(new Point(maxX, yPixel));

                    // @DEBUG
                    //System.out.println("New MaxX: " + maxX + " Y: " + yPixel);
                    // Return
                    return maxX;
                }
            }

            // When we're at the end of the image and have nothing.
            if (yPixel >= iHeight - 1 && maxX <= 0) {
                return -2;
            }
        }

        // @DEBUG
//        if (maxX == -1) {
//            System.err.println("Image must be clear.");
//        }
        return maxX;
    }

    // Method to remove all single pixels with nothing around them
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

    private void changeImage(BufferedImage image) {
        
        // Erase the current inprogress polygon
        poly.reset();
        
        // Stop the timer
        timer.stop();

        //
        refreshJButton.setEnabled(true);
        acquireJButton.setEnabled(false);
        nextJButton.setEnabled(false);

        // Find and set.
        origImage = copyImage(image, this, BufferedImage.TYPE_INT_ARGB);

        //
        traceImage = copyImage(origImage, this, BufferedImage.TYPE_INT_ARGB);
        progImage = copyImage(origImage, this, BufferedImage.TYPE_INT_ARGB);

        //
        tracer = new ImageTracer(traceImage);

        //
        polyList.clear();

        //
        repaint();
    }

    private BufferedImage copyImage(Image source, ImageObserver obs, int type) {

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

    // Tests if the pixel has transparency of 3 or less.
    private boolean isTransparent(int testPixel) {
        return ((testPixel >> 24) & 0xFF) <= transPixel;
    }

    @Override
    public void actionPerformed(ActionEvent evt) {

        //
        doWork();
    }

    private void doWork() {

        //
        if (maxX > -1) {

            // Grab the next (first) point.
            ColorPointPair cpp = tracer.getNextOpenSpace();

            // While the ImageTracer isn't in no-man's-land.
            while (cpp != null) {

                // We use more than once so a variable.
                final Point point = cpp.point();

                // This will be in the ImageTracer soon.
                if (!isTransparent(cpp.rgb())) {

                    // Next Chosen point
                    if (progImage != null) {

                        // Draw a blue dot at the next point on the in-progress image.
                        progImage.setRGB(point.x, point.y, (new Color(0, 0, 255, 255)).getRGB());
                    }

                    // Add the polygon point.
                    poly.addPoint(point.x, point.y);

                    // Paint god dammit.
                    repaint();

                    // Then break out.
                    break;
                }

                // Go after.
                cpp = tracer.getNextOpenSpace();
            }
        }

        // This signifies the end of a polygon.
        if (tracer.getNextOpenSpace() == null) {

            // Add the start point because usually, if not always, it's not added at the beginning.
            if (!poly.contains(maxX, yPixel)) {
                poly.addPoint(maxX, yPixel);
            }

            // Add to the list of polygons
            polyList.add(poly);

            /*
                Clear the pixels for that polygon from the image that so we don't process that region of the image again
                over and over.
             */
            traceImage = clearRegionFromImage(traceImage, poly);
            progImage.flush();
            progImage = copyImage(traceImage, this, BufferedImage.TYPE_INT_ARGB);

            // Our end result we've strived so desperately for.
            poly = new Polygon();

            // Attempt to find the next polygon by getting the new maxX.
            final int result = findStartPoint();

            // Repaint for the people of Tamriel.
            repaint();

            // -1 means this line might be transparent, but -2
            // means we're at the end of the image and nothing is found.
            // @RECURSION
            if (contJToggle.isSelected()) {

                // If we potentially have more to go; keep going as long as the toggle is selected.
                if (result < 0) {

                    //
                    timer.stop();
                    nextJButton.setEnabled(false);
                    nextJButton.setIcon(iconPlay);

                    //
                    acquireJButton.setEnabled(false);
                    refreshJButton.setEnabled(true);
                }
            } else {

                //
                //System.err.println("Result(MaxX): " + maxX + " Y: " + yPixel);
                // Stop the timer and disable the nextJButton; we have nothing else to find.
                timer.stop();
                nextJButton.setEnabled(result > -1);
                nextJButton.setIcon(iconPlay);

                //
                acquireJButton.setEnabled(result > -1);
                refreshJButton.setEnabled(true);
            }

            // Update JLabel
            polygonJLabel.setText("Polygons: " + polyList.size());
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainJScrollPane = new javax.swing.JScrollPane();
        buttonJPanel = new javax.swing.JPanel();
        acquireJButton = new javax.swing.JButton();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        contJToggle = new javax.swing.JToggleButton();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        refreshJButton = new javax.swing.JButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        polygonJLabel = new javax.swing.JLabel();
        filler4 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        nextJButton = new javax.swing.JButton();
        transJSlider = new javax.swing.JSlider();
        imageJPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(306, 388));
        setResizable(false);

        mainJScrollPane.setToolTipText("");
        mainJScrollPane.setMaximumSize(new java.awt.Dimension(238, 202));
        mainJScrollPane.setMinimumSize(new java.awt.Dimension(238, 202));
        mainJScrollPane.setPreferredSize(new java.awt.Dimension(238, 202));

        buttonJPanel.setLayout(new javax.swing.BoxLayout(buttonJPanel, javax.swing.BoxLayout.LINE_AXIS));

        acquireJButton.setToolTipText("Acquire Polygon");
        acquireJButton.setMaximumSize(new java.awt.Dimension(24, 24));
        acquireJButton.setMinimumSize(new java.awt.Dimension(24, 24));
        acquireJButton.setPreferredSize(new java.awt.Dimension(24, 24));
        acquireJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                acquireJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(acquireJButton);
        buttonJPanel.add(filler2);

        contJToggle.setToolTipText("Capture Single Polygon");
        contJToggle.setMaximumSize(new java.awt.Dimension(24, 24));
        contJToggle.setMinimumSize(new java.awt.Dimension(24, 24));
        contJToggle.setPreferredSize(new java.awt.Dimension(24, 24));
        contJToggle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contJToggleActionPerformed(evt);
            }
        });
        buttonJPanel.add(contJToggle);
        buttonJPanel.add(filler3);

        refreshJButton.setToolTipText("Reset Image");
        refreshJButton.setMaximumSize(new java.awt.Dimension(24, 24));
        refreshJButton.setMinimumSize(new java.awt.Dimension(24, 24));
        refreshJButton.setPreferredSize(new java.awt.Dimension(24, 24));
        refreshJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(refreshJButton);
        buttonJPanel.add(filler1);

        polygonJLabel.setText("Polygons:");
        polygonJLabel.setMaximumSize(new java.awt.Dimension(72, 24));
        polygonJLabel.setMinimumSize(new java.awt.Dimension(72, 24));
        polygonJLabel.setPreferredSize(new java.awt.Dimension(72, 24));
        buttonJPanel.add(polygonJLabel);
        buttonJPanel.add(filler4);

        nextJButton.setToolTipText("Polygonize (Visual)");
        nextJButton.setMaximumSize(new java.awt.Dimension(24, 24));
        nextJButton.setMinimumSize(new java.awt.Dimension(24, 24));
        nextJButton.setPreferredSize(new java.awt.Dimension(24, 24));
        nextJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(nextJButton);

        transJSlider.setToolTipText("Transparency Threshold");
        transJSlider.setValue(3);
        transJSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                transJSliderStateChanged(evt);
            }
        });

        imageJPanel.setPreferredSize(new java.awt.Dimension(32, 202));
        imageJPanel.setLayout(new javax.swing.BoxLayout(imageJPanel, javax.swing.BoxLayout.PAGE_AXIS));

        jLabel1.setText("Transparency:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(buttonJPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(mainJScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(imageJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(transJSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(imageJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(mainJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(transJSlider, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(buttonJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void nextJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextJButtonActionPerformed
        // DO IT!!!
        if (timer.isRunning()) {
            timer.stop();
            nextJButton.setIcon(iconPlay);
        } else {
            timer.start();
            nextJButton.setIcon(iconPause);
            refreshJButton.setEnabled(false);
            acquireJButton.setEnabled(false);
        }

        //
        repaint();
    }//GEN-LAST:event_nextJButtonActionPerformed

    private void acquireJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_acquireJButtonActionPerformed
        // Null case
        if (traceImage == null) {
            return;
        }

        //
        traceImage = destroyAllIslands(traceImage);

        // To show the progress; only reason we have it.
        progImage = copyImage(traceImage, this, BufferedImage.TYPE_INT_ARGB);

        // Get the new maxX and yPixel; those are the only two we care about.
        final int result = findStartPoint();

        // Updating controls.
        polygonJLabel.setText("Polygons: " + polyList.size());

        // More control updating
        if (result > -1) {

            // Once we have points enable the button.
            nextJButton.setEnabled(true);
        }

        // DO IT!!!
        if (timer.isRunning()) {
            timer.stop();
            nextJButton.setIcon(iconPlay);
        }

        // Paint damn you.
        repaint();
    }//GEN-LAST:event_acquireJButtonActionPerformed

    private void transJSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_transJSliderStateChanged
        // TODO add your handling code here:
        if (!transJSlider.getValueIsAdjusting()) {
            transPixel = transJSlider.getValue();
        }
    }//GEN-LAST:event_transJSliderStateChanged

    private void contJToggleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contJToggleActionPerformed
        //
        contJToggle.setToolTipText(contJToggle.isSelected() ? "Continuously Capture" : "Capture Single Polygon");
    }//GEN-LAST:event_contJToggleActionPerformed

    private void refreshJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshJButtonActionPerformed
        // Stop the timer.
        timer.stop();

        // Return the test image to the example.
        progImage = null;
        traceImage = copyImage(origImage, this, BufferedImage.TYPE_INT_ARGB);

        // Clear the polygons.
        polyList.clear();

        // Update buttons.
        nextJButton.setIcon(iconPlay);
        nextJButton.setEnabled(false);
        acquireJButton.setEnabled(true);

        // Update JLabel
        polygonJLabel.setText("Polygons: " + polyList.size());

        // Repaint.
        repaint();
    }//GEN-LAST:event_refreshJButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton acquireJButton;
    private javax.swing.JPanel buttonJPanel;
    private javax.swing.JToggleButton contJToggle;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.Box.Filler filler4;
    private javax.swing.JPanel imageJPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane mainJScrollPane;
    private javax.swing.JButton nextJButton;
    private javax.swing.JLabel polygonJLabel;
    private javax.swing.JButton refreshJButton;
    private javax.swing.JSlider transJSlider;
    // End of variables declaration//GEN-END:variables

    private class ImageButton extends JButton implements ActionListener {

        // Variable Declaration
        // Project Classes
        private final TracerJFrame frame;
        // Java Native Classes
        private final BufferedImage image;
        // End of Variable Declaration

        public ImageButton(TracerJFrame frame, BufferedImage image, String text) {

            //
            super();

            //
            this.frame = frame;
            this.image = image;
            
            //
            super.setToolTipText(text);

            //
            init();
        }

        private void init() {

            //
            final BufferedImage bi = new BufferedImage(28, 28, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D manet = bi.createGraphics();
            manet.setRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
            manet.drawImage(image, 0, 0, 28, 28, null);
            manet.dispose();

            //
            setIcon(new ImageIcon(bi));
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            frame.changeImage(image);
        }
    }
}
