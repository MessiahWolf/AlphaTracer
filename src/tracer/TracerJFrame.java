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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author Robert Cherry (MessiahWolf)
 */
public class TracerJFrame extends javax.swing.JFrame implements ActionListener {

    // Variable Declaration
    // Project Classes
    private AlphaTracer tracer;
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
    // End of Variable Declaration

    public static void main(String[] args) {
        final TracerJFrame main = new TracerJFrame();
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
    }

    private void init() {

        // Our rendering panel.
        panel = new JPanel() {
            @Override
            public void paint(Graphics monet) {

                // Cast to 2D for easier polygon rendering.
                final Graphics2D manet = (Graphics2D) monet;

                // Draw textile background for whatever reason. Don't know if i completely
                // enjoy the look it gives.
                drawTextileBackground(manet);

                //
                if (tracer != null) {

                    //
                    if (trackJToggle.isSelected()) {

                        // If the progress image is being used
                        if (tracer.hasStarted() && !tracer.isFinished()) {
                            manet.scale(2f, 2f);
                        } else {
                            manet.scale(1f, 1f);
                        }
                    }

                    // Draw the image under
                    manet.drawImage(tracer.isFinished() || !tracer.hasStarted() ? tracer.getTraceImage() : tracer.getProgressImage(), 0, 0, this);
                    manet.setColor(Color.BLACK);

                    // Draw those polygons over.
                    for (Polygon poly : tracer.getPolygonList()) {

                        if (poly.contains(mousePos)) {
                            manet.fill(poly);
                        } else {
                            manet.draw(poly);
                        }
                    }

                    //
                    if (hoverHorizontal || hoverVertical) {

                        // Hover line
                        final Point[] points = hoverHorizontal ? tracer.getHorizontalLineTrace(tracer.getTraceImage(), mousePos) : tracer.getVerticalLineTrace(tracer.getTraceImage(), mousePos);
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
                                //
                                manet.setColor(Color.BLACK);
                                manet.fillOval(points[0].x - 2, points[0].y - 2, 4, 4);
                                manet.fillOval(points[1].x - 2, points[1].y - 2, 4, 4);
                            }
                        }
                    }

                    //
                    final Point p = tracer.getCurrentPosition();

                    //
                    manet.setColor(Color.RED);
                    manet.fillOval(p.x, p.y, 2, 2);
                }
            }
        };
        panel.addMouseMotionListener(new MouseAdapter() {
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
                    //tracer.addVerticalLine(evt.getPoint());
                    if (hoverHorizontal) {
                        tracer.addHorizontalLine(evt.getPoint());
                        hoverVertical = false;
                    } else if (hoverVertical) {
                        tracer.addVerticalLine(evt.getPoint());
                        hoverHorizontal = false;
                    }
                } else if (evt.getButton() == MouseEvent.BUTTON3) {
                    hoverVertical = false;
                    hoverHorizontal = false;
                }

                //
                repaint();
            }
        });

        // The timer; can set as low as you want.
        timer = new Timer(16, this);
        mousePos = new Point();

        // My probably inefficient way of getting images that will soon be deprecated.
        final Toolkit kit = Toolkit.getDefaultToolkit();
        final Class closs = getClass();

        // Icons.
        iconError = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-error16.png")));
        iconPlay = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-play24.png")));
        iconPause = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-pause24.png")));
        final ImageIcon iconRefresh = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-refresh24.png")));
        final ImageIcon iconRefreshOver = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-refresh-roll24.png")));
        final ImageIcon iconImport = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-import24.png")));
        final ImageIcon iconImportOver = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-import-roll24.png")));
        final ImageIcon iconHorizontal = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-line-horizontal24.png")));
        final ImageIcon iconVertical = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-line-vertical24.png")));
        iconTrackOn = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-track16.png")));
        iconTrackOff = new ImageIcon(kit.createImage(closs.getResource("/icons/icon-track-off16.png")));
        final Image imageFrame16 = kit.createImage(closs.getResource("/icons/icon-frame16.png"));
        final Image imageFrame32 = kit.createImage(closs.getResource("/icons/icon-frame32.png"));

        // The magic itself.
        tracer = new AlphaTracer(selImage);

        //
        final ArrayList<Image> frameList = new ArrayList();
        frameList.add(imageFrame16);
        frameList.add(imageFrame32);

        // Change frame images.
        setIconImages(frameList);

        // Icon setting
        playJButton.setIcon(iconPlay);
        playJButton.requestFocus();
        refreshJButton.setIcon(iconRefreshOver);
        refreshJButton.setRolloverIcon(iconRefresh);
        horizontalJButton.setIcon(iconHorizontal);
        verticalJButton.setIcon(iconVertical);
        importJButton.setIcon(iconImport);
        importJButton.setRolloverIcon(iconImportOver);
        trackJToggle.setIcon(iconTrackOn);

        //
        transJSlider.setValue(tracer.getTransparencyThreshold());
        timerJLabel.setText("Trace Speed: " + timer.getDelay() + "m/s");

        // Update JLabel
        int size = tracer.getPolygonList().size();
        errorJButton.setText(size > 0 ? String.valueOf(size) : null);
        errorJButton.setIcon(size > 0 && tracer.getMessage() == null ? null : iconError);

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
            for (File file : files) {
                //
                // Null check
                if (file == null) {
                    break;
                }
                // Question it first
                if (file.getAbsolutePath().toLowerCase().endsWith(".png")) {

                    //
                    final BufferedImage image = AlphaTracer.bufferImage(kit.createImage(file.getAbsolutePath()), this, BufferedImage.TYPE_INT_ARGB);

                    // Attempt to create image from it.
                    final ImageButton button = new ImageButton(this, image, file.getName());

                    //
                    button.setPreferredSize(new Dimension(32, 32));
                    button.setMaximumSize(new Dimension(32, 32));
                    button.setMinimumSize(new Dimension(32, 32));

                    //
                    imageJPanel.add(button);

                    //
                    selImage = image;
                }
            }

            //
            imageJPanel.setPreferredSize(new Dimension(32, imageJPanel.getComponentCount() * 32));
            imageJPanel.setMaximumSize(imageJPanel.getPreferredSize());
            imageJPanel.setMinimumSize(imageJPanel.getPreferredSize());
            imageJPanel.revalidate();
        }
    }

    private void changeImage(BufferedImage image) {

        //
        selImage = image;

        // Stop the timer
        timer.stop();

        //
        tracer = new AlphaTracer(image);
        tracer.setTransparencyThreshold(transJSlider.getValue());

        // Enabled refresh because the timer is stopped.
        refreshJButton.setEnabled(true);
        horizontalJButton.setEnabled(true);
        verticalJButton.setEnabled(true);
        playJButton.setEnabled(true);

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
        //
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent evt) {

        // Step
        tracer.step();

        // In the case that you want to watch, the tracer work, more closely.
        if (trackJToggle.isSelected()) {

            // This makes the viewport scroll to the current position.
            if (tracer.hasStarted() && !tracer.isFinished()) {

                //
                final Point cur = new Point(tracer.getCurrentPosition());

                // First, if the images zoomed in bounds exceed the pane
                if (selImage.getWidth() * 2 > mainJScrollPane.getWidth()
                        || selImage.getHeight() * 2 > mainJScrollPane.getHeight()) {

                    // This fixes jittering when the setViewPosition goes oob.
                    if (cur.x * 2 >= mainJScrollPane.getWidth()) {
                        cur.x = (selImage.getWidth() * 2) - mainJScrollPane.getWidth();
                    }

                    // If the y * 2 (because the image is twice the size) is greater than
                    // the size of the pane then never exceed the imageHeight * 2.
                    if (cur.y * 2 >= mainJScrollPane.getHeight()) {
                        cur.y = (selImage.getHeight() * 2) - mainJScrollPane.getHeight();
                    }

                    // Now scroll to that.
                    mainJScrollPane.getViewport().setViewPosition(cur);
                }
            }
        }

        // When it's finished
        if (tracer.isFinished()) {

            // Stop the timer
            timer.stop();

            // Reset the controls.
            playJButton.setIcon(iconPlay);
            refreshJButton.setEnabled(true);

            // Return the size of the panel to the images size, because it was probably
            // zoomed in if you were tracking it.
            final Dimension dim = new Dimension(selImage.getWidth(), selImage.getHeight());
            panel.setSize(dim);
            panel.setPreferredSize(dim);
            panel.setMinimumSize(dim);
            panel.setMaximumSize(dim);

            // Revalidate that with the scrollpane.
            mainJScrollPane.setViewportView(panel);
            mainJScrollPane.revalidate();

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
        trackJToggle = new javax.swing.JToggleButton();
        filler8 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        jSeparator1 = new javax.swing.JSeparator();
        horizontalJButton = new javax.swing.JButton();
        filler6 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        verticalJButton = new javax.swing.JButton();
        filler7 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        refreshJButton = new javax.swing.JButton();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        jSeparator2 = new javax.swing.JSeparator();
        importJButton = new javax.swing.JButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        errorJButton = new javax.swing.JButton();
        filler4 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        playJButton = new javax.swing.JButton();
        imageJScrollPane = new javax.swing.JScrollPane();
        imageJPanel = new javax.swing.JPanel();
        percisionJPanel = new javax.swing.JPanel();
        transJLabel = new javax.swing.JLabel();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        transJSlider = new javax.swing.JSlider();
        viewJPanel = new javax.swing.JPanel();
        timerJLabel = new javax.swing.JLabel();
        filler10 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        timerJSlider = new javax.swing.JSlider();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(306, 388));

        mainJScrollPane.setToolTipText("");
        mainJScrollPane.setMaximumSize(new java.awt.Dimension(238, 202));
        mainJScrollPane.setMinimumSize(new java.awt.Dimension(238, 202));
        mainJScrollPane.setPreferredSize(new java.awt.Dimension(238, 202));

        buttonJPanel.setLayout(new javax.swing.BoxLayout(buttonJPanel, javax.swing.BoxLayout.LINE_AXIS));

        trackJToggle.setSelected(true);
        trackJToggle.setToolTipText("Get a closer Look");
        trackJToggle.setMaximumSize(new java.awt.Dimension(24, 24));
        trackJToggle.setMinimumSize(new java.awt.Dimension(24, 24));
        trackJToggle.setPreferredSize(new java.awt.Dimension(24, 24));
        trackJToggle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                trackJToggleActionPerformed(evt);
            }
        });
        buttonJPanel.add(trackJToggle);
        buttonJPanel.add(filler8);

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator1.setMaximumSize(new java.awt.Dimension(8, 24));
        jSeparator1.setMinimumSize(new java.awt.Dimension(8, 24));
        jSeparator1.setPreferredSize(new java.awt.Dimension(8, 24));
        buttonJPanel.add(jSeparator1);

        horizontalJButton.setToolTipText("Add a Horizontal cut to the Image");
        horizontalJButton.setMaximumSize(new java.awt.Dimension(24, 24));
        horizontalJButton.setMinimumSize(new java.awt.Dimension(24, 24));
        horizontalJButton.setPreferredSize(new java.awt.Dimension(24, 24));
        horizontalJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                horizontalJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(horizontalJButton);
        buttonJPanel.add(filler6);

        verticalJButton.setToolTipText("Add a Vertical cut to the Image");
        verticalJButton.setMaximumSize(new java.awt.Dimension(24, 24));
        verticalJButton.setMinimumSize(new java.awt.Dimension(24, 24));
        verticalJButton.setPreferredSize(new java.awt.Dimension(24, 24));
        verticalJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verticalJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(verticalJButton);
        buttonJPanel.add(filler7);

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
        buttonJPanel.add(filler2);

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator2.setMaximumSize(new java.awt.Dimension(8, 32767));
        jSeparator2.setPreferredSize(new java.awt.Dimension(8, 10));
        buttonJPanel.add(jSeparator2);

        importJButton.setToolTipText("Import a PNG Image");
        importJButton.setMaximumSize(new java.awt.Dimension(24, 24));
        importJButton.setMinimumSize(new java.awt.Dimension(24, 24));
        importJButton.setPreferredSize(new java.awt.Dimension(24, 24));
        importJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(importJButton);
        buttonJPanel.add(filler1);

        errorJButton.setToolTipText("No Errors");
        errorJButton.setEnabled(false);
        errorJButton.setIconTextGap(0);
        errorJButton.setMaximumSize(new java.awt.Dimension(48, 24));
        errorJButton.setMinimumSize(new java.awt.Dimension(48, 24));
        errorJButton.setPreferredSize(new java.awt.Dimension(48, 24));
        errorJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                errorJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(errorJButton);
        buttonJPanel.add(filler4);

        playJButton.setToolTipText("Polygonize (Visual)");
        playJButton.setMaximumSize(new java.awt.Dimension(24, 24));
        playJButton.setMinimumSize(new java.awt.Dimension(24, 24));
        playJButton.setPreferredSize(new java.awt.Dimension(24, 24));
        playJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playJButtonActionPerformed(evt);
            }
        });
        buttonJPanel.add(playJButton);

        imageJScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        imageJScrollPane.setToolTipText("Select a sample Image");
        imageJScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        imageJPanel.setPreferredSize(new java.awt.Dimension(32, 202));
        imageJPanel.setLayout(new javax.swing.BoxLayout(imageJPanel, javax.swing.BoxLayout.Y_AXIS));
        imageJScrollPane.setViewportView(imageJPanel);

        percisionJPanel.setLayout(new javax.swing.BoxLayout(percisionJPanel, javax.swing.BoxLayout.LINE_AXIS));

        transJLabel.setText("Polygon Precision:");
        transJLabel.setToolTipText("How close should the Polygon hug the Image");
        transJLabel.setMaximumSize(new java.awt.Dimension(128, 24));
        transJLabel.setMinimumSize(new java.awt.Dimension(128, 24));
        transJLabel.setPreferredSize(new java.awt.Dimension(128, 24));
        percisionJPanel.add(transJLabel);
        percisionJPanel.add(filler3);

        transJSlider.setMaximum(64);
        transJSlider.setMinorTickSpacing(8);
        transJSlider.setPaintTicks(true);
        transJSlider.setToolTipText("Transparency Threshold");
        transJSlider.setValue(16);
        transJSlider.setMaximumSize(new java.awt.Dimension(32767, 24));
        transJSlider.setMinimumSize(new java.awt.Dimension(36, 24));
        transJSlider.setPreferredSize(new java.awt.Dimension(200, 24));
        transJSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                transJSliderStateChanged(evt);
            }
        });
        percisionJPanel.add(transJSlider);

        viewJPanel.setLayout(new javax.swing.BoxLayout(viewJPanel, javax.swing.BoxLayout.LINE_AXIS));

        timerJLabel.setText("Trace Speed:");
        timerJLabel.setToolTipText("How close should the Polygon hug the Image");
        timerJLabel.setMaximumSize(new java.awt.Dimension(128, 24));
        timerJLabel.setMinimumSize(new java.awt.Dimension(128, 24));
        timerJLabel.setPreferredSize(new java.awt.Dimension(128, 24));
        viewJPanel.add(timerJLabel);
        viewJPanel.add(filler10);

        timerJSlider.setMaximum(1000);
        timerJSlider.setMinimum(1);
        timerJSlider.setMinorTickSpacing(100);
        timerJSlider.setPaintTicks(true);
        timerJSlider.setToolTipText("Trace Delay");
        timerJSlider.setValue(16);
        timerJSlider.setMaximumSize(new java.awt.Dimension(32767, 24));
        timerJSlider.setMinimumSize(new java.awt.Dimension(36, 24));
        timerJSlider.setPreferredSize(new java.awt.Dimension(200, 24));
        timerJSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                timerJSliderStateChanged(evt);
            }
        });
        viewJPanel.add(timerJSlider);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(percisionJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 419, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(mainJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(imageJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(viewJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(mainJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(imageJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 317, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(percisionJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(viewJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(buttonJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void playJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playJButtonActionPerformed
        // DO IT!!!
        if (timer.isRunning()) {
            timer.stop();
            playJButton.setIcon(iconPlay);
        } else {
            timer.start();
            playJButton.setIcon(iconPause);
            refreshJButton.setEnabled(false);
            horizontalJButton.setEnabled(false);
            verticalJButton.setEnabled(false);

            //
            panel.setSize(selImage.getWidth() * 2, selImage.getHeight() * 2);
            panel.setPreferredSize(panel.getSize());
            panel.setMinimumSize(panel.getSize());
            panel.setMaximumSize(panel.getSize());

            //
            mainJScrollPane.setViewportView(panel);
            mainJScrollPane.revalidate();
        }

        //
        if (tracer.isFinished()) {
            tracer.reset();
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

        //
        repaint();
    }//GEN-LAST:event_playJButtonActionPerformed

    private void transJSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_transJSliderStateChanged
        // TODO add your handling code here:
        final int size = tracer.getPolygonList().size();

        //
        if (tracer != null) {

            //
            if (!transJSlider.getValueIsAdjusting()) {

                //
                final int val = transJSlider.getValue();
                final int max = transJSlider.getMaximum();

                //
                tracer.setTransparencyThreshold(val);

                // If it's been run at least once with the current image.
                // Do the automatic Polygon acquisition.
                if (tracer.hasStarted() && !tracer.isRunning()) {

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
                    transJSlider.setToolTipText("Informal Handshake");
                } else if (val >= 24 && val < 48) {
                    transJSlider.setToolTipText("Weak Hug");
                } else if (val >= 48 && val < 64) {
                    transJSlider.setToolTipText("Warm Embrace");
                } else if (val >= 64) {
                    transJSlider.setToolTipText("Never Let Go Hug");
                }

                //
                transJLabel.setText("Polygon Precision: " + ((val * 100) / max) + "%");
            } else {

                // Allow active changes
                tracer.setTransparencyThreshold(transJSlider.getValue());
            }
        }

        // Update JLabel
        errorJButton.setText(size > 0 ? String.valueOf(size) : null);
        errorJButton.setIcon(size > 0 && tracer.getMessage() == null ? null : iconError);
    }//GEN-LAST:event_transJSliderStateChanged

    private void refreshJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshJButtonActionPerformed
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
    }//GEN-LAST:event_refreshJButtonActionPerformed

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

                try {

                    //
                    final BufferedImage image = AlphaTracer.bufferImage(ImageIO.read(file.getAbsoluteFile()), this, BufferedImage.TYPE_INT_ARGB);

                    // Attempt to create image from it.
                    final ImageButton button = new ImageButton(this, image, file.getName());

                    //
                    button.setPreferredSize(new Dimension(32, 32));
                    button.setMaximumSize(new Dimension(32, 32));
                    button.setMinimumSize(new Dimension(32, 32));

                    //
                    final Dimension d = imageJPanel.getSize();

                    //
                    imageJPanel.add(button);
                    imageJPanel.setPreferredSize(new Dimension(d.width, d.height + 32));
                    imageJPanel.setMaximumSize(imageJPanel.getPreferredSize());
                    imageJPanel.setMinimumSize(imageJPanel.getPreferredSize());
                    imageJPanel.revalidate();

                    //
                    imageJScrollPane.setViewportView(imageJPanel);
                    imageJScrollPane.revalidate();

                    //
                    selImage = image;

                    // The magic itself.
                    tracer = new AlphaTracer(selImage);
                    tracer.setTransparencyThreshold(transJSlider.getValue());

                    //
                    repaint();
                } catch (IOException ioe) {

                }
            }
        }
    }//GEN-LAST:event_importJButtonActionPerformed

    private void trackJToggleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_trackJToggleActionPerformed
        // If the tracer is running
        trackJToggle.setIcon(trackJToggle.isSelected() ? iconTrackOn : iconTrackOff);

        if (tracer.hasStarted() && !tracer.isFinished()) {

            // and this is deselected while it's running
            if (!trackJToggle.isSelected()) {

                // Return the size of the panel to the images size, because it was probably
                // zoomed in if you were tracking it.
                final Dimension dim = new Dimension(selImage.getWidth(), selImage.getHeight());
                panel.setSize(dim);
                panel.setPreferredSize(dim);
                panel.setMinimumSize(dim);
                panel.setMaximumSize(dim);

                // Revalidate that with the scrollpane.
                mainJScrollPane.setViewportView(panel);
                mainJScrollPane.revalidate();

                // Reset view position
                mainJScrollPane.getViewport().setViewPosition(new Point(0, 0));
            } else {

                //
                panel.setSize(selImage.getWidth() * 2, selImage.getHeight() * 2);
                panel.setPreferredSize(panel.getSize());
                panel.setMinimumSize(panel.getSize());
                panel.setMaximumSize(panel.getSize());

                //
                mainJScrollPane.setViewportView(panel);
                mainJScrollPane.revalidate();
            }
        }
    }//GEN-LAST:event_trackJToggleActionPerformed

    private void timerJSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_timerJSliderStateChanged
        // TODO add your handling code here:
        final int val = timerJSlider.getValue();

        //
        timer.setDelay(val);
        timerJLabel.setText("Trace Speed: " + val + "m/s");
    }//GEN-LAST:event_timerJSliderStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel buttonJPanel;
    private javax.swing.JButton errorJButton;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler10;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.Box.Filler filler4;
    private javax.swing.Box.Filler filler6;
    private javax.swing.Box.Filler filler7;
    private javax.swing.Box.Filler filler8;
    private javax.swing.JButton horizontalJButton;
    private javax.swing.JPanel imageJPanel;
    private javax.swing.JScrollPane imageJScrollPane;
    private javax.swing.JButton importJButton;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JScrollPane mainJScrollPane;
    private javax.swing.JPanel percisionJPanel;
    private javax.swing.JButton playJButton;
    private javax.swing.JButton refreshJButton;
    private javax.swing.JLabel timerJLabel;
    private javax.swing.JSlider timerJSlider;
    private javax.swing.JToggleButton trackJToggle;
    private javax.swing.JLabel transJLabel;
    private javax.swing.JSlider transJSlider;
    private javax.swing.JButton verticalJButton;
    private javax.swing.JPanel viewJPanel;
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
            this.image = AlphaTracer.bufferImage(image, null, BufferedImage.TYPE_INT_ARGB);

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
            manet.drawImage(image, 0, 0, 24, 24, null);
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
