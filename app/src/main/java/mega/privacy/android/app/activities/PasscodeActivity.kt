package mega.privacy.android.app.activities

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.appstate.MegaActivity.Companion.EXTERNAL_ACTIONS
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.presentation.security.PasscodeProcessLifecycleOwner
import javax.inject.Inject

@AndroidEntryPoint
open class PasscodeActivity : BaseActivity() {

    @Inject
    lateinit var passcodeFacade: PasscodeCheck

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContainerWrapper.setPasscodeCheck(passcodeFacade)
    }

    @Suppress("DEPRECATION")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        if (isExternalIntent(intent)) {
            PasscodeProcessLifecycleOwner.get().skipNextPasscodeCheck()
        }
        super.startActivityForResult(intent, requestCode, options)
    }

    @Suppress("DEPRECATION")
    override fun startIntentSenderForResult(
        intent: IntentSender,
        requestCode: Int,
        fillInIntent: Intent?,
        flagsMask: Int,
        flagsValues: Int,
        extraFlags: Int,
        options: Bundle?,
    ) {
        if (intent.creatorPackage == packageName) {
            PasscodeProcessLifecycleOwner.get().skipNextPasscodeCheck()
        }
        super.startIntentSenderForResult(
            intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options
        )
    }

    private fun isExternalIntent(intent: Intent): Boolean {
        return intent.action in EXTERNAL_ACTIONS
    }
}
