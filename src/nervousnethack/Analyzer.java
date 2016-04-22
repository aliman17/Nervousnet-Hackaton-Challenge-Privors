package nervousnethack;

import java.util.ArrayList;
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
            ArrayList<Double> ge = new ArrayList<>();
            ArrayList<Double> le = new ArrayList<>();
            ArrayList<Double> div = new ArrayList<>();
            ArrayList<Double> ent = new ArrayList<>();
            int num = 20;
            
            LocalAnalyser analyzer = new LocalAnalyser();
            
            for(int i = 0; i < num; i++) {
                analyze(rawMap, outputMap);
                LocalAnalyser.Ranking r = analyzer.analyse(rawMap, outputMap);
                //double ge = calcGlobalError(rawMap, outputMap);
                ge.add(r.globalError);
                le.add(r.localError);
                div.add(r.diversity);
                ent.add(r.entropy);
                System.out.println(i + " " + r.entropy + "/" + r.diversity + "/" + r.localError + "/" + r.globalError);
            }
            double avgGe = ge.stream().reduce(0.0, (a,b) -> a+b) / num;
            double avgLe = le.stream().reduce(0.0, (a,b) -> a+b) / num;
            double avgDiv = div.stream().reduce(0.0, (a,b) -> a+b) / num;
            double avgEnt = ent.stream().reduce(0.0, (a,b) -> a+b) / num;
            double stdGe = Math.sqrt(ge.stream().reduce(0.0, (a,b) -> a+(b-avgGe)*(b-avgGe)) / (num-1));
            double stdLe = Math.sqrt(le.stream().reduce(0.0, (a,b) -> a+(b-avgLe)*(b-avgLe)) / (num-1));
            double stdDiv = Math.sqrt(div.stream().reduce(0.0, (a,b) -> a+(b-avgDiv)*(b-avgDiv)) / (num-1));
            double stdEnt = Math.sqrt(ent.stream().reduce(0.0, (a,b) -> a+(b-avgEnt)*(b-avgEnt)) / (num-1));
            System.out.println("Avg: " + avgEnt + "/" + avgDiv + "/" + avgLe + "/" + avgGe);
            System.out.println("Std: " + stdEnt + "/" + stdDiv + "/" + stdLe + "/" + stdGe);
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
