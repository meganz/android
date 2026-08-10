package mega.privacy.android.app.deeplinks.model

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.NavOptions

data class DeepLinksUIState(
    val navKeys: List<NavKey>? = null,
    val navOptions: NavOptions? = null,
)