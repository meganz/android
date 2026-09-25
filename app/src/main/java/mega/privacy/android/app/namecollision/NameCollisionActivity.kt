package mega.privacy.android.app.namecollision

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.os.bundleOf
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.largebundle.largeBundleHolder
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.presentation.snackbar.LegacySnackBarWrapper
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.utils.AlertsAndWarnings.showForeignStorageOverQuotaWarningDialog
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_COLLISION_RESULTS
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_SINGLE_COLLISION_RESULT
import mega.privacy.android.core.sharedcomponents.extension.isDarkMode
import mega.privacy.android.core.sharedcomponents.parcelable
import mega.privacy.android.core.sharedcomponents.parcelableArrayList
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.shared.nodes.namecollision.NameCollisionArgs
import mega.privacy.android.shared.nodes.namecollision.NameCollisionOutcome
import mega.privacy.android.shared.nodes.namecollision.NameCollisionUiEntity
import mega.privacy.android.shared.nodes.namecollision.NameCollisionViewModel
import mega.privacy.android.shared.nodes.namecollision.toDomainEntity
import mega.privacy.android.shared.nodes.namecollision.toUiEntity
import mega.privacy.android.shared.nodes.namecollision.view.NameCollisionScreen
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity for showing name collisions and resolving them as per user's choices.
 *
 * The screen body is the shared Compose [NameCollisionScreen] backed by the shared
 * [NameCollisionViewModel]; the intent entry points and the result Intent are kept unchanged so the
 * existing Activity callers keep working without modification.
 */
@AndroidEntryPoint
class NameCollisionActivity : PasscodeActivity() {

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    private val isRefreshingSession by lazy(LazyThreadSafetyMode.NONE) {
        shouldRefreshSessionDueToSDK(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRefreshingSession) return
        enableEdgeToEdge()

        lifecycleScope.launch {
            val args = buildArgs()
            if (args == null) {
                Timber.e("No collisions received")
                finish()
                return@launch
            }
            setContent {
                val themeMode by monitorThemeModeUseCase()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val viewModel =
                    hiltViewModel<NameCollisionViewModel, NameCollisionViewModel.Factory> {
                        it.create(args)
                    }
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                AndroidTheme(isDark = themeMode.isDarkMode()) {
                    NameCollisionScreen(
                        uiState = uiState,
                        onReplaceUpdateMerge = viewModel::replaceUpdateOrMerge,
                        onCancel = viewModel::cancel,
                        onRename = viewModel::rename,
                        onLearnMore = { launchUrl(LEARN_MORE_URI) },
                        onBack = { onBackPressedDispatcher.onBackPressed() },
                    )
                }

                val snackbarHostState = remember { SnackbarHostState() }
                LegacySnackBarWrapper(
                    snackbarHostState = snackbarHostState,
                    context = this@NameCollisionActivity,
                )
                StartTransferComponent(
                    event = uiState.uploadEvent,
                    onConsumeEvent = {},
                    snackBarHostState = snackbarHostState,
                    onScanningFinished = { event ->
                        if (event is StartTransferEvent.FinishUploadProcessing) {
                            viewModel.consumeUploadEvent()
                        }
                    },
                )

                EventEffect(
                    event = uiState.outcome,
                    onConsumed = viewModel::consumeOutcome,
                ) { outcome ->
                    when (outcome) {
                        is NameCollisionOutcome.ActionCompleted -> {
                            val result = outcome.result
                            when {
                                result.isForeignNode ->
                                    showForeignStorageOverQuotaWarningDialog(this@NameCollisionActivity)

                                result.shouldFinish -> finishWithMessage(result.message)
                            }
                        }

                        is NameCollisionOutcome.Resolved ->
                            finishWithResolvedCollisions(outcome.collisions)

                        is NameCollisionOutcome.Failed -> {
                            val throwable = outcome.throwable
                            if (!manageCopyMoveException(throwable) && throwable is MegaException) {
                                showSnackbar(throwable.message.orEmpty())
                            }
                        }
                    }
                }

                EventEffect(
                    event = uiState.dismissEvent,
                    onConsumed = viewModel::consumeDismissEvent,
                ) {
                    finishWithMessage(uploadStartedMessage(uiState.uploadedFilesCount))
                }
            }
        }
    }

    private suspend fun buildArgs(): NameCollisionArgs? {
        val isFolderUploadContext = UPLOAD_FOLDER_CONTEXT == intent.action
        val singleCollision =
            intent.parcelable<NameCollisionUiEntity>(INTENT_EXTRA_SINGLE_COLLISION_RESULT)
        val collisionsList = intent.getStringExtra(EXTRA_COLLISIONS_KEY)
            ?.let { largeBundleHolder.get(it) }
            ?.parcelableArrayList<NameCollisionUiEntity>(INTENT_EXTRA_COLLISION_RESULTS)

        return when {
            collisionsList != null -> NameCollisionArgs.Multiple(
                collisions = collisionsList.map { it.toDomainEntity() },
                isFolderUploadContext = isFolderUploadContext,
            )

            singleCollision != null -> NameCollisionArgs.Single(
                collision = singleCollision.toDomainEntity(),
                isFolderUploadContext = isFolderUploadContext,
            )

            else -> null
        }
    }

    private fun uploadStartedMessage(uploadedFilesCount: Int): String? =
        if (uploadedFilesCount > 0) {
            getString(sharedR.string.transfers_upload_started_snackbar)
        } else {
            null
        }

    private fun finishWithMessage(message: String?) {
        setResult(RESULT_OK, Intent().putExtra(MESSAGE_RESULT, message))
        finish()
    }

    private fun finishWithResolvedCollisions(collisions: List<NodeNameCollisionResult>) {
        val key = largeBundleHolder.put(
            bundleOf(
                INTENT_EXTRA_COLLISION_RESULTS to collisions
                    .map { it.toUiEntity() }
                    .toCollection(ArrayList())
            )
        )
        setResult(RESULT_OK, Intent().putExtra(EXTRA_COLLISIONS_KEY, key))
        finish()
    }

    override fun onDestroy() {
        if (isFinishing && !isRefreshingSession) {
            intent.getStringExtra(EXTRA_COLLISIONS_KEY)?.let { largeBundleHolder.release(it) }
        }
        super.onDestroy()
    }

    companion object {
        private const val LEARN_MORE_URI =
            "https://help.mega.io/files-folders/restore-delete/file-version-history"

        private const val UPLOAD_FOLDER_CONTEXT = "UPLOAD_FOLDER_CONTEXT"
        const val MESSAGE_RESULT = "MESSAGE_RESULT"

        /** Intent extra carrying the [largeBundleHolder] key for the collisions payload. */
        const val EXTRA_COLLISIONS_KEY = "EXTRA_COLLISIONS_KEY"

        @JvmStatic
        fun getIntentForList(
            context: Context,
            collisions: ArrayList<NameCollision>,
        ): Intent {
            val key = context.largeBundleHolder.put(
                bundleOf(
                    INTENT_EXTRA_COLLISION_RESULTS to collisions
                        .map { it.toUiEntity() }
                        .toCollection(ArrayList())
                )
            )
            return Intent(context, NameCollisionActivity::class.java).apply {
                putExtra(EXTRA_COLLISIONS_KEY, key)
            }
        }

        @JvmStatic
        fun getIntentForFolderUpload(
            context: Context,
            collisions: ArrayList<NameCollision>,
        ): Intent = getIntentForList(
            context,
            collisions
        ).apply { action = UPLOAD_FOLDER_CONTEXT }

        @JvmStatic
        fun getIntentForSingleItem(
            context: Context,
            collision: NameCollision,
        ): Intent = Intent(context, NameCollisionActivity::class.java).apply {
            putExtra(INTENT_EXTRA_SINGLE_COLLISION_RESULT, collision.toUiEntity())
        }
    }
}
