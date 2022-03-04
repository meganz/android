package mega.privacy.android.app.imageviewer.util

import mega.privacy.android.app.imageviewer.data.ImageItem
import nz.mega.sdk.MegaApiJava

fun ImageItem.shouldShowInfoOption(isUserLoggedIn: Boolean): Boolean =
    isUserLoggedIn && nodeItem?.hasReadAccess == true && !nodeItem.isExternalNode

fun ImageItem.shouldShowFavoriteOption(): Boolean =
    !isOffline && nodeItem?.hasFullAccess == true && !nodeItem.isFromRubbishBin
            && nodeItem.node?.isTakenDown != true && !isFromChat()

fun ImageItem.shouldShowLabelOption(): Boolean =
    shouldShowFavoriteOption()

fun ImageItem.shouldShowDisputeOption(): Boolean =
    nodeItem?.node?.isTakenDown == true

fun ImageItem.shouldShowOpenWithOption(isUserLoggedIn: Boolean): Boolean =
    isUserLoggedIn && nodeItem?.hasReadAccess == true && nodeItem.node?.isTakenDown != true
            && !nodeItem.isFromRubbishBin && !nodeItem.isExternalNode && !isFromChat()

fun ImageItem.shouldShowForwardOption(): Boolean =
    isFromChat()

fun ImageItem.shouldShowDownloadOption(): Boolean =
    nodeItem?.node?.isTakenDown != true && nodeItem?.isFromRubbishBin != true

fun ImageItem.shouldShowOfflineOption(): Boolean =
    !isOffline && nodeItem?.node?.isTakenDown != true && nodeItem?.isFromRubbishBin != true

fun ImageItem.shouldShowManageLinkOption(): Boolean =
    nodeItem?.hasOwnerAccess == true && !nodeItem.isFromRubbishBin
            && nodeItem.node?.isTakenDown != true && !isFromChat()

fun ImageItem.shouldShowRemoveLinkOption(): Boolean =
    shouldShowManageLinkOption() && nodeItem?.node?.isExported == true

fun ImageItem.shouldShowSendToContactOption(isUserLoggedIn: Boolean): Boolean =
    isUserLoggedIn && nodeItem?.hasReadAccess == true && !nodeItem.isFromRubbishBin
            && nodeItem.node?.isTakenDown != true && !nodeItem.isExternalNode && !isFromChat()

fun ImageItem.shouldShowShareOption(): Boolean =
    nodeItem?.node?.isTakenDown != true && nodeItem?.isFromRubbishBin != true
            && (isOffline || nodeItem?.hasOwnerAccess == true || !nodePublicLink.isNullOrBlank())

fun ImageItem.shouldShowRenameOption(): Boolean =
    !isOffline && nodeItem?.isFromRubbishBin != true && nodeItem?.hasFullAccess == true
            && !isFromChat()

fun ImageItem.shouldShowMoveOption(): Boolean =
    shouldShowRenameOption()

fun ImageItem.shouldShowCopyOption(isUserLoggedIn: Boolean): Boolean =
    isUserLoggedIn && !isOffline && nodeItem?.node?.isTakenDown != true
            && nodeItem?.isFromRubbishBin != true

fun ImageItem.shouldShowRestoreOption(): Boolean =
    nodeItem?.isFromRubbishBin == true && nodeItem.node != null
            && nodeItem.node.restoreHandle != MegaApiJava.INVALID_HANDLE

fun ImageItem.shouldShowRubbishBinOption(): Boolean =
    nodeItem?.hasFullAccess == true && !isFromChat()
