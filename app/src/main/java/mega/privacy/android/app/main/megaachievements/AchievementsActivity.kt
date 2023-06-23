package mega.privacy.android.app.main.megaachievements

import android.os.Bundle
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.ActivityAchievementsBinding
import mega.privacy.android.app.listeners.GetAchievementsListener
import mega.privacy.android.app.presentation.achievements.invites.InviteFriendsFragment
import mega.privacy.android.app.presentation.achievements.referral.ReferralBonusesFragment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import timber.log.Timber
import javax.inject.Inject

/**
 * Achievements activity
 *
 */
@AndroidEntryPoint
class AchievementsActivity : PasscodeActivity() {

    /**
     * fetcher
     */
    @Inject
    @Deprecated("This field will be removed in future")
    lateinit var fetcher: GetAchievementsListener

    private val viewModel: AchievementsOverviewViewModel by viewModels()

    private val binding: ActivityAchievementsBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityAchievementsBinding.inflate(layoutInflater)
    }

    /**
     * On create
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return
        }
        fetcher.fetch()
        setContentView(binding.root)
        binding.toolbarAchievements.isVisible = true
        setSupportActionBar(binding.toolbarAchievements)
        supportActionBar?.apply {
            setHomeAsUpIndicator(if (Util.isDarkMode(this@AchievementsActivity)) R.drawable.ic_arrow_back_white else R.drawable.ic_arrow_back_black)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
        if (savedInstanceState == null) {
            val ft = supportFragmentManager.beginTransaction()
            ft.add(R.id.fragment_container_achievements, AchievementsFragment(), TAG_ACHIEVEMENTS)
            ft.commitNow()
        }

        collectFlow(
            viewModel.state,
            collectBlock = ::handleEvent
        )
    }

    private fun handleEvent(event: AchievementsUIState) {
        if (event.showError) {
            showSnackbar(getString(R.string.cancel_subscription_error))
        }
    }

    /**
     * On options item selected
     *
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected")
        when (item.itemId) {
            android.R.id.home -> onBackPressedDispatcher.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Show fragment
     *
     */
    @JvmOverloads
    fun showFragment(fragmentName: Int, arguments: Bundle?, type: Int = INVALID_TYPE) {
        Timber.d("showFragment: %d type: %d", fragmentName, type)
        val ft = supportFragmentManager.beginTransaction()
        var fragment: Fragment? = null
        var tag = ""
        when (fragmentName) {
            Constants.ACHIEVEMENTS_FRAGMENT -> {
                Util.hideKeyboard(this, InputMethodManager.HIDE_NOT_ALWAYS)
                supportActionBar?.title =
                    getString(R.string.achievements_title)
                fragment = AchievementsFragment()
                tag = "achievementsFragment"
            }

            Constants.INVITE_FRIENDS_FRAGMENT -> {
                fragment = InviteFriendsFragment().apply {
                    this.arguments = arguments
                }
                tag = "inviteFriendsFragment"
                ft.addToBackStack(tag)
            }

            Constants.BONUSES_FRAGMENT -> {
                fragment = ReferralBonusesFragment()
                tag = "referralBonusesFragment"
                ft.addToBackStack(tag)
            }

            Constants.INFO_ACHIEVEMENTS_FRAGMENT -> {
                fragment = InfoAchievementsFragment()
                fragment.setArguments(bundleOf("achievementType" to type))
                tag = "infoAchievementsFragment"
                ft.addToBackStack(tag)
            }

            else -> {}
        }
        fragment?.let {
            ft.replace(R.id.fragment_container_achievements, it, tag).commit()
        }
    }

    /**
     * Show snackbar
     *
     */
    fun showSnackbar(s: String) {
        showSnackbar(binding.fragmentContainerAchievements, s)
    }

    companion object {
        private const val TAG_ACHIEVEMENTS = "achievementsFragment"
        private const val INVALID_TYPE = -1
    }
}