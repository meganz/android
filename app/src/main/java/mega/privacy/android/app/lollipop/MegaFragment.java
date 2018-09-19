package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.Display;

import java.util.Stack;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

public class MegaFragment extends Fragment {
    
    protected MegaApiAndroid megaApi;
    protected Context mContext;
    protected Stack<Integer> lastPositionStack;
    protected DatabaseHandler dbH = null;
    protected MegaPreferences prefs = null;
    protected String downloadLocationDefaultPath = Util.downloadDIR;
    protected DisplayMetrics outMetrics;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        log("ContactFileBaseFragment onCreate");
        super.onCreate(savedInstanceState);
        
        if (megaApi == null) {
            megaApi = ((MegaApplication)((Activity)mContext).getApplication()).getMegaApi();
        }
        
        dbH = DatabaseHandler.getDbHandler(mContext);
        prefs = dbH.getPreferences();
        if (prefs != null) {
            log("prefs != null");
            if (prefs.getStorageAskAlways() != null) {
                if (!Boolean.parseBoolean(prefs.getStorageAskAlways())) {
                    log("askMe is false");
                    if (prefs.getStorageDownloadLocation() != null) {
                        if (prefs.getStorageDownloadLocation().compareTo("") != 0) {
                            downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
                        }
                    }
                }
            }
        }
        
        lastPositionStack = new Stack<>();
        
        Display display = ((Activity)mContext).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
    }
    
    protected void sendSignalPresenceActivity(){
        ((MegaApplication)((Activity)mContext).getApplication()).sendSignalPresenceActivity();
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity;
    }
    
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mContext = context;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    public static void log(String log) {
        Util.log(MegaFragment.class.toString(),log);
    }
}
