package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

/**
 * Created by mega on 27/03/18.
 */

public class TurnOnNotificationsFragment extends Fragment{

    MegaApiAndroid megaApi;
    Context context;

    float density;
    DisplayMetrics outMetrics;
    Display display;

    private LinearLayout containerTurnOnNotifications;

    public static TurnOnNotificationsFragment newInstance() {
        log("newInstance");
        TurnOnNotificationsFragment fragment = new TurnOnNotificationsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate");

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        display = ((Activity)context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density  = getResources().getDisplayMetrics().density;


//        ((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

        View v = inflater.inflate(R.layout.fragment_turn_on_notifications, container, false);

        containerTurnOnNotifications = (LinearLayout) v.findViewById(R.id.turnOnNotifications_fragment_container);
        containerTurnOnNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Window window = ((ManagerActivityLollipop)context).getWindow();
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                    window.setStatusBarColor(ContextCompat.getColor(context, R.color.lollipop_dark_primary_color));
                }
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD){
                    ((ManagerActivityLollipop) context).requestWindowFeature(Window.FEATURE_NO_TITLE);
                    ((ManagerActivityLollipop)context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
                ((ManagerActivityLollipop) context).selectDrawerItemLollipop(ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE);
            }
        });

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    private static void log(String log) {
        Util.log("SentRequestsFragmentLollipop", log);
    }

}
