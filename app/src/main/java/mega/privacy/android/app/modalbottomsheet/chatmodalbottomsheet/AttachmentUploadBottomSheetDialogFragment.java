package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.UtilsModalBottomSheet;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.LogUtil.*;

public class AttachmentUploadBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;

    private BottomSheetBehavior mBehavior;
    private LinearLayout items_layout;

    public LinearLayout mainLinearLayout;
    public TextView titleText;
    public LinearLayout optionFromCloud;
    public LinearLayout optionContact;
    public LinearLayout optionPhoto;

    DisplayMetrics outMetrics;

    MegaApiAndroid megaApi;
    DatabaseHandler dbH;

    private int heightDisplay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        dbH = DatabaseHandler.getDbHandler(getActivity());
    }
    @Override
    public void setupDialog(final Dialog dialog, int style) {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        heightDisplay = outMetrics.heightPixels;

        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_attachment_upload, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.attachment_upload_bottom_sheet);
        items_layout = (LinearLayout) contentView.findViewById(R.id.items_layout);

        titleText = (TextView) contentView.findViewById(R.id.attachment_upload_title_text);

        optionFromCloud= (LinearLayout) contentView.findViewById(R.id.attachment_upload_cloud_layout);
        optionContact = (LinearLayout) contentView.findViewById(R.id.attachment_upload_contact_layout);
        optionPhoto = (LinearLayout) contentView.findViewById(R.id.attachment_upload_photo_layout);

        optionFromCloud.setOnClickListener(this);
        optionContact.setOnClickListener(this);
        optionPhoto.setOnClickListener(this);

        dialog.setContentView(contentView);
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, 48));
        mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }


    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.attachment_upload_cloud_layout:{
                logDebug("Option attach from cloud");
                ((ChatActivityLollipop)context).attachFromCloud();
                dismissAllowingStateLoss();
                break;
            }
            case R.id.attachment_upload_contact_layout:{
                logDebug("Option attach contact");
                ((ChatActivityLollipop)context).chooseContactsDialog();
                dismissAllowingStateLoss();
                break;
            }

            case R.id.attachment_upload_photo_layout:{
                logDebug("Option attach photo");
                ((ChatActivityLollipop)context).attachPhotoVideo();
                dismissAllowingStateLoss();
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
}
