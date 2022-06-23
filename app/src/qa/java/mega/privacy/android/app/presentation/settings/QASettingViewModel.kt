package mega.privacy.android.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.UpdateApp
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class QASettingViewModel @Inject constructor(
    private val updateApp: UpdateApp,
) : ViewModel() {

    fun checkUpdatePressed() {
        viewModelScope.launch {
            updateApp()
                .onCompletion {
                    it?.let { error ->
                        Timber.e(error, "Auto update failed")
                    }
                }
                .collect()
        }
    }
}