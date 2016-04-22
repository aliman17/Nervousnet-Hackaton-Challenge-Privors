package nervousnethack;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import nervousnet.challenge.Dumper;
import nervousnet.challenge.Loader;
import nervousnet.challenge.exceptions.IllegalHashMapArgumentException;
import nervousnet.challenge.exceptions.MissingDataException;
import nervousnet.challenge.exceptions.MissingFileException;
import nervousnet.challenge.exceptions.NullArgumentException;

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

    public abstract void analyze(HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> in, HashMap<Integer, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>> out)  throws Exception;
}
