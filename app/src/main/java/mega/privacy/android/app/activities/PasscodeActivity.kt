package mega.privacy.android.app.activities

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.utils.JobUtil
import mega.privacy.android.app.utils.PasscodeUtil
import mega.privacy.android.app.utils.Util.setAppFontSize
import javax.inject.Inject

@AndroidEntryPoint
open class PasscodeActivity : BaseActivity() {

    companion object {
        private const val SCREEN_ORIENTATION = "SCREEN_ORIENTATION"
    }

    @Inject
    lateinit var passcodeUtil: PasscodeUtil
    private var lastStart = 0L

    /**
     * Used to control when onResume comes from a screen orientation change.
     * Since onConfigurationChanged is not implemented in all activities,
     * it cannot be used for that purpose.
     *
     * @see android.app.Activity#onConfigurationChanged(Configuration)
     */
    private var isScreenRotation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null &&
            resources.configuration.orientation != savedInstanceState.getInt(SCREEN_ORIENTATION)
        ) {
            isScreenRotation = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SCREEN_ORIENTATION, resources.configuration.orientation)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        passcodeUtil.pause()
        lastStart = System.currentTimeMillis()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        setAppFontSize(this)

        if (isScreenRotation) {
            isScreenRotation = false
        } else if (MegaApplication.getPasscodeManagement().showPasscodeScreen) {
            passcodeUtil.resume()
        }

        if (System.currentTimeMillis() - lastStart > 1000
            && megaApi.rootNode != null && !MegaApplication.isLoggingIn()
        ) {
            JobUtil.startCameraUploadServiceIgnoreAttr(this@PasscodeActivity)
        }
    }
}