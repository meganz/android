package mega.privacy.android.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MarqueeTextView;
import mega.privacy.android.app.components.twemoji.EmojiEditText;
import mega.privacy.android.app.components.twemoji.EmojiManager;
import mega.privacy.android.app.components.twemoji.EmojiRange;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.components.twemoji.EmojiUtilsShortcodes;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;
import mega.privacy.android.app.lollipop.listeners.ManageReactionListener;
import mega.privacy.android.app.lollipop.listeners.AudioFocusListener;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentHistoryActivity;
import nz.mega.sdk.AndroidGfxProcessor;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaPushNotificationSettings;
import nz.mega.sdk.MegaStringList;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;
import static mega.privacy.android.app.utils.Util.*;

public class ChatUtil {
    private static final int MIN_WIDTH = 44;
    private static final float TEXT_SIZE = 14f;
    private static final float DOWNSCALE_IMAGES_PX = 2000000f;
    private static final boolean SHOULD_BUILD_FOCUS_REQUEST = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    public static final int AUDIOFOCUS_DEFAULT = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE;
    public static final int STREAM_MUSIC_DEFAULT = AudioManager.STREAM_MUSIC;

    public static boolean isVoiceClip(String name) {
        return MimeTypeList.typeForName(name).isAudioVoiceClip();
    }

    public static long getVoiceClipDuration(MegaNode node) {
        return node.getDuration() <= 0 ? 0 : node.getDuration() * 1000;
    }

    /* Get the height of the action bar */
    public static int getActionBarHeight(Activity activity, Resources resources) {
        int actionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (activity != null && activity.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, resources.getDisplayMetrics());
        }
        return actionBarHeight;
    }

    private static int getRealLength(CharSequence text){
        int length = text.length();

        List<EmojiRange> emojisFound = EmojiManager.getInstance().findAllEmojis(text);
        int count = 0;
        if(emojisFound.size() > 0){
            for (int i=0; i<emojisFound.size();i++) {
                count = count + (emojisFound.get(i).end - emojisFound.get(i).start);
            }
            return length + count;

        }
        return length;
    }

    public static int getMaxAllowed(@Nullable CharSequence text) {
        int realLength = getRealLength(text);
        if (realLength > MAX_ALLOWED_CHARACTERS_AND_EMOJIS) {
            return text.length();
        }
        return MAX_ALLOWED_CHARACTERS_AND_EMOJIS;
    }

    public static boolean isAllowedTitle(String text) {
        return getMaxAllowed(text) != text.length() || getRealLength(text) == MAX_ALLOWED_CHARACTERS_AND_EMOJIS;
    }

    public static void showShareChatLinkDialog (final Context context, MegaChatRoom chat, final String chatLink) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
        LayoutInflater inflater = null;
        if (context instanceof GroupChatInfoActivityLollipop) {
            inflater = ((GroupChatInfoActivityLollipop) context).getLayoutInflater();
        } else if (context instanceof ChatActivityLollipop) {
            inflater = ((ChatActivityLollipop) context).getLayoutInflater();
        }
        View v = inflater.inflate(R.layout.chat_link_share_dialog, null);
        builder.setView(v);
        final AlertDialog shareLinkDialog = builder.create();

        EmojiTextView nameGroup = v.findViewById(R.id.group_name_text);
        nameGroup.setText(getTitleChat(chat));
        TextView chatLinkText = (TextView) v.findViewById(R.id.chat_link_text);
        chatLinkText.setText(chatLink);

        final boolean isModerator = chat.getOwnPrivilege() == MegaChatRoom.PRIV_MODERATOR ? true : false;

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.copy_button: {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", chatLink);
                        clipboard.setPrimaryClip(clip);
                        if (context instanceof GroupChatInfoActivityLollipop) {
                            ((GroupChatInfoActivityLollipop) context).showSnackbar(context.getString(R.string.chat_link_copied_clipboard));
                        } else if (context instanceof ChatActivityLollipop) {
                            ((ChatActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.chat_link_copied_clipboard), -1);

                        }
                        dismissShareChatLinkDialog(context, shareLinkDialog);
                        break;
                    }
                    case R.id.share_button: {
                        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                        sharingIntent.setType("text/plain");
                        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, chatLink);
                        context.startActivity(Intent.createChooser(sharingIntent, context.getString(R.string.context_share)));
                        dismissShareChatLinkDialog(context, shareLinkDialog);
                        break;
                    }
                    case R.id.delete_button: {
                        if (isModerator) {
                            showConfirmationRemoveChatLink(context);
                        }
                        dismissShareChatLinkDialog(context, shareLinkDialog);
                        break;
                    }
                    case R.id.dismiss_button: {
                        dismissShareChatLinkDialog(context, shareLinkDialog);
                        break;
                    }
                }
            }
        };

        Button copyButton = (Button) v.findViewById(R.id.copy_button);
        copyButton.setOnClickListener(clickListener);
        Button shareButton = (Button) v.findViewById(R.id.share_button);
        shareButton.setOnClickListener(clickListener);
        Button deleteButton = (Button) v.findViewById(R.id.delete_button);
        if (isModerator) {
            deleteButton.setVisibility(View.VISIBLE);
        } else {
            deleteButton.setVisibility(View.GONE);
        }
        deleteButton.setOnClickListener(clickListener);
        Button dismissButton = (Button) v.findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(clickListener);

        shareLinkDialog.setCancelable(false);
        shareLinkDialog.setCanceledOnTouchOutside(false);
        try {
            shareLinkDialog.show();
        } catch (Exception e) {
        }
    }

    private static void dismissShareChatLinkDialog(Context context, AlertDialog shareLinkDialog) {
        try {
            shareLinkDialog.dismiss();
            if (context instanceof ChatActivityLollipop) {
                ((ChatActivityLollipop) context).setShareLinkDialogDismissed(true);
            }
        } catch (Exception e) {
        }
    }

    public static void showConfirmationRemoveChatLink(final Context context) {
        logDebug("showConfirmationRemoveChatLink");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        if (context instanceof GroupChatInfoActivityLollipop) {
                            ((GroupChatInfoActivityLollipop) context).removeChatLink();
                        } else if (context instanceof ChatActivityLollipop) {
                            ((ChatActivityLollipop) context).removeChatLink();
                        }
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
        builder.setTitle(R.string.action_delete_link);
        builder.setMessage(R.string.context_remove_chat_link_warning_text).setPositiveButton(R.string.delete_button, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    public static MegaChatMessage getMegaChatMessage(Context context, MegaChatApiAndroid megaChatApi, long chatId, long messageId) {
        if (context instanceof NodeAttachmentHistoryActivity) {
            return megaChatApi.getMessageFromNodeHistory(chatId, messageId);
        } else {
            return megaChatApi.getMessage(chatId, messageId);
        }

    }

    /**
     * Locks the device window in landscape mode.
     */
    public static void lockOrientationLandscape(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    /**
     * Locks the device window in reverse landscape mode.
     */
    public static void lockOrientationReverseLandscape(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
    }

    /**
     * Locks the device window in portrait mode.
     */
    public static void lockOrientationPortrait(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    /**
     * Locks the device window in reverse portrait mode.
     */
    public static void lockOrientationReversePortrait(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
    }

    /**
     * Allows user to freely use portrait or landscape mode.
     */
    public static void unlockOrientation(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    public static String converterShortCodes(String text) {
        if (text == null || text.isEmpty()) return text;
        return EmojiUtilsShortcodes.emojify(text);
    }

    public static boolean areDrawablesIdentical(Drawable drawableA, Drawable drawableB) {
        Drawable.ConstantState stateA = drawableA.getConstantState();
        Drawable.ConstantState stateB = drawableB.getConstantState();
        return (stateA != null && stateB != null && stateA.equals(stateB)) || getBitmap(drawableA).sameAs(getBitmap(drawableB));
    }

    private static Bitmap getBitmap(Drawable drawable) {
        Bitmap result;
        if (drawable instanceof BitmapDrawable) {
            result = ((BitmapDrawable) drawable).getBitmap();
        } else {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            if (width <= 0) {
                width = 1;
            }
            if (height <= 0) {
                height = 1;
            }

            result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        return result;
    }

    /**
     * Gets the image compress format depending on the file extension.
     * @param name Name of the image file including the extension.
     * @return Image compress format.
     */
    private static Bitmap.CompressFormat getCompressFormat(String name) {
        String[] s = name.split("\\.");

        if (s.length > 1) {
            String ext = s[s.length - 1];
            switch (ext) {
                case "jpeg":
                case "jpg":
                default:
                    return Bitmap.CompressFormat.JPEG;

                case "png":
                    return Bitmap.CompressFormat.PNG;

                case "webp":
                    return Bitmap.CompressFormat.WEBP;
            }
        }
        return Bitmap.CompressFormat.JPEG;
    }

    /**
     * Checks an image before upload to a chat and downscales it if needed.
     * @param file Original image file.
     * @return Image file to be uploaded.
     */
    public static File checkImageBeforeUpload(File file) {
        int orientation = AndroidGfxProcessor.getExifOrientation(file.getAbsolutePath());
        Rect fileRect = AndroidGfxProcessor.getImageDimensions(file.getAbsolutePath(), orientation);
        Bitmap fileBitmap = AndroidGfxProcessor.getBitmap(file.getAbsolutePath(), fileRect, orientation, fileRect.right, fileRect.bottom);
        if (fileBitmap == null) {
            logError("Bitmap NULL when decoding image file for upload it to chat.");
            return null;
        }

        File outFile = null;
        float width = fileBitmap.getWidth();
        float height = fileBitmap.getHeight();
        float totalPixels = width * height;
        float division = DOWNSCALE_IMAGES_PX / totalPixels;
        float factor = (float) Math.min(Math.sqrt(division), 1);

        if (factor < 1) {
            width *= factor;
            height *= factor;
            logDebug("DATA connection factor<1 totalPixels: " + totalPixels + " width: " + width + " height: " + height +
                    " DOWNSCALE_IMAGES_PX/totalPixels: " + division + " Math.sqrt(DOWNSCALE_IMAGES_PX/totalPixels): " + Math.sqrt(division));

            Bitmap scaleBitmap = Bitmap.createScaledBitmap(fileBitmap, (int) width, (int) height, true);
            if (scaleBitmap == null) {
                logError("Bitmap NULL when scaling image file for upload it to chat.");
                return null;
            }

            outFile = buildChatTempFile(MegaApplication.getInstance().getApplicationContext(), file.getName());
            if (outFile == null) {
                logError("File NULL when building it for upload a scaled image to chat.");
                return null;
            }

            FileOutputStream fOut;
            try {
                fOut = new FileOutputStream(outFile);
                scaleBitmap.compress(getCompressFormat(file.getName()), 100, fOut);
                fOut.flush();
                fOut.close();
            } catch (Exception e) {
                logError("Exception compressing image file for upload it to chat.", e);
            }

            scaleBitmap.recycle();
        }

        fileBitmap.recycle();

        return outFile;
    }

    /**
     * Method for obtaining the reaction with the largest width.
     * @param receivedChatId The chat ID.
     * @param receivedMessageId The msg ID.
     * @param listReactions The reactions list.
     * @return The size.
     */
    public static int getMaxWidthItem(long receivedChatId, long receivedMessageId, ArrayList<String> listReactions, DisplayMetrics outMetrics) {
        if (listReactions == null || listReactions.isEmpty()) {
            return 0;
        }

        int initSize = px2dp(MIN_WIDTH, outMetrics);
        for (String reaction : listReactions) {
            int numUsers = MegaApplication.getInstance().getMegaChatApi().getMessageReactionCount(receivedChatId, receivedMessageId, reaction);
            if (numUsers > 0) {
                String text = numUsers + "";
                Paint paint = new Paint();
                paint.setTypeface(Typeface.DEFAULT);
                paint.setTextSize(TEXT_SIZE);
                int newWidth = (int) paint.measureText(text);
                int sizeText = isScreenInPortrait(MegaApplication.getInstance().getBaseContext()) ? newWidth + 1 : newWidth + 4;
                int possibleNewSize = px2dp(MIN_WIDTH, outMetrics) + px2dp(sizeText, outMetrics);
                if (possibleNewSize > initSize) {
                    initSize = possibleNewSize;
                }
            }
        }
        return initSize;
    }

    /**
     * Method that transforms an emoji into the right format to add a reaction.
     *
     * @param context        Context of Activity.
     * @param chatId         The chat ID.
     * @param messageId      The msg ID.
     * @param emoji          The chosen emoji.
     * @param isFromKeyboard If it's from the keyboard.
     */
    public static void addReactionInMsg(Context context, long chatId, long messageId, Emoji emoji, boolean isFromKeyboard) {
        if (!(context instanceof ChatActivityLollipop))
            return;

        EmojiEditText editText = new EmojiEditText(context);
        editText.input(emoji);
        String reaction = editText.getText().toString();
        addReactionInMsg(context, chatId, messageId, reaction, isFromKeyboard);
    }

    /**
     * Method for adding a reaction in a msg.
     *
     * @param context        Context of Activity.
     * @param chatId         The chat ID.
     * @param messageId      The msg ID.
     * @param reaction       The String with the reaction.
     * @param isFromKeyboard If it's from the keyboard.
     */
    public static void addReactionInMsg(Context context, long chatId, long messageId, String reaction, boolean isFromKeyboard) {
        if (!(context instanceof ChatActivityLollipop))
            return;

        MegaApplication.setIsReactionFromKeyboard(isFromKeyboard);
        MegaApplication.getInstance().getMegaChatApi().addReaction(chatId, messageId, reaction, new ManageReactionListener(context));
    }

    public static boolean shouldReactionBeClicked(MegaChatRoom chatRoom) {
        return !chatRoom.isPreview() &&
                (chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_STANDARD ||
                        chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_MODERATOR);
    }

    /**
     * Method of obtaining the reaction list
     *
     * @param listReactions   The string list.
     * @param invalidReaction The invalid reaction
     * @return The reaction list.
     */
    public static ArrayList<String> getReactionsList(MegaStringList listReactions, boolean invalidReaction) {
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < listReactions.size(); i++) {
            list.add(i, listReactions.get(i));
        }

        if (invalidReaction) {
            list.add(INVALID_REACTION);
        }

        return list;
    }

    /**
     * Sets the contact status icon
     *
     * @param userStatus         contact's status
     * @param contactStateIcon  view in which the status icon has to be set
     */
    public static void setContactStatus(int userStatus, ImageView contactStateIcon) {
        if (contactStateIcon == null) {
            return;
        }

        contactStateIcon.setVisibility(View.VISIBLE);

        switch (userStatus) {
            case MegaChatApi.STATUS_ONLINE:
                contactStateIcon.setImageDrawable(ContextCompat.getDrawable(MegaApplication.getInstance(), R.drawable.circle_status_contact_online));
                break;

            case MegaChatApi.STATUS_AWAY:
                contactStateIcon.setImageDrawable(ContextCompat.getDrawable(MegaApplication.getInstance(), R.drawable.circle_status_contact_away));
                break;

            case MegaChatApi.STATUS_BUSY:
                contactStateIcon.setImageDrawable(ContextCompat.getDrawable(MegaApplication.getInstance(), R.drawable.circle_status_contact_busy));
                break;

            case MegaChatApi.STATUS_OFFLINE:
                contactStateIcon.setImageDrawable(ContextCompat.getDrawable(MegaApplication.getInstance(), R.drawable.circle_status_contact_offline));
                break;

            case MegaChatApi.STATUS_INVALID:
            default:
                contactStateIcon.setVisibility(View.GONE);
        }
    }

    /**
     * Sets the contact status icon and status text
     *
     * @param userStatus         contact's status
     * @param contactStateIcon  view in which the status icon has to be set
     * @param contactStateText  view in which the status text has to be set
     */
    public static void setContactStatus(int userStatus, ImageView contactStateIcon, TextView contactStateText) {
        MegaApplication app = MegaApplication.getInstance();
        setContactStatus(userStatus, contactStateIcon);

        if (contactStateText == null) {
            return;
        }

        contactStateText.setVisibility(View.VISIBLE);

        switch (userStatus) {
            case MegaChatApi.STATUS_ONLINE:
                contactStateText.setText(app.getString(R.string.online_status));
                break;

            case MegaChatApi.STATUS_AWAY:
                contactStateText.setText(app.getString(R.string.away_status));
                break;

            case MegaChatApi.STATUS_BUSY:
                contactStateText.setText(app.getString(R.string.busy_status));
                break;

            case MegaChatApi.STATUS_OFFLINE:
                contactStateText.setText(app.getString(R.string.offline_status));
                break;

            case MegaChatApi.STATUS_INVALID:
            default:
                contactStateText.setVisibility(View.GONE);
        }
    }

    /**
     * If the contact has last green, sets is as status text
     *
     * @param context           current Context
     * @param userStatus        contact's status
     * @param lastGreen         contact's last green
     * @param contactStateText  view in which the last green has to be set
     */
    public static void setContactLastGreen(Context context, int userStatus, String lastGreen, MarqueeTextView contactStateText) {
        if (contactStateText == null || isTextEmpty(lastGreen)) {
            return;
        }

        if (userStatus != MegaChatApi.STATUS_ONLINE && userStatus != MegaChatApi.STATUS_BUSY && userStatus != MegaChatApi.STATUS_INVALID) {
            contactStateText.setText(lastGreen);
            contactStateText.isMarqueeIsNecessary(context);
        }
    }

    /**
     * Gets the name of an attached contact.
     *
     * @param message   chat message
     * @return The contact's name by this order if available: nickname, name or email
     */
    public static String getNameContactAttachment(MegaChatMessage message) {
        String email = message.getUserEmail(0);
        String name = getMegaUserNameDB(MegaApplication.getInstance().getMegaApi().getContact(email));
        if (isTextEmpty(name)) {
            name = message.getUserName(0);
        }

        if (isTextEmpty(name)) {
            name = email;
        }

        return name;
    }

    /*
     * Method for obtaining the AudioFocusRequest when get the focus audio.
     *
     * @param listener  The listener.
     * @param focusType Type of focus.
     * @return The AudioFocusRequest.
     */
    public static AudioFocusRequest getRequest(AudioFocusListener listener, int focusType) {
        if (SHOULD_BUILD_FOCUS_REQUEST) {
            AudioAttributes mAudioAttributes =
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build();
            return new AudioFocusRequest.Builder(focusType)
                    .setAudioAttributes(mAudioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener(listener)
                    .build();
        }

        return null;
    }

    /**
     * Knowing if permits have been successfully got.
     *
     * @return True, if it has been successful. False, if not.
     */
    public static boolean getAudioFocus(AudioManager mAudioManager, AudioFocusListener listener, AudioFocusRequest request, int focusType, int streamType) {
        if (mAudioManager == null) {
            logWarning("Audio Manager is NULL");
            return false;
        }

        int focusRequest;
        if (SHOULD_BUILD_FOCUS_REQUEST) {
            if (request == null) {
                logWarning("Audio Focus Request is NULL");
                return false;
            }
            focusRequest = mAudioManager.requestAudioFocus(request);
        } else {
            focusRequest = mAudioManager.requestAudioFocus(listener, streamType, focusType);
        }

        return focusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    /**
     * Method for leaving the audio focus.
     */
    public static void abandonAudioFocus(AudioFocusListener listener, AudioManager mAudioManager, AudioFocusRequest request) {
        if (SHOULD_BUILD_FOCUS_REQUEST) {
            if(request != null) {
                mAudioManager.abandonAudioFocusRequest(request);
            }
        } else {
            mAudioManager.abandonAudioFocus(listener);
        }
    }

    /**
     * Method for obtaining the title of a MegaChatRoom.
     *
     * @param chat The chat room.
     * @return String with the title.
     */
    public static String getTitleChat(MegaChatRoom chat) {
        if (chat.isActive()) {
            return chat.getTitle();
        }

        MegaApplication app = MegaApplication.getInstance();
        return app.getString(R.string.inactive_chat_title, formatDate(app.getBaseContext(), chat.getCreationTs(), DATE_AND_TIME_YYYY_MM_DD_HH_MM_FORMAT));
    }

    /**
     * Method for obtaining the title of a MegaChatListItem.
     *
     * @param chat The chat room.
     * @return String with the title.
     */
    public static String getTitleChat(MegaChatListItem chat) {
        if (chat.isActive()) {
            return chat.getTitle();
        }

        return getTitleChat(MegaApplication.getInstance().getMegaChatApi().getChatRoom(chat.getChatId()));
    }

    /**
     * Method to know if the chat notifications are activated or deactivated.
     *
     * @return The type of mute.
     */
    public static String getGeneralNotification() {
        MegaApplication app = MegaApplication.getInstance();
        MegaPushNotificationSettings pushNotificationSettings = MegaApplication.getPushNotificationSettingManagement().getPushNotificationSetting();
        if (pushNotificationSettings != null) {
            if (!pushNotificationSettings.isGlobalChatsDndEnabled() || pushNotificationSettings.getGlobalChatsDnd() == -1) {
                ChatSettings chatSettings = app.getDbH().getChatSettings();
                if (chatSettings == null) {
                    chatSettings = new ChatSettings();
                    app.getDbH().setChatSettings(chatSettings);
                }

                return NOTIFICATIONS_ENABLED;
            }

            if (pushNotificationSettings.getGlobalChatsDnd() == 0) {
                return NOTIFICATIONS_DISABLED;
            }

            return NOTIFICATIONS_DISABLED_X_TIME;
        }

        return NOTIFICATIONS_ENABLED;
    }

    /**
     * Method to display a dialog to mute a specific chat.
     * @param context Context of Activity.
     * @param chatId Chat ID.
     */
    public static void createMuteNotificationsAlertDialogOfAChat(Activity context, long chatId) {
        ArrayList<MegaChatListItem> chats = new ArrayList<>();
        MegaChatListItem chat = MegaApplication.getInstance().getMegaChatApi().getChatListItem(chatId);
        if (chat != null) {
            chats.add(chat);
            createMuteNotificationsChatAlertDialog(context, chats);
        }
    }

    /**
     * Method to display a dialog to mute general chat notifications or several specific chats.
     *
     * @param context Context of Activity.
     * @param chats  Chats. If the chats is null, it's for the general chats notifications.
     */
    public static void createMuteNotificationsChatAlertDialog(Activity context, ArrayList<MegaChatListItem> chats) {

        final AlertDialog muteDialog;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
        if (chats == null) {
            View view = context.getLayoutInflater().inflate(R.layout.title_mute_notifications, null);
            dialogBuilder.setCustomTitle(view);
        } else {
            dialogBuilder.setTitle(context.getString(R.string.title_dialog_mute_chatroom_notifications));
        }

        boolean isUntilThisMorning = isUntilThisMorning();
        String optionUntil = chats != null ?
                context.getString(R.string.mute_chatroom_notification_option_forever) :
                (isUntilThisMorning ? context.getString(R.string.mute_chatroom_notification_option_until_this_morning) :
                        context.getString(R.string.mute_chatroom_notification_option_until_tomorrow_morning));

        String optionSelected = chats != null ?
                NOTIFICATIONS_DISABLED :
                (isUntilThisMorning ? NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING :
                        NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING);

        AtomicReference<Integer> itemClicked = new AtomicReference<>();

        ArrayList<String> stringsArray = new ArrayList<>();
        stringsArray.add(0, removeFormatPlaceholder(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_minutes, 30, 30)));
        stringsArray.add(1, removeFormatPlaceholder(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, 1, 1)));
        stringsArray.add(2, removeFormatPlaceholder(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, 6, 6)));
        stringsArray.add(3, removeFormatPlaceholder(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, 24, 24)));
        stringsArray.add(4, optionUntil);

        ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(context, R.layout.checked_text_view_dialog_button, stringsArray);
        ListView listView = new ListView(context);
        listView.setAdapter(itemsAdapter);

        dialogBuilder.setSingleChoiceItems(itemsAdapter, INVALID_POSITION, (dialog, item) -> {
            itemClicked.set(item);
            ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        });

        dialogBuilder.setPositiveButton(context.getString(R.string.general_ok),
                (dialog, which) -> {
                    MegaApplication.getPushNotificationSettingManagement().controlMuteNotifications(context, getTypeMute(itemClicked.get(), optionSelected), chats);
                    dialog.dismiss();
                });
        dialogBuilder.setNegativeButton(context.getString(R.string.general_cancel), (dialog, which) -> dialog.dismiss());

        muteDialog = dialogBuilder.create();
        muteDialog.show();
        muteDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }

    /**
     * Method for getting the string depending on the selected mute option.
     *
     * @param option The selected mute option.
     * @return The appropriate string.
     */
    public static String getMutedPeriodString(String option) {
        Context context = MegaApplication.getInstance().getBaseContext();
        switch (option) {
            case NOTIFICATIONS_30_MINUTES:
                return removeFormatPlaceholder(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_minutes, 30, 30));
            case NOTIFICATIONS_1_HOUR:
                return removeFormatPlaceholder(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, 1, 1));
            case NOTIFICATIONS_6_HOURS:
                return removeFormatPlaceholder(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, 6, 6));
            case NOTIFICATIONS_24_HOURS:
                return removeFormatPlaceholder(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, 24, 24));
        }

        return null;
    }

    /**
     * Method for getting the selected mute option depending on the selected item.
     *
     * @param itemClicked   The selected item.
     * @param optionSelected The right choice when you select the fifth option.
     * @return The right mute option.
     */
    private static String getTypeMute(int itemClicked, String optionSelected) {
        switch (itemClicked) {
            case 0:
                return NOTIFICATIONS_30_MINUTES;
            case 1:
                return NOTIFICATIONS_1_HOUR;
            case 2:
                return NOTIFICATIONS_6_HOURS;
            case 3:
                return NOTIFICATIONS_24_HOURS;
            case 4:
                return optionSelected;
            default:
                return NOTIFICATIONS_ENABLED;
        }
    }

    /**
     * Method to mute a specific chat or general notifications chat for a specific period of time.
     * @param context Context of Activity.
     * @param muteOption The selected mute option.
     */
    public static void muteChat(Context context, String muteOption) {
        new ChatController(context).muteChat(muteOption);
    }

    /**
     * Method to know if the general chat notifications are activated or muted.
     *
     * @return True, if notifications are activated. False in the opposite case
     */
    public static boolean isEnableGeneralChatNotifications(){
        MegaPushNotificationSettings megaPushNotificationSettings = MegaApplication.getPushNotificationSettingManagement().getPushNotificationSetting();
        return megaPushNotificationSettings == null || !megaPushNotificationSettings.isGlobalChatsDndEnabled();

    }

    /**
     * Method to know if the notifications of a specific chat are activated or muted.
     *
     * @param chatId Chat id.
     * @return True, if notifications are activated. False in the opposite case
     */
    public static boolean isEnableChatNotifications(long chatId) {
        MegaPushNotificationSettings megaPushNotificationSettings = MegaApplication.getPushNotificationSettingManagement().getPushNotificationSetting();
        return megaPushNotificationSettings == null || !megaPushNotificationSettings.isChatDndEnabled(chatId);
    }

    /**
     * Method to checking when chat notifications are enabled and update the UI elements.
     *
     * @param chatHandle            Chat ID.
     * @param notificationsSwitch   The SwitchCompat.
     * @param notificationsSubTitle The TextView with the info.
     */
    public static void checkSpecificChatNotifications(long chatHandle, final SwitchCompat notificationsSwitch, final TextView notificationsSubTitle) {
        if (MegaApplication.getPushNotificationSettingManagement().getPushNotificationSetting() != null) {
            updateSwitchButton(chatHandle, notificationsSwitch, notificationsSubTitle);
        }
    }

    /**
     * Method to update the switch element related to the notifications of a specific chat.
     *
     * @param chatId                The chat ID.
     * @param notificationsSwitch   The SwitchCompat.
     * @param notificationsSubTitle The TextView with the info.
     */
    public static void updateSwitchButton(long chatId, final SwitchCompat notificationsSwitch, final TextView notificationsSubTitle) {
        MegaPushNotificationSettings push = MegaApplication.getPushNotificationSettingManagement().getPushNotificationSetting();
        if (push == null)
            return;

        if (push.isChatDndEnabled(chatId)) {
            notificationsSwitch.setChecked(false);
            long timestampMute = push.getChatDnd(chatId);
            notificationsSubTitle.setVisibility(View.VISIBLE);
            notificationsSubTitle.setText(timestampMute == 0 ?
                    MegaApplication.getInstance().getString(R.string.mute_chatroom_notification_option_off) :
                    getCorrectStringDependingOnOptionSelected(timestampMute));
        } else {
            notificationsSwitch.setChecked(true);
            notificationsSubTitle.setVisibility(View.GONE);
        }
    }

    /**
     * Gets the user's online status.
     *
     * @param userHandle    handle of the user
     * @return The user's status.
     */
    public static int getUserStatus(long userHandle) {
        return isContact(userHandle)
                ? MegaApplication.getInstance().getMegaChatApi().getUserOnlineStatus(userHandle)
                : MegaChatApi.STATUS_INVALID;
    }

    /**
     * Method for obtaining the contact status bitmap.
     *
     * @param userStatus The contact status.
     * @return The final bitmap.
     */
    public static Bitmap getStatusBitmap(int userStatus) {
        switch (userStatus) {
            case MegaChatApi.STATUS_ONLINE:
                return BitmapFactory.decodeResource(MegaApplication.getInstance().getBaseContext().getResources(), R.drawable.ic_online);

            case MegaChatApi.STATUS_AWAY:
                return BitmapFactory.decodeResource(MegaApplication.getInstance().getBaseContext().getResources(), R.drawable.ic_away);

            case MegaChatApi.STATUS_BUSY:
                return BitmapFactory.decodeResource(MegaApplication.getInstance().getBaseContext().getResources(), R.drawable.ic_busy);

            case MegaChatApi.STATUS_OFFLINE:
                return BitmapFactory.decodeResource(MegaApplication.getInstance().getBaseContext().getResources(), R.drawable.ic_offline);

            case MegaChatApi.STATUS_INVALID:
            default:
                return null;
        }
    }
}
