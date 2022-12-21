package mega.privacy.android.app.main.megaachievements

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.databinding.ActivityAchievementsBinding
import mega.privacy.android.app.listeners.GetAchievementsListener
import mega.privacy.android.app.listeners.GetAchievementsListener.RequestCallback
import mega.privacy.android.app.main.InviteContactActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.StringResourcesUtils
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
    lateinit var fetcher: GetAchievementsListener

    private val binding: ActivityAchievementsBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityAchievementsBinding.inflate(layoutInflater)
    }

    private var successDialog: AlertDialog? = null

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

        fetcher.setRequestCallback(object :
            RequestCallback {
            override fun onRequestError() {
                showSnackbar(getString(R.string.cancel_subscription_error))
            }
        })
        fetcher.fetch()
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
    fun showFragment(fragmentName: Int, type: Int = INVALID_TYPE) {
        Timber.d("showFragment: %d type: %d", fragmentName, type)
        val ft = supportFragmentManager.beginTransaction()
        var fragment: Fragment? = null
        var tag = ""
        when (fragmentName) {
            Constants.ACHIEVEMENTS_FRAGMENT -> {
                Util.hideKeyboard(this, InputMethodManager.HIDE_NOT_ALWAYS)
                supportActionBar?.title =
                    StringResourcesUtils.getString(R.string.achievements_title)
                fragment = AchievementsFragment()
                tag = "achievementsFragment"
            }
            Constants.INVITE_FRIENDS_FRAGMENT -> {
                fragment = InviteFriendsFragment()
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

    private fun showInviteConfirmationDialog(contentText: String) {
        Timber.d("showInviteConfirmationDialog")
        val builder = MaterialAlertDialogBuilder(this).apply {
            val dialogLayout =
                layoutInflater.inflate(R.layout.dialog_invite_friends_achievement, null)
            val content = dialogLayout.findViewById<TextView>(R.id.invite_content)
            content.text = contentText
            val closeButton = dialogLayout.findViewById<Button>(R.id.close_btn)
            closeButton.setOnClickListener { successDialog?.dismiss() }
            setView(dialogLayout)
        }
        successDialog = builder.create().also {
            it.show()
        }
    }

    /**
     * Show snackbar
     *
     */
    fun showSnackbar(s: String) {
        showSnackbar(binding.fragmentContainerAchievements, s)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == Constants.REQUEST_CODE_GET_CONTACTS && resultCode == RESULT_OK && intent != null) {
            val sentNumber = intent.getIntExtra(InviteContactActivity.KEY_SENT_NUMBER, 1)
            if (sentNumber > 1) {
                showInviteConfirmationDialog(getString(R.string.invite_sent_text_multi))
            } else {
                showInviteConfirmationDialog(getString(R.string.invite_sent_text))
            }
        }
    }

    companion object {
        private const val TAG_ACHIEVEMENTS = "achievementsFragment"
        private const val INVALID_TYPE = -1
    }
}