package mega.privacy.android.app.components.reaction;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.listeners.ChatParticipantAvatarListener;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.AvatarUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.CacheFolderManager.*;


public class UserReactionAdapter extends ArrayAdapter<Long> implements View.OnClickListener {
    private static final int MAX_WIDTH_PORT = 180;
    private static final int MAX_WIDTH_LAND = 260;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    private MegaChatRoom chatRoom;
    private Context context;

    public UserReactionAdapter(Context context, ArrayList<Long> users, long chatId, long messageId) {
        super(context, 0, users);
        this.context = context;

        if (megaApi == null) {
            megaApi = MegaApplication.getInstance().getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        }
        chatRoom = megaChatApi.getChatRoom(chatId);

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        long userHandle = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.user_reaction_item, parent, false);
        }
        RelativeLayout layout = convertView.findViewById(R.id.layout);
        layout.setOnClickListener(this);
        RoundedImageView imageView = convertView.findViewById(R.id.contact_list_thumbnail);
        EmojiTextView name = convertView.findViewById(R.id.contact_list_name);

        if (isScreenInPortrait(context)) {
            name.setMaxWidthEmojis(scaleWidthPx(MAX_WIDTH_PORT, outMetrics));
        } else {
            name.setMaxWidthEmojis(scaleWidthPx(MAX_WIDTH_LAND, outMetrics));
        }

        String userName;
        String email;

        if (userHandle == megaChatApi.getMyUserHandle()) {
            email = megaChatApi.getMyEmail();
            userName = megaChatApi.getMyFullname();
            if (isTextEmpty(userName)) {
                userName = email;
            }
            userName = getContext().getString(R.string.chat_me_text_bracket, userName);

        } else {
            MegaContactDB contactDB = getContactDB(userHandle);
            if (contactDB != null) {
                userName = getContactNameDB(contactDB);
                email = contactDB.getMail();
            } else {
                email = chatRoom.getPeerEmail(userHandle);
                userName = chatRoom.getPeerFullname(userHandle);
                if (isTextEmpty(userName)) {
                    userName = email;
                }
            }
        }

        name.setText(userName);

        /*Default Avatar*/
        int color = getColorAvatar(context, megaApi, userHandle);
        imageView.setImageBitmap(getDefaultAvatar(context, color, userName, AVATAR_SIZE, true));

        /*Avatar*/
        if (userHandle == megaChatApi.getMyUserHandle()) {
            File avatar = buildAvatarFile(context, email + ".jpg");
            Bitmap bitmap;
            if (avatar != null && avatar.exists() && avatar.length() > 0) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }

        } else {
            ChatParticipantAvatarListener listener = new ChatParticipantAvatarListener(context, imageView, email);
            logDebug("The participant is contact!!");
            MegaUser contact = megaApi.getContact(email);
            if (contact != null) {
                File avatar = buildAvatarFile(context, email + ".jpg");
                Bitmap bitmap;
                if(isFileAvailable(avatar) && avatar.length() > 0){
                    BitmapFactory.Options bOpts = new BitmapFactory.Options();
                    bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                        return convertView;
                    }
                }

                megaApi.getUserAvatar(contact, buildAvatarFile(context, contact.getEmail() + ".jpg").getAbsolutePath(), listener);
            } else {

                megaApi.getUserAvatar(email, buildAvatarFile(context, email + ".jpg").getAbsolutePath(), listener);
            }
        }

        return convertView;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.layout:
                break;
        }
    }
}