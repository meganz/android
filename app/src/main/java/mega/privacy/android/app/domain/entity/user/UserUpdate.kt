package mega.privacy.android.app.domain.entity.user


/**
 * User update
 *
 * @property changes
 */
data class UserUpdate(val changes: Map<UserId, List<UserChanges>>)
