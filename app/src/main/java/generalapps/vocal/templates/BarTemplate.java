package generalapps.vocal.templates;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import generalapps.vocal.Audio;
import generalapps.vocal.Rhythm;

/**
 * Created by edeetee on 1/07/2016.
 */
public class BarTemplate {

    public int mRecordingLength;
    public boolean[] mEnabledBars;

    public BarTemplate(boolean... enabledBars){
        mRecordingLength = Rhythm.maxBars/enabledBars.length;
        mEnabledBars = enabledBars;
    }

    public boolean isPlaying(int beats){
        return mEnabledBars[(beats % Rhythm.maxBeats())/Rhythm.bpb/mRecordingLength];
    }

    public boolean isStartOfBar(int beats){
        return (beats % Rhythm.maxBeats()) % (mRecordingLength*Rhythm.bpb) == 0;
    }

    public void serialize(Audio.MetaData meta){
        meta.barTemplateIndex = list.indexOf(this);
    }

    public static BarTemplate deSerialize(Audio.MetaData meta){
        return list.get(meta.barTemplateIndex);
    }

    public static BarTemplate defaultFromBars(int barLength){
        if(barLength == 1)
            return list.get(2);
        else if(barLength == 2)
            return list.get(1);
        else
            return list.get(0);
    }

    public static List<BarTemplate> list = new ArrayList<>(Arrays.asList(
            new BarTemplate(true),
            new BarTemplate(true, true),
            new BarTemplate(true, true, true, true),
            new BarTemplate(true, false, true, false),
            new BarTemplate(true, true, true, false),
            new BarTemplate(false, true, true, true),
            new BarTemplate(false, false, false, true),
            new BarTemplate(true, false, false, false),
            new BarTemplate(false, true, true, false),
            new BarTemplate(true, false)
    ));
}