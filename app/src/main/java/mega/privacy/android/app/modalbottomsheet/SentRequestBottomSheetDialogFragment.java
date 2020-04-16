package mega.privacy.android.app.modalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaContactRequest;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;


public class SentRequestBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;
    MegaContactRequest request = null;
    ContactController cC;

    private BottomSheetBehavior mBehavior;
    private LinearLayout items_layout;

    public LinearLayout mainLinearLayout;
    public TextView titleNameContactChatPanel;
    public TextView titleMailContactChatPanel;
    public RoundedImageView contactImageView;
    public LinearLayout optionReinvite;
    public LinearLayout optionDelete;

    DisplayMetrics outMetrics;
    private int heightDisplay;

    MegaApiAndroid megaApi;
    DatabaseHandler dbH;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if(savedInstanceState!=null) {
            logDebug("Bundle is NOT NULL");
            long handle = savedInstanceState.getLong("handle", -1);
            logDebug("Handle of the request: " + handle);
            request = megaApi.getContactRequestByHandle(handle);
        }
        else{
            logWarning("Bundle NULL");
            if(context instanceof ManagerActivityLollipop){
                request = ((ManagerActivityLollipop) context).getSelectedRequest();
            }
        }

        cC = new ContactController(context);

        dbH = DatabaseHandler.getDbHandler(getActivity());
    }
    @Override
    public void setupDialog(final Dialog dialog, int style) {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        heightDisplay = outMetrics.heightPixels;

        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_sent_request, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.sent_request_item_bottom_sheet);
        items_layout = (LinearLayout) contentView.findViewById(R.id.items_layout);

        titleNameContactChatPanel = (TextView) contentView.findViewById(R.id.sent_request_list_contact_name_text);
        titleMailContactChatPanel = (TextView) contentView.findViewById(R.id.sent_request_list_contact_mail_text);
        contactImageView = (RoundedImageView) contentView.findViewById(R.id.sliding_sent_request_list_thumbnail);
        optionReinvite = (LinearLayout) contentView.findViewById(R.id.contact_list_option_reinvite_layout);
        optionDelete= (LinearLayout) contentView.findViewById(R.id.contact_list_option_delete_request_layout);

        titleNameContactChatPanel.setMaxWidth(scaleWidthPx(200, outMetrics));
        titleMailContactChatPanel.setMaxWidth(scaleWidthPx(200, outMetrics));

        optionReinvite.setOnClickListener(this);
        optionDelete.setOnClickListener(this);

        if(request!=null){
            titleNameContactChatPanel.setText(request.getTargetEmail());
            titleMailContactChatPanel.setText(""+ DateUtils.getRelativeTimeSpanString(request.getCreationTime() * 1000));

            addAvatarRequestPanel(request);

            dialog.setContentView(contentView);
            mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
            mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, 81));
            mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        }
        else{
            logWarning("Request NULL");
        }
    }

    public void addAvatarRequestPanel(MegaContactRequest request){
        /*Default Avatar*/
        contactImageView.setImageBitmap(getDefaultAvatar(getSpecificAvatarColor(AVATAR_PRIMARY_COLOR), request.getTargetEmail(), AVATAR_SIZE, true));
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.contact_list_option_reinvite_layout:{
                logDebug("Click reinvite");
                if(request==null){
                    logWarning("Selected request NULL");
                    return;
                }

                cC.reinviteContact(request);
                break;
            }
            case R.id.contact_list_option_delete_request_layout:{
                logDebug("Option Delete");
                if(request==null){
                    logWarning("Selected request NULL");
                    return;
                }
                ((ManagerActivityLollipop)context).showConfirmationRemoveContactRequest(request);
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

    @Override
    public void onSaveInstanceState(Bundle outState){
        logDebug("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        long handle = request.getHandle();
        logDebug("Handle of the request: " + handle);
        outState.putLong("handle", handle);
    }
}
