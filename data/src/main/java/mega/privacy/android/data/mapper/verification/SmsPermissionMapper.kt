package mega.privacy.android.data.mapper.verification

import mega.privacy.android.domain.entity.verification.SmsPermission

/**
 * Sms permission mapper
 */
internal fun interface SmsPermissionMapper {
    /**
     * Invoke
     *
     * @param state
     * @return list of allowed permissions
     */
    operator fun invoke(state: Int): List<SmsPermission>
}
