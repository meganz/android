package mega.privacy.android.app.presentation.filecontact

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity
import mega.privacy.android.app.presentation.container.SharedAppContainer
import mega.privacy.android.app.presentation.filecontact.navigation.FileContactInfo
import mega.privacy.android.app.presentation.filecontact.navigation.fileContacts
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class FileContactListComposeActivity : AppCompatActivity() {

    @Inject
    lateinit var getThemeMode: GetThemeMode

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
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            SharedAppContainer(
                themeMode = themeMode,
                passcodeCryptObjectFactory = passcodeCryptObjectFactory
            ) {
                NavHost(
                    navController = rememberNavController(),
                    startDestination = FileContactInfo(
                        folderHandle = nodeHandle,
                        folderName = nodeName,
                    )
                ) {
                    fileContacts(
                        onNavigateBack = { supportFinishAfterTransition() },
                        onNavigateToInfo = {
                            val i = Intent(
                                this@FileContactListComposeActivity,
                                ContactInfoActivity::class.java
                            )
                            i.putExtra(Constants.NAME, it.email)
                            startActivity(i)
                        }
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
