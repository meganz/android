package mega.privacy.android.app.utils;

import static mega.privacy.android.app.constants.SettingsConstants.VIDEO_QUALITY_ORIGINAL;
import static mega.privacy.android.app.utils.CacheFolderManager.buildChatTempFile;
import static mega.privacy.android.app.utils.CallUtil.isStatusConnected;
import static mega.privacy.android.app.utils.Constants.APP_DATA_INDICATOR;
import static mega.privacy.android.app.utils.Constants.CHAT_ID;
import static mega.privacy.android.app.utils.Constants.COPIED_TEXT_LABEL;
import static mega.privacy.android.app.utils.Constants.DISABLED_RETENTION_TIME;
import static mega.privacy.android.app.utils.Constants.FROM_CHAT;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE;
import static mega.privacy.android.app.utils.Constants.INVALID_POSITION;
import static mega.privacy.android.app.utils.Constants.INVALID_REACTION;
import static mega.privacy.android.app.utils.Constants.MAX_ALLOWED_CHARACTERS_AND_EMOJIS;
import static mega.privacy.android.app.utils.Constants.MESSAGE_ID;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_1_HOUR;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_24_HOURS;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_30_MINUTES;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_6_HOURS;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_DISABLED;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_DISABLED_X_TIME;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_ENABLED;
import static mega.privacy.android.app.utils.Constants.PREFERENCE_EMOJI;
import static mega.privacy.android.app.utils.Constants.PREFERENCE_REACTION;
import static mega.privacy.android.app.utils.Constants.PREFERENCE_VARIANT_EMOJI;
import static mega.privacy.android.app.utils.Constants.PREFERENCE_VARIANT_REACTION;
import static mega.privacy.android.app.utils.Constants.SECONDS_IN_DAY;
import static mega.privacy.android.app.utils.Constants.SECONDS_IN_HOUR;
import static mega.privacy.android.app.utils.Constants.SECONDS_IN_MONTH_30;
import static mega.privacy.android.app.utils.Constants.SECONDS_IN_WEEK;
import static mega.privacy.android.app.utils.Constants.SECONDS_IN_YEAR;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.Constants.TYPE_TEXT_PLAIN;
import static mega.privacy.android.app.utils.ContactUtil.getMegaUserNameDB;
import static mega.privacy.android.app.utils.ContactUtil.isContact;
import static mega.privacy.android.app.utils.FileUtil.getLocalFile;
import static mega.privacy.android.app.utils.FileUtil.shareFile;
import static mega.privacy.android.app.utils.MegaNodeUtil.startShareIntent;
import static mega.privacy.android.app.utils.StringResourcesUtils.getString;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.TextUtil.removeFormatPlaceholder;
import static mega.privacy.android.app.utils.TimeUtils.DATE_AND_TIME_YYYY_MM_DD_HH_MM_FORMAT;
import static mega.privacy.android.app.utils.TimeUtils.formatDate;
import static mega.privacy.android.app.utils.TimeUtils.getCorrectStringDependingOnOptionSelected;
import static mega.privacy.android.app.utils.TimeUtils.isTodayOrYesterday;
import static mega.privacy.android.app.utils.TimeUtils.isUntilThisMorning;
import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.ManageChatHistoryActivity;
import mega.privacy.android.app.components.MarqueeTextView;
import mega.privacy.android.app.components.twemoji.EmojiEditText;
import mega.privacy.android.app.components.twemoji.EmojiManager;
import mega.privacy.android.app.components.twemoji.EmojiRange;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.components.twemoji.EmojiUtilsShortcodes;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;
import mega.privacy.android.app.interfaces.ChatManagementCallback;
import mega.privacy.android.app.listeners.ExportListener;
import mega.privacy.android.app.listeners.SetRetentionTimeListener;
import mega.privacy.android.app.main.controllers.ChatController;
import mega.privacy.android.app.main.listeners.ManageReactionListener;
import mega.privacy.android.app.main.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.main.megachat.ChatActivity;
import mega.privacy.android.app.main.megachat.ChatSettings;
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity;
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity;
import mega.privacy.android.app.main.megachat.PendingMessageSingle;
import mega.privacy.android.app.main.megachat.RemovedMessage;
import mega.privacy.android.app.textEditor.TextEditorActivity;
import nz.mega.sdk.AndroidGfxProcessor;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaPushNotificationSettings;
import nz.mega.sdk.MegaStringList;
import timber.log.Timber;

public class ChatUtil {
    private static final int MIN_WIDTH = 44;
    private static final float TEXT_SIZE = 14f;
    private static final float DOWNSCALE_IMAGES_PX = 2000000f;
    private static final boolean SHOULD_BUILD_FOCUS_REQUEST = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    public static final int AUDIOFOCUS_DEFAULT = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE;
    public static final int STREAM_MUSIC_DEFAULT = AudioManager.STREAM_MUSIC;

    private static final int RETENTION_TIME_DIALOG_OPTION_DISABLED = 0;
    private static final int RETENTION_TIME_DIALOG_OPTION_DAY = 1;
    private static final int RETENTION_TIME_DIALOG_OPTION_WEEK = 2;
    private static final int RETENTION_TIME_DIALOG_OPTION_MONTH = 3;
    private static final int RETENTION_TIME_DIALOG_OPTION_CUSTOM = 4;

    /**
     * Where is the status icon placed, according to the design,
     * according to the design,
     * on dark mode the status icon image is different based on the place where it's placed.
     */
    public enum StatusIconLocation {

        /**
         * On chat list
         * Contact list
         * Contact info
         * Flat app bar no chat room
         */
        STANDARD,

        /**
         * Raised app bar on chat room
         */
        APPBAR,

        /**
         * On nav drawer
         * Bottom sheets
         */
        DRAWER
    }

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

    private static int getRealLength(CharSequence text) {
        int length = text.length();

        List<EmojiRange> emojisFound = EmojiManager.getInstance().findAllEmojis(text);
        int count = 0;
        if (emojisFound.size() > 0) {
            for (int i = 0; i < emojisFound.size(); i++) {
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

    public static void showShareChatLinkDialog(final Context context, MegaChatRoom chat, final String chatLink) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
        LayoutInflater inflater = null;

        if (context instanceof GroupChatInfoActivity) {
            inflater = ((GroupChatInfoActivity) context).getLayoutInflater();
        } else if (context instanceof ChatActivity) {
            inflater = ((ChatActivity) context).getLayoutInflater();
        }

        View v = inflater.inflate(R.layout.chat_link_share_dialog, null);
        builder.setView(v);
        final AlertDialog shareLinkDialog = builder.create();

        EmojiTextView nameGroup = v.findViewById(R.id.group_name_text);
        nameGroup.setText(getTitleChat(chat));
        TextView chatLinkText = v.findViewById(R.id.chat_link_text);
        chatLinkText.setText(chatLink);

        Button copyButton = v.findViewById(R.id.copy_button);
        copyButton.setOnClickListener(v12 -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(COPIED_TEXT_LABEL, chatLink);
            clipboard.setPrimaryClip(clip);
            if (context instanceof ChatActivity) {
                ((ChatActivity) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.chat_link_copied_clipboard), MEGACHAT_INVALID_HANDLE);

            }
            dismissShareChatLinkDialog(context, shareLinkDialog);
        });

        Button shareButton = v.findViewById(R.id.share_button);
        shareButton.setOnClickListener(v13 -> {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType(TYPE_TEXT_PLAIN);
            sharingIntent.putExtra(Intent.EXTRA_TEXT, chatLink);
            context.startActivity(Intent.createChooser(sharingIntent, context.getString(R.string.context_share)));
            dismissShareChatLinkDialog(context, shareLinkDialog);
        });

        Button dismissButton = v.findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(v15 -> dismissShareChatLinkDialog(context, shareLinkDialog));

        shareLinkDialog.setCancelable(false);
        shareLinkDialog.setCanceledOnTouchOutside(false);

        try {
            shareLinkDialog.show();
        } catch (Exception e) {
            Timber.w(e, "Exception showing share link dialog.");
        }
    }

    private static void dismissShareChatLinkDialog(Context context, AlertDialog shareLinkDialog) {
        try {
            shareLinkDialog.dismiss();
            if (context instanceof ChatActivity) {
                ((ChatActivity) context).setShareLinkDialogDismissed(true);
            }
        } catch (Exception e) {
        }
    }

    public static void showConfirmationRemoveChatLink(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.action_delete_link)
                .setMessage(R.string.context_remove_chat_link_warning_text)
                .setPositiveButton(R.string.delete_button, (dialog, which) -> {
                    if (context instanceof GroupChatInfoActivity) {
                        ((GroupChatInfoActivity) context).removeChatLink();
                    }
                })
                .setNegativeButton(R.string.general_cancel, null).show();
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
        if (drawableA == null || drawableB == null)
            return false;

        Drawable.ConstantState stateA = drawableA.getConstantState();
        Drawable.ConstantState stateB = drawableB.getConstantState();
        return (stateA != null && stateA.equals(stateB)) || getBitmap(drawableA).sameAs(getBitmap(drawableB));
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
     *
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
     *
     * @param file Original image file.
     * @return Image file to be uploaded.
     */
    public static File checkImageBeforeUpload(File file) {
        int orientation = AndroidGfxProcessor.getExifOrientation(file.getAbsolutePath());
        Rect fileRect = AndroidGfxProcessor.getImageDimensions(file.getAbsolutePath(), orientation);
        Bitmap fileBitmap = AndroidGfxProcessor.getBitmap(file.getAbsolutePath(), fileRect, orientation, fileRect.right, fileRect.bottom);
        if (fileBitmap == null) {
            Timber.e("Bitmap NULL when decoding image file for upload it to chat.");
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
            Timber.d("DATA connection factor<1 totalPixels: " + totalPixels + " width: " + width + " height: " + height +
                    " DOWNSCALE_IMAGES_PX/totalPixels: " + division + " Math.sqrt(DOWNSCALE_IMAGES_PX/totalPixels): " + Math.sqrt(division));

            Bitmap scaleBitmap = Bitmap.createScaledBitmap(fileBitmap, (int) width, (int) height, true);
            if (scaleBitmap == null) {
                Timber.e("Bitmap NULL when scaling image file for upload it to chat.");
                return null;
            }

            outFile = buildChatTempFile(MegaApplication.getInstance().getApplicationContext(), file.getName());
            if (outFile == null) {
                Timber.e("File NULL when building it for upload a scaled image to chat.");
                return null;
            }

            FileOutputStream fOut;
            try {
                fOut = new FileOutputStream(outFile);
                scaleBitmap.compress(getCompressFormat(file.getName()), 100, fOut);
                fOut.flush();
                fOut.close();
            } catch (Exception e) {
                Timber.e(e, "Exception compressing image file for upload it to chat.");
            }

            scaleBitmap.recycle();
        }

        fileBitmap.recycle();

        return outFile;
    }

    /**
     * Method for obtaining the reaction with the largest width.
     *
     * @param receivedChatId    The chat ID.
     * @param receivedMessageId The msg ID.
     * @param listReactions     The reactions list.
     * @return The size.
     */
    public static int getMaxWidthItem(long receivedChatId, long receivedMessageId, ArrayList<String> listReactions, DisplayMetrics outMetrics) {
        if (listReactions == null || listReactions.isEmpty()) {
            return 0;
        }

        int initSize = dp2px(MIN_WIDTH, outMetrics);
        for (String reaction : listReactions) {
            int numUsers = MegaApplication.getInstance().getMegaChatApi().getMessageReactionCount(receivedChatId, receivedMessageId, reaction);
            if (numUsers > 0) {
                String text = numUsers + "";
                Paint paint = new Paint();
                paint.setTypeface(Typeface.DEFAULT);
                paint.setTextSize(TEXT_SIZE);
                int newWidth = (int) paint.measureText(text);
                int sizeText = isScreenInPortrait(MegaApplication.getInstance().getBaseContext()) ? newWidth + 1 : newWidth + 4;
                int possibleNewSize = dp2px(MIN_WIDTH, outMetrics) + dp2px(sizeText, outMetrics);
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
        if (!(context instanceof ChatActivity)) {
            Timber.w("Incorrect context");
            return;
        }

        EmojiEditText editText = new EmojiEditText(context);
        editText.input(emoji);
        if (editText.getText() == null) {
            Timber.e("Text null");
            return;
        }

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
        if (!(context instanceof ChatActivity)) {
            Timber.w("Incorrect context");
            return;
        }

        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        if (isMyOwnReaction(chatId, messageId, reaction) && !isFromKeyboard) {
            Timber.d("Removing reaction...");
            megaChatApi.delReaction(chatId, messageId, reaction, new ManageReactionListener(context));
        } else {
            Timber.d("Adding reaction...");
            megaChatApi.addReaction(chatId, messageId, reaction, new ManageReactionListener(context));
        }
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
     * Method for know if I have a concrete reaction to a particular message
     *
     * @param chatId   The chat ID.
     * @param msgId    The message ID.
     * @param reaction The reaction.
     * @return True, if I have reacted. False otherwise.
     */
    public static boolean isMyOwnReaction(long chatId, long msgId, String reaction) {
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        MegaHandleList handleList = megaChatApi.getReactionUsers(chatId, msgId, reaction);

        for (int i = 0; i < handleList.size(); i++) {
            if (handleList.get(i) == megaChatApi.getMyUserHandle()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Sets the contact status icon
     *
     * @param userStatus       contact's status
     * @param contactStateIcon view in which the status icon has to be set
     * @param where            Where the icon is placed.
     */
    public static void setContactStatus(int userStatus, ImageView contactStateIcon, StatusIconLocation where) {
        if (contactStateIcon == null) {
            return;
        }

        Context context = contactStateIcon.getContext();
        contactStateIcon.setVisibility(View.VISIBLE);

        int statusImageResId = getIconResourceIdByLocation(context, userStatus, where);

        // Hide the icon ImageView.
        if (statusImageResId == 0) {
            contactStateIcon.setVisibility(View.GONE);
        } else {
            contactStateIcon.setImageResource(statusImageResId);
        }
    }

    /**
     * Sets the contact status icon and status text
     *
     * @param userStatus          contact's status
     * @param textViewContactIcon view in which the status icon has to be set
     * @param contactStateText    view in which the status text has to be set
     * @param where               The status icon image resource is different based on the place where it's placed.
     */
    public static void setContactStatusParticipantList(int userStatus, final ImageView textViewContactIcon, TextView contactStateText, StatusIconLocation where) {
        MegaApplication app = MegaApplication.getInstance();
        Context context = app.getApplicationContext();
        int statusImageResId = getIconResourceIdByLocation(context, userStatus, where);

        if (statusImageResId == 0) {
            textViewContactIcon.setVisibility(View.GONE);
        } else {
            Drawable drawable = ContextCompat.getDrawable(MegaApplication.getInstance().getApplicationContext(), statusImageResId);
            textViewContactIcon.setImageDrawable(drawable);
            textViewContactIcon.setVisibility(View.VISIBLE);
        }

        if (contactStateText == null) {
            return;
        }

        contactStateText.setVisibility(View.VISIBLE);

        switch (userStatus) {
            case MegaChatApi.STATUS_ONLINE:
                contactStateText.setText(context.getString(R.string.online_status));
                break;

            case MegaChatApi.STATUS_AWAY:
                contactStateText.setText(context.getString(R.string.away_status));
                break;

            case MegaChatApi.STATUS_BUSY:
                contactStateText.setText(context.getString(R.string.busy_status));
                break;

            case MegaChatApi.STATUS_OFFLINE:
                contactStateText.setText(context.getString(R.string.offline_status));
                break;

            case MegaChatApi.STATUS_INVALID:
            default:
                contactStateText.setVisibility(View.GONE);
        }
    }

    /**
     * Get status icon image resource id by display mode and where the icon is placed.
     *
     * @param context    Context object.
     * @param userStatus User online status.
     * @param where      Where the icon is placed.
     * @return Image resource id based on where the icon is placed.
     * NOTE: when the user has an invalid online status, returns 0.
     * Caller should verify the return value, 0 is an invalid value for resource id.
     */
    public static int getIconResourceIdByLocation(Context context, int userStatus, StatusIconLocation where) {
        int statusImageResId = 0;

        switch (userStatus) {
            case MegaChatApi.STATUS_ONLINE:
                if (Util.isDarkMode(context)) {
                    switch (where) {
                        case STANDARD:
                            statusImageResId = R.drawable.ic_online_dark_standard;
                            break;

                        case DRAWER:
                            statusImageResId = R.drawable.ic_online_dark_drawer;
                            break;

                        case APPBAR:
                            statusImageResId = R.drawable.ic_online_dark_appbar;
                            break;
                    }
                } else {
                    statusImageResId = R.drawable.ic_online_light;
                }
                break;

            case MegaChatApi.STATUS_AWAY:
                if (Util.isDarkMode(context)) {
                    switch (where) {
                        case STANDARD:
                            statusImageResId = R.drawable.ic_away_dark_standard;
                            break;

                        case DRAWER:
                            statusImageResId = R.drawable.ic_away_dark_drawer;
                            break;

                        case APPBAR:
                            statusImageResId = R.drawable.ic_away_dark_appbar;
                            break;
                    }
                } else {
                    statusImageResId = R.drawable.ic_away_light;
                }
                break;

            case MegaChatApi.STATUS_BUSY:
                if (Util.isDarkMode(context)) {
                    switch (where) {
                        case STANDARD:
                            statusImageResId = R.drawable.ic_busy_dark_standard;
                            break;

                        case DRAWER:
                            statusImageResId = R.drawable.ic_busy_dark_drawer;
                            break;

                        case APPBAR:
                            statusImageResId = R.drawable.ic_busy_dark_appbar;
                            break;
                    }
                } else {
                    statusImageResId = R.drawable.ic_busy_light;
                }
                break;

            case MegaChatApi.STATUS_OFFLINE:
                if (Util.isDarkMode(context)) {
                    switch (where) {
                        case STANDARD:
                            statusImageResId = R.drawable.ic_offline_dark_standard;
                            break;

                        case DRAWER:
                            statusImageResId = R.drawable.ic_offline_dark_drawer;
                            break;

                        case APPBAR:
                            statusImageResId = R.drawable.ic_offline_dark_appbar;
                            break;
                    }
                } else {
                    statusImageResId = R.drawable.ic_offline_light;
                }
                break;

            case MegaChatApi.STATUS_INVALID:
            default:
                // Do nothing, let statusImageResId be 0.
        }

        return statusImageResId;
    }

    /**
     * Sets the contact status icon and status text
     *
     * @param userStatus       contact's status
     * @param contactStateIcon view in which the status icon has to be set
     * @param contactStateText view in which the status text has to be set
     * @param where            The status icon image resource is different based on the place where it's placed.
     */
    public static void setContactStatus(int userStatus, ImageView contactStateIcon, TextView contactStateText, StatusIconLocation where) {
        MegaApplication app = MegaApplication.getInstance();
        setContactStatus(userStatus, contactStateIcon, where);

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
     * @param context          current Context
     * @param userStatus       contact's status
     * @param lastGreen        contact's last green
     * @param contactStateText view in which the last green has to be set
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
     * @param message chat message
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
    public static AudioFocusRequest getRequest(AudioManager.OnAudioFocusChangeListener listener,
                                               int focusType) {
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
    public static boolean getAudioFocus(AudioManager audioManager,
                                        AudioManager.OnAudioFocusChangeListener listener,
                                        AudioFocusRequest request, int focusType, int streamType) {
        if (audioManager == null) {
            Timber.w("Audio Manager is NULL");
            return false;
        }

        int focusRequest;
        if (SHOULD_BUILD_FOCUS_REQUEST) {
            if (request == null) {
                Timber.w("Audio Focus Request is NULL");
                return false;
            }
            focusRequest = audioManager.requestAudioFocus(request);
        } else {
            focusRequest = audioManager.requestAudioFocus(listener, streamType, focusType);
        }

        return focusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    /**
     * Method for leaving the audio focus.
     */
    public static void abandonAudioFocus(AudioManager.OnAudioFocusChangeListener listener,
                                         AudioManager audioManager, AudioFocusRequest request) {
        if (SHOULD_BUILD_FOCUS_REQUEST) {
            if (request != null) {
                audioManager.abandonAudioFocusRequest(request);
            }
        } else {
            audioManager.abandonAudioFocus(listener);
        }
    }

    /**
     * Method for obtaining the title of a MegaChatRoom.
     *
     * @param chat The chat room.
     * @return String with the title.
     */
    public static String getTitleChat(MegaChatRoom chat) {
        if (chat == null) {
            Timber.e("chat is null");
            return "";
        }

        if (chat.isActive()) {
            return chat.getTitle();
        }

        String date = formatDate(chat.getCreationTs(), DATE_AND_TIME_YYYY_MM_DD_HH_MM_FORMAT);

        return isTodayOrYesterday(chat.getCreationTs())
                ? getString(R.string.inactive_chat_title_2, date.toLowerCase(Locale.getDefault()))
                : getString(R.string.inactive_chat_title, date);
    }

    /**
     * Method for obtaining the title of a MegaChatListItem.
     *
     * @param chat The chat room.
     * @return String with the title.
     */
    public static String getTitleChat(MegaChatListItem chat) {
        if (chat == null) {
            Timber.e("chat is null");
            return "";
        }

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
     *
     * @param context Context of Activity.
     * @param chatId  Chat ID.
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
     * @param chats   Chats. If the chats is null, it's for the general chats notifications.
     */
    public static void createMuteNotificationsChatAlertDialog(Activity context, ArrayList<MegaChatListItem> chats) {

        final AlertDialog muteDialog;
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(context);
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
     * @param itemClicked    The selected item.
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
     *
     * @param context    Context of Activity.
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
    public static boolean isEnableGeneralChatNotifications() {
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
     * @param userHandle handle of the user
     * @return The user's status.
     */
    public static int getUserStatus(long userHandle) {
        return isContact(userHandle)
                ? MegaApplication.getInstance().getMegaChatApi().getUserOnlineStatus(userHandle)
                : MegaChatApi.STATUS_INVALID;
    }

    /**
     * Method to know if a message is of the geolocation type.
     *
     * @param msg The MegaChatMessage.
     * @return True if it is. False, if not
     */
    public static boolean isGeolocation(MegaChatMessage msg) {
        return (msg.getType() == MegaChatMessage.TYPE_CONTAINS_META) &&
                (msg.getContainsMeta() != null &&
                        msg.getContainsMeta().getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION);
    }

    /**
     * Method for obtaining the contact status bitmap.
     *
     * @param userStatus The contact status.
     * @return The final bitmap.
     */
    public static Bitmap getStatusBitmap(int userStatus) {
        Resources resources = MegaApplication.getInstance().getBaseContext().getResources();
        boolean isDarkMode = Util.isDarkMode(MegaApplication.getInstance());
        switch (userStatus) {
            case MegaChatApi.STATUS_ONLINE:
                return BitmapFactory.decodeResource(resources,
                        isDarkMode ? R.drawable.ic_online_dark_standard
                                : R.drawable.ic_online_light);
            case MegaChatApi.STATUS_AWAY:
                return BitmapFactory.decodeResource(resources,
                        isDarkMode ? R.drawable.ic_away_dark_standard
                                : R.drawable.ic_away_light);
            case MegaChatApi.STATUS_BUSY:
                return BitmapFactory.decodeResource(resources,
                        isDarkMode ? R.drawable.ic_busy_dark_standard
                                : R.drawable.ic_busy_light);
            case MegaChatApi.STATUS_OFFLINE:
                return BitmapFactory.decodeResource(resources,
                        isDarkMode ? R.drawable.ic_offline_dark_standard
                                : R.drawable.ic_offline_light);
            case MegaChatApi.STATUS_INVALID:
            default:
                return null;
        }
    }

    /**
     * Gets the right message to show in case MegaChatContainsMeta type is CONTAINS_META_INVALID.
     *
     * @param message MegaChatMessage containing meta with type CONTAINS_META_INVALID.
     * @return String to show for invalid meta message.
     */
    public static String getInvalidMetaMessage(MegaChatMessage message) {
        String invalidMetaMessage = getString(R.string.error_meta_message_invalid);

        if (message == null) {
            return invalidMetaMessage;
        }

        String contentMessage = message.getContent();
        if (!isTextEmpty(contentMessage)) {
            return contentMessage;
        }

        MegaChatContainsMeta meta = message.getContainsMeta();

        String metaTextMessage = meta != null ? meta.getTextMessage() : null;
        if (!isTextEmpty(metaTextMessage)) {
            return metaTextMessage;
        }

        return invalidMetaMessage;
    }

    /**
     * Method to know if a message is an image.
     *
     * @param message The android msg.
     * @return True, if it's image. False, if not.
     */
    public static boolean isMsgImage(AndroidMegaChatMessage message) {
        if (message != null && message.getMessage().getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT) {
            MegaNodeList list = message.getMessage().getMegaNodeList();
            if (list.size() == 1) {
                return MimeTypeList.typeForName(list.get(0).getName()).isImage();
            }
        }

        return false;
    }

    /**
     * Dialog to confirm if you want to delete the history of a chat.
     *
     * @param chat The MegaChatRoom.
     */
    public static void showConfirmationClearChat(Activity context, MegaChatRoom chat) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        String message = context.getString(R.string.confirmation_clear_chat_history);

        builder.setTitle(R.string.title_properties_chat_clear)
                .setMessage(message)
                .setPositiveButton(R.string.general_clear, (dialog, which) -> new ChatController(context).clearHistory(chat))
                .setNegativeButton(R.string.general_cancel, null)
                .show();
    }

    /**
     * Method to display a dialog to configure the history retention.
     *
     * @param context    Context of Activity.
     * @param idChat     The chat ID.
     * @param isDisabled True, if the Retention Time is disabled. False, otherwise.
     */
    public static void createHistoryRetentionAlertDialog(Activity context, long idChat, boolean isDisabled) {
        if (idChat == MEGACHAT_INVALID_HANDLE)
            return;

        final AlertDialog historyRetentionDialog;
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(context);

        View view = context.getLayoutInflater().inflate(R.layout.title_mute_notifications, null);
        TextView title = view.findViewById(R.id.title);
        title.setText(context.getString(R.string.title_properties_history_retention));
        TextView subtitle = view.findViewById(R.id.subtitle);
        subtitle.setText(context.getString(R.string.subtitle_properties_manage_chat));
        dialogBuilder.setCustomTitle(view);

        AtomicReference<Integer> itemClicked = new AtomicReference<>();

        ArrayList<String> stringsArray = new ArrayList<>();
        stringsArray.add(RETENTION_TIME_DIALOG_OPTION_DISABLED, context.getString(R.string.history_retention_option_disabled));
        stringsArray.add(RETENTION_TIME_DIALOG_OPTION_DAY, context.getString(R.string.history_retention_option_one_day));
        stringsArray.add(RETENTION_TIME_DIALOG_OPTION_WEEK, context.getString(R.string.history_retention_option_one_week));
        stringsArray.add(RETENTION_TIME_DIALOG_OPTION_MONTH, context.getString(R.string.history_retention_option_one_month));
        stringsArray.add(RETENTION_TIME_DIALOG_OPTION_CUSTOM, context.getString(R.string.history_retention_option_custom));

        ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(context, R.layout.checked_text_view_dialog_button, stringsArray);
        ListView listView = new ListView(context);
        listView.setAdapter(itemsAdapter);
        int optionSelected = isDisabled ? RETENTION_TIME_DIALOG_OPTION_DISABLED : getOptionSelectedFromRetentionTime(idChat);

        itemClicked.set(optionSelected);

        dialogBuilder.setSingleChoiceItems(itemsAdapter, optionSelected, (dialog, item) -> {
            ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setText(
                    context.getString(item == RETENTION_TIME_DIALOG_OPTION_CUSTOM ?
                            R.string.general_next :
                            R.string.general_ok));

            itemClicked.set(item);

            updatePositiveButton(((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE),
                    checkIfPositiveButtonShouldBeShown(idChat, item));
        });

        dialogBuilder.setPositiveButton(context.getString(itemClicked.get() ==
                        RETENTION_TIME_DIALOG_OPTION_CUSTOM ?
                        R.string.general_next : R.string.general_ok),
                (dialog, which) -> {
                    if (itemClicked.get() == RETENTION_TIME_DIALOG_OPTION_CUSTOM) {
                        if (context instanceof ManageChatHistoryActivity) {
                            ((ManageChatHistoryActivity) context).showPickers(isDisabled ?
                                    DISABLED_RETENTION_TIME :
                                    getUpdatedRetentionTimeFromAChat(idChat));
                        }
                    } else {
                        MegaApplication.getInstance().getMegaChatApi().setChatRetentionTime(idChat,
                                getSecondsFromOption(itemClicked.get()), new SetRetentionTimeListener(context));
                    }
                });

        dialogBuilder.setNegativeButton(context.getString(R.string.general_cancel), null);

        historyRetentionDialog = dialogBuilder.create();
        historyRetentionDialog.show();

        updatePositiveButton(historyRetentionDialog.getButton(AlertDialog.BUTTON_POSITIVE),
                checkIfPositiveButtonShouldBeShown(idChat, itemClicked.get()));
    }

    /**
     * Method for checking whether the positive button should be enabled or not.
     *
     * @param idChat      The chat ID.
     * @param itemClicked The option selected.
     * @return True, if it must be enabled. False, if not.
     */
    private static boolean checkIfPositiveButtonShouldBeShown(long idChat, int itemClicked) {
        return getOptionSelectedFromRetentionTime(idChat) != RETENTION_TIME_DIALOG_OPTION_DISABLED ||
                itemClicked != RETENTION_TIME_DIALOG_OPTION_DISABLED;
    }

    /**
     * Gets retention time for a particular chat.
     *
     * @param idChat The chat ID.
     * @return The retention time in seconds.
     */
    public static long getUpdatedRetentionTimeFromAChat(long idChat) {
        MegaChatRoom chat = MegaApplication.getInstance().getMegaChatApi().getChatRoom(idChat);
        if (chat != null) {
            return chat.getRetentionTime();
        }

        return DISABLED_RETENTION_TIME;
    }

    /**
     * Gets the appropriate dialogue option from the chat ID.
     *
     * @param idChat The chat ID.
     * @return The option.
     */
    private static int getOptionSelectedFromRetentionTime(long idChat) {
        long seconds = getUpdatedRetentionTimeFromAChat(idChat);

        if (seconds == DISABLED_RETENTION_TIME) {
            return RETENTION_TIME_DIALOG_OPTION_DISABLED;
        }

        long days = seconds % SECONDS_IN_DAY;
        long weeks = seconds % SECONDS_IN_WEEK;
        long months = seconds % SECONDS_IN_MONTH_30;
        long years = seconds % SECONDS_IN_YEAR;

        if (years == 0) {
            return RETENTION_TIME_DIALOG_OPTION_CUSTOM;
        }

        if (months == 0) {
            if (seconds / SECONDS_IN_MONTH_30 == 1) {
                return RETENTION_TIME_DIALOG_OPTION_MONTH;
            } else {
                return RETENTION_TIME_DIALOG_OPTION_CUSTOM;
            }
        }

        if (weeks == 0) {
            if (seconds / SECONDS_IN_WEEK == 1) {
                return RETENTION_TIME_DIALOG_OPTION_WEEK;
            } else {
                return RETENTION_TIME_DIALOG_OPTION_CUSTOM;
            }
        }

        if (days == 0 && seconds / SECONDS_IN_DAY == 1) {
            return RETENTION_TIME_DIALOG_OPTION_DAY;
        } else {
            return RETENTION_TIME_DIALOG_OPTION_CUSTOM;
        }
    }

    /**
     * Update Retention Time Dialog Positive Button.
     *
     * @param buttonPositive The button
     * @param isEnabled      True, if it must be enabled. False, if not.
     */
    private static void updatePositiveButton(final Button buttonPositive, boolean isEnabled) {
        buttonPositive.setEnabled(isEnabled);
        buttonPositive.setAlpha(isEnabled ? 1f : 0.30f);
    }

    /**
     * Method for getting the seconds from an selected option.
     *
     * @param itemClicked The selected item.
     * @return The seconds.
     */
    private static long getSecondsFromOption(int itemClicked) {
        switch (itemClicked) {
            case 1:
                return SECONDS_IN_DAY;
            case 2:
                return SECONDS_IN_WEEK;
            case 3:
                return SECONDS_IN_MONTH_30;
            default:
                return DISABLED_RETENTION_TIME;
        }
    }

    /**
     * Method for getting the appropriate String from the seconds of rentention time.
     *
     * @param seconds The retention time in seconds
     * @return The right text
     */
    public static String transformSecondsInString(long seconds) {
        if (seconds == DISABLED_RETENTION_TIME)
            return " ";

        long hours = seconds % SECONDS_IN_HOUR;
        long days = seconds % SECONDS_IN_DAY;
        long weeks = seconds % SECONDS_IN_WEEK;
        long months = seconds % SECONDS_IN_MONTH_30;
        long years = seconds % SECONDS_IN_YEAR;

        if (years == 0) {
            return MegaApplication.getInstance().getBaseContext().getResources().getString(R.string.subtitle_properties_manage_chat_label_year);
        }

        if (months == 0) {
            int month = (int) (seconds / SECONDS_IN_MONTH_30);
            return MegaApplication.getInstance().getBaseContext().getResources().getQuantityString(R.plurals.subtitle_properties_manage_chat_label_months, month, month);
        }

        if (weeks == 0) {
            int week = (int) (seconds / SECONDS_IN_WEEK);
            return MegaApplication.getInstance().getBaseContext().getResources().getQuantityString(R.plurals.subtitle_properties_manage_chat_label_weeks, week, week);
        }

        if (days == 0) {
            int day = (int) (seconds / SECONDS_IN_DAY);
            return MegaApplication.getInstance().getBaseContext().getResources().getQuantityString(R.plurals.label_time_in_days_full, day, day);
        }

        if (hours == 0) {
            int hour = (int) (seconds / SECONDS_IN_HOUR);
            return MegaApplication.getInstance().getBaseContext().getResources().getQuantityString(R.plurals.subtitle_properties_manage_chat_label_hours, hour, hour);
        }

        return " ";
    }

    /**
     * Method for updating the Time retention layout.
     *
     * @param time The retention time in seconds.
     */
    public static void updateRetentionTimeLayout(final TextView retentionTimeText, long time) {
        String timeFormatted = transformSecondsInString(time);
        if (isTextEmpty(timeFormatted)) {
            retentionTimeText.setVisibility(View.GONE);
        } else {
            String subtitleText = getString(R.string.subtitle_properties_manage_chat) + " " + timeFormatted;
            retentionTimeText.setText(subtitleText);
            retentionTimeText.setVisibility(View.VISIBLE);
        }
    }

    public static void showConfirmationLeaveChat(Context context, long chatId, ChatManagementCallback chatManagementCallback) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
        builder.setTitle(StringResourcesUtils.getString(R.string.title_confirmation_leave_group_chat))
                .setMessage(StringResourcesUtils.getString(R.string.confirmation_leave_group_chat))
                .setPositiveButton(StringResourcesUtils.getString(R.string.general_leave), (dialog, which)
                        -> chatManagementCallback.confirmLeaveChat(chatId))
                .setNegativeButton(StringResourcesUtils.getString(R.string.general_cancel), null)
                .show();
    }

    public static void showConfirmationLeaveChats(Context context, final List<MegaChatListItem> chats, ChatManagementCallback chatManagementCallback) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
        builder.setTitle(StringResourcesUtils.getString(R.string.title_confirmation_leave_group_chat))
                .setMessage(StringResourcesUtils.getString(R.string.confirmation_leave_group_chat))
                .setPositiveButton(StringResourcesUtils.getString(R.string.general_leave), (dialog, which)
                        -> chatManagementCallback.confirmLeaveChats(chats))
                .setNegativeButton(StringResourcesUtils.getString(R.string.general_cancel), null)
                .show();
    }

    /**
     * Method to compare if the current message is the same message that needs to be updated.     *
     *
     * @param messageToUpdate The message to be updated.
     * @param currentMessage  The current message.
     * @return True, if it is the same. False, if not.
     */
    public static boolean isItSameMsg(MegaChatMessage messageToUpdate, MegaChatMessage currentMessage) {
        if (messageToUpdate.getMsgId() != MEGACHAT_INVALID_HANDLE) {
            return messageToUpdate.getMsgId() == currentMessage.getMsgId();
        } else {
            return messageToUpdate.getTempId() == currentMessage.getTempId();
        }
    }

    /**
     * Method to know whether to show mute or unmute options.
     *
     * @param context  The Activity context.
     * @param chatRoom The chat room.
     * @return True, if it should be shown. False, if not.
     */
    public static boolean shouldMuteOrUnmuteOptionsBeShown(Context context, MegaChatRoom chatRoom) {
        return chatRoom != null && !chatRoom.isPreview() && isStatusConnected(context, chatRoom.getChatId()) &&
                ((chatRoom.isGroup() && chatRoom.isActive()) ||
                        (!chatRoom.isGroup() && chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_MODERATOR));
    }

    /**
     * Creates a pending message representing an attachment message.
     *
     * @param idChat       Identifier of the chat where the message has to be sent.
     * @param filePath     Path of the file which will be attached to the chat.
     * @param fileName     Name of the file which will be attached to the chat.
     * @param fromExplorer True if the file comes from File Explorer, false otherwise.
     * @return The pending message after add it to the DB.
     */
    public static PendingMessageSingle createAttachmentPendingMessage(long idChat, String filePath, String fileName, boolean fromExplorer) {
        long idPendingMessage;
        DatabaseHandler dbH = MegaApplication.getInstance().getDbH();

        PendingMessageSingle pendingMsg = new PendingMessageSingle();
        pendingMsg.setChatId(idChat);
        pendingMsg.setUploadTimestamp(System.currentTimeMillis() / 1000);
        pendingMsg.setFilePath(filePath);
        pendingMsg.setName(fileName);
        pendingMsg.setFingerprint(MegaApplication.getInstance().getMegaApi().getFingerprint(filePath));

        if (MimeTypeList.typeForName(fileName).isMp4Video() && dbH.getChatVideoQuality() != VIDEO_QUALITY_ORIGINAL) {
            idPendingMessage = dbH.addPendingMessage(pendingMsg, PendingMessageSingle.STATE_COMPRESSING);
            pendingMsg.setState(PendingMessageSingle.STATE_COMPRESSING);
        } else if (fromExplorer) {
            idPendingMessage = dbH.addPendingMessageFromExplorer(pendingMsg);
        } else {
            idPendingMessage = dbH.addPendingMessage(pendingMsg);
        }

        pendingMsg.setId(idPendingMessage);

        return pendingMsg;
    }

    /**
     * Gets the identifier of a pending message from the appData of its transfer.
     *
     * @param appData AppData of the transfer in question.
     * @return The identifier of the pending message.
     */
    public static long getPendingMessageIdFromAppData(String appData) {
        String[] parts = appData.split(APP_DATA_INDICATOR);
        String idFound = parts[parts.length - 1];

        return Long.parseLong(idFound);
    }

    /**
     * Method to share a message from the chat.
     *
     * @param context    Context of Activity.
     * @param androidMsg The msg to be shared
     * @param chatId     The ID of a chat room.
     */
    public static void shareMsgFromChat(Context context, AndroidMegaChatMessage androidMsg, long chatId) {
        MegaChatMessage msg = androidMsg.getMessage();
        MegaNode node = getNodeFromMessage(msg);
        if (node == null)
            return;

        shareNodeFromChat(context, node, chatId, msg.getMsgId());
    }

    /**
     * Method to share a node from the chat.
     *
     * @param context Context of Activity.
     * @param node    The node to be shared
     * @param chatId  The ID of a chat room.
     */
    public static void shareNodeFromChat(Context context, MegaNode node, long chatId, long msgId) {
        if (!MegaNodeUtil.shouldContinueWithoutError(context, node)) {
            return;
        }

        String path = getLocalFile(node);
        if (!isTextEmpty(path)) {
            Timber.d("Node is downloaded, so share the file");
            shareFile(context, new File(path));
        } else if (node.isExported()) {
            Timber.d("Node is exported, so share the public link");
            startShareIntent(context, new Intent(android.content.Intent.ACTION_SEND), node.getPublicLink());
        } else {
            if (msgId == MEGACHAT_INVALID_HANDLE) {
                return;
            }

            Timber.d("Node is not exported, so export Node");
            MegaApplication.getInstance().getMegaApi().exportNode(node, new ExportListener(context, new Intent(android.content.Intent.ACTION_SEND), msgId, chatId));
        }
    }

    /**
     * Method that controls which nodes of messages should be shared directly and which need to be shared via a public link.
     *
     * @param context          The Activity context.
     * @param messagesSelected The ArrayList of selected messages.
     * @param chatId           The chat ID.
     */
    public static void shareNodesFromChat(Context context, ArrayList<AndroidMegaChatMessage> messagesSelected, long chatId) {
        ArrayList<MegaNode> listNodes = new ArrayList<>();
        for (AndroidMegaChatMessage androidMessage : messagesSelected) {
            MegaNode node = getNodeFromMessage(androidMessage.getMessage());
            if (node == null) continue;

            listNodes.add(node);
        }

        if (!MegaNodeUtil.shouldContinueWithoutError(context, listNodes)) {
            return;
        }

        if (MegaNodeUtil.areAllNodesDownloaded(context, listNodes)) {
            return;
        }

        StringBuilder links = MegaNodeUtil.getExportNodesLink(listNodes);
        if (areAllNodesExported(listNodes)) {
            Timber.d("All nodes are exported, so share the public links");
            startShareIntent(context, new Intent(android.content.Intent.ACTION_SEND),
                    links.toString());
            return;
        }

        ArrayList<MegaNode> arrayNodesNotExported = getNotExportedNodes(listNodes);
        if (!arrayNodesNotExported.isEmpty()) {
            ExportListener exportListener = new ExportListener(context, arrayNodesNotExported.size(), links,
                    new Intent(android.content.Intent.ACTION_SEND), messagesSelected, chatId);

            for (MegaNode nodeNotExported : arrayNodesNotExported) {
                Timber.d("Node is not exported, so export Node");
                MegaApplication.getInstance().getMegaApi().exportNode(nodeNotExported, exportListener);
            }
        }
    }

    /**
     * Method to get the node from a message.
     *
     * @param message The MegaChatMessage.
     * @return The MegaNode obtained.
     */
    private static MegaNode getNodeFromMessage(MegaChatMessage message) {
        if (message == null) {
            return null;
        }

        MegaNodeList nodeList = message.getMegaNodeList();
        if (nodeList == null || nodeList.size() == 0) {
            return null;
        }

        return nodeList.get(0);
    }

    /**
     * Method to find out if all nodes are exported nodes.
     *
     * @param listNodes list of nodes to check.
     * @return True, if all are exported nodes. False, otherwise.
     */
    private static boolean areAllNodesExported(ArrayList<MegaNode> listNodes) {
        for (MegaNode node : listNodes) {
            if (!node.isExported()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Method to get the nodes that are not exported.
     *
     * @param listNodes The list of nodes to be checked.
     * @return The list of nodes that are not exported.
     */
    private static ArrayList<MegaNode> getNotExportedNodes(ArrayList<MegaNode> listNodes) {
        ArrayList<MegaNode> arrayNodesNotExported = new ArrayList<>();
        for (MegaNode node : listNodes) {
            if (!node.isExported()) {
                arrayNodesNotExported.add(node);
            }
        }
        return arrayNodesNotExported;
    }

    /**
     * Authorizes the node if the chat is on preview mode.
     *
     * @param node        Node to authorize.
     * @param megaChatApi MegaChatApiAndroid instance.
     * @param megaApi     MegaApiAndroid instance.
     * @param chatId      Chat identifier to check.
     * @return The authorized node if preview, same node otherwise.
     */
    public static MegaNode authorizeNodeIfPreview(MegaNode node, MegaChatApiAndroid megaChatApi,
                                                  MegaApiAndroid megaApi, long chatId) {
        MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatId);

        if (chatRoom != null && chatRoom.isPreview()) {
            MegaNode nodeAuthorized = megaApi.authorizeChatNode(node, chatRoom.getAuthorizationToken());

            if (nodeAuthorized != null) {
                Timber.d("Authorized");
                return nodeAuthorized;
            }
        }

        return node;
    }

    /**
     * Remove an attachment message from chat.
     *
     * @param activity Android activity
     * @param chatId   chat id
     * @param message  chat message
     */
    public static void removeAttachmentMessage(Activity activity, long chatId,
                                               MegaChatMessage message) {
        new MaterialAlertDialogBuilder(activity)
                .setMessage(getString(R.string.confirmation_delete_one_attachment))
                .setPositiveButton(getString(R.string.context_remove), (dialog, which) -> {
                    new ChatController(activity).deleteMessage(message, chatId);
                    activity.finish();
                })
                .setNegativeButton(getString(R.string.general_cancel), null);
    }

    /**
     * Launches an Intent to open TextFileEditorActivity.
     *
     * @param context Current context.
     * @param msgId   Message identifier.
     * @param chatId  Chat identifier.
     */
    public static void manageTextFileIntent(Context context, long msgId, long chatId) {
        context.startActivity(new Intent(context, TextEditorActivity.class)
                .putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, FROM_CHAT)
                .putExtra(MESSAGE_ID, msgId)
                .putExtra(CHAT_ID, chatId));
    }

    /**
     * Method to remove SharePreferences related to emojis and reactions when logging out.
     */
    public static void removeEmojisSharedPreferences() {
        Context context = MegaApplication.getInstance().getBaseContext();
        removeSharedPreference(context.getSharedPreferences(PREFERENCE_EMOJI, Context.MODE_PRIVATE));
        removeSharedPreference(context.getSharedPreferences(PREFERENCE_REACTION, Context.MODE_PRIVATE));
        removeSharedPreference(context.getSharedPreferences(PREFERENCE_VARIANT_EMOJI, Context.MODE_PRIVATE));
        removeSharedPreference(context.getSharedPreferences(PREFERENCE_VARIANT_REACTION, Context.MODE_PRIVATE));
    }

    /**
     * Delete a specific SharePreferences.
     *
     * @param preferences The SharedPreferences.
     */
    private static void removeSharedPreference(SharedPreferences preferences) {
        if (preferences != null) {
            preferences.edit().clear().apply();
        }
    }

    /**
     * Method for finding out if the selected message is deleted or
     * has STATUS_SERVER_REJECTED, STATUS_SENDING_MANUAL or STATUS_SENDING status
     *
     * @param removedMessages List of deleted messages
     * @param message         The message selected.
     * @return True if it's removed, rejected or in sending or manual sending status . False, otherwise.
     */
    public static boolean isMsgRemovedOrHasRejectedOrManualSendingStatus(ArrayList<RemovedMessage> removedMessages, MegaChatMessage message) {
        int status = message.getStatus();
        if (status == MegaChatMessage.STATUS_SERVER_REJECTED ||
                status == MegaChatMessage.STATUS_SENDING_MANUAL ||
                status == MegaChatMessage.STATUS_SENDING) {
            return true;
        }

        if (removedMessages == null || removedMessages.isEmpty()) {
            return false;
        }

        for (RemovedMessage removeMsg : removedMessages) {
            if ((message.getMsgId() != MEGACHAT_INVALID_HANDLE && message.getMsgId() == removeMsg.getMsgId()) ||
                    (message.getTempId() != MEGACHAT_INVALID_HANDLE && message.getTempId() == removeMsg.getMsgTempId())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Method to know if the forward icon of a own message should be displayed.
     *
     * @param removedMessages  List of deleted messages
     * @param message          The message to be checked
     * @param isMultipleSelect True, if multi-select mode is activated. False, otherwise.
     * @param cC               ChatController
     * @return True, if it must be visible. False, if it must be hidden
     */
    public static boolean checkForwardVisibilityInOwnMsg(ArrayList<RemovedMessage> removedMessages, MegaChatMessage message, boolean isMultipleSelect, ChatController cC) {
        return !isMsgRemovedOrHasRejectedOrManualSendingStatus(removedMessages, message) && !cC.isInAnonymousMode() && !isMultipleSelect;
    }

    /**
     * Method to know if the forward icon a contact message should be displayed
     *
     * @param isMultipleSelect True, if multi-select mode is activated. False, otherwise
     * @param cC               ChatController
     * @return True, if it must be visible. False, if it must be hidden
     */
    public static boolean checkForwardVisibilityInContactMsg(boolean isMultipleSelect, ChatController cC) {
        return !cC.isInAnonymousMode() && !isMultipleSelect;
    }

    /**
     * Method to find out if I am participating in a chat room
     *
     * @param chatId The chat ID
     * @return True, if I am joined to the chat. False, if not
     */
    public static boolean amIParticipatingInAChat(long chatId) {
        MegaChatRoom chatRoom = MegaApplication.getInstance().getMegaChatApi().getChatRoom(chatId);
        if (chatRoom == null)
            return false;

        if (chatRoom.isPreview()) {
            return false;
        }

        int myPrivileges = chatRoom.getOwnPrivilege();
        return myPrivileges == MegaChatRoom.PRIV_RO || myPrivileges == MegaChatRoom.PRIV_STANDARD || myPrivileges == MegaChatRoom.PRIV_MODERATOR;
    }

    /**
     * Method to get the position of a chatroom in the chat list from the chat Id
     *
     * @param chats          List of chats.
     * @param chatIdToUpdate Chat ID.
     * @return The position of the chat.
     */
    public static int getPositionFromChatId(List<MegaChatListItem> chats, long chatIdToUpdate) {
        if (chats == null || chats.isEmpty())
            return INVALID_POSITION;

        ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
        while (itrReplace.hasNext()) {
            MegaChatListItem chat = itrReplace.next();
            if (chat != null && chat.getChatId() == chatIdToUpdate) {
                return itrReplace.nextIndex() - 1;

            }
        }

        return INVALID_POSITION;
    }

    /**
     * Method to get the initial state of megaChatApi and, if necessary, initiates it.
     *
     * @param session User session
     */
    public static void initMegaChatApi(String session) {
        initMegaChatApi(session, null);
    }

    /**
     * Method to get the initial state of megaChatApi and, if necessary, initiates it.
     *
     * @param session  User session
     * @param listener MegaChat listener for logout request.
     */
    public static void initMegaChatApi(String session, MegaChatRequestListenerInterface listener) {
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();

        int state = megaChatApi.getInitState();
        if (state == MegaChatApi.INIT_NOT_DONE || state == MegaChatApi.INIT_ERROR) {
            state = megaChatApi.init(session);
            Timber.d("result of init ---> %s", state);
            switch (state) {
                case MegaChatApi.INIT_NO_CACHE:
                    Timber.d("INIT_NO_CACHE");
                    break;
                case MegaChatApi.INIT_ERROR:
                    Timber.d("INIT_ERROR");
                    megaChatApi.logout(listener);
                    break;
                default:
                    Timber.d("Chat correctly initialized");
                    break;
            }
        }
    }
}
