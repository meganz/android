package mega.privacy.android.app.main.megaachievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.data.extensions.toUnitString
import mega.privacy.android.app.databinding.FragmentInfoAchievementsBinding
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.Util

/**
 * InfoAchievementsFragment
 */
@AndroidEntryPoint
class InfoAchievementsFragment : Fragment() {
    private var _binding: FragmentInfoAchievementsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InfoAchievementsViewModel by viewModels()

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentInfoAchievementsBinding.inflate(inflater, container, false)

        if (Util.isDarkMode(requireContext())) {
            val backgroundColor = getColorForElevation(requireContext(), 1f)
            binding.titleLayout.setBackgroundColor(backgroundColor)
            binding.howItWorksLayout.setBackgroundColor(backgroundColor)
        }

        return binding.root
    }

    /**
     * onDestroyView
     */
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.collectFlow(
            viewModel.uiState,
            collectBlock = ::handleEvent
        )
    }

    private fun handleEvent(uiState: InfoAchievementsUIState) {
        val attributes = uiState.achievementType.toInfoAchievementsAttribute(uiState.awardId != -1)

        binding.iconInfoAchievements.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                attributes.iconResourceId
            )
        )
        setTitleView(
            uiState.awardStorageInBytes,
            uiState.awardId,
            uiState.achievementRemainingDays
        )
        setFirstParagraphText(
            getString(
                attributes.subtitleTextResourceId,
                uiState.awardStorageInBytes.toUnitString(requireContext())
            )
        )
    }

    /**
     * Function to set paragraph text
     * @param firstParagraphText : Paragraph text
     */
    private fun setFirstParagraphText(firstParagraphText: String) {
        binding.infoAchievementsHowWorksFirstP.text = firstParagraphText
    }

    /**
     * Function to set title view
     * @param storageValue : Storage value
     * @param awardId : Award Id
     */
    private fun setTitleView(storageValue: Long, awardId: Int, daysRemaining: Long) {
        if (awardId == -1) {
            binding.iconAchievementCompleted.visibility = View.GONE
            binding.titleInfoAchievements.apply {
                text =
                    getString(
                        R.string.figures_achievements_text,
                        Util.getSizeString(storageValue, context)
                    )
                setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.transparent
                    )
                )
            }
            binding.howWorksTitle.visibility = View.VISIBLE
        } else {
            binding.titleInfoAchievements.apply {
                background = if (daysRemaining <= 15) {
                    setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.red_600_red_300
                        )
                    )
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.expired_border
                    )
                } else {
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.bonus_ts_border
                    )
                }

                text = if (daysRemaining > 0) {
                    resources.getQuantityString(
                        R.plurals.account_achievements_bonus_expiration_date,
                        daysRemaining.toInt(), daysRemaining.toInt()
                    )
                } else {
                    getString(R.string.expired_label)
                }
            }
        }
        binding.infoAchievementsHowWorksSecondP.visibility = View.GONE
    }
}
