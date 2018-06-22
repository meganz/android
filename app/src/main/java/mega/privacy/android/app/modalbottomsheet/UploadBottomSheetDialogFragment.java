package mega.privacy.android.app.modalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

public class UploadBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;

    private BottomSheetBehavior mBehavior;
    private LinearLayout items_layout;

    public LinearLayout mainLinearLayout;
    public TextView title;
    public LinearLayout optionFromDevice;
    public LinearLayout optionFromSystem;

    DisplayMetrics outMetrics;
    private int heightDisplay;

    MegaApiAndroid megaApi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }
    @Override
    public void setupDialog(final Dialog dialog, int style) {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        heightDisplay = outMetrics.heightPixels;

        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_upload, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.upload_bottom_sheet);
        items_layout = (LinearLayout) contentView.findViewById(R.id.items_layout);

        optionFromDevice = (LinearLayout) contentView.findViewById(R.id.upload_from_device_layout);
        optionFromSystem = (LinearLayout) contentView.findViewById(R.id.upload_from_system_layout);

        title = (TextView) contentView.findViewById(R.id.contact_list_contact_name_text);

        optionFromDevice.setOnClickListener(this);
        optionFromSystem.setOnClickListener(this);

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

        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, 48));
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.upload_from_device_layout:{
                log("click upload from device");

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setType("*/*");

                if(context instanceof ManagerActivityLollipop){
                    ((ManagerActivityLollipop)context).startActivityForResult(Intent.createChooser(intent, null), Constants.REQUEST_CODE_GET);
                }
                else if(context instanceof ContactFileListActivityLollipop){
                    ((ContactFileListActivityLollipop)context).startActivityForResult(Intent.createChooser(intent, null), Constants.REQUEST_CODE_GET);
                }

                dismissAllowingStateLoss();
                break;
            }
            case R.id.upload_from_system_layout:{
                log("click upload from_system");

                Intent intent = new Intent();
                intent.setAction(FileStorageActivityLollipop.Mode.PICK_FILE.getAction());
                intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, false);
                intent.setClass(context, FileStorageActivityLollipop.class);

                if(context instanceof ManagerActivityLollipop){
                    ((ManagerActivityLollipop)context).startActivityForResult(intent, Constants.REQUEST_CODE_GET_LOCAL);
                }
                else if(context instanceof ContactFileListActivityLollipop){
                    ((ContactFileListActivityLollipop)context).startActivityForResult(intent, Constants.REQUEST_CODE_GET_LOCAL);
                }
                break;
            }
        }

//        dismiss();
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.context = activity;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    private static void log(String log) {
        Util.log("UploadBottomSheetDialogFragment", log);
    }
}
