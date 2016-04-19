package generalapps.vocal;

/**
 * Created by edeetee on 19/04/2016.
 */
public final class Rhythm {
    public static int bpm = 100;
    public static int bpb = 4;
    public static int bars = 1;

    public static int totalBeats(){
        return bpb*bars;
    }

    public static int posInBar(int beat){
        return beat%bpb;
    }

    public static int msBeatPeriod(){
        return (1000*60)/bpm;
    }

    public static int msBarPeriod(){
        return msBeatPeriod()*bpb;
    }

    public static int msTotalPeriod(){
        return msBarPeriod()*bars;
    }
}