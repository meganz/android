package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSearchFilter

/**
 * In-memory stub of [MegaSearchFilter] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Each field is seeded from a
 * constructor parameter defaulting to the SDK's "not filtered" sentinel, and the `by*` setters
 * update it, so the stub answers its getters exactly as a real filter built through
 * `MegaSearchFilter.createInstance()` would.
 */
class StubMegaSearchFilter(
    private var name: String = "",
    private var nodeType: Int = MegaNode.TYPE_UNKNOWN,
    private var category: Int = MegaApiJava.FILE_TYPE_DEFAULT,
    private var favourite: Int = MegaSearchFilter.BOOL_FILTER_DISABLED,
    private var sensitivity: Int = MegaSearchFilter.BOOL_FILTER_DISABLED,
    private var locationHandle: Long = MegaApiJava.INVALID_HANDLE,
    private var location: Int = MegaApiJava.SEARCH_TARGET_ALL,
    private var creationTimeLowerLimit: Long = 0L,
    private var creationTimeUpperLimit: Long = 0L,
    private var modificationTimeLowerLimit: Long = 0L,
    private var modificationTimeUpperLimit: Long = 0L,
    private var description: String? = null,
    private var tag: String? = null,
    private var useAndForText: Boolean = true,
) : MegaSearchFilter(0, false) {

    override fun delete() = Unit

    override fun byName(searchString: String?) {
        name = searchString.orEmpty()
    }

    override fun byNodeType(nodeType: Int) {
        this.nodeType = nodeType
    }

    override fun byCategory(mimeType: Int) {
        category = mimeType
    }

    override fun byFavourite(boolFilterOption: Int) {
        favourite = boolFilterOption
    }

    override fun bySensitivity(boolFilterOption: Int) {
        sensitivity = boolFilterOption
    }

    override fun byLocationHandle(ancestorHandle: Long) {
        locationHandle = ancestorHandle
    }

    override fun byLocation(locationType: Int) {
        location = locationType
    }

    override fun byCreationTime(lowerLimit: Long, upperLimit: Long) {
        creationTimeLowerLimit = lowerLimit
        creationTimeUpperLimit = upperLimit
    }

    override fun byModificationTime(lowerLimit: Long, upperLimit: Long) {
        modificationTimeLowerLimit = lowerLimit
        modificationTimeUpperLimit = upperLimit
    }

    override fun byDescription(searchString: String?) {
        description = searchString
    }

    override fun byTag(searchString: String?) {
        tag = searchString
    }

    override fun useAndForTextQuery(useAnd: Boolean) {
        useAndForText = useAnd
    }

    override fun byName(): String = name
    override fun byNodeType(): Int = nodeType
    override fun byCategory(): Int = category
    override fun byFavourite(): Int = favourite
    override fun bySensitivity(): Int = sensitivity
    override fun byLocationHandle(): Long = locationHandle
    override fun byLocation(): Int = location
    override fun byCreationTimeLowerLimit(): Long = creationTimeLowerLimit
    override fun byCreationTimeUpperLimit(): Long = creationTimeUpperLimit
    override fun byModificationTimeLowerLimit(): Long = modificationTimeLowerLimit
    override fun byModificationTimeUpperLimit(): Long = modificationTimeUpperLimit
    override fun byDescription(): String? = description
    override fun byTag(): String? = tag
    override fun useAndForTextQuery(): Boolean = useAndForText
}
