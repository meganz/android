package mega.privacy.android.app.components;

import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

import nz.mega.sdk.MegaPushNotificationSettings;
import mega.privacy.android.app.MegaApplication;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class PushNotificationSettingManagement {

    private MegaPushNotificationSettings push;

    public PushNotificationSettingManagement() {
        push = getPushNotificationSetting();
    }

    /**
     * Method for getting the PushNotificationSetting instance.
     *
     * @return MegaPushNotificationSettings.
     */
    public MegaPushNotificationSettings getPushNotificationSetting() {
        if (this.push == null) {
            if (MegaApplication.getInstance().getMegaApi() != null) {
                MegaApplication.getInstance().getMegaApi().getPushNotificationSettings(null);
            }
        }

        return push;
    }

    /**
     * Method for getting the MegaPushNotificationSettings and sending it via a broadcast
     *
     * @param receivedPush The MegaPushNotificationSettings obtained from the request.
     */
    public void sendPushNotificationSettings(MegaPushNotificationSettings receivedPush) {
        push = receivedPush != null ? receivedPush.copy() : MegaPushNotificationSettings.createInstance();
        MegaApplication.getInstance().sendBroadcast(new Intent(ACTION_UPDATE_PUSH_NOTIFICATION_SETTING));
    }

    /**
     * Method that controls the change in general and specific chat notifications.
     *
     * @param context Context of Activity.
     * @param option  Muting option selected
     * @param chatId  Chat ID.
     */
    public void controlMuteNotifications(Context context, String option, long chatId) {
        switch (option) {
            case NOTIFICATIONS_DISABLED:
                if (chatId == MEGACHAT_INVALID_HANDLE) {
                    push.enableChats(false);
                } else {
                    push.enableChat(chatId, false);
                }
                break;

            case NOTIFICATIONS_ENABLED:
                if (chatId == MEGACHAT_INVALID_HANDLE) {
                    push.enableChats(true);
                } else {
                    push.enableChat(chatId, true);
                }
                break;

            case NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING:
            case NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING:
                long timestamp = getCalendarSpecificTime(option).getTimeInMillis();
                if (chatId == MEGACHAT_INVALID_HANDLE) {
                    push.setGlobalChatsDnd(timestamp);
                } else {
                    push.setChatDnd(chatId, timestamp);
                }
                break;

            default:
                Calendar newCalendar = Calendar.getInstance();
                newCalendar.setTimeInMillis(System.currentTimeMillis());
                switch (option) {
                    case NOTIFICATIONS_30_MINUTES:
                        newCalendar.add(Calendar.MINUTE, 30);
                        break;
                    case NOTIFICATIONS_1_HOUR:
                        newCalendar.add(Calendar.HOUR, 1);
                        break;
                    case NOTIFICATIONS_6_HOURS:
                        newCalendar.add(Calendar.HOUR, 6);
                        break;
                    case NOTIFICATIONS_24_HOURS:
                        newCalendar.add(Calendar.HOUR, 24);
                        break;
                }

                long time = newCalendar.getTimeInMillis();
                if (chatId == MEGACHAT_INVALID_HANDLE) {
                    push.setGlobalChatsDnd(time);
                } else {
                    push.setChatDnd(chatId, time);
                }
        }

        MegaApplication.getInstance().getMegaApi().setPushNotificationSettings(push, null);
        muteChat(context, option);
    }
}
