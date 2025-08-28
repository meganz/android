package mega.privacy.android.core.sharedcomponents.container

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView

/**
 * Provider interface for app-wide containers that manage global app state,
 * theming, session management, and other cross-cutting concerns.
 */
interface AppContainerProvider {

    /**
     * Creates the main app container with all necessary wrappers including
     * session management, theming, passcode, PSA, and business account handling.
     *
     * @param content The content to wrap with the container
     */
    fun buildAppContainer(
        context: Context,
        content: @Composable () -> Unit,
    ): ComposeView

    /**
     * Creates the shared app container using updated UI components and theme.
     * This is similar to [AppContainer] but uses the newer AndroidTheme.
     *
     * @param useLegacyStatusBarColor Whether to use legacy status bar color
     * @param content The content to wrap with the container
     */
    fun buildSharedAppContainer(
        context: Context,
        useLegacyStatusBarColor: Boolean = true,
        content: @Composable () -> Unit,
    ): ComposeView
}