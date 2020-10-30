package mega.privacy.android.app.psa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.Factory
import nz.mega.sdk.MegaApiAndroid

/**
 * ViewModel factory for PsaViewModel.
 */
internal class PsaViewModelFactory(
    private val megaApi: MegaApiAndroid
) : Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PsaViewModel::class.java)) {
            return PsaViewModel(megaApi) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
