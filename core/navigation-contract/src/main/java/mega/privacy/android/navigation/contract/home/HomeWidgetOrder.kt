package mega.privacy.android.navigation.contract.home

/**
 * Centralised default ordering for [HomeWidget] implementations on the Home screen.
 * Earlier-declared entries appear earlier in the default layout (their [ordinal] is used as the sort key).
 */
enum class HomeWidgetOrder {
    Banner,
    Shortcuts,
    Recents,
    MyAccount,
    ViewedLinks,
    ContinueWhereLeftOff,
}
