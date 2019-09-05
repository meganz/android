package mega.privacy.android.app.modalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.content.ContextCompat;
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
import java.util.Locale;

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
import mega.privacy.android.app.utils.ChatUtil;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtils.*;

public class ContactsBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;
    MegaContactAdapter contact = null;
    ContactController cC;

    private BottomSheetBehavior mBehavior;

    public LinearLayout mainLinearLayout;
    public EmojiTextView titleNameContactPanel;
    public TextView titleMailContactPanel;
    public RoundedImageView contactImageView;
    public EmojiTextView avatarInitialLetter;
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
            log("Bundle is NOT NULL");
            String email = savedInstanceState.getString("email");
            log("Email of the contact: "+email);
            if(email!=null){
                MegaUser megaUser = megaApi.getContact(email);
                MegaContactDB contactDB = dbH.findContactByHandle(megaUser.getHandle()+"");
                String fullName = "";
                if(contactDB!=null){
                    fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), megaUser.getEmail());
                }
                else{
                    fullName = megaUser.getEmail();
                }

                contact = new MegaContactAdapter(contactDB, megaUser, fullName);
            }
        }
        else{
            log("Bundle NULL");
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
        avatarInitialLetter = contentView.findViewById(R.id.sliding_contact_list_initial_letter);
        optionInfoContact = (LinearLayout) contentView.findViewById(R.id.contact_list_info_contact_layout);
        optionStartConversation = (LinearLayout) contentView.findViewById(R.id.contact_list_option_start_conversation_layout);
        optionSendFile= (LinearLayout) contentView.findViewById(R.id.contact_list_option_send_file_layout);
        optionSendContact = (LinearLayout) contentView.findViewById(R.id.contact_list_option_send_contact_layout);
        optionShareFolder = (LinearLayout) contentView.findViewById(R.id.contact_list_option_share_layout);
        optionRemove = (LinearLayout) contentView.findViewById(R.id.contact_list_option_remove_layout);
        contactStateIcon = (ImageView) contentView.findViewById(R.id.contact_list_drawable_state);

        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            log("onCreate: Landscape configuration");
            titleNameContactPanel.setMaxWidth(Util.scaleWidthPx(280, outMetrics));
        }
        else{
            titleNameContactPanel.setMaxWidth(Util.scaleWidthPx(230, outMetrics));
        }
        titleNameContactPanel.setEmojiSize(Util.px2dp(Constants.EMOJI_SIZE, outMetrics));
        avatarInitialLetter.setEmojiSize(Util.px2dp(Constants.EMOJI_SIZE_MEDIUM, outMetrics));

        optionInfoContact.setOnClickListener(this);
        optionRemove.setOnClickListener(this);
        optionSendFile.setOnClickListener(this);
        optionSendContact.setOnClickListener(this);
        optionShareFolder.setOnClickListener(this);

        if (Util.isChatEnabled()) {
            optionSendFile.setVisibility(View.VISIBLE);
            optionStartConversation.setVisibility(View.VISIBLE);
            optionSendContact.setVisibility(View.VISIBLE);
        }
        else {
            optionSendFile.setVisibility(View.GONE);
            optionStartConversation.setVisibility(View.GONE);
            optionSendContact.setVisibility(View.GONE);
        }

        if(contact!=null){
            fullName = contact.getFullName();
            titleNameContactPanel.setText(fullName);

            ArrayList<MegaNode> sharedNodes = megaApi.getInShares(contact.getMegaUser());
            String sharedNodesDescription = Util.getSubtitleDescription(sharedNodes);
            titleMailContactPanel.setText(sharedNodesDescription);

            addAvatarContactPanel(contact);

            if(Util.isChatEnabled()){

                if (megaChatApi == null){
                    megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
                }

                optionStartConversation.setVisibility(View.VISIBLE);
                optionStartConversation.setOnClickListener(this);

                contactStateIcon.setVisibility(View.VISIBLE);
                if (megaChatApi != null){
                    int userStatus = megaChatApi.getUserOnlineStatus(contact.getMegaUser().getHandle());
                    if(userStatus == MegaChatApi.STATUS_ONLINE){
                        log("This user is connected");
                        contactStateIcon.setVisibility(View.VISIBLE);
                        contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_online));
                    }
                    else if(userStatus == MegaChatApi.STATUS_AWAY){
                        log("This user is away");
                        contactStateIcon.setVisibility(View.VISIBLE);
                        contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_away));
                    }
                    else if(userStatus == MegaChatApi.STATUS_BUSY){
                        log("This user is busy");
                        contactStateIcon.setVisibility(View.VISIBLE);
                        contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_busy));
                    }
                    else if(userStatus == MegaChatApi.STATUS_OFFLINE){
                        log("This user is offline");
                        contactStateIcon.setVisibility(View.VISIBLE);
                        contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_offline));
                    }
                    else if(userStatus == MegaChatApi.STATUS_INVALID){
                        log("INVALID status: "+userStatus);
                        contactStateIcon.setVisibility(View.GONE);
                    }
                    else{
                        log("This user status is: "+userStatus);
                        contactStateIcon.setVisibility(View.GONE);
                    }
                }
            }
            else{
                optionStartConversation.setVisibility(View.GONE);
                contactStateIcon.setVisibility(View.GONE);
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
            log("Contact NULL");
        }
    }

//    public String getFullName(MegaUser contact){
//        String firstNameText ="";
//        String lastNameText ="";
//        MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contact.getHandle()));
//        if(contactDB!=null){
//            firstNameText = contactDB.getName();
//            lastNameText = contactDB.getLastName();
//
//            String fullName;
//
//            if (firstNameText.trim().length() <= 0){
//                fullName = lastNameText;
//            }
//            else{
//                fullName = firstNameText + " " + lastNameText;
//            }
//
//            if (fullName.trim().length() <= 0){
//                log("Put email as fullname");
//                String email = contact.getEmail();
//                String[] splitEmail = email.split("[@._]");
//                fullName = splitEmail[0];
//            }
//
//            return fullName;
//        }
//        else{
//            String email = contact.getEmail();
//            String[] splitEmail = email.split("[@._]");
//            String fullName = splitEmail[0];
//            return fullName;
//        }
//    }

    public void addAvatarContactPanel(MegaContactAdapter contact){

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
                    avatarInitialLetter.setVisibility(View.GONE);
                    contactImageView.setImageBitmap(bitmap);
                    return;
                }
            }
        }

        ////DEfault AVATAR
        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);

        if (contact != null) {
            String color = megaApi.getUserAvatarColor(contact.getMegaUser());
            if (color != null) {
                log("The color to set the avatar is " + color);
                p.setColor(Color.parseColor(color));
            } else {
                log("Default color to the avatar");
                p.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
            }
        } else {
            log("Contact is NULL");
            p.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
        }

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth() / 2;
        else
            radius = defaultAvatar.getHeight() / 2;

        c.drawCircle(defaultAvatar.getWidth() / 2, defaultAvatar.getHeight() / 2, radius, p);
        contactImageView.setImageBitmap(defaultAvatar);

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        fullName = contact.getFullName();
        String firstLetter = ChatUtil.getFirstLetter(fullName);
        if(firstLetter.trim().isEmpty() || firstLetter.equals("(")){
            avatarInitialLetter.setVisibility(View.INVISIBLE);
        }else {
            avatarInitialLetter.setText(firstLetter);
            avatarInitialLetter.setTextColor(Color.WHITE);
            avatarInitialLetter.setVisibility(View.VISIBLE);
        }
        avatarInitialLetter.setTextSize(22);
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.contact_list_info_contact_layout:{
                log("click contact info");
                if(contact==null){
                    log("Selected contact NULL");
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
                    log("Selected contact NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).startOneToOneChat(contact.getMegaUser());
                break;
            }
            case R.id.contact_list_option_send_file_layout:{
                log("optionSendFile");
                if(contact==null){
                    log("Selected contact NULL");
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
                log("optionSendContact");
                if(contact==null){
                    log("Selected contact NULL");
                    return;
                }

                ChatController cC = new ChatController(context);

                cC.selectChatsToAttachContact(contact.getMegaUser());

                dismissAllowingStateLoss();
                break;
            }
            case R.id.contact_list_option_share_layout:{
                log("optionShare");
                if(contact==null){
                    log("Selected contact NULL");
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
                log("optionRemove");
                if(contact==null){
                    log("Selected contact NULL");
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
        log("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        String email = contact.getMegaUser().getEmail();
        log("Email of the contact: "+email);
        outState.putString("email", email);
    }

    private static void log(String log) {
        Util.log("ContactsBottomSheetDialogFragment", log);
    }

    public interface CustomHeight{
        int getHeightToPanel(BottomSheetDialogFragment dialog);
    }
}
