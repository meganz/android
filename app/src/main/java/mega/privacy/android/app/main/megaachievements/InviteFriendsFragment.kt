package mega.privacy.android.app.main.megaachievements

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.FragmentInviteFriendsBinding
import mega.privacy.android.app.main.InviteContactActivity
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import timber.log.Timber

/**
 * InviteFriendsFragment
 */
@AndroidEntryPoint
class InviteFriendsFragment : Fragment() {
    private var _binding: FragmentInviteFriendsBinding? = null
    private val binding: FragmentInviteFriendsBinding
        get() = _binding!!

    private val viewModel by viewModels<InviteFriendsViewModel>()

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentInviteFriendsBinding.inflate(layoutInflater)

        binding.inviteContactsButton.setOnClickListener(onInviteButtonClick())

        if (Util.isDarkMode(requireContext())) {
            val backgroundColor = getColorForElevation(requireContext(), 1f)
            binding.inviteContactsLayout.setBackgroundColor(backgroundColor)
            binding.howItWorksLayout.setBackgroundColor(backgroundColor)
        }

        return binding.root
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title =
            getString(R.string.title_referral_bonuses)

        viewLifecycleOwner.collectFlow(viewModel.uiState) { state ->
            updateUI(state.grantStorageInBytes)
        }
    }

    /**
     * onDestroyView
     */
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
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

    private fun updateUI(storage: Long) {
        binding.titleCardInviteFragment.text = getString(
            R.string.figures_achievements_text_referrals,
            Util.getSizeString(storage, requireContext())
        )
    }
}