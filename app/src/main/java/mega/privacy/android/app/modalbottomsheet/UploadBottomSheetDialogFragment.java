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
import nz.mega.sdk.MegaUser;

public class UploadBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;
    MegaUser contact = null;

    private BottomSheetBehavior mBehavior;

    public LinearLayout mainLinearLayout;
    public TextView title;
    public LinearLayout optionImage;
    public LinearLayout optionAudio;
    public LinearLayout optionVideo;
    public LinearLayout optionFromSystem;

    DisplayMetrics outMetrics;

    MegaApiAndroid megaApi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if(context instanceof ManagerActivityLollipop){
            contact = ((ManagerActivityLollipop) context).getSelectedUser();
        }
    }
    @Override
    public void setupDialog(final Dialog dialog, int style) {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_upload, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.upload_bottom_sheet);

        optionImage = (LinearLayout) contentView.findViewById(R.id.upload_image_layout);
        optionAudio = (LinearLayout) contentView.findViewById(R.id.upload_audio_layout);
        optionVideo = (LinearLayout) contentView.findViewById(R.id.upload_video_layout);
        optionFromSystem = (LinearLayout) contentView.findViewById(R.id.upload_from_system_layout);

        title = (TextView) contentView.findViewById(R.id.contact_list_contact_name_text);

        optionImage.setOnClickListener(this);
        optionAudio.setOnClickListener(this);
        optionVideo.setOnClickListener(this);
        optionFromSystem.setOnClickListener(this);

        dialog.setContentView(contentView);
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.upload_image_layout:{
                log("click upload image");

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
            case R.id.upload_audio_layout:{
                log("click upload audio");
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
            case R.id.upload_video_layout:{
                log("click upload video");

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
        context = activity;
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
