package mega.privacy.android.app.utils

import android.content.Context
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
    fun getAppropriateStringForCallEnded(
        chat: MegaChatRoom,
        duration: Long,
        context: Context,
    ): Spanned {
        val isOneToOne = !chat.isGroup && !chat.isMeeting
        val hasDuration = duration > 0
        var textToShow: String
        if (!isOneToOne && !hasDuration) {
            textToShow =
                context.getString(R.string.group_call_ended_no_duration_message)
        } else {
            textToShow =
                if (isOneToOne) context.getString(R.string.call_ended_message) else context.getString(
                    R.string.group_call_ended_message
                )

            val hours = TimeUnit.SECONDS.toHours(duration)
            val minutes = (TimeUnit.SECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(hours))
            val seconds =
                TimeUnit.SECONDS.toSeconds(duration) - (TimeUnit.HOURS.toSeconds(hours) + TimeUnit.MINUTES.toSeconds(
                    minutes
                ))

            if (hours > 0) {
                val textHours =
                    context.resources.getQuantityString(
                        R.plurals.plural_call_ended_messages_hours,
                        hours.toInt(),
                        hours
                    )
                textToShow += textHours
                if (minutes > 0 || seconds > 0) {
                    textToShow += ", "
                }
            }

            if (minutes > 0) {
                val textMinutes =
                    context.resources.getQuantityString(
                        R.plurals.plural_call_ended_messages_minutes,
                        minutes.toInt(),
                        minutes
                    )
                textToShow += textMinutes
                if (seconds > 0) {
                    textToShow += ", "
                }
            }

            val textSeconds =
                context.resources.getQuantityString(
                    R.plurals.plural_call_ended_messages_seconds,
                    seconds.toInt(),
                    seconds
                )
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
    fun getAppropriateStringForCallRejected(context: Context): Spanned {
        val textToShow: String = context.getString(R.string.call_rejected_messages)
        return TextUtil.replaceFormatCallEndedMessage(textToShow)
    }

    /**
     * Get the appropriate string for call no answered
     *
     * @param lastMsgSender Handle of sender
     * @return appropriate string for call no answered
     */
    @JvmStatic
    fun getAppropriateStringForCallNoAnswered(lastMsgSender: Long, context: Context): Spanned {
        val textToShow: String =
            if (lastMsgSender == MegaApplication.getInstance().megaChatApi.myUserHandle) context.getString(
                R.string.call_not_answered_messages
            ) else context.getString(R.string.call_missed_messages)

        return TextUtil.replaceFormatCallEndedMessage(textToShow)
    }

    /**
     * Get the appropriate string for call failed
     *
     * @return appropriate string for call failed
     */
    @JvmStatic
    fun getAppropriateStringForCallFailed(context: Context): Spanned {
        val textToShow: String = context.getString(R.string.call_failed_messages)
        return TextUtil.replaceFormatCallEndedMessage(textToShow)
    }

    /**
     * Get the appropriate string for call cancelled
     *
     * @param lastMsgSender Handle of sender
     * @return appropriate string for call cancelled
     */
    @JvmStatic
    fun getAppropriateStringForCallCancelled(lastMsgSender: Long, context: Context): Spanned {
        val textToShow: String =
            if (lastMsgSender == MegaApplication.getInstance().megaChatApi.myUserHandle) context.getString(
                R.string.call_cancelled_messages
            ) else context.getString(R.string.call_missed_messages)

        return TextUtil.replaceFormatCallEndedMessage(textToShow)
    }

    /**
     * Get the appropriate string for call started
     *
     * @return appropriate string for call started
     */
    @JvmStatic
    fun getAppropriateStringForCallStarted(context: Context): Spanned {
        val textToShow: String = context.getString(R.string.call_started_messages)
        return TextUtil.replaceFormatCallEndedMessage(textToShow)
    }
}