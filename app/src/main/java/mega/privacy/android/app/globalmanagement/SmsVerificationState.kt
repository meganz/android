package mega.privacy.android.app.globalmanagement

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-scoped holder tracking whether the SMS verification screen has been shown.
 *
 * Kept as an injectable [Singleton] so its lifetime matches the process, replacing the former
 * mutable static on `MegaApplication`.
 */
@Singleton
class SmsVerificationState @Inject constructor() {
    /**
     * Whether the SMS verification screen has already been shown in this process.
     */
    var isVerificationShown = false
}
