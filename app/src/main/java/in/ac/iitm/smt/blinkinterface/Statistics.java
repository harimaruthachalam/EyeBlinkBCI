package in.ac.iitm.smt.blinkinterface;

import java.util.List;

public abstract class Statistics {

    public static int getMean(List<Integer> data) {
        int size = data.size();
        double sum = 0.0;
        for (double a : data)
            sum += a;
        return (int) (sum / size);
    }

    public static int getVariance(List<Integer> data) {
        int size = data.size();
        double mean = getMean(data);
        double temp = 0;
        for (double a : data)
            temp += (a - mean) * (a - mean);
        return (int) (temp / (size - 1));
    }

    public static int getStdDev(List<Integer> data) {
        return (int) (Math.sqrt(getVariance(data)));
    }
}
