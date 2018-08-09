package in.ac.iitm.smt.blinkinterface;

import java.util.LinkedList;
import java.util.Queue;

public class MovingAverage {
    private final Queue<Integer> window = new LinkedList();
    private final int period;
    private double sum;

    public MovingAverage(int period) {
        assert period > 0 : "Period must be a positive integer";
        this.period = period;
    }

    public void add(Integer num) {
        sum += num;
        window.add(num);
        if (window.size() > period) {
            sum -= window.remove();
        }
    }

    public int getAvg() {
        if (window.isEmpty()) return 0; // technically the average is undefined
        return (int) (sum / window.size());
    }
}