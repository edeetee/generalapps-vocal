package generalapps.vocal;

import android.util.Log;
import android.util.LogPrinter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;

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
}
