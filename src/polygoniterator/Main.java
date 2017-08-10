/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package polygoniterator;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;
import polygoniterator.PIterator.ColorPointPair;

/**
 *
 * @author Robert A. Cherry (MessiahWolf@gmail.com) You can use or sample if you
 * give original credit to me and don't distribute for monetary gains. Make sure
 * they give the credit to me. MUHAHAHAH!
 */
public class Main extends javax.swing.JFrame implements ActionListener {

    // Variable Declaration
    // Project Classes
    private PIterator nc;
    // Java Swing Classes
    private JPanel panel;
    private Timer timer;
    // Java Native Classes
    private BufferedImage example, test, progress;
    private Polygon poly;
    // Data Types
    private int maxX, yPixel;
    private ArrayList<Polygon> list;
    // End of Variable Declaration

    public static void main(String[] args) {
        Main main = new Main();
        main.setVisible(true);
    }

    public Main() {
        initComponents();
        init();
    }

    private void init() {

        // Our rendering panel.
        panel = new JPanel() {
            @Override
            public void paint(Graphics monet) {

                // Cast to 2D for easier polygon rendering.
                final Graphics2D manet = (Graphics2D) monet;

                // Draw the image under
                manet.drawImage(progress == null ? test : progress, 0, 0, this);
                manet.setColor(Color.BLACK);

                // Draw those polygons over.
                for (Polygon poly : list) {
                    manet.fill(poly);
                }
            }
        };

        // The timer; can set as low as you want.
        timer = new Timer(13, this);

        // Initializing.
        list = new ArrayList();
        poly = new Polygon();

        // My probably inefficient way of getting images that will soon be deprecated.
        final Toolkit kit = Toolkit.getDefaultToolkit();
        final Class closs = getClass();

        // Original.
        example = copyImage(kit.createImage(closs.getResource("mwolf2.png")), this, BufferedImage.TYPE_INT_ARGB);

        // Our working copy.
        test = copyImage(example, this, BufferedImage.TYPE_INT_ARGB);

        // The magic itself.
        nc = new PIterator(test);

        // Setting up the viewport for scrolling if nessecary.
        mainJScrollPane.setViewportView(panel);

        // Set the title
        setTitle("Polygons from Image");
    }

    private void acquirePolygon() {

        // Store some values for use
        final int iWidth = test.getWidth(this);
        final int iHeight = test.getHeight(this);

        // Only need this so we don't set maxX too low.
        int minX = -1;

        // Always reset.
        maxX = -1;
        yPixel = 0;

        // Go down y-coord and sweep left for first non-trans pixel then right for first non-trans pixel.
        for (yPixel = 0; yPixel < iHeight - 1; yPixel++) {

            // Now we're going to search for min and max width values
            // Initially in this example we're solely using transparent pixels
            // but we can bit-wise push to account for any user selected color.
            // Go from the left first then stop at first not transparent pixel
            for (int xPixel = 0; xPixel < iWidth; xPixel++) {

                // If the current pixel is not transparent.
                if (!isTransparent(test.getRGB(xPixel, yPixel))) {
                    minX = xPixel;
                    break;
                }
            }

            // Now from the other side
            for (int xPixel = iWidth - 1; xPixel >= 0; xPixel--) {

                // If the current pixel is not transparent.
                if (!isTransparent(test.getRGB(xPixel, yPixel))) {

                    // Very important; without this variable we have nothing.
                    maxX = xPixel;

                    // The magic.
                    nc = new PIterator(test);
                    nc.init(new Point(maxX, yPixel));

                    // @DEBUG
                    //System.out.println("New MaxX: " + maxX + " Y: " + yPixel);
                    // Break
                    break;
                }
            }

            // Might save some frames.
            if (minX > -1 && maxX > -1) {
                break;
            }
        }

        // @DEBUG
//        if (maxX == -1) {
//            System.err.println("Image must be clear.");
//        }
    }

    private BufferedImage clearRegionFromImage(BufferedImage img, Polygon region) {

        // GET RID OF IT.
        for (int i = 0; i < img.getWidth(); i++) {

            // Going over every pixel.
            for (int j = 0; j < img.getHeight(); j++) {

                // If the image has a point that the polygon has (inefficient i know; will optimize later)
                if (region.contains(i, j)) {

                    // Clear color
                    final int clear = (new Color(0, 0, 0, 0)).getRGB();

                    /// WE NEED THIS. It's going to clear those pesky pixels that aren't really visible
                    // but the isTransparent will still detect them.
                    int max=3;
                    
                    // It's an eraser with a size of 3-to-1.
                    for (int size = 0; size < max; size++) {
                        try {
                            img.setRGB(i, j, clear);
                            img.setRGB(i - size, j, clear);
                            img.setRGB(i, j - size, clear);
                            img.setRGB(i - size, j - size, clear);
                            img.setRGB(i + size, j - size, clear);
                            img.setRGB(i + size, j, clear);
                            img.setRGB(i + size, j + size, clear);
                            img.setRGB(i, j + size, clear);
                            img.setRGB(i - 1, j + 1, clear);
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

    private BufferedImage copyImage(Image image, ImageObserver obs, int type) {

        //
        int imageWidth = image.getWidth(obs);
        int imageHeight = image.getHeight(obs);

        // Try to force it this way
        if (imageWidth <= 0 || imageHeight <= 0) {
            final ImageIcon icon = new ImageIcon(image);
            imageWidth = icon.getIconWidth();
            imageHeight = icon.getIconHeight();
        }

        // Shell for the new BufferedImage
        final BufferedImage bufferedImage = new BufferedImage(imageWidth, imageHeight, type);

        // Create the raster space
        final Graphics2D manet = bufferedImage.createGraphics();

        // Draw the image and then Close it
        manet.drawImage(image, 0, 0, obs);
        manet.dispose();

        //
        return bufferedImage;
    }

    // Tests if the pixel has transparency of 3 or less.
    private boolean isTransparent(int testPixel) {
        return ((testPixel >> 24) & 0xFF) <= 3;
    }

    @Override
    public void actionPerformed(ActionEvent evt) {

        // Gut reaction to put this here, we might not need it.
        if (progress == null) {
            return;
        }

        //
        if (maxX > -1) {

            // Grab the next (first) point.
            ColorPointPair p = nc.getNextOpenSpace();

            // While the PIterator isn't in no-man's-land.
            while (nc.hasMore() && p != null) {

                // This will be in the PIterator soon.
                if (!isTransparent(p.rgb())) {

                    // Next Chosen point
                    progress.setRGB(p.point().x, p.point().y, (new Color(0, 0, 0, 255)).getRGB());
                    poly.addPoint(p.point().x, p.point().y);

                    // @DEBUG
                    //System.out.println("Added Point: " + p.point());
                    // Paint god dammit.
                    repaint();

                    // Then break out.
                    break;
                }

                // Go after.
                p = nc.getNextOpenSpace();
            }
        }

        // We're not detecting anymore new pixels so we're done.
        // It's a polygon now free to roam the world.
        if (nc.getNextOpenSpace() == null) {

            // Add to the list of polygons
            list.add(poly);

            // Clear that so we don't process that region of the image again.
            progress = null;
            test = clearRegionFromImage(test, poly);

            // Our end result we've strived so desperately for.
            poly = new Polygon();

            // Stop the timer until we set new minX and maxX
            timer.stop();

            // Repaint for the people of Tamriel.
            repaint();
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
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        nextJButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        buttonJPanel.setLayout(new javax.swing.BoxLayout(buttonJPanel, javax.swing.BoxLayout.LINE_AXIS));

        acquireJButton.setText("Get Points");
        acquireJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                acquireJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(acquireJButton);
        buttonJPanel.add(filler1);

        nextJButton.setText("Plot");
        nextJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(nextJButton);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(mainJScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 284, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(buttonJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mainJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
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
        } else {
            timer.start();
        }
    }//GEN-LAST:event_nextJButtonActionPerformed

    private void acquireJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_acquireJButtonActionPerformed
        // Null case
        if (test == null) {
            return;
        }

        // To show the progress; only reason we have it.
        progress = copyImage(test, this, BufferedImage.TYPE_INT_ARGB);

        // Get the new maxX and yPixel; those are the only two we care about.
        acquirePolygon();

        // Paint damn you.
        repaint();
    }//GEN-LAST:event_acquireJButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton acquireJButton;
    private javax.swing.JPanel buttonJPanel;
    private javax.swing.Box.Filler filler1;
    private javax.swing.JScrollPane mainJScrollPane;
    private javax.swing.JButton nextJButton;
    // End of variables declaration//GEN-END:variables
}
