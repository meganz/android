package mega.privacy.android.app.main.megaachievements

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.GetAchievementsListener
import mega.privacy.android.app.presentation.achievements.AchievementsViewModel
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.achievement.AchievementType
import nz.mega.sdk.MegaAchievementsDetails
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class InfoAchievementsFragment : Fragment(), GetAchievementsListener.DataCallback {

    @Inject
    lateinit var getAchievementsListener: GetAchievementsListener
    lateinit var actionBar: ActionBar
    lateinit var icon: ImageView
    lateinit var checkIcon: ImageView
    lateinit var title: TextView
    lateinit var sectionTitle: TextView
    lateinit var firstParagraph: TextView
    lateinit var secondParagraph: TextView
    var achievementType = -1
    var awardId = -1
    var rewardId = -1
    var diffDays: Long = 0
    var indexAward = 0

    val achievementsViewModel: AchievementsViewModel by viewModels()

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

        getAchievementsListener.setDataCallback(this)
        getAchievementsTitleAndType(achievementType = achievementType).run {
            achievementsViewModel.setToolbarTitle(this.first)
            achievementsViewModel.setAchievementType(this.second)
        }
    }

    private fun getAchievementsTitleAndType(achievementType: Int): Pair<String, AchievementType> {
        val title: String
        val type: AchievementType
        when (achievementType) {
            MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL -> {
                title = getString(R.string.title_install_app)
                type = AchievementType.MEGA_ACHIEVEMENT_MOBILE_INSTALL
            }
            MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE -> {
                title = getString(R.string.title_add_phone)
                type = AchievementType.MEGA_ACHIEVEMENT_ADD_PHONE
            }
            MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL -> {
                title = getString(R.string.title_install_desktop)
                type = AchievementType.MEGA_ACHIEVEMENT_DESKTOP_INSTALL
            }
            MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME -> {
                title = getString(R.string.title_regitration)
                type = AchievementType.MEGA_ACHIEVEMENT_WELCOME
            }
            else -> {
                title = ""
                type = AchievementType.INVALID_ACHIEVEMENT
            }
        }

        return Pair(title, type)
    }

    private fun updateUI() {
        val details = getAchievementsListener.achievementsDetails ?: return
        with(details) {
            achievementsViewModel.setAwardCount(awardsCount)
            for (i in 0 until awardsCount) {
                val type = getAwardClass(i)
                if (type == achievementType) {
                    awardId = getAwardId(i)
                    rewardId = getRewardAwardId(awardId.toLong())
                    Timber.d("AWARD ID: %d REWARD id: %d", awardId, rewardId)
                    val daysLeft = getAwardExpirationTs(i)
                    val start = Util.calculateDateFromTimestamp(daysLeft)
                    val end = Calendar.getInstance()
                    val startDate = start.time
                    val endDate = end.time
                    val startTime = startDate.time
                    val endTime = endDate.time
                    val diffTime = startTime - endTime
                    diffDays = diffTime / (1000 * 60 * 60 * 24)
                    indexAward = i.toInt()
                    break
                } else {
                    Timber.w("No match for achievement award!")
                }
            }

            if (achievementType == MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL) {
                icon.setImageDrawable(ContextCompat.getDrawable(requireContext(),
                    R.drawable.ic_install_mobile_big))
                if (awardId == -1) {
                    Timber.w("No award for this achievement")
                    setTitleView(
                        getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL),
                        awardId
                    )
                    setFirstParagraphText(
                        StringResourcesUtils.getString(R.string.paragraph_info_achievement_install_mobile_app,
                            Util.getSizeString(
                                getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL)))
                    )
                } else {
                    setTitleView(
                        getRewardStorageByAwardId(awardId),
                        awardId
                    )
                    setFirstParagraphText(
                        StringResourcesUtils.getString(R.string.result_paragraph_info_achievement_install_mobile_app,
                            Util.getSizeString(getRewardStorageByAwardId(awardId)))
                    )
                }
            } else if (achievementType == MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE) {
                icon.setImageDrawable(ContextCompat.getDrawable(requireContext(),
                    R.drawable.il_verify_phone_drawer))
                if (awardId == -1) {
                    Timber.d("No award for this achievement")
                    setTitleView(
                        getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE),
                        awardId
                    )
                    setFirstParagraphText(
                        StringResourcesUtils.getString(R.string.paragraph_info_achievement_add_phone,
                            Util.getSizeString(getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE)))
                    )
                } else {
                    setTitleView(
                        getRewardStorageByAwardId(awardId),
                        awardId
                    )
                    setFirstParagraphText(
                        StringResourcesUtils.getString(R.string.result_paragraph_info_achievement_add_phone,
                            Util.getSizeString(getRewardStorageByAwardId(awardId)))
                    )
                }
            } else if (achievementType == MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL) {
                icon.setImageDrawable(ContextCompat.getDrawable(requireContext(),
                    R.drawable.ic_install_mega_big))
                if (awardId == -1) {
                    Timber.w("No award for this achievement")
                    setTitleView(
                        getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL),
                        awardId
                    )
                    setFirstParagraphText(
                        StringResourcesUtils.getString(R.string.paragraph_info_achievement_install_desktop,
                            Util.getSizeString(getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL)))
                    )
                } else {
                    setTitleView(
                        getRewardStorageByAwardId(awardId),
                        awardId
                    )
                    setFirstParagraphText(
                        StringResourcesUtils.getString(R.string.result_paragraph_info_achievement_install_desktop,
                            Util.getSizeString(getRewardStorageByAwardId(awardId)))
                    )
                }
            } else if (achievementType == MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME) {
                icon.setImageDrawable(ContextCompat.getDrawable(requireContext(),
                    R.drawable.ic_registration_big))
                setTitleView(
                    getRewardStorageByAwardId(awardId),
                    awardId
                )
                setFirstParagraphText(
                    getString(R.string.result_paragraph_info_achievement_registration,
                        Util.getSizeString(getRewardStorageByAwardId(awardId)))
                )
            }
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
                    StringResourcesUtils.getString(R.string.expiration_date_for_achievements,
                        diffDays)
                } else {
                    StringResourcesUtils.getString(R.string.expired_label)
                }
            }
        }
        secondParagraph.visibility = View.GONE
    }

    override fun onAchievementsReceived() {
        updateUI()
    }
}