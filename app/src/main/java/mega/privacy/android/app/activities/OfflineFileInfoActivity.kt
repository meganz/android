package mega.privacy.android.app.activities

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.offline.OfflineFileInfoFragment
import mega.privacy.android.app.fragments.offline.OfflineFileInfoFragmentArgs
import mega.privacy.android.app.utils.Constants.HANDLE
import timber.log.Timber

@AndroidEntryPoint
class OfflineFileInfoActivity : PasscodeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_file_info)

        val handle = intent.getStringExtra(HANDLE)
        if (handle == null) {
            Timber.e("OfflineFileInfoActivity handle is null")

            finish()
            return
        }

        if (savedInstanceState == null) {
            val fragment = OfflineFileInfoFragment()
            fragment.arguments = OfflineFileInfoFragmentArgs(handle).toBundle()
            supportFragmentManager.beginTransaction()
                .add(R.id.container, fragment)
                .commit()
        }
    }
}
