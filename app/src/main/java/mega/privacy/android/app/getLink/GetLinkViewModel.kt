package mega.privacy.android.app.getLink

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.getLink.useCase.EncryptLinkWithPasswordUseCase
import mega.privacy.android.app.getLink.useCase.ExportNodeUseCase
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.notifyObserver
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
 * @property exportNodeUseCase              Use case to export a node.
 */
@HiltViewModel
class GetLinkViewModel @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val dbH: DatabaseHandler,
    private val encryptLinkWithPasswordUseCase: EncryptLinkWithPasswordUseCase,
    private val exportNodeUseCase: ExportNodeUseCase,
) : BaseRxViewModel() {

    private val linkText: MutableLiveData<String> = MutableLiveData()
    private val password: MutableLiveData<String> = MutableLiveData()
    private val expiryDate: MutableLiveData<String> = MutableLiveData()
    private val withElevation: MutableLiveData<Boolean> = MutableLiveData()

    private lateinit var linkFragmentTitle: String
    private lateinit var node: MegaNode
    private var linkWithPassword: String? = null
    private lateinit var linkWithoutKey: String
    private lateinit var key: String
    private var isSendDecryptedKeySeparatelyEnabled = false

    fun getLink(): LiveData<String> = linkText
    fun getLinkText(): String = linkText.value ?: ""

    fun getPassword(): LiveData<String> = password
    fun getPasswordText(): String? = password.value

    fun getExpiryDate(): LiveData<String> = expiryDate

    fun checkElevation(): LiveData<Boolean> = withElevation

    fun setElevation(withElevation: Boolean) {
        this.withElevation.value = withElevation
    }

    fun getNode(): MegaNode = node

    fun getLinkWithoutKey(): String = linkWithoutKey

    fun getLinkKey(): String = key

    fun getLinkWithPassword(): String? = linkWithPassword

    /**
     * Initializes the node and all the available info.
     *
     * @param handle MegaNode identifier.
     */
    fun initNode(handle: Long) {
        updateLink(handle)
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
                if (node.isExported) {
                    getString(R.string.edit_link_option)
                } else {
                    getQuantityString(R.plurals.get_links, 1)
                }
        }

        return linkFragmentTitle
    }

    /**
     * Checks if is a password set.
     *
     * @return True if a password is set, false otherwise.
     */
    fun isPasswordSet(): Boolean = !linkWithPassword.isNullOrEmpty()

    /**
     * Exports the node.
     */
    fun export() {
        exportNodeUseCase.export(node)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    updateLink(node.handle)
                    password.notifyObserver()
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
        exportNodeUseCase.export(node, expiryDate)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { updateLink(node.handle) },
                onError = Timber::w
            )
            .addTo(composite)
    }

    /**
     * Copies the link depending on the current configuration.
     *
     * @param action Copy action to perform.
     */
    fun copyLink(action: (Pair<String, String>) -> Unit) {
        action.invoke(
            Pair(
                when {
                    isSendDecryptedKeySeparatelyEnabled -> linkWithoutKey
                    !linkWithPassword.isNullOrEmpty() -> linkWithPassword!!
                    else -> node.publicLink
                }, getQuantityString(R.plurals.links_copied_clipboard, 1)
            )
        )
    }

    /**
     * Copies the key of the link.
     *
     * @param action Copy action to perform.
     */
    fun copyLinkKey(action: (Pair<String, String>) -> Unit) {
        action.invoke(Pair(key, getString(R.string.key_copied_clipboard)))
    }

    /**
     * Copies the link encrypted with a password.
     *
     * @param action Copy action to perform.
     */
    fun copyLinkPassword(action: (Pair<String, String>) -> Unit) {
        if (linkWithPassword.isNullOrEmpty()) {
            return
        }

        action.invoke(Pair(getPasswordText()!!, getString(R.string.password_copied_clipboard)))
    }

    /**
     * Reset the password values when password has been removed.
     */
    private fun resetLinkWithPassword() {
        password.value = null
        linkWithPassword = null
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
        dbH.showCopyright.toBoolean() && (megaApi.publicLinks == null || megaApi.publicLinks.size == 0)

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
    private fun updateLink(handle: Long) {
        node = megaApi.getNodeByHandle(handle)

        if (node.isExported) {
            val link = node.publicLink
            linkWithoutKey = LinksUtil.getLinkWithoutKey(link)
            key = LinksUtil.getKeyLink(link)
        }

        updateLink()

        expiryDate.value = if (node.expirationTime > 0) getExpiredDateText() else ""
    }

    /**
     * Updates the text to show as link value.
     */
    private fun updateLink() {
        linkText.value = when {
            !node.isExported -> getString(R.string.link_request_status)
            isSendDecryptedKeySeparatelyEnabled -> linkWithoutKey
            !linkWithPassword.isNullOrEmpty() -> linkWithPassword
            else -> node.publicLink
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
        intent.putExtra(Intent.EXTRA_TEXT, link ?: node.publicLink)
        action.invoke(Intent.createChooser(intent, getString(R.string.context_get_link)))
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
        data?.putExtra(Constants.EXTRA_LINK, link ?: node.publicLink)

        if (shouldAttachKeyOrPassword) {
            if (!linkWithPassword.isNullOrEmpty()) {
                data?.putExtra(Constants.EXTRA_PASSWORD, getPasswordText())
            } else {
                data?.putExtra(Constants.EXTRA_KEY, key)
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
        if (!linkWithPassword.isNullOrEmpty()) getString(
            R.string.share_link_with_password, linkWithPassword, getPasswordText()
        )
        else getString(R.string.share_link_with_key, linkWithoutKey, key)

    /**
     * Gets the link to share depending on the current enabled option. It can be:
     * - The link along with its decryption key
     * - The link without the decryption key
     * - The link along with its decryption key and with password protection
     *
     * @return The string with the info described.
     */
    fun getLinkToShare(): String = when {
        !linkWithPassword.isNullOrEmpty() -> linkWithPassword!!
        isSendDecryptedKeySeparatelyEnabled -> linkWithoutKey
        else -> node.publicLink
    }

    /**
     * Checks if should show the warning to share the decryption key or password protection.
     *
     * @return True if password protection or send decryption key separately option is enabled.
     *         False otherwise.
     */
    fun shouldShowShareKeyOrPasswordDialog(): Boolean =
        !linkWithPassword.isNullOrEmpty() || isSendDecryptedKeySeparatelyEnabled

    /**
     * Encrypts the link with a password.
     *
     * @param password Password to encrypt the link.
     */
    fun encryptLink(password: String) {
        encryptLinkWithPasswordUseCase.encrypt(node.publicLink, password)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { link ->
                    this.password.value = password
                    this.linkWithPassword = link
                    updateLink()
                },
                onError = Timber::w
            )
            .addTo(composite)
    }

    /**
     * Gets the expired date formatted.
     *
     * @return The formatted date.
     */
    private fun getExpiredDateText(): String {
        val df = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault())
        val cal = Util.calculateDateFromTimestamp(node.expirationTime)
        val tz = cal.timeZone
        df.timeZone = tz
        val date = cal.time
        return df.format(date)
    }
}