package mega.privacy.android.app.components.twemoji.reaction;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

import static mega.privacy.android.app.utils.AvatarUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class UserReactionAdapter extends ArrayAdapter<Long> {

    private static final int MAX_WIDTH_PORT = 180;
    private static final int MAX_WIDTH_LAND = 260;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    private Context context;

    public UserReactionAdapter(Context context, ArrayList<Long> users, long chatId) {
        super(context, 0, users);
        this.context = context;

        if (megaApi == null) {
            megaApi = MegaApplication.getInstance().getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        }
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
        RoundedImageView imageView = convertView.findViewById(R.id.contact_list_thumbnail);
        EmojiTextView name = convertView.findViewById(R.id.contact_list_name);
        name.setMaxWidthEmojis(scaleWidthPx(isScreenInPortrait(context) ? MAX_WIDTH_PORT : MAX_WIDTH_LAND, outMetrics));

        String userName;
        String email;
        ChatController chatC = new ChatController(context);

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
                chatC.setNonContactAttributesInDB(userHandle);
                email = chatC.getParticipantEmail(userHandle);
                userName = chatC.getParticipantFullName(userHandle);
                if (isTextEmpty(userName)) {
                    userName = email;
                }
            }
        }

        name.setText(userName);

        /*Default Avatar*/
        int avatarColor = getColorAvatar(userHandle);
        imageView.setImageBitmap(getDefaultAvatar(avatarColor, userName, AVATAR_SIZE, true));

        /*Avatar*/
        String userHandleString = MegaApiAndroid.userHandleToBase64(userHandle);
        String myUserHandleEncoded = MegaApiAndroid.userHandleToBase64(megaChatApi.getMyUserHandle());

        if (userHandleString.equals(myUserHandleEncoded)) {
            Bitmap bitmap = getAvatarBitmap(email);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        } else {
            String nameFileHandle = userHandleString;
            String nameFileEmail = email;
            Bitmap bitmap = isTextEmpty(nameFileEmail) ? getAvatarBitmap(nameFileHandle) : getUserAvatar(nameFileHandle, nameFileEmail);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }

        return convertView;
    }
}