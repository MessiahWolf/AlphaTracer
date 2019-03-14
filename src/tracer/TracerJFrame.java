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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

/**
 *
 * @author Robert Cherry (MessiahWolf)
 */
public class TracerJFrame extends javax.swing.JFrame implements ActionListener {

    // Variable Declaration
    // Project Classes
    private FormTracer tracer;
    // Java Swing Classes
    private JPanel panel;
    private Timer timer;
    private Point mousePos;
    // Java Native Classes
    private BufferedImage selImage;
    private Color textileBackground = Color.LIGHT_GRAY;
    private Color textileForeground = Color.WHITE;
    private ImageIcon iconPause;
    private ImageIcon iconPlay;
    private ImageIcon iconError;
    private ImageIcon iconTrackOn;
    private ImageIcon iconTrackOff;
    // Data Types
    private boolean hoverHorizontal = false;
    private boolean hoverVertical = false;
    private final int panelZoomMax = 5;
    private int panelZoom = 1;
    private int lastXSector;
    private int lastYSector;
    private int playSpeed = 10;
    private final int playDelayMax = 1000;
    private final int playDelayMin = 10;
    // End of Variable Declaration

    public static void main(String[] args) {
        final TracerJFrame main = new TracerJFrame();
        main.setTitle("Image Tracer (Robert Cherry)");
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

        //
        importSamples();

        //
        init();

        /* 
         @TODO Change destroy islands to a proximity to cursor, not entire panel.
         */
    }

    private void init() {

        // Our rendering panel.
        panel = new JPanel() {
            @Override
            public void paint(Graphics monet) {

                // Cast to 2D for easier polygon rendering.
                final Graphics2D manet = (Graphics2D) monet;

                //
                final Point zoomPos = new Point(mousePos.x / panelZoom, mousePos.y / panelZoom);

                // Draw textile background for whatever reason. Don't know if i completely
                // enjoy the look it gives.
                drawTextileBackground(manet);

                //
                manet.scale(panelZoom, panelZoom);

                //
                if (tracer != null) {

                    //
                    final BufferedImage ti = tracer.getTraceImage();
                    final BufferedImage pi = tracer.getProgressImage();

                    // Draw the image under
                    manet.drawImage(tracer.isFinished() || !tracer.isRunning() ? ti : pi, 0, 0, this);
                    manet.setColor(Color.BLACK);

                    // Draw those polygons over.
                    for (Polygon poly : tracer.getPolygonList()) {

                        //
                        if (panelZoom > 1 && poly.contains(zoomPos)) {
                            manet.fill(poly);
                        } else if (panelZoom == 1 && poly.contains(mousePos)) {
                            manet.fill(poly);
                        } else {
                            manet.draw(poly);
                        }
                    }

                    // If the Horizontal or Vertical Cut Tool is selected.
                    if (hoverHorizontal || hoverVertical) {

                        // Adjust that point for Zoom
                        // Hover line
                        final Point[] points = hoverHorizontal ? tracer.getHorizontalLineTrace(ti, zoomPos) : tracer.getVerticalLineTrace(ti, zoomPos);
                        manet.setColor(Color.BLACK);

                        // Drawing the line to indicate the cut
                        if (points[0] != null && points[1] != null) {

                            //
                            if (points[0].distance(points[1]) > 1) {
                                //
                                if (hoverHorizontal) {
                                    manet.drawLine(points[0].x, points[0].y, points[1].x, points[0].y);
                                } else if (hoverVertical) {
                                    manet.drawLine(points[0].x, points[0].y, points[0].x, points[1].y);
                                }

                                // If either is true.
                                manet.setColor(Color.BLACK);
                                manet.fillOval(points[0].x - 2, points[0].y - 2, 4, 4);
                                manet.fillOval(points[1].x - 2, points[1].y - 2, 4, 4);
                            }
                        }
                    }

                    // Conditional
                    if (tracer.isRunning()) {
                        
                        //
                        final int pntX = tracer.getCurrentX();
                        final int pntY = tracer.getCurrentY();

                        //
                        manet.setColor(Color.RED);
                        manet.fillOval(pntX, pntY, 2, 2);
                    }
                }

                //
                manet.scale(1f, 1f);
            }
        };
        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent evt) {
                mousePos = evt.getPoint();
                repaint();
            }
        });
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getButton() == MouseEvent.BUTTON1) {

                    // Adjust point for zoom
                    Point zoomPos = new Point(evt.getPoint().x / panelZoom, evt.getPoint().y / panelZoom);

                    //
                    if (hoverHorizontal) {
                        tracer.addHorizontalLine(zoomPos);
                        hoverVertical = false;
                    } else if (hoverVertical) {
                        tracer.addVerticalLine(zoomPos);
                        hoverHorizontal = false;
                    }
                } else if (evt.getButton() == MouseEvent.BUTTON3) {
                    hoverVertical = false;
                    hoverHorizontal = false;

                    // When hovering and you right click we can return the pixels
                    // that were cleared with left click -- soon.
                }

                //
                repaint();
            }
        });

        // The timer; can set as low as you want.
        timer = new Timer(playSpeed, this);
        mousePos = new Point();

        // My probably inefficient way of getting images that will soon be deprecated.
        final Toolkit kit = Toolkit.getDefaultToolkit();
        final Class closs = getClass();

        try {
            // Icons.
            iconError = new ImageIcon(ImageIO.read(closs.getResource("/icons/icon-error16.png")));
            iconPlay = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-play24.png")));
            iconPause = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-pause24.png")));
            final ImageIcon iconRefresh = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-refresh24.png")));
            final ImageIcon iconRefreshOver = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-refresh-roll24.png")));
            final ImageIcon iconImport = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-import24.png")));
            final ImageIcon iconImportOver = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-import-roll24.png")));
            final ImageIcon iconHorizontal = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-line-horizontal24.png")));
            final ImageIcon iconVertical = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-line-vertical24.png")));
            final ImageIcon iconZoomIn = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-zoom-in24.png")));
            final ImageIcon iconZoomOut = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-zoom-out24.png")));
            final ImageIcon iconForward = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-fast-forward24.png")));
            final ImageIcon iconReverse = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-fast-reverse24.png")));
            final ImageIcon iconStop = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-stop24.png")));
            final ImageIcon iconPNG = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-png12.png")));
            final ImageIcon iconCamera = new ImageIcon(ImageIO.read(closs.getResource("/icons/icon-camera24.png")));
            iconTrackOn = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-track16.png")));
            iconTrackOff = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-track-off16.png")));
            final Image imageFrame16 = kit.createImage(closs.getResource("/icons/icon-frame16.png"));
            final Image imageFrame32 = kit.createImage(closs.getResource("/icons/icon-frame32.png"));

            // The magic itself.
            tracer = new FormTracer(selImage);

            //
            final ArrayList<Image> frameList = new ArrayList();
            frameList.add(imageFrame16);
            frameList.add(imageFrame32);

            // Change frame images.
            setIconImages(frameList);

            // Icon setting
            playJButton.setIcon(iconPlay);
            playJButton.requestFocus();
            resetJButton.setIcon(iconRefreshOver);
            resetJButton.setRolloverIcon(iconRefresh);
            horizontalJButton.setIcon(iconHorizontal);
            verticalJButton.setIcon(iconVertical);
            importJButton.setIcon(iconImport);
            importJButton.setRolloverIcon(iconImportOver);
            trackJToggle.setIcon(iconTrackOn);
            zoomInJButton.setIcon(iconZoomIn);
            zoomOutJButton.setIcon(iconZoomOut);
            forwardJButton.setIcon(iconForward);
            reverseJButton.setIcon(iconReverse);
            stopJButton.setIcon(iconStop);
            cameraJButton.setIcon(iconCamera);

            // Set the Customer Renderer for the JTRee
            importJTree.setCellRenderer(new DefaultTreeCellRenderer() {
                @Override
                public Component getTreeCellRendererComponent(JTree tree,
                        Object value,
                        boolean sel,
                        boolean expanded,
                        boolean leaf,
                        int row,
                        boolean hasFocus) {

                    // Call to super to do the default JTree drawing operations
                    super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

                    // Grab the value from the node.
                    final Object real = ((DefaultMutableTreeNode) value).getUserObject();

                    // Remember that there is a root node that is simply a String object.
                    if (real instanceof File) {

                        //
                        setText(((File) real).getName());
                        setIcon(iconPNG);
                    } else {

                        // Otherwise just set as the text form.
                        setText(String.valueOf(real));
                    }

                    // @TODO Create an Icon and submit here
                    //
                    return this;
                }
            });
        } catch (IOException ioe) {
            javax.swing.JOptionPane.showMessageDialog(this, ioe.getMessage());
        }

        //
        thresholdJSlider.setValue(tracer.getPrecision());

        // Update JLabel
        int size = tracer.getPolygonList().size();
        errorJButton.setText(size > 0 ? String.valueOf(size) : null);
        errorJButton.setIcon(size > 0 && tracer.getMessage() == null ? null : iconError);

        // Setting up the viewport for scrolling if nessecary.
        mainJScrollPane.setViewportView(panel);

        // Set the title
        // Version will be made up as I don't know how to properly keep track of versions even though I use versioning software -- go figure.
        setTitle("Image Tracer (version 1.01a)");
    }

    private void importSamples() {

        //
        final File directory = new File(getClass().getResource("/samples").getFile());

        //
        final File[] files = directory.listFiles();
        final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Images");
        final DefaultTreeModel model = new DefaultTreeModel(rootNode);

        // If it's there.
        if (files != null) {

            // Max of eight samples for now. I know wrong way of ever implementing
            // something like this.
            for (File file : files) {
                //
                // Null check
                if (file == null) {
                    break;
                }
                // Question it first
                if (file.getAbsolutePath().toLowerCase().endsWith(".png")) {

                    //
                    BufferedImage image = null;

                    // Try
                    try {
                        image = FormTracer.bufferImage(ImageIO.read(file), this, BufferedImage.TYPE_INT_ARGB);
                    } catch (IOException ioe) {

                    }

                    // Null Check
                    if (image != null) {

                        //
                        rootNode.add(new DefaultMutableTreeNode(file));

                        //
                        selImage = image;
                    }
                }
            }

            //
            importJTree.setModel(model);
            importJTree.revalidate();
        }
    }

    private void changeImage(BufferedImage image) {

        //
        selImage = image;

        // Stop the timer
        timer.stop();

        //
        tracer = new FormTracer(image);
        tracer.setPrecision(thresholdJSlider.getValue());

        // Enabled refresh because the timer is stopped.
        resetJButton.setEnabled(true);
        horizontalJButton.setEnabled(true);
        verticalJButton.setEnabled(true);
        playJButton.setEnabled(true);

        // Reset the play button to it's play state
        playJButton.setIcon(iconPlay);

        // Update JLabel
        int size = tracer.getPolygonList().size();
        errorJButton.setText(size > 0 ? String.valueOf(size) : null);
        errorJButton.setIcon(size > 0 && tracer.getMessage() == null ? null : iconError);

        // Change size of viewport
        panel.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        panel.setMinimumSize(panel.getPreferredSize());
        panel.setMaximumSize(panel.getPreferredSize());
        mainJScrollPane.setViewportView(panel);

        //
        updatePanelZoom(panelZoom);

        //
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent evt) {

        // Step
        tracer.run();

        // In the case that you want to watch, the tracer work, more closely.
        if (trackJToggle.isSelected()) {

            // This makes the viewport scroll to the current position.
            if (tracer.isRunning()) {

                // Making the view snap to the tracers current position.
                int curX = tracer.getCurrentX();
                int curY = tracer.getCurrentY();
                curX *= panelZoom;
                curY *= panelZoom;

                // First, if the images zoomed in bounds exceed the pane
                if (panel.getWidth() > mainJScrollPane.getWidth() || panel.getHeight() > mainJScrollPane.getHeight()) {

                    // Determine which Sector we're in for X
                    final int curXSector = curX / mainJScrollPane.getWidth();
                    final int curYSector = curY / mainJScrollPane.getHeight();

                    // Only update if the sector changes.
                    if (lastXSector != curXSector || lastYSector != curYSector) {

                        // Change the sector
                        lastXSector = curXSector;
                        lastYSector = curYSector;

                        // Snap to that position
                        curX = (mainJScrollPane.getWidth() * lastXSector) + 1;
                        curY = (mainJScrollPane.getHeight() * lastYSector) + 1;

                        // Now scroll to that.
                        mainJScrollPane.getViewport().setViewPosition(new java.awt.Point(curX, curY));
                    }
                }
            }
        }

        // When it's finished
        if (tracer.isFinished()) {

            // Stop the timer
            timer.stop();

            // Reset the controls.
            playJButton.setIcon(iconPlay);
            resetJButton.setEnabled(true);

            // Return the size of the panel to the images size, because it was probably
            // zoomed in if you were tracking it.
            updatePanelZoom(1);

            // Reset view position
            mainJScrollPane.getViewport().setViewPosition(new Point(0, 0));

            // To update the errorJButton.
            final int size = tracer.getPolygonList().size();

            // Show the message
            if (tracer.getMessage() != null) {
                errorJButton.setEnabled(true);
                errorJButton.setToolTipText(tracer.getMessage());
            } else {
                errorJButton.setEnabled(false);
                errorJButton.setToolTipText("Polygons: " + size);
            }

            // Set Traces to false just in case they were still enabled for some reason.
            hoverHorizontal = false;
            hoverVertical = false;

            // Update JLabel
            errorJButton.setText(size > 0 ? String.valueOf(size) : null);
            errorJButton.setIcon(size > 0 && tracer.getMessage() == null ? null : iconError);
        }

        // Repaint last, as always.
        repaint();
    }

    private void drawTextileBackground(Graphics2D manet) {

        //
        final JScrollPane parentPane = (JScrollPane) panel.getParent().getParent();

        //
        final int rowSplit = 16;
        final int colSplit = 16;

        //
        final int parentWidth = parentPane == null ? getWidth() : parentPane.getWidth();
        final int parentHeight = parentPane == null ? getHeight() : parentPane.getHeight();

        //
        final int imageWidth = panel.getPreferredSize().width;
        final int imageHeight = panel.getPreferredSize().height;

        //
        final int width = parentWidth > imageWidth ? parentWidth : imageWidth;
        final int height = parentHeight > imageHeight ? parentHeight : imageHeight;

        //
        final int rowExtra = 2;
        final int colExtra = 2;

        // Calc. Rows and columns
        final int rows = (width / rowSplit) + rowExtra;
        final int cols = (height / colSplit) + colExtra;

        // Initial color
        Color color = textileForeground;

        // Drawing the rectangles.
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {

                // Alternating colors (Took a lot of playing around to get to this code)
                // @Ternary Option
                color = color == textileBackground ? textileForeground : textileBackground;

                // Giving it a little offset in columns
                if (j % 2 == 0) {
                    if (j == cols) {
                        //@Ternary Option
                        color = color == textileForeground ? textileBackground : textileForeground;
                    }
                } else if (j == cols - 1) {
                    //@Ternary Option
                    color = color == textileForeground ? textileBackground : textileForeground;
                }

                // Set the color
                manet.setColor(color);

                // Fill the rectangle
                manet.fill(new Rectangle(i * 16, j * 16, 16, 16));
            }
        }
    }

    private void updatePanelZoom(int ZOOM) {

        //
        this.panelZoom = ZOOM;

        //
        panel.setSize(selImage.getWidth() * ZOOM, selImage.getHeight() * ZOOM);
        panel.setPreferredSize(panel.getSize());
        panel.setMinimumSize(panel.getSize());
        panel.setMaximumSize(panel.getSize());

        //
        mainJScrollPane.setViewportView(panel);
        mainJScrollPane.revalidate();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonJPanel = new javax.swing.JPanel();
        playJButton = new javax.swing.JButton();
        filler9 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        reverseJButton = new javax.swing.JButton();
        filler4 = new javax.swing.Box.Filler(new java.awt.Dimension(4, 0), new java.awt.Dimension(4, 0), new java.awt.Dimension(4, 32767));
        stopJButton = new javax.swing.JButton();
        filler5 = new javax.swing.Box.Filler(new java.awt.Dimension(4, 0), new java.awt.Dimension(4, 0), new java.awt.Dimension(4, 32767));
        forwardJButton = new javax.swing.JButton();
        filler10 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        jSeparator3 = new javax.swing.JSeparator();
        trackJToggle = new javax.swing.JToggleButton();
        filler11 = new javax.swing.Box.Filler(new java.awt.Dimension(4, 0), new java.awt.Dimension(4, 0), new java.awt.Dimension(4, 32767));
        zoomInJButton = new javax.swing.JButton();
        filler12 = new javax.swing.Box.Filler(new java.awt.Dimension(4, 0), new java.awt.Dimension(4, 0), new java.awt.Dimension(4, 32767));
        zoomOutJButton = new javax.swing.JButton();
        filler8 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        jSeparator1 = new javax.swing.JSeparator();
        horizontalJButton = new javax.swing.JButton();
        filler6 = new javax.swing.Box.Filler(new java.awt.Dimension(4, 0), new java.awt.Dimension(4, 0), new java.awt.Dimension(4, 32767));
        verticalJButton = new javax.swing.JButton();
        filler7 = new javax.swing.Box.Filler(new java.awt.Dimension(4, 0), new java.awt.Dimension(4, 0), new java.awt.Dimension(4, 32767));
        resetJButton = new javax.swing.JButton();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        jSeparator2 = new javax.swing.JSeparator();
        importJButton = new javax.swing.JButton();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        jSeparator4 = new javax.swing.JSeparator();
        cameraJButton = new javax.swing.JButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        errorJButton = new javax.swing.JButton();
        percisionJPanel = new javax.swing.JPanel();
        captureJLabel = new javax.swing.JLabel();
        filler13 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        thresholdJSlider = new javax.swing.JSlider();
        mainJSplitPane = new javax.swing.JSplitPane();
        mainJScrollPane = new javax.swing.JScrollPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        importJTree = new javax.swing.JTree();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(574, 468));

        buttonJPanel.setMinimumSize(new java.awt.Dimension(376, 32));
        buttonJPanel.setPreferredSize(new java.awt.Dimension(384, 32));
        buttonJPanel.setLayout(new javax.swing.BoxLayout(buttonJPanel, javax.swing.BoxLayout.LINE_AXIS));

        playJButton.setToolTipText("Play / Resume");
        playJButton.setMaximumSize(new java.awt.Dimension(32, 32));
        playJButton.setMinimumSize(new java.awt.Dimension(32, 32));
        playJButton.setPreferredSize(new java.awt.Dimension(32, 32));
        playJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(playJButton);
        buttonJPanel.add(filler9);

        reverseJButton.setToolTipText("Slow Down");
        reverseJButton.setMaximumSize(new java.awt.Dimension(28, 28));
        reverseJButton.setMinimumSize(new java.awt.Dimension(28, 28));
        reverseJButton.setPreferredSize(new java.awt.Dimension(28, 28));
        reverseJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reverseJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(reverseJButton);
        buttonJPanel.add(filler4);

        stopJButton.setToolTipText("Stop");
        stopJButton.setMaximumSize(new java.awt.Dimension(28, 28));
        stopJButton.setMinimumSize(new java.awt.Dimension(28, 28));
        stopJButton.setPreferredSize(new java.awt.Dimension(28, 28));
        stopJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(stopJButton);
        buttonJPanel.add(filler5);

        forwardJButton.setToolTipText("Speed Up");
        forwardJButton.setMaximumSize(new java.awt.Dimension(28, 28));
        forwardJButton.setMinimumSize(new java.awt.Dimension(28, 28));
        forwardJButton.setPreferredSize(new java.awt.Dimension(28, 28));
        forwardJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                forwardJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(forwardJButton);
        buttonJPanel.add(filler10);

        jSeparator3.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator3.setMaximumSize(new java.awt.Dimension(8, 32767));
        jSeparator3.setMinimumSize(new java.awt.Dimension(8, 10));
        jSeparator3.setPreferredSize(new java.awt.Dimension(8, 10));
        buttonJPanel.add(jSeparator3);

        trackJToggle.setSelected(true);
        trackJToggle.setToolTipText("Track");
        trackJToggle.setMaximumSize(new java.awt.Dimension(28, 28));
        trackJToggle.setMinimumSize(new java.awt.Dimension(28, 28));
        trackJToggle.setPreferredSize(new java.awt.Dimension(28, 28));
        trackJToggle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                trackJToggleActionPerformed(evt);
            }
        });
        buttonJPanel.add(trackJToggle);
        buttonJPanel.add(filler11);

        zoomInJButton.setToolTipText("Zoom In");
        zoomInJButton.setMaximumSize(new java.awt.Dimension(28, 28));
        zoomInJButton.setMinimumSize(new java.awt.Dimension(28, 28));
        zoomInJButton.setPreferredSize(new java.awt.Dimension(28, 28));
        zoomInJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomInJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(zoomInJButton);
        buttonJPanel.add(filler12);

        zoomOutJButton.setToolTipText("Zoom Out");
        zoomOutJButton.setMaximumSize(new java.awt.Dimension(28, 28));
        zoomOutJButton.setMinimumSize(new java.awt.Dimension(28, 28));
        zoomOutJButton.setPreferredSize(new java.awt.Dimension(28, 28));
        zoomOutJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomOutJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(zoomOutJButton);
        buttonJPanel.add(filler8);

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator1.setMaximumSize(new java.awt.Dimension(8, 32));
        jSeparator1.setMinimumSize(new java.awt.Dimension(8, 32));
        jSeparator1.setPreferredSize(new java.awt.Dimension(8, 32));
        buttonJPanel.add(jSeparator1);

        horizontalJButton.setToolTipText("Horizontal Cut");
        horizontalJButton.setMaximumSize(new java.awt.Dimension(28, 28));
        horizontalJButton.setMinimumSize(new java.awt.Dimension(28, 28));
        horizontalJButton.setPreferredSize(new java.awt.Dimension(28, 28));
        horizontalJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                horizontalJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(horizontalJButton);
        buttonJPanel.add(filler6);

        verticalJButton.setToolTipText("Vertical Cut");
        verticalJButton.setMaximumSize(new java.awt.Dimension(28, 28));
        verticalJButton.setMinimumSize(new java.awt.Dimension(28, 28));
        verticalJButton.setPreferredSize(new java.awt.Dimension(28, 28));
        verticalJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verticalJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(verticalJButton);
        buttonJPanel.add(filler7);

        resetJButton.setToolTipText("Reset Image");
        resetJButton.setMaximumSize(new java.awt.Dimension(28, 28));
        resetJButton.setMinimumSize(new java.awt.Dimension(28, 28));
        resetJButton.setPreferredSize(new java.awt.Dimension(28, 28));
        resetJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(resetJButton);
        buttonJPanel.add(filler2);

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator2.setMaximumSize(new java.awt.Dimension(8, 32767));
        jSeparator2.setPreferredSize(new java.awt.Dimension(8, 10));
        buttonJPanel.add(jSeparator2);

        importJButton.setToolTipText("Import a PNG Image");
        importJButton.setMaximumSize(new java.awt.Dimension(28, 28));
        importJButton.setMinimumSize(new java.awt.Dimension(28, 28));
        importJButton.setPreferredSize(new java.awt.Dimension(28, 28));
        importJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(importJButton);
        buttonJPanel.add(filler3);

        jSeparator4.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator4.setMaximumSize(new java.awt.Dimension(8, 32767));
        jSeparator4.setPreferredSize(new java.awt.Dimension(8, 10));
        buttonJPanel.add(jSeparator4);

        cameraJButton.setToolTipText("Import a PNG Image");
        cameraJButton.setMaximumSize(new java.awt.Dimension(28, 28));
        cameraJButton.setMinimumSize(new java.awt.Dimension(28, 28));
        cameraJButton.setPreferredSize(new java.awt.Dimension(28, 28));
        cameraJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cameraJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(cameraJButton);
        buttonJPanel.add(filler1);

        errorJButton.setToolTipText("No Errors");
        errorJButton.setEnabled(false);
        errorJButton.setIconTextGap(0);
        errorJButton.setMaximumSize(new java.awt.Dimension(48, 28));
        errorJButton.setMinimumSize(new java.awt.Dimension(48, 28));
        errorJButton.setPreferredSize(new java.awt.Dimension(48, 28));
        errorJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                errorJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(errorJButton);

        percisionJPanel.setLayout(new javax.swing.BoxLayout(percisionJPanel, javax.swing.BoxLayout.LINE_AXIS));

        captureJLabel.setText("Precision:");
        captureJLabel.setToolTipText("How close should the Polygon hug the Image");
        captureJLabel.setMaximumSize(new java.awt.Dimension(96, 24));
        captureJLabel.setMinimumSize(new java.awt.Dimension(96, 24));
        captureJLabel.setPreferredSize(new java.awt.Dimension(96, 24));
        percisionJPanel.add(captureJLabel);
        percisionJPanel.add(filler13);

        thresholdJSlider.setMaximum(64);
        thresholdJSlider.setMinorTickSpacing(8);
        thresholdJSlider.setPaintTicks(true);
        thresholdJSlider.setToolTipText("Transparency Threshold");
        thresholdJSlider.setValue(16);
        thresholdJSlider.setMaximumSize(new java.awt.Dimension(32767, 24));
        thresholdJSlider.setMinimumSize(new java.awt.Dimension(36, 24));
        thresholdJSlider.setPreferredSize(new java.awt.Dimension(200, 24));
        thresholdJSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                thresholdJSliderStateChanged(evt);
            }
        });
        percisionJPanel.add(thresholdJSlider);

        mainJSplitPane.setDividerLocation(164);
        mainJSplitPane.setDividerSize(8);
        mainJSplitPane.setResizeWeight(0.2);

        mainJScrollPane.setToolTipText("");
        mainJScrollPane.setMaximumSize(new java.awt.Dimension(238, 32767));
        mainJScrollPane.setMinimumSize(new java.awt.Dimension(238, 202));
        mainJScrollPane.setPreferredSize(new java.awt.Dimension(238, 202));
        mainJSplitPane.setRightComponent(mainJScrollPane);

        importJTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                importJTreeValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(importJTree);

        mainJSplitPane.setLeftComponent(jScrollPane1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 550, Short.MAX_VALUE)
                    .addComponent(percisionJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(mainJSplitPane))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mainJSplitPane)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(percisionJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(buttonJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    private void playJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playJButtonActionPerformed
        // DO IT!!!
        if (timer.isRunning()) {

            // Pause the timer
            timer.stop();

            // Set the play icon
            playJButton.setIcon(iconPlay);

            // We can refresh at this point.
            resetJButton.setEnabled(true);
        } else {

            // Start the timer and change the play icon to a pause icon
            timer.start();
            playJButton.setIcon(iconPause);

            // Diable most control buttons when we start
            resetJButton.setEnabled(false);
            horizontalJButton.setEnabled(false);
            verticalJButton.setEnabled(false);

            //
            hoverVertical = false;
            hoverHorizontal = false;

            // If you have the track toggle selected
            if (trackJToggle.isSelected()) {

                //
                updatePanelZoom(panelZoom >= 2 ? panelZoom : 2);
            }
        }

        // If we finished then restart from the top.
        if (tracer.isFinished()) {

            //
            final int size = tracer.getPolygonList().size();
            errorJButton.setText(size > 0 ? String.valueOf(size) : null);
            errorJButton.setIcon(size > 0 && tracer.getMessage() == null ? null : iconError);

            // Restart the trace and run.
            tracer.reset();

            // Run.
            timer.start();
        }

        // Show the message
        if (tracer.getMessage() != null) {
            errorJButton.setEnabled(true);
            errorJButton.setToolTipText(tracer.getMessage());
        } else {
            errorJButton.setEnabled(false);
            errorJButton.setToolTipText("Polygons: " + tracer.getPolygonList().size());
        }

        // Quick refresh.
        repaint();
    }//GEN-LAST:event_playJButtonActionPerformed

    private void thresholdJSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_thresholdJSliderStateChanged
        // TODO add your handling code here:
        final int size = tracer.getPolygonList().size();

        //
        if (tracer != null) {

            //
            if (!thresholdJSlider.getValueIsAdjusting()) {

                //
                final int val = thresholdJSlider.getValue();
                final int max = thresholdJSlider.getMaximum();

                //
                tracer.setPrecision(val);

                // If it's been run at least once with the current image.
                // Do the automatic Polygon acquisition.
                if (tracer.isFinished()) {

                    // No longer need the timer running
                    timer.stop();

                    // Grab polygons
                    tracer.reset();
                    tracer.flash();

                    // Show the message
                    if (tracer.getMessage() != null) {
                        errorJButton.setEnabled(true);
                        errorJButton.setToolTipText(tracer.getMessage());
                    } else {
                        errorJButton.setEnabled(false);
                        errorJButton.setToolTipText("Polygons: " + size);
                    }

                    // Paint the picture.
                    repaint();
                }

                // Just describes how tight the tracer will stick to the image.
                if (val < 24) {
                    thresholdJSlider.setToolTipText("Informal Handshake");
                } else if (val >= 24 && val < 48) {
                    thresholdJSlider.setToolTipText("Weak Hug");
                } else if (val >= 48 && val < 64) {
                    thresholdJSlider.setToolTipText("Warm Embrace");
                } else if (val >= 64) {
                    thresholdJSlider.setToolTipText("Never Let Go Hug");
                }

                //
                captureJLabel.setText("Precision: " + ((val * 100) / max) + "%");
            } else {

                // Allow active changes
                tracer.setPrecision(thresholdJSlider.getValue());
            }
        }

        // Update JLabel
        errorJButton.setText(size > 0 ? String.valueOf(size) : null);
        errorJButton.setIcon(size > 0 && tracer.getMessage() == null ? null : iconError);
    }//GEN-LAST:event_thresholdJSliderStateChanged

    private void resetJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetJButtonActionPerformed

        // Stop the timer.
        timer.stop();
        tracer.reset(selImage);

        // Update buttons.
        playJButton.setIcon(iconPlay);
        playJButton.setEnabled(true);
        horizontalJButton.setEnabled(true);
        verticalJButton.setEnabled(true);

        // Update JLabel
        final int size = tracer.getPolygonList().size();
        errorJButton.setText(size > 0 ? String.valueOf(size) : null);
        errorJButton.setIcon(size > 0 && tracer.getMessage() == null ? null : iconError);

        // Show the message
        if (tracer.getMessage() != null) {
            errorJButton.setEnabled(true);
            errorJButton.setToolTipText(tracer.getMessage());
        } else {
            errorJButton.setEnabled(false);
            errorJButton.setToolTipText("Polygons: " + size);
        }

        // Repaint.
        repaint();
    }//GEN-LAST:event_resetJButtonActionPerformed
    private void errorJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_errorJButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_errorJButtonActionPerformed
    private void horizontalJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_horizontalJButtonActionPerformed
        // TODO add your handling code here:
        hoverHorizontal = true;
        hoverVertical = false;
        repaint();
    }//GEN-LAST:event_horizontalJButtonActionPerformed
    private void verticalJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_verticalJButtonActionPerformed
        // TODO add your handling code here:
        hoverVertical = true;
        hoverHorizontal = false;
        repaint();
    }//GEN-LAST:event_verticalJButtonActionPerformed
    private void importJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importJButtonActionPerformed

        // TODO add your handling code here:
        final JFileChooser chooser = new JFileChooser();

        //
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

            // Then file
            final File file = chooser.getSelectedFile();

            //
            if (file != null && file.getAbsolutePath().toLowerCase().endsWith("png")) {

                // Attempt to import the file
                try {

                    //
                    selImage = FormTracer.bufferImage(ImageIO.read(file.getAbsoluteFile()), this, BufferedImage.TYPE_INT_ARGB);

                    // The magic itself.
                    tracer = new FormTracer(selImage);
                    tracer.setPrecision(thresholdJSlider.getValue());

                    //
                    repaint();
                } catch (IOException ioe) {
                    javax.swing.JOptionPane.showMessageDialog(this, ioe);
                }
            }
        }
    }//GEN-LAST:event_importJButtonActionPerformed
    private void trackJToggleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_trackJToggleActionPerformed
        // Change the Icon depending on the track record :U (I'm funny)
        trackJToggle.setIcon(trackJToggle.isSelected() ? iconTrackOn : iconTrackOff);

        // We're still pushing along.
        if (!tracer.isFinished()) {

            // and this is deselected while it's running
            if (!trackJToggle.isSelected()) {

                // Return the size of the panel to the images size, because it was probably
                // zoomed in if you were tracking it.
                // @TODO Maybe don't return the zoom to normal, kinda annoying once you've already set it.
                updatePanelZoom(1);

                // Reset view position
                mainJScrollPane.getViewport().setViewPosition(new Point(0, 0));
            } else {

                //
                updatePanelZoom(2);
            }
        }
    }//GEN-LAST:event_trackJToggleActionPerformed

    private void zoomInJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomInJButtonActionPerformed
        // TODO add your handling code here:
        updatePanelZoom(panelZoom + 1 <= panelZoomMax ? panelZoom + 1 : panelZoom);

        //
        repaint();
    }//GEN-LAST:event_zoomInJButtonActionPerformed

    private void zoomOutJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomOutJButtonActionPerformed
        // TODO add your handling code here:
        updatePanelZoom(panelZoom - 1 >= 1 ? panelZoom - 1 : panelZoom);

        //
        repaint();
    }//GEN-LAST:event_zoomOutJButtonActionPerformed

    private void reverseJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reverseJButtonActionPerformed
        // If you want the play speed to run slower then change the max delay.
        playSpeed = playSpeed * 10 > playDelayMax ? playDelayMax : playSpeed * 10;

        //
        timer.setDelay(playSpeed);
    }//GEN-LAST:event_reverseJButtonActionPerformed

    private void stopJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopJButtonActionPerformed

        // Completely reset the Image and saved Polygons
        tracer.reset();

        // Set the Reset Button back enabled
        resetJButton.setEnabled(true);

        //
        repaint();

        // TODO add your handling code here:        
        timer.stop();
    }//GEN-LAST:event_stopJButtonActionPerformed

    private void forwardJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_forwardJButtonActionPerformed
        // If you want it to play faster then comment this out and set the platSpeed below 10.
        playSpeed = playSpeed / 10 < playDelayMin ? playDelayMin : playSpeed / 10;

        //
        timer.setDelay(playSpeed);
    }//GEN-LAST:event_forwardJButtonActionPerformed

    private void importJTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_importJTreeValueChanged

        // TODO add your handling code here:
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) importJTree.getLastSelectedPathComponent();

        // Kick out if null.
        if (node == null) {
            return;
        }

        // The Absolute Path of the File to open.
        final File file = new File(String.valueOf(node.getUserObject()));

        // Do not process directories.
        if (file.exists() && !file.isDirectory()) {
            //
            try {

                // Open that file.
                changeImage(ImageIO.read(file));
            } catch (IOException ioe) {

                // Show a JOptionPane with the error
                javax.swing.JOptionPane.showMessageDialog(this, ioe);
            }
        }
    }//GEN-LAST:event_importJTreeValueChanged

    private void cameraJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cameraJButtonActionPerformed
        // TODO add your handling code here:        // DO IT!!!
        if (timer.isRunning()) {

            // Pause the timer
            timer.stop();

            // Set the play icon
            playJButton.setIcon(iconPlay);

            // We can refresh at this point.
            resetJButton.setEnabled(true);
        }

        // Reset the tracer
        tracer.reset();

        //
        tracer.flash();

        //
        repaint();
    }//GEN-LAST:event_cameraJButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel buttonJPanel;
    private javax.swing.JButton cameraJButton;
    private javax.swing.JLabel captureJLabel;
    private javax.swing.JButton errorJButton;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler10;
    private javax.swing.Box.Filler filler11;
    private javax.swing.Box.Filler filler12;
    private javax.swing.Box.Filler filler13;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.Box.Filler filler4;
    private javax.swing.Box.Filler filler5;
    private javax.swing.Box.Filler filler6;
    private javax.swing.Box.Filler filler7;
    private javax.swing.Box.Filler filler8;
    private javax.swing.Box.Filler filler9;
    private javax.swing.JButton forwardJButton;
    private javax.swing.JButton horizontalJButton;
    private javax.swing.JButton importJButton;
    private javax.swing.JTree importJTree;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JScrollPane mainJScrollPane;
    private javax.swing.JSplitPane mainJSplitPane;
    private javax.swing.JPanel percisionJPanel;
    private javax.swing.JButton playJButton;
    private javax.swing.JButton resetJButton;
    private javax.swing.JButton reverseJButton;
    private javax.swing.JButton stopJButton;
    private javax.swing.JSlider thresholdJSlider;
    private javax.swing.JToggleButton trackJToggle;
    private javax.swing.JButton verticalJButton;
    private javax.swing.JButton zoomInJButton;
    private javax.swing.JButton zoomOutJButton;
    // End of variables declaration//GEN-END:variables
}
