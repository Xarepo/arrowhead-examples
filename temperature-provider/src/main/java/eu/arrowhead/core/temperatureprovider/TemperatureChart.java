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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TemperatureChart extends ApplicationFrame {

    private static final String TITLE = "Dynamic Series";
    private static final float MINMAX = 40;
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
            final float[] newData = new float[1];

            @Override
            public void actionPerformed(ActionEvent e) {
                newData[0] = thermometer.getTemperature();
                dataset.advanceTime();
                dataset.appendData(newData);
            }
        });
    }

    private JFreeChart createChart(final XYDataset dataset) {
        final JFreeChart result = ChartFactory.createTimeSeriesChart(
            TITLE, "hh:mm:ss", "Temperature (c)", dataset, true, true, false);
        final XYPlot plot = result.getXYPlot();
        ValueAxis domain = plot.getDomainAxis();
        domain.setAutoRange(true);
        ValueAxis range = plot.getRangeAxis();
        range.setRange(-MINMAX, MINMAX);
        return result;
    }

    public void start() {
        pack();
        UIUtils.centerFrameOnScreen(this);
        setVisible(true);
        timer.start();
    }

}
