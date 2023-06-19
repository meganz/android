package mega.privacy.android.app.main.megaachievements

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.GetAchievementsListener
import mega.privacy.android.app.main.InviteContactActivity
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaAchievementsDetails
import timber.log.Timber
import javax.inject.Inject

/**
 * InviteFriendsFragment
 */
@AndroidEntryPoint
class InviteFriendsFragment : Fragment(), GetAchievementsListener.DataCallback {
    private var titleCard: TextView? = null

    /**
     * Deprecated getAchievementsListener
     */
    @Inject
    lateinit var getAchievementsListener: GetAchievementsListener

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val v = inflater.inflate(R.layout.fragment_invite_friends, container, false)
        v.findViewById<Button>(R.id.invite_contacts_button)
            .setOnClickListener(onInviteButtonClick())
        titleCard = v.findViewById(R.id.title_card_invite_fragment)

        if (Util.isDarkMode(requireContext())) {
            val backgroundColor = getColorForElevation(requireContext(), 1f)
            v.findViewById<View>(R.id.invite_contacts_layout).setBackgroundColor(backgroundColor)
            v.findViewById<View>(R.id.how_it_works_layout).setBackgroundColor(backgroundColor)
        }
        return v
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title =
            getString(R.string.title_referral_bonuses)

        // The root view has been created, fill it with the data when data ready
        getAchievementsListener.setDataCallback(this)
    }

    /**
     * onBackPressed
     */
    fun onBackPressed(): Int {
        Timber.d("onBackPressed")
        (requireActivity() as? AchievementsActivity)?.showFragment(Constants.ACHIEVEMENTS_FRAGMENT)
        return 0
    }

    private fun onInviteButtonClick(): (View) -> Unit = {
        val intent = Intent(requireContext(), InviteContactActivity::class.java).apply {
            putExtra(InviteContactActivity.KEY_FROM, true)
        }
        requireActivity().startActivityForResult(intent, Constants.REQUEST_CODE_GET_CONTACTS)
    }

    private fun updateUI() {
        val details = getAchievementsListener.achievementsDetails
        if (details == null || context == null) return
        val referralsStorageValue =
            details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE)
        titleCard?.text = getString(
            R.string.figures_achievements_text_referrals,
            Util.getSizeString(referralsStorageValue, requireContext())
        )
    }

    override fun onAchievementsReceived() {
        updateUI()
    }
}