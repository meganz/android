package mega.privacy.android.domain.entity.user


/**
 * User update
 *
 * @property changes
 */
data class UserUpdate(val changes: Map<UserId, List<UserChanges>>)
