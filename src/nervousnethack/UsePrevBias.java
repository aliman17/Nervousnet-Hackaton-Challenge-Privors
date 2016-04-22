/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nervousnethack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;
import nervousnet.challenge.CalendarCalculator;
import nervousnet.challenge.tags.DAYS;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author Peter
 */
public class UsePrevBias extends Analyzer {
    
    double targetStd = 0.05;

    @Override
    public void analyze(HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> in, HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> out) throws Exception {
        int numUser = in.size();
        double std = Math.sqrt(numUser)*targetStd;
        Random r = new Random();

        for (int user : in.keySet()) {
            LinkedHashMap<Integer, LinkedHashMap<Integer, Double>> days = in.get(user);
            
            double bias = r.nextGaussian()*std;
            int prevWorkDay = -1;
            int prevWeekend = -1;
            for (int day : days.keySet()) {
                LinkedHashMap<Integer, Double> vals = days.get(day);
                
                if(isWorkDay(day)) {
                    if(prevWorkDay >= 0 && r.nextBoolean() && true) {
                        vals = days.get(prevWorkDay);
                    }
                    prevWorkDay = day;
                } else {
                    if(prevWeekend >= 0 && r.nextBoolean() && true) {
                        vals = days.get(prevWeekend);
                    }
                    prevWeekend = day;
                }
                
                ArrayList<Double> x = new ArrayList<>(vals.values());
                int idx = 0;
                for(int val : vals.keySet()) {
                    out.get(user).get(day).put(val, x.get(idx) + bias);
                    idx++;
                }
            }
        }
    }

    private boolean isWorkDay(int day) {
        DAYS dow = CalendarCalculator.calculateDayOfWeek(day);
        return !(dow == DAYS.SATURDAY && dow == DAYS.SUNDAY);
    }
}
