package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaRecentActionBucket;

public class RecentsFragment extends Fragment implements View.OnClickListener {

    private RecentsFragment recentsFragment;
    private Context context;
    private DisplayMetrics outMetrics;

    private MegaApiAndroid megaApi;

    private ArrayList<MegaRecentActionBucket> buckets;

    private RelativeLayout emptyLayout;
    private ImageView emptyImage;
    private TextView emptyText;

    public static RecentsFragment newInstance() {
        log("newInstance");
        RecentsFragment fragment = new RecentsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        recentsFragment = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
        if (megaApi.getRootNode() == null) return null;

        buckets = megaApi.getRecentActions();

        View v = inflater.inflate(R.layout.fragment_recents, container, false);

        emptyLayout = (RelativeLayout) v.findViewById(R.id.empty_state_recents);
        emptyImage = (ImageView) v.findViewById(R.id.empty_image_recents);

        RelativeLayout.LayoutParams params;
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            params = new RelativeLayout.LayoutParams(Util.px2dp(200, outMetrics), Util.px2dp(200, outMetrics));
        }
        else {
            params = new RelativeLayout.LayoutParams(Util.px2dp(100, outMetrics), Util.px2dp(100, outMetrics));
        }
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        emptyImage.setLayoutParams(params);

        emptyText = (TextView) v.findViewById(R.id.empty_text_recents);

        String textToShow = String.format(context.getString(R.string.context_empty_recents)).toUpperCase();
        try {
            textToShow = textToShow.replace("[A]","<font color=\'#000000\'>");
            textToShow = textToShow.replace("[/A]","</font>");
            textToShow = textToShow.replace("[B]","<font color=\'#7a7a7a\'>");
            textToShow = textToShow.replace("[/B]","</font>");
        } catch (Exception e) {
        }
        Spanned result = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(textToShow);
        }
        emptyText.setText(result);


        return v;
    }

    public int onBackPressed() {
        return 0;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public void onClick(View v) {

    }

    private static void log(String log) {
        Util.log("RecentsFragment",log);
    }
}
