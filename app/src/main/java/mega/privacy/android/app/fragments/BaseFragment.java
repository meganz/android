package mega.privacy.android.app.fragments;

import android.app.Activity;
import android.content.Context;
import androidx.fragment.app.Fragment;
import android.util.DisplayMetrics;
import android.view.Display;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

public class BaseFragment extends Fragment {

    protected Context context;

    protected MegaApplication app;

    protected MegaApiAndroid megaApi;
    protected MegaApiAndroid megaApiFolder;
    protected MegaChatApiAndroid megaChatApi;

    protected DatabaseHandler dbH;

    protected DisplayMetrics outMetrics;

    protected Activity mActivity;

    public BaseFragment() {
        app = MegaApplication.getInstance();
        if (app != null) {
            megaApi = app.getMegaApi();
            megaApiFolder = app.getMegaApiFolder();
            megaChatApi = app.getMegaChatApi();
            dbH = app.getDbH();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        this.context = context;
        mActivity = getActivity();

        if (mActivity != null) {
            Display display = mActivity.getWindowManager().getDefaultDisplay();
            outMetrics = new DisplayMetrics();
            display.getMetrics(outMetrics);
        }
    }

    public DisplayMetrics getOutMetrics() {
        return outMetrics;
    }
}
