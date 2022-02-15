package mega.privacy.android.app.presentation.home.model

data class HomeState(
    val unreadNotificationsCount: Int = 0,
    val displayChatCount: Boolean = false,
    val displayCallBadge: Boolean = false,
)
