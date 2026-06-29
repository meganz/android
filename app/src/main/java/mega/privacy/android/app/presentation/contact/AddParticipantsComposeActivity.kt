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
import androidx.navigation3.runtime.NavKey
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
import mega.privacy.android.navigation.destination.AddChatParticipantsNavKey
import mega.privacy.android.navigation.destination.AddMeetingParticipantsNavKey
import javax.inject.Inject

/**
 * A thin Compose host that lets a legacy Activity (e.g. the meeting screen) open the Compose
 * contacts picker for a result. It renders the participants picker destination via
 * [LegacyActivityScaffold] and bridges the picker's published emails back to the caller as an
 * Activity result, mirroring the legacy [AddContactActivity] contract
 * (`RESULT_OK` + [AddContactActivity.EXTRA_CONTACTS]) so existing result handling is unchanged.
 *
 * Launch it only when `ContactsComposeUI` is enabled; the caller keeps launching
 * [AddContactActivity] when the flag is off.
 */
@AndroidEntryPoint
class AddParticipantsComposeActivity : AppCompatActivity() {

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

        val chatId = intent.getLongExtra(EXTRA_CHAT_ID, -1L)
        val isMeeting = intent.getBooleanExtra(EXTRA_IS_MEETING, false)
        val initialKey: NavKey
        val resultKey: String
        if (isMeeting) {
            initialKey = AddMeetingParticipantsNavKey(chatId)
            resultKey = AddMeetingParticipantsNavKey.KEY
        } else {
            initialKey = AddChatParticipantsNavKey(chatId)
            resultKey = AddChatParticipantsNavKey.KEY
        }

        navigationResultManager.clearResult(resultKey)
        collectResult(resultKey)

        enableEdgeToEdge()
        setContent {
            val themeMode by monitorThemeModeUseCase()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val startKey = remember { initialKey }
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

    private fun collectResult(resultKey: String) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigationResultManager.monitorResult<List<String>>(resultKey)
                    .filterNotNull()
                    .collect { emails ->
                        navigationResultManager.clearResult(resultKey)
                        val data = Intent().putStringArrayListExtra(
                            AddContactActivity.EXTRA_CONTACTS,
                            ArrayList(emails),
                        )
                        setResult(RESULT_OK, data)
                        finish()
                    }
            }
        }
    }

    companion object {
        private const val EXTRA_CHAT_ID = "extra_chat_id"
        private const val EXTRA_IS_MEETING = "extra_is_meeting"

        /**
         * Builds an [Intent] to open the picker for adding participants to a meeting [chatId].
         */
        fun getMeetingIntent(context: Context, chatId: Long): Intent =
            Intent(context, AddParticipantsComposeActivity::class.java)
                .putExtra(EXTRA_CHAT_ID, chatId)
                .putExtra(EXTRA_IS_MEETING, true)
    }
}
