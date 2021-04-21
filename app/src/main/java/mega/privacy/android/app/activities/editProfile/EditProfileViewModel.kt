package mega.privacy.android.app.activities.editProfile

import androidx.hilt.lifecycle.ViewModelInject
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.lollipop.MyAccountInfo
import nz.mega.sdk.MegaApiAndroid

class EditProfileViewModel @ViewModelInject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) : BaseRxViewModel() {

    private var accountInfo: MyAccountInfo = MegaApplication.getInstance().myAccountInfo

    fun getName(): String? = accountInfo.fullName

    fun getEmail(): String = megaApi.myEmail
}