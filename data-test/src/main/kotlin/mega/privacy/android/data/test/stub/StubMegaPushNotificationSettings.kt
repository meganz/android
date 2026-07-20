package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaPushNotificationSettings

/**
 * In-memory stub of [MegaPushNotificationSettings] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaPushNotificationSettings(
    private var globalDndEnabled: Boolean = false,
    private var globalDnd: Long = -1L,
    private var globalChatsDndEnabled: Boolean = false,
    private var globalChatsDnd: Long = -1L,
    private var globalScheduleEnabled: Boolean = false,
    private var globalScheduleStart: Int = 0,
    private var globalScheduleEnd: Int = 0,
    private var globalScheduleTimezone: String? = null,
    private var contactsEnabled: Boolean = false,
    private var sharesEnabled: Boolean = false,
) : MegaPushNotificationSettings(0, false) {

    override fun delete() = Unit

    override fun isGlobalDndEnabled(): Boolean = globalDndEnabled
    override fun isGlobalChatsDndEnabled(): Boolean = globalChatsDndEnabled
    override fun getGlobalDnd(): Long = globalDnd
    override fun isGlobalScheduleEnabled(): Boolean = globalScheduleEnabled
    override fun getGlobalScheduleStart(): Int = globalScheduleStart
    override fun getGlobalScheduleEnd(): Int = globalScheduleEnd
    override fun getGlobalScheduleTimezone(): String? = globalScheduleTimezone
    override fun isChatDndEnabled(p0: Long): Boolean = false
    override fun getChatDnd(p0: Long): Long = -1L
    override fun isChatAlwaysNotifyEnabled(p0: Long): Boolean = false
    override fun isContactsEnabled(): Boolean = contactsEnabled
    override fun isSharesEnabled(): Boolean = sharesEnabled
    override fun getGlobalChatsDnd(): Long = globalChatsDnd
    override fun enableGlobal(p0: Boolean) = Unit
    override fun setGlobalDnd(p0: Long) {
        globalDnd = p0
        globalDndEnabled = true
    }
    override fun disableGlobalDnd() {
        globalDnd = -1L
        globalDndEnabled = false
    }
    override fun setGlobalSchedule(p0: Int, p1: Int, p2: String?) {
        globalScheduleStart = p0
        globalScheduleEnd = p1
        globalScheduleTimezone = p2
        globalScheduleEnabled = true
    }
    override fun disableGlobalSchedule() {
        globalScheduleEnabled = false
    }
    override fun enableChat(p0: Long, p1: Boolean) = Unit
    override fun setChatDnd(p0: Long, p1: Long) = Unit
    override fun setGlobalChatsDnd(p0: Long) {
        globalChatsDnd = p0
        globalChatsDndEnabled = true
    }
    override fun enableChatAlwaysNotify(p0: Long, p1: Boolean) = Unit
    override fun enableContacts(p0: Boolean) {
        contactsEnabled = p0
    }
    override fun enableShares(p0: Boolean) {
        sharesEnabled = p0
    }
    override fun enableChats(p0: Boolean) = Unit
}
