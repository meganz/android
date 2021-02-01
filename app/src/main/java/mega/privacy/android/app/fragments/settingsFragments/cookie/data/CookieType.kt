package mega.privacy.android.app.fragments.settingsFragments.cookie.data

enum class CookieType(val value: Int) {
    ESSENTIAL(0), PREFERENCE(1), ANALYTICS(2), ADVERTISEMENT(3), THIRDPARTY(4);

    companion object {
        fun valueOf(type: Int): CookieType =
            values().firstOrNull { it.value == type } ?: ESSENTIAL
    }
}
