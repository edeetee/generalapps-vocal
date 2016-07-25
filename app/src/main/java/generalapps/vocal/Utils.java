package generalapps.vocal;

import android.content.Context;
import android.util.Log;
import android.util.LogPrinter;
import android.view.View;
import android.view.ViewParent;
import android.widget.ListView;

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
}
