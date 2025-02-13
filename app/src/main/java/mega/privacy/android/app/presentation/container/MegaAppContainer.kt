package mega.privacy.android.app.presentation.container

import androidx.compose.runtime.Composable
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme


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
        { PsaContainer(content = it) },
        {
            PasscodeContainer(
                passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                content = it
            )
        },
        { OriginalTheme(isDark = themeMode.isDarkMode(), content = it) },
        { SessionContainer(content = it) },
    )

    AppContainer(
        containers = containers,
        content = content
    )
}