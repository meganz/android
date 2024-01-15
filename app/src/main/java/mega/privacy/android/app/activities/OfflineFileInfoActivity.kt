package mega.privacy.android.app.activities

import android.os.Bundle
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.offline.offlinefileinfocompose.OfflineFileInfoComposeFragment
import mega.privacy.android.app.presentation.offline.offlinefileinfocompose.OfflineFileInfoComposeFragmentArgs
import mega.privacy.android.app.utils.Constants.HANDLE
import timber.log.Timber

@AndroidEntryPoint
class OfflineFileInfoActivity : PasscodeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_file_info)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val handle = intent.getStringExtra(HANDLE)
        if (handle == null) {
            Timber.e("OfflineFileInfoActivity handle is null")
            finish()
            return
        }

        if (savedInstanceState == null) {
            val fragment = OfflineFileInfoComposeFragment()
            fragment.arguments = OfflineFileInfoComposeFragmentArgs(handle.toLong()).toBundle()
            supportFragmentManager.beginTransaction().add(R.id.container, fragment).commit()
        }
    }
}
