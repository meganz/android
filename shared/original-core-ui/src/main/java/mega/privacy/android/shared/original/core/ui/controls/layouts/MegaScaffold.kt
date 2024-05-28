package mega.privacy.android.shared.original.core.ui.controls.layouts

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import mega.privacy.android.shared.original.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme

/**
 * MegaScaffold is a wrapper around Scaffold that provides a convenient way to show a snackBar from any view inside this scaffold.
 * It also provides the appropriate look and feel,such as background color or snack bar style.
 *
 * @param modifier Modifier
 * @param scaffoldState ScaffoldState
 * @param topBar TopBar
 * @param bottomBar BottomBar
 * @param floatingActionButton FloatingActionButton
 * @param content Content
 */
@Composable
fun MegaScaffold(
    modifier: Modifier = Modifier,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    CompositionLocalProvider(LocalSnackBarHostState provides scaffoldState.snackbarHostState) {
        Scaffold(
            modifier = modifier,
            scaffoldState = scaffoldState,
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = {
                SnackbarHost(hostState = it) { data ->
                    MegaSnackbar(snackbarData = data)
                }
            },
            floatingActionButton = floatingActionButton,
            backgroundColor = MegaOriginalTheme.colors.background.pageBackground,
            content = content
        )
    }
}

/**
 * Provides SnackbarHostState to be used to show a snackBar from any view inside this scaffold.
 * This is a convenient accessor to [ScaffoldState.snackbarHostState] without the need to send it to all the view hierarchy
 */
val LocalSnackBarHostState = compositionLocalOf<SnackbarHostState?> { null }

