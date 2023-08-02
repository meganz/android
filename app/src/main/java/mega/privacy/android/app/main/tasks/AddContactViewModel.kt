package mega.privacy.android.app.main.tasks

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import javax.inject.Inject

/**
 * AddContactView Model
 * @param getFeatureFlagValue [GetFeatureFlagValueUseCase]
 */
@HiltViewModel
class AddContactViewModel @Inject constructor(private val getFeatureFlagValue: GetFeatureFlagValueUseCase) :
    ViewModel() {
    /**
     * Gets contact enabled Value from feature flag
     */
    fun getContactFeatureEnabledFromFlag() = runBlocking {
        getFeatureFlagValue(AppFeatures.ContactVerification)
    }
}