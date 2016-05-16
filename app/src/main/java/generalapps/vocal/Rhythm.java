package generalapps.vocal;

/**
 * Created by edeetee on 19/04/2016.
 */
public final class Rhythm {
    public static int maxBars = 4;
    public static int bpb = 4;
    public static int[] barTypes = {1,2,4};

    public static int maxBeats(){
        return bpb*maxBars;
    }
}
