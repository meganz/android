package mega.privacy.android.app.activities.settingsActivities

import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_SCROLL
import mega.privacy.android.app.databinding.ActivitySettingsBinding
import mega.privacy.android.app.fragments.settingsFragments.SettingsBaseFragment
import mega.privacy.android.app.interfaces.SimpleSnackbarCallBack
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import java.util.*

@AndroidEntryPoint
open class PreferencesBaseActivity : PasscodeActivity(), SimpleSnackbarCallBack {

    protected lateinit var binding: ActivitySettingsBinding

    private val isDark by lazy { Util.isDarkMode(this) }
    private val transparentColor by lazy { ContextCompat.getColor(this, android.R.color.transparent) }
    private val elevation by lazy { resources.getDimension(R.dimen.toolbar_elevation) }
    private var withElevation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarSettings)

        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        LiveEventBus.get(EVENT_UPDATE_SCROLL, Boolean::class.java)
            .observe(this) { withElevation -> updateElevation(withElevation) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

    override fun setTitle(@StringRes titleId: Int) {
        supportActionBar?.title = StringResourcesUtils.getString(titleId)
            .uppercase(Locale.getDefault())
    }

    protected fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, fragment)
        }
    }

    protected fun showSaveButton(callback: () -> Unit) {
        binding.viewSave.isVisible = true
        binding.btnSave.setOnClickListener { callback.invoke() }
    }

    /**
     * Updates the action bar UI depending on elevation.
     *
     * @param withElevation True if should show elevation, false if should hide it.
     */
    private fun updateElevation(withElevation: Boolean) {
        if (this.withElevation == withElevation) {
            return
        }

        this.withElevation = withElevation
        val darkAndElevation = withElevation && isDark

        if (darkAndElevation) {
            ColorUtils.changeStatusBarColorForElevation(this, true)
        } else {
            window?.statusBarColor = transparentColor
        }

        binding.toolbarSettings.setBackgroundColor(
            if (darkAndElevation) ColorUtils.getColorForElevation(this, elevation)
            else transparentColor
        )

        binding.appBarLayoutSettings.elevation = if (withElevation && !isDark) elevation else 0F
    }

    override fun showSnackbar(message: String?) {
        super.showSnackbar(binding.root, message)
    }
}
