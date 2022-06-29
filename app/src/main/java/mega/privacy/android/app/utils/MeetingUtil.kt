package mega.privacy.android.app.utils

import android.text.Spanned
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import nz.mega.sdk.MegaChatRoom
import java.util.concurrent.TimeUnit

object MeetingUtil {

    /**
     * Get the appropriate string for call ended
     *
     * @param chat MegaChatRoom
     * @param duration Call duration
     * @return appropriate string for call ended
     */
    @JvmStatic
    fun getAppropriateStringForCallEnded(chat: MegaChatRoom, duration: Long): Spanned {
        val isOneToOne = !chat.isGroup && !chat.isMeeting
        val hasDuration = duration > 0
        var textToShow: String
        if (!isOneToOne && !hasDuration) {
            textToShow =
                StringResourcesUtils.getString(R.string.group_call_ended_no_duration_message)
        } else {
            textToShow =
                if (isOneToOne) StringResourcesUtils.getString(R.string.call_ended_message) else StringResourcesUtils.getString(
                    R.string.group_call_ended_message)

            val hours = TimeUnit.SECONDS.toHours(duration)
            val minutes = (TimeUnit.SECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(hours))
            val seconds =
                TimeUnit.SECONDS.toSeconds(duration) - (TimeUnit.HOURS.toSeconds(hours) + TimeUnit.MINUTES.toSeconds(
                    minutes))

            if (hours > 0) {
                val textHours =
                    StringResourcesUtils.getQuantityString(R.plurals.plural_call_ended_messages_hours,
                        hours.toInt(),
                        hours)
                textToShow += textHours
                if (minutes > 0 || seconds > 0) {
                    textToShow += ", "
                }
            }

            if (minutes > 0) {
                val textMinutes =
                    StringResourcesUtils.getQuantityString(R.plurals.plural_call_ended_messages_minutes,
                        minutes.toInt(),
                        minutes)
                textToShow += textMinutes
                if (seconds > 0) {
                    textToShow += ", "
                }
            }

            val textSeconds =
                StringResourcesUtils.getQuantityString(R.plurals.plural_call_ended_messages_seconds,
                    seconds.toInt(),
                    seconds)
            textToShow += textSeconds
        }

        return TextUtil.replaceFormatCallEndedMessage(textToShow)
    }

    /**
     * Get the appropriate string for call rejected
     *
     * @return appropriate string for call rejected
     */
    @JvmStatic
    fun getAppropriateStringForCallRejected(): Spanned {
        val textToShow: String = StringResourcesUtils.getString(R.string.call_rejected_messages)
        return TextUtil.replaceFormatCallEndedMessage(textToShow)
    }

    /**
     * Get the appropriate string for call no answered
     *
     * @param lastMsgSender Handle of sender
     * @return appropriate string for call no answered
     */
    @JvmStatic
    fun getAppropriateStringForCallNoAnswered(lastMsgSender: Long): Spanned {
        val textToShow: String =
            if (lastMsgSender == MegaApplication.getInstance().megaChatApi.myUserHandle) StringResourcesUtils.getString(
                R.string.call_not_answered_messages) else StringResourcesUtils.getString(R.string.call_missed_messages)

        return TextUtil.replaceFormatCallEndedMessage(textToShow)
    }

    /**
     * Get the appropriate string for call failed
     *
     * @return appropriate string for call failed
     */
    @JvmStatic
    fun getAppropriateStringForCallFailed(): Spanned {
        val textToShow: String = StringResourcesUtils.getString(R.string.call_failed_messages)
        return TextUtil.replaceFormatCallEndedMessage(textToShow)
    }

    /**
     * Get the appropriate string for call cancelled
     *
     * @param lastMsgSender Handle of sender
     * @return appropriate string for call cancelled
     */
    @JvmStatic
    fun getAppropriateStringForCallCancelled(lastMsgSender: Long): Spanned {
        val textToShow: String =
            if (lastMsgSender == MegaApplication.getInstance().megaChatApi.myUserHandle) StringResourcesUtils.getString(
                R.string.call_cancelled_messages) else StringResourcesUtils.getString(R.string.call_missed_messages)

        return TextUtil.replaceFormatCallEndedMessage(textToShow)
    }

    /**
     * Get the appropriate string for call started
     *
     * @return appropriate string for call started
     */
    @JvmStatic
    fun getAppropriateStringForCallStarted(): Spanned {
        val textToShow: String = StringResourcesUtils.getString(R.string.call_started_messages)
        return TextUtil.replaceFormatCallEndedMessage(textToShow)
    }
}