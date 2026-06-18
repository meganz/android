package mega.privacy.android.feature.cloudexplorer.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/**
 * Builds a [ViewModelStoreOwner] whose store resolves each given ViewModel by its class, so a
 * composable's internal `hiltViewModel` calls return these mocks instead of hitting the Hilt graph.
 */
internal fun viewModelStoreOwnerOf(vararg entries: Pair<Class<*>, ViewModel>): ViewModelStoreOwner {
    val store = mock<ViewModelStore> {
        entries.forEach { (type, viewModel) ->
            on { get(argThat<String> { contains(type.canonicalName.orEmpty()) }) } doReturn viewModel
        }
    }
    return mock { on { viewModelStore } doReturn store }
}
