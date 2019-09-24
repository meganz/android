package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
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

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class CopyrightFragmentLollipop extends Fragment implements View.OnClickListener {

    Context context;

    float scaleH, scaleW;
    float density;
    DisplayMetrics outMetrics;
    Display display;
    private MegaApiAndroid megaApi;
    ActionBar aB;
    Button agreeButton;
    Button disagreeButton;

    DatabaseHandler dbH;

    @Override
    public void onCreate (Bundle savedInstanceState){
        logDebug("onCreate");
        super.onCreate(savedInstanceState);

        if(context==null){
            logWarning("context is null");
            return;
        }

        dbH = DatabaseHandler.getDbHandler(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        logDebug("onCreateView");

        if(megaApi==null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        View v = inflater.inflate(R.layout.fragment_copyright, container, false);

        agreeButton = (Button) v.findViewById(R.id.agree_button);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            agreeButton.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_upgrade));
        }
        disagreeButton = (Button) v.findViewById(R.id.disagree_button);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            disagreeButton.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_upgrade));
        }

        agreeButton.setOnClickListener(this);
        disagreeButton.setOnClickListener(this);

        return v;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.disagree_button:{
                dbH.setShowCopyright(true);
                ((GetLinkActivityLollipop)context).finish();
                break;
            }
            case R.id.agree_button:{
                dbH.setShowCopyright(false);
                ((GetLinkActivityLollipop)context).showFragment(GET_LINK_FRAGMENT);
                break;
            }
        }
    }


    @Override
    public void onAttach(Context context) {
        logDebug("onAttach");
        super.onAttach(context);
        this.context = context;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    @Override
    public void onAttach(Activity context) {
        logDebug("onAttach Activity");
        super.onAttach(context);
        this.context = context;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }
}
