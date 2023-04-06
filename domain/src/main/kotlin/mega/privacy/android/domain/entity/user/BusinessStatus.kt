package mega.privacy.android.domain.entity.user

enum class BusinessStatus(val value: Int) {
    EXPIRED(-1),
    INACTIVE(0),
    ACTIVE(1),
    GRACE_PERIOD(2)
}