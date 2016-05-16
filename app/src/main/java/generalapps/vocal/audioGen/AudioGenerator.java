package generalapps.vocal.audioGen;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class AudioGenerator {

    private AudioTrack audioTrack;

    public AudioGenerator(double toneFreq, int samples, int sampleFreq) {
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleFreq, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, sampleFreq,
                AudioTrack.MODE_STATIC);

        byte[] generatedSnd = get16BitPcm(getSineWave(samples, sampleFreq, toneFreq));
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
    }

    private double[] getSineWave(int samples,int sampleRate,double frequencyOfTone){
        double[] sample = new double[samples];
        for (int i = 0; i < samples; i++) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/frequencyOfTone));
        }
        return sample;
    }

    private byte[] get16BitPcm(double[] samples) {
        byte[] generatedSound = new byte[2 * samples.length];
        int index = 0;
        for (double sample : samples) {
            // scale to maximum amplitude
            short maxSample = (short) ((sample * Short.MAX_VALUE));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSound[index++] = (byte) (maxSample & 0x00ff);
            generatedSound[index++] = (byte) ((maxSample & 0xff00) >>> 8);

        }
        return generatedSound;
    }

    public void play(){
        audioTrack.stop();
        audioTrack.setPlaybackHeadPosition(0);
        audioTrack.play();
    }

    public void destroyAudioTrack() {
        audioTrack.stop();
        audioTrack.release();
    }

}