package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaStringList

/**
 * In-memory stub of [MegaNode] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaNode(
    private val handle: Long = -1L,
    private val name: String = "",
    private val parentHandle: Long = -1L,
    private val isFolder: Boolean = false,
    private val size: Long = 0L,
    private val creationTime: Long = 0L,
    private val modificationTime: Long = 0L,
    private val fingerprint: String? = null,
    private val originalFingerprint: String? = null,
    private val label: Int = 0,
    private val duration: Int = -1,
    private val isFavourite: Boolean = false,
    private val isMarkedSensitive: Boolean = false,
    private val isExported: Boolean = false,
    private val isTakenDown: Boolean = false,
    private val isInShare: Boolean = false,
    private val isOutShare: Boolean = false,
    private val publicLink: String? = null,
    private val base64Handle: String? = null,
    private val description: String? = null,
    private val owner: Long = -1L,
    private val restoreHandle: Long = -1L,
    private val publicHandle: Long = -1L,
    private val changes: Long = 0L,
) : MegaNode(0, false) {

    override fun delete() = Unit

    override fun getType(): Int = if (isFolder) MegaNode.TYPE_FOLDER else MegaNode.TYPE_FILE
    override fun getName(): String = name
    override fun getFingerprint(): String? = fingerprint
    override fun getOriginalFingerprint(): String? = originalFingerprint
    override fun hasCustomAttrs(): Boolean = false
    override fun getCustomAttrNames(): MegaStringList? = null
    override fun getCustomAttr(p0: String?): String? = null
    override fun getDuration(): Int = duration
    override fun getWidth(): Int = 0
    override fun getHeight(): Int = 0
    override fun getShortformat(): Int = 0
    override fun getVideocodecid(): Int = 0
    override fun isFavourite(): Boolean = isFavourite
    override fun isMarkedSensitive(): Boolean = isMarkedSensitive
    override fun getLabel(): Int = label
    override fun getLatitude(): Double = 0.0
    override fun getLongitude(): Double = 0.0
    override fun getDescription(): String? = description
    override fun getTags(): MegaStringList? = null
    /**
     * Defaults to a base64-url encoding of the decimal handle (matching the fake gateway's
     * handleToBase64 default) — the real SDK never returns null and mappers rely on that.
     */
    override fun getBase64Handle(): String = base64Handle
        ?: java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(handle.toString().toByteArray())
    override fun getSize(): Long = size
    override fun getCreationTime(): Long = creationTime
    override fun getModificationTime(): Long = modificationTime
    override fun getHandle(): Long = handle
    override fun getRestoreHandle(): Long = restoreHandle
    override fun getParentHandle(): Long = parentHandle
    override fun getBase64Key(): String? = null
    override fun getExpirationTime(): Long = 0L
    override fun getPublicHandle(): Long = publicHandle
    override fun getPublicNode(): MegaNode? = null
    override fun getPublicLink(p0: Boolean): String? = publicLink
    override fun getPublicLink(): String? = publicLink
    override fun getPublicLinkCreationTime(): Long = 0L
    override fun getWritableLinkAuthKey(): String? = null
    override fun isFile(): Boolean = !isFolder
    override fun isFolder(): Boolean = isFolder
    override fun isRemoved(): Boolean = false
    override fun hasChanged(p0: Long): Boolean = (changes and p0) != 0L
    override fun getChanges(): Long = changes
    override fun hasThumbnail(): Boolean = false
    override fun hasPreview(): Boolean = false
    override fun isPublic(): Boolean = false
    override fun isShared(): Boolean = isInShare || isOutShare
    override fun isOutShare(): Boolean = isOutShare
    override fun isInShare(): Boolean = isInShare
    override fun isExported(): Boolean = isExported
    override fun isExpired(): Boolean = false
    override fun isTakenDown(): Boolean = isTakenDown
    override fun isForeign(): Boolean = false
    override fun isCreditCardNode(): Boolean = false
    override fun isPasswordNode(): Boolean = false
    override fun isPasswordManagerNode(): Boolean = false
    override fun getCreditCardData(): MegaNode.CreditCardNodeData? = null
    override fun getPasswordData(): MegaNode.PasswordNodeData? = null
    override fun isNodeKeyDecrypted(): Boolean = true
    override fun getFileAttrString(): String? = null
    override fun setPrivateAuth(p0: String?) = Unit
    override fun getChildren(): MegaNodeList? = null
    override fun getOwner(): Long = owner
    override fun getDeviceId(): String? = null
    override fun getS4(): String? = null
    override fun serialize(): String? = null
}
