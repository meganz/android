package mega.privacy.android.app.presentation.meeting.chat

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.util.Consumer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.appstate.content.navigation.LegacyActivityScaffold
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_LINK
import mega.privacy.android.app.presentation.meeting.chat.navigation.ChatLegacyContainerNavKey
import mega.privacy.android.app.presentation.meeting.chat.navigation.ChatTabsContainerNavKey
import mega.privacy.android.app.presentation.meeting.chat.navigation.chatDestination
import mega.privacy.android.app.presentation.meeting.chat.navigation.chatTabsDestination
import mega.privacy.android.app.presentation.meeting.chat.navigation.openChatConversation
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.destination.ChatNavKey
import timber.log.Timber
import javax.inject.Inject

/**
 * Host Activity for new chat room.
 *
 * Hosts a [LegacyActivityScaffold] that registers the chat tabs and chat conversation entries.
 * The chat conversation keeps using its existing legacy (Navigation 2) graph inside a nested
 * `NavHost`, so only the host has been migrated to Navigation 3 (fragments removed).
 */
@AndroidEntryPoint
class ChatActivity : AppCompatActivity() {

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    /**
     * The centralized navigator in the :app module
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    @Inject
    lateinit var featureDestinations: Set<@JvmSuppressWildcards FeatureDestination>

    @Inject
    lateinit var navigationResultManager: NavigationResultManager

    @Inject
    lateinit var appDialogDestinations: Set<@JvmSuppressWildcards AppDialogDestinations>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Timber.d("ChatActivity.onCreate: intent.action=${intent.action}")
        val initialKey = intent.toChatNavKey()

        setContent {
            val mode by monitorThemeModeUseCase()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            // The scaffold owns the back stack and only exposes its NavigationHandler inside the
            // (non-composable) entry builder. Bridge it through a composition-scoped holder so
            // onNewIntent can drive navigation without leaking the handler into an Activity field.

            LegacyActivityScaffold(
                container = { content ->
                    MegaAppContainer(
                        themeMode = mode,
                        content = content,
                        finishOnSessionRefresh = false,
                    )
                },
                initialKey = initialKey,
                navigationResultManager = navigationResultManager,
                featureDestinations = featureDestinations,
                appDialogDestinations = appDialogDestinations,
                onEmptyBackStack = { if (!isFinishing) finish() },
                overlayContent = { navigationHandler ->
                    DisposableEffect(Unit) {
                        val listener = Consumer<Intent> { newIntent ->
                            when (val key = newIntent.toChatNavKey()) {
                                is ChatLegacyContainerNavKey ->
                                    navigationHandler.openChatConversation(key)

                                else -> navigationHandler.navigate(key)
                            }
                        }
                        addOnNewIntentListener(listener)
                        onDispose { removeOnNewIntentListener(listener) }
                    }
                }
            ) { navigationHandler, _ ->
                chatTabsDestination(navigationHandler = navigationHandler)
                chatDestination(
                    navigationHandler = navigationHandler,
                    megaNavigator = megaNavigator,
                )
            }
        }
    }

    private fun Intent.toChatNavKey(): NavKey =
        if (getBooleanExtra(OPEN_CHAT_LIST, false)) {
            ChatTabsContainerNavKey(
                showMeetingTab = getBooleanExtra(EXTRA_SHOW_MEETING_TAB, false),
                createNewChat = getBooleanExtra(CREATE_NEW_CHAT, false),
            )
        } else {
            ChatLegacyContainerNavKey(
                chatId = getLongExtra(ChatNavKey.LEGACY_CHAT_ID, -1L),
                action = getStringExtra(EXTRA_ACTION) ?: Constants.ACTION_CHAT_SHOW_MESSAGES,
                link = getStringExtra(EXTRA_LINK),
            )
        }

    companion object {
        const val OPEN_CHAT_LIST = "open_chat_list"
        const val CREATE_NEW_CHAT = "create_new_chat"
        const val EXTRA_SHOW_MEETING_TAB = "EXTRA_SHOW_MEETING_TAB"
    }
}
