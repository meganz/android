package mega.privacy.android.app.activities.editProfile

import androidx.hilt.lifecycle.ViewModelInject
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.di.MegaApi
import nz.mega.sdk.MegaApiAndroid

class EditProfileViewModel @ViewModelInject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) : BaseRxViewModel() {
}