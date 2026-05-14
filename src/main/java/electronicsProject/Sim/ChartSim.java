package electronicsProject.Sim;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JPanel;

import electronicsProject.classes.SweepChart;

public class ChartSim {

    public static void main(String[] args) {

        JFrame mainFrame = new JFrame("Multi-Sensor Monitor");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLayout(new BorderLayout());

        int maxSamples = 5000;

        SweepChart chart_SPO2 = new SweepChart("SPO2", maxSamples, true, false, 0.2, 0.1, 0.05, 400.0, 1000.0, 1.5);
        SweepChart chart_IBP = new SweepChart("Invasive BP", maxSamples, true, false, 0.2, 0.1, 0.05, 400.0, 1000.0, 1.0);
        SweepChart chart_ECG = new SweepChart("ECG", maxSamples, true, false, 0.2, 0.1, 0.05, 400.0, 1000.0, 1.0);

        JPanel chartContainer = new JPanel(new GridLayout(3, 1)); 
        chartContainer.add(chart_SPO2.getContainer());
        chartContainer.add(chart_IBP.getContainer());
        chartContainer.add(chart_ECG.getContainer());

        mainFrame.add(chartContainer, BorderLayout.CENTER);
        mainFrame.pack();
        mainFrame.setVisible(true);

        new Thread(() -> {
            try (Socket socket = new Socket("localhost", 12345)) {

                System.out.println("Connected to simulator!");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                );

                String line;

                int cursor = 0;

                while ((line = reader.readLine()) != null) {

                    //System.out.println("Received: " + line);

                    String[] parts = line.split(",");

                    if (parts.length != 4) continue;

                    double SPO2 = Double.parseDouble(parts[2].trim());
                    double IBP = Double.parseDouble(parts[3].trim());
                    double ECG = Double.parseDouble(parts[1].trim());

                    long timestamp = System.currentTimeMillis();

                    chart_SPO2.addData(timestamp, SPO2, cursor);
                    chart_IBP.addData(timestamp, IBP, cursor);
                    chart_ECG.addData(timestamp, ECG, cursor);

                    cursor = (cursor + 1) % maxSamples;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

}