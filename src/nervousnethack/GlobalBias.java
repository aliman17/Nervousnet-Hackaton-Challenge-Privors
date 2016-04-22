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
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author Peter
 */
public class GlobalBias extends Analyzer {
    
    double targetStd = 0.05;
    public int numOfClusters = 4;

    private ArrayList<Double> kMeans(ArrayList<Double> raws, int numOfClusters) throws Exception {
        FastVector atts = new FastVector();
        atts.addElement(new Attribute("NN"));

        Instances kmeans = new Instances("kmeans", atts, 0);
        for (Double raw : raws) {
            double vals[] = new double[kmeans.numAttributes()];
            vals[0] = raw;
            kmeans.add(new Instance(1.0, vals));
        }

        SimpleKMeans kMeansAlgo = new SimpleKMeans();
        kMeansAlgo.setSeed(10);
        kMeansAlgo.setPreserveInstancesOrder(true);
        kMeansAlgo.setNumClusters(numOfClusters);
        kMeansAlgo.buildClusterer(kmeans);

        int[] assignments = kMeansAlgo.getAssignments();
        Instances centroids = kMeansAlgo.getClusterCentroids();

        ArrayList<Double> centroidList = new ArrayList<Double>();
        for (int i = 0; i < assignments.length; i++) {
            double rawValue = kmeans.instance(i).value(0);
            double centroidValue = centroids.instance(assignments[i]).value(0);
            centroidList.add(centroidValue);
        }

        return centroidList;
    }

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
                
                ArrayList<Double> x = kMeans(new ArrayList<>(vals.values()), numOfClusters);
                int idx = 0;
                for(int val : vals.keySet()) {
                    out.get(user).get(day).put(val, x.get(idx) + bias);
                    idx++;
                }
            }
        }
    }

}
