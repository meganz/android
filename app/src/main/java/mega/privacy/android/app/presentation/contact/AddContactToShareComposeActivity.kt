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
import mega.privacy.android.navigation.destination.AddContactToShareNavKey
import javax.inject.Inject

/**
 * A thin Compose host that lets a legacy Activity (e.g. the share flow) open the Compose
 * add-contacts-to-share picker for a result. It renders the [AddContactToShareNavKey] destination
 * via [LegacyActivityScaffold] and bridges the picker's published emails back to the caller as an
 * Activity result, mirroring the legacy [AddContactActivity] contract — [AddContactActivity.EXTRA_CONTACTS]
 * plus the [AddContactActivity.EXTRA_NODE_HANDLE] and `MULTISELECT` extras it was launched with — so
 * existing share-folder result handling is unchanged.
 *
 * Launch it only when `ContactsComposeUI` is enabled; the caller keeps launching
 * [AddContactActivity] when the flag is off.
 */
@AndroidEntryPoint
class AddContactToShareComposeActivity : AppCompatActivity() {

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

        val contactType = AddContactToShareNavKey.ContactType.entries[
            intent.getIntExtra(EXTRA_CONTACT_TYPE, AddContactToShareNavKey.ContactType.All.ordinal)
        ]
        val nodeHandles = intent.getLongArrayExtra(EXTRA_NODE_HANDLES)?.toList().orEmpty()

        navigationResultManager.clearResult(AddContactToShareNavKey.KEY)
        collectResult(nodeHandles)

        enableEdgeToEdge()
        setContent {
            val themeMode by monitorThemeModeUseCase()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val startKey = remember {
                AddContactToShareNavKey(contactType = contactType, nodeHandle = nodeHandles)
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

    private fun collectResult(nodeHandles: List<Long>) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigationResultManager.monitorResult<List<String>>(AddContactToShareNavKey.KEY)
                    .filterNotNull()
                    .collect { emails ->
                        navigationResultManager.clearResult(AddContactToShareNavKey.KEY)
                        val data = Intent().putStringArrayListExtra(
                            AddContactActivity.EXTRA_CONTACTS,
                            ArrayList(emails),
                        )
                        if (nodeHandles.size == 1) {
                            data.putExtra(AddContactActivity.EXTRA_NODE_HANDLE, nodeHandles.first())
                            data.putExtra(AddContactActivity.EXTRA_MULTISELECT, 0)
                        } else if (nodeHandles.size > 1) {
                            data.putExtra(
                                AddContactActivity.EXTRA_NODE_HANDLE,
                                nodeHandles.toLongArray(),
                            )
                            data.putExtra(AddContactActivity.EXTRA_MULTISELECT, 1)
                        }
                        setResult(RESULT_OK, data)
                        finish()
                    }
            }
        }
    }

    companion object {
        private const val EXTRA_CONTACT_TYPE = "extra_contact_type"
        private const val EXTRA_NODE_HANDLES = "extra_node_handles"

        /**
         * Builds an [Intent] to open the picker for adding contacts to a shared folder.
         *
         * @param contactType the contact source to surface in the picker.
         * @param nodeHandles the handle(s) of the folder(s) being shared.
         */
        fun getIntent(
            context: Context,
            contactType: AddContactToShareNavKey.ContactType,
            nodeHandles: List<Long>,
        ): Intent =
            Intent(context, AddContactToShareComposeActivity::class.java)
                .putExtra(EXTRA_CONTACT_TYPE, contactType.ordinal)
                .putExtra(EXTRA_NODE_HANDLES, nodeHandles.toLongArray())
    }
}
