package mega.privacy.android.domain.entity.verification

/**
 * Sms permission
 */
sealed interface SmsPermission

/**
 * Sms permission - Opt in verification sms calls allowed
 */
object OptInVerification: SmsPermission

/**
 * Sms permission - Unblock sms calls allowed
 */
object Unblock: SmsPermission