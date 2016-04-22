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
import nervousnet.challenge.Dumper;
import nervousnet.challenge.exceptions.IllegalHashMapArgumentException;
import nervousnet.challenge.exceptions.MissingDataException;
import nervousnet.challenge.exceptions.MissingFileException;
import nervousnet.challenge.exceptions.NullArgumentException;
import nervousnet.challenge.tags.Tags;

/**
 *
 * @author Peter
 */
public class LocalBias extends Analyzer {
    
    double targetStd = 0.005;

    @Override
    public void analyze(HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> in, HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> out) throws IllegalHashMapArgumentException, NullArgumentException {
        int numUser = in.size();
        double std = targetStd*Math.sqrt(numUser);
        Random r = new Random();

        for (int user : in.keySet()) {
            LinkedHashMap<Integer, LinkedHashMap<Integer, Double>> days = in.get(user);
            for (int day : days.keySet()) {
                LinkedHashMap<Integer, Double> vals = days.get(day);
                for (int val : vals.keySet()) {
                    double bias = r.nextGaussian()*std;
                    out.get(user).get(day).put(val, vals.get(val) + bias);
                }
            }
        }
    }

}
