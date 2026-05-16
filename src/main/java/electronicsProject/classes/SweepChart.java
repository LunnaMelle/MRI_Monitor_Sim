package electronicsProject.classes;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * SweepChart handles the real-time visualization of sensor data using a "wiper" effect.
 * It includes integrated signal processing (Bandpass) and heart rate calculation.
 */
public class SweepChart {
    public XYSeries series;
    public JPanel container;

    private NumberAxis xAxis, yAxis;
    private List<List<Number>> data = Collections.synchronizedList(new ArrayList<>());

    public boolean paused = false;
    public boolean recording = false;
    private PrintWriter recordWriter = null;

    private HeartRateCalc   hrc;
    private BandpassFilter  bp;

    private JLabel statusLabel;
    private int cursor = 0;
    private int maxSamples;

    /**
     * Basic constructor for SweepChart with default signal processing parameters.
     * @param title The title of the chart and the label for the data series
     * @param maxSamples The maximum number of samples to display in the sweep
     * @param buttons Whether to include the control buttons (Pause, Save, Record)
     */
    public SweepChart(String title, int maxSamples, boolean buttons) {
        this(title, maxSamples, buttons, null, null, false, 0.2, 0.1, 0.01, 800.0, 1000.0, 1.0);
    }

    /**
     * Constructor for SweepChart allowing toggle for auto-scaling the Y-axis.
     * @param title The title of the chart and the label for the data series
     * @param maxSamples The maximum number of samples to display in the sweep
     * @param buttons Whether to include the control buttons (Pause, Save, Record)
     * @param autoRange If true, the Y-axis will automatically adjust its range based on incoming data
     * @param hrcOffsetThreshold The offset added to the average to create the trigger point for heart rate calculation
     * @param hrcAlpha The smoothing factor (0.0 - 1.0) for the baseline average in heart rate calculation
     * @param bpLowCutoff The low cutoff frequency for the bandpass filter in Hz
     * @param bpHighCutoff The high cutoff frequency for the bandpass filter in Hz
     * @param bpSamplingRate The sampling rate of the incoming data for the bandpass filter in Hz
     * @param bpGain The gain applied to the output of the bandpass filter
     */
    public SweepChart(String title, int maxSamples, boolean buttons, boolean autoRange, double hrcOffsetThreshold, double hrcAlpha, double bpLowCutoff, double bpHighCutoff, double bpSamplingRate, double bpGain) {
        this(title, maxSamples, buttons, null, null, autoRange, hrcOffsetThreshold, hrcAlpha, bpLowCutoff, bpHighCutoff, bpSamplingRate, bpGain);
    }

    /**
     * Comprehensive constructor that initializes the filters, heart rate logic, 
     * JFreeChart UI components, and the layout container.
     * @param title The title of the chart and the label for the data series
     * @param maxSamples The maximum number of samples to display in the sweep
     * @param buttons Whether to include the control buttons (Pause, Save, Record)
     * @param xLabel The label for the X-axis (e.g., "Time (s)")
     * @param yLabel The label for the Y-axis (e.g., "Voltage (V)")
     * @param autoRange If true, the Y-axis will automatically adjust its range based on incoming data
     * @param hrcOffsetThreshold The offset added to the average to create the trigger point for heart rate calculation
     * @param hrcAlpha The smoothing factor (0.0 - 1.0) for the baseline average in heart rate calculation
     * @param bpLowCutoff The low cutoff frequency for the bandpass filter in Hz
     * @param bpHighCutoff The high cutoff frequency for the bandpass filter in Hz
     * @param bpSamplingRate The sampling rate of the incoming data for the bandpass filter in Hz
     * @param bpGain The gain applied to the output of the bandpass filter
     */
    public SweepChart(String title, int maxSamples, boolean buttons, String xLabel, String yLabel, boolean autoRange, double hrcOffsetThreshold, double hrcAlpha, double bpLowCutoff, double bpHighCutoff, double bpSamplingRate, double bpGain) {
        this.maxSamples = maxSamples;

        this.hrc = new HeartRateCalc(title, hrcOffsetThreshold, hrcAlpha);
        this.bp = new BandpassFilter(bpLowCutoff, bpHighCutoff, bpSamplingRate, bpGain);
        this.statusLabel = new JLabel(hrc.getStatus(), SwingConstants.CENTER);
        statusLabel.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 18));
        
        this.series = new XYSeries(title, false);
        for (int i = 0; i < maxSamples; i++) {
            series.add(i, null);
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
                title, xLabel, yLabel, dataset
        );

        this.xAxis = (NumberAxis) chart.getXYPlot().getDomainAxis();
        xAxis.setRange(0, maxSamples);
        xAxis.setTickUnit(new NumberTickUnit(500));
        xAxis.setNumberFormatOverride(new DecimalFormat("0.0") {
            @Override
            public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
                return super.format(number / 1000.0, toAppendTo, pos);
            }
        });

        this.yAxis = (NumberAxis) chart.getXYPlot().getRangeAxis();
        if (autoRange) {
            yAxis.setAutoRange(true);
        } else {
            yAxis.setRange(-1.5, 3.5);
        }

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        chartPanel.setPreferredSize(new java.awt.Dimension(800, 250));

        container = new JPanel(new BorderLayout());
        container.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        if (buttons) {
            topPanel.add(statusLabel, BorderLayout.WEST);

            JPanel rightGroup = createButtonPanel();
            topPanel.add(rightGroup, BorderLayout.EAST);
            
            container.add(topPanel, BorderLayout.NORTH);
            container.add(chartPanel, BorderLayout.CENTER);
        } else {
            topPanel.add(statusLabel, BorderLayout.CENTER);
            container.add(topPanel, BorderLayout.NORTH);
            container.add(chartPanel, BorderLayout.CENTER);
        }

    }

    /**
     * Helper method to group the Save, Pause, and Record buttons into a single panel.
     * @return A JPanel containing the control buttons aligned to the right
     */
    private JPanel createButtonPanel() {
        JButton pauseBtn = pauseButton();
        JButton saveBtn = saveButton();
        JButton recordBtn = recordButton();

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveBtn);
        buttonPanel.add(pauseBtn);
        buttonPanel.add(recordBtn);

        return buttonPanel;
    }

    /**
     * @return The underlying JFreeChart XYSeries data object
     */
    public XYSeries getSeries() {
        return series;
    }

    /**
     * @return The main JPanel containing the chart and its control interface.
     */
    public JPanel getContainer() {
        return container;
    }

    /**
     * @return A list of lists containing the historical data points.
     */
    public List<List<Number>> getData() {
        return new ArrayList<>(data);
    }

    /**
     * Manually sets the minimum and maximum bounds for the Y-axis.
     * @param min The minimum value for the Y-axis
     * @param max The maximum value for the Y-axis
     */
    public void ySetRange(double min, double max) {
        yAxis.setRange(min, max);
        xAxis.setRange(0, maxSamples);
    }

    /**
     * Thread-safe method to add new sensor data, apply filtering, calculate 
     * heart rate, and update the visual "sweep" cursor on the chart.
     * @param x The timestamp of the new data point
     * @param v The raw integer value of the new data point (e.g., 0 - 4095 for a 12-bit ADC)
     * @param newCursor The index of the new cursor position (if -1, it will auto-increment)
     */
    public synchronized void addData(long x, int v, int newCursor) {
        double double_v = convertToVoltage(v);
        addData(x, double_v, newCursor);
    }

    /**
     * Thread-safe method to add new sensor data, apply filtering, calculate 
     * heart rate, and update the visual "sweep" cursor on the chart.
     * @param x The timestamp of the new data point
     * @param v The voltage value of the new data point
     * @param newCursor The index of the new cursor position (if -1, it will auto-increment)
     */
    public synchronized void addData(long x, double v, int newCursor) {
        List<Number> d = new ArrayList<>();
        d.add(x);
        d.add(v);
        data.add(d);

        if (recording && recordWriter != null) {
            recordWriter.printf("%d,%d,%d%n",
                    data.size(),
                    x, v);
        }

        if (!paused) {
            double f = bp.filter(v);


            if (newCursor == -1) {
                cursor = (cursor + 1) % maxSamples;
            } else {
                cursor = newCursor;
            }
            int nullpoint = (cursor + maxSamples/20) % maxSamples;

            hrc.processPoint(x, f);
            String status = hrc.getStatus();

            series.updateByIndex(cursor, f);
            series.updateByIndex(nullpoint, null);
            statusLabel.setText(status);
        }
    }

    /**
     * Converts a raw integer ADC value to a standard voltage scale (0.0V - 5.0V).
     * @param value The raw integer value from the sensor (e.g., 0 - 4095 for a 12-bit ADC)
     * @return The corresponding voltage value
     */
    private double convertToVoltage(int value) {
        return value * (5.0 / 4095.0);
    }

    /**
     * Clears the current chart series and resets the heart rate calculation logic.
     */
    private synchronized void resetChart() {
        hrc.reset();
        for (int i = 0; i < maxSamples; i++) {
            series.updateByIndex(i, null);
        }
        statusLabel.setText(hrc.getStatus());
    }

    /**
     * Creates the Pause/Resume button and defines its toggle and reset behavior.
     * @return A JButton that allows the user to pause and resume the real-time data updates on the chart
     */
    private JButton pauseButton() {
        JButton pauseBtn = new JButton("Pause");
        pauseBtn.addActionListener(e -> {
            if (paused) {
                resetChart();
            }
            paused = !paused;
            pauseBtn.setText(paused ? "Resume" : "Pause");
        });
        return pauseBtn;
    }

    /**
     * Creates the Save button which launches a background thread to write 
     * the historical data list to a CSV file.
     * @return A JButton that allows the user to save the current data points to a CSV file
     */
    private JButton saveButton() {
        JButton saveBtn = new JButton("Save CSV");
        saveBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(".");
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {

                new Thread(() -> {
                    try (PrintWriter pw = new PrintWriter(new FileWriter(chooser.getSelectedFile()))) {
                        pw.println("sample,timeStamp," + series.getKey());

                        synchronized(data) { 
                            for (int i = 0; i < data.size(); i++) {
                                List<Number> d = data.get(i);
                                pw.printf("%d,%d,%d%n", 
                                        i, 
                                        d.get(0).longValue(), 
                                        d.get(1).intValue());
                            }
                        }
                        System.out.println("Save complete!");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        });
        return saveBtn;
    }

    /**
     * Creates the Recording button which enables real-time file writing 
     * for every incoming data point.
     * @return A JButton that allows the user to start and stop recording incoming data points to a CSV file in real-time
     */
    private JButton recordButton() {
        JButton recordBtn = new JButton("Start recording");
        recordBtn.addActionListener(e -> {
            if (!recording) {
                JFileChooser chooser = new JFileChooser(".");
                if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    try {
                        recordWriter = new PrintWriter(new FileWriter(chooser.getSelectedFile()));
                        recordWriter.println("sample,timeStamp," + series.getKey());
                        recording = true;
                        recordBtn.setText("Stop recording");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            } else {
                recording = false;
                if (recordWriter != null) recordWriter.close();
                recordBtn.setText("Start recording");
            }
        });
        return recordBtn;
    }
    
}