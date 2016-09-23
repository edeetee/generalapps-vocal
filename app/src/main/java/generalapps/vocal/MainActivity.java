package generalapps.vocal;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class MainActivity extends AppCompatActivity implements TracksFragment.TracksFragmentListener {

    static Recorder recorder;
    static MainActivity context;
    //static RecorderFragment recFrag;
    FragmentManager fragManager;
    static FirebaseStorage storage;
    static StorageReference storageRef;
    static FirebaseDatabase database;
    static FirebaseAuth auth;
    static User user;

    boolean started = false;

    static String MAIN_FRAGMENT_TAG = "MAIN_FRAGMENT_TAG";
    static int RC_SIGN_IN = 10231;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        if(getIntent().getExtras() != null)
            for (String s : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(s);
            }

        //check permissions
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, 0);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        if(!started){
            recorder = new Recorder(this);

            //new AndroidFFMPEGLocator(this);

            //TODO make work with restart again
            try{
                FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            } catch(DatabaseException e){
                Log.e("MainActivity", "setPersistenceEnabled Failed", e);
            }

            storage = FirebaseStorage.getInstance();
            database = FirebaseDatabase.getInstance();
            storageRef = MainActivity.storage.getReferenceFromUrl("gs://vocal-d80ba.appspot.com/");
            auth = FirebaseAuth.getInstance();
            fragManager = getSupportFragmentManager();

            if(auth.getCurrentUser() == null){
                startActivityForResult(AuthUI.getInstance()
                        .createSignInIntentBuilder().build(), RC_SIGN_IN);
            } else {
                makeUserThenStart();
            }
        }
    }

    private void makeUserThenStart(){
        final UserInfo userInfo = auth.getCurrentUser();
        final DatabaseReference ref = database.getReference("users").child(userInfo.getUid());
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() == null){
                    user = new User(userInfo);
                    ref.setValue(user);
                } else
                    user = dataSnapshot.getValue(User.class);
                start();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void start(){
        started = true;
        FragmentTransaction trans = fragManager.beginTransaction();
        trans.add(R.id.fragmentContainer, new TracksFragment(), MAIN_FRAGMENT_TAG);
        trans.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_SIGN_IN){
            if(resultCode == RESULT_OK){
                makeUserThenStart();
            }
        }
    }

    @Override
    public void OnTrackSelected(Track.MetaData meta) {
        TrackPagerFragment fragment = TrackPagerFragment.newInstance(meta);

        FragmentTransaction transaction = fragManager.beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment, MAIN_FRAGMENT_TAG);
        transaction.addToBackStack(null);

        transaction.commit();
    }

    @Override
    public void OnNewTrack() {
        TrackPagerFragment fragment = new TrackPagerFragment();

        FragmentTransaction transaction = fragManager.beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment, MAIN_FRAGMENT_TAG);
        transaction.addToBackStack(null);

        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        try{
            BackOverrideFragment curRecorder = (BackOverrideFragment)getSupportFragmentManager().findFragmentByTag(MAIN_FRAGMENT_TAG);
            if(curRecorder != null && curRecorder.processBackPressed())
                super.onBackPressed();
        } catch (ClassCastException e) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_settings){
            if(fragManager.findFragmentByTag(MAIN_FRAGMENT_TAG) instanceof SettingsFragment) {
                fragManager.popBackStack();
            } else{
                SettingsFragment fragment = new SettingsFragment();
                FragmentTransaction transaction = fragManager.beginTransaction();
                transaction.replace(R.id.fragmentContainer, fragment, MAIN_FRAGMENT_TAG);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        }
        return super.onOptionsItemSelected(item);
    }
}