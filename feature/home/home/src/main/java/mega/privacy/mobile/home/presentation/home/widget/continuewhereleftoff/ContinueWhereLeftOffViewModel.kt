package mega.privacy.mobile.home.presentation.home.widget.continuewhereleftoff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.usecase.continuewhereleftoff.MonitorContinueWhereLeftOffItemsUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ContinueWhereLeftOffViewModel @Inject constructor(
    monitorContinueWhereLeftOffItemsUseCase: MonitorContinueWhereLeftOffItemsUseCase,
) : ViewModel() {

    init {
        Timber.d("CWLO: ViewModel created")
    }

    val items: StateFlow<List<ContinueWhereLeftOffItem>> =
        monitorContinueWhereLeftOffItemsUseCase(limit = MAX_CAROUSEL_ITEMS)
            .onEach { Timber.d("CWLO: Use case emitted ${it.size} items") }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    companion object {
        private const val MAX_CAROUSEL_ITEMS = 10
    }
}
