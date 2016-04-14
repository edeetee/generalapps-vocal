package generalapps.vocal;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.ProgressBar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by edeetee on 13/04/2016.
 */
public class Audio extends AudioTrack {
    String name;
    private String file;
    int maxBeats;
    int beats;
    View view;

    public Audio(int maxBeats, String name){
        super(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, 500000, AudioTrack.MODE_STATIC);

        this.name = name;
        beats = 0;
        this.maxBeats = maxBeats;

        setPlaybackPositionUpdateListener(new OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack track) {
                stop();
            }

            @Override
            public void onPeriodicNotification(AudioTrack track) {

            }
        });
    }

    public Audio(int maxBeats, String name, String audioFile){
        this(maxBeats, name);
        setFile(audioFile);
    }

    public void setFile(String audioFile){
        file = audioFile;
        File file= new File(audioFile);
        int length = (int)file.length();
        byte[] bytes = new byte[length];
        short[] shortBuf = new short[length/2];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, length);
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(int i = 0; i<length/2; i++){
            shortBuf[i] = ( (short)( ( bytes[i*2] & 0xff )|( bytes[i*2 + 1] << 8 ) ) );
        }
        write(shortBuf, 0, length/2);
        setNotificationMarkerPosition(length/2);
    }

    @Override
    public String toString(){
        return name;
    }

    public boolean isPlaying(){
        return getPlayState() == PLAYSTATE_PLAYING;
    }

    public void PlayStop(){
        if(file != null){
            if(isPlaying()){
                stop();
            } else
                play();
        }
    }
}
