package mega.privacy.android.app.presentation.transfers.preview

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import mega.privacy.android.app.appstate.content.navigation.LegacyActivityScaffold
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.presentation.container.SharedAppContainer
import mega.privacy.android.app.presentation.transfers.preview.model.LoadingPreviewViewModel
import mega.privacy.android.app.presentation.transfers.preview.view.LoadingPreviewNavKey
import mega.privacy.android.app.presentation.transfers.preview.view.loadingPreviewEntry
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import javax.inject.Inject

/**
 * Activity to show a loading preview of a file being downloaded. Only for preview purposes.
 */
@AndroidEntryPoint
class LoadingPreviewActivity : AppCompatActivity() {

    /**
     * Use case to monitor the theme mode
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    /**
     * Navigation result manager
     */
    @Inject
    lateinit var navigationResultManager: NavigationResultManager

    /**
     * Feature destinations
     */
    @Inject
    lateinit var featureDestinations: Set<@JvmSuppressWildcards FeatureDestination>

    /**
     * App dialog destinations
     */
    @Inject
    lateinit var appDialogDestinations: Set<@JvmSuppressWildcards AppDialogDestinations>

    private val viewModel: LoadingPreviewViewModel by viewModels(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<LoadingPreviewViewModel.Factory> { factory ->
                val args = LoadingPreviewViewModel.Args(
                    transferPath = intent?.getStringExtra(EXTRA_FILE_PATH)
                        .takeUnless { it.isNullOrEmpty() },
                    transferUniqueId = intent?.getLongExtra(EXTRA_TRANSFER_UNIQUE_ID, -1)
                        .takeUnless { it == -1L },
                    transferTag = intent?.getIntExtra(EXTRA_TRANSFER_TAG, -1)
                        .takeUnless { it == -1 },
                )
                factory.create(args)
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val mode by monitorThemeModeUseCase()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            LegacyActivityScaffold(
                container = { content ->
                    // A preview download can be started from a link, so no session is required
                    SharedAppContainer(
                        themeMode = mode,
                        isSessionRequired = false,
                        finishOnSessionRefresh = false,
                        content = content,
                    )
                },
                initialKey = LoadingPreviewNavKey,
                navigationResultManager = navigationResultManager,
                featureDestinations = featureDestinations,
                appDialogDestinations = appDialogDestinations,
                onEmptyBackStack = { if (!isFinishing) finish() },
            ) { _, _ ->
                loadingPreviewEntry(
                    viewModel = viewModel,
                    themeMode = mode,
                    onBackPress = { supportFinishAfterTransition() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        intent.getIntExtra(EXTRA_TRANSFER_TAG, -1).takeIf { it != -1 }?.let { tag ->
            viewModel.onNewIntent(tag)
        }
    }

    companion object {
        /**
         * Intent extra for transfer unique ID
         */
        const val EXTRA_TRANSFER_UNIQUE_ID = "TRANSFER_UNIQUE_ID"

        /**
         * Intent extra for file path
         */
        const val EXTRA_FILE_PATH = "FILE_PATH"

        /**
         * Intent extra for error
         */
        const val EXTRA_ERROR = "ERROR"

        /**
         * Intent extra for transfer tag
         */
        const val EXTRA_TRANSFER_TAG = "TRANSFER_TAG"
    }
}
