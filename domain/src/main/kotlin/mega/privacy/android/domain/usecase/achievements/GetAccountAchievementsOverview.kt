package mega.privacy.android.domain.usecase.achievements

import mega.privacy.android.domain.entity.achievement.AchievementsOverview

fun interface GetAccountAchievementsOverview {

    suspend operator fun invoke(): AchievementsOverview
}
