package mega.privacy.android.app.presentation.contact

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import mega.privacy.android.app.appstate.content.navigation.LegacyActivityScaffold
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.destination.CreateGroupChatNavKey
import javax.inject.Inject

/**
 * A thin Compose host that lets a legacy Activity open the Compose create-group-chat flow for a
 * result. It renders the [CreateGroupChatNavKey] destination via [LegacyActivityScaffold] and
 * bridges the published [CreateGroupChatNavKey.NewGroupChatResult] back to the caller as an Activity
 * result, mirroring the legacy [AddContactActivity] "only create group" contract — the selected
 * emails plus [AddContactActivity.EXTRA_CHAT_TITLE], [AddContactActivity.EXTRA_EKR],
 * [AddContactActivity.EXTRA_CHAT_LINK], [AddContactActivity.ALLOW_ADD_PARTICIPANTS] and the
 * [AddContactActivity.EXTRA_GROUP_CHAT] / [AddContactActivity.EXTRA_ONLY_CREATE_GROUP] flags — so
 * existing group-chat result handling is unchanged.
 *
 * Launch it only when `ContactsComposeUI` is enabled; the caller keeps launching
 * [AddContactActivity] when the flag is off.
 */
@AndroidEntryPoint
class CreateGroupChatComposeActivity : AppCompatActivity() {

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

        navigationResultManager.clearResult(CreateGroupChatNavKey.KEY)
        collectResult()

        enableEdgeToEdge()
        setContent {
            val themeMode by monitorThemeModeUseCase()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val startKey = remember {
                CreateGroupChatNavKey(
                    allowEmptyGroup = intent.getBooleanExtra(EXTRA_ALLOW_EMPTY_GROUP, false),
                )
            }
            LegacyActivityScaffold(
                container = { content ->
                    MegaAppContainer(
                        themeMode = themeMode,
                        finishOnSessionRefresh = false,
                        content = content,
                    )
                },
                initialKey = startKey,
                navigationResultManager = navigationResultManager,
                featureDestinations = featureDestinations,
                appDialogDestinations = appDialogDestinations,
                onEmptyBackStack = { if (!isFinishing) finish() },
            ) { _, _ -> }
        }
    }

    private fun collectResult() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigationResultManager
                    .monitorResult<CreateGroupChatNavKey.NewGroupChatResult>(CreateGroupChatNavKey.KEY)
                    .filterNotNull()
                    .collect { result ->
                        navigationResultManager.clearResult(CreateGroupChatNavKey.KEY)
                        val data = Intent()
                            .putStringArrayListExtra(
                                AddContactActivity.EXTRA_CONTACTS,
                                ArrayList(result.emails),
                            )
                            .putExtra(AddContactActivity.EXTRA_CHAT_TITLE, result.title)
                            .putExtra(AddContactActivity.EXTRA_EKR, result.isEkr)
                            .putExtra(AddContactActivity.EXTRA_CHAT_LINK, result.isChatLink)
                            .putExtra(
                                AddContactActivity.ALLOW_ADD_PARTICIPANTS,
                                result.allowAddParticipants,
                            )
                            .putExtra(AddContactActivity.EXTRA_GROUP_CHAT, true)
                            .putExtra(AddContactActivity.EXTRA_ONLY_CREATE_GROUP, true)
                        setResult(RESULT_OK, data)
                        finish()
                    }
            }
        }
    }

    companion object {
        private const val EXTRA_ALLOW_EMPTY_GROUP = "extra_allow_empty_group"

        /**
         * Builds an [Intent] to open the create-group-chat flow.
         *
         * @param allowEmptyGroup when true the group may be created with no other participants.
         */
        fun getIntent(context: Context, allowEmptyGroup: Boolean = false): Intent =
            Intent(context, CreateGroupChatComposeActivity::class.java)
                .putExtra(EXTRA_ALLOW_EMPTY_GROUP, allowEmptyGroup)
    }
}
