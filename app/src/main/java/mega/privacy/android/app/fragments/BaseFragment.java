package mega.privacy.android.app.fragments;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.util.DisplayMetrics;
import android.view.Display;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.service.ads.GoogleAdsLoader;
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

    /** The Loader to load Google Ads for this fragment */
    protected GoogleAdsLoader mAdsLoader;

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
    public void onAttach(@NonNull Context context) {
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

    /**
     * Init the Ads Loader and associate it with the Ad Slot
     * Add it as the fragment lifecycle observer
     * @param adSlot the Ads Slot Id, defined by API side
     * @param loadImmediate load the Ads immediately or not
     */
    protected void initAdsLoader(String adSlot, Boolean loadImmediate) {
        mAdsLoader = new GoogleAdsLoader(context, adSlot, loadImmediate);
        getLifecycle().addObserver(mAdsLoader);
    }
}
