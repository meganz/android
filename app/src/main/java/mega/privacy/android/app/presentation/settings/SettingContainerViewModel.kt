package mega.privacy.android.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.settings.model.SettingUiItem
import timber.log.Timber

/**
 * Setting container view model
 */
class SettingContainerViewModel : ViewModel() {
    private val _state = MutableStateFlow(emptyList<SettingUiItem>())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                getSettings()
                    .catch { Timber.e(it) }
                    .collectLatest {
                        _state.value = it
                    }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun getSettings(): Flow<List<SettingUiItem>> {
        return emptyFlow()
    }
}
