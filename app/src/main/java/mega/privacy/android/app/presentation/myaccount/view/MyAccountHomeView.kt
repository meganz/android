package mega.privacy.android.app.presentation.myaccount.view

import android.content.res.Configuration
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavController
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.avatar.model.PhotoAvatarContent
import mega.privacy.android.app.presentation.avatar.model.TextAvatarContent
import mega.privacy.android.app.presentation.avatar.view.Avatar
import mega.privacy.android.app.presentation.changepassword.view.Constants
import mega.privacy.android.app.presentation.myaccount.MyAccountHomeViewActions
import mega.privacy.android.app.presentation.myaccount.model.MyAccountHomeUIState
import mega.privacy.android.app.presentation.myaccount.model.mapper.toAccountAttributes
import mega.privacy.android.app.presentation.myaccount.view.Constants.ACCOUNT_TYPE_SECTION
import mega.privacy.android.app.presentation.myaccount.view.Constants.ACCOUNT_TYPE_TOP_PADDING
import mega.privacy.android.app.presentation.myaccount.view.Constants.ACHIEVEMENTS
import mega.privacy.android.app.presentation.myaccount.view.Constants.ADD_PHONE_NUMBER
import mega.privacy.android.app.presentation.myaccount.view.Constants.AVATAR_SIZE
import mega.privacy.android.app.presentation.myaccount.view.Constants.BACKUP_RECOVERY_KEY
import mega.privacy.android.app.presentation.myaccount.view.Constants.CLICKS_TO_CHANGE_API_SERVER
import mega.privacy.android.app.presentation.myaccount.view.Constants.CONTACTS
import mega.privacy.android.app.presentation.myaccount.view.Constants.EMAIL_TEXT
import mega.privacy.android.app.presentation.myaccount.view.Constants.EXPIRED_BUSINESS_BANNER
import mega.privacy.android.app.presentation.myaccount.view.Constants.EXPIRED_BUSINESS_BANNER_TEXT
import mega.privacy.android.app.presentation.myaccount.view.Constants.HEADER_TOP_PADDING
import mega.privacy.android.app.presentation.myaccount.view.Constants.IMAGE_AVATAR
import mega.privacy.android.app.presentation.myaccount.view.Constants.LAST_SESSION
import mega.privacy.android.app.presentation.myaccount.view.Constants.NAME_TEXT
import mega.privacy.android.app.presentation.myaccount.view.Constants.PAYMENT_ALERT_INFO
import mega.privacy.android.app.presentation.myaccount.view.Constants.PHONE_NUMBER_TEXT
import mega.privacy.android.app.presentation.myaccount.view.Constants.TEXT_AVATAR
import mega.privacy.android.app.presentation.myaccount.view.Constants.TIME_TO_SHOW_PAYMENT_INFO_IN_SECONDS
import mega.privacy.android.app.presentation.myaccount.view.Constants.TOOLBAR_HEIGHT
import mega.privacy.android.app.presentation.myaccount.view.Constants.UPGRADE_BUTTON
import mega.privacy.android.app.presentation.myaccount.view.Constants.USAGE_METER
import mega.privacy.android.app.presentation.myaccount.view.Constants.USAGE_STORAGE_IMAGE
import mega.privacy.android.app.presentation.myaccount.view.Constants.USAGE_STORAGE_PROGRESS
import mega.privacy.android.app.presentation.myaccount.view.Constants.USAGE_STORAGE_SECTION
import mega.privacy.android.app.presentation.myaccount.view.Constants.USAGE_TRANSFER_IMAGE
import mega.privacy.android.app.presentation.myaccount.view.Constants.USAGE_TRANSFER_PROGRESS
import mega.privacy.android.app.presentation.myaccount.view.Constants.USAGE_TRANSFER_SECTION
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.core.ui.controls.MegaSpannedText
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.amber_400
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.extensions.body2medium
import mega.privacy.android.core.ui.theme.extensions.grey_020_black
import mega.privacy.android.core.ui.theme.extensions.grey_050_grey_700
import mega.privacy.android.core.ui.theme.extensions.grey_050_grey_900
import mega.privacy.android.core.ui.theme.extensions.grey_100_alpha_060_grey_100
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.red_600_red_300
import mega.privacy.android.core.ui.theme.extensions.teal_300_teal_200
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.core.ui.theme.extensions.white_alpha_087_grey_alpha_087
import mega.privacy.android.core.ui.theme.extensions.white_black
import mega.privacy.android.core.ui.theme.extensions.white_grey_800
import mega.privacy.android.core.ui.theme.red_400
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import org.jetbrains.anko.displayMetrics
import java.io.File

internal object Constants {
    const val AVATAR_SIZE = 60
    const val CLICKS_TO_CHANGE_API_SERVER = 5
    const val TIME_TO_SHOW_PAYMENT_INFO_IN_SECONDS = 604800
    const val ANIMATION_DURATION = 1000
    val TOOLBAR_HEIGHT = 56.dp
    val HEADER_TOP_PADDING = 24.dp
    val ACCOUNT_TYPE_TOP_PADDING = 48.dp

    // Test Tags
    const val IMAGE_AVATAR = "image_avatar"
    const val TEXT_AVATAR = "text_avatar"
    const val NAME_TEXT = "name_text"
    const val EMAIL_TEXT = "email_text"
    const val PHONE_NUMBER_TEXT = "phone_number_text"
    const val ACCOUNT_TYPE_SECTION = "account_type_section"
    const val UPGRADE_BUTTON = "upgrade_button"
    const val EXPIRED_BUSINESS_BANNER = "expired_business_banner"
    const val EXPIRED_BUSINESS_BANNER_TEXT = "expired_business_banner_text"
    const val PAYMENT_ALERT_INFO = "payment_alert_info"
    const val USAGE_METER = "usage_meter"
    const val USAGE_STORAGE_SECTION = "usage_storage_section"
    const val USAGE_STORAGE_PROGRESS = "usage_storage_progress"
    const val USAGE_STORAGE_IMAGE = "usage_storage_image"
    const val USAGE_TRANSFER_SECTION = "usage_transfer_section"
    const val USAGE_TRANSFER_PROGRESS = "usage_transfer_progress"
    const val USAGE_TRANSFER_IMAGE = "usage_transfer_image"
    const val ADD_PHONE_NUMBER = "add_phone_number"
    const val BACKUP_RECOVERY_KEY = "backup_recovery_key"
    const val CONTACTS = "contacts"
    const val ACHIEVEMENTS = "achievements"
    const val LAST_SESSION = "last_session"
}

/**
 * My Account Home Screen in Jetpack Compose
 */
@Composable
fun MyAccountHomeView(
    uiState: MyAccountHomeUIState,
    uiActions: MyAccountHomeViewActions,
    navController: NavController?,
) {
    val scrollState = rememberScrollState()
    val snackBarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = rememberScaffoldState(),
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState) { data ->
                Snackbar(
                    modifier = Modifier.testTag(Constants.SNACKBAR_TEST_TAG),
                    snackbarData = data,
                    backgroundColor = black.takeIf { MaterialTheme.colors.isLight } ?: white
                )
            }
        }
    ) { padding ->
        val context = LocalContext.current
        val density = LocalDensity.current
        val displayMetrics = context.displayMetrics
        val screenHeight = displayMetrics.heightPixels / displayMetrics.density
        var headerHeight by remember { mutableStateOf(0.dp) }
        var accountTypeHeight by remember { mutableStateOf(0.dp) }

        EventEffect(event = uiState.userMessage, onConsumed = uiActions::resetUserMessage) { res ->
            snackBarHostState.showSnackbar(context.resources.getString(res))
        }

        EventEffect(
            event = uiState.navigateToAchievements,
            onConsumed = uiActions::resetAchievementsNavigationEvent
        ) {
            navController?.navigate(R.id.action_my_account_to_achievements)
        }

        LaunchedEffect(key1 = scrollState.isScrollInProgress) {
            uiActions.onPageScroll(scrollState.canScrollBackward.not())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colors.grey_020_black)
                .verticalScroll(scrollState)
        ) {
            MyAccountHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = HEADER_TOP_PADDING)
                    .onGloballyPositioned { c ->
                        headerHeight = with(density) { c.size.height.toDp() }
                    },
                avatar = uiState.avatar,
                avatarColor = uiState.avatarColor,
                name = uiState.name,
                email = uiState.email,
                verifiedPhoneNumber = uiState.verifiedPhoneNumber,
                onClickUserAvatar = uiActions::onClickUserAvatar,
                onEditProfile = uiActions::onEditProfile
            )

            AccountTypeSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = ACCOUNT_TYPE_TOP_PADDING)
                    .onGloballyPositioned { c ->
                        accountTypeHeight = with(density) { c.size.height.toDp() }
                    },
                accountType = uiState.accountType,
                showButton = (uiState.isBusinessAccount || uiState.accountType == AccountType.PRO_FLEXI).not(),
                buttonText = stringResource(id = R.string.my_account_upgrade_pro),
                onButtonClickListener = {
                    if (uiState.isBusinessAccount.not()) {
                        uiActions.onUpgradeAccount()
                    }
                }
            )

            if (shouldShowPaymentInfo(uiState)) {
                when {
                    uiState.isMasterBusinessAccount && uiState.isBusinessStatusActive.not() -> {
                        ExpiredOrGraceBusinessInfo(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            businessStatus = uiState.businessStatus,
                            backgroundColor = uiState.accountType.toAccountAttributes().background
                        )
                    }

                    (uiState.isMasterBusinessAccount && uiState.isBusinessStatusActive) || uiState.isBusinessAccount.not() -> {
                        PaymentAlertSection(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            renewTime = uiState.subscriptionRenewTime,
                            expirationTime = uiState.proExpirationTime,
                            hasRenewableSubscription = uiState.hasRenewableSubscription,
                            hasExpiringSubscription = uiState.hasExpireAbleSubscription,
                            backgroundColor = uiState.accountType.toAccountAttributes().background
                        )
                    }
                }
            }

            val accountTypeInfoHeight =
                screenHeight.dp - headerHeight - accountTypeHeight - TOOLBAR_HEIGHT - HEADER_TOP_PADDING - ACCOUNT_TYPE_TOP_PADDING

            AccountInfoSection(
                modifier = Modifier
                    .defaultMinSize(minHeight = accountTypeInfoHeight),
                uiState = uiState,
                uiActions = uiActions,
            )
        }
    }
}

@Composable
internal fun MyAccountHeader(
    modifier: Modifier = Modifier,
    avatar: File?,
    avatarColor: Int?,
    name: String?,
    email: String?,
    verifiedPhoneNumber: String?,
    onClickUserAvatar: () -> Unit,
    onEditProfile: () -> Unit,
) {
    ConstraintLayout(modifier = modifier) {
        val (avatarIv, qr, nameTv, edit, emailTv, phoneTv) = createRefs()
        val avatarModifier = Modifier
            .size(AVATAR_SIZE.dp)
            .clickable(onClick = onClickUserAvatar)
            .constrainAs(avatarIv) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start, 17.dp)
            }
        avatar
            ?.takeIf { BitmapFactory.decodeFile(it.absolutePath, BitmapFactory.Options()) != null }
            ?.let {
                Avatar(
                    modifier = avatarModifier.testTag(IMAGE_AVATAR),
                    content = PhotoAvatarContent(it.absolutePath, AVATAR_SIZE.toLong(), false)
                )
            }
            ?: run {
                Avatar(
                    modifier = avatarModifier.testTag(TEXT_AVATAR),
                    content = TextAvatarContent(
                        avatarText = name?.take(1).orEmpty(),
                        backgroundColor = avatarColor ?: -1,
                        showBorder = false
                    )
                )
            }

        Image(
            modifier = Modifier
                .size(10.dp)
                .constrainAs(qr) {
                    bottom.linkTo(avatarIv.bottom, 5.dp)
                    end.linkTo(avatarIv.end, 5.dp)
                }
                .drawBehind {
                    drawCircle(
                        color = Color.White,
                        radius = 24.dp.value
                    )
                },
            painter = painterResource(id = R.drawable.ic_qr_code_scan),
            contentDescription = "QR Code"
        )

        Text(
            modifier = Modifier
                .testTag(NAME_TEXT)
                .clickable(onClick = onEditProfile)
                .constrainAs(nameTv) {
                    top.linkTo(parent.top)
                    linkTo(
                        start = avatarIv.end,
                        end = parent.end,
                        startMargin = 18.dp,
                        endMargin = 40.dp,
                        bias = 0f
                    )
                },
            text = name.orEmpty(),
            style = MaterialTheme.typography.subtitle1.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp
            ),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )

        Icon(
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onEditProfile)
                .constrainAs(edit) {
                    top.linkTo(nameTv.top)
                    bottom.linkTo(nameTv.bottom)
                    start.linkTo(nameTv.end)
                },
            painter = painterResource(id = R.drawable.ic_view_edit_profile),
            contentDescription = "Edit Profile",
            tint = MaterialTheme.colors.teal_300_teal_200
        )

        if (email != null) {
            Text(
                modifier = Modifier
                    .testTag(EMAIL_TEXT)
                    .constrainAs(emailTv) {
                        top.linkTo(nameTv.bottom, 8.dp)
                        linkTo(
                            start = avatarIv.end,
                            end = parent.end,
                            startMargin = 18.dp,
                            endMargin = 40.dp,
                            bias = 0f
                        )
                    },
                text = email,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.textColorSecondary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }

        if (verifiedPhoneNumber != null) {
            Text(
                modifier = Modifier
                    .testTag(PHONE_NUMBER_TEXT)
                    .constrainAs(phoneTv) {
                        top.linkTo(emailTv.bottom, 8.dp)
                        linkTo(
                            start = avatarIv.end,
                            end = parent.end,
                            startMargin = 18.dp,
                            endMargin = 40.dp,
                            bias = 0f
                        )
                    },
                text = verifiedPhoneNumber,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.secondary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun AccountTypeSection(
    modifier: Modifier = Modifier,
    accountType: AccountType?,
    showButton: Boolean,
    buttonText: String,
    onButtonClickListener: () -> Unit,
) {
    val attributes = accountType.toAccountAttributes()

    ConstraintLayout(
        modifier = modifier
            .testTag(ACCOUNT_TYPE_SECTION)
            .fillMaxWidth()
            .height(46.dp)
            .padding(horizontal = 22.dp)
            .clip(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .background(attributes.background)
    ) {
        val (iconIv, typeTv, upgradeBtn) = createRefs()

        Icon(
            modifier = Modifier
                .size(24.dp)
                .constrainAs(iconIv) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start, 10.dp)
                },
            painter = painterResource(id = attributes.icon),
            contentDescription = stringResource(id = attributes.description),
            tint = MaterialTheme.colors.white_black,
        )

        Text(
            modifier = Modifier
                .constrainAs(typeTv) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(iconIv.end, 9.dp)
                },
            text = stringResource(id = attributes.description),
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.white_black,
        )

        if (showButton) {
            TextButton(
                modifier = Modifier
                    .testTag(UPGRADE_BUTTON)
                    .constrainAs(upgradeBtn) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        end.linkTo(parent.end)
                    },
                onClick = onButtonClickListener,
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.button,
                    color = MaterialTheme.colors.white_black,
                )
            }
        }
    }
}

@Composable
internal fun ExpiredOrGraceBusinessInfo(
    modifier: Modifier = Modifier,
    businessStatus: BusinessAccountStatus?,
    backgroundColor: Color,
) {
    AnimatedVisibility(
        visible = true,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Box(
            modifier = modifier
                .testTag(EXPIRED_BUSINESS_BANNER)
                .padding(horizontal = 22.dp)
                .background(backgroundColor)
        ) {
            val businessExpiryText =
                if (businessStatus == BusinessAccountStatus.Expired) R.string.payment_overdue_label else R.string.payment_required_label
            val businessTextColor =
                if (businessStatus == BusinessAccountStatus.Expired) red_400 else amber_400

            Text(
                modifier = Modifier
                    .testTag(EXPIRED_BUSINESS_BANNER_TEXT)
                    .fillMaxWidth()
                    .padding(start = 12.dp, bottom = 10.dp),
                text = stringResource(id = businessExpiryText),
                style = MaterialTheme.typography.body2,
                color = businessTextColor
            )
        }
    }
}

@Composable
private fun PaymentAlertSection(
    modifier: Modifier = Modifier,
    renewTime: Long,
    expirationTime: Long,
    hasRenewableSubscription: Boolean,
    hasExpiringSubscription: Boolean,
    backgroundColor: Color,
) {
    AnimatedVisibility(
        visible = hasRenewableSubscription || hasExpiringSubscription,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Box(
            modifier = modifier
                .testTag(PAYMENT_ALERT_INFO)
                .padding(horizontal = 22.dp)
                .background(backgroundColor)
        ) {
            MegaSpannedText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, bottom = 10.dp),
                value = stringResource(
                    if (hasRenewableSubscription) R.string.account_info_renews_on else R.string.account_info_expires_on,
                    TimeUtils.formatDate(
                        if (hasRenewableSubscription) renewTime else expirationTime,
                        TimeUtils.DATE_MM_DD_YYYY_FORMAT,
                        LocalContext.current
                    )
                ),
                baseStyle = MaterialTheme.typography.subtitle2.copy(color = MaterialTheme.colors.white_alpha_087_grey_alpha_087),
                styles = hashMapOf(
                    SpanIndicator('A') to SpanStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    SpanIndicator('B') to SpanStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            )
        }
    }
}

@Composable
private fun AccountInfoSection(
    modifier: Modifier = Modifier,
    uiState: MyAccountHomeUIState,
    uiActions: MyAccountHomeViewActions,
) {
    var lastSessionClick by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .shadow(3.dp)
            .clip(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .background(MaterialTheme.colors.white_grey_800)
    ) {
        UsageMeterSection(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 8.dp),
            usedStorage = uiState.usedStorage,
            usedTransfer = uiState.usedTransfer,
            totalStorage = uiState.totalStorage,
            totalTransfer = uiState.totalTransfer,
            usedStoragePercentage = uiState.usedStoragePercentage,
            usedTransferPercentage = uiState.usedTransferPercentage,
            showTransfer = uiState.accountType != AccountType.FREE,
            showProgressBar = (uiState.isBusinessAccount || uiState.accountType == AccountType.PRO_FLEXI).not(),
            onUsageMeterClick = uiActions::onClickUsageMeter
        )

        if (uiState.isBusinessAccount && uiState.isMasterBusinessAccount) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 10.dp)
                    .background(MaterialTheme.colors.grey_050_grey_900)
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    text = stringResource(id = R.string.business_management_alert)
                )
            }
        }

        if (uiState.canVerifyPhoneNumber && uiState.verifiedPhoneNumber == null) {
            AccountInfoListItem(
                modifier = Modifier.testTag(ADD_PHONE_NUMBER),
                icon = R.drawable.ic_verify_phone_circle,
                title = R.string.add_phone_label,
                description = if (uiState.isAchievementsEnabled) stringResource(
                    R.string.sms_add_phone_number_dialog_msg_achievement_user,
                    formatSize(size = uiState.bonusStorageSms)
                ) else stringResource(R.string.sms_add_phone_number_dialog_msg_non_achievement_user),
                isIconMode = false,
                testTag = ADD_PHONE_NUMBER,
                onClickListener = {
                    when {
                        uiState.canVerifyPhoneNumber -> uiActions.onAddPhoneNumber()
                        uiActions.isPhoneNumberDialogShown.not() -> uiActions.showPhoneNumberDialog()
                        else -> {}
                    }
                }
            )
        }

        AccountInfoListItem(
            modifier = Modifier.testTag(BACKUP_RECOVERY_KEY),
            icon = R.drawable.ic_recovery_key_circle,
            title = R.string.action_export_master_key,
            description = stringResource(id = R.string.backup_recovery_key_subtitle),
            isIconMode = false,
            onClickListener = uiActions::onBackupRecoveryKey
        )

        AccountInfoListItem(
            modifier = Modifier.testTag(CONTACTS),
            icon = R.drawable.ic_contacts_connections,
            title = R.string.section_contacts,
            description = uiState.visibleContacts?.let {
                pluralStringResource(id = R.plurals.my_account_connections, count = it, it)
            } ?: stringResource(id = R.string.recovering_info),
            isIconMode = true,
            onClickListener = uiActions::onClickContacts
        )

        if (uiState.isBusinessAccount.not()) {
            AccountInfoListItem(
                modifier = Modifier.testTag(ACHIEVEMENTS),
                icon = R.drawable.ic_achievement,
                title = R.string.achievements_title,
                description = stringResource(id = R.string.achievements_subtitle),
                isIconMode = true,
                onClickListener = uiActions::onClickAchievements
            )
        }

        AccountInfoListItem(
            modifier = Modifier.testTag(LAST_SESSION),
            icon = R.drawable.ic_last_session,
            title = R.string.my_account_last_session,
            description = uiState.lastSession?.let {
                TimeUtils.formatDateAndTime(
                    LocalContext.current,
                    it,
                    TimeUtils.DATE_LONG_FORMAT
                )
            } ?: stringResource(id = R.string.recovering_info),
            isIconMode = true,
            withDivider = false,
            onClickListener = {
                lastSessionClick++

                if (lastSessionClick >= CLICKS_TO_CHANGE_API_SERVER) {
                    uiActions.showApiServerDialog()
                    lastSessionClick = 0
                }
            }
        )
    }
}

@Composable
internal fun UsageMeterSection(
    modifier: Modifier = Modifier,
    showTransfer: Boolean,
    showProgressBar: Boolean,
    usedStoragePercentage: Int,
    usedStorage: Long,
    totalStorage: Long,
    usedTransferPercentage: Int,
    usedTransfer: Long,
    totalTransfer: Long,
    onUsageMeterClick: () -> Unit,
) {
    val isStorageOverQuota = remember { usedStoragePercentage > 100 }

    ConstraintLayout(
        modifier = modifier
            .testTag(USAGE_METER)
            .wrapContentHeight()
            .border(1.dp, MaterialTheme.colors.grey_050_grey_700, RoundedCornerShape(4.dp))
            .clickable(onClick = onUsageMeterClick)
    ) {
        val (storageLayout, storageTop, storageBottom, transferLayout, transferTop, transferBottom) = createRefs()
        val guideline = createGuidelineFromStart(0.5f)
        val storageChain =
            createVerticalChain(storageTop, storageBottom, chainStyle = ChainStyle.Packed)
        val transferChain =
            createVerticalChain(transferTop, transferBottom, chainStyle = ChainStyle.Packed)

        Box(
            modifier = Modifier
                .testTag(USAGE_STORAGE_SECTION)
                .constrainAs(storageLayout) {
                    top.linkTo(parent.top, 14.dp)
                    bottom.linkTo(parent.bottom, 14.dp)
                    start.linkTo(parent.start, 14.dp)
                }
        ) {
            if (showProgressBar) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .testTag(USAGE_STORAGE_PROGRESS)
                        .size(50.dp)
                        .align(Alignment.Center),
                    backgroundColor = MaterialTheme.colors.grey_100_alpha_060_grey_100,
                    color = if (isStorageOverQuota) MaterialTheme.colors.red_600_red_300 else MaterialTheme.colors.secondary,
                    strokeWidth = 5.dp,
                    progress = (usedStoragePercentage.toFloat() / 100).coerceAtMost(1f)
                )

                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "$usedStoragePercentage%",
                    style = MaterialTheme.typography.body2medium,
                    color = if (isStorageOverQuota) MaterialTheme.colors.red_600_red_300 else MaterialTheme.colors.secondary
                )
            } else {
                Image(
                    modifier = Modifier
                        .testTag(USAGE_STORAGE_IMAGE)
                        .align(Alignment.Center),
                    painter = painterResource(id = R.drawable.ic_storage),
                    contentDescription = "Storage"
                )
            }
        }

        constrain(storageChain) {
            top.linkTo(storageLayout.top)
            bottom.linkTo(storageLayout.bottom)
        }

        Text(
            modifier = Modifier.constrainAs(storageTop) {
                start.linkTo(storageLayout.end, 9.dp)
                end.linkTo(guideline, 9.dp)
                top.linkTo(storageLayout.top)
                bottom.linkTo(storageBottom.top)
                width = Dimension.fillToConstraints
            },
            text = buildAnnotatedString {
                if (isStorageOverQuota) {
                    withStyle(style = SpanStyle(MaterialTheme.colors.red_600_red_300)) {
                        append(formatSize(size = usedStorage))
                    }
                } else {
                    append(formatSize(size = usedStorage))
                }

                append("/${formatSize(size = totalStorage)}")
            },
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onPrimary,
        )

        Text(
            modifier = Modifier.constrainAs(storageBottom) {
                start.linkTo(storageLayout.end, 9.dp)
                end.linkTo(guideline, 9.dp)
                top.linkTo(storageTop.bottom)
                bottom.linkTo(storageLayout.bottom)
                width = Dimension.fillToConstraints
            },
            text = stringResource(id = R.string.account_storage_label),
            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colors.onPrimary,
        )

        if (showTransfer) {
            Box(
                modifier = Modifier
                    .testTag(USAGE_TRANSFER_SECTION)
                    .wrapContentSize()
                    .constrainAs(transferLayout) {
                        top.linkTo(parent.top, 14.dp)
                        bottom.linkTo(parent.bottom, 14.dp)
                        start.linkTo(guideline)
                    }
            ) {
                if (showProgressBar) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(55.dp)
                            .testTag(USAGE_TRANSFER_PROGRESS),
                        backgroundColor = MaterialTheme.colors.grey_100_alpha_060_grey_100,
                        color = MaterialTheme.colors.secondary,
                        strokeWidth = 6.dp,
                        progress = (usedTransferPercentage.toFloat() / 100).coerceAtMost(1f)
                    )

                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = "$usedTransferPercentage%",
                        style = MaterialTheme.typography.body2medium,
                        color = MaterialTheme.colors.secondary
                    )
                } else {
                    Image(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag(USAGE_TRANSFER_IMAGE),
                        painter = painterResource(id = R.drawable.ic_transfer),
                        contentDescription = "Transfer"
                    )
                }
            }

            constrain(transferChain) {
                top.linkTo(transferLayout.top)
                bottom.linkTo(transferLayout.bottom)
            }

            Text(
                modifier = Modifier.constrainAs(transferTop) {
                    end.linkTo(parent.end, 9.dp)
                    start.linkTo(transferLayout.end, 9.dp)
                    top.linkTo(transferLayout.top)
                    bottom.linkTo(transferBottom.top)
                    width = Dimension.fillToConstraints
                },
                text = "${formatSize(size = usedTransfer)}/${formatSize(size = totalTransfer)}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onPrimary,
            )

            Text(
                modifier = Modifier.constrainAs(transferBottom) {
                    end.linkTo(parent.end, 9.dp)
                    start.linkTo(transferLayout.end, 9.dp)
                    top.linkTo(transferTop.bottom)
                    bottom.linkTo(transferLayout.bottom)
                    width = Dimension.fillToConstraints
                },
                text = stringResource(id = R.string.transfer_label),
                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colors.onPrimary,
            )
        }
    }
}

@Composable
private fun AccountInfoListItem(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    @StringRes title: Int,
    description: String,
    isIconMode: Boolean,
    withDivider: Boolean = true,
    testTag: String = "",
    onClickListener: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }

    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable(
                onClick = onClickListener,
                interactionSource = interactionSource,
                indication = null
            )
    ) {
        val (iconImage, titleTv, subtitleTv, divider) = createRefs()
        val topMargin = if (isIconMode) 14.dp else 10.dp

        if (isIconMode) {
            Icon(
                modifier = Modifier.constrainAs(iconImage) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start, 20.dp)
                },
                painter = painterResource(id = icon),
                tint = MaterialTheme.colors.textColorSecondary,
                contentDescription = stringResource(id = title),
            )
        } else {
            Image(
                modifier = Modifier
                    .size(40.dp)
                    .constrainAs(iconImage) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start, 14.dp)
                    },
                painter = painterResource(id = icon),
                contentDescription = stringResource(id = title)
            )
        }

        Text(
            modifier = Modifier.constrainAs(titleTv) {
                top.linkTo(parent.top, topMargin)
                linkTo(
                    start = parent.start,
                    end = parent.end,
                    startMargin = 72.dp,
                    endMargin = 18.dp,
                    bias = 0f
                )
                width = Dimension.fillToConstraints
            },
            text = stringResource(id = title),
            style = MaterialTheme.typography.subtitle2.copy(
                fontWeight = FontWeight.Medium,
                fontSize = if (isIconMode) 16.sp else 14.sp
            ),
            color = MaterialTheme.colors.onPrimary
        )

        Text(
            modifier = Modifier
                .testTag("${testTag}_description")
                .constrainAs(subtitleTv) {
                    top.linkTo(titleTv.bottom, 3.dp)
                    linkTo(
                        start = parent.start,
                        end = parent.end,
                        startMargin = 72.dp,
                        endMargin = 18.dp,
                        bias = 0f
                    )
                    width = Dimension.fillToConstraints
                },
            text = description,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.textColorSecondary
        )

        if (withDivider) {
            Divider(
                modifier = Modifier.constrainAs(divider) {
                    top.linkTo(subtitleTv.bottom, topMargin)
                    end.linkTo(parent.end)
                    start.linkTo(parent.start, 144.dp)
                },
                color = MaterialTheme.colors.grey_alpha_012_white_alpha_012
            )
        }
    }
}

@Composable
private fun formatSize(size: Long): String = Util.getSizeString(size, LocalContext.current)

private fun shouldShowPaymentInfo(uiState: MyAccountHomeUIState): Boolean {
    val timeToCheck =
        if (uiState.hasRenewableSubscription) uiState.subscriptionRenewTime else uiState.proExpirationTime
    val currentTime = System.currentTimeMillis() / 1000

    return timeToCheck.minus(currentTime) <= TIME_TO_SHOW_PAYMENT_INFO_IN_SECONDS
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "MyAccountHomePreviewDark")
@Composable
internal fun MyAccountHomePreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MyAccountHomeView(
            uiState = MyAccountHomeUIState(
                name = "Kevin Gozali",
                email = "kgozali23@gmail.com",
                verifiedPhoneNumber = "+6282141058000",
                avatarColor = R.color.dark_grey,
                accountType = AccountType.BUSINESS,
                usedTransfer = 100,
                totalTransfer = 1000,
                usedStorage = 500,
                totalStorage = 1000,
                usedStoragePercentage = 130,
                usedTransferPercentage = 10,
                isBusinessAccount = true,
                hasExpireAbleSubscription = true,
                hasRenewableSubscription = true,
                isMasterBusinessAccount = true,
                isBusinessStatusActive = true,
                subscriptionRenewTime = 1000000,
                proExpirationTime = 150000,
                businessStatus = BusinessAccountStatus.Expired
            ),
            uiActions = object : MyAccountHomeViewActions {
                override val isPhoneNumberDialogShown: Boolean
                    get() = false
            },
            navController = null
        )
    }
}