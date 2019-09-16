package mega.privacy.android.app.modalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileContactListActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.LogUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtils.*;

public class FileContactsListBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;
    MegaUser contact = null;
    MegaNode node = null;
    MegaShare share = null;
    ContactController cC;
    String nonContactEmail;

    private BottomSheetBehavior mBehavior;
    private LinearLayout items_layout;

    public LinearLayout mainLinearLayout;
    public TextView titleNameContactPanel;
    public TextView titleMailContactPanel;
    public RoundedImageView contactImageView;
    public TextView avatarInitialLetter;
    public LinearLayout optionChangePermissions;
    public LinearLayout optionDelete;
    public LinearLayout optionInfo;

    String fullName="";

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
            LogUtil.logDebug("Bundle is NOT NULL");
            String email = savedInstanceState.getString("email");
            LogUtil.logDebug("Email of the contact: " + email);
            if(email!=null){
                contact = megaApi.getContact(email);
                if(contact==null){
                    nonContactEmail = email;
                }
            }
        }
        else{
            LogUtil.logWarning("Bundle NULL");
            if(context instanceof FileContactListActivityLollipop){
                share = ((FileContactListActivityLollipop) context).getSelectedShare();
                contact = ((FileContactListActivityLollipop) context).getSelectedContact();
            }else if(context instanceof FileInfoActivityLollipop){
                share = ((FileInfoActivityLollipop) context).getSelectedShare();
                contact = ((FileInfoActivityLollipop) context).getSelectedContact();
            }
            
            if(contact==null){
                nonContactEmail = share.getUser();
            }
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
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_file_contact_list, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.file_contact_list_bottom_sheet);
        items_layout = (LinearLayout) contentView.findViewById(R.id.items_layout);

        titleNameContactPanel = (TextView) contentView.findViewById(R.id.file_contact_list_contact_name_text);
        titleMailContactPanel = (TextView) contentView.findViewById(R.id.file_contact_list_contact_mail_text);
        contactImageView = (RoundedImageView) contentView.findViewById(R.id.sliding_file_contact_list_thumbnail);
        avatarInitialLetter = (TextView) contentView.findViewById(R.id.sliding_file_contact_list_initial_letter);

        optionChangePermissions = (LinearLayout) contentView.findViewById(R.id.file_contact_list_option_permissions_layout);
        optionDelete = (LinearLayout) contentView.findViewById(R.id.file_contact_list_option_delete_layout);

        optionInfo = (LinearLayout) contentView.findViewById(R.id.file_contact_list_option_info_layout);
        optionInfo.setOnClickListener(this);

        titleNameContactPanel.setMaxWidth(Util.scaleWidthPx(200, outMetrics));
        titleMailContactPanel.setMaxWidth(Util.scaleWidthPx(200, outMetrics));

        optionChangePermissions.setOnClickListener(this);
        optionDelete.setOnClickListener(this);

        LinearLayout separatorInfo = (LinearLayout) contentView.findViewById(R.id.separator_info);

        if(contact!=null){
            fullName = getFullName(contact);
        }
        else{
            LogUtil.logWarning("Contact NULL");
//            nonContactEmail
            fullName = nonContactEmail;
        }

        if(contact!=null && contact.getVisibility()==MegaUser.VISIBILITY_VISIBLE){
            optionInfo.setVisibility(View.VISIBLE);
            separatorInfo.setVisibility(View.VISIBLE);
        }
        else{
            optionInfo.setVisibility(View.GONE);
            separatorInfo.setVisibility(View.GONE);
        }

        titleNameContactPanel.setText(fullName);
        addAvatarContactPanel(contact);

        if(share!=null){
            int accessLevel = share.getAccess();
            switch(accessLevel){
                case MegaShare.ACCESS_OWNER:
                case MegaShare.ACCESS_FULL:{
                    titleMailContactPanel.setText(context.getString(R.string.file_properties_shared_folder_full_access));
                    break;
                }
                case MegaShare.ACCESS_READ:{
                    titleMailContactPanel.setText(context.getString(R.string.file_properties_shared_folder_read_only));
                    break;
                }
                case MegaShare.ACCESS_READWRITE:{
                    titleMailContactPanel.setText(context.getString(R.string.file_properties_shared_folder_read_write));
                    break;
                }
            }
        }
        else{
            titleMailContactPanel.setText(contact.getEmail());
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

        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, 81));
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    public String getFullName(MegaUser contact){
        String firstNameText ="";
        String lastNameText ="";
        MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contact.getHandle()));
        if(contactDB!=null){
            firstNameText = contactDB.getName();
            lastNameText = contactDB.getLastName();

            String fullName;

            if (firstNameText.trim().length() <= 0){
                fullName = lastNameText;
            }
            else{
                fullName = firstNameText + " " + lastNameText;
            }

            if (fullName.trim().length() <= 0){
                LogUtil.logDebug("Put email as fullname");
                String email = contact.getEmail();
                String[] splitEmail = email.split("[@._]");
                fullName = splitEmail[0];
            }

            return fullName;
        }
        else{
            String email = contact.getEmail();
            String[] splitEmail = email.split("[@._]");
            String fullName = splitEmail[0];
            return fullName;
        }
    }

    public void addAvatarContactPanel(MegaUser contact){

        if(contact!=null){
            String contactMail = contact.getEmail();
            File avatar = buildAvatarFile(getActivity(),contactMail + ".jpg");
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
        }

        ////DEfault AVATAR
        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);

        if (contact != null) {
            String color = megaApi.getUserAvatarColor(contact);
            if (color != null) {
                LogUtil.logDebug("The color to set the avatar is " + color);
                p.setColor(Color.parseColor(color));
            } else {
                LogUtil.logDebug("Default color to the avatar");
                p.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
            }
        } else {
            LogUtil.logWarning("Contact is NULL");
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

        if (fullName != null) {
            if (fullName.length() > 0) {
                if (fullName.trim().length() > 0) {
                    String firstLetter = fullName.charAt(0) + "";
                    firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                    avatarInitialLetter.setText(firstLetter);
                    avatarInitialLetter.setTextColor(Color.WHITE);
                    avatarInitialLetter.setVisibility(View.VISIBLE);
                    avatarInitialLetter.setTextSize(22);
                } else {
                    avatarInitialLetter.setVisibility(View.INVISIBLE);
                }
            }
            else{
                avatarInitialLetter.setVisibility(View.INVISIBLE);
            }

        } else {
            avatarInitialLetter.setVisibility(View.INVISIBLE);
        }

        ////
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.file_contact_list_option_permissions_layout:{
                LogUtil.logDebug("Permissions layout");
                if(context instanceof FileContactListActivityLollipop){
                    ((FileContactListActivityLollipop)context).changePermissions();
                }else if(context instanceof FileInfoActivityLollipop){
                    ((FileInfoActivityLollipop)context).changePermissions();
                }
                break;
            }
            case R.id.file_contact_list_option_delete_layout:{
                LogUtil.logDebug("Option delete");
                if(context instanceof FileContactListActivityLollipop){
                    ((FileContactListActivityLollipop)context).removeFileContactShare();
                }else if(context instanceof FileInfoActivityLollipop){
                    ((FileInfoActivityLollipop)context).removeFileContactShare();
                }
                break;
            }
            case R.id.file_contact_list_option_info_layout:{
                LogUtil.logDebug("Option send file");
                if(contact==null){
                    LogUtil.logWarning("Selected contact NULL");
                    return;
                }

                LogUtil.logDebug("Contact info participants panel");
                Intent i = new Intent(context, ContactInfoActivityLollipop.class);
                i.putExtra("name", share.getUser());
                context.startActivity(i);
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

    @Override
    public void onSaveInstanceState(Bundle outState){
        LogUtil.logDebug("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        String email = contact.getEmail();
        LogUtil.logDebug("Email of the contact: " + email);
        outState.putString("email", email);
    }
}
