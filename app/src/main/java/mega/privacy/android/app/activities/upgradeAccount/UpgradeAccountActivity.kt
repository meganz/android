package mega.privacy.android.app.activities.upgradeAccount

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.databinding.ActivityUpgradeAccountBinding
import mega.privacy.android.app.fragments.homepage.Scrollable
import mega.privacy.android.app.utils.ColorUtils.changeStatusBarColorForElevation
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION
import mega.privacy.android.app.utils.Util.isDarkMode

open class UpgradeAccountActivity : PasscodeActivity(), Scrollable {

    private lateinit var binding: ActivityUpgradeAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUpgradeAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setUpView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        ListenScrollChangesHelper().addViewToListen(
            binding.scrollViewUpgrade
        ) { _: View?, _: Int, _: Int, _: Int, _: Int ->
            checkScroll()
        }

        checkScroll()
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized)
            return

        val withElevation = binding.scrollViewUpgrade.canScrollVertically(SCROLLING_UP_DIRECTION)
        val elevation = resources.getDimension(R.dimen.toolbar_elevation)

        binding.toolbar.setBackgroundColor(
            if (isDarkMode(this@UpgradeAccountActivity) && withElevation) getColorForElevation(
                this@UpgradeAccountActivity,
                elevation
            ) else android.R.color.transparent
        )

        supportActionBar?.elevation = if (withElevation) elevation else 0f
        val el = supportActionBar?.elevation
        changeStatusBarColorForElevation(this, withElevation)
    }
}