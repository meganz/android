package mega.privacy.android.app.main.managerSections;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.utils.ColorUtils;
import nz.mega.sdk.MegaApiAndroid;
import timber.log.Timber;

public class TurnOnNotificationsFragment extends Fragment implements View.OnClickListener {

    DatabaseHandler dbH;
    MegaApiAndroid megaApi;
    Context context;
    TextView firstText;
    TextView secondText;
    TextView thirdText;
    TextView fourthText;

    private LinearLayout containerTurnOnNotifications;

    public static TurnOnNotificationsFragment newInstance() {
        Timber.d("newInstance");
        TurnOnNotificationsFragment fragment = new TurnOnNotificationsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("onCreate");

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }

        if (dbH == null) {
            dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        }

        dbH.setShowNotifOff(false);
        ((ManagerActivity) context).turnOnNotifications = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        Timber.d("onCreateView");

        View v = inflater.inflate(R.layout.fragment_turn_on_notifications, container, false);
        containerTurnOnNotifications = (LinearLayout) v.findViewById(R.id.turnOnNotifications_fragment_container);
        containerTurnOnNotifications.setOnClickListener(this);

        firstText = (TextView) v.findViewById(R.id.first_text);
        secondText = (TextView) v.findViewById(R.id.second_text);
        thirdText = (TextView) v.findViewById(R.id.third_text);
        fourthText = (TextView) v.findViewById(R.id.fourth_text);

        String textToShow = getString(R.string.turn_on_notifications_first_step);
        try {
            textToShow = textToShow.replace("[A]", "<font color=\'"
                    + ColorUtils.getColorHexString(context, R.color.white_black)
                    + "\'>");
            textToShow = textToShow.replace("[/A]", "</font>");
        } catch (Exception e) {
        }
        firstText.setText(HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY));

        textToShow = getString(R.string.turn_on_notifications_second_step);
        try {
            textToShow = textToShow.replace("[A]", "<font color=\'"
                    + ColorUtils.getColorHexString(context, R.color.white_black)
                    + "\'>");
            textToShow = textToShow.replace("[/A]", "</font>");
        } catch (Exception e) {
        }
        secondText.setText(HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY));

        textToShow = getString(R.string.turn_on_notifications_third_step);
        try {
            textToShow = textToShow.replace("[A]", "<font color=\'"
                    + ColorUtils.getColorHexString(context, R.color.white_black)
                    + "\'>");
            textToShow = textToShow.replace("[/A]", "</font>");
        } catch (Exception e) {
        }
        thirdText.setText(HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY));

        textToShow = getString(R.string.turn_on_notifications_fourth_step);
        try {
            textToShow = textToShow.replace("[A]", "<font color=\'"
                    + ColorUtils.getColorHexString(context, R.color.white_black)
                    + "\'>");
            textToShow = textToShow.replace("[/A]", "</font>");
        } catch (Exception e) {
        }
        fourthText.setText(HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY));

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.turnOnNotifications_fragment_container: {
                ((ManagerActivity) context).deleteTurnOnNotificationsFragment();
                break;
            }
        }
    }
}
