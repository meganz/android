package mega.privacy.mobile.home.presentation.home.model

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey

fun interface HomeWidgetItem {
    @SuppressLint("ComposableNaming")
    @Composable
    fun content(modifier: Modifier, onNavigate: (NavKey) -> Unit)
}
