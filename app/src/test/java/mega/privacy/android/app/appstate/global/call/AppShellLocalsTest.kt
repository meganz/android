package mega.privacy.android.app.appstate.global.call

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.flowOf
import mega.android.core.ui.components.LocalMegaScaffoldTopBanner
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.chat.MonitorOngoingCallUseCase
import mega.privacy.android.feature.chat.components.RETURN_TO_CALL_BANNER_TAG
import mega.privacy.android.navigation.contract.state.LocalMiniAudioPlayer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Guards the app-shell locals every host provides through [ProvideAppShellLocals], so a host that
 * wraps its destinations in it cannot leave them on the no-op defaults.
 */
@RunWith(AndroidJUnit4::class)
class AppShellLocalsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that content sees the provided top banner and mini audio player`() {
        composeRule.setContent {
            ProvideAppShellLocals(
                topBanner = { Box(Modifier.testTag(BANNER_TAG)) },
                miniAudioPlayer = { modifier, _ -> Box(modifier.testTag(MINI_PLAYER_TAG)) },
            ) {
                LocalMegaScaffoldTopBanner.current?.invoke()
                LocalMiniAudioPlayer.current(Modifier, false)
            }
        }

        composeRule.onNodeWithTag(BANNER_TAG).assertExists()
        composeRule.onNodeWithTag(MINI_PLAYER_TAG).assertExists()
    }

    @Test
    fun `test that content scaffold displays the return to call banner when a call is in progress`() {
        val monitorOngoingCallUseCase = mock<MonitorOngoingCallUseCase> {
            on { invoke() }.thenReturn(flowOf(ChatCall(chatId = 1L, callId = 2L)))
        }
        val monitorThemeModeUseCase = mock<MonitorThemeModeUseCase>()
        whenever(monitorThemeModeUseCase()).thenReturn(flowOf(ThemeMode.System))
        val viewModel = OngoingCallBannerViewModel(
            monitorOngoingCallUseCase = monitorOngoingCallUseCase,
            monitorThemeModeUseCase = monitorThemeModeUseCase,
        )
        val viewModelStoreOwner = object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore().apply {
                put(
                    "androidx.lifecycle.ViewModelProvider.DefaultKey:" +
                            OngoingCallBannerViewModel::class.java.canonicalName,
                    viewModel,
                )
            }
        }

        composeRule.setContent {
            AndroidThemeForPreviews {
                CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                    ProvideAppShellLocals {
                        MegaScaffold(
                            topBar = {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                )
                            },
                            contentWindowInsets = WindowInsets(0.dp),
                        ) {
                            Box(Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithTag(RETURN_TO_CALL_BANNER_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    private companion object {
        const val BANNER_TAG = "app_shell_locals_test:banner"
        const val MINI_PLAYER_TAG = "app_shell_locals_test:mini_player"
    }
}
