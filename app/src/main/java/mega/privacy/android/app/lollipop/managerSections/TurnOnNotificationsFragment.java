package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

/**
 * Created by mega on 27/03/18.
 */

public class TurnOnNotificationsFragment extends Fragment implements View.OnClickListener{

    DatabaseHandler dbH;
    MegaApiAndroid megaApi;
    Context context;

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

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        }

        dbH.setShowNotifOff(false);
        ((ManagerActivityLollipop) context).turnOnNotifications = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        View v = inflater.inflate(R.layout.fragment_turn_on_notifications, container, false);
        containerTurnOnNotifications = (LinearLayout) v.findViewById(R.id.turnOnNotifications_fragment_container);
        containerTurnOnNotifications.setOnClickListener(this);

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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.turnOnNotifications_fragment_container: {
                ((ManagerActivityLollipop) context).deleteTurnOnNotificationsFragment();
                break;
            }
        }
    }
}
