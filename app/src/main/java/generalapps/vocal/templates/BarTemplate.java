package generalapps.vocal.templates;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import generalapps.vocal.Rhythm;

/**
 * Created by edeetee on 1/07/2016.
 */
public class BarTemplate {

    public int mRecordingLength;
    public boolean[] mEnabledBars;

    public BarTemplate(int recordingLength, boolean... enabledBars){
        if(enabledBars.length == Rhythm.maxBars/recordingLength){
            mRecordingLength = recordingLength;
            mEnabledBars = enabledBars;
        } else {
            Log.e("BarTemplate", "Incorrect parameters. enabledBars.length != Rhythm.maxBars/recordingLength");
        }
    }

    public boolean shouldPlay(int beats){
        return beats % (mRecordingLength*Rhythm.bpb) == 0 && mEnabledBars[beats/Rhythm.bpb/mRecordingLength];
    }

    public JSONObject serialize(){
        try{
            JSONObject obj = new JSONObject();
            obj.put("index", list.indexOf(this));
            return obj;
        } catch(JSONException e){
            Log.e("Effect", "serialize failed", e);
            return null;
        }
    }

    public static BarTemplate deSerialize(JSONObject obj){
        try{
            return list.get(obj.getInt("index"));
        } catch(JSONException e){
            Log.e("Effect", "serialize failed", e);
            return null;
        }
    }

    public static List<BarTemplate> list = new ArrayList<>(Arrays.asList(
            new BarTemplate(1, true, true, true, true),
            new BarTemplate(1, true, false, true, false),
            new BarTemplate(1, true, true, true, false),
            new BarTemplate(1, false, true, true, true),
            new BarTemplate(2, true, false)
    ));
}
