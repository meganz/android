package mega.privacy.android.app.presentation.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.main.dialog.businessgrace.BusinessAccountContainer
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.psa.MegaPsaContainer
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

val LocalIsDarkTheme = staticCompositionLocalOf { false }

/**
 * Mega app container
 *
 * @param themeMode
 * @param passcodeCryptObjectFactory
 * @param content
 *
 * *** IMPORTANT PLEASE NOTE ***
 * Containers are applied from the last to first.  So each container is wrapped in the one after it.
 */
@Composable
internal fun MegaAppContainer(
    themeMode: ThemeMode,
    passcodeCryptObjectFactory: PasscodeCryptObjectFactory,
    content: @Composable () -> Unit,
) {
    val containers: List<@Composable (@Composable () -> Unit) -> Unit> = listOf(
        { BusinessAccountContainer(content = it) },
        { PsaContainer(content = it) },
        {
            PasscodeContainer(
                passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                content = it
            )
        },
        {
            val darkMode = themeMode.isDarkMode()
            CompositionLocalProvider(
                LocalIsDarkTheme provides darkMode
            ) {
                OriginalTheme(isDark = darkMode, content = it)
            }
        },
        { SessionContainer(content = it) },
    )

    AppContainer(
        containers = containers,
        content = content
    )
}

/**
 * Shared app container
 * Implements the same functionality as [MegaAppContainer] using the updated ui components and theme
 *
 * @param themeMode
 * @param passcodeCryptObjectFactory
 * @param content
 *
 */
@Composable
internal fun SharedAppContainer(
    themeMode: ThemeMode,
    passcodeCryptObjectFactory: PasscodeCryptObjectFactory,
    content: @Composable () -> Unit,
) {
    val containers: List<@Composable (@Composable () -> Unit) -> Unit> = listOf(
        { MegaPsaContainer(content = it) },
        {
            PasscodeContainer(
                passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                content = it
            )
        },
        {
            val darkMode = themeMode.isDarkMode()
            CompositionLocalProvider(
                LocalIsDarkTheme provides darkMode
            ) {
                AndroidTheme(isDark = darkMode, content = it)
            }
        },
        { SessionContainer(content = it) },
    )

    AppContainer(
        containers = containers,
        content = content
    )
}