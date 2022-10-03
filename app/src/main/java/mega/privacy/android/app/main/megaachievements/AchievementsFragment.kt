package mega.privacy.android.app.main.megaachievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.R
import mega.privacy.android.app.data.extensions.getBigFormattedStorageString
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.GetAchievementsListener
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaAchievementsDetails
import nz.mega.sdk.MegaApiAndroid
import timber.log.Timber
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * Achievements Fragment
 */
@AndroidEntryPoint
class AchievementsFragment : Fragment(), View.OnClickListener,
    GetAchievementsListener.DataCallback {

    /**
     * MegaApi
     */
    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    private lateinit var registrationLayout: RelativeLayout
    private lateinit var separatorRegistration: LinearLayout
    private lateinit var figuresInstallAppLayout: RelativeLayout
    private lateinit var zeroFiguresInstallAppText: TextView
    private lateinit var figuresReferralBonusesLayout: RelativeLayout
    private lateinit var zeroFiguresReferralBonusesText: TextView
    private lateinit var figuresRegistrationLayout: RelativeLayout
    private lateinit var figuresInstallDesktopLayout: RelativeLayout
    private lateinit var zeroFiguresInstallDesktopText: TextView
    private lateinit var figuresAddPhoneLayout: RelativeLayout
    private lateinit var zeroFiguresAddPhoneText: TextView
    private lateinit var installAppIcon: ImageView
    private lateinit var installDesktopIcon: ImageView
    private lateinit var registrationIcon: ImageView
    private lateinit var addPhoneIcon: ImageView
    private lateinit var referralBonusIcon: ImageView
    private lateinit var figureUnlockedRewardStorage: TextView
    private lateinit var figureReferralBonusesStorage: TextView
    private lateinit var figureInstallAppStorage: TextView
    private lateinit var textInstallAppStorage: TextView
    private lateinit var daysLeftInstallAppText: TextView
    private lateinit var figureAddPhoneStorage: TextView
    private lateinit var textAddPhoneStorage: TextView
    private lateinit var daysLeftAddPhoneText: TextView
    private lateinit var figureRegistrationStorage: TextView
    private lateinit var textRegistrationStorage: TextView
    private lateinit var daysLeftRegistrationText: TextView
    private lateinit var figureInstallDesktopStorage: TextView
    private lateinit var textInstallDesktopStorage: TextView
    private lateinit var daysLeftInstallDesktopText: TextView

    private var storageReferrals: Long = 0
    private var transferReferrals: Long = 0

    /**
     * Init Fragment UI
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        val enabledAchievements = megaApi.isAchievementsEnabled
        Timber.d("The achievements are: $enabledAchievements")
        val root = inflater.inflate(R.layout.fragment_achievements, container, false)

        val inviteFriendsButton = root.findViewById<Button>(R.id.invite_button)
        inviteFriendsButton.setOnClickListener(this)
        val referralBonusesLayout = root.findViewById<RelativeLayout>(R.id.referral_bonuses_layout)
        referralBonusesLayout.setOnClickListener(this)
        val titleReferralBonuses = root.findViewById<TextView>(R.id.title_referral_bonuses)
        val isPortrait = Util.isScreenInPortrait(getInstance())
        titleReferralBonuses.maxWidth = Util.scaleWidthPx(
            if (isPortrait) 190 else 250,
            resources.displayMetrics
        )

        figuresReferralBonusesLayout = root.findViewById(R.id.figures_referral_bonuses_layout)
        figuresReferralBonusesLayout.visibility = View.GONE
        zeroFiguresReferralBonusesText = root.findViewById(R.id.zero_figures_referral_bonuses_text)
        separatorRegistration = root.findViewById(R.id.separator_registration)
        registrationLayout = root.findViewById(R.id.registration_layout)
        registrationLayout.setOnClickListener(this)

        val titleRegistration = root.findViewById<TextView>(R.id.title_registration)
        titleRegistration.maxWidth = Util.scaleWidthPx(
            if (isPortrait) 190 else 250,
            resources.displayMetrics
        )
        val installAppLayout = root.findViewById<RelativeLayout>(R.id.install_app_layout)
        installAppLayout.setOnClickListener(this)
        val titleInstallApp = root.findViewById<TextView>(R.id.title_install_app)
        titleInstallApp.maxWidth = Util.scaleWidthPx(
            if (isPortrait) 190 else 250,
            resources.displayMetrics
        )

        figuresRegistrationLayout = root.findViewById(R.id.figures_registration_layout)
        figuresInstallAppLayout = root.findViewById(R.id.figures_install_app_layout)
        figuresInstallAppLayout.visibility = View.GONE
        zeroFiguresInstallAppText = root.findViewById(R.id.zero_figures_install_app_text)

        val addPhoneLayout = root.findViewById<RelativeLayout>(R.id.add_phone_layout)
        if (megaApi.smsAllowedState() == 2) {
            addPhoneLayout.setOnClickListener(this)
            val titleAddPhone = root.findViewById<TextView>(R.id.title_add_phone)
            titleAddPhone.maxWidth = Util.scaleWidthPx(
                if (isPortrait) 190 else 250,
                resources.displayMetrics
            )
        } else {
            root.findViewById<View>(R.id.separator_add_phone).visibility = View.GONE
            addPhoneLayout.visibility = View.GONE
        }

        figuresAddPhoneLayout = root.findViewById(R.id.figures_add_phone_layout)
        figuresAddPhoneLayout.visibility = View.GONE
        zeroFiguresAddPhoneText = root.findViewById(R.id.zero_figures_add_phone_text)

        val installDesktopLayout = root.findViewById<RelativeLayout>(R.id.install_desktop_layout)
        installDesktopLayout.setOnClickListener(this)
        val titleInstallDesktop = root.findViewById<TextView>(R.id.title_install_desktop)
        titleInstallDesktop.maxWidth = Util.scaleWidthPx(
            if (isPortrait) 190 else 250,
            resources.displayMetrics
        )

        figuresInstallDesktopLayout = root.findViewById(R.id.figures_install_desktop_layout)
        figuresInstallDesktopLayout.visibility = View.GONE
        zeroFiguresInstallDesktopText = root.findViewById(R.id.zero_figures_install_desktop_text)

        installAppIcon = root.findViewById(R.id.install_app_icon)
        addPhoneIcon = root.findViewById(R.id.add_phone_icon)
        installDesktopIcon = root.findViewById(R.id.install_desktop_icon)
        registrationIcon = root.findViewById(R.id.registration_icon)
        referralBonusIcon = root.findViewById(R.id.referral_bonuses_icon)

        figureUnlockedRewardStorage = root.findViewById(R.id.unlocked_storage)
        figureUnlockedRewardStorage.text = 0L.getBigFormattedStorageString()
        figureReferralBonusesStorage = root.findViewById(R.id.figure_unlocked_storage_text_referral)
        figureReferralBonusesStorage.text = Util.getSizeString(0)

        val storageSpaceString = getString(R.string.storage_space).lowercase(Locale.getDefault())
        val textReferralBonusesStorage =
            root.findViewById<TextView>(R.id.unlocked_storage_title_referral)
        textReferralBonusesStorage.text = storageSpaceString

        figureInstallAppStorage = root.findViewById(R.id.figure_unlocked_storage_text_install_app)
        figureInstallAppStorage.text = Util.getSizeString(0)
        textInstallAppStorage = root.findViewById(R.id.unlocked_storage_title_install_app)
        textInstallAppStorage.text = storageSpaceString
        daysLeftInstallAppText = root.findViewById(R.id.days_left_text_install_app)
        daysLeftInstallAppText.text = "..."
        figureAddPhoneStorage = root.findViewById(R.id.figure_unlocked_storage_text_add_phone)
        figureAddPhoneStorage.text = Util.getSizeString(0)
        textAddPhoneStorage = root.findViewById(R.id.unlocked_storage_title_add_phone)
        textAddPhoneStorage.text = storageSpaceString
        daysLeftAddPhoneText = root.findViewById(R.id.days_left_text_add_phone)
        daysLeftAddPhoneText.text = "..."
        figureRegistrationStorage =
            root.findViewById(R.id.figure_unlocked_storage_text_registration)
        figureRegistrationStorage.text = Util.getSizeString(0)
        textRegistrationStorage = root.findViewById(R.id.unlocked_storage_title_registration)
        textRegistrationStorage.text = storageSpaceString
        daysLeftRegistrationText = root.findViewById(R.id.days_left_text_registration)
        daysLeftRegistrationText.text = "..."
        figureInstallDesktopStorage =
            root.findViewById(R.id.figure_unlocked_storage_text_install_desktop)
        figureInstallDesktopStorage.text = Util.getSizeString(0)
        textInstallDesktopStorage = root.findViewById(R.id.unlocked_storage_title_install_desktop)
        textInstallDesktopStorage.text = storageSpaceString
        daysLeftInstallDesktopText = root.findViewById(R.id.days_left_text_install_desktop)
        daysLeftInstallDesktopText.text = "..."

        daysLeftInstallDesktopText.visibility = View.INVISIBLE
        daysLeftInstallAppText.visibility = View.INVISIBLE
        daysLeftAddPhoneText.visibility = View.INVISIBLE
        figureUnlockedRewardStorage.text = "..."

        if (Util.isDarkMode(requireContext())) {
            val backgroundColor = getColorForElevation(requireContext(), 1f)
            root.findViewById<View>(R.id.unlocked_rewards_layout)
                .setBackgroundColor(backgroundColor)
            root.findViewById<View>(R.id.card_view_2).setBackgroundColor(backgroundColor)
        }
        return root
    }

    /**
     * Called after onCreateView
     */
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
        if (actionBar != null) {
            actionBar.title =
                StringResourcesUtils.getString(R.string.achievements_title)
        }

        // The root view has been created, fill it with the data when data ready
        if (AchievementsActivity.sFetcher != null) {
            AchievementsActivity.sFetcher.setDataCallback(this)
        }
    }

    /**
     * Click event
     */
    override fun onClick(view: View) {
        when (view.id) {
            R.id.referral_bonuses_layout -> {
                Timber.d("Go to section Referral bonuses")
                (requireActivity() as AchievementsActivity).showFragment(
                    if (transferReferrals > 0 || storageReferrals > 0) Constants.BONUSES_FRAGMENT else Constants.INVITE_FRIENDS_FRAGMENT,
                    AchievementsActivity.INVALID_TYPE
                )
            }
            R.id.install_app_layout -> {
                Timber.d("Go to info app install")
                (requireActivity() as AchievementsActivity).showFragment(
                    Constants.INFO_ACHIEVEMENTS_FRAGMENT,
                    MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL
                )
            }
            R.id.add_phone_layout -> {
                Timber.d("Go to info add phone")
                (requireActivity() as AchievementsActivity).showFragment(
                    Constants.INFO_ACHIEVEMENTS_FRAGMENT,
                    MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE
                )
            }
            R.id.registration_layout -> {
                Timber.d("Go to info registration")
                (requireActivity() as AchievementsActivity).showFragment(
                    Constants.INFO_ACHIEVEMENTS_FRAGMENT,
                    MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME
                )
            }
            R.id.install_desktop_layout -> {
                Timber.d("Go to info desktop install")
                (requireActivity() as AchievementsActivity).showFragment(
                    Constants.INFO_ACHIEVEMENTS_FRAGMENT,
                    MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL
                )
            }
            R.id.invite_button -> {
                Timber.d("Invite friends")
                (requireActivity() as AchievementsActivity).showFragment(
                    Constants.INVITE_FRIENDS_FRAGMENT,
                    -1
                )
            }
        }
    }

    /**
     * Receive Achievements
     */
    override fun onAchievementsReceived() {
        Timber.d("Achievements received - Update UI")
        if (AchievementsActivity.sFetcher == null) return
        val details = AchievementsActivity.sFetcher.achievementsDetails
        val bonuses = AchievementsActivity.sFetcher.referralBonuses
        if (details == null || bonuses == null) {
            return
        }

        var totalStorage: Long = 0
        var totalTransfer: Long = 0
        storageReferrals = details.currentStorageReferrals()
        totalStorage += storageReferrals
        transferReferrals = details.currentTransferReferrals()
        totalTransfer += transferReferrals
        Timber.d(
            "After referrals: storage: %s transfer %s",
            Util.getSizeString(totalStorage),
            Util.getSizeString(totalTransfer)
        )

        val referralsStorageValue =
            details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE)
        val installAppStorageValue =
            details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL)
        val addPhoneStorageValue =
            details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE)
        val installDesktopStorageValue =
            details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL)

        if (transferReferrals > 0 || storageReferrals > 0) {
            figureReferralBonusesStorage.text = Util.getSizeString(storageReferrals)
            figuresReferralBonusesLayout.visibility = View.VISIBLE
            zeroFiguresReferralBonusesText.visibility = View.GONE
            Timber.d("Check if referrals are expired")
            var expiredNumber = 0
            for (i in bonuses.indices) {
                val referralBonus = bonuses[i]
                if (referralBonus.getDaysLeft() < 0) {
                    expiredNumber++
                }
            }
            if (expiredNumber >= bonuses.size - 1) {
                Timber.d("All the referrals are expired")
                figuresReferralBonusesLayout.alpha = 0.5f
                referralBonusIcon.alpha = 0.5f
            }
        } else {
            figuresReferralBonusesLayout.visibility = View.GONE
            zeroFiguresReferralBonusesText.text = StringResourcesUtils.getString(
                R.string.figures_achievements_text_referrals,
                Util.getSizeString(referralsStorageValue)
            )
            zeroFiguresReferralBonusesText.visibility = View.VISIBLE
        }

        zeroFiguresInstallAppText.text = StringResourcesUtils.getString(
            R.string.figures_achievements_text,
            Util.getSizeString(installAppStorageValue)
        )
        zeroFiguresAddPhoneText.text = StringResourcesUtils.getString(
            R.string.figures_achievements_text,
            Util.getSizeString(addPhoneStorageValue)
        )
        zeroFiguresInstallDesktopText.text = StringResourcesUtils.getString(
            R.string.figures_achievements_text,
            Util.getSizeString(installDesktopStorageValue)
        )

        val count = details.awardsCount

        for (i in 0 until count) {
            val type = details.getAwardClass(i)
            val awardId = details.getAwardId(i)
            val rewardId = details.getRewardAwardId(awardId.toLong())
            Timber.d("AWARD ID: $awardId REWARD id: $rewardId")
            Timber.d("type: $type AWARD ID: $awardId REWARD id: $rewardId")

            if (type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL) {
                Timber.d("MEGA_ACHIEVEMENT_MOBILE_INSTALL")
                figuresInstallAppLayout.visibility = View.VISIBLE
                zeroFiguresInstallAppText.visibility = View.GONE
                val storageInstallApp = details.getRewardStorageByAwardId(awardId)
                if (storageInstallApp > 0) {
                    figureInstallAppStorage.text = Util.getSizeString(storageInstallApp)
                    figureInstallAppStorage.visibility = View.VISIBLE
                    textInstallAppStorage.visibility = View.VISIBLE
                } else {
                    figureInstallAppStorage.visibility = View.INVISIBLE
                    textInstallAppStorage.visibility = View.INVISIBLE
                }

                val transferInstallApp = details.getRewardTransferByAwardId(awardId)
                daysLeftInstallAppText.visibility = View.VISIBLE
                val daysLeftInstallApp = details.getAwardExpirationTs(i)
                Timber.d("Install App AwardExpirationTs: $daysLeftInstallApp")

                val start = Util.calculateDateFromTimestamp(daysLeftInstallApp)
                val end = Calendar.getInstance()
                val startDate = start.time
                val endDate = end.time
                val startTime = startDate.time
                val endTime = endDate.time
                val diffTime = startTime - endTime
                val diffDays = diffTime / (1000 * 60 * 60 * 24)

                if (diffDays <= 15) {
                    daysLeftInstallAppText.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.red_600_red_400
                        )
                    )
                }

                if (diffDays > 0) {
                    daysLeftInstallAppText.text = StringResourcesUtils.getString(
                        R.string.general_num_days_left,
                        diffDays.toInt()
                    )
                    totalStorage += storageInstallApp
                    totalTransfer += transferInstallApp
                    Timber.d(
                        "After mobile install: storage: %s transfer %s",
                        Util.getSizeString(totalStorage),
                        Util.getSizeString(totalTransfer)
                    )
                } else {
                    daysLeftInstallAppText.background = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.expired_border
                    )
                    figuresInstallAppLayout.alpha = 0.5f
                    installAppIcon.alpha = 0.5f
                    daysLeftInstallAppText.setPadding(
                        Util.scaleWidthPx(8, resources.displayMetrics),
                        Util.scaleHeightPx(4, resources.displayMetrics),
                        Util.scaleWidthPx(8, resources.displayMetrics),
                        Util.scaleHeightPx(4, resources.displayMetrics)
                    )
                    daysLeftInstallAppText.text =
                        StringResourcesUtils.getString(R.string.expired_label)
                }
            } else if (type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE) {
                Timber.d("MEGA_ACHIEVEMENT_ADD_PHONE")
                figuresAddPhoneLayout.visibility = View.VISIBLE
                zeroFiguresAddPhoneText.visibility = View.GONE
                val storageAddPhone = details.getRewardStorageByAwardId(awardId)
                if (storageAddPhone > 0) {
                    figureAddPhoneStorage.text = Util.getSizeString(storageAddPhone)
                    figureAddPhoneStorage.visibility = View.VISIBLE
                    textAddPhoneStorage.visibility = View.VISIBLE
                } else {
                    figureAddPhoneStorage.visibility = View.INVISIBLE
                    textAddPhoneStorage.visibility = View.INVISIBLE
                }

                val transferAddPhone = details.getRewardTransferByAwardId(awardId)
                daysLeftAddPhoneText.visibility = View.VISIBLE
                val daysLeftAddPhone = details.getAwardExpirationTs(i)
                Timber.d("Add phone AwardExpirationTs: $daysLeftAddPhone")

                val start = Util.calculateDateFromTimestamp(daysLeftAddPhone)
                val end = Calendar.getInstance()
                val startDate = start.time
                val endDate = end.time
                val startTime = startDate.time
                val endTime = endDate.time
                val diffTime = startTime - endTime
                val diffDays = diffTime / (1000 * 60 * 60 * 24)

                if (diffDays <= 15) {
                    daysLeftAddPhoneText.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.red_600_red_400
                        )
                    )
                }

                if (diffDays > 0) {
                    daysLeftAddPhoneText.text = StringResourcesUtils.getString(
                        R.string.general_num_days_left,
                        diffDays.toInt()
                    )
                    totalStorage += storageAddPhone
                    totalTransfer += transferAddPhone
                    Timber.d(
                        "After phone added: storage: %s transfer %s",
                        Util.getSizeString(totalStorage),
                        Util.getSizeString(totalTransfer)
                    )
                } else {
                    daysLeftAddPhoneText.background = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.expired_border
                    )
                    figuresAddPhoneLayout.alpha = 0.5f
                    addPhoneIcon.alpha = 0.5f
                    daysLeftAddPhoneText.setPadding(
                        Util.scaleWidthPx(8, resources.displayMetrics),
                        Util.scaleHeightPx(4, resources.displayMetrics),
                        Util.scaleWidthPx(8, resources.displayMetrics),
                        Util.scaleHeightPx(4, resources.displayMetrics)
                    )
                    daysLeftAddPhoneText.text =
                        StringResourcesUtils.getString(R.string.expired_label)
                }
            } else if (type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL) {
                Timber.d("MEGA_ACHIEVEMENT_DESKTOP_INSTALL")
                figuresInstallDesktopLayout.visibility = View.VISIBLE
                zeroFiguresInstallDesktopText.visibility = View.GONE
                val storageInstallDesktop = details.getRewardStorageByAwardId(awardId)
                if (storageInstallDesktop > 0) {
                    figureInstallDesktopStorage.text = Util.getSizeString(storageInstallDesktop)
                    textInstallDesktopStorage.visibility = View.VISIBLE
                    textInstallDesktopStorage.visibility = View.VISIBLE
                } else {
                    figureInstallDesktopStorage.visibility = View.INVISIBLE
                    textInstallDesktopStorage.visibility = View.INVISIBLE
                }

                val transferInstallDesktop = details.getRewardTransferByAwardId(awardId)
                daysLeftInstallDesktopText.visibility = View.VISIBLE
                val daysLeftInstallDesktop = details.getAwardExpirationTs(i)
                Timber.d("Install Desktop AwardExpirationTs: $daysLeftInstallDesktop")

                val start = Util.calculateDateFromTimestamp(daysLeftInstallDesktop)
                val end = Calendar.getInstance()
                val startDate = start.time
                val endDate = end.time
                val startTime = startDate.time
                val endTime = endDate.time
                val diffTime = startTime - endTime
                val diffDays = diffTime / (1000 * 60 * 60 * 24)

                if (diffDays <= 15) {
                    daysLeftInstallDesktopText.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.red_600_red_400
                        )
                    )
                }

                if (diffDays > 0) {
                    daysLeftInstallDesktopText.text = StringResourcesUtils.getString(
                        R.string.general_num_days_left,
                        diffDays.toInt()
                    )
                    totalStorage += storageInstallDesktop
                    totalTransfer += transferInstallDesktop
                    Timber.d(
                        "After desktop install: storage: %s transfer %s",
                        Util.getSizeString(totalStorage),
                        Util.getSizeString(totalTransfer)
                    )
                } else {
                    daysLeftInstallDesktopText.background = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.expired_border
                    )
                    figuresInstallDesktopLayout.alpha = 0.5f
                    installDesktopIcon.alpha = 0.5f
                    daysLeftInstallDesktopText.setPadding(
                        Util.scaleWidthPx(8, resources.displayMetrics),
                        Util.scaleHeightPx(4, resources.displayMetrics),
                        Util.scaleWidthPx(8, resources.displayMetrics),
                        Util.scaleHeightPx(4, resources.displayMetrics)
                    )
                    daysLeftInstallDesktopText.text =
                        StringResourcesUtils.getString(R.string.expired_label)
                }
            } else if (type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME) {
                Timber.d("MEGA_ACHIEVEMENT_WELCOME")
                registrationLayout.visibility = View.VISIBLE
                separatorRegistration.visibility = View.VISIBLE
                val storageRegistration = details.getRewardStorageByAwardId(awardId)
                if (storageRegistration > 0) {
                    figureRegistrationStorage.text = Util.getSizeString(storageRegistration)
                    figureRegistrationStorage.visibility = View.VISIBLE
                    textRegistrationStorage.visibility = View.VISIBLE
                } else {
                    figureRegistrationStorage.visibility = View.INVISIBLE
                    textRegistrationStorage.visibility = View.INVISIBLE
                }

                val transferRegistration = details.getRewardTransferByAwardId(awardId)
                val daysLeftRegistration = details.getAwardExpirationTs(i)
                Timber.d("Registration AwardExpirationTs: $daysLeftRegistration")

                val start = Util.calculateDateFromTimestamp(daysLeftRegistration)
                val end = Calendar.getInstance()
                val startDate = start.time
                val endDate = end.time
                val startTime = startDate.time
                val endTime = endDate.time
                val diffTime = startTime - endTime
                val diffDays = diffTime / (1000 * 60 * 60 * 24)

                if (diffDays <= 15) {
                    daysLeftRegistrationText.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.red_600_red_400
                        )
                    )
                }

                if (diffDays > 0) {
                    daysLeftRegistrationText.text = StringResourcesUtils.getString(
                        R.string.general_num_days_left,
                        diffDays.toInt()
                    )
                    totalStorage += storageRegistration
                    totalTransfer += transferRegistration
                    Timber.d("After desktop install: storage: $totalStorage transfer $totalTransfer")
                } else {
                    daysLeftRegistrationText.background = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.expired_border
                    )
                    figuresRegistrationLayout.alpha = 0.5f
                    registrationIcon.alpha = 0.5f
                    daysLeftRegistrationText.setPadding(
                        Util.scaleWidthPx(8, resources.displayMetrics),
                        Util.scaleHeightPx(4, resources.displayMetrics),
                        Util.scaleWidthPx(8, resources.displayMetrics),
                        Util.scaleHeightPx(4, resources.displayMetrics)
                    )
                    daysLeftRegistrationText.text =
                        StringResourcesUtils.getString(R.string.expired_label)
                }
            } else {
                Timber.d("MEGA_ACHIEVEMENT: $type")
            }
        }

        val storageQuota = details.currentStorage()
        Timber.d("My calculated totalTransfer: $totalStorage")
        figureUnlockedRewardStorage.text = storageQuota.getBigFormattedStorageString()
        Timber.d("My calculated totalTransfer: $totalTransfer")
    }
}
