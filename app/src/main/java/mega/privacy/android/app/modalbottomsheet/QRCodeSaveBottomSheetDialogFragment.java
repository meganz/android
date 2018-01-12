package mega.privacy.android.app.modalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.lollipop.qrcode.QRCodeActivity;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;

/**
 * Created by mega on 12/01/18.
 */

public class QRCodeSaveBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    public LinearLayout mainLinearLayout;
    private BottomSheetBehavior mBehavior;

    DisplayMetrics outMetrics;
    private int heightDisplay;

    public TextView titleText;
    public LinearLayout optionSaveToCloudDrive;
    public LinearLayout optionSaveToFileSystem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate");

    }

    @Override
    public void onClick(View v) {
        log("onClick");
        switch(v.getId()){

            case R.id.qr_code_saveTo_cloud_layout:{
                log("option save to Cloud Drive");

                break;
            }
            case R.id.qr_code_saveTo_fileSystem_layout:{
                log("option save to File System");

                break;
            }
        }

        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    public void setupDialog(final Dialog dialog, int style) {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        heightDisplay = outMetrics.heightPixels;

        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_qr_code, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.qr_code_bottom_sheet);

        titleText = (TextView) contentView.findViewById(R.id.qr_code_title_text);

        optionSaveToCloudDrive= (LinearLayout) contentView.findViewById(R.id.qr_code_saveTo_cloud_layout);
        optionSaveToFileSystem = (LinearLayout) contentView.findViewById(R.id.qr_code_saveTo_fileSystem_layout);

        optionSaveToCloudDrive.setOnClickListener(this);
        optionSaveToFileSystem.setOnClickListener(this);

        dialog.setContentView(contentView);
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mBehavior.setPeekHeight((heightDisplay / 4) * 2);
        }
        else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            mBehavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
        }
    }

    public static void log(String message) {
        Util.log("QRCodeSaveBottomSheetDialogFragment", message);
    }
}
