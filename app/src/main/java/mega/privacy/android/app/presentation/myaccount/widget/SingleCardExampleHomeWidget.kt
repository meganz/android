package mega.privacy.android.app.presentation.myaccount.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.navigation.destination.MyAccountNavKey
import javax.inject.Inject

class SingleCardExampleHomeWidget @Inject constructor(
) : HomeWidget {
    override val identifier: String = "AccountHomeWidgetProvider"
    override val defaultOrder: Int = 1
    override val canDelete: Boolean = false

    override suspend fun getWidgetName() = LocalizedText.Literal("My Account")

    @Composable
    override fun DisplayWidget(
        modifier: Modifier,
        onNavigate: (NavKey) -> Unit,
        transferHandler: TransferHandler,
    ) {
        val viewModel = hiltViewModel<SingleCardExampleWidgetViewModel>()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        MyAccountHomeWidget(state, modifier, { onNavigate(MyAccountNavKey()) })
    }
}

