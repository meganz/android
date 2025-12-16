package mega.privacy.android.app.deeplinks.model

import androidx.navigation3.runtime.NavKey

data class DeepLinksUIState(
    val navKeys: List<NavKey>? = null,
)