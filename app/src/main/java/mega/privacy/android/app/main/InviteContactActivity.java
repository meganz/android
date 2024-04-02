package mega.privacy.android.app.main;

import static mega.privacy.android.app.main.InvitationContactInfo.TYPE_MANUAL_INPUT_EMAIL;
import static mega.privacy.android.app.main.InvitationContactInfo.TYPE_MANUAL_INPUT_PHONE;
import static mega.privacy.android.app.utils.CallUtil.isNecessaryDisableLocalCamera;
import static mega.privacy.android.app.utils.CallUtil.showConfirmationOpenCamera;
import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_QR;
import static mega.privacy.android.app.utils.Constants.CONTACT_LINK_BASE_URL;
import static mega.privacy.android.app.utils.Constants.OPEN_SCAN_QR;
import static mega.privacy.android.app.utils.Constants.PHONE_NUMBER_REGEX;
import static mega.privacy.android.app.utils.Constants.REQUEST_READ_CONTACTS;
import static mega.privacy.android.app.utils.Constants.TYPE_TEXT_PLAIN;
import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.Util.hideKeyboard;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;
import static mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions;
import static mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.arch.extensions.ViewExtensionsKt;
import mega.privacy.android.app.components.ContactInfoListDialog;
import mega.privacy.android.app.components.ContactsDividerDecoration;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.components.scrollBar.FastScrollerScrollListener;
import mega.privacy.android.app.main.adapters.InvitationContactsAdapter;
import mega.privacy.android.app.presentation.qrcode.QRCodeComposeActivity;
import mega.privacy.android.app.psa.PsaWebBrowser;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.contacts.ContactsFilter;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import timber.log.Timber;

@AndroidEntryPoint
public class InviteContactActivity extends PasscodeActivity implements ContactInfoListDialog.OnMultipleSelectedListener, MegaRequestListenerInterface, InvitationContactsAdapter.OnItemClickListener, View.OnClickListener, TextWatcher, TextView.OnEditorActionListener {

    public static final int SCAN_QR_FOR_INVITE_CONTACTS = 1111;
    private static final String KEY_PHONE_CONTACTS = "KEY_PHONE_CONTACTS";
    private static final String KEY_ADDED_CONTACTS = "KEY_ADDED_CONTACTS";
    private static final String KEY_IS_PERMISSION_GRANTED = "KEY_IS_PERMISSION_GRANTED";
    private static final String KEY_IS_GET_CONTACT_COMPLETED = "KEY_IS_GET_CONTACT_COMPLETED";
    private static final String CURRENT_SELECTED_CONTACT = "CURRENT_SELECTED_CONTACT";
    private static final String CHECKED_INDEX = "CHECKED_INDEX";
    private static final String SELECTED_CONTACT_INFO = "SELECTED_CONTACT_INFO";
    private static final String UNSELECTED = "UNSELECTED";
    private static final int USER_INDEX_NONE_EXIST = -1;
    private static final int MIN_LIST_SIZE_FOR_FAST_SCROLLER = 20;
    private static final int ADDED_CONTACT_VIEW_MARGIN_LEFT = 10;

    public static final String KEY_FROM = "fromAchievement";
    public static final String KEY_SENT_EMAIL = "sentEmail";
    public static final String KEY_SENT_NUMBER = "sentNumber";
    private boolean fromAchievement;

    private InviteContactViewModel viewModel;
    private DisplayMetrics outMetrics;
    private ActionBar aB;
    private RelativeLayout containerContacts;
    private RecyclerView recyclerViewList;
    private ImageView emptyImageView;
    private TextView emptyTextView, emptySubTextView, noPermissionHeader;
    private ProgressBar progressBar;
    private InvitationContactsAdapter invitationContactsAdapter;
    private ArrayList<InvitationContactInfo> phoneContacts, addedContacts;
    private FastScroller fastScroller;
    private FloatingActionButton fabButton;
    private EditText typeContactEditText;
    private HorizontalScrollView scrollView;
    private LinearLayout itemContainer;
    private Handler handler;
    private LayoutInflater inflater;
    private boolean isPermissionGranted, isGetContactCompleted;
    private String contactLink;

    private ArrayList<String> contactsEmailsSelected, contactsPhoneSelected;

    private ContactInfoListDialog listDialog;

    private InvitationContactInfo currentSelected;


    //work around for android bug - https://issuetracker.google.com/issues/37007605#c10
    static class LinearLayoutManagerWrapper extends LinearLayoutManager {

        LinearLayoutManagerWrapper(Context context) {
            super(context);
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                super.onLayoutChildren(recycler, state);
            } catch (IndexOutOfBoundsException e) {
                Timber.d("IndexOutOfBoundsException in RecyclerView happens");
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(InviteContactViewModel.class);
        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        handler = new Handler();
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setContentView(R.layout.activity_invite_contact);

        phoneContacts = new ArrayList<>();
        addedContacts = new ArrayList<>();
        contactsEmailsSelected = new ArrayList<>();
        contactsPhoneSelected = new ArrayList<>();

        Toolbar tB = findViewById(R.id.invite_contact_toolbar);
        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        if (aB != null) {
            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);
            aB.setTitle(getString(R.string.invite_contacts));
            setTitleAB();
        }

        scrollView = findViewById(R.id.scroller);
        itemContainer = findViewById(R.id.label_container);
        fabButton = findViewById(R.id.fab_button_next);
        fabButton.setOnClickListener(this);
        typeContactEditText = findViewById(R.id.type_mail_edit_text);
        typeContactEditText.addTextChangedListener(this);
        typeContactEditText.setOnEditorActionListener(this);
        typeContactEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);

        RelativeLayout scanQRButton = findViewById(R.id.layout_scan_qr);
        scanQRButton.setOnClickListener(this);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManagerWrapper(this);
        recyclerViewList = findViewById(R.id.invite_contact_list);
        recyclerViewList.setOnTouchListener((v, event) -> {
            hideKeyboard(InviteContactActivity.this, 0);
            return false;
        });
        recyclerViewList.setClipToPadding(false);
        recyclerViewList.setItemAnimator(new DefaultItemAnimator());
        recyclerViewList.setLayoutManager(linearLayoutManager);
        recyclerViewList.addItemDecoration(new ContactsDividerDecoration(this));
        invitationContactsAdapter = new InvitationContactsAdapter(
                this,
                viewModel.getFilterUiState().getValue().getFilteredContacts(),
                this,
                megaApi
        );
        recyclerViewList.setAdapter(invitationContactsAdapter);
        containerContacts = findViewById(R.id.container_list_contacts);

        fastScroller = findViewById(R.id.fastScroller);
        fastScroller.setRecyclerView(recyclerViewList);

        emptyImageView = findViewById(R.id.invite_contact_list_empty_image);
        emptyTextView = findViewById(R.id.invite_contact_list_empty_text);
        emptyTextView.setText(R.string.contacts_list_empty_text_loading_share);
        emptySubTextView = findViewById(R.id.invite_contact_list_empty_subtext);
        noPermissionHeader = findViewById(R.id.no_permission_header);

        progressBar = findViewById(R.id.invite_contact_progress_bar);
        refreshInviteContactButton();
        fastScroller.setUpScrollListener(new FastScrollerScrollListener() {
            @Override
            public void onScrolled() {
                hideFabButton();
            }

            @Override
            public void onScrolledToTop() {
                showFabButton();
            }
        });

        //orientation changes
        if (savedInstanceState != null) {
            isGetContactCompleted = savedInstanceState.getBoolean(KEY_IS_GET_CONTACT_COMPLETED, false);
            fromAchievement = savedInstanceState.getBoolean(KEY_FROM, false);
        } else {
            fromAchievement = getIntent().getBooleanExtra(KEY_FROM, false);
        }
        Timber.d("Request by Achievement: %s", fromAchievement);
        if (isGetContactCompleted) {
            if (savedInstanceState != null) {
                phoneContacts = savedInstanceState.getParcelableArrayList(KEY_PHONE_CONTACTS);
                addedContacts = savedInstanceState.getParcelableArrayList(KEY_ADDED_CONTACTS);
                isPermissionGranted = savedInstanceState.getBoolean(KEY_IS_PERMISSION_GRANTED, false);
                currentSelected = savedInstanceState.getParcelable(CURRENT_SELECTED_CONTACT);
            }
            refreshAddedContactsView(true);
            setRecyclersVisibility();
            setTitleAB();
            if (!viewModel.getAllContacts().isEmpty()) {
                setEmptyStateVisibility(false);
            } else if (isPermissionGranted) {
                setEmptyStateVisibility(true);
                showEmptyTextView();
            } else {
                setEmptyStateVisibility(true);
                emptyTextView.setText(R.string.no_contacts_permissions);
                emptyImageView.setVisibility(View.VISIBLE);
                noPermissionHeader.setVisibility(View.VISIBLE);
            }

            if (currentSelected != null) {
                listDialog = new ContactInfoListDialog(this, currentSelected, this);
                if (savedInstanceState != null) {
                    listDialog.setCheckedIndex(savedInstanceState.getIntegerArrayList(CHECKED_INDEX));
                    ArrayList<InvitationContactInfo> selectedList = savedInstanceState.getParcelableArrayList(SELECTED_CONTACT_INFO);
                    if (selectedList != null) {
                        listDialog.setSelected(new HashSet<>(selectedList));
                    }
                    ArrayList<InvitationContactInfo> unSelectedList = savedInstanceState.getParcelableArrayList(UNSELECTED);
                    if (unSelectedList != null) {
                        listDialog.setUnSelected(new HashSet<>(unSelectedList));
                    }
                }
                listDialog.showInfo(addedContacts, true);
            }
        } else {
            queryIfHasReadContactsPermissions();
        }

        MegaRequestListenerInterface getContactLinkCallback = new MegaRequestListenerInterface() {
            @Override
            public void onRequestStart(@NonNull MegaApiJava api, @NonNull MegaRequest request) {

            }

            @Override
            public void onRequestUpdate(@NonNull MegaApiJava api, @NonNull MegaRequest request) {

            }

            @Override
            public void onRequestFinish(@NonNull MegaApiJava api, MegaRequest request, @NonNull MegaError e) {
                if (request.getType() == MegaRequest.TYPE_CONTACT_LINK_CREATE && e.getErrorCode() == MegaError.API_OK) {
                    contactLink = CONTACT_LINK_BASE_URL + MegaApiAndroid.handleToBase64(request.getNodeHandle());
                } else {
                    Timber.w("Create contact link failed.");
                    contactLink = "";
                }
            }

            @Override
            public void onRequestTemporaryError(@NonNull MegaApiJava api, @NonNull MegaRequest request, @NonNull MegaError e) {
                Timber.w("Create contact link temp error.");
                contactLink = "";
            }
        };
        megaApi.contactLinkCreate(false, getContactLinkCallback);

        collectFlows();
    }

    private void collectFlows() {
        ViewExtensionsKt.collectFlow(this, viewModel.getUiState(), Lifecycle.State.STARTED, uiState -> {
            if (uiState.getOnContactsInitialized()) {
                onGetContactCompleted();
                viewModel.filterContacts(typeContactEditText.getText().toString());
                viewModel.resetOnContactsInitializedState();
            }
            return Unit.INSTANCE;
        });

        ViewExtensionsKt.collectFlow(this, viewModel.getFilterUiState(), Lifecycle.State.STARTED, filterUiState -> {
            refreshList();
            visibilityFastScroller();
            return Unit.INSTANCE;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Timber.d("onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_invite_contact, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
        } else if (id == R.id.action_my_qr) {
            initMyQr();
        } else if (id == R.id.action_more) {
            Timber.i("more button clicked - share invitation through other app");
            String message = getResources().getString(R.string.invite_contacts_to_start_chat_text_message, contactLink);
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, message);
            sendIntent.setType(TYPE_TEXT_PLAIN);
            startActivity(Intent.createChooser(sendIntent, getString(R.string.invite_contact_chooser_title)));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Timber.d("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(KEY_PHONE_CONTACTS, phoneContacts);
        outState.putParcelableArrayList(KEY_ADDED_CONTACTS, addedContacts);
        outState.putBoolean(KEY_IS_PERMISSION_GRANTED, isPermissionGranted);
        outState.putBoolean(KEY_IS_GET_CONTACT_COMPLETED, isGetContactCompleted);
        outState.putBoolean(KEY_FROM, fromAchievement);
        outState.putParcelable(CURRENT_SELECTED_CONTACT, currentSelected);
        if (listDialog != null) {
            outState.putIntegerArrayList(CHECKED_INDEX, listDialog.getCheckedIndex());
            outState.putParcelableArrayList(SELECTED_CONTACT_INFO, new ArrayList<Parcelable>(listDialog.getSelected()));
            outState.putParcelableArrayList(UNSELECTED, new ArrayList<Parcelable>(listDialog.getUnSelected()));
        }
    }

    @Override
    public void onBackPressed() {
        Timber.d("onBackPressed");
        PsaWebBrowser psaWebBrowser = getPsaWebBrowser();
        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return;
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listDialog != null) {
            listDialog.recycle();
        }
    }

    private void setTitleAB() {
        Timber.d("setTitleAB");
        if (aB != null) {
            if (!addedContacts.isEmpty()) {
                aB.setSubtitle(getResources().getQuantityString(R.plurals.general_selection_num_contacts,
                        addedContacts.size(), addedContacts.size()));
            } else {
                aB.setSubtitle(null);
            }
        }
    }

    private void setRecyclersVisibility() {
        Timber.d("setRecyclersVisibility %s", viewModel.getAllContacts().size());
        if (!viewModel.getAllContacts().isEmpty()) {
            containerContacts.setVisibility(View.VISIBLE);
        } else {
            containerContacts.setVisibility(View.GONE);
        }
    }

    private void queryIfHasReadContactsPermissions() {
        Timber.d("queryIfHasReadContactsPermissions");
        boolean hasReadContactsPermission = hasPermissions(this, Manifest.permission.READ_CONTACTS);
        if (hasReadContactsPermission) {
            isPermissionGranted = true;
            prepareToGetContacts();
        } else {
            requestPermission(this, Constants.REQUEST_READ_CONTACTS, Manifest.permission.READ_CONTACTS);
        }
    }

    private void prepareToGetContacts() {
        Timber.d("prepareToGetContacts");
        setEmptyStateVisibility(true);
        progressBar.setVisibility(View.VISIBLE);
        viewModel.initializeContacts();
    }

    private void visibilityFastScroller() {
        Timber.d("visibilityFastScroller");
        fastScroller.setRecyclerView(recyclerViewList);
        if (viewModel.getAllContacts().size() < MIN_LIST_SIZE_FOR_FAST_SCROLLER) {
            fastScroller.setVisibility(View.GONE);
        } else {
            fastScroller.setVisibility(View.VISIBLE);
        }
    }

    private void setPhoneAdapterContacts(ArrayList<InvitationContactInfo> contacts) {
        Timber.d("setPhoneAdapterContacts");
        recyclerViewList.post(() -> {
            invitationContactsAdapter.setContactData(contacts);
            invitationContactsAdapter.notifyDataSetChanged();
        });
    }

    private void showEmptyTextView() {
        Timber.d("showEmptyTextView");
        String textToShow = getString(R.string.context_empty_contacts);
        try {
            textToShow = textToShow.replace(
                    "[A]", "<font color=\'"
                            + ColorUtils.getColorHexString(this, R.color.grey_900_grey_100)
                            + "\'>"
            ).replace("[/A]", "</font>").replace(
                    "[B]", "<font color=\'"
                            + ColorUtils.getColorHexString(this, R.color.grey_300_grey_600)
                            + "\'>"
            ).replace("[/B]", "</font>");
        } catch (Exception e) {
            Timber.e(e);
        }

        Spanned result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
        emptyTextView.setText(result);
    }

    private void setEmptyStateVisibility(boolean visible) {
        Timber.d("setEmptyStateVisibility");
        if (visible) {
            emptyImageView.setVisibility(View.VISIBLE);
            emptyTextView.setVisibility(View.VISIBLE);
            emptySubTextView.setVisibility(View.GONE);
        } else {
            emptyImageView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.GONE);
            emptySubTextView.setVisibility(View.GONE);
        }
    }

    public void initScanQR() {
        Timber.d("initScanQR");
        Intent intent = new Intent(this, QRCodeComposeActivity.class);
        intent.putExtra(OPEN_SCAN_QR, true);
        startQRActivity(intent);
    }

    private void initMyQr() {
        Intent intent = new Intent(this, QRCodeComposeActivity.class);
        startQRActivity(intent);
    }

    private void startQRActivity(Intent intent) {
        startActivityForResult(intent, SCAN_QR_FOR_INVITE_CONTACTS);
    }

    private void refreshKeyboard() {
        Timber.d("refreshKeyboard");
        int imeOptions = typeContactEditText.getImeOptions();
        typeContactEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);

        int imeOptionsNew = typeContactEditText.getImeOptions();
        if (imeOptions != imeOptionsNew) {
            View view = getCurrentFocus();
            if (view != null) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (inputMethodManager != null) {
                    inputMethodManager.restartInput(view);
                }
            }
        }
    }

    private boolean isValidEmail(CharSequence target) {
        boolean result = target != null && Constants.EMAIL_ADDRESS.matcher(target).matches();
        Timber.d("isValidEmail%s", result);
        return result;
    }

    private boolean isValidPhone(CharSequence target) {
        boolean result = target != null && PHONE_NUMBER_REGEX.matcher(target).matches();
        Timber.d("isValidPhone%s", result);
        return result;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        refreshKeyboard();
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        Timber.d("onTextChanged: s is %sstart: %d before: %d count: %d", s.toString(), start, before, count);
        if (!TextUtils.isEmpty(s)) {
            String temp = s.toString();
            char last = s.charAt(s.length() - 1);
            if (last == ' ') {
                String processedString = temp.trim();
                boolean isEmailValid = isValidEmail(processedString);
                boolean isPhoneValid = isValidPhone(processedString);
                if (isEmailValid) {
                    addContactInfo(processedString, TYPE_MANUAL_INPUT_EMAIL);
                } else if (isPhoneValid) {
                    addContactInfo(processedString, TYPE_MANUAL_INPUT_PHONE);
                }
                if (isEmailValid || isPhoneValid) {
                    typeContactEditText.getText().clear();
                }
                if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    hideKeyboard(this, 0);
                }
            } else {
                Timber.d("Last character is: %s", last);
            }
        }

        refreshInviteContactButton();
        viewModel.onSearchQueryChange(typeContactEditText.getText().toString());
        refreshKeyboard();
    }

    @Override
    public void afterTextChanged(Editable editable) {
        refreshKeyboard();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        refreshKeyboard();
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            String s = v.getText().toString();
            String processedStrong = s.trim();
            if (!TextUtils.isEmpty(processedStrong)) {
                typeContactEditText.getText().clear();
                boolean isEmailValid = isValidEmail(processedStrong);
                boolean isPhoneValid = isValidPhone(processedStrong);
                if (isEmailValid) {
                    String result = checkInputEmail(processedStrong);
                    if (result != null) {
                        hideKeyboard(this, 0);
                        showSnackbar(result);
                        return true;
                    }
                    addContactInfo(processedStrong, TYPE_MANUAL_INPUT_EMAIL);
                } else if (isPhoneValid) {
                    addContactInfo(processedStrong, TYPE_MANUAL_INPUT_PHONE);
                }
                if (isEmailValid || isPhoneValid) {
                    hideKeyboard(this, 0);
                } else {
                    Toast.makeText(this, R.string.invalid_input, Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (!isScreenInPortrait(this)) {
                    hideKeyboard(this, 0);
                }
                viewModel.filterContacts(typeContactEditText.getText().toString());
            }
            hideKeyboard(this, 0);
            refreshInviteContactButton();
            return true;
        }
        if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_SEND)) {
            if (addedContacts.isEmpty()) {
                hideKeyboard(this, 0);
            } else {
                inviteContacts(addedContacts);
            }
            return true;
        }
        return false;
    }

    private String checkInputEmail(String email) {
        String result = null;
        if (ContactsFilter.isMySelf(megaApi, email)) {
            result = getString(R.string.error_own_email_as_contact);
        }
        if (ContactsFilter.isEmailInContacts(megaApi, email)) {
            result = getString(R.string.context_contact_already_exists, email);
        }
        if (ContactsFilter.isEmailInPending(megaApi, email)) {
            result = getString(R.string.invite_not_sent_already_sent, email);
        }
        return result;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.layout_scan_qr) {
            Timber.d("Scan QR code pressed");
            if (isNecessaryDisableLocalCamera() != MEGACHAT_INVALID_HANDLE) {
                showConfirmationOpenCamera(this, ACTION_OPEN_QR, true);
                return;
            }
            initScanQR();
        } else if (id == R.id.fab_button_next) {
            enableFabButton(false);
            Timber.d("invite Contacts");
            inviteContacts(addedContacts);
            hideKeyboard(this, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        Timber.d("onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Timber.d("Permission granted");
                isPermissionGranted = true;
                prepareToGetContacts();
                noPermissionHeader.setVisibility(View.GONE);
            } else {
                Timber.d("Permission denied");
                setEmptyStateVisibility(true);
                emptyTextView.setText(R.string.no_contacts_permissions);
                emptyImageView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                noPermissionHeader.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onSelect(@NonNull Set<InvitationContactInfo> selected, @NonNull Set<InvitationContactInfo> toRemove) {
        long id = -1;
        cancel();
        for (InvitationContactInfo select : selected) {
            id = select.getId();
            if (!isContactAdded(select)) {
                addedContacts.add(select);
            }
        }
        for (InvitationContactInfo select : toRemove) {
            id = select.getId();
            addedContacts.removeIf(contact -> ContactsFilter.isTheSameContact(contact, select));
        }
        controlHighlighted(id);
        refreshComponents(selected.size() > toRemove.size());
    }

    @Override
    public void cancel() {
        currentSelected = null;
    }

    private void refreshComponents(boolean shouldScroll) {
        refreshAddedContactsView(shouldScroll);
        refreshInviteContactButton();
        //clear input text view after selection
        typeContactEditText.setText("");
        setTitleAB();
    }

    @Override
    public void onItemClick(int position) {
        InvitationContactInfo invitationContactInfo = invitationContactsAdapter.getItem(position);
        Timber.d("on Item click at %d name is %s", position, invitationContactInfo.getContactName());
        if (invitationContactInfo.hasMultipleContactInfos()) {
            this.currentSelected = invitationContactInfo;
            listDialog = new ContactInfoListDialog(this, invitationContactInfo, this);
            listDialog.showInfo(addedContacts, false);
        } else {
            viewModel.toggleContactHighlightedInfo(invitationContactInfo);
            InvitationContactInfo singleInvitationContactInfo = viewModel.getFilterUiState().getValue().getFilteredContacts().get(position);

            boolean shouldScroll;
            if (isContactAdded(singleInvitationContactInfo)) {
                addedContacts.removeIf(contact -> ContactsFilter.isTheSameContact(contact, singleInvitationContactInfo));
                shouldScroll = false;
            } else {
                addedContacts.add(singleInvitationContactInfo);
                shouldScroll = true;
            }
            refreshComponents(shouldScroll);
        }
    }

    private void refreshHorizontalScrollView() {
        handler.postDelayed(() -> scrollView.fullScroll(android.view.View.FOCUS_RIGHT), 100);
    }

    private void onGetContactCompleted() {
        progressBar.setVisibility(View.GONE);
        refreshList();
        setRecyclersVisibility();
        visibilityFastScroller();

        if (!viewModel.getAllContacts().isEmpty()) {
            setEmptyStateVisibility(false);
        } else {
            showEmptyTextView();
        }
    }

    private View createContactTextView(String name, final int id) {
        Timber.d("createTextView contact name is %s", name);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        params.setMargins(dp2px(ADDED_CONTACT_VIEW_MARGIN_LEFT, outMetrics), 0, 0, 0);
        View rowView = inflater.inflate(R.layout.selected_contact_item, null, false);
        rowView.setLayoutParams(params);
        rowView.setId(id);
        rowView.setClickable(true);
        rowView.setOnClickListener(v -> {
            InvitationContactInfo invitationContactInfo = addedContacts.get(v.getId());
            addedContacts.remove(id);
            if (invitationContactInfo.hasMultipleContactInfos()) {
                controlHighlighted(invitationContactInfo.getId());
            } else {
                viewModel.toggleContactHighlightedInfo(invitationContactInfo, false);
            }
            refreshAddedContactsView(false);
            refreshInviteContactButton();
            refreshList();
            setTitleAB();
        });

        TextView displayName = rowView.findViewById(R.id.contact_name);
        displayName.setText(name);
        return rowView;
    }

    private void refreshAddedContactsView(boolean shouldScroll) {
        Timber.d("refreshAddedContactsView");
        itemContainer.removeAllViews();
        for (int i = 0; i < addedContacts.size(); i++) {
            InvitationContactInfo invitationContactInfo = addedContacts.get(i);
            String name = invitationContactInfo.getContactName();
            String displayedLabel;
            if (TextUtils.isEmpty(name)) {
                displayedLabel = invitationContactInfo.getDisplayInfo();
            } else {
                displayedLabel = name;
            }
            itemContainer.addView(createContactTextView(displayedLabel, i));
        }
        itemContainer.invalidate();
        if (shouldScroll) {
            refreshHorizontalScrollView();
        } else {
            scrollView.clearFocus();
        }
    }

    private void controlHighlighted(long id) {
        boolean shouldHighlighted = false;
        for (InvitationContactInfo added : addedContacts) {
            if (added.getId() == id) {
                shouldHighlighted = true;
                break;
            }
        }
        for (InvitationContactInfo temp : invitationContactsAdapter.getData()) {
            if (temp.getId() == id) {
                viewModel.toggleContactHighlightedInfo(temp, shouldHighlighted);
            }
        }
    }

    private boolean isContactAdded(InvitationContactInfo invitationContactInfo) {
        Timber.d("isContactAdded contact name is %s", invitationContactInfo.getContactName());
        for (InvitationContactInfo addedContact : addedContacts) {
            if (ContactsFilter.isTheSameContact(addedContact, invitationContactInfo)) {
                return true;
            }
        }
        return false;
    }

    private void refreshList() {
        Timber.d("refresh list");
        ArrayList<InvitationContactInfo> contacts = new ArrayList<>(viewModel.getFilterUiState().getValue().getFilteredContacts());
        setPhoneAdapterContacts(contacts);
    }

    private void refreshInviteContactButton() {
        Timber.d("refreshInviteContactButton");
        String stringInEditText = typeContactEditText.getText().toString();
        boolean isStringValidNow = stringInEditText.isEmpty()
                || isValidEmail(stringInEditText)
                || isValidPhone(stringInEditText);
        enableFabButton(!addedContacts.isEmpty() && isStringValidNow);
    }

    private void addContactInfo(String inputString, int type) {
        Timber.d("addContactInfo inputString is %s type is %d", inputString, type);
        InvitationContactInfo info = null;
        if (type == TYPE_MANUAL_INPUT_EMAIL) {
            info = InvitationContactInfo.createManualInput(
                    inputString,
                    TYPE_MANUAL_INPUT_EMAIL,
                    R.color.grey_500_grey_400
            );
        } else if (type == TYPE_MANUAL_INPUT_PHONE) {
            info = InvitationContactInfo.createManualInput(
                    inputString,
                    TYPE_MANUAL_INPUT_PHONE,
                    R.color.grey_500_grey_400
            );
        }
        if (info != null) {
            int index = isUserEnteredContactExistInList(info);
            RecyclerView.ViewHolder holder = recyclerViewList.findViewHolderForAdapterPosition(index);
            if (index >= 0 && holder != null) {
                holder.itemView.performClick();
            } else if (!isContactAdded(info)) {
                addedContacts.add(info);
                refreshAddedContactsView(true);
            }
        }
        setTitleAB();
    }

    private void inviteContacts(ArrayList<InvitationContactInfo> addedContacts) {
        // Email/phone contacts to be invited
        contactsEmailsSelected = new ArrayList<>();
        contactsPhoneSelected = new ArrayList<>();

        if (addedContacts != null) {
            for (InvitationContactInfo contact : addedContacts) {
                boolean isEmailContact = contact.isEmailContact();
                if (isEmailContact) {
                    contactsEmailsSelected.add(contact.getDisplayInfo());
                } else {
                    contactsPhoneSelected.add(contact.getDisplayInfo());
                }
            }
        }

        if (!contactsEmailsSelected.isEmpty()) {
            //phone contact will be invited once email done
            inviteEmailContacts(contactsEmailsSelected);
        } else if (!contactsPhoneSelected.isEmpty()) {
            invitePhoneContacts(contactsPhoneSelected);
            finish();
        } else {
            finish();
        }
    }

    private void invitePhoneContacts(ArrayList<String> phoneNumbers) {
        Timber.d("invitePhoneContacts");
        StringBuilder recipient = new StringBuilder("smsto:");
        for (String phone : phoneNumbers) {
            recipient.append(phone);
            recipient.append(";");
            Timber.d("setResultPhoneContacts: %s", phone);
        }
        String smsBody = getResources().getString(R.string.invite_contacts_to_start_chat_text_message, contactLink);
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse(recipient.toString()));
        smsIntent.putExtra("sms_body", smsBody);
        startActivity(smsIntent);
    }

    private int numberToSend;
    private int numberSent;
    private int numberNotSent;

    @Override
    public void onRequestStart(@NonNull MegaApiJava api, @NonNull MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(@NonNull MegaApiJava api, @NonNull MegaRequest request) {

    }

    @Override
    public void onRequestFinish(@NonNull MegaApiJava api, MegaRequest request, @NonNull MegaError e) {
        if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT) {
            Timber.d("MegaRequest.TYPE_INVITE_CONTACT finished: %s", request.getNumber());
            if (e.getErrorCode() == MegaError.API_OK) {
                numberSent++;
                Timber.d("OK INVITE CONTACT: %s", request.getEmail());
            } else {
                numberNotSent++;
                Timber.d("ERROR: %d___%s", e.getErrorCode(), e.getErrorString());
            }
            if (numberSent + numberNotSent == numberToSend) {
                final Intent result = new Intent();
                if (numberToSend == 1) {
                    if (numberSent == 1) {
                        if (!fromAchievement) {
                            showSnackbar(getString(R.string.context_contact_request_sent, request.getEmail()));
                        } else {
                            result.putExtra(KEY_SENT_EMAIL, request.getEmail());
                        }
                    }
                } else {
                    if (numberNotSent > 0) {
                        if (!fromAchievement) {
                            String requestsSent = getResources().getQuantityString(
                                    R.plurals.contact_snackbar_invite_contact_requests_sent,
                                    numberSent,
                                    numberSent);
                            String requestsNotSent = getResources().getQuantityString(
                                    R.plurals.contact_snackbar_invite_contact_requests_not_sent,
                                    numberNotSent,
                                    numberNotSent);
                            String errorMessage = requestsSent.concat(requestsNotSent);
                            showSnackbar(errorMessage);
                        }
                    } else {
                        if (!fromAchievement) {
                            showSnackbar(getResources().getQuantityString(R.plurals.number_correctly_invite_contact_request, numberToSend, numberToSend));
                        } else {
                            result.putExtra(KEY_SENT_NUMBER, numberSent);
                        }
                    }
                }

                hideKeyboard(InviteContactActivity.this, 0);
                new Handler().postDelayed(() -> {
                    if (!contactsPhoneSelected.isEmpty()) {
                        invitePhoneContacts(contactsPhoneSelected);
                    }
                    numberSent = 0;
                    numberToSend = 0;
                    numberNotSent = 0;
                    setResult(Activity.RESULT_OK, result);
                    finish();
                }, 2000);
            }
        }
    }

    @Override
    public void onRequestTemporaryError(@NonNull MegaApiJava api, @NonNull MegaRequest request, @NonNull MegaError e) {

    }

    private void inviteEmailContacts(ArrayList<String> emails) {
        numberToSend = emails.size();
        for (String email : emails) {
            Timber.d("setResultEmailContacts: %s", email);
            megaApi.inviteContact(email, null, MegaContactRequest.INVITE_ACTION_ADD, this);
        }
    }

    public void showSnackbar(String message) {
        showSnackbar(Constants.SNACKBAR_TYPE, scrollView, message, -1);
    }

    private int isUserEnteredContactExistInList(InvitationContactInfo userEnteredInfo) {
        List<InvitationContactInfo> list = invitationContactsAdapter.getData();
        for (int i = 0; i < list.size(); i++) {
            InvitationContactInfo info = list.get(i);
            if (userEnteredInfo.getDisplayInfo().equalsIgnoreCase(info.getDisplayInfo())) {
                return i;
            }
        }
        return USER_INDEX_NONE_EXIST;
    }

    private void enableFabButton(Boolean enableFabButton) {
        Timber.d("enableFabButton: %s", enableFabButton);
        fabButton.setEnabled(enableFabButton);
    }

    /**
     * Shows the fabButton
     */
    private void showFabButton() {
        if (fabButton != null) {
            fabButton.show();
        }
    }

    /**
     * Hides the fabButton
     */
    private void hideFabButton() {
        if (fabButton != null) {
            fabButton.hide();
        }
    }
}
