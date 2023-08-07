package mega.privacy.android.data.mapper.useralert

import mega.privacy.android.domain.entity.useralert.UserAlertChange
import nz.mega.sdk.MegaUserAlert
import javax.inject.Inject

/**
 * Mapper to convert User Alert changes to List of [UserAlertChange]
 */
internal class UserAlertChangesMapper @Inject constructor() {

    operator fun invoke(userAlert: MegaUserAlert): List<UserAlertChange> =
        UserAlertChange.values().filter { alertChange ->
            userAlert.hasSchedMeetingChanged(userAlertChanges[alertChange] as Long)
        }

    companion object {
        internal val userAlertChanges: Map<UserAlertChange, Long> = mapOf(
            UserAlertChange.Title to MegaUserAlert.SM_CHANGE_TYPE_TITLE.toLong(),
            UserAlertChange.Description to MegaUserAlert.SM_CHANGE_TYPE_DESCRIPTION.toLong(),
            UserAlertChange.Canceled to MegaUserAlert.SM_CHANGE_TYPE_CANCELLED.toLong(),
            UserAlertChange.Timezone to MegaUserAlert.SM_CHANGE_TYPE_TIMEZONE.toLong(),
            UserAlertChange.StartDate to MegaUserAlert.SM_CHANGE_TYPE_STARTDATE.toLong(),
            UserAlertChange.EndDate to MegaUserAlert.SM_CHANGE_TYPE_ENDDATE.toLong(),
            UserAlertChange.Rules to MegaUserAlert.SM_CHANGE_TYPE_RULES.toLong(),
        )
    }
}
