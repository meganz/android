package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.Product;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaPricing;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class CopyrightFragmentLollipop extends Fragment implements View.OnClickListener {

    Context context;

    float scaleH, scaleW;
    float density;
    DisplayMetrics outMetrics;
    Display display;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    ActionBar aB;
    Button agreeButton;
    Button disagreeButton;

    private LinearLayout mainLinearLayout;
    private ScrollView scrollView;


    @Override
    public void onCreate (Bundle savedInstanceState){
        log("onCreate");
        super.onCreate(savedInstanceState);

        if(context==null){
            log("context is null");
            return;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        if(megaApi==null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        View v = inflater.inflate(R.layout.fragment_copyright, container, false);

        scrollView = (ScrollView) v.findViewById(R.id.scroll_view_copyright);
        mainLinearLayout = (LinearLayout) v.findViewById(R.id.copyright_main_linear_layout);

        agreeButton = (Button) v.findViewById(R.id.agree_button);
        disagreeButton = (Button) v.findViewById(R.id.disagree_button);

        agreeButton.setOnClickListener(this);
        disagreeButton.setOnClickListener(this);

        //Replace elevation
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.grid_item_separator));
            mainLinearLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.grid_item_separator));
        }

        return v;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.disagree_button:{
                ((GetLinkActivityLollipop)context).finish();
                break;
            }
            case R.id.agree_button:{
                ((GetLinkActivityLollipop)context).showFragment(Constants.GET_LINK_FRAGMENT);
                break;
            }
        }
    }


    @Override
    public void onAttach(Context context) {
        log("onAttach");
        super.onAttach(context);
        this.context = context;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    @Override
    public void onAttach(Activity context) {
        log("onAttach Activity");
        super.onAttach(context);
        this.context = context;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    public static void log(String message) {
        Util.log("CopyrightFragmentLollipop", message);
    }

}
