package de.lmu.dbs.jfeaturelib.edgeDetector;

import de.lmu.dbs.jfeaturelib.Progress;
import de.lmu.dbs.jfeaturelib.features.FeatureDescriptor;
import de.lmu.dbs.jfeaturelib.utils.RGBtoGray;
import de.lmu.ifi.dbs.utilities.Arrays2;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * FIXME docu
 * @author Benedikt
 */
public class Susan implements FeatureDescriptor {

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private int radius;
    private int threshold;
    private int[] features;
    private ColorProcessor image;

    /**
     * Standart constructor with radius 2 and threshold 15
     */
    public Susan() {
        this.radius = 2;
        this.threshold = 15;
    }

    /**
     * Custom constructor
     * @param radius Radius in which the image is looked at
     * @param threshold Threshold for difference in luminosity
     */
    public Susan(int radius, int threshold) {
        this.radius = radius;
        this.threshold = threshold;

    }

    /**
     * 
     * @return Radius in which the image is looked at
     */
    public int getRadius() {
        return radius;
    }

    /**
     * 
     * @param radius Radius in which the image is looked at
     */
    public void setRadius(int radius) {
        this.radius = radius;
    }

    /**
     * 
     * @return Threshold for difference in luminosity
     */
    public int getThreshold() {
        return threshold;
    }

    /**
     * 
     * @param threshold Threshold for difference in luminosity
     */
    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public List<double[]> getFeatures() {
        if (features != null) {
            ArrayList<double[]> result = new ArrayList<>(1);
            result.add(Arrays2.convertToDouble(features));
            return result;
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Defines the capability of the algorithm.
     * 
     * @see PlugInFilter
     * @see #supports() 
     */
    @Override
    public EnumSet<Supports> supports() {
        EnumSet set = EnumSet.of(
                Supports.NoChanges,
                Supports.DOES_8C,
                Supports.DOES_8G,
                Supports.DOES_RGB);
        //set.addAll(DOES_ALL);
        return set;
    }

    /**
     * 
     * @param ip ImageProcessor of the source image
     */
    @Override
    public void run(ImageProcessor ip) {
        this.image = (ColorProcessor) ip;
        pcs.firePropertyChange(Progress.getName(), null, Progress.START);
        process();
        pcs.firePropertyChange(Progress.getName(), null, Progress.END);
    }

    /* 
     * http://users.fmrib.ox.ac.uk/~steve/susan/susan/node6.html
     * Place a circular mask around the pixel in question (the nucleus).
     * Using Equation 4 calculate the number of pixels within the circular mask which have similar brightness to the nucleus. (These pixels define the USAN.)
     * Using Equation 3 subtract the USAN size from the geometric threshold to produce an edge strength image.
     * Use moment calculations applied to the USAN to find the edge direction.
     * Apply non-maximum suppression, thinning and sub-pixel estimation, if required. 
     */
    private void process() {
        int WIDTH = image.getWidth();
        int HEIGHT = image.getHeight();
        int[][] picture = image.getIntArray();
        features = new int[WIDTH * HEIGHT];

        BufferedImage result = new BufferedImage(WIDTH, HEIGHT, java.awt.image.BufferedImage.TYPE_INT_ARGB);

        int[][] mask = new int[radius * 2 + 1][radius * 2 + 1];

        int i = 0;
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                //ignore borders
                if (x >= radius && x < WIDTH - radius && y >= radius && y < HEIGHT - radius) {

                    for (int maskX = 0; maskX <= radius * 2; maskX++) {
                        for (int maskY = 0; maskY <= radius * 2; maskY++) {
                            mask[maskX][maskY] = RGBtoGray.ARGB_NTSC(picture[x - radius + maskX][y - radius + maskY]);

                            //System.out.print(mask[maskX][maskY] + " ");
                        }//System.out.print("\n");
                    } //System.out.print("\n\n");
                    //horizontal edge
                    boolean edge = true;
                    for (int maskX = 0; maskX <= radius * 2; maskX++) {
                        for (int maskY = 0; maskY <= radius; maskY++) {
                            if (edge && Math.abs(mask[maskX][maskY] - mask[radius][radius]) < threshold) {
                                edge = true;
                            } else {
                                edge = false;
                            }

                        }
                        if (edge) {
                            result.setRGB(x, y, -1);
                            //features[i] = -1;
                        } else {
                            result.setRGB(x, y, -16777216);
                            //features[i] = -16777216;
                        }
                    }
                    //vertical edge
                    edge = true;
                    for (int maskX = 0; maskX <= radius; maskX++) {
                        for (int maskY = 0; maskY <= radius * 2; maskY++) {
                            if (edge && Math.abs(mask[maskX][maskY] - mask[radius][radius]) < threshold) {
                                edge = true;
                            } else {
                                edge = false;
                            }

                        }
                        if (edge) {
                            result.setRGB(x, y, -1);
                            //features[i] = -1;
                        } else if (/*features[i] != -1*/result.getRGB(x, y) != -1) {
                            result.setRGB(x, y, -16777216);
                            //features[i] = -16777216;
                        }
                    }
                } //border
                else {
                }
                i++;
                //System.out.println("Int: " + picture[x][y] + ", converted: " + de.lmu.dbs.jfeaturelib.utils.RGBtoGray.ARGB_NTSC(picture[x][y]));
            }
            int progress = (int) Math.round(i * (100.0 / (double) (WIDTH * HEIGHT)));
            pcs.firePropertyChange(Progress.getName(), null, new Progress(progress, "Step " + i + " of " + WIDTH * HEIGHT));
        }
        ColorProcessor cp = new ColorProcessor(result);
        features = (int[]) cp.convertToRGB().getBufferedImage().getData().getDataElements(0, 0, WIDTH, HEIGHT, null);
        //TODO this is not very nice
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}