package mega.privacy.android.domain.entity.user


/**
 * User update
 *
 * @property changes
 * @property emailMap - Some legacy code requires an email
 */
data class UserUpdate(
    val changes: Map<UserId, List<UserChanges>>,
    val emailMap: Map<UserId, String>,
) {
    /**
     * return changes by email instead of user id
     */
    @Deprecated(
        "Only included for legacy code",
        ReplaceWith("changes")
    )
    fun changesByEmail() = changes.mapKeys { emailMap[it.key] }
}
