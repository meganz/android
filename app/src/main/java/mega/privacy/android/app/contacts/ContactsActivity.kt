package mega.privacy.android.app.contacts

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.databinding.ActivityContactsBinding

@AndroidEntryPoint
class ContactsActivity : PasscodeActivity() {

    private lateinit var binding: ActivityContactsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.toolbar.setupWithNavController(
            getNavController(),
            AppBarConfiguration(
                topLevelDestinationIds = setOf(),
                fallbackOnNavigateUpListener = {
                    onBackPressed()
                    true
                }
            )
        )
    }

    private fun getNavController(): NavController =
        (supportFragmentManager.findFragmentById(R.id.contacts_nav_host_fragment) as NavHostFragment).navController
}
