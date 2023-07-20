package mega.privacy.android.app.getLink

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.getLink.useCase.LegacyExportNodeUseCase
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.usecase.filelink.EncryptLinkWithPasswordUseCase
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

/**
 * View Model used for manage data related to get or manage a link.
 * It is shared by the fragments [GetLinkFragment], [DecryptionKeyFragment], [CopyrightFragment],
 * [LinkPasswordFragment] and their activity [GetLinkActivity].
 *
 * @property megaApi                        MegaApiAndroid instance to use.
 * @property dbH                            DataBaseHandle instance to use.
 * @property encryptLinkWithPasswordUseCase Use case to encrypt a link with a password.
 * @property legacyExportNodeUseCase        Use case to export a node.
 */
@HiltViewModel
class GetLinkViewModel @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val dbH: DatabaseHandler,
    private val encryptLinkWithPasswordUseCase: EncryptLinkWithPasswordUseCase,
    private val legacyExportNodeUseCase: LegacyExportNodeUseCase,
    @ApplicationContext private val context: Context,
) : BaseRxViewModel() {

    private val linkText: MutableLiveData<String> = MutableLiveData()
    private val expiryDate: MutableLiveData<String> = MutableLiveData()
    private val withElevation: MutableLiveData<Boolean> = MutableLiveData()
    private val _linkCopied: MutableStateFlow<Pair<String, String>?> = MutableStateFlow(null)
    val linkCopied = _linkCopied.asStateFlow()

    private val _state = MutableStateFlow(GetLinkUiState())

    /**
     * Get Link Ui State
     */
    val state = _state.asStateFlow()

    private lateinit var linkFragmentTitle: String
    private var node: MegaNode? = null
    private var isSendDecryptedKeySeparatelyEnabled = false

    fun getLink(): LiveData<String> = linkText

    fun getPasswordText(): String? = state.value.password

    fun getExpiryDate(): LiveData<String> = expiryDate

    fun checkElevation(): LiveData<Boolean> = withElevation

    fun setElevation(withElevation: Boolean) {
        this.withElevation.value = withElevation
    }

    fun getNode(): MegaNode? = node

    fun getLinkWithPassword(): String? = state.value.linkWithPassword

    /**
     * Initializes the node and all the available info.
     *
     * @param handle MegaNode identifier.
     */
    fun initNode(handle: Long) {
        updateLink(handle = handle)
        resetLinkWithPassword()
    }

    /**
     * Gets the title to show as [GetLinkFragment] title.
     *
     * @return The title to show.
     */
    fun getLinkFragmentTitle(): String {
        if (!this::linkFragmentTitle.isInitialized) {
            linkFragmentTitle =
                if (node?.isExported == true) {
                    context.getString(R.string.edit_link_option)
                } else {
                    context.resources.getQuantityString(R.plurals.get_links, 1)
                }
        }

        return linkFragmentTitle
    }

    /**
     * Checks if is a password set.
     *
     * @return True if a password is set, false otherwise.
     */
    fun isPasswordSet(): Boolean = !getLinkWithPassword().isNullOrEmpty()

    /**
     * Exports the node.
     */
    fun export(isFirstTime: Boolean = false) {
        legacyExportNodeUseCase.export(node)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    updateLink(node?.handle)
                    if (isFirstTime) {
                        copyLink(true)
                    }
                },
                onError = Timber::w
            )
            .addTo(composite)

    }

    /**
     * Exports the node with an expiry date.
     *
     * @param expiryDate Expiry date to export.
     */
    fun exportWithTimestamp(expiryDate: Long) {
        legacyExportNodeUseCase.export(node, expiryDate)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { updateLink(node?.handle) },
                onError = Timber::w
            )
            .addTo(composite)
    }

    /**
     * Copies the key of the link.
     *
     * @param action Copy action to perform.
     */
    fun copyLinkKey(action: (Pair<String, String>) -> Unit) {
        action.invoke(Pair(state.value.key, context.getString(R.string.key_copied_clipboard)))
    }

    /**
     * Copies the link encrypted with a password.
     *
     * @param action Copy action to perform.
     */
    fun copyLinkPassword(action: (Pair<String, String>) -> Unit) {
        if (getLinkWithPassword().isNullOrEmpty()) {
            return
        }

        action.invoke(
            Pair(
                getPasswordText().orEmpty(),
                context.getString(R.string.password_copied_clipboard)
            )
        )
    }

    /**
     * Reset the password values when password has been removed.
     */
    private fun resetLinkWithPassword() {
        _state.update { it.copy(password = null, linkWithPassword = null) }
    }

    /**
     * Removes the password of the link.
     */
    fun removeLinkWithPassword() {
        resetLinkWithPassword()
        updateLink()
    }

    /**
     * Checks if should show [CopyrightFragment].
     *
     * @return True if should show it, false otherwise.
     */
    fun shouldShowCopyright(): Boolean =
        dbH.shouldShowCopyright && megaApi.publicLinks.isNullOrEmpty()

    /**
     * Updates the flag to show or not [CopyrightFragment] in DB.
     */
    fun updateShowCopyRight(show: Boolean) {
        dbH.setShowCopyright(show)
    }

    /**
     * Updates the link info depending on if send decrypted key separately
     * has been enabled or disabled.
     *
     * @param enabled True if has been enabled, false otherwise.
     */
    fun updateSendDecryptedKeySeparatelyEnabled(enabled: Boolean) {
        isSendDecryptedKeySeparatelyEnabled = enabled
        updateLink()
    }

    /**
     * Checks if the current account is Pro.
     *
     * @return True if the account is Pro, false otherwise.
     */
    fun isPro(): Boolean =
        MegaApplication.getInstance().myAccountInfo.accountType > MegaAccountDetails.ACCOUNT_TYPE_FREE

    /**
     * Updates the node from which the link is getting or managing.
     * Gets the link without its decryption key and the key separately if the node
     * it's already exported.
     *
     * @param handle The identifier of the MegaNode from which the link has to be managed.
     */
    private fun updateLink(handle: Long?) {
        node = handle?.let { megaApi.getNodeByHandle(it) }
        if (node?.isExported == true) {
            val link = node?.publicLink.orEmpty()
            _state.update {
                it.copy(
                    key = LinksUtil.getKeyLink(link),
                    linkWithoutKey = LinksUtil.getLinkWithoutKey(link)
                )
            }
            updateLink()
        }
        expiryDate.value = if ((node?.expirationTime ?: 0) > 0) getExpiredDateText() else ""
    }

    /**
     * Updates the text to show as link value.
     */
    private fun updateLink() {
        linkText.value = when {
            node?.isExported == false -> context.getString(R.string.link_request_status)
            isSendDecryptedKeySeparatelyEnabled -> state.value.linkWithoutKey
            !getLinkWithPassword().isNullOrEmpty() -> getLinkWithPassword().orEmpty()
            else -> node?.publicLink
        }
    }

    fun shareLinkAndKeyOrPassword(action: (Intent) -> Unit) {
        shareLink(getLinkAndKeyOrPasswordToShare(), action)
    }

    fun shareCompleteLink(action: (Intent) -> Unit) {
        shareLink(getLinkToShare(), action)
    }

    /**
     * Launches an intent to share the link outside the app.
     *
     * @param link The link to share.
     */
    fun shareLink(link: String? = null, action: (Intent) -> Unit) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = Constants.TYPE_TEXT_PLAIN
        intent.putExtra(Intent.EXTRA_TEXT, link ?: node?.publicLink)
        action.invoke(Intent.createChooser(intent, context.getString(R.string.context_get_link)))
    }

    fun sendLinkToChat(
        data: Intent?,
        shouldAttachKeyOrPassword: Boolean,
        action: (Intent?) -> Unit,
    ) {
        sendToChat(data, getLinkToShare(), shouldAttachKeyOrPassword, action)
    }

    /**
     * Shares the link and extra content if enabled (decryption key or password) to chat.
     *
     * @param data                      Intent containing the info to share the content to chats.
     * @param link                      The link to share.
     * @param shouldAttachKeyOrPassword True if should share the decryption key or password. False otherwise.
     * @param action                    Action to perform.
     */
    fun sendToChat(
        data: Intent?,
        link: String? = null,
        shouldAttachKeyOrPassword: Boolean,
        action: (Intent?) -> Unit,
    ) {
        data?.putExtra(Constants.EXTRA_LINK, link ?: node?.publicLink)

        if (shouldAttachKeyOrPassword) {
            if (!getLinkWithPassword().isNullOrEmpty()) {
                data?.putExtra(Constants.EXTRA_PASSWORD, getPasswordText())
            } else {
                data?.putExtra(Constants.EXTRA_KEY, state.value.key)
            }
        }

        action.invoke(data)
    }

    /**
     * Gets the string containing the link without its key and its key separately or the link
     * protected with password and the password depending on the current enabled option.
     *
     * @return The string with the info described.
     */
    private fun getLinkAndKeyOrPasswordToShare(): String =
        if (!getLinkWithPassword().isNullOrEmpty()) context.getString(
            R.string.share_link_with_password, getLinkWithPassword(), getPasswordText()
        )
        else context.getString(R.string.share_link_with_key, state.value.linkWithoutKey, state.value.key)

    /**
     * Gets the link to share depending on the current enabled option. It can be:
     * - The link along with its decryption key
     * - The link without the decryption key
     * - The link along with its decryption key and with password protection
     *
     * @return The string with the info described.
     */
    fun getLinkToShare(): String = when {
        !getLinkWithPassword().isNullOrEmpty() -> getLinkWithPassword().orEmpty()
        isSendDecryptedKeySeparatelyEnabled -> state.value.linkWithoutKey
        else -> node?.publicLink.orEmpty()
    }

    /**
     * Checks if should show the warning to share the decryption key or password protection.
     *
     * @return True if password protection or send decryption key separately option is enabled.
     *         False otherwise.
     */
    fun shouldShowShareKeyOrPasswordDialog(): Boolean =
        !getLinkWithPassword().isNullOrEmpty() || isSendDecryptedKeySeparatelyEnabled

    /**
     * Encrypts the link with a password.
     *
     * @param password Password to encrypt the link.
     */
    fun encryptLink(password: String) {
        viewModelScope.launch {
            runCatching {
                encryptLinkWithPasswordUseCase(node?.publicLink.orEmpty(), password)
            }.onSuccess { link ->
                _state.update { it.copy(password = password, linkWithPassword = link) }
                updateLink()
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Gets the expired date formatted.
     *
     * @return The formatted date.
     */
    private fun getExpiredDateText(): String {
        val df = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault())
        val cal = Util.calculateDateFromTimestamp(node?.expirationTime ?: -1)
        val tz = cal.timeZone
        df.timeZone = tz
        val date = cal.time
        return df.format(date)
    }

    /**
     * Copies the link depending on the current configuration.
     *
     * @param isFirstTime if link created for first time
     */
    fun copyLink(isFirstTime: Boolean = false) {
        _linkCopied.value = Pair(
            when {
                isSendDecryptedKeySeparatelyEnabled -> state.value.linkWithoutKey
                !getLinkWithPassword().isNullOrEmpty() -> getLinkWithPassword().orEmpty()
                else -> node?.publicLink.orEmpty()
            },
            if (isFirstTime) context.resources.getQuantityString(
                R.plurals.general_snackbar_link_created_and_copied,
                1
            ) else context.resources.getQuantityString(
                R.plurals.links_copied_clipboard,
                1
            )
        )
    }

    /**
     * Reset link copied flow once value is consumed
     */
    fun resetLink() {
        _linkCopied.value = null
    }
}