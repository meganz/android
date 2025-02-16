package mega.privacy.android.app.presentation.security

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.qualifiers.ActivityContext
import javax.inject.Inject

/**
 * Passcode facade
 *
 * A component that can be injected onto any [AppCompatActivity] to enforce a Passcode
 * security check.
 *
 * All the necessary setup and implementation is handled inside this class and requires no
 * further changes to the host activity
 *
 * @property context
 */
class PasscodeFacade @Inject constructor(
    @ActivityContext private val context: Context,
) : PasscodeCheck {

    /**
     * Used to disable passcode.
     * E.g.: PdfViewerActivity when it is opened from outside the app.
     */
    private var isDisabled = false

    override fun disablePasscode() {
        isDisabled = true
    }

    override fun enablePassCode() {
        isDisabled = false
    }

    override fun canLock() = isDisabled.not()

}
