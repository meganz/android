package mega.privacy.android.app.activities.settingsActivities

import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ActivitySettingsBinding
import mega.privacy.android.app.fragments.settingsFragments.SettingsBaseFragment
import mega.privacy.android.app.lollipop.PinActivityLollipop
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import java.util.*

open class PreferencesBaseActivity : PinActivityLollipop() {

    protected lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarSettings)

        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_arrow_back_black)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        Util.changeStatusBarColor(this, window, R.color.dark_primary_color)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

    override fun setTitle(@StringRes titleId: Int) {
        supportActionBar?.title = StringResourcesUtils.getString(titleId)
            .toUpperCase(Locale.getDefault())
    }

    protected fun replaceFragment(fragment: SettingsBaseFragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, fragment)
        }
    }

    protected fun showSaveButton(callback: () -> Unit) {
        binding.viewSave.isVisible = true
        binding.btnSave.setOnClickListener { callback.invoke() }
    }
}
