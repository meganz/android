package mega.privacy.android.app.main.megaachievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.main.megaachievements.composables.AchievementScreen
import mega.privacy.android.app.presentation.achievements.invites.InviteFriendsViewModel.Companion.REFERRAL_STORAGE_BONUS
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.INFO_ACHIEVEMENTS_FRAGMENT
import mega.privacy.android.app.utils.Constants.INVITE_FRIENDS_FRAGMENT
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * Fragment for showing achievements information
 */
@AndroidEntryPoint
class AchievementsFragment : Fragment() {

    /**
     * Get theme mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel by activityViewModels<AchievementsOverviewViewModel>()

    private val achievementActivity: AchievementsActivity
        get() = requireActivity() as AchievementsActivity

    /**
     * Set title which is controlled by activity
     */
    override fun onResume() {
        super.onResume()
        achievementActivity.supportActionBar?.title = getString(R.string.achievements_title)
    }

    /**
     * Setup theme and state in OnCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            AndroidTheme(isDark = themeMode.isDarkMode()) {
                AchievementScreen(
                    viewModel = viewModel,
                    onInviteFriendsClicked = ::navigateToInviteFriends,
                    onShowInfoAchievementsClicked = ::navigateToInfoAchievements,
                    onReferBonusesClicked = ::navigateToReferralBonuses,
                )
            }
        }
    }

    private fun navigateToInviteFriends() =
        achievementActivity.showFragment(
            INVITE_FRIENDS_FRAGMENT,
            bundleOf(
                REFERRAL_STORAGE_BONUS to viewModel.state.value.referralsStorage
            )
        )

    private fun navigateToInfoAchievements(achievementType: AchievementType) =
        achievementActivity.showFragment(
            INFO_ACHIEVEMENTS_FRAGMENT,
            null,
            achievementType.classValue
        )

    private fun navigateToReferralBonuses() =
        achievementActivity.showFragment(Constants.BONUSES_FRAGMENT, null)
}
