package eu.arrowhead.core.fan;

import javax.swing.JFrame;
import java.awt.Color;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class FanAnimation extends JFrame {

    final ThermometerReader thermometerReader;
    private final double alpha = 0.05;
    private int angle = 0;
    private double weightedAverageTemp = 0;

    public FanAnimation(final ThermometerReader thermometerReader) {
        this.thermometerReader = thermometerReader;
    }

    private static Color getColor(double temperature) {

        final double MAX_TEMPERATURE = 30;
        final int MAX_COLOR = 255;
        final int MIN_COLOR = 128;

        int blue = (int) ((MAX_TEMPERATURE - temperature) * MAX_COLOR / MAX_TEMPERATURE);
        int red = (int) (temperature * MAX_COLOR / MAX_TEMPERATURE);

        blue = Math.min(MAX_COLOR, Math.max(blue, MIN_COLOR));
        red = Math.min(MAX_COLOR, Math.max(red, MIN_COLOR));

        return new Color(red, MIN_COLOR, blue);
    }

    private double getSpeed(double temperature) {
        double STOP_TEMPERATURE = 5;
        double speed = (temperature - STOP_TEMPERATURE) / 6;
        double MAX_SPEED = 5;
        return Math.max(Math.min(speed, MAX_SPEED), 0);
    }

    public void init() throws IOException {

        FanPanel fanPanel = new FanPanel();
        fanPanel.loadGraphics();

        add(fanPanel);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                final double temperature = thermometerReader.getTemperature();
                weightedAverageTemp = alpha * temperature + (1 - alpha) * weightedAverageTemp;
                angle -= getSpeed(temperature);
                fanPanel.setAngle(angle);
                fanPanel.setBackground(getColor(weightedAverageTemp));
                fanPanel.repaint();
            }
        }, 0, 16);

        pack();

        setTitle("Fan");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }
}