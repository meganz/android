package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.utils.LogUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

public class ExportRecoveryKeyFragment extends Fragment implements View.OnClickListener{

    DatabaseHandler dbH;
    MegaApiAndroid megaApi;
    Context context;

    RelativeLayout exportMKLayout;
    LinearLayout exportMKButtonsLayout;
    TextView titleExportMK;
    TextView subTitleExportMK;
    TextView firstParExportMK;
    TextView secondParExportMK;
    TextView thirdParExportMK;
    TextView actionExportMK;
    Button copyMK;
    Button saveMK;

    DisplayMetrics outMetrics;

    public static ExportRecoveryKeyFragment newInstance() {
        LogUtil.logDebug("newInstance");
        ExportRecoveryKeyFragment fragment = new ExportRecoveryKeyFragment();
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

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        LogUtil.logDebug("onCreateView");

        View v = inflater.inflate(R.layout.export_mk_layout, container, false);

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        exportMKLayout = (RelativeLayout) v.findViewById(R.id.export_mk_full_layout);
        LinearLayout.LayoutParams exportMKButtonsParams = (LinearLayout.LayoutParams)exportMKLayout.getLayoutParams();
        exportMKButtonsParams.setMargins(0, 0, 0, Util.scaleHeightPx(10, outMetrics));
        exportMKLayout.setLayoutParams(exportMKButtonsParams);

        exportMKButtonsLayout = (LinearLayout) v.findViewById(R.id.MK_buttons_layout);

        titleExportMK = (TextView) v.findViewById(R.id.title_export_MK_layout);
        RelativeLayout.LayoutParams titleExportMKParams = (RelativeLayout.LayoutParams)titleExportMK.getLayoutParams();
        titleExportMKParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(50, outMetrics), Util.scaleWidthPx(24, outMetrics), 0);
        titleExportMK.setLayoutParams(titleExportMKParams);

        subTitleExportMK = (TextView) v.findViewById(R.id.subtitle_export_MK_layout);
        RelativeLayout.LayoutParams subTitleExportMKParams = (RelativeLayout.LayoutParams)subTitleExportMK.getLayoutParams();
        subTitleExportMKParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(24, outMetrics), Util.scaleWidthPx(24, outMetrics), 0);
        subTitleExportMK.setLayoutParams(subTitleExportMKParams);

        firstParExportMK = (TextView) v.findViewById(R.id.first_par_export_MK_layout);
        RelativeLayout.LayoutParams firstParExportMKParams = (RelativeLayout.LayoutParams)firstParExportMK.getLayoutParams();
        firstParExportMKParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(24, outMetrics), 0);
        firstParExportMK.setLayoutParams(firstParExportMKParams);

        secondParExportMK = (TextView) v.findViewById(R.id.second_par_export_MK_layout);
        RelativeLayout.LayoutParams secondParExportMKParams = (RelativeLayout.LayoutParams)secondParExportMK.getLayoutParams();
        secondParExportMKParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(24, outMetrics), 0);
        secondParExportMK.setLayoutParams(secondParExportMKParams);

        thirdParExportMK = (TextView) v.findViewById(R.id.third_par_export_MK_layout);
        RelativeLayout.LayoutParams thirdParExportMKParams = (RelativeLayout.LayoutParams)thirdParExportMK.getLayoutParams();
        thirdParExportMKParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(24, outMetrics), Util.scaleWidthPx(24, outMetrics), 0);
        thirdParExportMK.setLayoutParams(thirdParExportMKParams);

        actionExportMK = (TextView) v.findViewById(R.id.action_export_MK_layout);
        RelativeLayout.LayoutParams actionExportMKParams = (RelativeLayout.LayoutParams)actionExportMK.getLayoutParams();
        actionExportMKParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(24, outMetrics), 0);
        actionExportMK.setLayoutParams(actionExportMKParams);

        copyMK = (Button) v.findViewById(R.id.copy_MK_button);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            copyMK.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_upgrade));
        }
        LinearLayout.LayoutParams copyMKParams = (LinearLayout.LayoutParams)copyMK.getLayoutParams();
        copyMKParams.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(20, outMetrics), 0, 0);
        copyMK.setLayoutParams(copyMKParams);
        copyMK.setOnClickListener(this);

        saveMK = (Button) v.findViewById(R.id.save_MK_button);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            saveMK.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_upgrade));
        }
        LinearLayout.LayoutParams saveMKParams = (LinearLayout.LayoutParams)saveMK.getLayoutParams();
        saveMKParams.setMargins(Util.scaleWidthPx(8, outMetrics), Util.scaleHeightPx(20, outMetrics), 0, 0);
        saveMK.setLayoutParams(saveMKParams);
        saveMK.setOnClickListener(this);

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        LogUtil.logDebug("onAttach");
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public void onAttach(Context context) {
        LogUtil.logDebug("onAttach context");
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.copy_MK_button:{
                LogUtil.logDebug("Copy Master Key button");
                ((ManagerActivityLollipop)context).hideMKLayout();
                AccountController aC = new AccountController(context);
                aC.copyMK(false);
                break;
            }
            case R.id.save_MK_button:{
                LogUtil.logDebug("Save Master Key button");
                ((ManagerActivityLollipop)context).hideMKLayout();
                AccountController aC = new AccountController(context);
                aC.exportMK(null, false);
                break;
            }
        }
    }
}
