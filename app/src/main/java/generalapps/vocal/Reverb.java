package generalapps.vocal;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

/**
 * Created by edeetee on 5/08/2016.
 */
public class Reverb implements AudioProcessor {
    float decay;
    float[] reverb;
    int position = 0;

    public Reverb(float length, float decay, double sampleRate){
        reverb = new float[(int)(length*sampleRate)];
        this.decay = decay;
    }

    @Override
    public boolean process(AudioEvent audioEvent) {
        float[] audioFloatBuffer = audioEvent.getFloatBuffer();
        int overlap = audioEvent.getOverlap();

        for(int i = overlap ; i < audioFloatBuffer.length ; i++){
            if(position >= reverb.length){
                position = 0;
            }

            //output is the input added with the decayed echo
            audioFloatBuffer[i] = audioFloatBuffer[i] + reverb[position] * decay;
            //store the sample in the buffer;
            reverb[position] = audioFloatBuffer[i];

            position++;
        }
        return true;
    }

    @Override
    public void processingFinished() {

    }
}
