package generalapps.vocal;

import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.Exclude;
import com.google.firebase.iid.FirebaseInstanceId;

/**
 * Created by edeetee on 8/09/2016.
 */
public class User {
    public String uid;
    public String name;
    public String instanceIDToken;

    public User(){}
    public User(UserInfo user){
        uid = user.getUid();
        name = user.getDisplayName();
        instanceIDToken = FirebaseInstanceId.getInstance().getToken();
    }

    public boolean equals(User obj) {
        return uid != null && uid.equals(obj.uid) && name != null && name.equals(obj.name) && instanceIDToken != null && instanceIDToken.equals(obj.instanceIDToken);
    }

    @Exclude public String firstName(){
        return name.split(" ")[0];
    }
}
