package mega.privacy.android.app.imageviewer.util

import mega.privacy.android.app.imageviewer.data.ImageItem
import nz.mega.sdk.MegaApiJava

fun ImageItem.shouldShowInfoOption(isUserLoggedIn: Boolean): Boolean =
    isUserLoggedIn && nodeItem?.hasReadAccess == true && nodeItem?.isExternalNode != true

fun ImageItem.shouldShowFavoriteOption(): Boolean =
    this !is ImageItem.OfflineNode && this !is ImageItem.ChatNode && nodeItem?.hasFullAccess == true
            && nodeItem?.isFromRubbishBin != true && nodeItem?.node?.isTakenDown != true

fun ImageItem.shouldShowLabelOption(): Boolean =
    shouldShowFavoriteOption()

fun ImageItem.shouldShowDisputeOption(): Boolean =
    nodeItem?.node?.isTakenDown == true

fun ImageItem.shouldShowOpenWithOption(isUserLoggedIn: Boolean): Boolean =
    this !is ImageItem.ChatNode && isUserLoggedIn && nodeItem?.hasReadAccess == true
            && nodeItem?.node?.isTakenDown != true && nodeItem?.isFromRubbishBin != true
            && nodeItem?.isExternalNode != true

fun ImageItem.shouldShowForwardOption(): Boolean =
    this is ImageItem.ChatNode

fun ImageItem.shouldShowDownloadOption(): Boolean =
    nodeItem?.node?.isTakenDown != true && nodeItem?.isFromRubbishBin != true

fun ImageItem.shouldShowOfflineOption(): Boolean =
    this !is ImageItem.OfflineNode && nodeItem?.node?.isTakenDown != true && nodeItem?.isFromRubbishBin != true

fun ImageItem.shouldShowManageLinkOption(): Boolean =
    this !is ImageItem.ChatNode && nodeItem?.hasOwnerAccess == true
            && nodeItem?.isFromRubbishBin != true && nodeItem?.node?.isTakenDown != true

fun ImageItem.shouldShowRemoveLinkOption(): Boolean =
    shouldShowManageLinkOption() && nodeItem?.node?.isExported == true

fun ImageItem.shouldShowSendToContactOption(isUserLoggedIn: Boolean): Boolean =
    this !is ImageItem.ChatNode && isUserLoggedIn && nodeItem?.hasReadAccess == true
            && nodeItem?.isFromRubbishBin != true && nodeItem?.node?.isTakenDown != true
            && nodeItem?.isExternalNode != true

fun ImageItem.shouldShowShareOption(): Boolean =
    nodeItem?.node?.isTakenDown != true && nodeItem?.isFromRubbishBin != true
            && (this is ImageItem.OfflineNode || nodeItem?.hasOwnerAccess == true || this !is ImageItem.PublicNode)

fun ImageItem.shouldShowRenameOption(): Boolean =
    this !is ImageItem.OfflineNode && nodeItem?.isFromRubbishBin != true && nodeItem?.hasFullAccess == true
            && this !is ImageItem.ChatNode

fun ImageItem.shouldShowMoveOption(): Boolean =
    shouldShowRenameOption()

fun ImageItem.shouldShowCopyOption(isUserLoggedIn: Boolean): Boolean =
    isUserLoggedIn && this !is ImageItem.OfflineNode && nodeItem?.node?.isTakenDown != true
            && nodeItem?.isFromRubbishBin != true

fun ImageItem.shouldShowRestoreOption(): Boolean =
    nodeItem?.isFromRubbishBin == true && nodeItem?.node != null
            && nodeItem?.node?.restoreHandle != MegaApiJava.INVALID_HANDLE

fun ImageItem.shouldShowChatRemoveOption(): Boolean =
    this is ImageItem.ChatNode && isDeletable

fun ImageItem.shouldShowRubbishBinOption(): Boolean =
    nodeItem?.hasFullAccess == true && this !is ImageItem.ChatNode
