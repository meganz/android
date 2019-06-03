package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
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
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.lollipop.adapters.ContactsAdapter;
import mega.privacy.android.app.lollipop.qrcode.QRCodeActivity;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.lollipop.ContactInfo.TYPE_MANUAL_INPUT_EMAIL;
import static mega.privacy.android.app.lollipop.ContactInfo.TYPE_MANUAL_INPUT_PHONE;
import static mega.privacy.android.app.lollipop.ContactInfo.TYPE_MEGA_CONTACT;
import static mega.privacy.android.app.lollipop.ContactInfo.TYPE_MEGA_CONTACT_HEADER;
import static mega.privacy.android.app.lollipop.ContactInfo.TYPE_PHONE_CONTACT;
import static mega.privacy.android.app.lollipop.ContactInfo.TYPE_PHONE_CONTACT_HEADER;


public class InviteContactActivityLollipop extends PinActivityLollipop implements ContactsAdapter.OnItemClickListener, View.OnClickListener, RecyclerView.OnItemTouchListener, TextWatcher, TextView.OnEditorActionListener, MegaRequestListenerInterface {

    public static final int SCAN_QR_FOR_invite_contactS = 1111;
    public static final String INVITE_CONTACT_SCAN_QR = "inviteContacts";
    private final String KEY_PHONE_CONTACTS = "KEY_PHONE_CONTACTS";
    private final String KEY_MEGA_CONTACTS = "KEY_MEGA_CONTACTS";
    private final String KEY_ADDED_CONTACTS = "KEY_ADDED_CONTACTS";
    private final String KEY_FILTERED_CONTACTS = "KEY_FILTERED_CONTACTS";
    private final String KEY_TOTAL_CONTACTS = "KEY_TOTAL_CONTACTS";
    private final int ID_MEGA_CONTACTS_HEADER = -2;
    private final int ID_PHONE_CONTACTS_HEADER = -1;

    private DisplayMetrics outMetrics;
    private MegaApplication app;
    private MegaApiAndroid megaApi;
    private DatabaseHandler dbH;
    private ActionBar aB;
    private RelativeLayout containerContacts, container;
    private RecyclerView recyclerViewList;
    private ImageView emptyImageView;
    private TextView emptyTextView, emptySubTextView;
    private ProgressBar progressBar;
    private String inputString;
    private ContactsAdapter contactsAdapter;
    private ArrayList<ContactInfo> phoneContacts, megaContacts, addedContacts, filteredContacts, totalContacts;
    private FilterContactsTask filterContactsTask;
    private FastScroller fastScroller;
    private FloatingActionButton fabButton;
    private EditText typeContactEditText;
    private HorizontalScrollView scrollView;
    private LinearLayout itemContainer;
    private Handler handler;
    private LayoutInflater inflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");

        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dark_primary_color));
        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        app = (MegaApplication) getApplication();
        megaApi = app.getMegaApi();
        dbH = DatabaseHandler.getDbHandler(this);
        handler = new Handler();
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setContentView(R.layout.activity_invite_contact);

        phoneContacts = new ArrayList<>();
        addedContacts = new ArrayList<>();
        filteredContacts = new ArrayList<>();
        totalContacts = new ArrayList<>();
        megaContacts = megaContactToPhoneContact(megaApi.getContacts());

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
        container = findViewById(R.id.relative_container_invite_contact);
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

        RelativeLayout moreButton = findViewById(R.id.layout_more);
        moreButton.setOnClickListener(this);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerViewList = findViewById(R.id.invite_contact_list);
        recyclerViewList.setClipToPadding(false);
        recyclerViewList.setHasFixedSize(true);
        recyclerViewList.addOnItemTouchListener(this);
        recyclerViewList.setItemAnimator(new DefaultItemAnimator());
        recyclerViewList.setLayoutManager(linearLayoutManager);
        recyclerViewList.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
        contactsAdapter = new ContactsAdapter(this, filteredContacts, this);
        recyclerViewList.setAdapter(contactsAdapter);
        containerContacts = findViewById(R.id.container_list_contacts);

        fastScroller = findViewById(R.id.fastScroller);
        fastScroller.setRecyclerView(recyclerViewList);

        emptyImageView = findViewById(R.id.invite_contact_list_empty_image);
        emptyTextView = findViewById(R.id.invite_contact_list_empty_text);
        emptyTextView.setText(R.string.contacts_list_empty_text_loading_share);
        emptySubTextView = findViewById(R.id.invite_contact_list_empty_subtext);

        progressBar = findViewById(R.id.add_contact_progress_bar);
        progressBar = findViewById(R.id.invite_contact_progress_bar);
        refreshInviteContactButton();

        //orientation changes
        if (savedInstanceState != null) {
            phoneContacts = savedInstanceState.getParcelableArrayList(KEY_PHONE_CONTACTS);
            megaContacts = savedInstanceState.getParcelableArrayList(KEY_MEGA_CONTACTS);
            addedContacts = savedInstanceState.getParcelableArrayList(KEY_ADDED_CONTACTS);
            filteredContacts = savedInstanceState.getParcelableArrayList(KEY_FILTERED_CONTACTS);
            totalContacts = savedInstanceState.getParcelableArrayList(KEY_TOTAL_CONTACTS);
            refreshAddedContactsView();
            setRecyclersVisibility();
            setTitleAB();
            if(totalContacts.size() > 0){
                setEmptyStateVisibility(false);
            }else{
                setEmptyStateVisibility(true);
                showEmptyTextView();
            }
        } else {
            queryIfHasReadContactsPermissions();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected");
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                onBackPressed();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        log("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(KEY_PHONE_CONTACTS, phoneContacts);
        outState.putParcelableArrayList(KEY_MEGA_CONTACTS, megaContacts);
        outState.putParcelableArrayList(KEY_ADDED_CONTACTS, addedContacts);
        outState.putParcelableArrayList(KEY_FILTERED_CONTACTS, filteredContacts);
        outState.putParcelableArrayList(KEY_TOTAL_CONTACTS, totalContacts);
    }

    @Override
    public void onBackPressed() {
        log("onBackPressed");
        finish();
    }

    private void setTitleAB() {
        log("setTitleAB");
        if (aB != null) {
            if (addedContacts.size() > 0) {
                aB.setSubtitle(getResources().getQuantityString(R.plurals.num_contacts_selected, addedContacts.size(), addedContacts.size()));
            } else {
                aB.setSubtitle(null);
            }
        }
    }

    private void setRecyclersVisibility() {
        log("setRecyclersVisibility " + totalContacts.size());
        if (totalContacts.size() > 0) {
            containerContacts.setVisibility(View.VISIBLE);
        } else {
            containerContacts.setVisibility(View.GONE);
        }
    }

    private void queryIfHasReadContactsPermissions() {
        log("queryIfHasReadContactsPermissions");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasReadContactsPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);
            if (hasReadContactsPermission) {
                prepareToGetPhoneContacts();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, Constants.REQUEST_READ_CONTACTS);
            }
        } else {
            prepareToGetPhoneContacts();
        }
    }

    private void prepareToGetPhoneContacts() {
        log("prepareToGetPhoneContacts");
        setEmptyStateVisibility(true);
        progressBar.setVisibility(View.VISIBLE);
        new GetContactsTask().execute();
    }

    private void visibilityFastScroller() {
        log("visibilityFastScroller");
        fastScroller.setRecyclerView(recyclerViewList);
        if (totalContacts.size() < 20) {
            fastScroller.setVisibility(View.GONE);
        } else {
            fastScroller.setVisibility(View.VISIBLE);
        }
    }

    private void setPhoneAdapterContacts(ArrayList<ContactInfo> contacts) {
        log("setPhoneAdapterContacts");
        contactsAdapter.setContactData(contacts);
        contactsAdapter.notifyDataSetChanged();
    }

    private void showEmptyTextView() {
        log("showEmptyTextView");
        String textToShow = String.format(getString(R.string.context_empty_contacts)).toUpperCase();
        try {
            textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
            textToShow = textToShow.replace("[/A]", "</font>");
            textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
            textToShow = textToShow.replace("[/B]", "</font>");
        } catch (Exception e) {
            log(e.toString());
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
        log("setEmptyStateVisibility");
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

    private ArrayList<ContactInfo> getPhoneContacts() {
        log("getPhoneContacts");
        ArrayList<ContactInfo> contactList = new ArrayList<>();
        ContactInfo phoneContactHeader = new ContactInfo(ID_PHONE_CONTACTS_HEADER, getString(R.string.contacts_phone), "", "", TYPE_PHONE_CONTACT_HEADER);
        contactList.add(phoneContactHeader);
        log("inputString empty");
        String filter = ContactsContract.CommonDataKinds.Email.DATA + " NOT LIKE ''  AND " + ContactsContract.Contacts.IN_VISIBLE_GROUP + "=1";

        try {
            ContentResolver cr = getBaseContext().getContentResolver();
            String SORT_ORDER = ContactsContract.Contacts.SORT_KEY_PRIMARY;

            Cursor c = cr.query(
                    ContactsContract.Data.CONTENT_URI,
                    null, filter,
                    null, SORT_ORDER);

            while (c.moveToNext()) {
                long id = c.getLong(c.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                String name = c.getString(c.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                String emailAddress = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                log("ID: " + id + "___ NAME: " + name + "____ EMAIL: " + emailAddress);

                if ((!emailAddress.equalsIgnoreCase("")) && (emailAddress.contains("@")) && (!emailAddress.contains("s.whatsapp.net"))) {
                    if (inputString == "" || inputString == null) {
                        log("VALID Contact: " + name + " ---> " + emailAddress);
                        ContactInfo contactPhone = new ContactInfo(id, name, emailAddress, null, TYPE_PHONE_CONTACT);
                        contactList.add(contactPhone);
                    } else if (!inputString.isEmpty() && (name.toUpperCase().contains(inputString.toUpperCase()) || emailAddress.toUpperCase().contains(inputString.toUpperCase()))) {
                        log("VALID Contact: " + name + " ---> " + emailAddress + " inputString: " + inputString);
                        ContactInfo contactPhone = new ContactInfo(id, name, emailAddress, null, TYPE_PHONE_CONTACT);
                        contactList.add(contactPhone);
                    }
                }
            }
            c.close();

            log("contactList.size() = " + contactList.size());

        } catch (Exception e) {
            log("Exception: " + e.getMessage());
        }

        return contactList;
    }

    private void initScanQR() {
        log("initScanQR");
        Intent intent = new Intent(this, QRCodeActivity.class);
        intent.putExtra(INVITE_CONTACT_SCAN_QR, true);
        startActivityForResult(intent, SCAN_QR_FOR_invite_contactS);
    }

    private void refreshKeyboard() {
        log("refreshKeyboard");
        int imeOptions = typeContactEditText.getImeOptions();
        typeContactEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);

        int imeOptionsNew = typeContactEditText.getImeOptions();
        if (imeOptions != imeOptionsNew) {
            View view = getCurrentFocus();
            if (view != null) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.restartInput(view);
            }
        }
    }

    private boolean isValidEmail(CharSequence target) {
        boolean result = target != null && Constants.EMAIL_ADDRESS.matcher(target).matches();
        log("isValidEmail" + result);
        return result;
    }

    private boolean isValidPhone(CharSequence target) {
        boolean result = target != null && Constants.PHONE_NUMBER.matcher(target).matches();
        log("isValidPhone" + result);
        return result;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        refreshKeyboard();
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        log("onTextChanged: " + s.toString() + "_ " + start + "__" + before + "__" + count);
        if (s != null) {
            if (s.length() > 0) {
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
                    refreshInviteContactButton();
                    if (isEmailValid || isPhoneValid) {
                        typeContactEditText.getText().clear();
                    }
                    if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        Util.hideKeyboard(this, 0);
                    }
                } else {
                    log("Last character is: " + last);
                }
            }
        }

        if (filterContactsTask != null && filterContactsTask.getStatus() == AsyncTask.Status.RUNNING) {
            filterContactsTask.cancel(true);
        }
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
            log("s: " + s);
            if (s.isEmpty() || s.equals("null") || s.equals("")) {
                Util.hideKeyboard(this, 0);
            } else {
                boolean isEmailValid = isValidEmail(processedStrong);
                boolean isPhoneValid = isValidPhone(processedStrong);
                if (isEmailValid) {
                    addContactInfo(processedStrong, TYPE_MANUAL_INPUT_EMAIL);
                } else if (isPhoneValid) {
                    addContactInfo(processedStrong, TYPE_MANUAL_INPUT_PHONE);
                }
                refreshInviteContactButton();
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
            return true;
        }
        if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_SEND)) {
            if (addedContacts.isEmpty() || addedContacts == null) {
                Util.hideKeyboard(this, 0);
            } else {
                inviteContacts(addedContacts);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layout_scan_qr: {
                log("Scan QR code pressed");
                initScanQR();
                break;
            }
            case R.id.fab_button_next: {
                log("invite Contacts");
                inviteContacts(addedContacts);
                Util.hideKeyboard(this, 0);
                break;
            }
            case R.id.layout_more: {
                log("more button clicked - share invitation through other app");
                String message = "This is my text to send.";
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, message);//todo update the message
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
                break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        log("onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constants.REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    log("Permission granted");
                    prepareToGetPhoneContacts();
                } else {
                    log("Permission denied");
                    setEmptyStateVisibility(true);
                    emptyTextView.setText(R.string.no_contacts_permissions);
                    progressBar.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        log("onRequestStart: " + request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT) {
            log("MegaRequest.TYPE_INVITE_CONTACT finished: " + request.getNumber());

            if (request.getNumber() == MegaContactRequest.INVITE_ACTION_REMIND) {
                showSnackBar(getString(R.string.context_contact_invitation_resent));
            } else {
                if (e.getErrorCode() == MegaError.API_OK) {
                    log("OK INVITE CONTACT: " + request.getEmail());
                    if (request.getNumber() == MegaContactRequest.INVITE_ACTION_ADD) {
                        showSnackBar(getString(R.string.context_contact_request_sent, request.getEmail()));
                    } else if (request.getNumber() == MegaContactRequest.INVITE_ACTION_DELETE) {
                        showSnackBar(getString(R.string.context_contact_invitation_deleted));
                    }
                } else {
                    log("Code: " + e.getErrorString());
                    if (e.getErrorCode() == MegaError.API_EEXIST) {
                        showSnackBar(getString(R.string.context_contact_already_exists, request.getEmail()));
                    } else if (request.getNumber() == MegaContactRequest.INVITE_ACTION_ADD && e.getErrorCode() == MegaError.API_EARGS) {
                        showSnackBar(getString(R.string.error_own_email_as_contact));
                    } else {
                        showSnackBar(getString(R.string.general_error));
                    }
                    log("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    @Override
    public void onItemClick(View view, int position) {
        ContactInfo contactInfo = contactsAdapter.getItem(position);
        log("on Item click at " + position + " name is " + contactInfo.getName());
        if (isContactAdded(contactInfo)) {
            addedContacts.remove(contactInfo);
        } else {
            addedContacts.add(contactInfo);
        }

        refreshAddedContactsView();
        refreshInviteContactButton();
        //refresh scroll view
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(android.view.View.FOCUS_RIGHT);
            }
        }, 100);

        setTitleAB();
    }

    private ArrayList<ContactInfo> megaContactToPhoneContact(ArrayList<MegaUser> megaContacts) {
        log("megaContactToPhoneContact " + megaContacts.size());
        ArrayList<ContactInfo> result = new ArrayList<>();
        if (megaContacts.size() > 0) {
            ContactInfo megaContactHeader = new ContactInfo(ID_MEGA_CONTACTS_HEADER, getString(R.string.contacts_mega), "", "", TYPE_MEGA_CONTACT_HEADER);
            result.add(megaContactHeader);
        } else {
            return result;
        }

        for (MegaUser user : megaContacts) {
            MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(user.getHandle()));
            long id = 0;
            String name = "", email = "", phoneNumber = "";
            if (contactDB != null) {
                id = user.getHandle();
                name = contactDB.getName() + contactDB.getLastName();
                email = user.getEmail();
                phoneNumber = "missing now ";//todo
            }
            ContactInfo info = new ContactInfo(id, name, email, phoneNumber, TYPE_MEGA_CONTACT);
            result.add(info);
        }

        return result;
    }

    private class GetContactsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            log("GetContactsTask doInBackground");

            //clear cache
            phoneContacts.clear();
            filteredContacts.clear();
            totalContacts.clear();

            //add new value
            phoneContacts.addAll(getPhoneContacts());
            filteredContacts.addAll(megaContacts);
            filteredContacts.addAll(phoneContacts);

            //keep all contacts for records
            totalContacts.addAll(filteredContacts);
            return null;
        }

        @Override
        protected void onPostExecute(Void avoid) {
            log("onPostExecute GetContactsTask");
            InviteContactActivityLollipop.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
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
            });
        }
    }

    private class FilterContactsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            log("FilterContactsTask doInBackground");
            String query = inputString == null ? null : inputString.toLowerCase();
            ArrayList<ContactInfo> megaContacts = new ArrayList<>();
            ArrayList<ContactInfo> phoneContacts = new ArrayList<>();

            if (query != null && !query.equals("")) {
                for (int i = 0; i < totalContacts.size(); i++) {
                    ContactInfo contactInfo = totalContacts.get(i);
                    String email = contactInfo.getEmail().toLowerCase();
                    String name = contactInfo.getName().toLowerCase();
                    int type = contactInfo.getType();
                    if ((email.contains(query) || name.contains(query))) {
                        if (type == TYPE_PHONE_CONTACT) {
                            phoneContacts.add(contactInfo);
                        } else if (type == TYPE_MEGA_CONTACT) {
                            megaContacts.add(contactInfo);
                        }
                    }
                }
                filteredContacts.clear();

                //add header
                if (megaContacts.size() > 0) {
                    filteredContacts.add(new ContactInfo(ID_MEGA_CONTACTS_HEADER, getString(R.string.contacts_mega), "", "", TYPE_MEGA_CONTACT_HEADER));
                    filteredContacts.addAll(megaContacts);
                }
                if (phoneContacts.size() > 0) {
                    filteredContacts.add(new ContactInfo(ID_PHONE_CONTACTS_HEADER, getString(R.string.contacts_phone), "", "", TYPE_PHONE_CONTACT_HEADER));
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
            log("onPostExecute FilterContactsTask");
            refreshList();
            visibilityFastScroller();
        }
    }

    private void startFilterTask() {
        log("startFilterTask");
        inputString = typeContactEditText.getText().toString();
        filterContactsTask = new FilterContactsTask();
        filterContactsTask.execute();
    }

    private View createContactTextView(String name, final int id) {
        log("createTextView contact name is " + name);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        final int MARGIN_LEFT = Util.px2dp(10, outMetrics);
        params.setMargins(MARGIN_LEFT, 0, 0, 0);
        View rowView = inflater.inflate(R.layout.selected_contact_item, null, false);
        rowView.setLayoutParams(params);
        rowView.setId(id);
        rowView.setClickable(true);
        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContactInfo contactInfo = addedContacts.get(v.getId());
                contactInfo.setHighlighted(false);
                addedContacts.remove(id);
                refreshAddedContactsView();
                refreshList();
                setTitleAB();
            }
        });

        TextView displayName = rowView.findViewById(R.id.contact_name);
        displayName.setText(name);
        return rowView;
    }

    private void refreshAddedContactsView() {
        log("refreshAddedContactsView");
        itemContainer.removeAllViews();
        for (int i = 0; i < addedContacts.size(); i++) {
            ContactInfo contactInfo = addedContacts.get(i);
            String displayedLabel = "";
            if (contactInfo.getName().equals("")) {
                if (!contactInfo.getEmail().equals("")) {
                    displayedLabel = contactInfo.getEmail();
                } else if (!contactInfo.getPhoneNumber().equals("")) {
                    displayedLabel = contactInfo.getPhoneNumber();
                }
            } else {
                displayedLabel = contactInfo.getName();
            }
            itemContainer.addView(createContactTextView(displayedLabel, i));
        }
        itemContainer.invalidate();
    }

    private void refreshInviteContactButton() {
        log("refreshInviteContactButton");
        String stringInEditText = typeContactEditText.getText().toString();
        Boolean isStringValidNow = stringInEditText.length() == 0
                || isValidEmail(stringInEditText)
                || isValidPhone(stringInEditText);
        enableFabButton(addedContacts.size() > 0 && isStringValidNow);
    }

    private boolean isContactAdded(ContactInfo contactInfo) {
        log("isContactAdded contact name is " + contactInfo.getName());
        for (ContactInfo addedContact : addedContacts) {
            if (addedContact.getId() == contactInfo.getId()) {
                return true;
            }
        }
        return false;
    }

    private void refreshList() {
        log("refresh list");
        setPhoneAdapterContacts(filteredContacts);
    }

    private void inviteContacts(ArrayList<ContactInfo> addedContacts) {
        log("inviteContacts");

        // Email contacts to be invited
        ArrayList<String> contactsEmailsSelected = new ArrayList<>();
        // Phone contacts to be invited
        ArrayList<String> contactsPhoneSelected = new ArrayList<>();

        if (addedContacts != null) {
            String contactEmail;
            String contactPhone;
            for (ContactInfo contact : addedContacts) {
                contactEmail = contact.getEmail();
                contactPhone = contact.getPhoneNumber();
                if (contactEmail != null) {
                    contactsEmailsSelected.add(contactEmail);
                } else if (contactPhone != null) {
                    contactsPhoneSelected.add(contactPhone);
                }
            }
        }

        if (contactsPhoneSelected.size() > 0) {
            invitePhoneContacts(contactsPhoneSelected);
        }

        if (contactsEmailsSelected.size() > 0) {
            inviteEmailContacts(contactsEmailsSelected);
        }
    }

    private void invitePhoneContacts(ArrayList<String> phoneNumbers) {
        log("invitePhoneContacts");
        String recipents = "smsto:";
        for (String phone : phoneNumbers) {
            recipents += (phone + ";");
            log("setResultPhoneContacts: " + phone);
        }
        String smsBody = getResources().getString(R.string.invite_contacts_to_start_chat_text_message);
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse(recipents));
        smsIntent.putExtra("sms_body", smsBody);
        startActivity(smsIntent);
    }

    private void inviteEmailContacts(ArrayList<String> emails) {
        log("invitePhoneContacts");
        Intent intent = new Intent();
        for (String email : emails) {
            log("setResultEmailContacts: " + email);
        }
        setResult(RESULT_OK, intent);
        intent.putStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS, emails);
        Util.hideKeyboard(this, 0);
        finish();
    }

    private void addContactInfo(String inputString, int type) {
        log("addContactInfo inputString is " + inputString + " type is " + type);
        ContactInfo info = null;
        switch (type) {
            case TYPE_MANUAL_INPUT_EMAIL: {
                info = new ContactInfo(inputString.hashCode(), "", inputString, "", TYPE_MANUAL_INPUT_EMAIL);
                break;
            }

            case TYPE_MANUAL_INPUT_PHONE: {
                info = new ContactInfo(inputString.hashCode(), "", "", inputString, TYPE_MANUAL_INPUT_PHONE);
                break;
            }
            default:
                break;
        }
        if (info != null) {
            if (!isContactAdded(info)) {
                addedContacts.add(info);
            }
            refreshAddedContactsView();
        }
    }

    private void enableFabButton(Boolean enableFabButton) {
        log("enableFabButton: " + enableFabButton);
        int lintColor = enableFabButton ? R.color.accentColor : R.color.disable_fab_invite_contact;
        fabButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(lintColor)));
        fabButton.setEnabled(enableFabButton);
    }

    private void showSnackBar(String message) {
        Util.hideKeyboard(this, 0);
        showSnackbar(container, message);
    }

    public static void log(String message) {
        Util.log("InviteContactActivityLollipop", message);
    }
}
