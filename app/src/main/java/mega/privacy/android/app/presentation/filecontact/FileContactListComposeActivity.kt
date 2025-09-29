package mega.privacy.android.app.presentation.filecontact

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.app.appstate.transfer.AppTransferViewModel
import mega.privacy.android.app.appstate.transfer.TransferHandlerImpl
import mega.privacy.android.app.presentation.container.SharedAppContainer
import mega.privacy.android.app.presentation.filecontact.navigation.FileContactFeatureDestination
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.contract.NavigationHandler
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
            val navController = rememberNavController()
            val navigationHandlerImpl = object : NavigationHandler {
                override fun back() {
                    if (!navController.popBackStack()) {
                        finish()
                    }
                }

                override fun navigate(destination: NavKey) {
                    navController.navigate(destination)
                }

                override fun backTo(destination: NavKey, inclusive: Boolean) {
                    navController.popBackStack(destination, inclusive)
                }

                override fun navigateAndClearBackStack(destination: NavKey) {
                    navController.navigate(destination) {
                        popUpTo(0) { inclusive = true }
                    }
                }

                override fun navigateAndClearTo(
                    destination: NavKey,
                    newParent: NavKey,
                    inclusive: Boolean,
                ) {
                    navController.navigate(destination) {
                        popUpTo(newParent) { this.inclusive = inclusive }
                    }
                }

                override fun <T> returnResult(key: String, value: T) {
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        key = key,
                        value = value
                    )
                    navController.popBackStack()
                }

                override fun <T> monitorResult(key: String) =
                    navController.currentBackStackEntryFlow.mapNotNull {
                        if (it.savedStateHandle.contains(key)) {
                            val result = it.savedStateHandle.get<T>(key)
                            it.savedStateHandle.remove<T>(key)
                            result
                        } else null
                    }
            }

            SharedAppContainer(
                themeMode = themeMode,
                passcodeCryptObjectFactory = passcodeCryptObjectFactory
            ) {
                BackHandler(
                    onBack = {
                        if (!navController.popBackStack()) {
                            finish()
                        }
                    }
                )

                NavHost(
                    navController = navController,
                    startDestination = FileContactInfoNavKey(
                        folderHandle = nodeHandle,
                        folderName = nodeName,
                    )
                ) {
                    FileContactFeatureDestination().navigationGraph(
                        this,
                        navigationHandlerImpl,
                        TransferHandlerImpl(appTransferViewModel)
                    )
                }
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
