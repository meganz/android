package mega.privacy.android.app.psa

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.Factory
import nz.mega.sdk.MegaApiAndroid

/**
 * ViewModel factory for PsaViewModel.
 */
internal class PsaViewModelFactory(
    private val megaApi: MegaApiAndroid,
    private val context: Context,
) : Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PsaViewModel::class.java)) {
            return PsaViewModel(megaApi, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
