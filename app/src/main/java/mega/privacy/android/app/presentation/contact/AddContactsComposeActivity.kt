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
import mega.privacy.android.navigation.destination.AddContactsNavKey
import javax.inject.Inject

/**
 * A thin Compose host that lets a legacy Activity (e.g. the create-scheduled-meeting screen) open
 * the Compose MEGA-contacts picker for a result. It renders the [AddContactsNavKey] destination via
 * [LegacyActivityScaffold] with the already-chosen participants pre-selected, and bridges the
 * picker's published emails back to the caller as an Activity result, mirroring the legacy
 * [AddContactActivity] contract (`RESULT_OK` + [AddContactActivity.EXTRA_CONTACTS]) so existing
 * result handling is unchanged.
 *
 * Launch it only when `ContactsComposeUI` is enabled; the caller keeps launching
 * [AddContactActivity] when the flag is off.
 */
@AndroidEntryPoint
class AddContactsComposeActivity : AppCompatActivity() {

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

        val preselectedHandles = intent.getLongArrayExtra(EXTRA_PRESELECTED_HANDLES)?.toList().orEmpty()

        navigationResultManager.clearResult(AddContactsNavKey.KEY)
        collectResult()

        enableEdgeToEdge()
        setContent {
            val themeMode by monitorThemeModeUseCase()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val startKey = remember {
                AddContactsNavKey(preselectedHandles = preselectedHandles)
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
                navigationResultManager.monitorResult<List<String>>(AddContactsNavKey.KEY)
                    .filterNotNull()
                    .collect { emails ->
                        navigationResultManager.clearResult(AddContactsNavKey.KEY)
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
        private const val EXTRA_PRESELECTED_HANDLES = "extra_preselected_handles"

        /**
         * Builds an [Intent] to open the MEGA-contacts picker with [preselectedHandles] pre-selected.
         */
        fun getIntent(context: Context, preselectedHandles: List<Long>): Intent =
            Intent(context, AddContactsComposeActivity::class.java)
                .putExtra(EXTRA_PRESELECTED_HANDLES, preselectedHandles.toLongArray())
    }
}
