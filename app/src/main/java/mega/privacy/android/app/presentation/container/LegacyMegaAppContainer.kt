package mega.privacy.android.app.presentation.container

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.main.dialog.businessgrace.BusinessAccountContainer
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.psa.PsaContentView
import mega.privacy.android.app.presentation.psa.model.PsaState
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

@Composable
internal fun LegacyMegaAppContainer(
    context: Context,
    psaState: PsaState,
    markPsaAsSeen: (Int) -> Unit,
    themeMode: ThemeMode,
    passcodeCryptObjectFactory: PasscodeCryptObjectFactory,
    canLock: () -> Boolean,
) {
    val containers: List<(@Composable (@Composable () -> Unit) -> Unit)?> = listOf(
        {
            BusinessAccountContainer(content = it)
        },
        {
            PsaContentView(
                context = context,
                coroutineScope = rememberCoroutineScope(),
                markAsSeen = markPsaAsSeen,
                state = psaState,
                content = it,
                containerModifier = Modifier
                    .navigationBarsPadding(),
                innerModifier = { it.padding(bottom = 16.dp) }
            )
        },
        {
            PasscodeContainer(
                passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                canLock = canLock,
                content = it,
            )
        },
        { OriginalTheme(isDark = themeMode.isDarkMode(), content = it) },
    )

    AppContainer(
        containers = containers.filterNotNull(),
        content = { Box(Modifier.fillMaxSize()) }
    )
}