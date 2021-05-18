package eu.arrowhead.core.fan;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

class FanPanel extends JPanel {

    private BufferedImage image;
    private int angle = 0;

    public void loadGraphics() throws IOException {

        final ClassLoader loader = FanPanel.class.getClassLoader();
        final URL imageUrl = loader.getResource("fan.png");

        if (imageUrl == null) {
            throw new IOException("Failed to load graphics.");
        }

        image = ImageIO.read(imageUrl);
        setSurfaceSize();
    }

    private void setSurfaceSize() {
        Dimension dimension = new Dimension();
        dimension.width = image.getWidth(null);
        dimension.height = image.getHeight(null);
        setPreferredSize(dimension);
    }

    private void doDrawing(Graphics g) {

        Graphics2D g2d = (Graphics2D) g;

        double rotationRequired = Math.toRadians(angle);
        double locationX = image.getWidth(null) / 2.0;
        double locationY = image.getHeight(null) / 2.0;
        AffineTransform tx =
            AffineTransform.getRotateInstance(rotationRequired, locationX, locationY);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);

        g2d.drawImage(op.filter(image, null), 0, 0, null);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        doDrawing(g);
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }
}