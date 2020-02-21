package mega.privacy.android.app.fragments;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.Display;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

import static mega.privacy.android.app.utils.Util.*;

public class BaseFragment extends Fragment {

    protected Context context;

    protected MegaApplication app;

    protected MegaApiAndroid megaApi;
    protected MegaApiAndroid megaApiFolder;
    protected MegaChatApiAndroid megaChatApi;

    protected DatabaseHandler dbH;

    protected DisplayMetrics outMetrics;

    public BaseFragment() {
        app = MegaApplication.getInstance();
        megaApi = app.getMegaApi();
        megaApiFolder = app.getMegaApiFolder();
        megaChatApi = app.getMegaChatApi();
        dbH = app.getDbH();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        this.context = context;

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
    }

    public DisplayMetrics getOutMetrics() {
        return outMetrics;
    }
}
