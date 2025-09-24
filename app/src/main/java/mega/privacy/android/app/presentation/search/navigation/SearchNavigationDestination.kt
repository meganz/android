package mega.privacy.android.app.presentation.search.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import mega.privacy.android.navigation.destination.SearchNode
import mega.privacy.android.navigation.megaNavigator

fun NavGraphBuilder.searchLegacyDestination(
    removeDestination: () -> Unit,
) {
    composable<SearchNode> {
        val route = it.toRoute<SearchNode>()
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.megaNavigator.openSearchActivity(
                context = context,
                nodeSourceType = route.nodeSourceType,
                parentHandle = route.parentHandle,
                isFirstNavigationLevel = route.isFirstNavigationLevel
            )
            removeDestination()
        }
    }
}
