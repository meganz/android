package mega.privacy.android.app.presentation.twofactorauthentication

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.domain.usecase.IsMasterKeyExported
import javax.inject.Inject

/**
 * TwoFactorAuthenticationViewModel of the TwoFactorAuthenticationActivity
 */
@HiltViewModel
class TwoFactorAuthenticationViewModel @Inject constructor(
    private val isMasterKeyExported: IsMasterKeyExported,
) : ViewModel() {

}