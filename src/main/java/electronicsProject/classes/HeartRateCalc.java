package electronicsProject.classes;

public class HeartRateCalc {
    private long lastBeatTime = 0;
    private double currentHR = 0;
    private double dynamicThreshold = 0;
    private double runningAverage = 0;

    public String label;
    private double offsetThreshold, alpha;
        
    /**
     * Initializes a new SignalProcessor with a given label for status display.
     * 
     * @param label             A descriptive label for the signal being processed (e.g., "ECG", "PPG").
     * @param offsetThreshold   The offset added to the average to create the trigger point.
     * @param alpha             The smoothing factor (0.0 - 1.0) for the baseline average.
     */
    public HeartRateCalc(String label, double offsetThreshold, double alpha) {
        this.label = label;
        updateParameters(offsetThreshold, alpha);
    }

    /**
     * Updates the parameters of the heart rate calculation, allowing dynamic adjustment of the offset threshold and smoothing factor.
     * @param offsetThreshold
     * @param alpha
     */
    public void updateParameters(double offsetThreshold, double alpha) {
        this.offsetThreshold = offsetThreshold;
        this.alpha = alpha;
    }

    /**
     * Processes a new data point and updates the heart rate estimate by applying a 
     * dynamic threshold based on a running average of the signal.
     *
     * @param currentTime The timestamp of the current data point in milliseconds.
     * @param voltage     The raw voltage value of the current data point.
     * @return The current estimated Heart Rate in BPM.
     */
    public double processPoint(long currentTime, double voltage) {

        runningAverage = (alpha * voltage) + ((1 - alpha) * runningAverage);
        dynamicThreshold = runningAverage + offsetThreshold;

        if (voltage > dynamicThreshold && (currentTime - lastBeatTime) > 250) {
            
            if (lastBeatTime > 0) {
                long rrInterval = currentTime - lastBeatTime;
                
                if (rrInterval > 300 && rrInterval < 2000) { 
                    double instantHR = 60000.0 / rrInterval;
                    
                    currentHR = (currentHR == 0) ? instantHR : (currentHR * 0.7) + (instantHR * 0.3);
                }
            }
            lastBeatTime = currentTime;
        }
        
        return currentHR;
    }

    /** 
     * @return A string representing the current heart rate.
     */
    public String getStatus() {
        return String.format("%s: %.0f BPM", label, currentHR);
    }

    /**
     * Resets the internal state of the processor, clearing any stored beat times and heart rate estimates.
     */
    public void reset() {
        lastBeatTime = 0;
        currentHR = 0;
        dynamicThreshold = 0;
        runningAverage = 0;
    }


    /**
     * @return The current estimated Heart Rate in BPM.
     */
    public double getCurrentHR() {
        return currentHR;
    }
}
