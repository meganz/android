package mega.privacy.android.domain.entity.settings.cookie

enum class CookieType(val value: Int) {
    ESSENTIAL(0), PREFERENCE(1), ANALYTICS(2), ADVERTISEMENT(3), THIRDPARTY(4), ADS_CHECK(5);

    companion object {
        fun valueOf(type: Int): CookieType =
            entries.firstOrNull { it.value == type } ?: ESSENTIAL
    }
}