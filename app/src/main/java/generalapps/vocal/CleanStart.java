package generalapps.vocal;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.GainProcessor;

/**
 * Created by edeetee on 12/01/2017.
 */

public class CleanStart implements AudioProcessor {
    private double duration = 0.1D;
    private double firstTime = -1.0D;
    private double time;
    private GainProcessor gp = new GainProcessor(0.1D);
    private boolean isFadeIn = true;

    public CleanStart() {}

    public void stopFadeIn() {
        isFadeIn = false;
        firstTime = -1.0D;
    }

    public boolean process(AudioEvent var1) {
        if(isFadeIn) {
            if(firstTime == -1.0D) {
                firstTime = var1.getTimeStamp();
            }

            time = var1.getTimeStamp() - firstTime;
            if(1d < time/duration)
                stopFadeIn();
            gp.setGain(Math.min(time / duration, 1));
            gp.process(var1);
        }

        return true;
    }

    public void processingFinished() {
        gp.processingFinished();
    }
}