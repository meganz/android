package mega.privacy.android.app.main.megaachievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import nz.mega.sdk.MegaAchievementsDetails
import timber.log.Timber
import java.util.Calendar

@AndroidEntryPoint
class InfoAchievementsFragment : Fragment() {

    lateinit var actionBar: ActionBar
    lateinit var icon: ImageView
    lateinit var checkIcon: ImageView
    lateinit var title: TextView
    lateinit var sectionTitle: TextView
    lateinit var firstParagraph: TextView
    lateinit var secondParagraph: TextView
    var achievementType = -1
    var awardId = -1
    var diffDays: Long = 0
    var indexAward = 0

    private val viewModel: AchievementsOverviewViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        Timber.d("onCreateView")
        return inflater.inflate(R.layout.fragment_info_achievements, container, false).apply {
            icon = findViewById(R.id.icon_info_achievements)
            checkIcon = findViewById(R.id.icon_achievement_completed)
            title = findViewById(R.id.title_info_achievements)
            sectionTitle = findViewById(R.id.how_works_title)
            firstParagraph = findViewById(R.id.info_achievements_how_works_first_p)
            secondParagraph = findViewById(R.id.info_achievements_how_works_second_p)
            arguments?.let {
                achievementType = it.getInt("achievementType")
            } ?: run {
                Timber.w("Arguments are null. No achievement type.")
            }

            if (Util.isDarkMode(requireContext())) {
                val backgroundColor = getColorForElevation(requireContext(), 1f)
                findViewById<View>(R.id.title_layout).setBackgroundColor(backgroundColor)
                findViewById<View>(R.id.how_it_works_layout).setBackgroundColor(backgroundColor)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.collectFlow(
            viewModel.state,
            collectBlock = ::handleEvent
        )
    }

    private fun handleEvent(event: AchievementsUI) {
        if (event is AchievementsUI.Content) {
            onAchievementsReceived(
                event.achievementsOverview,
            )
        }
    }

    private fun onAchievementsReceived(
        achievements: AchievementsOverview,
    ) {
        achievements
            .awardedAchievements
            .firstOrNull { it.type.classValue == achievementType }
            ?.let { award ->
                awardId = award.awardId
                Timber.d("AWARD ID: %d", award.awardId)
                val daysLeft = award.expirationTimestampInDays
                val start = Util.calculateDateFromTimestamp(daysLeft)
                val end = Calendar.getInstance()
                val startTime = start.timeInMillis
                val endTime = end.timeInMillis
                val diffTime = startTime - endTime
                diffDays = diffTime / (1000 * 60 * 60 * 24)
            }

        if (achievementType == MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL) {
            icon.setImageDrawable(ContextCompat.getDrawable(requireContext(),
                R.drawable.ic_install_mobile_big))
            if (awardId == -1) {
                Timber.w("No award for the achievement $achievementType")
                val grantedStorage = achievements.allAchievements.first {
                    it.type == AchievementType.MEGA_ACHIEVEMENT_MOBILE_INSTALL
                }.grantStorageInBytes

                setTitleView(
                    grantedStorage,
                    awardId
                )
                setFirstParagraphText(
                    StringResourcesUtils.getString(R.string.paragraph_info_achievement_install_mobile_app,
                        Util.getSizeString(grantedStorage))
                )
            } else {
                val awardedStorage = achievements.awardedAchievements.first {
                    it.awardId == awardId
                }.rewardedStorageInBytes

                setTitleView(
                    awardedStorage,
                    awardId
                )
                setFirstParagraphText(
                    StringResourcesUtils.getString(R.string.result_paragraph_info_achievement_install_mobile_app,
                        awardedStorage)
                )
            }
        } else if (achievementType == MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE) {
            icon.setImageDrawable(ContextCompat.getDrawable(requireContext(),
                R.drawable.il_verify_phone_drawer))
            if (awardId == -1) {
                Timber.d("No award for the achievement $achievementType")
                val grantedStorage = achievements.allAchievements.first {
                    it.type == AchievementType.MEGA_ACHIEVEMENT_ADD_PHONE
                }.grantStorageInBytes

                setTitleView(
                    grantedStorage,
                    awardId
                )
                setFirstParagraphText(
                    StringResourcesUtils.getString(R.string.paragraph_info_achievement_add_phone,
                        Util.getSizeString(grantedStorage))
                )
            } else {
                val awardedStorage = achievements.awardedAchievements.first {
                    it.awardId == awardId
                }.rewardedStorageInBytes

                setTitleView(
                    awardedStorage,
                    awardId
                )
                setFirstParagraphText(
                    StringResourcesUtils.getString(R.string.result_paragraph_info_achievement_add_phone,
                        Util.getSizeString(awardedStorage))
                )
            }
        } else if (achievementType == MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL) {
            icon.setImageDrawable(ContextCompat.getDrawable(requireContext(),
                R.drawable.ic_install_mega_big))
            if (awardId == -1) {
                Timber.w("No award for the achievement $achievementType")
                val grantedStorage = achievements.allAchievements.first {
                    it.type == AchievementType.MEGA_ACHIEVEMENT_DESKTOP_INSTALL
                }.grantStorageInBytes

                setTitleView(
                    grantedStorage,
                    awardId
                )
                setFirstParagraphText(
                    StringResourcesUtils.getString(R.string.paragraph_info_achievement_install_desktop,
                        Util.getSizeString(grantedStorage))
                )
            } else {
                val awardedStorage = achievements.awardedAchievements.first {
                    it.awardId == awardId
                }.rewardedStorageInBytes

                setTitleView(
                    awardedStorage,
                    awardId
                )
                setFirstParagraphText(
                    StringResourcesUtils.getString(R.string.result_paragraph_info_achievement_install_desktop,
                        Util.getSizeString(awardedStorage))
                )
            }
        } else if (achievementType == MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME) {
            val awardedStorage = achievements.awardedAchievements.first {
                it.awardId == awardId
            }.rewardedStorageInBytes

            icon.setImageDrawable(ContextCompat.getDrawable(requireContext(),
                R.drawable.ic_registration_big))
            setTitleView(
                awardedStorage,
                awardId
            )
            setFirstParagraphText(
                getString(R.string.result_paragraph_info_achievement_registration,
                    Util.getSizeString(awardedStorage))
            )
        }
    }

    /**
     * Function to set paragraph text
     * @param firstParagraphText : Paragraph text
     */
    private fun setFirstParagraphText(firstParagraphText: String) {
        firstParagraph.text = firstParagraphText
    }

    /**
     * Function to set title view
     * @param storageValue : Storage value
     * @param awardId : Award Id
     */
    private fun setTitleView(storageValue: Long, awardId: Int) {
        if (awardId == -1) {
            checkIcon.visibility = View.GONE
            title.apply {
                text =
                    StringResourcesUtils.getString(R.string.figures_achievements_text,
                        Util.getSizeString(storageValue))
                setBackgroundColor(ContextCompat.getColor(requireContext(),
                    android.R.color.transparent))
            }
            sectionTitle.visibility = View.VISIBLE
        } else {
            title.apply {
                background = if (diffDays <= 15) {
                    setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.red_600_red_300))
                    ContextCompat.getDrawable(requireContext(),
                        R.drawable.expired_border)
                } else {
                    ContextCompat.getDrawable(requireContext(),
                        R.drawable.bonus_ts_border)
                }

                text = if (diffDays > 0) {
                    StringResourcesUtils.getQuantityString(R.plurals.account_achievements_bonus_expiration_date,
                        diffDays.toInt(), diffDays.toInt())
                } else {
                    StringResourcesUtils.getString(R.string.expired_label)
                }
            }
        }
        secondParagraph.visibility = View.GONE
    }
}
