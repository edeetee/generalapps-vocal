package generalapps.vocal;

/**
 * Created by edeetee on 19/04/2016.
 */
public final class Rhythm {
    public static int bpm = 100;
    public static int maxBars = 4;
    public static int bpb = 4;

    public static int[] barTypes = {1,2,4};

    public static int maxBeats(){
        return bpb*maxBars;
    }

    public static int msBeatPeriod(){
        return (1000*60)/bpm;
    }

    public static int msBarPeriod(){
        return msBeatPeriod()*bpb;
    }

    public static int msMaxPeriod(){
        return msBarPeriod()*maxBars;
    }

    public static int maxTicks(){
        return msMaxPeriod()*Recorder.FREQ/1000;
    }
}
