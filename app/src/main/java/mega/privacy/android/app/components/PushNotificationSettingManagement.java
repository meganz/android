package mega.privacy.android.app.components;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Calendar;

import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaPushNotificationSettings;
import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaPushNotificationSettingsAndroid;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.TimeUtils.*;

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
            updateMegaPushNotificationSetting();
        }

        return push;
    }

    /**
     * Method for getting MegaPushNotificationSettings from megaApi.
     */
    public void updateMegaPushNotificationSetting(){
        if (MegaApplication.getInstance().getMegaApi() != null) {
            MegaApplication.getInstance().getMegaApi().getPushNotificationSettings(null);
        }
    }

    /**
     * Method for getting the MegaPushNotificationSettings and sending it via a broadcast
     *
     * @param receivedPush The MegaPushNotificationSettings obtained from the request.
     */
    public void sendPushNotificationSettings(MegaPushNotificationSettings receivedPush) {
        push = receivedPush != null ? MegaPushNotificationSettingsAndroid.copy(receivedPush) : MegaPushNotificationSettings.createInstance();
        MegaApplication.getInstance().sendBroadcast(new Intent(ACTION_UPDATE_PUSH_NOTIFICATION_SETTING));
    }

    /**
     * Method that controls the change in the notifications of a specific chat.
     *
     * @param context Context of Activity.
     * @param option  Muting option selected.
     * @param chatId  Chat ID.
     */
    public void controlMuteNotificationsOfAChat(Context context, String option, long chatId) {
        ArrayList<MegaChatListItem> chats = new ArrayList<>();
        MegaChatListItem chat = MegaApplication.getInstance().getMegaChatApi().getChatListItem(chatId);
        if (chat != null) {
            chats.add(chat);
            controlMuteNotifications(context, option, chats);
        }
    }

    /**
     * Method that controls the change in general and specific chat notifications.
     *
     * @param context Context of Activity.
     * @param option  Muting option selected
     * @param chats  List of Chats.
     */
    public void controlMuteNotifications(Context context, String option, ArrayList<MegaChatListItem> chats) {
        switch (option) {
            case NOTIFICATIONS_DISABLED:
                if (chats == null) {
                    push.enableChats(false);
                } else {
                    for (MegaChatListItem chat : chats) {
                        if (chat != null) {
                            push.enableChat(chat.getChatId(), false);
                        }
                    }
                }
                break;

            case NOTIFICATIONS_ENABLED:
                if (chats == null) {
                    push.enableChats(true);
                } else {
                    for (MegaChatListItem chat : chats) {
                        if (chat != null) {
                            push.enableChat(chat.getChatId(), true);
                        }
                    }
                }
                break;

            case NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING:
            case NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING:
                long timestamp = getCalendarSpecificTime(option).getTimeInMillis()/1000;
                if (chats == null) {
                    push.setGlobalChatsDnd(timestamp);
                } else {
                    for (MegaChatListItem chat : chats) {
                        if (chat != null) {
                            push.setChatDnd(chat.getChatId(), timestamp);
                        }
                    }
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

                long time = newCalendar.getTimeInMillis()/1000;
                if (chats == null) {
                    push.setGlobalChatsDnd(time);
                } else {
                    for (MegaChatListItem chat : chats) {
                        if (chat != null) {
                            push.setChatDnd(chat.getChatId(), time);
                        }
                    }
                }
        }

        MegaApplication.getInstance().getMegaApi().setPushNotificationSettings(push, null);
        muteChat(context, option);
    }
}
