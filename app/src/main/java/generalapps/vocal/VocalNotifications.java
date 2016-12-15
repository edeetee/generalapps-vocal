package generalapps.vocal;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by edeetee on 26/09/2016.
 */

class VocalNotifications {
    private static final OkHttpClient client = new OkHttpClient();
    static private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    static public final String OPEN_TRACK = "openTrack";

    static void usersTurnToRecord(String trackKey, String uidToNotify){
        sendOpenTrackNotification(trackKey, uidToNotify, "Vocal Track", "Your turn to record");
    }

    static void notifyFinished(String trackKey, String uidToNotify){
        sendOpenTrackNotification(trackKey, uidToNotify, "Finished", "A track has finished");
    }

    static private void sendOpenTrackNotification(String trackKey, String uidToNotify, String title, String body){
        JSONObject dataObj = new JSONObject();
        try{
            dataObj.put("trackKey", trackKey);
        } catch(JSONException e){
            Log.e("VocalNotifications", "JSON Writing Failed", e);
        }
        sendNotification(uidToNotify, title, body, OPEN_TRACK, dataObj);
    }

    static private void sendNotification(String toUID, final String title, final String body, final String type, final JSONObject data){
        MainActivity.database.getReference("users").child(toUID).child("instanceIDToken").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                JSONObject obj = new JSONObject();
                JSONObject notification = new JSONObject();
                try{
                    data.put("type", type);
                    data.put("vocalNotification", true);
                    notification.put("title", title);
                    notification.put("body", body);
                    notification.put("sound", "default");
                    obj.put("notification", notification);
                    obj.put("data", data);
                    obj.put("to", dataSnapshot.getValue(String.class));
                } catch (JSONException e){
                    Log.e("VocalNotifications", "JSON Writing Failed", e);
                }

                post(obj, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.i("VocalNotification", "POST Failure!", e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Log.i("VocalNotification", "POST Code: " + response.code() + ", Body: " + response.body().string());
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private static void post(JSONObject json, Callback callback){
        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
                .url("https://fcm.googleapis.com/fcm/send")
                .addHeader("Authorization", "key=AIzaSyDJXu40p5JO3S32TXIGGQBOMd5uE6oruGU")
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }
}
