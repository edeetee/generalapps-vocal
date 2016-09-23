package generalapps.vocal;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

/**
 * Created by edeetee on 5/08/2016.
 */
public class BitCrusher implements AudioProcessor {
    int downsampling;
    int bitDepthFactor;

    /**
     *
     * @param downsampling how many samples to combine
     * @param bitDepthFactor how much the bit depth should be decreased
     */
    public BitCrusher(int downsampling, int bitDepthFactor){
        this.downsampling = downsampling;
        this.bitDepthFactor = bitDepthFactor;
    }

    @Override
    public boolean process(AudioEvent audioEvent) {
        float[] buffer = audioEvent.getFloatBuffer();
        for(int i = 0; i < buffer.length/downsampling; i++){
            if(downsampling != 1){
                float average = 0;
                for(int k = 0; k < downsampling; k++){
                    average += (int)(buffer[i*downsampling+k]*bitDepthFactor);
                }
                average /= downsampling*bitDepthFactor;
                for(int k = 0; k < downsampling; k++){
                    buffer[i*downsampling+k] = average;
                }
            } else{
                for(int k = 0; k < downsampling; k++){
                    buffer[i*downsampling+k] = ((float)((int) (buffer[i*downsampling+k]*bitDepthFactor)))/bitDepthFactor;
                }
            }
        }
        return true;
    }

    @Override
    public void processingFinished() {

    }
}
