package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.LogUtil;
import nz.mega.sdk.MegaApiAndroid;

public class TurnOnNotificationsFragment extends Fragment implements View.OnClickListener{

    DatabaseHandler dbH;
    MegaApiAndroid megaApi;
    Context context;
    TextView firstText;
    TextView secondText;
    TextView thirdText;
    TextView fourthText;

    private LinearLayout containerTurnOnNotifications;

    public static TurnOnNotificationsFragment newInstance() {
        LogUtil.logDebug("newInstance");
        TurnOnNotificationsFragment fragment = new TurnOnNotificationsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.logDebug("onCreate");

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
        LogUtil.logDebug("onCreateView");

        View v = inflater.inflate(R.layout.fragment_turn_on_notifications, container, false);
        containerTurnOnNotifications = (LinearLayout) v.findViewById(R.id.turnOnNotifications_fragment_container);
        containerTurnOnNotifications.setOnClickListener(this);

        firstText = (TextView) v.findViewById(R.id.first_text);
        secondText = (TextView) v.findViewById(R.id.second_text);
        thirdText = (TextView) v.findViewById(R.id.third_text);
        fourthText = (TextView) v.findViewById(R.id.fourth_text);

        String textToShow = String.format(getString(R.string.turn_on_notifications_first_step));
        try{
            textToShow = textToShow.replace("[A]", "<font color=\'#ffffff\'>");
            textToShow = textToShow.replace("[/A]", "</font>");
        }
        catch (Exception e){}
        Spanned result = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(textToShow);
        }
        firstText.setText(result);

        textToShow = String.format(getString(R.string.turn_on_notifications_second_step));
        try{
            textToShow = textToShow.replace("[A]", "<font color=\'#ffffff\'>");
            textToShow = textToShow.replace("[/A]", "</font>");
        }
        catch (Exception e){}
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(textToShow);
        }
        secondText.setText(result);

        textToShow = String.format(getString(R.string.turn_on_notifications_third_step));
        try{
            textToShow = textToShow.replace("[A]", "<font color=\'#ffffff\'>");
            textToShow = textToShow.replace("[/A]", "</font>");
        }
        catch (Exception e){}
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(textToShow);
        }
        thirdText.setText(result);

        textToShow = String.format(getString(R.string.turn_on_notifications_fourth_step));
        try{
            textToShow = textToShow.replace("[A]", "<font color=\'#ffffff\'>");
            textToShow = textToShow.replace("[/A]", "</font>");
        }
        catch (Exception e){}
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(textToShow);
        }
        fourthText.setText(result);

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
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
