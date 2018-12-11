package br.com.wellington.chatsocket.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.Snackbar;

import br.com.wellington.chatsocket.R;

/**
 * Classe Nova
 *
 * @author Wellington
 * @version 1.0 - 26/08/2017.
 */
public class ConnectionHelper {

    private static final String PREFS_PRIVATE = "PREFS_PRIVATE";
    public static final int CONNECT_MESSAGE = 0;
    public static final int DISCONNECT_MESSAGE = 1;
    public static final int SIMPLE_MESSAGE = 2;
    public static final int PRIVATE_MESSAGE = 3;
    public static final int POLLING = 4;
    public static final int CONNECTION_ACCEPTED = 5;
    public static final int CONNECTION_REFUSED = 6;
    public static final int ERROR = 7;
    public static final int CLOSE = 8;

    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void showIsNotConected(Activity activity){
        Snackbar.make(activity.findViewById(android.R.id.content), activity.getResources().getString(R.string.no_internet_connectivity), Snackbar.LENGTH_LONG).show();
    }

    public static String getUserSave(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_PRIVATE, Context.MODE_PRIVATE);
        return sharedPreferences.getString("userName","");
    }

    public static void saveUser( String userName, Context context) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_PRIVATE, Context.MODE_PRIVATE);

        SharedPreferences.Editor prefsPrivateEditor = sharedPreferences.edit();

        prefsPrivateEditor.putString("userName", userName);

        prefsPrivateEditor.apply();

    }

    public static boolean isUserEmpty(Context context){
        return getUserSave(context).isEmpty();
    }

}
