package eu.arrowhead.core.temperatureprovider;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class Thermometer {

    private static final Random random = new Random();
    private final double averageTemp;
    private double temperature;

    public Thermometer(double averageTemp) {
        this.averageTemp = averageTemp;
        temperature = averageTemp;
    }

    private double randomTemperature() {
        return averageTemp + random.nextGaussian();
    }

    public double getTemperature() {
        return temperature;
    }

    public void start() {
        int updateInterval = 500;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                temperature = randomTemperature();
            }
        }, 0, updateInterval);
    }

}
