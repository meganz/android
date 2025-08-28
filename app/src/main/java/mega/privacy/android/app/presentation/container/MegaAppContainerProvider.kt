package mega.privacy.android.app.presentation.container

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.core.sharedcomponents.container.AppContainerProvider
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import javax.inject.Inject

/**
 * Implementation of AppContainerProvider that wraps the internal MegaAppContainer
 * and SharedAppContainer functions, allowing other modules to access them
 * without exposing internal implementation details.
 */
class MegaAppContainerProvider @Inject constructor(
    private val passcodeCryptObjectFactory: PasscodeCryptObjectFactory,
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase,
) : AppContainerProvider {
    override fun buildAppContainer(
        context: Context,
        content: @Composable (() -> Unit),
    ): ComposeView =
        ComposeView(context).apply {
            setContent {
                val mode by monitorThemeModeUseCase()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                MegaAppContainer(
                    themeMode = mode,
                    passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                    content = content
                )
            }
        }

    override fun buildSharedAppContainer(
        context: Context,
        useLegacyStatusBarColor: Boolean,
        content: @Composable (() -> Unit),
    ): ComposeView =
        ComposeView(context).apply {
            setContent {
                val mode by monitorThemeModeUseCase()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                SharedAppContainer(
                    themeMode = mode,
                    useLegacyStatusBarColor = useLegacyStatusBarColor,
                    passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                    content = content
                )
            }
        }
}
