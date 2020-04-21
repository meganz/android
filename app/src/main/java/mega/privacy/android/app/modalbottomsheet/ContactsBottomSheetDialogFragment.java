package mega.privacy.android.app.modalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import androidx.core.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;


public class ContactsBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;
    MegaContactAdapter contact = null;
    ContactController cC;

    private BottomSheetBehavior mBehavior;

    public LinearLayout mainLinearLayout;
    public EmojiTextView titleNameContactPanel;
    public TextView titleMailContactPanel;
    public RoundedImageView contactImageView;
    public LinearLayout optionInfoContact;
    public LinearLayout optionStartConversation;
    public LinearLayout optionSendFile;
    public LinearLayout optionSendContact;
    public LinearLayout optionShareFolder;
    public LinearLayout optionRemove;
    ImageView contactStateIcon;

    private LinearLayout items_layout;

    String fullName="";

    DisplayMetrics outMetrics;

    private int height = -1;
    private boolean heightseted = false;
    private int heightReal = -1;
    private int heightDisplay;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    DatabaseHandler dbH;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
        cC = new ContactController(context);

        dbH = DatabaseHandler.getDbHandler(getActivity());

        if(savedInstanceState!=null) {
            logDebug("Bundle is NOT NULL");
            String email = savedInstanceState.getString("email");
            logDebug("Email of the contact: " + email);
            if(email!=null){
                MegaUser megaUser = megaApi.getContact(email);
                String fullName = getMegaUserNameDB(megaUser);
                if (fullName == null) {
                    fullName = megaUser.getEmail();
                }
                contact = new MegaContactAdapter(getContactDB(megaUser.getHandle()), megaUser, fullName);
            }
        }
        else{
            logWarning("Bundle NULL");
            if(context instanceof ManagerActivityLollipop){
                contact = ((ManagerActivityLollipop) context).getSelectedUser();
            }
        }

    }
    @Override
    public void setupDialog(final Dialog dialog, int style) {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        heightDisplay = outMetrics.heightPixels;

        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_contact_item, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.contact_item_bottom_sheet);

        mainLinearLayout.post(new Runnable() {
            @Override
            public void run() {
                heightReal = mainLinearLayout.getHeight();
            }
        });

        items_layout = (LinearLayout) contentView.findViewById(R.id.items_layout_bottom_sheet_contact);

        titleNameContactPanel = contentView.findViewById(R.id.contact_list_contact_name_text);
        titleMailContactPanel = (TextView) contentView.findViewById(R.id.contact_list_contact_mail_text);
        contactImageView = (RoundedImageView) contentView.findViewById(R.id.sliding_contact_list_thumbnail);
        optionInfoContact = (LinearLayout) contentView.findViewById(R.id.contact_list_info_contact_layout);
        optionStartConversation = (LinearLayout) contentView.findViewById(R.id.contact_list_option_start_conversation_layout);
        optionSendFile= (LinearLayout) contentView.findViewById(R.id.contact_list_option_send_file_layout);
        optionSendContact = (LinearLayout) contentView.findViewById(R.id.contact_list_option_send_contact_layout);
        optionShareFolder = (LinearLayout) contentView.findViewById(R.id.contact_list_option_share_layout);
        optionRemove = (LinearLayout) contentView.findViewById(R.id.contact_list_option_remove_layout);
        contactStateIcon = (ImageView) contentView.findViewById(R.id.contact_list_drawable_state);

        if(isScreenInPortrait(context)){
            titleNameContactPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));
            titleMailContactPanel.setMaxWidth(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));
        }else{
            titleNameContactPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND, outMetrics));
            titleMailContactPanel.setMaxWidth(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND, outMetrics));
        }

        optionInfoContact.setOnClickListener(this);
        optionRemove.setOnClickListener(this);
        optionSendFile.setOnClickListener(this);
        optionSendContact.setOnClickListener(this);
        optionShareFolder.setOnClickListener(this);

        optionSendFile.setVisibility(View.VISIBLE);
        optionStartConversation.setVisibility(View.VISIBLE);
        optionSendContact.setVisibility(View.VISIBLE);

        if(contact!=null){
            fullName = contact.getFullName();
            titleNameContactPanel.setText(fullName);

            ArrayList<MegaNode> sharedNodes = megaApi.getInShares(contact.getMegaUser());
            String sharedNodesDescription = getSubtitleDescription(sharedNodes);
            titleMailContactPanel.setText(sharedNodesDescription);

            addAvatarContactPanel(contact);

            if (megaChatApi == null) {
                megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
            }

            optionStartConversation.setVisibility(View.VISIBLE);
            optionStartConversation.setOnClickListener(this);

            contactStateIcon.setVisibility(View.VISIBLE);
            if (megaChatApi != null) {
                int userStatus = megaChatApi.getUserOnlineStatus(contact.getMegaUser().getHandle());
                if (userStatus == MegaChatApi.STATUS_ONLINE) {
                    logDebug("This user is connected");
                    contactStateIcon.setVisibility(View.VISIBLE);
                    contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_online));
                } else if (userStatus == MegaChatApi.STATUS_AWAY) {
                    logDebug("This user is away");
                    contactStateIcon.setVisibility(View.VISIBLE);
                    contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_away));
                } else if (userStatus == MegaChatApi.STATUS_BUSY) {
                    logDebug("This user is busy");
                    contactStateIcon.setVisibility(View.VISIBLE);
                    contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_busy));
                } else if (userStatus == MegaChatApi.STATUS_OFFLINE) {
                    logDebug("This user is offline");
                    contactStateIcon.setVisibility(View.VISIBLE);
                    contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_offline));
                } else if (userStatus == MegaChatApi.STATUS_INVALID) {
                    logWarning("INVALID status: " + userStatus);
                    contactStateIcon.setVisibility(View.GONE);
                } else {
                    logDebug("This user status is: " + userStatus);
                    contactStateIcon.setVisibility(View.GONE);
                }
            }

            dialog.setContentView(contentView);
            mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
//            mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
//
//            final ContactsBottomSheetDialogFragment thisclass = this;
//
//
//            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                mBehavior.setPeekHeight((heightDisplay / 4) * 2);
//            }
//            else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
//                mBehavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
//            }

            mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, 81));
            mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);


            mBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if(newState == BottomSheetBehavior.STATE_HIDDEN){
                        dismissAllowingStateLoss();
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
//                    if(slideOffset> 0 && !heightseted){
//                        if(context instanceof CustomHeight){
//                            height = ((CustomHeight) context).getHeightToPanel(thisclass);
//                        }
//                        if(height != -1 && heightReal != -1){
//                            heightseted = true;
//                            int numSons = 0;
//                            int num = items_layout.getChildCount();
//                            for(int i=0; i<num; i++){
//                                View v = items_layout.getChildAt(i);
//                                if(v.getVisibility() == View.VISIBLE){
//                                    numSons++;
//                                }
//                            }
//                            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && numSons > 3){
//
//                                ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
//                                params.height = height;
//                                bottomSheet.setLayoutParams(params);
//                            }
//                            else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && numSons > 9){
//                                ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
//                                params.height = height;
//                                bottomSheet.setLayoutParams(params);
////                            }
//                            if(heightReal > height){
//                                ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
//                                params.height = height;
//                                bottomSheet.setLayoutParams(params);
//                            }
//                        }
//                    }
                    if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
                        ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                        if (getActivity() != null && getActivity().findViewById(R.id.toolbar) != null) {
                            int tBHeight = getActivity().findViewById(R.id.toolbar).getHeight();
                            Rect rectangle = new Rect();
                            getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(rectangle);
                            int windowHeight = rectangle.bottom;
                            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());
                            int maxHeight = windowHeight - tBHeight - rectangle.top - padding;

//                            log("bottomSheet.height: " + mainLinearLayout.getHeight() + " maxHeight: " + maxHeight);
                            if (mainLinearLayout.getHeight() > maxHeight) {
                                params.height = maxHeight;
                                bottomSheet.setLayoutParams(params);
                            }
                        }
                    }
                }
            });
        }
        else{
            logWarning("Contact NULL");
        }
    }

    public void addAvatarContactPanel(MegaContactAdapter contact){
        /*Default Avatar*/
        contactImageView.setImageBitmap(getDefaultAvatar(getColorAvatar(contact.getMegaUser()), contact.getFullName(), AVATAR_SIZE, true));

        /*Avatar*/
        String contactMail = contact.getMegaUser().getEmail();
        File avatar = buildAvatarFile(getActivity(), contactMail + ".jpg");
        Bitmap bitmap = null;
        if (isFileAvailable(avatar)){
            if (avatar.length() > 0){
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                if (bitmap == null) {
                    avatar.delete();
                }
                else{
                    contactImageView.setImageBitmap(bitmap);
                    return;
                }
            }
        }
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.contact_list_info_contact_layout:{
                logDebug("Contact info");
                if(contact==null){
                    logWarning("Selected contact NULL");
                    return;
                }

                Intent i = new Intent(context, ContactInfoActivityLollipop.class);
                i.putExtra("name", contact.getMegaUser().getEmail());
                context.startActivity(i);

                dismissAllowingStateLoss();
                break;
            }
            case R.id.contact_list_option_start_conversation_layout:{
                if(contact==null){
                    logWarning("Selected contact NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).startOneToOneChat(contact.getMegaUser());
                break;
            }
            case R.id.contact_list_option_send_file_layout:{
                logDebug("Option send file");
                if(contact==null){
                    logWarning("Selected contact NULL");
                    return;
                }
                List<MegaUser> user = new ArrayList<MegaUser>();
                user.add(contact.getMegaUser());
                ContactController cC = new ContactController(context);
                cC.pickFileToSend(user);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.contact_list_option_send_contact_layout:{
                logDebug("Option send contact");
                if(contact==null){
                    logWarning("Selected contact NULL");
                    return;
                }

                ChatController cC = new ChatController(context);

                cC.selectChatsToAttachContact(contact.getMegaUser());

                dismissAllowingStateLoss();
                break;
            }
            case R.id.contact_list_option_share_layout:{
                logDebug("Option share");
                if(contact==null){
                    logWarning("Selected contact NULL");
                    return;
                }
                List<MegaUser> user = new ArrayList<MegaUser>();
                user.add(contact.getMegaUser());
                ContactController cC = new ContactController(context);
                cC.pickFolderToShare(user);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.contact_list_option_remove_layout:{
                logDebug("Option remove");
                if(contact==null){
                    logWarning("Selected contact NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).showConfirmationRemoveContact(contact.getMegaUser());
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
        String email = contact.getMegaUser().getEmail();
        logDebug("Email of the contact: " + email);
        outState.putString("email", email);
    }

    public interface CustomHeight{
        int getHeightToPanel(BottomSheetDialogFragment dialog);
    }
}
