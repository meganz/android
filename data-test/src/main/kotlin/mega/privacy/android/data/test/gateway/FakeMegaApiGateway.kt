package mega.privacy.android.data.test.gateway

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.model.RequestEvent
import mega.privacy.android.data.test.engine.FakeGatewayEngine
import mega.privacy.android.data.test.engine.FakeGatewayStubbing
import mega.privacy.android.data.test.state.FakeAccountState
import mega.privacy.android.data.test.state.FakeNodeTree
import mega.privacy.android.data.test.stub.StubMegaAccountDetails
import mega.privacy.android.data.test.stub.StubMegaCancelToken
import mega.privacy.android.data.test.stub.StubMegaError
import mega.privacy.android.data.test.stub.StubMegaPushNotificationSettings
import mega.privacy.android.data.test.stub.StubMegaRequest
import mega.privacy.android.data.test.stub.StubMegaSetElementList
import mega.privacy.android.data.test.stub.StubMegaSetList
import mega.privacy.android.data.test.stub.StubMegaSyncList
import mega.privacy.android.data.test.stub.StubMegaTransfer
import mega.privacy.android.data.test.stub.StubMegaUser
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaDateSectionList
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaFileServiceReclaimOptions
import nz.mega.sdk.MegaFlag
import nz.mega.sdk.MegaGroupNodesByDateFilter
import nz.mega.sdk.MegaHandleList
import nz.mega.sdk.MegaListAllNodesFilter
import nz.mega.sdk.MegaLoggerInterface
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaPushNotificationSettings
import nz.mega.sdk.MegaRecentActionBucket
import nz.mega.sdk.MegaRecentActionBucketList
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaSearchFilter
import nz.mega.sdk.MegaSearchPage
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElementList
import nz.mega.sdk.MegaSetList
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaStringList
import nz.mega.sdk.MegaStringMap
import nz.mega.sdk.MegaSyncList
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransferData
import nz.mega.sdk.MegaTransferListenerInterface
import nz.mega.sdk.MegaUploadOptions
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import java.io.File
import java.util.Base64
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction10
import kotlin.reflect.KFunction3
import kotlin.reflect.KFunction4
import kotlin.reflect.KFunction5
import kotlin.reflect.KFunction6
import kotlin.reflect.KSuspendFunction1
import kotlin.reflect.KSuspendFunction2

private const val DETAILS_FLAG_STORAGE = 0x01
private const val DETAILS_FLAG_TRANSFER = 0x02
private const val DETAILS_FLAG_PRO = 0x04
private const val DETAILS_FLAG_SESSIONS = 0x20

/**
 * WireMock-style in-process fake of [MegaApiGateway] for automated tests.
 *
 * Every gateway call is recorded and answered by a common-sense default, so tests only configure
 * the behaviour they care about:
 * - **Stub** results per method (optionally per arguments) via the [FakeGatewayStubbing] surface
 *   (`stub`, `stubResult`, `stubError`) or, for listener-based commands, via [stubRequest] /
 *   [stubTransfer].
 * - **Verify** calls through [invocations] / [invocationsOf].
 * - **Emit** SDK events into the gateway flows via [emitGlobalUpdate], [emitGlobalTransfer] and
 *   [emitRequestEvent].
 * - **Reset** everything back to defaults with [resetToDefaults].
 *
 * Default behaviour: the fake represents a logged-in account (see [FakeAccountState]) with a node
 * tree seeded with Cloud Drive / Rubbish Bin / Vault roots (see [FakeNodeTree]). Node reads resolve
 * through [nodeTree], account reads through [account]; other queries return empty/neutral values
 * and listener-based commands complete successfully with API_OK. No deep SDK behaviour is
 * simulated: commands such as copy/move/delete never mutate [nodeTree]; tests needing tree changes
 * mutate it directly or stub the relevant reads.
 *
 * The [MegaApiJava] instance passed to listener callbacks is an inert, uninitialised object
 * ([inertMegaApiJava]); listeners must not call methods on it.
 *
 * Example:
 * ```
 * val gateway = FakeMegaApiGateway()
 * gateway.stubResult(MegaApiGateway::getNumUnreadUserAlerts, 3)
 * gateway.stubRequest(MegaApiGateway::login, error = StubMegaError(MegaError.API_EARGS))
 *
 * underTest.doSomething()
 *
 * assertThat(gateway.invocationsOf(MegaApiGateway::login)).hasSize(1)
 * gateway.emitGlobalUpdate(GlobalUpdate.OnReloadNeeded)
 * ```
 */
class FakeMegaApiGateway(
    private val engine: FakeGatewayEngine = FakeGatewayEngine(),
    val account: FakeAccountState = FakeAccountState(),
    val nodeTree: FakeNodeTree = FakeNodeTree(),
) : MegaApiGateway, FakeGatewayStubbing by engine {

    private val globalUpdatesFlow = MutableSharedFlow<GlobalUpdate>(extraBufferCapacity = 64)
    private val globalTransferFlow = MutableSharedFlow<GlobalTransfer>(extraBufferCapacity = 64)
    private val globalRequestEventsFlow = MutableSharedFlow<RequestEvent>(extraBufferCapacity = 64)

    /** Emits [update] to collectors of [globalUpdates]. */
    suspend fun emitGlobalUpdate(update: GlobalUpdate) {
        globalUpdatesFlow.emit(update)
    }

    /** Emits [event] to collectors of [globalTransfer]. */
    suspend fun emitGlobalTransfer(event: GlobalTransfer) {
        globalTransferFlow.emit(event)
    }

    /** Emits [event] to collectors of [globalRequestEvents]. */
    suspend fun emitRequestEvent(event: RequestEvent) {
        globalRequestEventsFlow.emit(event)
    }

    /**
     * Stubs the outcome delivered to the listener of a listener-based request method.
     *
     * ```
     * gateway.stubRequest(
     *     MegaApiGateway::login,
     *     error = StubMegaError(MegaError.API_EMFAREQUIRED),
     * )
     * ```
     */
    fun stubRequest(
        method: KFunction<*>,
        error: MegaError = StubMegaError(MegaError.API_OK),
        request: MegaRequest? = null,
        matcher: (List<Any?>) -> Boolean = { true },
    ) = engine.stubRequestOutcome(method, matcher, MegaRequestOutcome(request, error))

    /**
     * Stubs the outcome delivered to the listener of a transfer-listener-based method
     * (uploads, downloads, [getFullImage]).
     */
    fun stubTransfer(
        method: KFunction<*>,
        error: MegaError = StubMegaError(MegaError.API_OK),
        transfer: MegaTransfer? = null,
        matcher: (List<Any?>) -> Boolean = { true },
    ) = engine.stubRequestOutcome(method, matcher, MegaTransferOutcome(transfer, error))

    /**
     * Stubs a transfer-listener-based method with progressive delivery, simulating a large
     * transfer: onTransferStart with the first of [steps], onTransferUpdate for each remaining
     * step every [stepDelayMs] (on a background thread), then onTransferFinish with
     * [finalTransfer] and [error].
     */
    fun stubTransferScript(
        method: KFunction<*>,
        steps: List<MegaTransfer>,
        finalTransfer: MegaTransfer,
        error: MegaError = StubMegaError(MegaError.API_OK),
        stepDelayMs: Long = 500L,
        matcher: (List<Any?>) -> Boolean = { true },
    ) = engine.stubRequestOutcome(
        method,
        matcher,
        MegaTransferScriptOutcome(steps, finalTransfer, error, stepDelayMs),
    )

    /** Clears stubs, recorded invocations, account state and the node tree back to defaults. */
    fun resetToDefaults() {
        engine.reset()
        nodeTree.clear()
        account.isLoggedIn = true
        account.email = "test@mega.nz"
        account.myUserHandle = 111L
        account.session = "fake-session"
        account.isBusinessAccount = false
        account.isAchievementsEnabled = true
    }

    private fun completeRequest(
        method: KFunction<*>,
        arguments: List<Any?>,
        listener: MegaRequestListenerInterface?,
        requestType: Int,
        defaultRequest: MegaRequest? = null,
    ) {
        engine.record(method, arguments)
        val outcome = engine.requestOutcomeFor(method, arguments) as? MegaRequestOutcome
            ?: MegaRequestOutcome(request = null, error = StubMegaError(MegaError.API_OK))
        if (listener == null) return
        val request = outcome.request ?: defaultRequest ?: StubMegaRequest(type = requestType)
        listener.onRequestStart(inertMegaApiJava, request)
        listener.onRequestFinish(inertMegaApiJava, request, outcome.error)
    }

    private fun completeTransfer(
        method: KFunction<*>,
        arguments: List<Any?>,
        listener: MegaTransferListenerInterface?,
    ) {
        engine.record(method, arguments)
        when (val outcome = engine.requestOutcomeFor(method, arguments)) {
            is MegaTransferScriptOutcome -> runTransferScript(listener, outcome)
            is MegaTransferOutcome -> deliverTransferOutcome(listener, outcome)
            else -> deliverTransferOutcome(
                listener,
                MegaTransferOutcome(transfer = null, error = StubMegaError(MegaError.API_OK)),
            )
        }
    }

    private fun deliverTransferOutcome(
        listener: MegaTransferListenerInterface?,
        outcome: MegaTransferOutcome,
    ) {
        val transfer = outcome.transfer ?: StubMegaTransfer()
        notifyTransferStart(listener, transfer)
        notifyTransferFinish(listener, transfer, outcome.error)
    }

    private fun runTransferScript(
        listener: MegaTransferListenerInterface?,
        outcome: MegaTransferScriptOutcome,
    ) {
        Thread {
            notifyTransferStart(listener, outcome.steps.first())
            outcome.steps.drop(1).forEach { step ->
                Thread.sleep(outcome.stepDelayMs)
                notifyTransferUpdate(listener, step)
            }
            Thread.sleep(outcome.stepDelayMs)
            notifyTransferFinish(listener, outcome.finalTransfer, outcome.error)
        }.apply {
            isDaemon = true
            name = "fake-transfer-script"
        }.start()
    }

    // The real SDK reports transfer callbacks both to the per-call listener and to global
    // transfer listeners, so every notification is mirrored to [globalTransfer].
    private fun notifyTransferStart(
        listener: MegaTransferListenerInterface?,
        transfer: MegaTransfer,
    ) {
        listener?.onTransferStart(inertMegaApiJava, transfer)
        globalTransferFlow.tryEmit(GlobalTransfer.OnTransferStart(transfer))
    }

    private fun notifyTransferUpdate(
        listener: MegaTransferListenerInterface?,
        transfer: MegaTransfer,
    ) {
        listener?.onTransferUpdate(inertMegaApiJava, transfer)
        globalTransferFlow.tryEmit(GlobalTransfer.OnTransferUpdate(transfer))
    }

    private fun notifyTransferFinish(
        listener: MegaTransferListenerInterface?,
        transfer: MegaTransfer,
        error: MegaError,
    ) {
        listener?.onTransferFinish(inertMegaApiJava, transfer, error)
        globalTransferFlow.tryEmit(GlobalTransfer.OnTransferFinish(transfer, error))
    }

    /**
     * Default outcome for user-attribute requests: echoes the requested attribute back as the
     * request's paramType — repository listeners filter on it and would otherwise never resume.
     */
    private fun userAttributeRequest(attributeType: Int, email: String? = null): MegaRequest =
        StubMegaRequest(
            type = MegaRequest.TYPE_GET_ATTR_USER,
            paramType = attributeType,
            email = email,
        )

    /**
     * Default outcome for account-details requests: a free account with nothing used, so
     * account-detail dependent features (e.g. hidden nodes gating) resolve out of the box.
     * The [numDetails] flag bits mirror the ones the data layer's AccountDetailMapper reads.
     */
    private fun accountDetailsRequest(numDetails: Int): MegaRequest = StubMegaRequest(
        type = MegaRequest.TYPE_ACCOUNT_DETAILS,
        numDetails = numDetails,
        megaAccountDetails = StubMegaAccountDetails(),
    )

    private fun loggedInUserOrNull(): MegaUser? = if (account.isLoggedIn) {
        StubMegaUser(handle = account.myUserHandle, email = account.email)
    } else {
        null
    }

    private fun currentBusinessStatus(): Int = if (account.isBusinessAccount) {
        MegaApiJava.BUSINESS_STATUS_ACTIVE
    } else {
        MegaApiJava.BUSINESS_STATUS_INACTIVE
    }

    private fun isInSubtree(node: MegaNode, ancestorHandle: Long): Boolean {
        val visited = mutableSetOf<Long>()
        var current: MegaNode? = node
        while (current != null && visited.add(current.handle)) {
            if (current.handle == ancestorHandle) return true
            current = nodeTree.nodeByHandle(current.parentHandle)
        }
        return false
    }

    private fun encodeHandle(handle: Long): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(handle.toString().toByteArray())

    private fun decodeHandle(base64Handle: String): Long = runCatching {
        String(Base64.getUrlDecoder().decode(base64Handle)).toLong()
    }.getOrDefault(MegaApiJava.INVALID_HANDLE)

    @Suppress("DEPRECATION")
    private val startUploadDeprecatedRef: KFunction10<MegaApiGateway, String, MegaNode, String?, Long?, String?, Boolean, Boolean, MegaCancelToken?, MegaTransferListenerInterface, Unit> =
        MegaApiGateway::startUpload

    private val startUploadRef: KFunction6<MegaApiGateway, String, MegaNode, MegaCancelToken?, MegaUploadOptions, MegaTransferListenerInterface, Unit> =
        MegaApiGateway::startUpload

    private val getPublicLinksNullableOrderRef: KSuspendFunction2<MegaApiGateway, Int?, List<MegaNode>> =
        MegaApiGateway::getPublicLinks

    private val getUserAvatarColorByUserRef: KSuspendFunction2<MegaApiGateway, MegaUser, String?> =
        MegaApiGateway::getUserAvatarColor

    private val getUserAvatarColorByHandleRef: KSuspendFunction2<MegaApiGateway, Long, String?> =
        MegaApiGateway::getUserAvatarColor

    private val getPublicLinksRef: KSuspendFunction1<MegaApiGateway, List<MegaNode>> =
        MegaApiGateway::getPublicLinks

    private val getUserAttributeByEmailRef: KFunction4<MegaApiGateway, String, Int, MegaRequestListenerInterface, Unit> =
        MegaApiGateway::getUserAttribute

    private val getUserAttributeByUserRef: KFunction4<MegaApiGateway, MegaUser, Int, MegaRequestListenerInterface, Unit> =
        MegaApiGateway::getUserAttribute

    private val getUserAttributeByIdRef: KFunction3<MegaApiGateway, Int, MegaRequestListenerInterface, Unit> =
        MegaApiGateway::getUserAttribute

    private val getPublicLinksOrderRef: KSuspendFunction2<MegaApiGateway, Int, List<MegaNode>> =
        MegaApiGateway::getPublicLinks

    private val getInSharesByOrderRef: KSuspendFunction2<MegaApiGateway, Int, List<MegaNode>> =
        MegaApiGateway::getInShares

    private val getInSharesByUserRef: KSuspendFunction2<MegaApiGateway, MegaUser, List<MegaNode>> =
        MegaApiGateway::getInShares

    private val getOutSharesByOrderRef: KSuspendFunction2<MegaApiGateway, Int, List<MegaShare>> =
        MegaApiGateway::getOutShares

    private val getOutSharesByNodeRef: KSuspendFunction2<MegaApiGateway, MegaNode, List<MegaShare>> =
        MegaApiGateway::getOutShares

    private val updateSetNameWithListenerRef: KFunction4<MegaApiGateway, Long, String?, MegaRequestListenerInterface?, Unit> =
        MegaApiGateway::updateSetName

    private val updateSetNameRef: KFunction3<MegaApiGateway, Long, String?, Unit> =
        MegaApiGateway::updateSetName

    private val inviteContactRef: KFunction3<MegaApiGateway, String, MegaRequestListenerInterface, Unit> =
        MegaApiGateway::inviteContact

    private val inviteContactWithMessageRef: KFunction5<MegaApiGateway, String, Long, String?, MegaRequestListenerInterface, Unit> =
        MegaApiGateway::inviteContact

    private val setUserAttributeStringRef: KFunction4<MegaApiGateway, Int, String, MegaRequestListenerInterface, Unit> =
        MegaApiGateway::setUserAttribute

    private val setUserAttributeStringMapRef: KFunction4<MegaApiGateway, Int, MegaStringMap, MegaRequestListenerInterface, Unit> =
        MegaApiGateway::setUserAttribute

    override fun getWaitingReason(): Int =
        engine.dispatchBlocking(MegaApiGateway::getWaitingReason, emptyList()) {
            MegaApiJava.RETRY_NONE
        }

    override fun getInvalidHandle(): Long =
        engine.dispatchBlocking(MegaApiGateway::getInvalidHandle, emptyList()) {
            MegaApiJava.INVALID_HANDLE
        }

    override fun getInvalidAffiliateType(): Int =
        engine.dispatchBlocking(MegaApiGateway::getInvalidAffiliateType, emptyList()) {
            MegaApiJava.AFFILIATE_TYPE_INVALID
        }

    override fun getInvalidBackupType(): Int =
        engine.dispatchBlocking(MegaApiGateway::getInvalidBackupType, emptyList()) {
            MegaApiJava.BACKUP_TYPE_INVALID
        }

    override fun multiFactorAuthAvailable(): Boolean =
        engine.dispatchBlocking(MegaApiGateway::multiFactorAuthAvailable, emptyList()) { true }

    override fun multiFactorAuthEnabled(email: String?, listener: MegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::multiFactorAuthEnabled,
            listOf(email, listener),
            listener,
            MegaRequest.TYPE_MULTI_FACTOR_AUTH_CHECK,
        )
    }

    override fun cancelAccount(listener: MegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::cancelAccount,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_CANCEL_LINK,
        )
    }

    override fun createSupportTicket(
        ticketContent: String,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::createSupportTicket,
            listOf(ticketContent, listener),
            listener,
            MegaRequest.TYPE_SUPPORT_TICKET,
        )
    }

    override fun getNodesFromMegaNodeList(nodeList: MegaNodeList): List<MegaNode> =
        engine.dispatchBlocking(MegaApiGateway::getNodesFromMegaNodeList, listOf(nodeList)) {
            (0 until nodeList.size()).mapNotNull { i -> nodeList.get(i) }
        }

    @Deprecated(message = "This will be removed soon. Use startUpload with MegaUploadOptions param instead.")
    override fun startUpload(
        localPath: String,
        parent: MegaNode,
        fileName: String?,
        mtime: Long?,
        appData: String?,
        isSourceTemporary: Boolean,
        startFirst: Boolean,
        cancelToken: MegaCancelToken?,
        listener: MegaTransferListenerInterface,
    ) {
        completeTransfer(
            startUploadDeprecatedRef,
            listOf(localPath, parent, fileName, mtime, appData, isSourceTemporary, startFirst, cancelToken, listener),
            listener,
        )
    }

    override fun startUpload(
        localPath: String,
        parent: MegaNode,
        cancelToken: MegaCancelToken?,
        options: MegaUploadOptions,
        listener: MegaTransferListenerInterface,
    ) {
        completeTransfer(
            startUploadRef,
            listOf(localPath, parent, cancelToken, options, listener),
            listener,
        )
    }

    override fun addTransferListener(listener: MegaTransferListenerInterface) {
        engine.dispatchBlocking(MegaApiGateway::addTransferListener, listOf(listener)) { }
    }

    override fun removeTransferListener(listener: MegaTransferListenerInterface) {
        engine.dispatchBlocking(MegaApiGateway::removeTransferListener, listOf(listener)) { }
    }

    override fun startUploadForSupport(path: String, listener: MegaTransferListenerInterface) {
        completeTransfer(MegaApiGateway::startUploadForSupport, listOf(path, listener), listener)
    }

    override val myUserHandle: Long
        get() = account.myUserHandle

    override val myUser: MegaUser?
        get() = loggedInUserOrNull()

    override val accountEmail: String?
        get() = account.email.takeIf { account.isLoggedIn }

    override val isBusinessAccount: Boolean
        get() = account.isBusinessAccount

    override suspend fun isMasterBusinessAccount(): Boolean =
        engine.dispatch(MegaApiGateway::isMasterBusinessAccount, emptyList()) { false }

    /** Active when the fake account is a business account, inactive otherwise. */
    override suspend fun getBusinessStatus(): Int =
        engine.dispatch(MegaApiGateway::getBusinessStatus, emptyList()) { currentBusinessStatus() }

    override val isEphemeralPlusPlus: Boolean
        get() = false

    override suspend fun getAccountAuth(): String? =
        engine.dispatch(MegaApiGateway::getAccountAuth, emptyList()) { account.session }

    override val myCredentials: String?
        get() = "fake-credentials".takeIf { account.isLoggedIn }

    override val dumpSession: String?
        get() = account.session.takeIf { account.isLoggedIn }

    override val businessStatus: Int
        get() = currentBusinessStatus()

    override val isAchievementsEnabled: Boolean
        get() = account.isAchievementsEnabled

    override suspend fun areUploadTransfersPaused(): Boolean =
        engine.dispatch(MegaApiGateway::areUploadTransfersPaused, emptyList()) { false }

    override suspend fun areDownloadTransfersPaused(): Boolean =
        engine.dispatch(MegaApiGateway::areDownloadTransfersPaused, emptyList()) { false }

    override suspend fun getRootNode(): MegaNode? =
        engine.dispatch(MegaApiGateway::getRootNode, emptyList()) { nodeTree.rootNode }

    override suspend fun getParentNode(node: MegaNode): MegaNode? =
        engine.dispatch(MegaApiGateway::getParentNode, listOf(node)) {
            nodeTree.nodeByHandle(node.parentHandle)
        }

    override suspend fun getChildNode(parentNode: MegaNode?, name: String?): MegaNode? =
        engine.dispatch(MegaApiGateway::getChildNode, listOf(parentNode, name)) {
            parentNode?.let { parent -> nodeTree.childrenOf(parent.handle).firstOrNull { child -> child.name == name } }
        }

    override suspend fun getRubbishBinNode(): MegaNode? =
        engine.dispatch(MegaApiGateway::getRubbishBinNode, emptyList()) { nodeTree.rubbishBinNode }

    override suspend fun isNodeS4Container(nodeHandle: Long): Boolean =
        engine.dispatch(MegaApiGateway::isNodeS4Container, listOf(nodeHandle)) { false }

    override fun getSdkVersion(): String? =
        engine.dispatchBlocking(MegaApiGateway::getSdkVersion, emptyList()) { "fake-sdk" }

    override val globalUpdates: Flow<GlobalUpdate> = globalUpdatesFlow

    override val globalTransfer: Flow<GlobalTransfer> = globalTransferFlow

    override val globalRequestEvents: Flow<RequestEvent> = globalRequestEventsFlow

    override suspend fun getMegaNodeByHandle(nodeHandle: Long): MegaNode? =
        engine.dispatch(MegaApiGateway::getMegaNodeByHandle, listOf(nodeHandle)) {
            nodeTree.nodeByHandle(nodeHandle)
        }

    override suspend fun getNodeByPath(path: String?, megaNode: MegaNode?): MegaNode? =
        engine.dispatch(MegaApiGateway::getNodeByPath, listOf(path, megaNode)) { null }

    override suspend fun getFingerprint(filePath: String): String? =
        engine.dispatch(MegaApiGateway::getFingerprint, listOf(filePath)) { null }

    override suspend fun getNodesByOriginalFingerprint(
        originalFingerprint: String,
        parentNode: MegaNode?,
    ): MegaNodeList? =
        engine.dispatch(
            MegaApiGateway::getNodesByOriginalFingerprint,
            listOf(originalFingerprint, parentNode),
        ) {
            null
        }

    override suspend fun getNodeByFingerprintAndParentNode(
        fingerprint: String,
        parentNode: MegaNode?,
    ): MegaNode? =
        engine.dispatch(
            MegaApiGateway::getNodeByFingerprintAndParentNode,
            listOf(fingerprint, parentNode),
        ) {
            null
        }

    override suspend fun getNodeByFingerprint(fingerprint: String): MegaNode? =
        engine.dispatch(MegaApiGateway::getNodeByFingerprint, listOf(fingerprint)) { null }

    override fun setOriginalFingerprint(
        node: MegaNode,
        originalFingerprint: String,
        listener: MegaRequestListenerInterface?,
    ) {
        completeRequest(
            MegaApiGateway::setOriginalFingerprint,
            listOf(node, originalFingerprint, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_NODE,
        )
    }

    override suspend fun hasVersion(node: MegaNode): Boolean =
        engine.dispatch(MegaApiGateway::hasVersion, listOf(node)) { false }

    override suspend fun getNumVersions(node: MegaNode): Int =
        engine.dispatch(MegaApiGateway::getNumVersions, listOf(node)) { 0 }

    override suspend fun getVersions(node: MegaNode): List<MegaNode> =
        engine.dispatch(MegaApiGateway::getVersions, listOf(node)) { emptyList() }

    override fun deleteVersion(nodeVersion: MegaNode, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::deleteVersion,
            listOf(nodeVersion, listener),
            listener,
            MegaRequest.TYPE_REMOVE,
        )
    }

    override suspend fun getIncomingSharesNode(order: Int?): List<MegaNode> =
        engine.dispatch(MegaApiGateway::getIncomingSharesNode, listOf(order)) { emptyList() }

    override suspend fun getOutgoingSharesNode(order: Int?): List<MegaShare> =
        engine.dispatch(MegaApiGateway::getOutgoingSharesNode, listOf(order)) { emptyList() }

    override suspend fun isPendingShare(node: MegaNode): Boolean =
        engine.dispatch(MegaApiGateway::isPendingShare, listOf(node)) { false }

    override suspend fun getPublicLinks(order: Int?): List<MegaNode> =
        engine.dispatch(getPublicLinksNullableOrderRef, listOf(order)) { emptyList() }

    override suspend fun getNumChildFolders(node: MegaNode): Int =
        engine.dispatch(MegaApiGateway::getNumChildFolders, listOf(node)) {
            nodeTree.childrenOf(node.handle).count { child -> child.isFolder }
        }

    override suspend fun getNumChildFiles(node: MegaNode): Int =
        engine.dispatch(MegaApiGateway::getNumChildFiles, listOf(node)) {
            nodeTree.childrenOf(node.handle).count { child -> !child.isFolder }
        }

    override fun setContactLinksOption(enable: Boolean, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::setContactLinksOption,
            listOf(enable, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_USER,
        )
    }

    override fun getContactLinksOption(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::getContactLinksOption,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_USER,
        )
    }

    override fun getFolderInfo(node: MegaNode?, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::getFolderInfo,
            listOf(node, listener),
            listener,
            MegaRequest.TYPE_FOLDER_INFO,
        )
    }

    override fun setNodeFavourite(node: MegaNode?, favourite: Boolean) {
        engine.dispatchBlocking(MegaApiGateway::setNodeFavourite, listOf(node, favourite)) { }
    }

    override fun setNodeSensitive(
        node: MegaNode?,
        sensitive: Boolean,
        listener: MegaRequestListenerInterface?,
    ) {
        completeRequest(
            MegaApiGateway::setNodeSensitive,
            listOf(node, sensitive, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_NODE,
        )
    }

    override suspend fun isSensitiveInherited(node: MegaNode): Boolean =
        engine.dispatch(MegaApiGateway::isSensitiveInherited, listOf(node)) { false }

    override fun addLogger(logger: MegaLoggerInterface) {
        engine.dispatchBlocking(MegaApiGateway::addLogger, listOf(logger)) { }
    }

    override fun removeLogger(logger: MegaLoggerInterface) {
        engine.dispatchBlocking(MegaApiGateway::removeLogger, listOf(logger)) { }
    }

    override fun setLogLevel(logLevel: Int) {
        engine.dispatchBlocking(MegaApiGateway::setLogLevel, listOf(logLevel)) { }
    }

    override suspend fun getLoggedInUser(): MegaUser? =
        engine.dispatch(MegaApiGateway::getLoggedInUser, emptyList()) { loggedInUserOrNull() }

    override fun getThumbnail(
        node: MegaNode,
        thumbnailFilePath: String,
        listener: MegaRequestListenerInterface?,
    ) {
        completeRequest(
            MegaApiGateway::getThumbnail,
            listOf(node, thumbnailFilePath, listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_FILE,
        )
    }

    /** Fake reversible encoding; [base64ToHandle] decodes it back to the same handle. */
    override fun handleToBase64(handle: Long): String =
        engine.dispatchBlocking(MegaApiGateway::handleToBase64, listOf(handle)) {
            encodeHandle(handle)
        }

    override fun base64ToHandle(base64Handle: String): Long =
        engine.dispatchBlocking(MegaApiGateway::base64ToHandle, listOf(base64Handle)) {
            decodeHandle(base64Handle)
        }

    override fun cancelTransfer(transfer: MegaTransfer, listener: MegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::cancelTransfer,
            listOf(transfer, listener),
            listener,
            MegaRequest.TYPE_CANCEL_TRANSFER,
        )
    }

    override suspend fun getNumUnreadUserAlerts(): Int =
        engine.dispatch(MegaApiGateway::getNumUnreadUserAlerts, emptyList()) { 0 }

    override suspend fun getBackupsNode(): MegaNode? =
        engine.dispatch(MegaApiGateway::getBackupsNode, emptyList()) { nodeTree.vaultNode }

    override suspend fun hasChildren(node: MegaNode): Boolean =
        engine.dispatch(MegaApiGateway::hasChildren, listOf(node)) {
            nodeTree.childrenOf(node.handle).isNotEmpty()
        }

    override fun registerPushNotifications(
        deviceType: Int,
        newToken: String,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::registerPushNotifications,
            listOf(deviceType, newToken, listener),
            listener,
            MegaRequest.TYPE_REGISTER_PUSH_NOTIFICATION,
        )
    }

    override fun fastLogin(session: String, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::fastLogin,
            listOf(session, listener),
            listener,
            MegaRequest.TYPE_LOGIN,
        )
    }

    override fun fetchNodes(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::fetchNodes,
            listOf(listener),
            listener,
            MegaRequest.TYPE_FETCH_NODES,
        )
    }

    override fun retryPendingConnections() {
        engine.dispatchBlocking(MegaApiGateway::retryPendingConnections, emptyList()) { }
    }

    override fun setMaxConnections(
        direction: Int,
        connections: Int,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::setMaxConnections,
            listOf(direction, connections, listener),
            listener,
            MegaRequest.TYPE_SET_MAX_CONNECTIONS,
        )
    }

    override fun getMaxUploadConnections(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::getMaxUploadConnections,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_MAX_CONNECTIONS,
        )
    }

    override fun getMaxDownloadConnections(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::getMaxDownloadConnections,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_MAX_CONNECTIONS,
        )
    }

    override suspend fun getTransfers(type: Int): List<MegaTransfer> =
        engine.dispatch(MegaApiGateway::getTransfers, listOf(type)) { emptyList() }

    override suspend fun getTransferByTag(tag: Int): MegaTransfer? =
        engine.dispatch(MegaApiGateway::getTransferByTag, listOf(tag)) { null }

    override suspend fun getTransferByUniqueId(id: Long): MegaTransfer? =
        engine.dispatch(MegaApiGateway::getTransferByUniqueId, listOf(id)) { null }

    override fun startDownload(
        node: MegaNode,
        localPath: String,
        fileName: String?,
        appData: String?,
        startFirst: Boolean,
        cancelToken: MegaCancelToken?,
        collisionCheck: Int,
        collisionResolution: Int,
        listener: MegaTransferListenerInterface?,
    ) {
        completeTransfer(
            MegaApiGateway::startDownload,
            listOf(node, localPath, fileName, appData, startFirst, cancelToken, collisionCheck, collisionResolution, listener),
            listener,
        )
    }

    override fun getUserEmail(userHandle: Long, callback: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::getUserEmail,
            listOf(userHandle, callback),
            callback,
            MegaRequest.TYPE_GET_USER_EMAIL,
        )
    }

    override suspend fun getContact(emailOrBase64Handle: String): MegaUser? =
        engine.dispatch(MegaApiGateway::getContact, listOf(emailOrBase64Handle)) { null }

    override suspend fun getUserAlerts(): List<MegaUserAlert> =
        engine.dispatch(MegaApiGateway::getUserAlerts, emptyList()) { emptyList() }

    override suspend fun sendEvent(
        eventId: Int,
        message: String,
        addJourneyId: Boolean,
        viewId: String?,
    ) {
        engine.dispatch(
            MegaApiGateway::sendEvent,
            listOf(eventId, message, addJourneyId, viewId),
        ) { }
    }

    override suspend fun generateViewId(): String =
        engine.dispatch(MegaApiGateway::generateViewId, emptyList()) { "0123456789abcdef" }

    override suspend fun acknowledgeUserAlerts() {
        engine.dispatch(MegaApiGateway::acknowledgeUserAlerts, emptyList()) { }
    }

    override suspend fun getIncomingContactRequests(): List<MegaContactRequest> =
        engine.dispatch(MegaApiGateway::getIncomingContactRequests, emptyList()) { emptyList() }

    override suspend fun getContactRequestByHandle(requestHandle: Long): MegaContactRequest? =
        engine.dispatch(MegaApiGateway::getContactRequestByHandle, listOf(requestHandle)) { null }

    override fun replyReceivedContactRequest(
        contactRequest: MegaContactRequest,
        action: Int,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::replyReceivedContactRequest,
            listOf(contactRequest, action, listener),
            listener,
            MegaRequest.TYPE_REPLY_CONTACT_REQUEST,
        )
    }

    override fun sendInvitedContactRequest(
        email: String,
        message: String,
        action: Int,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::sendInvitedContactRequest,
            listOf(email, message, action, listener),
            listener,
            MegaRequest.TYPE_INVITE_CONTACT,
        )
    }

    override suspend fun getUserAvatarColor(megaUser: MegaUser): String? =
        engine.dispatch(getUserAvatarColorByUserRef, listOf(megaUser)) { "#FF6A19" }

    override suspend fun getUserAvatarColor(userHandle: Long): String? =
        engine.dispatch(getUserAvatarColorByHandleRef, listOf(userHandle)) { "#FF6A19" }

    override suspend fun getUserAvatarSecondaryColor(userHandle: Long): String? =
        engine.dispatch(MegaApiGateway::getUserAvatarSecondaryColor, listOf(userHandle)) {
            "#00BFA5"
        }

    override fun getUserAvatar(
        user: MegaUser,
        destinationPath: String,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::getUserAvatar,
            listOf(user, destinationPath, listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_USER,
        )
    }

    override suspend fun getPublicLinks(): List<MegaNode> =
        engine.dispatch(getPublicLinksRef, emptyList()) { emptyList() }

    override fun getPreview(
        node: MegaNode,
        previewFilePath: String,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::getPreview,
            listOf(node, previewFilePath, listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_FILE,
        )
    }

    override fun getFullImage(
        node: MegaNode,
        fullFile: File,
        highPriority: Boolean,
        listener: MegaTransferListenerInterface,
    ) {
        completeTransfer(
            MegaApiGateway::getFullImage,
            listOf(node, fullFile, highPriority, listener),
            listener,
        )
    }

    override suspend fun isInRubbish(node: MegaNode): Boolean =
        engine.dispatch(MegaApiGateway::isInRubbish, listOf(node)) {
            isInSubtree(node, nodeTree.rubbishBinNode.handle)
        }

    override suspend fun isInBackups(node: MegaNode): Boolean =
        engine.dispatch(MegaApiGateway::isInBackups, listOf(node)) {
            isInSubtree(node, nodeTree.vaultNode.handle)
        }

    override suspend fun isInCloudDrive(node: MegaNode): Boolean =
        engine.dispatch(MegaApiGateway::isInCloudDrive, listOf(node)) {
            isInSubtree(node, nodeTree.rootNode.handle)
        }

    override suspend fun moveTransferToFirst(
        transfer: MegaTransfer,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::moveTransferToFirst,
            listOf(transfer, listener),
            listener,
            MegaRequest.TYPE_MOVE_TRANSFER,
        )
    }

    override suspend fun moveTransferToLast(
        transfer: MegaTransfer,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::moveTransferToLast,
            listOf(transfer, listener),
            listener,
            MegaRequest.TYPE_MOVE_TRANSFER,
        )
    }

    override suspend fun moveTransferBefore(
        transfer: MegaTransfer,
        prevTransfer: MegaTransfer,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::moveTransferBefore,
            listOf(transfer, prevTransfer, listener),
            listener,
            MegaRequest.TYPE_MOVE_TRANSFER,
        )
    }

    override fun moveTransferToFirstByTag(
        transferTag: Int,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::moveTransferToFirstByTag,
            listOf(transferTag, listener),
            listener,
            MegaRequest.TYPE_MOVE_TRANSFER,
        )
    }

    override fun moveTransferToLastByTag(transferTag: Int, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::moveTransferToLastByTag,
            listOf(transferTag, listener),
            listener,
            MegaRequest.TYPE_MOVE_TRANSFER,
        )
    }

    override fun moveTransferBeforeByTag(
        transferTag: Int,
        prevTransferTag: Int,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::moveTransferBeforeByTag,
            listOf(transferTag, prevTransferTag, listener),
            listener,
            MegaRequest.TYPE_MOVE_TRANSFER,
        )
    }

    override suspend fun getContacts(): List<MegaUser> =
        engine.dispatch(MegaApiGateway::getContacts, emptyList()) { emptyList() }

    override suspend fun areCredentialsVerified(megaUser: MegaUser): Boolean =
        engine.dispatch(MegaApiGateway::areCredentialsVerified, listOf(megaUser)) { false }

    override fun getUserAlias(userHandle: Long, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::getUserAlias,
            listOf(userHandle, listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_USER,
        )
    }

    override fun getContactAvatar(
        emailOrHandle: String,
        path: String,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::getContactAvatar,
            listOf(emailOrHandle, path, listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_USER,
        )
    }

    override fun getUserAttribute(
        emailOrHandle: String,
        type: Int,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            getUserAttributeByEmailRef,
            listOf(emailOrHandle, type, listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_USER,
            defaultRequest = userAttributeRequest(type, email = emailOrHandle),
        )
    }

    override fun userHandleToBase64(userHandle: Long): String =
        engine.dispatchBlocking(MegaApiGateway::userHandleToBase64, listOf(userHandle)) {
            encodeHandle(userHandle)
        }

    override fun getUserAttribute(
        user: MegaUser,
        type: Int,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            getUserAttributeByUserRef,
            listOf(user, type, listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_USER,
            defaultRequest = userAttributeRequest(type, email = user.email),
        )
    }

    override fun getRecentActionsAsync(
        days: Long,
        maxNodes: Long,
        excludeSensitives: Boolean,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::getRecentActionsAsync,
            listOf(days, maxNodes, excludeSensitives, listener),
            listener,
            MegaRequest.TYPE_GET_RECENT_ACTIONS,
        )
    }

    override fun getRecentBucketById(
        id: String,
        excludeSensitives: Boolean,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::getRecentBucketById,
            listOf(id, excludeSensitives, listener),
            listener,
            MegaRequest.TYPE_GET_RECENT_ACTION_BY_ID,
        )
    }

    override fun clearRecentActions(until: Long, listener: MegaRequestListenerInterface) {
        completeRequest(MegaApiGateway::clearRecentActions, listOf(until, listener), listener, 0)
    }

    override fun copyNode(
        nodeToCopy: MegaNode,
        newNodeParent: MegaNode,
        newNodeName: String?,
        listener: MegaRequestListenerInterface?,
    ) {
        completeRequest(
            MegaApiGateway::copyNode,
            listOf(nodeToCopy, newNodeParent, newNodeName, listener),
            listener,
            MegaRequest.TYPE_COPY,
        )
    }

    override fun moveNode(
        nodeToMove: MegaNode,
        newNodeParent: MegaNode,
        newNodeName: String?,
        listener: MegaRequestListenerInterface?,
    ) {
        completeRequest(
            MegaApiGateway::moveNode,
            listOf(nodeToMove, newNodeParent, newNodeName, listener),
            listener,
            MegaRequest.TYPE_MOVE,
        )
    }

    override fun deleteNode(node: MegaNode, listener: MegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::deleteNode,
            listOf(node, listener),
            listener,
            MegaRequest.TYPE_REMOVE,
        )
    }

    /** No native copy is possible; the argument itself is returned. */
    override fun copyBucketList(
        bucketList: MegaRecentActionBucketList,
    ): MegaRecentActionBucketList =
        engine.dispatchBlocking(MegaApiGateway::copyBucketList, listOf(bucketList)) { bucketList }

    /** No native copy is possible; the argument itself is returned. */
    override fun copyBucket(bucket: MegaRecentActionBucket): MegaRecentActionBucket =
        engine.dispatchBlocking(MegaApiGateway::copyBucket, listOf(bucket)) { bucket }

    override fun checkAccessErrorExtended(node: MegaNode, level: Int): MegaError =
        engine.dispatchBlocking(MegaApiGateway::checkAccessErrorExtended, listOf(node, level)) {
            StubMegaError()
        }

    override fun checkMoveErrorExtended(node: MegaNode, targetNode: MegaNode): MegaError =
        engine.dispatchBlocking(MegaApiGateway::checkMoveErrorExtended, listOf(node, targetNode)) {
            StubMegaError()
        }

    override suspend fun isBusinessAccountActive(): Boolean =
        engine.dispatch(MegaApiGateway::isBusinessAccountActive, emptyList()) { true }

    override fun getPricing(listener: MegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::getPricing,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_PRICING,
        )
    }

    override fun getPaymentMethods(listener: MegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::getPaymentMethods,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_PAYMENT_METHODS,
        )
    }

    override fun getAccountDetails(listener: MegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::getAccountDetails,
            listOf(listener),
            listener,
            MegaRequest.TYPE_ACCOUNT_DETAILS,
        )
    }

    override fun getSpecificAccountDetails(
        storage: Boolean,
        transfer: Boolean,
        pro: Boolean,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::getSpecificAccountDetails,
            listOf(storage, transfer, pro, listener),
            listener,
            MegaRequest.TYPE_ACCOUNT_DETAILS,
            defaultRequest = accountDetailsRequest(
                (if (storage) DETAILS_FLAG_STORAGE else 0)
                        or (if (transfer) DETAILS_FLAG_TRANSFER else 0)
                        or (if (pro) DETAILS_FLAG_PRO else 0)
            ),
        )
    }

    override fun creditCardQuerySubscriptions(listener: MegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::creditCardQuerySubscriptions,
            listOf(listener),
            listener,
            MegaRequest.TYPE_CREDIT_CARD_QUERY_SUBSCRIPTIONS,
        )
    }

    override fun getUserAttribute(
        attributeIdentifier: Int,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            getUserAttributeByIdRef,
            listOf(attributeIdentifier, listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_USER,
            defaultRequest = userAttributeRequest(attributeIdentifier),
        )
    }

    override suspend fun areAccountAchievementsEnabled(): Boolean =
        engine.dispatch(MegaApiGateway::areAccountAchievementsEnabled, emptyList()) {
            account.isAchievementsEnabled
        }

    override fun getAccountAchievements(listener: MegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::getAccountAchievements,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_ACHIEVEMENTS,
        )
    }

    /** Authorization always succeeds by returning the node unchanged. */
    override suspend fun authorizeNode(node: MegaNode): MegaNode? =
        engine.dispatch(MegaApiGateway::authorizeNode, listOf(node)) { node }

    override suspend fun httpServerGetLocalLink(node: MegaNode): String? =
        engine.dispatch(MegaApiGateway::httpServerGetLocalLink, listOf(node)) { null }

    override suspend fun httpServerIsRunning(): Int =
        engine.dispatch(MegaApiGateway::httpServerIsRunning, emptyList()) { 0 }

    override suspend fun httpServerStart(): Boolean =
        engine.dispatch(MegaApiGateway::httpServerStart, emptyList()) { true }

    override suspend fun httpServerStop() {
        engine.dispatch(MegaApiGateway::httpServerStop, emptyList()) { }
    }

    override suspend fun httpServerSetMaxBufferSize(bufferSize: Int) {
        engine.dispatch(MegaApiGateway::httpServerSetMaxBufferSize, listOf(bufferSize)) { }
    }

    override suspend fun getPublicLinks(order: Int): List<MegaNode> =
        engine.dispatch(getPublicLinksOrderRef, listOf(order)) { emptyList() }

    override suspend fun getInShares(order: Int): List<MegaNode> =
        engine.dispatch(getInSharesByOrderRef, listOf(order)) { emptyList() }

    override suspend fun getInShares(user: MegaUser): List<MegaNode> =
        engine.dispatch(getInSharesByUserRef, listOf(user)) { emptyList() }

    override suspend fun getUserFromInShare(node: MegaNode, recursive: Boolean): MegaUser? =
        engine.dispatch(MegaApiGateway::getUserFromInShare, listOf(node, recursive)) { null }

    override suspend fun getOutShares(order: Int): List<MegaShare> =
        engine.dispatch(getOutSharesByOrderRef, listOf(order)) { emptyList() }

    override suspend fun getOutShares(megaNode: MegaNode): List<MegaShare> =
        engine.dispatch(getOutSharesByNodeRef, listOf(megaNode)) { emptyList() }

    override suspend fun getUnverifiedIncomingShares(order: Int): List<MegaShare> =
        engine.dispatch(MegaApiGateway::getUnverifiedIncomingShares, listOf(order)) { emptyList() }

    override suspend fun getVerifiedIncomingShares(order: Int?): List<MegaShare> =
        engine.dispatch(MegaApiGateway::getVerifiedIncomingShares, listOf(order)) { emptyList() }

    override fun createSet(name: String, type: Int, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::createSet,
            listOf(name, type, listener),
            listener,
            MegaRequest.TYPE_PUT_SET,
        )
    }

    override fun createSetElement(sid: Long, node: Long, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::createSetElement,
            listOf(sid, node, listener),
            listener,
            MegaRequest.TYPE_PUT_SET_ELEMENT,
        )
    }

    override fun createSetElements(
        sid: Long,
        nodes: MegaHandleList,
        names: MegaStringList?,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::createSetElements,
            listOf(sid, nodes, names, listener),
            listener,
            MegaRequest.TYPE_PUT_SET_ELEMENTS,
        )
    }

    override suspend fun removeSetElement(
        sid: Long,
        eid: Long,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::removeSetElement,
            listOf(sid, eid, listener),
            listener,
            MegaRequest.TYPE_REMOVE_SET_ELEMENT,
        )
    }

    override suspend fun getSets(): MegaSetList =
        engine.dispatch(MegaApiGateway::getSets, emptyList()) { StubMegaSetList() }

    override suspend fun getSet(sid: Long): MegaSet? =
        engine.dispatch(MegaApiGateway::getSet, listOf(sid)) { null }

    override suspend fun getSetElements(sid: Long): MegaSetElementList =
        engine.dispatch(MegaApiGateway::getSetElements, listOf(sid)) { StubMegaSetElementList() }

    override fun removeSet(sid: Long, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::removeSet,
            listOf(sid, listener),
            listener,
            MegaRequest.TYPE_REMOVE_SET,
        )
    }

    override fun updateSetName(sid: Long, name: String?, listener: MegaRequestListenerInterface?) {
        completeRequest(
            updateSetNameWithListenerRef,
            listOf(sid, name, listener),
            listener,
            MegaRequest.TYPE_PUT_SET,
        )
    }

    override fun updateSetName(sid: Long, name: String?) {
        engine.dispatchBlocking(updateSetNameRef, listOf(sid, name)) { }
    }

    override suspend fun putSetCover(sid: Long, eid: Long) {
        engine.dispatch(MegaApiGateway::putSetCover, listOf(sid, eid)) { }
    }

    override fun exportSet(sid: Long, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::exportSet,
            listOf(sid, listener),
            listener,
            MegaRequest.TYPE_EXPORT_SET,
        )
    }

    override fun disableExportSet(sid: Long, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::disableExportSet,
            listOf(sid, listener),
            listener,
            MegaRequest.TYPE_EXPORT_SET,
        )
    }

    override fun getPreviewElementNode(eid: Long, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::getPreviewElementNode,
            listOf(eid, listener),
            listener,
            MegaRequest.TYPE_GET_EXPORTED_SET_ELEMENT,
        )
    }

    override fun fetchPublicSet(publicSetLink: String, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::fetchPublicSet,
            listOf(publicSetLink, listener),
            listener,
            MegaRequest.TYPE_FETCH_SET,
        )
    }

    override fun stopPublicSetPreview() {
        engine.dispatchBlocking(MegaApiGateway::stopPublicSetPreview, emptyList()) { }
    }

    override fun removeRequestListener(listener: MegaRequestListenerInterface) {
        engine.dispatchBlocking(MegaApiGateway::removeRequestListener, listOf(listener)) { }
    }

    override fun getUserCredentials(user: MegaUser, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::getUserCredentials,
            listOf(user, listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_USER,
        )
    }

    override fun resetCredentials(user: MegaUser, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::resetCredentials,
            listOf(user, listener),
            listener,
            MegaRequest.TYPE_VERIFY_CREDENTIALS,
        )
    }

    override fun verifyCredentials(user: MegaUser, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::verifyCredentials,
            listOf(user, listener),
            listener,
            MegaRequest.TYPE_VERIFY_CREDENTIALS,
        )
    }

    override suspend fun isCurrentPassword(password: String): Boolean =
        engine.dispatch(MegaApiGateway::isCurrentPassword, listOf(password)) { false }

    override fun changePassword(newPassword: String, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::changePassword,
            listOf(newPassword, listener),
            listener,
            MegaRequest.TYPE_CHANGE_PW,
        )
    }

    override fun resetPasswordFromLink(
        link: String?,
        newPassword: String,
        masterKey: String?,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::resetPasswordFromLink,
            listOf(link, newPassword, masterKey, listener),
            listener,
            MegaRequest.TYPE_CONFIRM_RECOVERY_LINK,
        )
    }

    override suspend fun getPasswordStrength(password: String): Int =
        engine.dispatch(MegaApiGateway::getPasswordStrength, listOf(password)) { 0 }

    override fun getCountryCallingCodes(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::getCountryCallingCodes,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_COUNTRY_CALLING_CODES,
        )
    }

    override fun logout(listener: MegaRequestListenerInterface?) {
        completeRequest(MegaApiGateway::logout, listOf(listener), listener, MegaRequest.TYPE_LOGOUT)
    }

    override fun sendSMSVerificationCode(
        phoneNumber: String,
        reVerifyingWhitelisted: Boolean,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::sendSMSVerificationCode,
            listOf(phoneNumber, reVerifyingWhitelisted, listener),
            listener,
            MegaRequest.TYPE_SEND_SMS_VERIFICATIONCODE,
        )
    }

    override fun resetSmsVerifiedPhoneNumber(listener: MegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::resetSmsVerifiedPhoneNumber,
            listOf(listener),
            listener,
            MegaRequest.TYPE_RESET_SMS_VERIFIED_NUMBER,
        )
    }

    override fun getExtendedAccountDetails(
        sessions: Boolean,
        purchases: Boolean,
        transactions: Boolean,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::getExtendedAccountDetails,
            listOf(sessions, purchases, transactions, listener),
            listener,
            MegaRequest.TYPE_ACCOUNT_DETAILS,
            defaultRequest = accountDetailsRequest(
                if (sessions) DETAILS_FLAG_SESSIONS else 0
            ),
        )
    }

    override fun contactLinkCreate(renew: Boolean, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::contactLinkCreate,
            listOf(renew, listener),
            listener,
            MegaRequest.TYPE_CONTACT_LINK_CREATE,
        )
    }

    override fun contactLinkDelete(handle: Long, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::contactLinkDelete,
            listOf(handle, listener),
            listener,
            MegaRequest.TYPE_CONTACT_LINK_DELETE,
        )
    }

    override fun isChatNotifiable(chatId: Long): Boolean =
        engine.dispatchBlocking(MegaApiGateway::isChatNotifiable, listOf(chatId)) { true }

    override fun getPushNotificationSettings(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::getPushNotificationSettings,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_USER,
        )
    }

    override fun setPushNotificationSettings(
        settings: MegaPushNotificationSettings,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::setPushNotificationSettings,
            listOf(settings, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_USER,
        )
    }

    override fun inviteContact(email: String, listener: MegaRequestListenerInterface) {
        completeRequest(
            inviteContactRef,
            listOf(email, listener),
            listener,
            MegaRequest.TYPE_INVITE_CONTACT,
        )
    }

    override fun inviteContact(
        email: String,
        handle: Long,
        message: String?,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            inviteContactWithMessageRef,
            listOf(email, handle, message, listener),
            listener,
            MegaRequest.TYPE_INVITE_CONTACT,
        )
    }

    override suspend fun getOutgoingContactRequests(): List<MegaContactRequest> =
        engine.dispatch(MegaApiGateway::getOutgoingContactRequests, emptyList()) { emptyList() }

    override fun createFolder(
        name: String,
        parent: MegaNode,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::createFolder,
            listOf(name, parent, listener),
            listener,
            MegaRequest.TYPE_CREATE_FOLDER,
        )
    }

    override fun setCameraUploadsFolders(
        primaryFolder: Long,
        secondaryFolder: Long,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::setCameraUploadsFolders,
            listOf(primaryFolder, secondaryFolder, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_USER,
        )
    }

    override fun renameNode(
        node: MegaNode,
        newName: String,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::renameNode,
            listOf(node, newName, listener),
            listener,
            MegaRequest.TYPE_RENAME,
        )
    }

    /** Authorization always succeeds by returning the node unchanged. */
    override fun authorizeChatNode(node: MegaNode, authorizationToken: String): MegaNode? =
        engine.dispatchBlocking(
            MegaApiGateway::authorizeChatNode,
            listOf(node, authorizationToken),
        ) {
            node
        }

    override fun submitPurchaseReceipt(
        gateway: Int,
        receipt: String?,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::submitPurchaseReceipt,
            listOf(gateway, receipt, listener),
            listener,
            MegaRequest.TYPE_SUBMIT_PURCHASE_RECEIPT,
        )
    }

    override fun setMyChatFilesFolder(nodeHandle: Long, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::setMyChatFilesFolder,
            listOf(nodeHandle, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_USER,
        )
    }

    override fun getMyChatFilesFolder(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::getMyChatFilesFolder,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_USER,
        )
    }

    override fun getFileVersionsOption(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::getFileVersionsOption,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_USER,
        )
    }

    override fun setFileVersionsOption(disable: Boolean, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::setFileVersionsOption,
            listOf(disable, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_USER,
        )
    }

    /** 3 means a valid full session, matching MegaApi::isLoggedIn. */
    override fun isMegaApiLoggedIn(): Int =
        engine.dispatchBlocking(MegaApiGateway::isMegaApiLoggedIn, emptyList()) {
            if (account.isLoggedIn) 3 else 0
        }

    override fun cancelTransferByTag(transferTag: Int, listener: MegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::cancelTransferByTag,
            listOf(transferTag, listener),
            listener,
            MegaRequest.TYPE_CANCEL_TRANSFER,
        )
    }

    override fun contactLinkQuery(handle: Long, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::contactLinkQuery,
            listOf(handle, listener),
            listener,
            MegaRequest.TYPE_CONTACT_LINK_QUERY,
        )
    }

    override fun changeEmail(email: String, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::changeEmail,
            listOf(email, listener),
            listener,
            MegaRequest.TYPE_GET_CHANGE_EMAIL_LINK,
        )
    }

    override suspend fun isAccountNew(): Boolean =
        engine.dispatch(MegaApiGateway::isAccountNew, emptyList()) { false }

    override suspend fun getExportMasterKey(): String? =
        engine.dispatch(MegaApiGateway::getExportMasterKey, emptyList()) { "fake-master-key" }

    override fun setMasterKeyExported(listener: MegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::setMasterKeyExported,
            listOf(listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_USER,
        )
    }

    override fun isMasterKeyExported(listener: MegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::isMasterKeyExported,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_USER,
        )
    }

    override fun getMultiFactorAuthCode(listener: MegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::getMultiFactorAuthCode,
            listOf(listener),
            listener,
            MegaRequest.TYPE_MULTI_FACTOR_AUTH_GET,
        )
    }

    override fun enableMultiFactorAuth(pin: String, listener: MegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::enableMultiFactorAuth,
            listOf(pin, listener),
            listener,
            MegaRequest.TYPE_MULTI_FACTOR_AUTH_SET,
        )
    }

    override fun multiFactorAuthDisable(pin: String, listener: MegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::multiFactorAuthDisable,
            listOf(pin, listener),
            listener,
            MegaRequest.TYPE_MULTI_FACTOR_AUTH_SET,
        )
    }

    override fun multiFactorAuthCancelAccount(
        pin: String,
        listener: MegaRequestListenerInterface?,
    ) {
        completeRequest(
            MegaApiGateway::multiFactorAuthCancelAccount,
            listOf(pin, listener),
            listener,
            MegaRequest.TYPE_GET_CANCEL_LINK,
        )
    }

    override fun multiFactorAuthChangeEmail(
        newEmail: String,
        pin: String,
        listener: MegaRequestListenerInterface?,
    ) {
        completeRequest(
            MegaApiGateway::multiFactorAuthChangeEmail,
            listOf(newEmail, pin, listener),
            listener,
            MegaRequest.TYPE_GET_CHANGE_EMAIL_LINK,
        )
    }

    override fun multiFactorAuthChangePassword(
        currentPassword: String?,
        newPassword: String,
        pin: String,
        listener: MegaRequestListenerInterface?,
    ) {
        completeRequest(
            MegaApiGateway::multiFactorAuthChangePassword,
            listOf(currentPassword, newPassword, pin, listener),
            listener,
            MegaRequest.TYPE_CHANGE_PW,
        )
    }

    override fun setUserAttribute(
        type: Int,
        value: String,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            setUserAttributeStringRef,
            listOf(type, value, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_USER,
        )
    }

    override fun setLastPurgeAcknowledged(ts: Long, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::setLastPurgeAcknowledged,
            listOf(ts, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_USER,
        )
    }

    override fun setUserAttribute(
        type: Int,
        value: MegaStringMap,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            setUserAttributeStringMapRef,
            listOf(type, value, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_USER,
        )
    }

    override fun querySignupLink(link: String, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::querySignupLink,
            listOf(link, listener),
            listener,
            MegaRequest.TYPE_QUERY_SIGNUP_LINK,
        )
    }

    override fun queryResetPasswordLink(link: String, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::queryResetPasswordLink,
            listOf(link, listener),
            listener,
            MegaRequest.TYPE_QUERY_RECOVERY_LINK,
        )
    }

    override fun getPublicNode(nodeFileLink: String, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::getPublicNode,
            listOf(nodeFileLink, listener),
            listener,
            MegaRequest.TYPE_GET_PUBLIC_NODE,
        )
    }

    override fun cancelTransfers(direction: Int, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::cancelTransfers,
            listOf(direction, listener),
            listener,
            MegaRequest.TYPE_CANCEL_TRANSFERS,
        )
    }

    override suspend fun getVerifiedPhoneNumber(): String? =
        engine.dispatch(MegaApiGateway::getVerifiedPhoneNumber, emptyList()) { null }

    override fun verifyPhoneNumber(pin: String, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::verifyPhoneNumber,
            listOf(pin, listener),
            listener,
            MegaRequest.TYPE_CHECK_SMS_VERIFICATIONCODE,
        )
    }

    override fun localLogout(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::localLogout,
            listOf(listener),
            listener,
            MegaRequest.TYPE_LOGOUT,
        )
    }

    override suspend fun searchWithFilter(
        filter: MegaSearchFilter,
        order: Int,
        megaCancelToken: MegaCancelToken,
        megaSearchPage: MegaSearchPage?,
    ): List<MegaNode> =
        engine.dispatch(
            MegaApiGateway::searchWithFilter,
            listOf(filter, order, megaCancelToken, megaSearchPage),
        ) {
            emptyList()
        }

    override suspend fun getChildren(
        filter: MegaSearchFilter,
        order: Int,
        megaCancelToken: MegaCancelToken,
        megaSearchPage: MegaSearchPage?,
    ): List<MegaNode> =
        engine.dispatch(
            MegaApiGateway::getChildren,
            listOf(filter, order, megaCancelToken, megaSearchPage),
        ) {
            emptyList()
        }

    override fun openShareDialog(megaNode: MegaNode, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::openShareDialog,
            listOf(megaNode, listener),
            listener,
            MegaRequest.TYPE_OPEN_SHARE_DIALOG,
        )
    }

    override fun upgradeSecurity(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::upgradeSecurity,
            listOf(listener),
            listener,
            MegaRequest.TYPE_UPGRADE_SECURITY,
        )
    }

    override suspend fun getSmsAllowedState(): Int =
        engine.dispatch(MegaApiGateway::getSmsAllowedState, emptyList()) { 0 }

    override fun login(email: String, password: String, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::login,
            listOf(email, password, listener),
            listener,
            MegaRequest.TYPE_LOGIN,
        )
    }

    override fun multiFactorAuthLogin(
        email: String,
        password: String,
        pin: String,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::multiFactorAuthLogin,
            listOf(email, password, pin, listener),
            listener,
            MegaRequest.TYPE_LOGIN,
        )
    }

    override suspend fun getNodePath(node: MegaNode): String? =
        engine.dispatch(MegaApiGateway::getNodePath, listOf(node)) {
            node.name?.let { nodeName -> "/" + nodeName }
        }

    override suspend fun getNodePathByHandle(handle: Long): String? =
        engine.dispatch(MegaApiGateway::getNodePathByHandle, listOf(handle)) {
            nodeTree.nodeByHandle(handle)?.name?.let { nodeName -> "/" + nodeName }
        }

    /** The fake account owns every node by default. */
    override fun getAccess(megaNode: MegaNode): Int =
        engine.dispatchBlocking(MegaApiGateway::getAccess, listOf(megaNode)) {
            MegaShare.ACCESS_OWNER
        }

    override fun stopSharingNode(megaNode: MegaNode) {
        engine.dispatchBlocking(MegaApiGateway::stopSharingNode, listOf(megaNode)) { }
    }

    override fun setShareAccess(
        megaNode: MegaNode,
        email: String,
        accessLevel: Int,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::setShareAccess,
            listOf(megaNode, email, accessLevel, listener),
            listener,
            MegaRequest.TYPE_SHARE,
        )
    }

    override fun setAvatar(srcFilePath: String?, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::setAvatar,
            listOf(srcFilePath, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_USER,
        )
    }

    override fun skipPasswordReminderDialog(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::skipPasswordReminderDialog,
            listOf(listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_USER,
        )
    }

    override fun blockPasswordReminderDialog(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::blockPasswordReminderDialog,
            listOf(listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_USER,
        )
    }

    override fun successPasswordReminderDialog(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::successPasswordReminderDialog,
            listOf(listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_USER,
        )
    }

    override fun setUserAlias(
        userHandle: Long,
        name: String?,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::setUserAlias,
            listOf(userHandle, name, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_USER,
        )
    }

    override suspend fun getTransferData(): MegaTransferData? =
        engine.dispatch(MegaApiGateway::getTransferData, emptyList()) { null }

    override fun removeContact(user: MegaUser, listener: MegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::removeContact,
            listOf(user, listener),
            listener,
            MegaRequest.TYPE_REMOVE_CONTACT,
        )
    }

    override fun sendBackupHeartbeat(
        backupId: Long,
        status: Int,
        progress: Int,
        ups: Int,
        downs: Int,
        ts: Long,
        lastNode: Long,
        listener: MegaRequestListenerInterface?,
    ) {
        completeRequest(
            MegaApiGateway::sendBackupHeartbeat,
            listOf(backupId, status, progress, ups, downs, ts, lastNode, listener),
            listener,
            MegaRequest.TYPE_BACKUP_PUT_HEART_BEAT,
        )
    }

    override fun updateBackup(
        backupId: Long,
        backupType: Int,
        targetNode: Long,
        localFolder: String?,
        backupName: String?,
        state: Int,
        subState: Int,
        listener: MegaRequestListenerInterface?,
    ) {
        completeRequest(
            MegaApiGateway::updateBackup,
            listOf(backupId, backupType, targetNode, localFolder, backupName, state, subState, listener),
            listener,
            MegaRequest.TYPE_BACKUP_PUT,
        )
    }

    override fun setCoordinates(
        nodeId: NodeId,
        latitude: Double,
        longitude: Double,
        listener: MegaRequestListenerInterface?,
    ) {
        completeRequest(
            MegaApiGateway::setCoordinates,
            listOf(nodeId, latitude, longitude, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_NODE,
        )
    }

    override fun shouldShowPasswordReminderDialog(
        atLogout: Boolean,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::shouldShowPasswordReminderDialog,
            listOf(atLogout, listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_USER,
        )
    }

    override suspend fun isForeignNode(handle: Long): Boolean =
        engine.dispatch(MegaApiGateway::isForeignNode, listOf(handle)) { false }

    override fun setBackup(
        backupType: Int,
        targetNode: Long,
        localFolder: String,
        backupName: String,
        state: Int,
        subState: Int,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::setBackup,
            listOf(backupType, targetNode, localFolder, backupName, state, subState, listener),
            listener,
            MegaRequest.TYPE_BACKUP_PUT,
        )
    }

    override fun removeBackup(backupId: Long, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::removeBackup,
            listOf(backupId, listener),
            listener,
            MegaRequest.TYPE_BACKUP_REMOVE,
        )
    }

    override fun getBackupInfo(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::getBackupInfo,
            listOf(listener),
            listener,
            MegaRequest.TYPE_BACKUP_INFO,
        )
    }

    override suspend fun reconnect() {
        engine.dispatch(MegaApiGateway::reconnect, emptyList()) { }
    }

    override fun createCancelToken(): MegaCancelToken =
        engine.dispatchBlocking(MegaApiGateway::createCancelToken, emptyList()) {
            StubMegaCancelToken()
        }

    override fun exportNode(
        node: MegaNode,
        expireTime: Long?,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::exportNode,
            listOf(node, expireTime, listener),
            listener,
            MegaRequest.TYPE_EXPORT,
        )
    }

    override fun getDownloadUrl(node: MegaNode, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::getDownloadUrl,
            listOf(node, listener),
            listener,
            MegaRequest.TYPE_GET_DOWNLOAD_URLS,
        )
    }

    override fun getDeviceName(deviceId: String, listener: MegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::getDeviceName,
            listOf(deviceId, listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_USER,
        )
    }

    override fun setDeviceName(
        deviceId: String,
        deviceName: String,
        listener: MegaRequestListenerInterface?,
    ) {
        completeRequest(
            MegaApiGateway::setDeviceName,
            listOf(deviceId, deviceName, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_USER,
        )
    }

    override fun getDeviceId(): String? =
        engine.dispatchBlocking(MegaApiGateway::getDeviceId, emptyList()) { "fake-device-id" }

    override fun getABTestValue(flag: String): Long =
        engine.dispatchBlocking(MegaApiGateway::getABTestValue, listOf(flag)) { 0L }

    override suspend fun getBandwidthOverQuotaDelay(): Long =
        engine.dispatch(MegaApiGateway::getBandwidthOverQuotaDelay, emptyList()) { 0L }

    override suspend fun getNumNodes(): Long =
        engine.dispatch(MegaApiGateway::getNumNodes, emptyList()) { 0L }

    override suspend fun getOverquotaDeadlineTs(): Long =
        engine.dispatch(MegaApiGateway::getOverquotaDeadlineTs, emptyList()) { 0L }

    override suspend fun getOverquotaWarningsTs(): List<Long> =
        engine.dispatch(MegaApiGateway::getOverquotaWarningsTs, emptyList()) { emptyList() }

    override fun disableExport(node: MegaNode, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::disableExport,
            listOf(node, listener),
            listener,
            MegaRequest.TYPE_EXPORT,
        )
    }

    override fun encryptLinkWithPassword(
        link: String,
        password: String,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::encryptLinkWithPassword,
            listOf(link, password, listener),
            listener,
            MegaRequest.TYPE_PASSWORD_LINK,
        )
    }

    override fun decryptPasswordProtectedLink(
        passwordProtectedLink: String,
        password: String,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::decryptPasswordProtectedLink,
            listOf(passwordProtectedLink, password, listener),
            listener,
            MegaRequest.TYPE_PASSWORD_LINK,
        )
    }

    override val currentUploadSpeed: Int
        get() = 0

    override suspend fun setNodeCoordinates(node: MegaNode, latitude: Double, longitude: Double) {
        engine.dispatch(MegaApiGateway::setNodeCoordinates, listOf(node, latitude, longitude)) { }
    }

    override suspend fun createThumbnail(imagePath: String, destinationPath: String): Boolean =
        engine.dispatch(MegaApiGateway::createThumbnail, listOf(imagePath, destinationPath)) {
            false
        }

    override suspend fun createPreview(imagePath: String, destinationPath: String): Boolean =
        engine.dispatch(MegaApiGateway::createPreview, listOf(imagePath, destinationPath)) { false }

    override fun pauseTransfers(pause: Boolean, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::pauseTransfers,
            listOf(pause, listener),
            listener,
            MegaRequest.TYPE_PAUSE_TRANSFERS,
        )
    }

    override fun setThumbnail(
        node: MegaNode,
        srcFilePath: String,
        listener: MegaRequestListenerInterface?,
    ) {
        completeRequest(
            MegaApiGateway::setThumbnail,
            listOf(node, srcFilePath, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_FILE,
        )
    }

    override fun setPreview(
        node: MegaNode,
        srcFilePath: String,
        listener: MegaRequestListenerInterface?,
    ) {
        completeRequest(
            MegaApiGateway::setPreview,
            listOf(node, srcFilePath, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_FILE,
        )
    }

    override fun pauseTransferByTag(
        transferTag: Int,
        pause: Boolean,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::pauseTransferByTag,
            listOf(transferTag, pause, listener),
            listener,
            MegaRequest.TYPE_PAUSE_TRANSFER,
        )
    }

    override suspend fun getContactVerificationWarningEnabled(): Boolean =
        engine.dispatch(MegaApiGateway::getContactVerificationWarningEnabled, emptyList()) { false }

    override fun createEphemeralAccountPlusPlus(
        firstName: String,
        lastName: String,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::createEphemeralAccountPlusPlus,
            listOf(firstName, lastName, listener),
            listener,
            MegaRequest.TYPE_CREATE_ACCOUNT,
        )
    }

    /** No escaping is performed; the file name is returned unchanged. */
    override suspend fun escapeFsIncompatible(fileName: String, dstPath: String): String? =
        engine.dispatch(MegaApiGateway::escapeFsIncompatible, listOf(fileName, dstPath)) {
            fileName
        }

    override val currentDownloadSpeed: Int
        get() = 0

    override fun getPsa(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::getPsa,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_PSA,
        )
    }

    override suspend fun setPsaHandled(psaId: Int) {
        engine.dispatch(MegaApiGateway::setPsaHandled, listOf(psaId)) { }
    }

    override suspend fun setNodeLabel(node: MegaNode, label: Int) {
        engine.dispatch(MegaApiGateway::setNodeLabel, listOf(node, label)) { }
    }

    override suspend fun resetNodeLabel(node: MegaNode) {
        engine.dispatch(MegaApiGateway::resetNodeLabel, listOf(node)) { }
    }

    override suspend fun setPublicKeyPinning(enable: Boolean) {
        engine.dispatch(MegaApiGateway::setPublicKeyPinning, listOf(enable)) { }
    }

    override suspend fun changeApiUrl(apiURL: String, disablePkp: Boolean) {
        engine.dispatch(MegaApiGateway::changeApiUrl, listOf(apiURL, disablePkp)) { }
    }

    /** No native copy is possible; the argument itself is returned. */
    override suspend fun copyMegaPushNotificationsSettings(
        pushNotificationSettings: MegaPushNotificationSettings,
    ): MegaPushNotificationSettings? =
        engine.dispatch(
            MegaApiGateway::copyMegaPushNotificationsSettings,
            listOf(pushNotificationSettings),
        ) {
            pushNotificationSettings
        }

    override fun createInstanceMegaPushNotificationSettings(): MegaPushNotificationSettings =
        engine.dispatchBlocking(
            MegaApiGateway::createInstanceMegaPushNotificationSettings,
            emptyList(),
        ) {
            StubMegaPushNotificationSettings()
        }

    override fun unSerializeNode(serializedData: String): MegaNode? =
        engine.dispatchBlocking(MegaApiGateway::unSerializeNode, listOf(serializedData)) { null }

    override fun isGeolocationEnabled(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::isGeolocationEnabled,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_USER,
        )
    }

    override fun enableGeolocation(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::enableGeolocation,
            listOf(listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_USER,
        )
    }

    override fun isCookieBannerEnabled(): Boolean =
        engine.dispatchBlocking(MegaApiGateway::isCookieBannerEnabled, emptyList()) { false }

    override fun getMiscFlags(listener: OptionalMegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::getMiscFlags,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_MISC_FLAGS,
        )
    }

    override fun getUserData(listener: OptionalMegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::getUserData,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_USER_DATA,
        )
    }

    override fun getCookieSettings(listener: OptionalMegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::getCookieSettings,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_USER,
        )
    }

    override fun setCookieSettings(
        bitSetToDecimal: Int,
        listener: OptionalMegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::setCookieSettings,
            listOf(bitSetToDecimal, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_USER,
        )
    }

    override fun shouldShowRichLinkWarning(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::shouldShowRichLinkWarning,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_USER,
        )
    }

    override fun isRichPreviewsEnabled(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::isRichPreviewsEnabled,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_USER,
        )
    }

    override fun setRichLinkWarningCounterValue(
        value: Int,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::setRichLinkWarningCounterValue,
            listOf(value, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_USER,
        )
    }

    override fun enableRichPreviews(enable: Boolean, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::enableRichPreviews,
            listOf(enable, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_USER,
        )
    }

    override fun getSessionTransferURL(path: String, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::getSessionTransferURL,
            listOf(path, listener),
            listener,
            MegaRequest.TYPE_GET_SESSION_TRANSFER_URL,
        )
    }

    override fun getMyUserHandleBinary(): Long =
        engine.dispatchBlocking(MegaApiGateway::getMyUserHandleBinary, emptyList()) {
            account.myUserHandle
        }

    override fun getNodesByFingerprint(fingerprint: String): List<MegaNode> =
        engine.dispatchBlocking(MegaApiGateway::getNodesByFingerprint, listOf(fingerprint)) {
            emptyList()
        }

    override fun killSession(sessionHandle: Long, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::killSession,
            listOf(sessionHandle, listener),
            listener,
            MegaRequest.TYPE_KILL_SESSION,
        )
    }

    override fun confirmCancelAccount(
        link: String,
        pwd: String,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::confirmCancelAccount,
            listOf(link, pwd, listener),
            listener,
            MegaRequest.TYPE_CONFIRM_CANCEL_LINK,
        )
    }

    override fun confirmChangeEmail(
        link: String,
        pwd: String,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::confirmChangeEmail,
            listOf(link, pwd, listener),
            listener,
            MegaRequest.TYPE_CONFIRM_CHANGE_EMAIL_LINK,
        )
    }

    override fun queryCancelLink(link: String, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::queryCancelLink,
            listOf(link, listener),
            listener,
            MegaRequest.TYPE_QUERY_RECOVERY_LINK,
        )
    }

    override fun queryChangeEmailLink(link: String, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::queryChangeEmailLink,
            listOf(link, listener),
            listener,
            MegaRequest.TYPE_QUERY_RECOVERY_LINK,
        )
    }

    override fun resendSignupLink(
        email: String,
        name: String?,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::resendSignupLink,
            listOf(email, name, listener),
            listener,
            MegaRequest.TYPE_SEND_SIGNUP_LINK,
        )
    }

    override fun cancelCreateAccount(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::cancelCreateAccount,
            listOf(listener),
            listener,
            MegaRequest.TYPE_CREATE_ACCOUNT,
        )
    }

    override fun setNodeDescription(
        node: MegaNode,
        description: String?,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::setNodeDescription,
            listOf(node, description, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_NODE,
        )
    }

    override fun addNodeTag(node: MegaNode, tag: String, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::addNodeTag,
            listOf(node, tag, listener),
            listener,
            MegaRequest.TYPE_TAG_NODE,
        )
    }

    override fun removeNodeTag(
        node: MegaNode,
        tag: String,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::removeNodeTag,
            listOf(node, tag, listener),
            listener,
            MegaRequest.TYPE_TAG_NODE,
        )
    }

    override fun updateNodeTag(
        node: MegaNode,
        newTag: String,
        oldTag: String,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::updateNodeTag,
            listOf(node, newTag, oldTag, listener),
            listener,
            MegaRequest.TYPE_TAG_NODE,
        )
    }

    override fun getFlag(flagName: String, commit: Boolean): MegaFlag? =
        engine.dispatchBlocking(MegaApiGateway::getFlag, listOf(flagName, commit)) { null }

    override fun getAllNodeTags(
        searchString: String,
        cancelToken: MegaCancelToken?,
    ): MegaStringList? =
        engine.dispatchBlocking(MegaApiGateway::getAllNodeTags, listOf(searchString, cancelToken)) {
            null
        }

    override fun creditCardCancelSubscriptions(
        reason: String,
        subscriptionId: String,
        canContact: Int,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::creditCardCancelSubscriptions,
            listOf(reason, subscriptionId, canContact, listener),
            listener,
            MegaRequest.TYPE_CREDIT_CARD_CANCEL_SUBSCRIPTIONS,
        )
    }

    override fun moveOrRemoveDeconfiguredBackupNodes(
        deconfiguredBackupRoot: NodeId,
        backupDestination: NodeId,
        listener: MegaRequestListenerInterface?,
    ) {
        completeRequest(
            MegaApiGateway::moveOrRemoveDeconfiguredBackupNodes,
            listOf(deconfiguredBackupRoot, backupDestination, listener),
            listener,
            MegaRequest.TYPE_REMOVE_OLD_BACKUP_NODES,
        )
    }

    override fun enableRequestStatusMonitor() {
        engine.dispatchBlocking(MegaApiGateway::enableRequestStatusMonitor, emptyList()) { }
    }

    override fun createAccount(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::createAccount,
            listOf(email, password, firstName, lastName, listener),
            listener,
            MegaRequest.TYPE_CREATE_ACCOUNT,
        )
    }

    override fun setMyBackupsFolder(
        localizedName: String,
        listener: MegaRequestListenerInterface?,
    ) {
        completeRequest(
            MegaApiGateway::setMyBackupsFolder,
            listOf(localizedName, listener),
            listener,
            MegaRequest.TYPE_SET_MY_BACKUPS,
        )
    }

    override fun getSyncs(): MegaSyncList =
        engine.dispatchBlocking(MegaApiGateway::getSyncs, emptyList()) { StubMegaSyncList() }

    override fun removeVersions(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::removeVersions,
            listOf(listener),
            listener,
            MegaRequest.TYPE_REMOVE_VERSIONS,
        )
    }

    override fun cleanRubbishBin(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::cleanRubbishBin,
            listOf(listener),
            listener,
            MegaRequest.TYPE_CLEAN_RUBBISH_BIN,
        )
    }

    override fun resendVerificationEmail(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::resendVerificationEmail,
            listOf(listener),
            listener,
            MegaRequest.TYPE_RESEND_VERIFICATION_EMAIL,
        )
    }

    override fun resumeCreateAccount(session: String, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::resumeCreateAccount,
            listOf(session, listener),
            listener,
            MegaRequest.TYPE_CREATE_ACCOUNT,
        )
    }

    override fun getBanners(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::getBanners,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_BANNERS,
        )
    }

    override fun dismissBanner(id: Int, listener: MegaRequestListenerInterface?) {
        completeRequest(
            MegaApiGateway::dismissBanner,
            listOf(id, listener),
            listener,
            MegaRequest.TYPE_DISMISS_BANNER,
        )
    }

    override fun getRubbishBinAutopurgePeriod(listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::getRubbishBinAutopurgePeriod,
            listOf(listener),
            listener,
            MegaRequest.TYPE_GET_ATTR_USER,
        )
    }

    override fun setRubbishBinAutopurgePeriod(days: Int, listener: MegaRequestListenerInterface) {
        completeRequest(
            MegaApiGateway::setRubbishBinAutopurgePeriod,
            listOf(days, listener),
            listener,
            MegaRequest.TYPE_SET_ATTR_USER,
        )
    }

    override suspend fun serverSideRubbishBinAutopurgeEnabled(): Boolean =
        engine.dispatch(MegaApiGateway::serverSideRubbishBinAutopurgeEnabled, emptyList()) { true }

    override fun checkRecoveryKey(
        link: String,
        recoveryKey: String,
        listener: MegaRequestListenerInterface,
    ) {
        completeRequest(
            MegaApiGateway::checkRecoveryKey,
            listOf(link, recoveryKey, listener),
            listener,
            MegaRequest.TYPE_CHECK_RECOVERY_KEY,
        )
    }

    override suspend fun resumeTransfersForNotLoggedInInstance() {
        engine.dispatch(MegaApiGateway::resumeTransfersForNotLoggedInInstance, emptyList()) { }
    }

    override fun addRequestListener(listener: MegaRequestListenerInterface) {
        engine.dispatchBlocking(MegaApiGateway::addRequestListener, listOf(listener)) { }
    }

    override fun fileServiceGetReclaimOptions(): MegaFileServiceReclaimOptions? =
        engine.dispatchBlocking(MegaApiGateway::fileServiceGetReclaimOptions, emptyList()) { null }

    override fun fileServiceSetReclaimOptions(options: MegaFileServiceReclaimOptions?) {
        engine.dispatchBlocking(MegaApiGateway::fileServiceSetReclaimOptions, listOf(options)) { }
    }

    override fun fileServiceReclaim(
        options: MegaFileServiceReclaimOptions?,
        listener: MegaRequestListenerInterface?,
    ) {
        completeRequest(
            MegaApiGateway::fileServiceReclaim,
            listOf(options, listener),
            listener,
            MegaRequest.TYPE_FILE_SERVICE_RECLAIM,
        )
    }

    override suspend fun groupAllNodesByDate(
        filter: MegaGroupNodesByDateFilter,
        order: Int,
        cancelToken: MegaCancelToken?,
    ): MegaDateSectionList? =
        engine.dispatch(MegaApiGateway::groupAllNodesByDate, listOf(filter, order, cancelToken)) {
            null
        }

    override suspend fun listAllNodesByPageAtOffset(
        filter: MegaListAllNodesFilter,
        order: Int,
        cancelToken: MegaCancelToken?,
        maxElements: Int,
        offset: Long,
    ): MegaNodeList? =
        engine.dispatch(
            MegaApiGateway::listAllNodesByPageAtOffset,
            listOf(filter, order, cancelToken, maxElements, offset),
        ) {
            null
        }
}
