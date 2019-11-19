package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
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
import android.view.MotionEvent;
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

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.ContactsDividerDecoration;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.lollipop.adapters.InvitationContactsAdapter;
import mega.privacy.android.app.lollipop.qrcode.QRCodeActivity;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.utils.contacts.ContactsFilter;
import mega.privacy.android.app.utils.contacts.ContactsUtil;
import mega.privacy.android.app.utils.contacts.MegaContactGetter;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.lollipop.InvitationContactInfo.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;


public class InviteContactActivity extends PinActivityLollipop implements MegaRequestListenerInterface, InvitationContactsAdapter.OnItemClickListener, View.OnClickListener, TextWatcher, TextView.OnEditorActionListener, MegaContactGetter.MegaContactUpdater {

    public static final int SCAN_QR_FOR_INVITE_CONTACTS = 1111;
    public static final String INVITE_CONTACT_SCAN_QR = "inviteContacts";
    private static final String KEY_PHONE_CONTACTS = "KEY_PHONE_CONTACTS";
    private static final String KEY_MEGA_CONTACTS = "KEY_MEGA_CONTACTS";
    private static final String KEY_ADDED_CONTACTS = "KEY_ADDED_CONTACTS";
    private static final String KEY_FILTERED_CONTACTS = "KEY_FILTERED_CONTACTS";
    private static final String KEY_TOTAL_CONTACTS = "KEY_TOTAL_CONTACTS";
    private static final String KEY_IS_PERMISSION_GRANTED = "KEY_IS_PERMISSION_GRANTED";
    private static final String KEY_IS_GET_CONTACT_COMPLETED = "KEY_IS_GET_CONTACT_COMPLETED";
    private static final int ID_MEGA_CONTACTS_HEADER = -2;
    private static final int ID_PHONE_CONTACTS_HEADER = -1;
    private static final int PHONE_NUMBER_MIN_LENGTH = 5;
    private static final int USER_INDEX_NONE_EXIST = -1;
    private static final int MIN_LIST_SIZE_FOR_FAST_SCROLLER = 20;
    private static final int ADDED_CONTACT_VIEW_MARGIN_LEFT = 10;

    private DisplayMetrics outMetrics;
    private ActionBar aB;
    private RelativeLayout containerContacts;
    private RecyclerView recyclerViewList;
    private ImageView emptyImageView;
    private TextView emptyTextView, emptySubTextView, noPermissionHeader;
    private ProgressBar progressBar;
    private String inputString, defaultLocalContactAvatarColor;
    private InvitationContactsAdapter invitationContactsAdapter;
    private ArrayList<InvitationContactInfo> phoneContacts, megaContacts, addedContacts, filteredContacts, totalContacts;
    private FilterContactsTask filterContactsTask;
    private FastScroller fastScroller;
    private FloatingActionButton fabButton;
    private EditText typeContactEditText;
    private HorizontalScrollView scrollView;
    private LinearLayout itemContainer;
    private Handler handler;
    private LayoutInflater inflater;
    private boolean isGettingLocalContact, isGettingMegaContact, isPermissionGranted, isGetContactCompleted;
    private MegaContactGetter megaContactGetter;
    private List<ContactsUtil.LocalContact> rawLocalContacts;
    private String contactLink;
    private DatabaseHandler dbH;
    private ArrayList<String> contactsEmailsSelected, contactsPhoneSelected;

    //work around for android bug - https://issuetracker.google.com/issues/37007605#c10
    class LinearLayoutManagerWrapper extends LinearLayoutManager {

        LinearLayoutManagerWrapper(Context context) {
            super(context);
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                super.onLayoutChildren(recycler, state);
            } catch (IndexOutOfBoundsException e) {
                logDebug("IndexOutOfBoundsException in RecyclerView happens");
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        logDebug("onCreate");

        super.onCreate(savedInstanceState);
        dbH = DatabaseHandler.getDbHandler(this);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dark_primary_color));
        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        handler = new Handler();
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setContentView(R.layout.activity_invite_contact);
        Context context = getApplicationContext();
        defaultLocalContactAvatarColor = "#" + Integer.toHexString(ContextCompat.getColor(context, R.color.color_default_avatar_phone));

        phoneContacts = new ArrayList<>();
        addedContacts = new ArrayList<>();
        filteredContacts = new ArrayList<>();
        totalContacts = new ArrayList<>();
        megaContacts = new ArrayList<>();
        contactsEmailsSelected = new ArrayList<>();
        contactsPhoneSelected = new ArrayList<>();

        megaContactGetter = new MegaContactGetter(context);
        megaContactGetter.setMegaContactUpdater(InviteContactActivity.this);

        Toolbar tB = findViewById(R.id.invite_contact_toolbar);
        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        if (aB != null) {
            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);
            aB.setTitle(getString(R.string.invite_contacts).toUpperCase());
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
        typeContactEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (filterContactsTask != null && filterContactsTask.getStatus() == AsyncTask.Status.RUNNING) {
                        filterContactsTask.cancel(true);
                    }
                    startFilterTask();
                }
            }
        });

        RelativeLayout scanQRButton = findViewById(R.id.layout_scan_qr);
        scanQRButton.setOnClickListener(this);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManagerWrapper(this);
        recyclerViewList = findViewById(R.id.invite_contact_list);
        recyclerViewList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Util.hideKeyboard(InviteContactActivity.this, 0);
                return false;
            }
        });
        recyclerViewList.setClipToPadding(false);
        recyclerViewList.setHasFixedSize(true);
        recyclerViewList.setItemAnimator(new DefaultItemAnimator());
        recyclerViewList.setLayoutManager(linearLayoutManager);
        recyclerViewList.addItemDecoration(new ContactsDividerDecoration(this, outMetrics));
        invitationContactsAdapter = new InvitationContactsAdapter(this, filteredContacts, this, megaApi);
        recyclerViewList.setAdapter(invitationContactsAdapter);
        containerContacts = findViewById(R.id.container_list_contacts);

        fastScroller = findViewById(R.id.fastScroller);
        fastScroller.setRecyclerView(recyclerViewList);

        emptyImageView = findViewById(R.id.invite_contact_list_empty_image);
        emptyTextView = findViewById(R.id.invite_contact_list_empty_text);
        emptyTextView.setText(R.string.contacts_list_empty_text_loading_share);
        emptySubTextView = findViewById(R.id.invite_contact_list_empty_subtext);
        noPermissionHeader = findViewById(R.id.no_permission_header);

        progressBar = findViewById(R.id.add_contact_progress_bar);
        progressBar = findViewById(R.id.invite_contact_progress_bar);
        refreshInviteContactButton();

        //orientation changes
        if (savedInstanceState != null) {
            isGetContactCompleted = savedInstanceState.getBoolean(KEY_IS_GET_CONTACT_COMPLETED, false);
        }

        if (isGetContactCompleted) {
            phoneContacts = savedInstanceState.getParcelableArrayList(KEY_PHONE_CONTACTS);
            megaContacts = savedInstanceState.getParcelableArrayList(KEY_MEGA_CONTACTS);
            addedContacts = savedInstanceState.getParcelableArrayList(KEY_ADDED_CONTACTS);
            filteredContacts = savedInstanceState.getParcelableArrayList(KEY_FILTERED_CONTACTS);
            totalContacts = savedInstanceState.getParcelableArrayList(KEY_TOTAL_CONTACTS);
            isPermissionGranted = savedInstanceState.getBoolean(KEY_IS_PERMISSION_GRANTED, false);
            refreshAddedContactsView();
            setRecyclersVisibility();
            setTitleAB();
            if (totalContacts.size() > 0) {
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
        } else {
            queryIfHasReadContactsPermissions();
        }

        MegaRequestListenerInterface getContactLinkCallback = new MegaRequestListenerInterface() {
            @Override
            public void onRequestStart(MegaApiJava api, MegaRequest request) {

            }

            @Override
            public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

            }

            @Override
            public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
                if (request.getType() == MegaRequest.TYPE_CONTACT_LINK_CREATE && e.getErrorCode() == MegaError.API_OK) {
                    contactLink = CONTACT_LINK_BASE_URL + MegaApiAndroid.handleToBase64(request.getNodeHandle());
                } else {
                    logWarning("Create contact link failed.");
                    contactLink = "";
                }
            }

            @Override
            public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
                logWarning("Create contact link temp error.");
                contactLink = "";
            }
        };
        megaApi.contactLinkCreate(false, getContactLinkCallback);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        logDebug("onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_invite_contact, menu);

        MenuItem scanQrMenuItem = menu.findItem(R.id.action_more);
        scanQrMenuItem.setIcon(Util.mutateIcon(this, R.drawable.ic_more, R.color.black));
        scanQrMenuItem.setVisible(true);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                onBackPressed();
                break;
            }
            case R.id.action_more: {
                logInfo("more button clicked - share invitation through other app");
                String message = getResources().getString(R.string.invite_contacts_to_start_chat_text_message, contactLink);
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, message);
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.invite_contact_chooser_title)));
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public synchronized void onFinish(List<MegaContactGetter.MegaContact> contacts) {
        isGettingMegaContact = false;
        isGetContactCompleted = true;
        clearLists();
        megaContacts.addAll(megaContactToContactInfo(contacts));
        fillUpLists();
        onGetContactCompleted();
    }

    @Override
    public void onException(int errorCode, String requestString) {
        isGettingMegaContact = false;
        isGetContactCompleted = true;
        onGetContactCompleted();
    }

    @Override
    public void noContacts() {
        isGettingMegaContact = false;
        isGetContactCompleted = true;
        onGetContactCompleted();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        logDebug("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(KEY_PHONE_CONTACTS, phoneContacts);
        outState.putParcelableArrayList(KEY_MEGA_CONTACTS, megaContacts);
        outState.putParcelableArrayList(KEY_ADDED_CONTACTS, addedContacts);
        outState.putParcelableArrayList(KEY_FILTERED_CONTACTS, filteredContacts);
        outState.putParcelableArrayList(KEY_TOTAL_CONTACTS, totalContacts);
        outState.putBoolean(KEY_IS_PERMISSION_GRANTED, isPermissionGranted);
        outState.putBoolean(KEY_IS_GET_CONTACT_COMPLETED, isGetContactCompleted);
    }

    @Override
    public void onBackPressed() {
        logDebug("onBackPressed");
        finish();
    }

    private void setTitleAB() {
        logDebug("setTitleAB");
        if (aB != null) {
            if (addedContacts.size() > 0) {
                aB.setSubtitle(getResources().getQuantityString(R.plurals.num_contacts_selected, addedContacts.size(), addedContacts.size()));
            } else {
                aB.setSubtitle(null);
            }
        }
    }

    private void setRecyclersVisibility() {
        logDebug("setRecyclersVisibility " + totalContacts.size());
        if (totalContacts.size() > 0) {
            containerContacts.setVisibility(View.VISIBLE);
        } else {
            containerContacts.setVisibility(View.GONE);
        }
    }

    private void queryIfHasReadContactsPermissions() {
        logDebug("queryIfHasReadContactsPermissions");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasReadContactsPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);
            if (hasReadContactsPermission) {
                isPermissionGranted = true;
                prepareToGetContacts();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, Constants.REQUEST_READ_CONTACTS);
            }
        } else {
            isPermissionGranted = true;
            prepareToGetContacts();
        }
    }

    private void prepareToGetContacts() {
        logDebug("prepareToGetContacts");
        setEmptyStateVisibility(true);
        progressBar.setVisibility(View.VISIBLE);
        new GetPhoneContactsTask().execute();
    }

    private void visibilityFastScroller() {
        logDebug("visibilityFastScroller");
        fastScroller.setRecyclerView(recyclerViewList);
        if (totalContacts.size() < MIN_LIST_SIZE_FOR_FAST_SCROLLER) {
            fastScroller.setVisibility(View.GONE);
        } else {
            fastScroller.setVisibility(View.VISIBLE);
        }
    }

    private void setPhoneAdapterContacts(ArrayList<InvitationContactInfo> contacts) {
        logDebug("setPhoneAdapterContacts");
        invitationContactsAdapter.setContactData(contacts);
        invitationContactsAdapter.notifyDataSetChanged();
    }

    private void showEmptyTextView() {
        logDebug("showEmptyTextView");
        String textToShow = getString(R.string.context_empty_contacts).toUpperCase();
        try {
            textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
            textToShow = textToShow.replace("[/A]", "</font>");
            textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
            textToShow = textToShow.replace("[/B]", "</font>");
        } catch (Exception e) {
            logError(e.toString());
        }

        Spanned result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(textToShow);
        }
        emptyTextView.setText(result);
    }

    private void setEmptyStateVisibility(boolean visible) {
        logDebug("setEmptyStateVisibility");
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

    private void initScanQR() {
        logDebug("initScanQR");
        Intent intent = new Intent(this, QRCodeActivity.class);
        intent.putExtra(INVITE_CONTACT_SCAN_QR, true);
        startActivityForResult(intent, SCAN_QR_FOR_INVITE_CONTACTS);
    }

    private void refreshKeyboard() {
        logDebug("refreshKeyboard");
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
        logDebug("isValidEmail" + result);
        return result;
    }

    private boolean isValidPhone(CharSequence target) {
        boolean result = target != null && target.length() > PHONE_NUMBER_MIN_LENGTH;
        logDebug("isValidPhone" + result);
        return result;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        refreshKeyboard();
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        logDebug("onTextChanged: s is " + s.toString() + "start: " + start + " before: " + before + " count: " + count);
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
                    Util.hideKeyboard(this, 0);
                }
            } else {
                logDebug("Last character is: " + last);
            }
        }

        if (filterContactsTask != null && filterContactsTask.getStatus() == AsyncTask.Status.RUNNING) {
            filterContactsTask.cancel(true);
        }
        refreshInviteContactButton();
        startFilterTask();
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
            logDebug("s: " + s);
            if (TextUtils.isEmpty(s)) {
                Util.hideKeyboard(this, 0);
            } else {
                String result = checkInputEmail(s);
                if (result != null) {
                    typeContactEditText.getText().clear();
                    Util.hideKeyboard(this, 0);
                    showSnackbar(result);
                    return true;
                }

                boolean isEmailValid = isValidEmail(processedStrong);
                boolean isPhoneValid = isValidPhone(processedStrong);
                if (isEmailValid) {
                    addContactInfo(processedStrong, TYPE_MANUAL_INPUT_EMAIL);
                } else if (isPhoneValid) {
                    addContactInfo(processedStrong, TYPE_MANUAL_INPUT_PHONE);
                }
                if (isEmailValid || isPhoneValid) {
                    typeContactEditText.getText().clear();
                    Util.hideKeyboard(this, 0);
                }
                if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    Util.hideKeyboard(this, 0);
                }
                if (filterContactsTask != null && filterContactsTask.getStatus() == AsyncTask.Status.RUNNING) {
                    filterContactsTask.cancel(true);
                }
                startFilterTask();
                Util.hideKeyboard(this, 0);
            }
            refreshInviteContactButton();
            return true;
        }
        if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_SEND)) {
            if (addedContacts.isEmpty()) {
                Util.hideKeyboard(this, 0);
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
        switch (v.getId()) {
            case R.id.layout_scan_qr: {
                logDebug("Scan QR code pressed");
                initScanQR();
                break;
            }
            case R.id.fab_button_next: {
                enableFabButton(false);
                logDebug("invite Contacts");
                inviteContacts(addedContacts);
                Util.hideKeyboard(this, 0);
                break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        logDebug("onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constants.REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    logDebug("Permission granted");
                    isPermissionGranted = true;
                    prepareToGetContacts();
                    noPermissionHeader.setVisibility(View.GONE);
                } else {
                    logDebug("Permission denied");
                    setEmptyStateVisibility(true);
                    emptyTextView.setText(R.string.no_contacts_permissions);
                    emptyImageView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    noPermissionHeader.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void onItemClick(int position) {
        InvitationContactInfo invitationContactInfo = invitationContactsAdapter.getItem(position);
        logDebug("on Item click at " + position + " name is " + invitationContactInfo.getName());
        if (isContactAdded(invitationContactInfo)) {
            addedContacts.remove(invitationContactInfo);
        } else {
            addedContacts.add(invitationContactInfo);
        }

        refreshAddedContactsView();
        refreshInviteContactButton();
        //clear input text view after selection
        typeContactEditText.setText("");
        setTitleAB();
    }

    private void refreshHorizontalScrollView() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(android.view.View.FOCUS_RIGHT);
            }
        }, 100);
    }

    private ArrayList<InvitationContactInfo> megaContactToContactInfo(List<MegaContactGetter.MegaContact> megaContacts) {
        logDebug("megaContactToContactInfo " + megaContacts.size());
        ArrayList<InvitationContactInfo> result = new ArrayList<>();
        if (megaContacts.size() > 0) {
            InvitationContactInfo megaContactHeader = new InvitationContactInfo(ID_MEGA_CONTACTS_HEADER, getString(R.string.contacts_mega), TYPE_MEGA_CONTACT_HEADER);
            result.add(megaContactHeader);
        } else {
            return result;
        }

        for (MegaContactGetter.MegaContact contact : megaContacts) {
            long id = contact.getHandle();
            String name = contact.getLocalName();
            String email = contact.getEmail();
            String handle = contact.getId();
            String color = megaApi.getUserAvatarColor(handle);
            InvitationContactInfo info = new InvitationContactInfo(id, name, TYPE_MEGA_CONTACT, email, color);
            info.setHandle(handle);
            info.setNormalizedNumber(contact.getNormalizedPhoneNumber());
            result.add(info);
        }

        return result;
    }

    private ArrayList<InvitationContactInfo> localContactToContactInfo(List<ContactsUtil.LocalContact> localContacts) {
        logDebug("megaContactToContactInfo " + localContacts.size());
        ArrayList<InvitationContactInfo> result = new ArrayList<>();
        if (localContacts.size() > 0) {
            InvitationContactInfo megaContactHeader = new InvitationContactInfo(ID_PHONE_CONTACTS_HEADER, getString(R.string.contacts_phone), TYPE_MEGA_CONTACT_HEADER);
            result.add(megaContactHeader);
        } else {
            return result;
        }

        for (ContactsUtil.LocalContact contact : localContacts) {
            long id = contact.getId();
            String name = contact.getName();
            List<String> phoneNumberList = contact.getPhoneNumberList();
            List<String> emailList = contact.getEmailList();

            //flatten contacts that have multiple phone numbers/emails
            for (String phoneNumber : phoneNumberList) {
                InvitationContactInfo info = new InvitationContactInfo(id, name, TYPE_PHONE_CONTACT, phoneNumber, defaultLocalContactAvatarColor);
                info.setNormalizedNumber(Util.normalizePhoneNumberByNetwork(this, phoneNumber));
                result.add(info);
            }

            for (String email : emailList) {
                InvitationContactInfo info = new InvitationContactInfo(id, name, TYPE_PHONE_CONTACT, email, defaultLocalContactAvatarColor);
                result.add(info);
            }
        }

        return result;
    }

    private void getMegaContact() {
        //clear cache
        isGettingMegaContact = true;
        megaContactGetter.getMegaContacts(megaApi, MegaContactGetter.DAY);
    }

    private void onGetContactCompleted() {
        if (isGettingLocalContact || isGettingMegaContact) {
            return;
        }
        progressBar.setVisibility(View.GONE);
        refreshList();
        setRecyclersVisibility();
        visibilityFastScroller();

        if (totalContacts.size() > 0) {
            setEmptyStateVisibility(false);
        } else {
            showEmptyTextView();
        }
    }

    private class GetPhoneContactsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            logDebug("GetPhoneContactsTask doInBackground");

            isGettingLocalContact = true;

            //add new value
            rawLocalContacts = megaContactGetter.getLocalContacts();
            filteredContacts.addAll(megaContacts);
            phoneContacts.addAll(localContactToContactInfo(rawLocalContacts));
            ContactsFilter.filterOutContacts(megaApi, phoneContacts);
            ContactsFilter.filterOutPendingContacts(megaApi, phoneContacts);
            ContactsFilter.filterOutMegaUsers(megaContactToContactInfo(dbH.getMegaContacts()), phoneContacts);
            ContactsFilter.filterOutMyself(megaApi, phoneContacts);
            filteredContacts.addAll(phoneContacts);

            //keep all contacts for records
            totalContacts.addAll(filteredContacts);
            return null;
        }

        @Override
        protected void onPostExecute(Void avoid) {
            logDebug("onPostExecute GetPhoneContactsTask");
            isGettingLocalContact = false;
            onGetContactCompleted();
            getMegaContact();
        }
    }

    private class FilterContactsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            logDebug("FilterContactsTask doInBackground");
            String query = inputString == null ? null : inputString.toLowerCase();
            ArrayList<InvitationContactInfo> megaContacts = new ArrayList<>();
            ArrayList<InvitationContactInfo> phoneContacts = new ArrayList<>();

            if (query != null && !query.equals("")) {
                for (int i = 0; i < totalContacts.size(); i++) {
                    InvitationContactInfo invitationContactInfo = totalContacts.get(i);
                    int type = invitationContactInfo.getType();
                    String name = invitationContactInfo.getName().toLowerCase();
                    String nameWithoutSpace = name.replaceAll("\\s", "");
                    String displayLabel = invitationContactInfo.getDisplayInfo().toLowerCase().replaceAll("\\s", "");

                    if (name.contains(query) || displayLabel.contains(query) || nameWithoutSpace.contains(query)) {
                        if (type == TYPE_PHONE_CONTACT) {
                            phoneContacts.add(invitationContactInfo);
                        } else if (type == TYPE_MEGA_CONTACT) {
                            megaContacts.add(invitationContactInfo);
                        }
                    }
                }
                filteredContacts.clear();

                //add header
                if (megaContacts.size() > 0) {
                    filteredContacts.add(new InvitationContactInfo(ID_MEGA_CONTACTS_HEADER, getString(R.string.contacts_mega), TYPE_MEGA_CONTACT_HEADER));
                    filteredContacts.addAll(megaContacts);
                }
                if (phoneContacts.size() > 0) {
                    filteredContacts.add(new InvitationContactInfo(ID_PHONE_CONTACTS_HEADER, getString(R.string.contacts_phone), TYPE_MEGA_CONTACT_HEADER));
                    filteredContacts.addAll(phoneContacts);
                }
            } else {
                filteredContacts.clear();
                filteredContacts.addAll(totalContacts);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void voids) {
            logDebug("onPostExecute FilterContactsTask");
            refreshList();
            visibilityFastScroller();
        }
    }

    private void startFilterTask() {
        logDebug("startFilterTask");
        inputString = typeContactEditText.getText().toString();
        filterContactsTask = new FilterContactsTask();
        filterContactsTask.execute();
    }

    private View createContactTextView(String name, final int id) {
        logDebug("createTextView contact name is " + name);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        params.setMargins(Util.px2dp(ADDED_CONTACT_VIEW_MARGIN_LEFT, outMetrics), 0, 0, 0);
        View rowView = inflater.inflate(R.layout.selected_contact_item, null, false);
        rowView.setLayoutParams(params);
        rowView.setId(id);
        rowView.setClickable(true);
        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InvitationContactInfo invitationContactInfo = addedContacts.get(v.getId());
                invitationContactInfo.setHighlighted(false);
                addedContacts.remove(id);
                refreshAddedContactsView();
                refreshInviteContactButton();
                refreshList();
                setTitleAB();
            }
        });

        TextView displayName = rowView.findViewById(R.id.contact_name);
        displayName.setText(name);
        return rowView;
    }

    private void refreshAddedContactsView() {
        logDebug("refreshAddedContactsView");
        itemContainer.removeAllViews();
        for (int i = 0; i < addedContacts.size(); i++) {
            InvitationContactInfo invitationContactInfo = addedContacts.get(i);
            String name = invitationContactInfo.getName();
            String displayedLabel;
            if (TextUtils.isEmpty(name)) {
                displayedLabel = invitationContactInfo.getDisplayInfo();
            } else {
                displayedLabel = name;
            }
            itemContainer.addView(createContactTextView(displayedLabel, i));
        }
        itemContainer.invalidate();
        refreshHorizontalScrollView();
    }

    private void refreshInviteContactButton() {
        logDebug("refreshInviteContactButton");
        String stringInEditText = typeContactEditText.getText().toString();
        boolean isStringValidNow = stringInEditText.length() == 0
                || isValidEmail(stringInEditText)
                || isValidPhone(stringInEditText);
        enableFabButton(addedContacts.size() > 0 && isStringValidNow);
    }

    private boolean isContactAdded(InvitationContactInfo invitationContactInfo) {
        logDebug("isContactAdded contact name is " + invitationContactInfo.getName());
        for (InvitationContactInfo addedContact : addedContacts) {
            if (addedContact.getId() == invitationContactInfo.getId() &&
                    addedContact.getDisplayInfo().equalsIgnoreCase(invitationContactInfo.getDisplayInfo())) {
                return true;
            }
        }
        return false;
    }

    private void refreshList() {
        logDebug("refresh list");
        setPhoneAdapterContacts(filteredContacts);
    }

    private void inviteContacts(ArrayList<InvitationContactInfo> addedContacts) {
        logDebug("inviteContacts");

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

        if (contactsEmailsSelected.size() > 0) {
            //phone contact will be invited once email done
            inviteEmailContacts(contactsEmailsSelected);
        } else if (contactsPhoneSelected.size() > 0) {
            invitePhoneContacts(contactsPhoneSelected);
            finish();
        } else {
            finish();
        }
    }

    private void invitePhoneContacts(ArrayList<String> phoneNumbers) {
        logDebug("invitePhoneContacts");
        StringBuilder recipents = new StringBuilder("smsto:");
        for (String phone : phoneNumbers) {
            recipents.append(phone);
            recipents.append(";");
            logDebug("setResultPhoneContacts: " + phone);
        }
        String smsBody = getResources().getString(R.string.invite_contacts_to_start_chat_text_message, contactLink);
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse(recipents.toString()));
        smsIntent.putExtra("sms_body", smsBody);
        startActivity(smsIntent);
    }

    private int numberToSend;
    private int numberSent;
    private int numberNotSent;

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT) {
            logDebug("MegaRequest.TYPE_INVITE_CONTACT finished: " + request.getNumber());
            if (e.getErrorCode() == MegaError.API_OK) {
                numberSent++;
                logDebug("OK INVITE CONTACT: " + request.getEmail());
            } else {
                numberNotSent++;
                logDebug("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
            }
            if (numberSent + numberNotSent == numberToSend) {
                if (numberToSend == 1) {
                    if (numberSent == 1) {
                        showSnackbar(getString(R.string.context_contact_request_sent, request.getEmail()));
                    }
                } else {
                    if (numberNotSent > 0) {
                        showSnackbar(getString(R.string.number_no_invite_contact_request, numberSent, numberNotSent));
                    } else {
                        showSnackbar(getString(R.string.number_correctly_invite_contact_request, numberToSend));
                    }
                }
                numberSent = 0;
                numberToSend = 0;
                numberNotSent = 0;
                Util.hideKeyboard(InviteContactActivity.this, 0);
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        if (contactsPhoneSelected.size() > 0) {
                            invitePhoneContacts(contactsPhoneSelected);
                        }
                        setResult(Activity.RESULT_OK);
                        finish();
                    }
                }, 2000);
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    private void inviteEmailContacts(ArrayList<String> emails) {
        numberToSend = emails.size();
        for (String email : emails) {
            logDebug("setResultEmailContacts: " + email);
            megaApi.inviteContact(email, null, MegaContactRequest.INVITE_ACTION_ADD, this);
        }
    }

    public void showSnackbar(String message) {
        showSnackbar(Constants.SNACKBAR_TYPE, scrollView, message, -1);
    }

    private void addContactInfo(String inputString, int type) {
        logDebug("addContactInfo inputString is " + inputString + " type is " + type);
        InvitationContactInfo info = null;
        if (type == TYPE_MANUAL_INPUT_EMAIL) {
            info = new InvitationContactInfo(inputString.hashCode(), "", TYPE_MANUAL_INPUT_EMAIL, inputString, defaultLocalContactAvatarColor);
        } else if (type == TYPE_MANUAL_INPUT_PHONE) {
            info = new InvitationContactInfo(inputString.hashCode(), "", TYPE_MANUAL_INPUT_PHONE, inputString, defaultLocalContactAvatarColor);
        }
        if (info != null) {
            int index = isUserEnteredContactExistInList(info);
            if (index >= 0) {
                recyclerViewList.findViewHolderForAdapterPosition(index).itemView.performClick();
            } else if (!isContactAdded(info)) {
                addedContacts.add(info);
                refreshAddedContactsView();
            }
        }
        setTitleAB();
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
        logDebug("enableFabButton: " + enableFabButton);
        int lintColor = enableFabButton ? R.color.accentColor : R.color.disable_fab_invite_contact;
        fabButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(lintColor)));
        fabButton.setEnabled(enableFabButton);
    }

    private void clearLists() {
        totalContacts.clear();
        filteredContacts.clear();
        megaContacts.clear();
    }

    private void fillUpLists() {
        filteredContacts.addAll(megaContacts);
        filteredContacts.addAll(phoneContacts);
        totalContacts.addAll(filteredContacts);
    }
}
