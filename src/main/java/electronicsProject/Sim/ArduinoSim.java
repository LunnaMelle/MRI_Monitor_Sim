package electronicsProject.Sim;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ArduinoSim {
    public static void main(String[] args) {
        int port = 12345;
        String csvFilePath = "src/main/java/electronicsProject/data/mr_bla.csv";

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Simulator started. Waiting for connection on port " + port + "...");

            while (true) { // Keep server alive for new connections
                try (Socket clientSocket = serverSocket.accept();
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                     BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {

                    System.out.println("Client connected!");

                    reader.readLine();
                    
                    AtomicBoolean isRunning = new AtomicBoolean(true);
                    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

                    scheduler.scheduleAtFixedRate(() -> {
                        try {
                            String line = reader.readLine();

                            if (line != null) {
                                String[] values = line.split(",");
                                String dataLine = values[6] + "," + values[3] + "," + values[4] + "," + values[5];
                                System.out.println("[ArduinoSim] Sending: " + dataLine);
                                out.println(dataLine);
                            } else {
                                System.out.println("End of CSV reached.");
                                isRunning.set(false);
                            }
                        } catch (Exception e) {
                            isRunning.set(false);
                            scheduler.shutdown();
                        }
                    }, 0, 1, TimeUnit.MILLISECONDS);

                    while (isRunning.get() && !clientSocket.isClosed()) {
                        Thread.sleep(100); 
                    }
                    
                    scheduler.shutdownNow();
                    System.out.println("Connection closed or file finished.");
                } catch (Exception e) {
                    System.err.println("Connection error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}