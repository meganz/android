package mega.privacy.android.app.modalbottomsheet;

import android.app.Dialog;
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
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.TestPasswordActivity;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.lollipop.twofa.TwoFactorAuthenticationActivity;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

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
        log("onCreate");

    }

    @Override
    public void onClick(View v) {
        log("onClick");
        if (getContext() instanceof ManagerActivityLollipop){
            ((ManagerActivityLollipop) getContext()).rememberPasswordDialog.dismiss();
        }

        switch(v.getId()){
            case R.id.recovery_key_copytoclipboard_layout:{
                if (getContext() instanceof TestPasswordActivity){
                    ((TestPasswordActivity) getContext()).finish();
                }
                else if (getContext() instanceof TwoFactorAuthenticationActivity) {
                    ((TwoFactorAuthenticationActivity) getContext()).finish();
                }
                AccountController aC = new AccountController(getContext());
                aC.copyRkToClipboard();
                break;
            }
            case R.id.recovery_key_saveTo_fileSystem_layout:{
                log("option save to File System");
                AccountController aC = new AccountController(getContext());
                aC.saveRkToFileSystem(false);
                break;
            }
            case R.id.recovery_key_print_layout:{
                log("Option print rK");
                printRK();
                break;
            }
        }

        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    public void printRK(){
        Bitmap rKBitmap = null;
        AccountController aC = new AccountController(getContext());
        rKBitmap = aC.createRkBitmap();

        if (rKBitmap != null){
            PrintHelper printHelper = new PrintHelper(getActivity());
            printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            printHelper.printBitmap("rKPrint", rKBitmap);
        }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            optionPrint.setVisibility(View.VISIBLE);
        }
        else {
            optionPrint.setVisibility(View.GONE);
        }

        dialog.setContentView(contentView);
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
//        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
//
//        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            mBehavior.setPeekHeight((heightDisplay / 4) * 2);
//        }
//        else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
//            mBehavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
//        }
        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, getContext(), 48));
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    public static void log(String message) {
        Util.log("RecoveryKeyBottomSheetDialogFragment", message);
    }
}
