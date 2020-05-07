package mega.privacy.android.app.modalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileContactListActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;

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
    public EmojiTextView titleNameContactPanel;
    public TextView titleMailContactPanel;
    public RoundedImageView contactImageView;
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
            logDebug("Bundle is NOT NULL");
            String email = savedInstanceState.getString("email");
            logDebug("Email of the contact: " + email);
            if(email!=null){
                contact = megaApi.getContact(email);
                if(contact==null){
                    nonContactEmail = email;
                }
            }
        }
        else{
            logWarning("Bundle NULL");
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

        titleNameContactPanel = contentView.findViewById(R.id.file_contact_list_contact_name_text);
        titleMailContactPanel = (TextView) contentView.findViewById(R.id.file_contact_list_contact_mail_text);
        contactImageView = (RoundedImageView) contentView.findViewById(R.id.sliding_file_contact_list_thumbnail);

        optionChangePermissions = (LinearLayout) contentView.findViewById(R.id.file_contact_list_option_permissions_layout);
        optionDelete = (LinearLayout) contentView.findViewById(R.id.file_contact_list_option_delete_layout);

        optionInfo = (LinearLayout) contentView.findViewById(R.id.file_contact_list_option_info_layout);
        optionInfo.setOnClickListener(this);

        titleNameContactPanel.setMaxWidthEmojis(scaleWidthPx(200, outMetrics));
        titleMailContactPanel.setMaxWidth(scaleWidthPx(200, outMetrics));

        optionChangePermissions.setOnClickListener(this);
        optionDelete.setOnClickListener(this);

        LinearLayout separatorInfo = (LinearLayout) contentView.findViewById(R.id.separator_info);

        if(contact!=null){
            fullName = getMegaUserNameDB(contact);
        }
        else{
            logWarning("Contact NULL");
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

            if (share.isPending()) {
                titleMailContactPanel.append(" " + getString(R.string.pending_outshare_indicator));
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

    public void addAvatarContactPanel(MegaUser contact){

        /*Default Avatar*/
        contactImageView.setImageBitmap(getDefaultAvatar(getColorAvatar(contact), fullName, AVATAR_SIZE, false));

        /*Avatar*/
        if(contact!=null){
            String contactMail = contact.getEmail();
            File avatar = buildAvatarFile(getActivity(),contactMail + ".jpg");
            Bitmap bitmap = null;
            if (isFileAvailable(avatar) && avatar.length() > 0){
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
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

            case R.id.file_contact_list_option_permissions_layout:{
                logDebug("Permissions layout");
                if(context instanceof FileContactListActivityLollipop){
                    ((FileContactListActivityLollipop)context).changePermissions();
                }else if(context instanceof FileInfoActivityLollipop){
                    ((FileInfoActivityLollipop)context).changePermissions();
                }
                break;
            }
            case R.id.file_contact_list_option_delete_layout:{
                logDebug("Option delete");
                if(context instanceof FileContactListActivityLollipop){
                    ((FileContactListActivityLollipop)context).removeFileContactShare();
                }else if(context instanceof FileInfoActivityLollipop){
                    ((FileInfoActivityLollipop)context).removeFileContactShare();
                }
                break;
            }
            case R.id.file_contact_list_option_info_layout:{
                logDebug("Option send file");
                if(contact==null){
                    logWarning("Selected contact NULL");
                    return;
                }

                logDebug("Contact info participants panel");
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
        logDebug("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        String email = contact.getEmail();
        logDebug("Email of the contact: " + email);
        outState.putString("email", email);
    }
}
