package mega.privacy.android.app.psa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.Factory
import nz.mega.sdk.MegaApiAndroid

internal class PsaViewModelFactory(
    private val megaApi: MegaApiAndroid
) : Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PsaViewModel::class.java)) {
            return PsaViewModel(megaApi) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
