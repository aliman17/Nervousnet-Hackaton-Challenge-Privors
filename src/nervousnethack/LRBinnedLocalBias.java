/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nervousnethack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Peter
 */
public class LRBinnedLocalBias extends Analyzer {
    
    double targetStd = 0.07;
    double threshold = 0.56;
    int numBins = 1; // not used in final submission

    @Override
    public void analyze(HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> in, HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> out) throws Exception {
        int numUser = in.size();
        double std = Math.sqrt(numUser)*targetStd;
        Random r = new Random();

        for (int user : in.keySet()) {
            LinkedHashMap<Integer, LinkedHashMap<Integer, Double>> days = in.get(user);
            
            double halfedStd = std*Math.sqrt(2)/5;
            double bias = r.nextGaussian()*halfedStd*4;
            int nonZeroIdx = r.nextInt(numBins);
            
            for (int day : days.keySet()) {
                LinkedHashMap<Integer, Double> vals = days.get(day);
                
                List<Double> arr = new ArrayList<>(vals.values());
                int start;
                int end;
                
                // divide data (not used in final submission)
                start = (arr.size()/numBins)*nonZeroIdx;
                end = (arr.size()/numBins)*(nonZeroIdx+1);
                double zeroBias = r.nextGaussian()*halfedStd;
                for(int i = 0; i < start; i++) {
                    out.get(user).get(day).put(i+1, zeroBias);
                }
                for(int i = start; i < end; i++) {
                    arr.set(i, numBins*arr.get(i));
                }
                zeroBias = r.nextGaussian()*halfedStd;
                for(int i = end; i < arr.size(); i++) {
                    out.get(user).get(day).put(i+1, zeroBias);
                }
                
                arr = arr.subList(start, end);
                
                // segment the signal + add local bias
                double sum = 0;
                int num = 0;
                for(int i = 0; i < arr.size(); i++) {
                    double hypotheticalAvg = (sum + arr.get(i))/(num+1);
                    if(Math.abs(arr.get(i) - hypotheticalAvg)/Math.abs(hypotheticalAvg) > threshold) {
                        double avg = sum/num;
                        double localBias = r.nextGaussian()*halfedStd;
                        for(int j = i - num; j < i; j++) {
                            arr.set(j, avg + localBias);
                        }
                        sum = arr.get(i);
                        num = 1;
                    } else {
                        sum += arr.get(i);
                        num++;
                    }
                }
                if(num > 0) {
                    double avg = sum/num;
                    double localBias = r.nextGaussian()*halfedStd;
                    for(int j = arr.size() - num; j < arr.size(); j++) {
                        arr.set(j, avg + localBias);
                    }
                }
                
                // add global bias
                for(int i = 0; i < arr.size(); i++) {
                    out.get(user).get(day).put(i+start+1, arr.get(i) + bias);
                }
            }
        }
    }

}
