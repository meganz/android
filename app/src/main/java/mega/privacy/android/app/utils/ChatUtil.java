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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MarqueeTextView;
import mega.privacy.android.app.components.SimpleSpanBuilder;
import mega.privacy.android.app.components.twemoji.EmojiManager;
import mega.privacy.android.app.components.twemoji.EmojiRange;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.components.twemoji.EmojiUtilsShortcodes;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatItemPreferences;
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

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class ChatUtil {

    private static final float DOWNSCALE_IMAGES_PX = 2000000f;

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

    private static String getStringPlural(String option){
        try{
            option = option.replace("[A]", "");
            option = option.replace("[/A]", "");
            option = option.replace("[B]", "");
            option = option.replace("[/B]", "");
            option = option.replace("[C]", "");
            option = option.replace("[/C]", "");
        }catch (Exception e){
            logWarning("Error replacing text.");
        }
        return option;
    }

    /**
     * Method for displaying a dialog to mute all chat notifications.
     *
     * @param context The context of Activity.
     */
    public static void createMuteChatAlertDialog(Activity context) {
        final android.app.AlertDialog muteDialog;
        android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
        View view = context.getLayoutInflater().inflate(R.layout.title_mute_notifications, null);
        dialogBuilder.setCustomTitle(view);
        CharSequence[] items = {
                context.getString(R.string.mute_chatroom_notification_option_off),
                getStringPlural(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_minutes, 10, 10)),
                getStringPlural(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, 1, 1)),
                getStringPlural(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, 24, 24))};


        int itemClicked = 0;
//        if (!MegaApplication.getInstance().getDbH().areNotificationsEnabled(chatHandle)) {
//            itemClicked = NOTIFICATIONS_30_MINUTES;
//        }

        dialogBuilder.setSingleChoiceItems(items, itemClicked, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (item) {
                    case 0:
                        dialog.dismiss();
                        break;

                    default:
                        dialog.dismiss();
                        break;
                }
            }
        });

        dialogBuilder.setPositiveButton(context.getString(R.string.general_cancel), (dialog, which) -> dialog.dismiss());
        muteDialog = dialogBuilder.create();
        muteDialog.show();
    }

    private static int getItemClicked(String typeMute){
        switch (typeMute){
            case NOTIFICATIONS_30_MINUTES:
                return 1;
            case NOTIFICATIONS_1_HOUR:
                return 2;
            case NOTIFICATIONS_6_HOURS:
                return 3;
            case NOTIFICATIONS_24_HOURS:
                return 4;
            case NOTIFICATIONS_DISABLED:
                return 5;
            default:
                return 0;
        }
    }

    private static String getTypeMute(int itemClicked){
        switch (itemClicked){
            case 0 :
                return NOTIFICATIONS_ENABLED;
            case 1:
                return NOTIFICATIONS_30_MINUTES;
            case 2:
                return NOTIFICATIONS_1_HOUR;
            case 3:
                return NOTIFICATIONS_6_HOURS;
            case 4:
                return NOTIFICATIONS_24_HOURS;
            case 5:
                return NOTIFICATIONS_DISABLED;
            default:
                return NOTIFICATIONS_ENABLED;
        }
    }

    /**
     * Method to display a dialog to mute a specific chat room.
     *
     * @param chatId  The chat ID.
     */
    public static void createMuteChatRoomAlertDialog(Context context, long chatId) {
        if (chatId == MEGACHAT_INVALID_HANDLE)
            return;

        final android.app.AlertDialog muteDialog;
        android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
        dialogBuilder.setTitle(context.getString(R.string.title_dialog_mute_chatroom_notifications));
        CharSequence[] items = {
                context.getString(R.string.mute_chatroom_notification_option_off),
                getStringPlural(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_minutes, 30, 30)),
                getStringPlural(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, 1, 1)),
                getStringPlural(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, 6, 6)),
                getStringPlural(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, 24, 24)),
                context.getString(R.string.mute_chatroom_notification_option_indefinitely)};

        String chatHandle = String.valueOf(chatId);
        String typeMuted = MegaApplication.getInstance().getDbH().areNotificationsEnabled(chatHandle);
        AtomicReference<Integer> itemClicked = new AtomicReference<>(getItemClicked(typeMuted));
        dialogBuilder.setSingleChoiceItems(items, itemClicked.get(), (dialog, item) -> {
            itemClicked.set(item);
        });

        dialogBuilder.setPositiveButton(context.getString(R.string.general_ok),
                (dialog, which) -> {
                    MegaApplication.getInstance().controlMuteNotifications(chatId, getTypeMute(itemClicked.get()));
                    dialog.dismiss();
                });
        dialogBuilder.setNegativeButton(context.getString(R.string.general_cancel), (dialog, which) -> dialog.dismiss());
        muteDialog = dialogBuilder.create();
        muteDialog.show();
    }

    public static void muteChat(Context context, long chatId, String muteOption){
        ChatController chatC = new ChatController(context);
        chatC.muteChat(chatId, muteOption);
    }


    public static String getMutedPeriodString(String typeMute){
        Context context = MegaApplication.getInstance().getBaseContext();
        switch (typeMute) {
            case NOTIFICATIONS_ENABLED:
                break;
            case NOTIFICATIONS_30_MINUTES:
                return getStringPlural(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_minutes, 30, 30));
            case NOTIFICATIONS_1_HOUR:
                return getStringPlural(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, 1, 1));
            case NOTIFICATIONS_6_HOURS:
                return getStringPlural(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, 6, 6));
            case NOTIFICATIONS_24_HOURS:
                return getStringPlural(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, 24, 24));
            case NOTIFICATIONS_DISABLED:
               break;
        }
        return null;
    }

}
