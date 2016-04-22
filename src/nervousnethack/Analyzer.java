package nervousnethack;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import nervousnet.challenge.Dumper;
import nervousnet.challenge.Loader;

public abstract class Analyzer {

    Loader loader = new Loader();
    Dumper dumper = new Dumper();

    public final void analyze() {
        try {
            HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> rawMap = loader.exportRawValues();
            HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> outputMap = Dumper.initOutputMap();
            analyze(rawMap, outputMap);
            dumper.dump(outputMap);
        } catch (Exception ex) {
            Logger.getLogger(Analyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public final void analyzeLocally() {
        try {
            HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> rawMap = loader.exportRawValues();
            HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> outputMap = Dumper.initOutputMap();
            double sumGE = 0;
            double sumLE = 0;
            int num = 20;
            
            LocalAnalyser analyzer = new LocalAnalyser();
            
            for(int i = 0; i < num; i++) {
                analyze(rawMap, outputMap);
                LocalAnalyser.Ranking r = analyzer.analyse(rawMap, outputMap);
                //double ge = calcGlobalError(rawMap, outputMap);
                sumGE += r.globalError;
                sumLE += r.localError;
                System.out.println(i + " " + r.localError + "/" + r.globalError);
            }
            System.out.println("Avg: " + sumLE/num + "/" + sumGE/num);
        } catch (Exception ex) {
            Logger.getLogger(Analyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public double calcGlobalError(HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> in, HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> out) {
        double sum = 0;
        int num = 0;
        
        for(int user : in.keySet()) {
            for(int day : in.get(user).keySet()) {
                double inSum = in.get(user).get(day).values().stream().reduce(0.0, (a,b) -> a+b);
                double outSum = out.get(user).get(day).values().stream().reduce(0.0, (a,b) -> a+b);
                double globalError = Math.abs(inSum-outSum)/Math.abs(inSum);
                if(!Double.isFinite(globalError)) {
                    globalError = 0;
                }
                sum += globalError;
                num++;
            }
        }
        
        return sum/num;
    }
    public double calcLocalError(HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> in, HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> out) {
        double sum = 0;
        int num = 0;
        
        HashMap<Integer,Double> userError = new HashMap<>();
        
        for(int user : in.keySet()) {
            for(int day : in.get(user).keySet()) {
                double inSum = in.get(user).get(day).values().stream().reduce(0.0, (a,b) -> a+b);
                double outSum = out.get(user).get(day).values().stream().reduce(0.0, (a,b) -> a+b);
                double globalError = Math.abs(inSum-outSum)/Math.abs(inSum);
                if(!Double.isFinite(globalError)) {
                    globalError = 0;
                }
                sum += globalError;
                num++;
            }
        }
        
        return sum/num;
    }

    public abstract void analyze(HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> in, HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> out)  throws Exception;
}
