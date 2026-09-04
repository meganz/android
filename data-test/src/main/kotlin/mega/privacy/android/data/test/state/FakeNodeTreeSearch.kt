package mega.privacy.android.data.test.state

import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSearchFilter

/**
 * Direct children of [filter]'s location that match its predicates — how a [FakeNodeTree] answers
 * `MegaApiGateway.getChildren(filter, ...)`.
 *
 * The SDK requires that call to carry a valid ancestor handle and returns nothing without one, so
 * a filter with no location handle resolves to an empty list here too.
 */
fun FakeNodeTree.childrenMatching(filter: MegaSearchFilter): List<MegaNode> =
    filter.byLocationHandle()
        .takeIf { it != MegaApiJava.INVALID_HANDLE }
        ?.let { locationHandle -> childrenOf(locationHandle).filter { it.matches(filter) } }
        .orEmpty()

/**
 * Every node in [filter]'s scope that matches its predicates — how a [FakeNodeTree] answers
 * `MegaApiGateway.searchWithFilter(filter, ...)`, which walks the scope recursively.
 *
 * The scope is the subtree below the filter's location handle when it carries one, and otherwise
 * the location target it was built with (`byLocation()`).
 *
 * `byCategory()` is deliberately not applied: classifying a node into a `FILE_TYPE_*` bucket needs
 * a MIME database the fake does not have, so a category-scoped read returns the whole scope and
 * leaves the file-type filtering to the production mapper on top of it.
 */
fun FakeNodeTree.nodesMatching(filter: MegaSearchFilter): List<MegaNode> =
    scopeOf(filter).filter { it.matches(filter) }

private fun FakeNodeTree.scopeOf(filter: MegaSearchFilter): List<MegaNode> {
    val locationHandle = filter.byLocationHandle()
    if (locationHandle != MegaApiJava.INVALID_HANDLE) return descendantsOf(locationHandle)
    return when (filter.byLocation()) {
        MegaApiJava.SEARCH_TARGET_ROOTNODE -> descendantsOf(rootNode.handle)
        MegaApiJava.SEARCH_TARGET_INSHARE -> subtreesOf { it.isInShare }
        MegaApiJava.SEARCH_TARGET_OUTSHARE -> subtreesOf { it.isOutShare }
        MegaApiJava.SEARCH_TARGET_PUBLICLINK -> subtreesOf { it.isExported }
        else -> addedNodes()
    }
}

private fun FakeNodeTree.subtreesOf(isRoot: (MegaNode) -> Boolean): List<MegaNode> =
    addedNodes()
        .filter(isRoot)
        .flatMap { root -> listOf(root) + descendantsOf(root.handle) }
        .distinctBy { it.handle }

private fun MegaNode.matches(filter: MegaSearchFilter): Boolean =
    matchesNodeType(filter)
            && matchesFavourite(filter)
            && matchesSensitivity(filter)
            && matchesTimestamps(filter)
            && matchesText(filter)

private fun MegaNode.matchesNodeType(filter: MegaSearchFilter): Boolean =
    when (filter.byNodeType()) {
        MegaNode.TYPE_FILE -> isFile
        MegaNode.TYPE_FOLDER -> isFolder
        else -> true
    }

private fun MegaNode.matchesFavourite(filter: MegaSearchFilter): Boolean =
    when (filter.byFavourite()) {
        MegaSearchFilter.BOOL_FILTER_ONLY_TRUE -> isFavourite
        MegaSearchFilter.BOOL_FILTER_ONLY_FALSE -> !isFavourite
        else -> true
    }

private fun MegaNode.matchesSensitivity(filter: MegaSearchFilter): Boolean =
    when (filter.bySensitivity()) {
        MegaSearchFilter.BOOL_FILTER_ONLY_TRUE -> isMarkedSensitive
        MegaSearchFilter.BOOL_FILTER_ONLY_FALSE -> !isMarkedSensitive
        else -> true
    }

private fun MegaNode.matchesTimestamps(filter: MegaSearchFilter): Boolean =
    isWithin(
        creationTime,
        filter.byCreationTimeLowerLimit(),
        filter.byCreationTimeUpperLimit(),
    ) && isWithin(
        modificationTime,
        filter.byModificationTimeLowerLimit(),
        filter.byModificationTimeUpperLimit(),
    )

private fun isWithin(timestamp: Long, lowerLimit: Long, upperLimit: Long): Boolean =
    (lowerLimit <= 0L || timestamp >= lowerLimit) && (upperLimit <= 0L || timestamp <= upperLimit)

/**
 * Name, description and tag are combined with the operator the filter was built with
 * (`useAndForTextQuery()`); a term left unset takes no part in the decision.
 */
private fun MegaNode.matchesText(filter: MegaSearchFilter): Boolean {
    val outcomes = listOfNotNull(
        filter.byName().asSearchTerm()?.let { term -> name.contains(term, ignoreCase = true) },
        filter.byDescription().asSearchTerm()
            ?.let { term -> description?.contains(term, ignoreCase = true) == true },
        filter.byTag().asSearchTerm()?.let { term -> hasTag(term) },
    )
    if (outcomes.isEmpty()) return true
    return if (filter.useAndForTextQuery()) outcomes.all { it } else outcomes.any { it }
}

private fun MegaNode.hasTag(tag: String): Boolean {
    val nodeTags = tags ?: return false
    return (0 until nodeTags.size()).any { nodeTags.get(it).equals(tag, ignoreCase = true) }
}

private fun String?.asSearchTerm(): String? = this?.takeIf { it.isNotEmpty() }
