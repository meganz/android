package mega.privacy.android.app.activities

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.presentation.security.PasscodeCheck
import javax.inject.Inject

@AndroidEntryPoint
open class PasscodeActivity : BaseActivity() {

    @Inject
    lateinit var passcodeFacade: PasscodeCheck

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContainerWrapper.setPasscodeCheck(passcodeFacade)
    }

}
