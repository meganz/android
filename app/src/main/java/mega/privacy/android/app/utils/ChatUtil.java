package mega.privacy.android.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MarqueeTextView;
import mega.privacy.android.app.components.SimpleSpanBuilder;
import mega.privacy.android.app.components.twemoji.EmojiEditText;
import mega.privacy.android.app.components.twemoji.EmojiManager;
import mega.privacy.android.app.components.twemoji.EmojiRange;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.components.twemoji.EmojiUtilsShortcodes;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.listeners.AudioFocusListener;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentHistoryActivity;
import nz.mega.sdk.AndroidGfxProcessor;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaStringList;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.TimeUtils.*;

public class ChatUtil {

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

    public static SimpleSpanBuilder formatText(Context context, String text) {

        SimpleSpanBuilder result;

        try {
            RTFFormatter formatter = new RTFFormatter(text, context);
            result = formatter.setRTFFormat();
        } catch (Exception e) {
            logError("FORMATTER EXCEPTION!!!", e);
            result = null;
        }
        return result;
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
     * Method for adding a reaction in a message.
     *
     * @param context        Context of Activity.
     * @param chatId         The chat ID.
     * @param messageId        The msg ID.
     * @param emoji          The chosen reaction.
     * @param isFromKeyboard If it's from the keyboard.
     */
    public static void addReactionInMsg(Context context, long chatId, long messageId, Emoji emoji, boolean isFromKeyboard) {
        if(!(context instanceof ChatActivityLollipop))
            return;

        MegaApplication.setIsReactionFromKeyboard(isFromKeyboard);
        EmojiEditText editText = new EmojiEditText(context);
        editText.input(emoji);
        String reaction = editText.getText().toString();
        MegaApplication.getInstance().getMegaChatApi().addReaction(chatId, messageId, reaction, (ChatActivityLollipop) context);
    }

    /**
     * Method for removing a reaction from a message.
     *
     * @param chatId   The chat ID.
     * @param message  The msg ID.
     * @param reaction The chosen reaction.
     */
    public static void delReactionInMsg(Context context, long chatId, MegaChatMessage message, String reaction) {
        MegaApplication.getInstance().getMegaChatApi().delReaction(chatId, message.getMsgId(), reaction, (ChatActivityLollipop) context);
    }

    /**
     * Method for knowing if the option to add reactions should be visible.
     *
     * @param context  Context of Activity.
     * @param chatRoom The chat.
     * @param message  The message.
     * @return True, if it should be visible. False, the opposite.
     */
    public static boolean shouldReactionOptionsBeVisible(Context context, MegaChatRoom chatRoom, AndroidMegaChatMessage message) {
        return chatRoom != null && message != null &&
                context instanceof ChatActivityLollipop &&
                !((ChatActivityLollipop) context).hasMessagesRemoved(message.getMessage()) &&
                !message.isUploading() &&
                ((chatRoom.getOwnPrivilege() != MegaChatRoom.PRIV_RM && chatRoom.getOwnPrivilege() != MegaChatRoom.PRIV_RO)
                        || chatRoom.isPreview());
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
        int focusRequest;
        if (SHOULD_BUILD_FOCUS_REQUEST) {
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
}
