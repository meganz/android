package mega.privacy.android.app.providers

import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.utils.PasscodeUtil
import javax.inject.Inject

@AndroidEntryPoint
open class PasscodeFileProviderActivity: AppCompatActivity() {

    @Inject
    lateinit var passcodeUtil: PasscodeUtil

    override fun onPause() {
        passcodeUtil.pauseUpdate()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        passcodeUtil.resume(false)
    }
}