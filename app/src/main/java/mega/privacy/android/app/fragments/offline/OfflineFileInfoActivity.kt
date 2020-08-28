package mega.privacy.android.app.fragments.offline

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants.HANDLE

@AndroidEntryPoint
class OfflineFileInfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_file_info)

        val fragment = OfflineFileInfoFragment()
        fragment.arguments = OfflineFileInfoFragmentArgs(intent.getStringExtra(HANDLE)).toBundle()
        supportFragmentManager.beginTransaction()
            .add(R.id.container, fragment)
            .commit()
    }
}
