package electronicsProject.classes;

public class BandpassFilter {
    private double[] x1, x2, y1, y2;
    private double b0, b1, b2, a1, a2;
    private double low, high, fs, gain;

    /**
     * Initializes a new BandpassFilter with the specified low and high cutoff frequencies and sampling rate.
     * @param lowCutoff Lower bound in Hz
     * @param highCutoff Upper bound in Hz
     * @param samplingRate Sampling rate in Hz
     * @param gain The gain of the filter
     */
    public BandpassFilter(double lowCutoff, double highCutoff, double samplingRate, double gain) {
        updateFilter(lowCutoff, highCutoff, samplingRate, gain);
    }

    /**
     * Calculates the filter coefficients for a bandpass Butterworth filter based on the given low and high cutoff frequencies and the sampling rate.
     * @param low Lower cutoff frequency in Hz
     * @param high Upper cutoff frequency in Hz
     * @param fs Sampling rate in Hz
     * @param g The gain of the filter
     */
    public void updateFilter(double low, double high, double fs, double g) {
        reset();
        double wo = 2 * Math.PI * Math.sqrt(low * high) / fs;
        double bw = 2 * Math.PI * (high - low) / fs;

        double alpha = Math.sin(wo) * Math.sinh(Math.log(2) / 2 * bw * wo / Math.sin(wo));
        
        double a0 = 1 + alpha;
        this.b0 = alpha / a0;
        this.b1 = 0 / a0;
        this.b2 = -alpha / a0;
        this.a1 = -2 * Math.cos(wo) / a0;
        this.a2 = (1 - alpha) / a0;

        this.low = low;
        this.high = high;
        this.fs = fs;
        this.gain = g;
    }

    /**
     * Applies the bandpass filter to a new input sample and returns the filtered output.
     * @param inputs The new input samples.
     * @return The filtered output.
     */
    public double[] filter(double[] inputs) {
        double[] outputs = new double[0];
        for (int i = 0; i < inputs.length; i++) {
            double input = inputs[i];
            outputs[i] = b0 * input + b1 * x1[i] + b2 * x2[i] - a1 * y1[i] - a2 * y2[i];

            x2[i] = x1[i];
            x1[i] = inputs[i];
            y2[i] = y1[i];
            y1[i] = outputs[i];

            outputs[i] = outputs[i]*gain;
        }

        return outputs;
    }

    /**
     * Applies the bandpass filter to a new input sample and returns the filtered output.
     * @param input The new input sample.
     * @return The filtered output.
     */
    public double filter(double input) {
        double output = b0 * input + b1 * x1[0] + b2 * x2[0] - a1 * y1[0] - a2 * y2[0];

        x2[0] = x1[0];
        x1[0] = input;
        y2[0] = y1[0];
        y1[0] = output;

        return output * gain;
    }

    /**
     * Resets the internal state of the filter, clearing any stored past input and output values.
     */
    public void reset() {
        x1 = new double[10];
        x2 = new double[10];
        y1 = new double[10];
        y2 = new double[10];
    }

    /**
     * Resets the filter and updates it for the new sampling rate
     * @param samplingRate Sampling rate in Hz
     */
    public void updateSamplingRate(double samplingRate) {
        updateFilter(low, high, samplingRate, gain);
    }

    /**
     * Resets the filter and updates it for the new lower bound
     * @param lowCutoff Lower bound in Hz
     */
    public void updateLowCutoff(double lowCutoff) {
        updateFilter(lowCutoff, high, fs, gain);
    }

    /**
     * Resets the filter and updates it for the new higher bound
     * @param highCutoff Higher bound in Hz
     */
    public void updateHighCutoff(double highCutoff) {
        updateFilter(low, highCutoff, fs, gain);
    }

    /**
     * Resets the filter and updates it for the new gain
     * @param gain The gain of the filter
     */
    public void updateGain(double gain) {
        updateFilter(low, high, fs, gain);
    }
}
