package generalapps.vocal;


import android.Manifest;
import android.app.FragmentManager;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.app.FragmentTransaction;
import android.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.File;

import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;

public class MainActivity extends AppCompatActivity implements TracksAdapter.TracksFragmentListener {

    static Recorder recorder;
    static MainActivity context;
    //static RecorderFragment recFrag;
    FragmentManager fragManager;

    static String RECORDER_FRAGMENT_TAG = "RECORDER_FRAGMENT_TAG";

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        //check permissions
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, 0);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        recorder = new Recorder(this);

        new AndroidFFMPEGLocator(this);

        fragManager = getFragmentManager();
        FragmentTransaction trans = fragManager.beginTransaction();
        trans.add(R.id.fragmentContainer, new TracksFragment());
        trans.commit();
    }

    @Override
    public void OnTrackSelected(File file) {
        RecorderFragment fragment = RecorderFragment.newInstance(file);

        FragmentTransaction transaction = fragManager.beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment, RECORDER_FRAGMENT_TAG);
        transaction.addToBackStack(null);

        transaction.commit();
    }

    @Override
    public void OnNewTrack() {
        RecorderFragment fragment = new RecorderFragment();

        FragmentTransaction transaction = fragManager.beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment, RECORDER_FRAGMENT_TAG);
        transaction.addToBackStack(null);

        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        try{
            BackOverrideFragment curRecorder = (BackOverrideFragment)getFragmentManager().findFragmentByTag(RECORDER_FRAGMENT_TAG);
            if(curRecorder != null && curRecorder.processBackPressed())
                super.onBackPressed();
        } catch (ClassCastException e) {
            super.onBackPressed();
        }
    }
}