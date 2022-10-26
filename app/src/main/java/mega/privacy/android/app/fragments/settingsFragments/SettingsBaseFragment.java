package mega.privacy.android.app.fragments.settingsFragments;

import static mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_SCROLL;
import static mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jeremyliao.liveeventbus.LiveEventBus;

import mega.privacy.android.data.database.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.data.model.MegaPreferences;
import mega.privacy.android.app.interfaces.SimpleSnackbarCallBack;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

public class SettingsBaseFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    protected Context context;
    protected MegaApiAndroid megaApi;
    protected MegaChatApiAndroid megaChatApi;
    protected DatabaseHandler dbH;
    protected MegaPreferences prefs;

    protected SimpleSnackbarCallBack snackbarCallBack;

    public SettingsBaseFragment() {
        if (megaApi == null) {
            megaApi = MegaApplication.getInstance().getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        }

        dbH = MegaApplication.getInstance().getDbH();
        prefs = dbH.getPreferences();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;

        if (context instanceof SimpleSnackbarCallBack) {
            snackbarCallBack = (SimpleSnackbarCallBack) context;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getListView().addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LiveEventBus.get(EVENT_UPDATE_SCROLL, Boolean.class)
                        .post(recyclerView.canScrollVertically(SCROLLING_UP_DIRECTION));
            }
        });
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }
}
