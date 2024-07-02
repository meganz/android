package mega.privacy.android.app.fragments.homepage.main

import mega.privacy.android.domain.entity.contacts.UserChatStatus

/**
 * Home page ui state
 *
 * @property userChatStatus User chat status
 */
data class HomePageUiState(
    val userChatStatus: UserChatStatus = UserChatStatus.Invalid,
)