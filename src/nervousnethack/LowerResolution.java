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

/**
 *
 * @author Peter
 */
public class LowerResolution extends Analyzer {
    
    double targetStd = 0.05;
    double threshold = 0.4;

    @Override
    public void analyze(HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> in, HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> out) throws Exception {
        int numUser = in.size();
        double std = Math.sqrt(numUser)*targetStd;
        Random r = new Random();

        for (int user : in.keySet()) {
            LinkedHashMap<Integer, LinkedHashMap<Integer, Double>> days = in.get(user);
            
            double bias = r.nextGaussian()*std;
            
            for (int day : days.keySet()) {
                LinkedHashMap<Integer, Double> vals = days.get(day);
                
                ArrayList<Double> arr = new ArrayList<>(vals.values());
                
                double sum = 0;
                int num = 0;
                for(int i = 0; i < arr.size(); i++) {
                    double hypotheticalAvg = (sum + arr.get(i))/(num+1);
                    if(Math.abs(arr.get(i) - hypotheticalAvg)/Math.abs(hypotheticalAvg) > threshold) {
                    //if(Math.abs(arr.get(i) - (sum + arr.get(i))/(num+1))/Math.abs(arr.get(i)) > threshold) {
                    //if((sum + arr.get(i))/(num+1) > threshold) {
                        double avg = sum/num;
                        for(int j = i - num; j < i; j++) {
                            arr.set(j, avg);
                        }
                        sum = arr.get(i);
                        num = 1;
                    } else {
                        sum += arr.get(i);
                        num++;
                    }
                }
                if(num > 0) {
                    for(int j = arr.size() - num; j < arr.size(); j++) {
                        arr.set(j, sum/num);
                    }
                }
                
                int idx = 0;
                for(int val : vals.keySet()) {
                    out.get(user).get(day).put(val, arr.get(idx) + bias);
                    idx++;
                }
            }
        }
    }

}
