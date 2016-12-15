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
 * Created by edeetee on 14/07/2016.
 */
public class GroupTemplate {
    public GroupTemplate(boolean... enabledGroups){
        if(enabledGroups.length != 4)
            Log.e("GroupTemplate", "enabledGroups length should be 4, is " + enabledGroups.length);
        mEnabledGroups = enabledGroups;
    }

    public boolean[] mEnabledGroups;

    public boolean isPlaying(int beat){
        return mEnabledGroups[(beat / Rhythm.maxBeats()) % 4];
    }

    public void serialize(Audio.MetaData meta){
        //meta.groupTemplateIndex = list.indexOf(this);
    }

    public static GroupTemplate deSerialize(Audio.MetaData meta){
        //return list.get(meta.groupTemplateIndex);
        return null;
    }

    public static List<GroupTemplate> list = new ArrayList<>(Arrays.asList(
            new GroupTemplate(true, true, true, true),
            new GroupTemplate(true, false, true, false),
            new GroupTemplate(false, false, false, true)
    ));
}
