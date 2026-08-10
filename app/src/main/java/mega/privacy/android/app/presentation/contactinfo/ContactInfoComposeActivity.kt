package mega.privacy.android.app.presentation.contactinfo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.appstate.content.navigation.LegacyActivityScaffold
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.destination.ContactInfoNavKey
import nz.mega.sdk.MegaChatApiJava
import javax.inject.Inject

/**
 * A thin Compose host that lets a legacy Activity (e.g. the chat room) open the Compose contact
 * info screen on its own task, so Back returns to the caller rather than the single-activity Menu
 * root. It renders the [ContactInfoNavKey] destination via [LegacyActivityScaffold].
 *
 * Launch it only when `ContactInfoComposeUI` is enabled; the caller keeps launching the legacy
 * [ContactInfoActivity] when the flag is off.
 */
@AndroidEntryPoint
class ContactInfoComposeActivity : AppCompatActivity() {

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

        val email = intent.getStringExtra(Constants.NAME)
        val chatId = intent.getLongExtra(Constants.HANDLE, MegaChatApiJava.MEGACHAT_INVALID_HANDLE)
        val initialKey = ContactInfoNavKey(
            email = email,
            chatId = chatId.takeIf { it != MegaChatApiJava.MEGACHAT_INVALID_HANDLE },
        )

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

    companion object {
        /**
         * Builds an [Intent] to open the Compose contact info screen for a contact [email].
         */
        fun getIntent(context: Context, email: String): Intent =
            Intent(context, ContactInfoComposeActivity::class.java)
                .apply { putExtra(Constants.NAME, email) }

        /**
         * Builds an [Intent] to open the Compose contact info screen for a 1:1 [chatId].
         */
        fun getIntent(context: Context, chatId: Long): Intent =
            Intent(context, ContactInfoComposeActivity::class.java)
                .apply { putExtra(Constants.HANDLE, chatId) }
    }
}
