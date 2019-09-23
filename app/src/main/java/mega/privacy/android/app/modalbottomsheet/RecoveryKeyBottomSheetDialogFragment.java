package mega.privacy.android.app.modalbottomsheet;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.print.PrintHelper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.TestPasswordActivity;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.lollipop.TwoFactorAuthenticationActivity;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.LogUtil.*;

public class RecoveryKeyBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    public LinearLayout mainLinearLayout;
    private BottomSheetBehavior mBehavior;
    private LinearLayout items_layout;

    DisplayMetrics outMetrics;
    private int heightDisplay;

    public TextView titleText;
    public LinearLayout optionCopyToClipboard;
    public LinearLayout optionSaveToFileSystem;
    public LinearLayout optionPrint;

    MegaApiAndroid megaApi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logDebug("onCreate");
    }

    @Override
    public void onClick(View v) {
        logDebug("onClick");

        switch(v.getId()){
            case R.id.recovery_key_copytoclipboard_layout:{
                if (getContext() instanceof TwoFactorAuthenticationActivity) {
                    ((TwoFactorAuthenticationActivity) getContext()).finish();
                }
                AccountController aC = new AccountController(getContext());
                aC.copyRkToClipboard();
                break;
            }
            case R.id.recovery_key_saveTo_fileSystem_layout:{
                logDebug("Option save to File System");
                AccountController aC = new AccountController(getContext());
                aC.saveRkToFileSystem();
                break;
            }
            case R.id.recovery_key_print_layout:{
                logDebug("Option print RK");
                AccountController aC = new AccountController(getContext());
                aC.printRK();
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
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_recovery_key, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.recovery_key_bottom_sheet);
        items_layout = (LinearLayout) contentView.findViewById(R.id.items_layout);

        titleText = (TextView) contentView.findViewById(R.id.recovery_key_title_text);

        optionPrint = (LinearLayout) contentView.findViewById(R.id.recovery_key_print_layout);
        optionCopyToClipboard= (LinearLayout) contentView.findViewById(R.id.recovery_key_copytoclipboard_layout);
        optionSaveToFileSystem = (LinearLayout) contentView.findViewById(R.id.recovery_key_saveTo_fileSystem_layout);

        optionPrint.setOnClickListener(this);
        optionCopyToClipboard.setOnClickListener(this);
        optionSaveToFileSystem.setOnClickListener(this);


        dialog.setContentView(contentView);
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, getContext(), 48));
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}
