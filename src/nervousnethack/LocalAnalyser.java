package nervousnethack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import nervousnet.challenge.tags.Tags;

public class LocalAnalyser {

    private String basePath = "";
    private static final String bigInfoPath = "bigInfo/";
    private static final String smallInfoPath = "smallInfo/";
    private static final String rankingsPath = "rankings/";
    private static final String perDayPath = "perDay/";
    private static final String delimiter = ",";

    private HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> outputMap;
    private HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> rawMap;

    private HashMap<Integer, HashMap<Integer, Double>> counters = new HashMap<Integer, HashMap<Integer, Double>>();				// <User, <Day, Counter>>

    private HashMap<Integer, HashMap<Integer, HashMap<Integer, Ranking>>> perUserMap;	// <User, <Day, <Time, Ranking>>>
    private HashMap<Integer, HashMap<Integer, Ranking>> perEpochMap;						// <Day, <Time, Ranking>>
    private HashMap<Integer, Ranking> plotMap;											// <Time, Ranking>
    private HashMap<Integer, Ranking> perDaysMap;

    private Ranking rankings;

    public class Ranking {

        public double localError = 0;
        public double globalError = 0;
        public double entropy = 0;
        public double diversity = 0;

        public double raw;
        public double centroid;
        public double counter;
    }

    public Ranking analyse(HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> rawMap, HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> outputMap) {

        this.rawMap = rawMap;
        this.outputMap = outputMap;

        double m = comparator();

        perUserAnalysis();
        perEpochAnalysis();
        perDaysAnalysis();
        plotAnalysis();
        determineRankings();
        return rankings;
    }

    private double comparator() {
        for (Integer user : outputMap.keySet()) {
            counters.put(user, new HashMap<Integer, Double>());
            for (Integer day : outputMap.get(user).keySet()) {
                counters.get(user).put(day, 0.0);
                for (int val : outputMap.get(user).get(day).keySet()) {
                    if (outputMap.get(user).get(day).get(val) == rawMap.get(user).get(day).get(val)) {
                        double prev = counters.get(user).remove(day);
                        prev += 1;
                        counters.get(user).put(day, prev);
                    }
                }
            }
        }

        int counter = 0;

        for (Integer user : outputMap.keySet()) {
            for (Integer day : outputMap.get(user).keySet()) {
                double prev = counters.get(user).get(day);
                counter += prev;
                prev /= Tags.maxTime;
                counters.get(user).put(day, prev);
            }
        }

        double matchingPercentage = (double) counter / (double) (1000 * 2 * 28 * 48);
        return matchingPercentage * 100;
    }

    private void initperUserMap() {
        this.perUserMap = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Ranking>>>();
        try {
            for (Integer user : outputMap.keySet()) {
                perUserMap.put(user, new HashMap<Integer, HashMap<Integer, Ranking>>());
                for (Integer day : outputMap.get(user).keySet()) {
                    perUserMap.get(user).put(day, new HashMap<Integer, Ranking>());
                    for (int time = Tags.minTime; time <= Tags.maxTime; time++) {
                        perUserMap.get(user).get(day).put(time, new Ranking());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void perUserAnalysis() {
        this.initperUserMap();
        try {
            for (Integer user : outputMap.keySet()) {
                for (Integer day : outputMap.get(user).keySet()) {
                    ArrayList<Double> summValues = new ArrayList<>(outputMap.get(user).get(day).values());
                    double entropy = this.calculateEntropy(summValues);
                    double diversity = this.calculateRateOfChange(summValues);
                    for (int time = Tags.minTime; time <= Tags.maxTime; time++) {
                        double rawValue = rawMap.get(user).get(day).get(time);
                        double centroidValue = outputMap.get(user).get(day).get(time);
                        double localError = this.calculateRelError(rawValue, centroidValue);
                        Ranking rec = perUserMap.get(user).get(day).get(time);
                        rec.diversity = diversity;
                        rec.entropy = entropy;
                        rec.localError = localError;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initPerEpochMap() {
        this.perEpochMap = new HashMap<Integer, HashMap<Integer, Ranking>>();
        for (int day = Tags.winterStartDay; day <= Tags.winterEndDay; day++) {
            this.perEpochMap.put(day, new HashMap<Integer, Ranking>());
            for (int time = Tags.minTime; time <= Tags.maxTime; time++) {
                this.perEpochMap.get(day).put(time, new Ranking());
            }
        }
        for (int day = Tags.summerStartDay; day <= Tags.summerEndDay; day++) {
            this.perEpochMap.put(day, new HashMap<Integer, Ranking>());
            for (int time = Tags.minTime; time <= Tags.maxTime; time++) {
                this.perEpochMap.get(day).put(time, new Ranking());
            }
        }
    }

    private void perEpochAnalysis() {
        this.initPerEpochMap();
        for (int day = Tags.winterStartDay; day <= Tags.winterEndDay; day++) {
            for (int time = Tags.minTime; time <= Tags.maxTime; time++) {
                this.perEpochProcessing(day, time);
            }
        }
        for (int day = Tags.summerStartDay; day <= Tags.summerEndDay; day++) {
            for (int time = Tags.minTime; time <= Tags.maxTime; time++) {
                this.perEpochProcessing(day, time);
            }
        }
    }

    private void perEpochProcessing(Integer day, Integer time) {
        Ranking rec = this.perEpochMap.get(day).get(time);
        for (Integer user : rawMap.keySet()) {
            double localError = this.perUserMap.get(user).get(day).get(time).localError;
            double entropy = this.perUserMap.get(user).get(day).get(time).entropy;
            double diversity = this.perUserMap.get(user).get(day).get(time).diversity;

            rec.localError += localError;
            rec.diversity += diversity;
            rec.entropy += entropy;

            double rawValue = rawMap.get(user).get(day).get(time);
            double centroidValue = outputMap.get(user).get(day).get(time);

            rec.raw += rawValue;
            rec.centroid += centroidValue;
            rec.counter += 1;
        }
        rec.localError /= rec.counter;
        rec.entropy /= rec.counter;
        rec.diversity /= rec.counter;
        rec.globalError = this.calculateRelError(rec.raw, rec.centroid);
    }

    private void initPlotMap() {
        this.plotMap = new HashMap<Integer, Ranking>();
        for (int time = Tags.minTime; time <= Tags.maxTime; time++) {
            this.plotMap.put(time, new Ranking());
        }
    }

    private void plotAnalysis() {
        this.initPlotMap();
        for (int time = Tags.minTime; time <= Tags.maxTime; time++) {
            this.deepPlotAnalysis(time);
        }
    }

    private void deepPlotAnalysis(Integer time) {
        Ranking rec = this.plotMap.get(time);
        Iterator<Integer> days = this.perEpochMap.keySet().iterator();
        while (days.hasNext()) {
            Integer day = days.next();
            rec.globalError += this.perEpochMap.get(day).get(time).globalError;
            rec.localError += this.perEpochMap.get(day).get(time).localError;
            rec.entropy += this.perEpochMap.get(day).get(time).entropy;
            rec.diversity += this.perEpochMap.get(day).get(time).diversity;
            rec.counter += 1;
        }
        rec.globalError /= rec.counter;
        rec.localError /= rec.counter;
        rec.entropy /= rec.counter;
        rec.diversity /= rec.counter;
    }

    private void perDaysAnalysis() {
        this.perDaysMap = new HashMap<Integer, Ranking>();
        for (int day = Tags.winterStartDay; day <= Tags.winterEndDay; day++) {
            this.deepPerDaysAnalysis(day);
        }
        for (int day = Tags.summerStartDay; day <= Tags.summerEndDay; day++) {
            this.deepPerDaysAnalysis(day);
        }
    }

    private void deepPerDaysAnalysis(Integer day) {
        Ranking rec = new Ranking();
        for (int time = Tags.minTime; time <= Tags.maxTime; time++) {
            rec.localError += this.perEpochMap.get(day).get(time).localError;
            rec.globalError += this.perEpochMap.get(day).get(time).globalError;
            rec.entropy += this.perEpochMap.get(day).get(time).entropy;
            rec.diversity += this.perEpochMap.get(day).get(time).diversity;
            rec.counter += 1;
        }
        rec.localError /= rec.counter;
        rec.globalError /= rec.counter;
        rec.entropy /= rec.counter;
        rec.diversity /= rec.counter;
        this.perDaysMap.put(day, rec);
    }

    private void determineRankings() {
        rankings = new Ranking();
        int counter = 0;
        for (int time = Tags.minTime; time <= Tags.maxTime; time++) {
            rankings.entropy += this.plotMap.get(time).entropy;
            rankings.diversity += this.plotMap.get(time).diversity;
            rankings.localError += this.plotMap.get(time).localError;
            rankings.globalError += this.plotMap.get(time).globalError;
            counter += 1;
        }
        rankings.entropy /= counter;
        rankings.diversity /= counter;
        rankings.localError /= counter;
        rankings.globalError /= counter;
    }

    /**
     * Method that calculates entropy from the data set given in the array list.
     * Entropy is calculated by the formula H = - SUM ( -p * ln(p)/ln(2) );
     * where p is the possibility of occurring of each element, and SUM is
     * performed for every different value appearing.
     *
     * @param values - data set
     * @return entropy value
     */
    public Double calculateEntropy(ArrayList<Double> values) {
        HashMap<Double, Double> frequencies = new HashMap<Double, Double>();		// <value, counter>
        for (int i = 0; i < values.size(); i++) {
            if (!frequencies.containsKey(values.get(i))) {
                frequencies.put(values.get(i), 1.0);
            } else {
                double counter = frequencies.remove(values.get(i));
                counter++;
                frequencies.put(values.get(i), counter);
            }
        }

        Set<Double> rawValues = frequencies.keySet();
        double size = (double) values.size();
        Iterator<Double> iter = rawValues.iterator();
        double entropy = 0;
        while (iter.hasNext()) {
            Double raw = iter.next();
            double counter = (double) frequencies.get(raw);
            double p = counter / size;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }

    /**
     * Method that calculates diversity (rate of change) among the data set
     * given in an array list. Switch occurs only if current value in the data
     * set is different from the previous one. If there are n items in the list,
     * then n-1 changes occurred. Rate of change is quotient of switch and
     * change: Rate = SWITCH / CHANGE;
     *
     * @param values - data set
     * @return rate of change value (always <= 1)
     */
    public Double calculateRateOfChange(ArrayList<Double> values) {
        double rate = 0;
        double prev_value = values.get(0);

        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) != prev_value) {
                rate++;
            }
            prev_value = values.get(i);
        }

        double rateOfChange = rate / (values.size() - 1);
        return rateOfChange;
    }

    /**
     * Calculates relative error by formula: ERROR = | 1 - CENTROID / RAW | If
     * raw value is 0.0, then it returns 0.
     *
     * @param raw - correct value
     * @param centroid - estimated value
     * @return relative error if raw != 0, returns 0 otherwise
     */
    public double calculateRelError(double raw, double centroid) {
        if (raw == 0.0) {
            return 0;
        } else {
            double er = Math.abs(1 - centroid / raw);
//			if(er > 1) {
//				er = 1;
//			}
            return er;
        }
    }

    private String prepHeaderBigInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("day" + delimiter);
        sb.append("time" + delimiter);
        sb.append("average_local_error" + delimiter);
        sb.append("global_error" + delimiter);
        sb.append("entropy" + delimiter);
        sb.append("diversity" + delimiter);
        return sb.toString();
    }

    private String prepHeaderSmallInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("time" + delimiter);
        sb.append("average_local_error" + delimiter);
        sb.append("global_error" + delimiter);
        sb.append("entropy" + delimiter);
        sb.append("diversity" + delimiter);
        return sb.toString();
    }

    private String prepBigInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(prepHeaderBigInfo());
        sb.append(System.lineSeparator());
        for (int day = Tags.winterStartDay; day <= Tags.winterEndDay; day++) {
            for (int time = Tags.minTime; time <= Tags.maxTime; time++) {
                sb.append(day + delimiter + time + delimiter);
                sb.append(this.perEpochMap.get(day).get(time).localError + delimiter);
                sb.append(this.perEpochMap.get(day).get(time).globalError + delimiter);
                sb.append(this.perEpochMap.get(day).get(time).entropy + delimiter);
                sb.append(this.perEpochMap.get(day).get(time).diversity + delimiter);
                sb.append(System.lineSeparator());
            }

        }
        for (int day = Tags.summerStartDay; day <= Tags.summerEndDay; day++) {
            for (int time = Tags.minTime; time <= Tags.maxTime; time++) {
                sb.append(day + delimiter + time + delimiter);
                sb.append(this.perEpochMap.get(day).get(time).localError + delimiter);
                sb.append(this.perEpochMap.get(day).get(time).globalError + delimiter);
                sb.append(this.perEpochMap.get(day).get(time).entropy + delimiter);
                sb.append(this.perEpochMap.get(day).get(time).diversity + delimiter);
                sb.append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    private String prepSmallInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(prepHeaderSmallInfo());
        sb.append(System.lineSeparator());
        for (int time = Tags.minTime; time <= Tags.maxTime; time++) {
            sb.append(time + delimiter);
            sb.append(this.plotMap.get(time).localError + delimiter);
            sb.append(this.plotMap.get(time).globalError + delimiter);
            sb.append(this.plotMap.get(time).entropy + delimiter);
            sb.append(this.plotMap.get(time).diversity + delimiter);
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    private String prepPerDayInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(prepHeaderSmallInfo());
        sb.append(System.lineSeparator());
        for (int day = Tags.winterStartDay; day <= Tags.winterEndDay; day++) {
            sb.append(day + delimiter);
            sb.append(this.perDaysMap.get(day).localError + delimiter);
            sb.append(this.perDaysMap.get(day).globalError + delimiter);
            sb.append(this.perDaysMap.get(day).entropy + delimiter);
            sb.append(this.perDaysMap.get(day).diversity + delimiter);
            sb.append(System.lineSeparator());
        }
        for (int day = Tags.summerStartDay; day <= Tags.summerEndDay; day++) {
            sb.append(day + delimiter);
            sb.append(this.perDaysMap.get(day).localError + delimiter);
            sb.append(this.perDaysMap.get(day).globalError + delimiter);
            sb.append(this.perDaysMap.get(day).entropy + delimiter);
            sb.append(this.perDaysMap.get(day).diversity + delimiter);
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
    
}
