package mega.privacy.android.app.presentation.contact

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import mega.privacy.android.app.appstate.content.navigation.LegacyActivityScaffold
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.feature.contact.group.create.navigation.NewChatEntry
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.destination.NewChatNavKey
import javax.inject.Inject

/**
 * A thin Compose host that lets a legacy Activity (the share/forward target pickers) open the Compose
 * "new chat" multi-select picker ([NewChatEntry]) for a result. It renders the picker via
 * [LegacyActivityScaffold] under its own key, because the shared [NewChatNavKey] destination shows
 * the chat list's start-conversation screen. It bridges the published [NewChatNavKey.NewChatResult]
 * back to the caller as an Activity result, mirroring the legacy [AddContactActivity] new-chat contract: always [AddContactActivity.EXTRA_CONTACTS]
 * (the selected emails), plus — only for a group (two or more selected) —
 * [AddContactActivity.EXTRA_CHAT_TITLE], [AddContactActivity.EXTRA_EKR], [AddContactActivity.EXTRA_CHAT_LINK]
 * and [AddContactActivity.ALLOW_ADD_PARTICIPANTS]. The caller branches on the selected-contact count, so
 * existing new-chat result handling is unchanged.
 *
 * Launch it only when `ContactComposeFeature` is enabled; the caller keeps launching [AddContactActivity]
 * when the flag is off.
 */
@AndroidEntryPoint
class NewChatComposeActivity : AppCompatActivity() {

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var featureDestinations: Set<@JvmSuppressWildcards FeatureDestination>

    @Inject
    lateinit var navigationResultManager: NavigationResultManager

    @Inject
    lateinit var appDialogDestinations: Set<@JvmSuppressWildcards AppDialogDestinations>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navigationResultManager.clearResult(NewChatNavKey.KEY)
        collectResult()

        enableEdgeToEdge()
        setContent {
            val themeMode by monitorThemeModeUseCase()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            LegacyActivityScaffold(
                container = { content ->
                    MegaAppContainer(
                        themeMode = themeMode,
                        finishOnSessionRefresh = false,
                        content = content,
                    )
                },
                initialKey = InternalNewChatNavKey,
                navigationResultManager = navigationResultManager,
                featureDestinations = featureDestinations,
                appDialogDestinations = appDialogDestinations,
                onEmptyBackStack = { if (!isFinishing) finish() },
            ) { navigationHandler, _ ->
                entry<InternalNewChatNavKey> {
                    NewChatEntry(navigationHandler = navigationHandler)
                }
            }
        }
    }

    private fun collectResult() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigationResultManager
                    .monitorResult<NewChatNavKey.NewChatResult>(NewChatNavKey.KEY)
                    .filterNotNull()
                    .collect { result ->
                        navigationResultManager.clearResult(NewChatNavKey.KEY)
                        val data = Intent()
                            .putStringArrayListExtra(
                                AddContactActivity.EXTRA_CONTACTS,
                                ArrayList(result.emails),
                            )
                        result.groupSettings?.let { settings ->
                            data.putExtra(AddContactActivity.EXTRA_CHAT_TITLE, settings.title)
                                .putExtra(AddContactActivity.EXTRA_EKR, settings.isEkr)
                                .putExtra(AddContactActivity.EXTRA_CHAT_LINK, settings.isChatLink)
                                .putExtra(
                                    AddContactActivity.ALLOW_ADD_PARTICIPANTS,
                                    settings.allowAddParticipants,
                                )
                                .putExtra(
                                    AddContactActivity.EXTRA_GROUP_CHAT_IMAGE,
                                    settings.imageUri?.value,
                                )
                        }
                        setResult(RESULT_OK, data)
                        finish()
                    }
            }
        }
    }

    companion object {
        /**
         * Builds an [Intent] to open the "new chat" flow.
         */
        fun getIntent(context: Context): Intent =
            Intent(context, NewChatComposeActivity::class.java)
    }
}

@Serializable
private data object InternalNewChatNavKey : NavKey
