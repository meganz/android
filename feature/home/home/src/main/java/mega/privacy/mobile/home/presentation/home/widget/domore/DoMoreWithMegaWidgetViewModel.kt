package mega.privacy.mobile.home.presentation.home.widget.domore

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.mobile.home.presentation.home.widget.domore.model.DoMoreWithMegaUiState
import javax.inject.Inject

/**
 * ViewModel for [DoMoreWithMegaWidget].
 *
 * Collects every [DoMoreWithMegaItem] contributed via Dagger `@IntoSet`, sorts them by
 * their order, and gates the whole section behind the
 * [ApiFeatures.DoMoreWithMEGA] remote feature flag.
 */
@HiltViewModel
class DoMoreWithMegaWidgetViewModel @Inject constructor(
    private val items: Set<@JvmSuppressWildcards DoMoreWithMegaItem>,
) : ViewModel() {

    val uiState: StateFlow<DoMoreWithMegaUiState> = MutableStateFlow(
        DoMoreWithMegaUiState(items = items.sortedBy { it.identifier.ordinal })
    ).asStateFlow()
}
