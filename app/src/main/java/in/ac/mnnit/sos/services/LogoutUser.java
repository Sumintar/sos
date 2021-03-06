package in.ac.mnnit.sos.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import in.ac.mnnit.sos.ProcessEmailActivity;
import in.ac.mnnit.sos.database.LocalDatabaseAdapter;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by Banda Prashanth Yadav on 1/3/17.
 */

public class LogoutUser {

    Context context;
    public LogoutUser(Context context) {
        this.context = context;
    }

    public void logout(){
        LocalDatabaseAdapter localDatabaseAdapter = new LocalDatabaseAdapter(context);
        localDatabaseAdapter.deleteDatabase();
        SharedPreferences sharedPreferences = context.getSharedPreferences("session", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("loggedin", false);
        editor.commit();

        Intent intent = new Intent(context, ProcessEmailActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
