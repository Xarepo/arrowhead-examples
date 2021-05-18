package eu.arrowhead.core.temperatureprovider;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class Thermometer {

    private static final Random random = new Random();
    private final float averageTemp;
    private final int updateInterval = 500;
    private float temperature;

    public Thermometer(float averageTemp) {
        this.averageTemp = averageTemp;
        temperature = averageTemp;
    }

    private float randomTemperature() {
        return (float) (averageTemp + random.nextGaussian());
    }

    public float getTemperature() {
        return temperature;
    }

    public void start() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                temperature = randomTemperature();
            }
        }, 0, updateInterval);
    }

}
