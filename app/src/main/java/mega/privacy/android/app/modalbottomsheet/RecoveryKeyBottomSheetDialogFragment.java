package mega.privacy.android.app.modalbottomsheet;

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

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.TestPasswordActivity;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

/**
 * Created by mega on 5/04/18.
 */

public class RecoveryKeyBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    private static int REQUEST_DOWNLOAD_FOLDER = 1111;
    final String ACTION_RECOVERY_KEY_COPY_TO_CLIPBOARD = "ACTION_RECOVERY_KEY_COPY_TO_CLIPBOARD";

    public LinearLayout mainLinearLayout;
    private BottomSheetBehavior mBehavior;

    DisplayMetrics outMetrics;
    private int heightDisplay;

    public TextView titleText;
    public LinearLayout optionCopyToClipboard;
    public LinearLayout optionSaveToFileSystem;

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
                copyToClipboard();
                break;
            }
            case R.id.recovery_key_saveTo_fileSystem_layout:{
                log("option save to File System");
                saveToFileSystem();
                break;
            }
        }

        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    void copyToClipboard () {
        if (getContext() instanceof  ManagerActivityLollipop) {
            ((ManagerActivityLollipop) getContext()).copyMK();
        }
        else if (getContext() instanceof TestPasswordActivity) {
            Intent intent = new Intent(getContext(), ManagerActivityLollipop.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setAction(ACTION_RECOVERY_KEY_COPY_TO_CLIPBOARD);
            startActivity(intent);
            ((TestPasswordActivity) getContext()).finish();
        }
    }

    void saveToFileSystem () {

        Intent intent = new Intent(getActivity(), FileStorageActivityLollipop.class);
        intent.setAction(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
        intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, true);
        if (getContext() instanceof TestPasswordActivity){
            ((TestPasswordActivity) getActivity()).startActivityForResult(intent, REQUEST_DOWNLOAD_FOLDER);
        }
        else if (getContext() instanceof ManagerActivityLollipop){
            ((ManagerActivityLollipop) getActivity()).startActivityForResult(intent, REQUEST_DOWNLOAD_FOLDER);
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

        titleText = (TextView) contentView.findViewById(R.id.recovery_key_title_text);

        optionCopyToClipboard= (LinearLayout) contentView.findViewById(R.id.recovery_key_copytoclipboard_layout);
        optionSaveToFileSystem = (LinearLayout) contentView.findViewById(R.id.recovery_key_saveTo_fileSystem_layout);

        optionCopyToClipboard.setOnClickListener(this);
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
        Util.log("RecoveryKeyBottomSheetDialogFragment", message);
    }
}
