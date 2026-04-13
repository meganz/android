package mega.privacy.android.app.presentation.container

import mega.privacy.android.core.passcode.PasscodeCheck

/**
 * App container wrapper - Interface to add compose container functionality to legacy Activities
 */
interface AppContainerWrapper{
    /**
     * Set passcode check
     *
     * @param check
     */
    fun setPasscodeCheck(check: PasscodeCheck)
}