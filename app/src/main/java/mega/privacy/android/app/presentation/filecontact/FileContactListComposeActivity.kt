package mega.privacy.android.app.presentation.filecontact

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.scene.rememberSceneSetupNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.appstate.content.navigation.NavigationHandlerImpl
import mega.privacy.android.app.appstate.transfer.AppTransferViewModel
import mega.privacy.android.app.appstate.transfer.TransferHandlerImpl
import mega.privacy.android.app.presentation.container.SharedAppContainer
import mega.privacy.android.app.presentation.filecontact.navigation.FileContactFeatureDestination
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.destination.FileContactInfoNavKey
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class FileContactListComposeActivity : AppCompatActivity() {

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory


    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val nodeHandle = intent.getLongExtra(FILE_HANDLE, -1L).takeUnless { it == -1L }
            ?: run {
                Timber.e("Node handle not found in intent")
                finish()
                return
            }
        val nodeName = intent.getStringExtra(FILE_NAME) ?: "Unknown"

        setContent {
            val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val appTransferViewModel = hiltViewModel<AppTransferViewModel>()
            val backStack = rememberNavBackStack(
                FileContactInfoNavKey(
                    folderHandle = nodeHandle,
                    folderName = nodeName,
                )
            )
            val navigationHandler = NavigationHandlerImpl(backStack)

            SharedAppContainer(
                themeMode = themeMode,
                passcodeCryptObjectFactory = passcodeCryptObjectFactory
            ) {
                BackHandler(
                    onBack = {
                        navigationHandler.back()
                    }
                )

                NavDisplay(
                    modifier = Modifier.fillMaxSize(),
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryDecorators = listOf(
                        rememberSceneSetupNavEntryDecorator(),
                        rememberSavedStateNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = entryProvider {
                        FileContactFeatureDestination().navigationGraph(
                            this,
                            navigationHandler,
                            TransferHandlerImpl(appTransferViewModel)
                        )
                    }
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context, nodeHandle: Long, nodeName: String): Intent {
            return Intent(context, FileContactListComposeActivity::class.java).apply {
                putExtra(FILE_HANDLE, nodeHandle)
                putExtra(FILE_NAME, nodeName)
            }
        }

        private const val FILE_HANDLE = "file_handle"
        private const val FILE_NAME = "file_name"
    }
}
