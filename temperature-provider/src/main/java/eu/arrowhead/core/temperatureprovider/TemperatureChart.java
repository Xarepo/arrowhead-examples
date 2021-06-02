package eu.arrowhead.core.temperatureprovider;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.chart.ui.UIUtils;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Second;
import org.jfree.data.xy.XYDataset;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TemperatureChart extends ApplicationFrame {

    private static final String TITLE = "Thermometer";
    private static final double MIN_TEMPERATURE = 0;
    private static final double MAX_TEMPERATURE = 30;
    private static final int MAX_COLOR = 255;
    private static final int COUNT = 2 * 60;
    private static final int UPDATE_INTERVAL_MILLIS = 1000;
    private final Timer timer;

    public TemperatureChart(String title, Thermometer thermometer) {
        super(title);

        final DynamicTimeSeriesCollection dataset =
            new DynamicTimeSeriesCollection(1, COUNT, new Second());
        dataset.setTimeBase(new Second());
        float[] data = {};
        dataset.addSeries(data, 0, "Temperature (c)");
        JFreeChart chart = createChart(dataset);

        this.add(new ChartPanel(chart), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout());
        this.add(btnPanel, BorderLayout.SOUTH);

        timer = new Timer(UPDATE_INTERVAL_MILLIS, new ActionListener() {
            final float[] latestReading = new float[1];

            @Override
            public void actionPerformed(ActionEvent e) {
                double temperature = thermometer.getTemperature();
                latestReading[0] = (float) temperature;
                dataset.advanceTime();
                dataset.appendData(latestReading);

                final XYPlot plot = chart.getXYPlot();
                plot.getRenderer().setSeriesPaint(0, getColor(temperature));
            }
        });
    }

    private static Color getColor(double temperature) {
        int blue = (int) ((MAX_TEMPERATURE - temperature) * MAX_COLOR / MAX_TEMPERATURE);
        int red = (int) (temperature * MAX_COLOR / MAX_TEMPERATURE);
        int green = 0;

        blue = Math.min(MAX_COLOR, Math.max(blue, 0));
        red = Math.min(MAX_COLOR, Math.max(red, 0));

        return new Color(red, green, blue);
    }

    private JFreeChart createChart(final XYDataset dataset) {
        final JFreeChart chart = ChartFactory.createTimeSeriesChart(
            TITLE, "hh:mm:ss", "Temperature (c)", dataset, true, true, false);
        final XYPlot plot = chart.getXYPlot();

        ValueAxis domain = plot.getDomainAxis();
        domain.setAutoRange(true);
        ValueAxis range = plot.getRangeAxis();
        range.setRange(MIN_TEMPERATURE, MAX_TEMPERATURE);
        return chart;
    }

    public void start() {
        pack();
        UIUtils.centerFrameOnScreen(this);
        setVisible(true);
        timer.start();
    }

}
