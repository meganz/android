package mega.privacy.android.app.presentation.contact.invite

import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.icon.pack.R as iconPackR
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import mega.privacy.android.app.R
import mega.privacy.android.app.main.InvitationContactInfo
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_MANUAL_INPUT_EMAIL
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_MANUAL_INPUT_PHONE
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_PHONE_CONTACT
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_PHONE_CONTACT_HEADER
import mega.privacy.android.app.main.model.InviteContactUiState
import mega.privacy.android.app.main.model.InviteContactUiState.InvitationStatusMessageUiState.InvitationsSent
import mega.privacy.android.app.main.model.InviteContactUiState.InvitationStatusMessageUiState.NavigateUpWithResult
import mega.privacy.android.app.main.model.InviteContactUiState.MessageTypeUiState.Plural
import mega.privacy.android.app.main.model.InviteContactUiState.MessageTypeUiState.Singular
import mega.privacy.android.app.presentation.contact.invite.actions.InviteContactMenuAction.InviteAFriendViaALink
import mega.privacy.android.app.presentation.contact.invite.actions.InviteContactMenuAction.MyQRCode
import mega.privacy.android.app.presentation.contact.invite.component.ContactInfoListDialog
import mega.privacy.android.app.presentation.contact.invite.navigation.InviteContactScreenResult
import mega.privacy.android.app.presentation.view.open.camera.confirmation.OpenCameraConfirmationDialogRoute
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.EMAIL_ADDRESS
import mega.privacy.android.app.utils.Constants.PHONE_NUMBER_REGEX
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.buttons.MegaFloatingActionButton
import mega.privacy.android.shared.original.core.ui.controls.chip.MegaChip
import mega.privacy.android.shared.original.core.ui.controls.chip.TransparentChipStyle
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.controls.textfields.GenericTextField
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import timber.log.Timber

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun InviteContactRoute(
    isDarkMode: Boolean,
    onNavigateUp: (result: InviteContactScreenResult?) -> Unit,
    onBackPressed: () -> Unit,
    onShareContactLink: (contactLink: String) -> Unit,
    onOpenPersonalQRCode: () -> Unit,
    onOpenQRScanner: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InviteContactViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val localKeyboardController = LocalSoftwareKeyboardController.current

    val snackBarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.shouldInitializeQR) {
        if (uiState.shouldInitializeQR) {
            onOpenQRScanner()
            viewModel.onQRScannerInitialized()
        }
    }

    LaunchedEffect(uiState.pendingPhoneNumberInvitations) {
        if (uiState.pendingPhoneNumberInvitations.isNotEmpty() && uiState.invitationStatusResult == null) {
            invitePhoneContacts(
                context = context,
                phoneNumbers = uiState.pendingPhoneNumberInvitations,
                contactLink = uiState.contactLink
            )
            onNavigateUp(null)
        }
    }

    LaunchedEffect(uiState.emailValidationMessage) {
        uiState.emailValidationMessage?.let {
            if (it is Singular) {
                val message = if (it.argument != null) {
                    context.resources.getString(
                        it.id,
                        it.argument
                    )
                } else context.getString(it.id)
                snackBarHostState.showSnackbar(message)
            }
        }
    }

    LaunchedEffect(uiState.invitationStatusResult) {
        uiState.invitationStatusResult?.let { status ->
            var inviteContactScreenResult: InviteContactScreenResult? = null
            when (status) {
                is NavigateUpWithResult -> {
                    inviteContactScreenResult = InviteContactScreenResult(
                        key = status.result.key,
                        totalInvitationsSent = status.result.totalInvitationsSent
                    )
                }

                is InvitationsSent -> {
                    val message = status.messages.fold("") { acc, messageType ->
                        acc + when (messageType) {
                            is Plural -> {
                                context.resources.getQuantityString(
                                    messageType.id,
                                    messageType.quantity,
                                    messageType.quantity
                                )
                            }

                            is Singular -> {
                                if (messageType.argument != null) {
                                    context.resources.getString(
                                        messageType.id,
                                        messageType.argument
                                    )
                                } else context.resources.getString(messageType.id)
                            }
                        }
                    }
                    snackBarHostState.showSnackbar(message)
                }
            }

            localKeyboardController?.hide()
            delay(NAVIGATE_UP_AFTER_INVITATIONS_DELAY_DURATION)
            if (uiState.pendingPhoneNumberInvitations.isNotEmpty()) {
                invitePhoneContacts(
                    context = context,
                    phoneNumbers = uiState.pendingPhoneNumberInvitations,
                    contactLink = uiState.contactLink
                )
            }
            onNavigateUp(inviteContactScreenResult)
        }
    }

    Box(modifier = modifier.semantics { testTagsAsResourceId = true }) {
        InviteContactScreen(
            modifier = Modifier.fillMaxSize(),
            uiState = uiState,
            isDarkMode = isDarkMode,
            onBackPressed = onBackPressed,
            onInitializeContacts = viewModel::initializeContacts,
            onShareContactLink = onShareContactLink,
            onOpenPersonalQRCode = onOpenPersonalQRCode,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onInviteContactClick = {
                viewModel.inviteContacts()
                localKeyboardController?.hide()
            },
            onScanQRCodeClick = viewModel::validateCameraAvailability,
            onAddContactInfo = viewModel::addContactInfo,
            onContactListItemClick = viewModel::validateContactListItemClick,
            onDoneImeActionClick = {
                val trimmedQuery = uiState.query.trim()
                if (trimmedQuery.isNotBlank()) {
                    viewModel.onSearchQueryChange("")
                    when {
                        isValidEmail(trimmedQuery) -> {
                            viewModel.validateEmailInput(trimmedQuery)
                            localKeyboardController?.hide()
                        }

                        isValidPhone(trimmedQuery) -> {
                            with(viewModel) {
                                addContactInfo(trimmedQuery, TYPE_MANUAL_INPUT_PHONE)
                                filterContacts(uiState.query)
                            }
                            localKeyboardController?.hide()
                        }

                        else -> Toast.makeText(
                            context,
                            R.string.invalid_input,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    localKeyboardController?.hide()
                }
            },
            onContactChipClick = viewModel::onContactChipClick
        )

        if (uiState.showOpenCameraConfirmation) {
            OpenCameraConfirmationDialogRoute(
                onConfirm = {
                    onOpenQRScanner()
                    with(viewModel) {
                        onQRScannerInitialized()
                        onOpenCameraConfirmationShown()
                    }
                },
                onDismiss = viewModel::onOpenCameraConfirmationShown
            )
        }

        uiState.invitationContactInfoWithMultipleContacts?.let {
            ContactInfoListDialog(
                contactInfo = it,
                currentSelectedContactInfo = uiState.selectedContactInformation,
                onConfirm = { newListOfSelectedContact ->
                    with(viewModel) {
                        onDismissContactListContactInfo()
                        updateSelectedContactInfoByInfoWithMultipleContacts(
                            it,
                            newListOfSelectedContact
                        )
                    }
                },
                onCancel = viewModel::onDismissContactListContactInfo
            )
        }

        SnackbarHost(
            modifier = Modifier.align(Alignment.BottomCenter),
            hostState = snackBarHostState
        )
    }
}

@Composable
internal fun InviteContactScreen(
    uiState: InviteContactUiState,
    isDarkMode: Boolean,
    onBackPressed: () -> Unit,
    onInitializeContacts: () -> Unit,
    onShareContactLink: (contactLink: String) -> Unit,
    onOpenPersonalQRCode: () -> Unit,
    onSearchQueryChange: (query: String) -> Unit,
    onInviteContactClick: () -> Unit,
    onScanQRCodeClick: () -> Unit,
    onAddContactInfo: (query: String, type: Int) -> Unit,
    onContactListItemClick: (contactInfo: InvitationContactInfo) -> Unit,
    onDoneImeActionClick: () -> Unit,
    onContactChipClick: (contactInfo: InvitationContactInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val localConfiguration = LocalConfiguration.current
    val localKeyboardController = LocalSoftwareKeyboardController.current

    val isInputValid by remember(uiState.query) { derivedStateOf { isInputValid(uiState.query) } }
    var isInviteButtonEnabled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.selectedContactInformation, isInputValid) {
        isInviteButtonEnabled = uiState.selectedContactInformation.isNotEmpty() && isInputValid
    }

    var showDefaultBody by rememberSaveable { mutableStateOf(true) }
    var isPermissionGranted by rememberSaveable { mutableStateOf(false) }

    val readContactsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Timber.d("Read contacts permission granted")
                showDefaultBody = false
                isPermissionGranted = true
                onInitializeContacts()
            } else {
                Timber.d("Read contacts permission denied")
                showDefaultBody = false
            }
        }

    LaunchedEffect(Unit) {
        if (!isPermissionGranted) {
            readContactsLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    MegaScaffold(
        modifier = modifier,
        topBar = {
            MegaAppBar(
                modifier = Modifier.fillMaxWidth(),
                appBarType = AppBarType.BACK_NAVIGATION,
                onNavigationPressed = onBackPressed,
                title = stringResource(id = R.string.invite_contacts),
                subtitle = if (uiState.selectedContactInformation.isNotEmpty()) pluralStringResource(
                    R.plurals.general_selection_num_contacts,
                    uiState.selectedContactInformation.size,
                    uiState.selectedContactInformation.size
                ) else null,
                actions = listOf(InviteAFriendViaALink, MyQRCode),
                onActionPressed = {
                    when (it) {
                        InviteAFriendViaALink -> onShareContactLink(uiState.contactLink)
                        MyQRCode -> onOpenPersonalQRCode()
                    }
                }
            )
        },
        floatingActionButton = {
            MegaFloatingActionButton(
                modifier = Modifier
                    .padding(16.dp)
                    .size(56.dp)
                    .testTag(INVITE_CONTACT_FAB_TAG),
                enabled = isInviteButtonEnabled,
                onClick = {
                    isInviteButtonEnabled = false
                    onInviteContactClick()
                }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = iconPackR.drawable.ic_send_horizontal_medium_regular_outline),
                    contentDescription = null,
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SelectedContactView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                inputQuery = uiState.query,
                contactNames = uiState.selectedContactInformation,
                onSearchQueryChange = {
                    var newQuery = it
                    if (it.isNotBlank() && it.last().isWhitespace()) {
                        val trimmedQuery = it.trim()
                        if (isValidEmail(trimmedQuery)) {
                            onAddContactInfo(trimmedQuery, TYPE_MANUAL_INPUT_EMAIL)
                            newQuery = ""
                        } else if (isValidPhone(trimmedQuery)) {
                            onAddContactInfo(trimmedQuery, TYPE_MANUAL_INPUT_PHONE)
                            newQuery = ""
                        }
                        if (localConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            localKeyboardController?.hide()
                        }
                    }
                    onSearchQueryChange(newQuery)
                },
                onContactChipClick = onContactChipClick,
                onDone = onDoneImeActionClick
            )

            Divider()

            MegaText(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onScanQRCodeClick
                    )
                    .padding(start = 16.dp)
                    .wrapContentHeight()
                    .testTag(SCAN_QR_CODE_TEXT_TAG),
                text = stringResource(id = R.string.menu_item_scan_code),
                textColor = TextColor.Accent,
                style = MaterialTheme.typography.subtitle1,
            )

            Divider()

            when {
                // rememberSaveable won't work in a fragment because the fragment itself will be destroyed.
                // The workaround is to add !uiState.isContactsInitialized until we fully migrate
                // to a single activity.
                showDefaultBody && !uiState.areContactsInitialized -> DefaultContactListBody(
                    modifier = Modifier.fillMaxWidth()
                )

                uiState.isLoading -> ContactListLoadingBody(
                    modifier = Modifier.fillMaxWidth(),
                    isDarkMode = isDarkMode
                )

                uiState.areContactsInitialized -> {
                    if (uiState.filteredContacts.isEmpty()) {
                        EmptyContactResultBody(
                            modifier = Modifier.fillMaxWidth(),
                            isDarkMode = isDarkMode
                        )
                    } else {
                        ContactListBody(
                            modifier = Modifier.fillMaxSize(),
                            contacts = uiState.filteredContacts,
                            onContactClick = onContactListItemClick
                        )
                    }
                }

                !isPermissionGranted -> ContactsPermissionDeniedBody(
                    modifier = Modifier.fillMaxWidth(),
                    isDarkMode = isDarkMode
                )
            }
        }
    }
}

private fun isInputValid(input: String): Boolean =
    input.isEmpty() || isValidEmail(input) || isValidPhone(input)

private fun isValidEmail(email: String) = EMAIL_ADDRESS.matcher(email).matches()

private fun isValidPhone(number: String) = PHONE_NUMBER_REGEX.matcher(number).matches()

@Composable
private fun SelectedContactView(
    inputQuery: String,
    contactNames: List<InvitationContactInfo>,
    onSearchQueryChange: (query: String) -> Unit,
    onContactChipClick: (contactInfo: InvitationContactInfo) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    var lastItemSize by remember { mutableStateOf(contactNames.size) }
    LaunchedEffect(contactNames.size) {
        // Only scroll if the user add new contact.
        if (contactNames.size > lastItemSize) {
            scrollState.scrollTo(scrollState.maxValue)
        }
        lastItemSize = contactNames.size
    }

    // We will process the text whenever it changes.
    // Therefore, we need this local query state to update the UI immediately.
    var query by remember(inputQuery) { mutableStateOf(inputQuery) }

    Row(
        modifier = modifier.horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactChipBar(
            modifier = Modifier
                .padding(start = 10.dp)
                .testTag(CONTACT_CHIP_BAR_TAG),
            values = contactNames,
            onClick = onContactChipClick
        )

        GenericTextField(
            modifier = Modifier
                .defaultMinSize(minWidth = 100.dp)
                .padding(start = 6.dp, end = 18.dp),
            text = query,
            placeholder = stringResource(id = R.string.type_mail),
            singleLine = true,
            onTextChange = {
                query = it
                onSearchQueryChange(query)
            },
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colors.secondary,
            )
        )
    }
}

@Composable
private fun ContactChipBar(
    values: List<InvitationContactInfo>,
    onClick: (contactInfo: InvitationContactInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        values.forEach {
            MegaChip(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .testTag(SELECTED_CONTACT_CHIP_TAG + it.getContactName()),
                selected = false,
                text = it.getContactName(),
                style = TransparentChipStyle,
                trailingIcon = IconPackR.drawable.ic_x_circle_medium_regular_solid,
                onClick = { onClick(it) }
            )
        }
    }
}

@Composable
private fun DefaultContactListBody(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MegaText(
            modifier = Modifier.testTag(DEFAULT_BODY_LOADING_TEXT_TAG),
            text = stringResource(id = R.string.contacts_list_empty_text_loading_share),
            textColor = TextColor.Disabled,
            style = MaterialTheme.typography.body2
        )

        MegaText(
            modifier = Modifier
                .padding(start = 34.dp, end = 34.dp, top = 12.dp)
                .testTag(DEFAULT_BODY_DESCRIPTION_TEXT_TAG),
            text = stringResource(id = R.string.invite_contacts_to_start_chat),
            textColor = TextColor.Disabled,
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ContactListLoadingBody(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyContactsImage(isDarkMode)

        MegaText(
            modifier = Modifier.testTag(CONTACT_LIST_LOADING_TEXT_TAG),
            text = stringResource(id = R.string.contacts_list_empty_text_loading_share),
            textColor = TextColor.Disabled,
            style = MaterialTheme.typography.body2
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            MegaCircularProgressIndicator(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .align(Alignment.Center)
                    .testTag(CIRCULAR_LOADING_INDICATOR_TAG)
            )
        }
    }
}

@Composable
private fun EmptyContactResultBody(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyContactsImage(isDarkMode)

        MegaSpannedText(
            modifier = Modifier.testTag(NO_CONTACTS_TEXT_TAG),
            value = stringResource(id = R.string.context_empty_contacts),
            baseStyle = MaterialTheme.typography.body2,
            styles = mapOf(
                SpanIndicator('A') to MegaSpanStyle(
                    color = TextColor.Primary
                ),
                SpanIndicator('B') to MegaSpanStyle(
                    color = TextColor.Disabled
                )
            ),
            color = TextColor.Disabled
        )
    }
}

@Composable
private fun ContactListBody(
    contacts: List<InvitationContactInfo>,
    onContactClick: (contact: InvitationContactInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.testTag(CONTACT_LIST_BODY_TAG)) {
        items(
            items = contacts,
            key = { item -> item.id },
            contentType = { item -> item.type }
        ) { item ->
            when (item.type) {
                TYPE_PHONE_CONTACT_HEADER -> {
                    PhoneContactsHeader(modifier = Modifier.fillMaxWidth())
                }

                TYPE_PHONE_CONTACT -> {
                    ContactItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clickable { onContactClick(item) },
                        item = item
                    )

                    Divider(modifier = Modifier.padding(start = 71.dp))
                }
            }
        }
    }
}

@Composable
private fun ContactItem(
    item: InvitationContactInfo,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(
            modifier = Modifier
                .padding(start = 18.dp)
                .size(40.dp)
                .clip(CircleShape),
            name = item.getContactName(),
            uri = item.photoUri,
            isHighlighted = item.isHighlighted
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 13.dp),
            verticalArrangement = Arrangement.Center
        ) {
            MegaText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp)
                    .testTag(CONTACT_NAME_TAG + item.getContactName()),
                text = item.getContactName(),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.subtitle1,
                overflow = LongTextBehaviour.MiddleEllipsis
            )

            MegaText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp)
                    .testTag(CONTACT_DISPLAY_INFO_TAG + item.displayInfo),
                text = item.displayInfo,
                textColor = TextColor.Secondary,
                style = MaterialTheme.typography.subtitle2,
                overflow = LongTextBehaviour.Ellipsis(maxLines = 1)
            )
        }
    }
}

// We can't simply use AsyncImage, It will cause every avatar to blink when the screen recomposes.
@Composable
private fun ContactAvatar(
    name: String,
    uri: String?,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isHighlighted) {
        Image(
            modifier = modifier.testTag(HIGHLIGHTED_CONTACT_AVATAR_TAG),
            painter = painterResource(id = R.drawable.ic_chat_avatar_select),
            contentDescription = null
        )
    } else {
        if (uri != null) {
            AsyncImage(
                modifier = modifier.testTag(CONTACT_AVATAR_WITH_URI_TAG + uri),
                model = uri,
                contentDescription = null
            )
        } else {
            Image(
                modifier = modifier.testTag(CONTACT_DEFAULT_AVATAR_TAG),
                bitmap = getDefaultAvatarBitmap(contactName = name).asImageBitmap(),
                contentDescription = null
            )
        }
    }
}

@Composable
private fun getDefaultAvatarBitmap(contactName: String): Bitmap =
    AvatarUtil.getDefaultAvatar(
        colorResource(id = R.color.grey_500_grey_400).toArgb(),
        contactName,
        Constants.AVATAR_SIZE,
        true,
        false
    )

@Composable
private fun ContactsPermissionDeniedBody(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PhoneContactsHeader(modifier = Modifier.fillMaxWidth())

        EmptyContactsImage(isDarkMode)

        MegaText(
            modifier = Modifier.testTag(PERMISSION_DENIED_TEXT_TAG),
            text = stringResource(id = R.string.no_contacts_permissions),
            textColor = TextColor.Disabled,
            style = MaterialTheme.typography.body2
        )
    }
}

@Composable
private fun PhoneContactsHeader(
    modifier: Modifier = Modifier,
) {
    MegaText(
        modifier = modifier
            .padding(top = 10.dp, bottom = 10.dp, start = 16.dp)
            .testTag(PHONE_CONTACTS_HEADER_TEXT_TAG),
        text = stringResource(id = R.string.contacts_phone),
        textColor = TextColor.Primary,
        style = MaterialTheme.typography.subtitle1
    )
}

@Composable
private fun EmptyContactsImage(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
) {
    Image(
        modifier = modifier
            .alpha(if (isDarkMode) 0.16F else 1F)
            .testTag(EMPTY_CONTACTS_IMAGE_TAG),
        painter = painterResource(id = R.drawable.ic_empty_contacts),
        contentDescription = null
    )
}

private fun invitePhoneContacts(
    context: Context,
    phoneNumbers: List<String>,
    contactLink: String,
) {
    Timber.d("invitePhoneContacts")
    val recipient = buildString {
        append("smsto:")
        phoneNumbers.forEach { phone ->
            append(phone)
            append(";")
            Timber.d("setResultPhoneContacts: $phone")
        }
    }
    val smsBody = context.resources.getString(
        R.string.invite_contacts_to_start_chat_text_message,
        contactLink
    )
    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse(recipient))
    smsIntent.putExtra("sms_body", smsBody)
    context.startActivity(smsIntent)
}

@CombinedTextAndThemePreviews
@Composable
private fun InviteContactScreenWithoutSelectedContactsPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        InviteContactScreen(
            uiState = InviteContactUiState(),
            isDarkMode = isSystemInDarkTheme(),
            onBackPressed = {},
            onInitializeContacts = {},
            onShareContactLink = {},
            onOpenPersonalQRCode = {},
            onSearchQueryChange = {},
            onInviteContactClick = {},
            onScanQRCodeClick = {},
            onAddContactInfo = { _, _ -> },
            onContactListItemClick = {},
            onDoneImeActionClick = {},
            onContactChipClick = {}
        )
    }
}

@CombinedTextAndThemePreviews
@Composable
private fun InviteContactScreenWithSelectedContactsPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        InviteContactScreen(
            uiState = InviteContactUiState(
                areContactsInitialized = true,
                filteredContacts = listOf(
                    InvitationContactInfo(name = "Abc"),
                    InvitationContactInfo(name = "Abc ss", displayInfo = "(021) 232-3232"),
                    InvitationContactInfo(name = "Abc 2"),
                    InvitationContactInfo(name = "Abc 3"),
                    InvitationContactInfo(name = "Abc 4"),
                    InvitationContactInfo(name = "Abc 5"),
                ),
                selectedContactInformation = listOf(
                    InvitationContactInfo(name = "Abc"),
                    InvitationContactInfo(displayInfo = "Abc ss"),
                    InvitationContactInfo(name = "Abc 2"),
                    InvitationContactInfo(name = "Abc 3"),
                    InvitationContactInfo(name = "Abc 4"),
                    InvitationContactInfo(name = "Abc 5"),
                )
            ),
            isDarkMode = isSystemInDarkTheme(),
            onBackPressed = {},
            onInitializeContacts = {},
            onShareContactLink = {},
            onOpenPersonalQRCode = {},
            onSearchQueryChange = {},
            onInviteContactClick = {},
            onScanQRCodeClick = {},
            onAddContactInfo = { _, _ -> },
            onContactListItemClick = {},
            onDoneImeActionClick = {},
            onContactChipClick = {}
        )
    }
}

internal const val INVITE_CONTACT_FAB_TAG =
    "invite_contact_screen:floating_action_button_invite_contacts"
internal const val SCAN_QR_CODE_TEXT_TAG = "invite_contact_screen:text_scan_qr_code"
internal const val CONTACT_CHIP_BAR_TAG =
    "selected_contact_view:chip_bar_selected_contact_chips_wrapper"
internal const val SELECTED_CONTACT_CHIP_TAG = "contact_chip_bar:chip_selected_contact"
internal const val DEFAULT_BODY_LOADING_TEXT_TAG = "default_contact_list_body:text_loading"
internal const val DEFAULT_BODY_DESCRIPTION_TEXT_TAG =
    "default_contact_list_body:text_default_description"
internal const val EMPTY_CONTACTS_IMAGE_TAG = "empty_contacts_image:image_empty_contacts"
internal const val CONTACT_LIST_LOADING_TEXT_TAG = "default_contact_list_loading_body:text_loading"
internal const val CIRCULAR_LOADING_INDICATOR_TAG =
    "default_contact_list_loading_body:circular_loading_indicator"
internal const val NO_CONTACTS_TEXT_TAG = "empty_contact_result_body:text_no_contacts"
internal const val PHONE_CONTACTS_HEADER_TEXT_TAG = "phone_contacts_header:text_phone_contacts"
internal const val HIGHLIGHTED_CONTACT_AVATAR_TAG = "contact_avatar:image_highlighted_avatar"
internal const val CONTACT_AVATAR_WITH_URI_TAG = "contact_avatar:image_with_uri"
internal const val CONTACT_DEFAULT_AVATAR_TAG = "contact_avatar:image_default"
internal const val CONTACT_NAME_TAG = "contact_item:text_contact_name"
internal const val CONTACT_DISPLAY_INFO_TAG = "contact_item:text_contact_display_info"
internal const val PERMISSION_DENIED_TEXT_TAG =
    "contacts_permission_denied_body:text_contact_permission_denied"
internal const val CONTACT_LIST_BODY_TAG = "contact_list_body:lazy_column_contact_list"

private const val NAVIGATE_UP_AFTER_INVITATIONS_DELAY_DURATION = 2000L
