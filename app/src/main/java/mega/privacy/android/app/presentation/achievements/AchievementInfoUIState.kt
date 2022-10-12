package mega.privacy.android.app.presentation.achievements

/**
 * UI state for AchievementInfo screen
 *
 * @param toolbarTitle : Toolbar title
 * @param achievementType : Achievement type
 * @param infoAchievementsTitle : Achievement Title
 * @param firstParagraphText : First paragraph text
 * @param checkIconVisible : boolean to decide icon visibility
 * @param sectionTitleVisible : boolean to decide section title visibility
 * @param secondParagraphVisible : boolean to decide second paragraph visibility
 */
data class AchievementInfoUIState(
    val toolbarTitle: String = "",
    val achievementType: Int = 0,
    val infoAchievementsTitle: String = "",
    val firstParagraphText: String = "",
    val checkIconVisible: Boolean = false,
    val sectionTitleVisible: Boolean = false,
    val secondParagraphVisible: Boolean = false,
)
