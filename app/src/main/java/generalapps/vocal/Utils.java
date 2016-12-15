package generalapps.vocal;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.graphics.ColorUtils;
import android.util.Log;
import android.util.LogPrinter;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageReference;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by edeetee on 12/05/2016.
 */
public class Utils {
    public static boolean deleteDirectory(File directory) {
        if(directory.exists()){
            File[] files = directory.listFiles();
            if(null!=files){
                for(int i=0; i<files.length; i++) {
                    if(files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    }
                    else {
                        files[i].delete();
                    }
                }
            }
        }
        return(directory.delete());
    }

    public static void printFile(String tag, File file) throws IOException{
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while(true) {
            line = br.readLine();
            if (line == null)
                break;
            else
                Log.i(tag, line);
        }
        br.close();
    }

    static int getInnerViewPosition(View v){
        ViewParent parent = v.getParent();
        while(parent != null){
            if(parent.getClass().equals(ListView.class))
                break;
            parent = parent.getParent();
        }
        ListView listView = (ListView)parent;
        return listView.getPositionForView(v);
    }



    public static float getDpToPxMod(Context context) {
        return context.getResources().getDisplayMetrics().densityDpi / 160f;
    }

    public static JSONObject loadJSON(File file, String debugTAG) {
        String jsonString = null;
        byte[] buffer;
        try {
            InputStream is = new FileInputStream(file);
            int size = is.available();
            buffer = new byte[size];
            is.read(buffer);
            is.close();
            jsonString = new String(buffer, "UTF-8");
            if(debugTAG != null)
                Log.i("loadJSON", debugTAG + jsonString);
            return new JSONObject(jsonString);
        } catch (Exception e) {
            Log.e("Utils", "loadJSON failed: \"" + jsonString + "\"", e);
            return null;
        }
    }

    public static JSONObject loadJSON(File file) {
        return loadJSON(file, null);
    }

    public static void deleteTrack(Track.MetaData trackMeta){
        MainActivity.database.getReference("meta").child("tracks").child(trackMeta.key).removeValue();
        DatabaseReference audiosRef = MainActivity.database.getReference("meta").child("audios");
        StorageReference cloudAudio = MainActivity.storageRef.child("audios");
        for (String audio : trackMeta.audios.keySet()) {
            audiosRef.child(audio).removeValue();
            cloudAudio.child(audio + ".wav").delete();
        }
    }

    /**
     *
     * @param max max number exclusive
     * @return random number 0 - max(exclusive)
     */
    static int randomInt(int max){
        return (int)(Math.random()*max);
    }

    /**
     *
     * @param min min number incluside
     * @param max max number exclusive
     * @return random number min(inclusive) - max(exclusive)
     */
    static int randomInt(int min, int max){
        return randomInt(max-min)+min;
    }

    static <E extends Enum<E>> boolean isEnum(Class<E> enumClass, String value){
        for (Enum<E> enumConst : enumClass.getEnumConstants()) {
            //if special things required to finish
            if(enumConst.name().equals(value))
                return true;
        }
        return false;
    }

    static LinearLayout.LayoutParams setWeight(LinearLayout.LayoutParams input, int weight){
        input.weight = weight;
        return input;
    }
}
