package generalapps.vocal;

import android.media.AudioManager;
import android.media.MediaPlayer;

import java.io.IOException;

/**
 * Created by edeetee on 13/04/2016.
 */
public class Audio extends MediaPlayer {
    public String name;

    public Audio(String audioFile){
        name = audioFile;
        setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            setDataSource(audioFile);
            prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString(){
        return name;
    }

    public void PlayStop(){
        if(isPlaying()){
            stop();
            try {
                prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else
            start();
    }
}
