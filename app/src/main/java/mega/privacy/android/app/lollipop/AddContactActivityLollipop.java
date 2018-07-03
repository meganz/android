package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
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
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.flowlayoutmanager.Alignment;
import mega.privacy.android.app.components.flowlayoutmanager.FlowLayoutManager;
import mega.privacy.android.app.components.tokenautocomplete.ContactInfo;
import mega.privacy.android.app.lollipop.adapters.AddContactsLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.MegaAddContactsLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.MegaContactsLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.PhoneContactsLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.ShareContactsAdapter;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaUser;


public class AddContactActivityLollipop extends PinActivityLollipop implements View.OnClickListener, RecyclerView.OnItemTouchListener{

    public static String ACTION_PICK_CONTACT_SHARE_FOLDER = "ACTION_PICK_CONTACT_SHARE_FOLDER";
    public static String ACTION_PICK_CONTACT_SEND_FILE = "ACTION_PICK_CONTACT_SEND_FILE";

    DisplayMetrics outMetrics;
    private android.support.v7.app.AlertDialog shareFolderDialog;
    MegaApplication app;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    DatabaseHandler dbH = null;
    int contactType = 0;
    int multipleSelectIntent;
    int sendToInbox;
    long nodeHandle = -1;
    long[] nodeHandles;
    Handler handler;
    boolean cancelled = false;
    boolean heightMax = false;
    boolean recyclerOn = true;
    long chatId = -1;

    AddContactActivityLollipop addContactActivityLollipop;

    Toolbar tB;
    ActionBar aB;

    RecyclerView recyclerViewMega;
    RecyclerView recyclerViewPhone;
    ImageView emptyImageView;
    TextView emptyTextView;
    ProgressBar progressBar;
    EditText addContactEditText;
    private RelativeLayout contactErrorLayout;
    private RelativeLayout notPermitedAddContacts;
    private Drawable editTextBackground;
    RecyclerView addedContactsRecyclerView;
    LinearLayoutManager mLayoutManager_2;
    FlowLayoutManager mLayoutManager;
    String inputString  ="";

    MegaContactsLollipopAdapter adapterMEGA;
    GestureDetectorCompat detector;

    PhoneContactsLollipopAdapter adapterPhone;
    AddContactsLollipopAdapter adapterContacts;
    MegaAddContactsLollipopAdapter adapterMEGAContacts;

    RelativeLayout recyclerLayoutAddContacts;
    RelativeLayout relativeLayout;
    int screenHeight;
    int recyclerHeight;
    int aBHeight;
    int max;

    ArrayList<PhoneContactInfo> phoneContacts = new ArrayList<>();
    ArrayList<PhoneContactInfo> addedContactsPhone = new ArrayList<>();
    ArrayList<PhoneContactInfo> filteredContactsPhone = new ArrayList<>();

    ArrayList<MegaUser> contactsMEGA;
    ArrayList<MegaContactAdapter> visibleContactsMEGA = new ArrayList<>();
    ArrayList<MegaContactAdapter> filteredContactMEGA = new ArrayList<>();
    ArrayList<MegaContactAdapter> addedContactsMEGA = new ArrayList<>();

    ArrayList<ShareContactInfo> addedContactsShare = new ArrayList<>();
    ShareContactsAdapter adapterShare;

    boolean itemClickPressed = false;

    public static String EXTRA_MEGA_CONTACTS = "mega_contacts";
    public static String EXTRA_CONTACTS = "extra_contacts";
    public static String EXTRA_NODE_HANDLE = "node_handle";

    private MenuItem sendInvitationMenuItem;

    private boolean comesFromChat;

    private RelativeLayout headerContactsMega;
    private RelativeLayout headerContactsPhone;

    private boolean filteredContactsPhoneIsEmpty  = false;

    public class RecyclerViewOnGestureListener extends GestureDetector.SimpleOnGestureListener {

        public void onLongPress(MotionEvent e) {

            super.onLongPress(e);
        }
    }

    private class PhoneContactsTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            cancelled = false;
            phoneContacts = getPhoneContacts();
            if (cancelled){
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            progressBar.setVisibility(View.GONE);
            if(phoneContacts!=null){
                if (phoneContacts.size() == 0){
                    headerContactsPhone.setVisibility(View.GONE);
                    String textToShow = String.format(getString(R.string.context_empty_contacts), getString(R.string.section_contacts));
                    try{
                        textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                    }
                    catch (Exception e){}
                    Spanned result = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(textToShow);
                    }
                    emptyTextView.setText(result);
                }
                else {
                    emptyTextView.setText(R.string.contacts_list_empty_text_loading);
                }
                filteredContactsPhone.clear();
                if (phoneContacts != null) {
                    log("PhoneContactsTask: phoneContacts.size() = " + phoneContacts.size());

                    if (contactType == Constants.CONTACT_TYPE_DEVICE){
                        if (addedContactsPhone.size() == 0) {
                            log("addedContactsPhone == 0");
                            for (int i = 0; i < phoneContacts.size(); i++) {
                                log("filteredContacts.add(visibleContacts.get(" + i + ") = " + phoneContacts.get(i).getEmail());
                                filteredContactsPhone.add(phoneContacts.get(i));
                            }
                        }
                        else {
                            for (int i = 0; i < phoneContacts.size(); i++) {
                                boolean found = false;
                                for (int j = 0; j < addedContactsPhone.size(); j++) {
                                    if (phoneContacts.get(i).getEmail().equals(addedContactsPhone.get(j).getEmail())) {
                                        log("found true");
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    log("!found -> filteredContacts.add(visibleContacts.get(" + i + ") = " + phoneContacts.get(i).getEmail());
                                    filteredContactsPhone.add(phoneContacts.get(i));
                                }
                            }
                        }
                    }
                    else if (contactType == Constants.CONTACT_TYPE_BOTH){
                        if (addedContactsShare.size() == 0) {
                            log("addedContactsPhone == 0");
                            for (int i = 0; i < phoneContacts.size(); i++) {
                                log("filteredContacts.add(visibleContacts.get(" + i + ") = " + phoneContacts.get(i).getEmail());
                                filteredContactsPhone.add(phoneContacts.get(i));
                            }
                        }
                        else {
                            for (int i = 0; i < phoneContacts.size(); i++) {
                                boolean found = false;
                                for (int j = 0; j < addedContactsShare.size(); j++) {
                                    if (addedContactsShare.get(j).getPhoneContactInfo()){
                                        if (phoneContacts.get(i).getEmail().equals(addedContactsShare.get(j).getEmail())) {
                                            log("found true");
                                            found = true;
                                            break;
                                        }
                                    }
                                }
                                if (!found) {
                                    log("!found -> filteredContacts.add(visibleContacts.get(" + i + ") = " + phoneContacts.get(i).getEmail());
                                    filteredContactsPhone.add(phoneContacts.get(i));
                                }
                            }
                        }
                    }

                    if (filteredContactsPhone.size() == 0) {
                        String textToShow = String.format(getString(R.string.context_empty_contacts), getString(R.string.section_contacts));
                        try{
                            textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                            textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                            textToShow = textToShow.replace("[/B]", "</font>");
                        }
                        catch (Exception e){}
                        Spanned result = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                        } else {
                            result = Html.fromHtml(textToShow);
                        }
                        emptyTextView.setText(result);
                    }
                }

                if (adapterPhone == null){
                    adapterPhone = new PhoneContactsLollipopAdapter(addContactActivityLollipop, phoneContacts);

                    recyclerViewPhone.setAdapter(adapterPhone);

                    adapterPhone.SetOnItemClickListener(new PhoneContactsLollipopAdapter.OnItemClickListener() {

                        @Override
                        public void onItemClick(View view, int position) {
                            itemClick(view, position);
                        }
                    });
                }
                else{
                    adapterPhone.setContacts(filteredContactsPhone);
                }

                if(adapterPhone!=null){
                    if (adapterPhone.getItemCount() == 0){
                        filteredContactsPhoneIsEmpty = true;
                        headerContactsPhone.setVisibility(View.GONE);
                        recyclerViewPhone.setVisibility(View.GONE);
                        if (contactType == Constants.CONTACT_TYPE_BOTH) {
                            if (adapterMEGA != null) {
                                if (adapterMEGA.getItemCount() == 0) {
                                    emptyImageView.setVisibility(View.VISIBLE);
                                    emptyTextView.setVisibility(View.VISIBLE);
                                }
                                else {
                                    emptyImageView.setVisibility(View.GONE);
                                    emptyTextView.setVisibility(View.GONE);
                                }
                            }
                        }
                        else {
                            emptyImageView.setVisibility(View.VISIBLE);
                            emptyTextView.setVisibility(View.VISIBLE);
                        }
                    }
                    else{
                        filteredContactsPhoneIsEmpty = false;
                        headerContactsPhone.setVisibility(View.VISIBLE);
                        recyclerViewPhone.setVisibility(View.VISIBLE);
                        emptyImageView.setVisibility(View.GONE);
                        emptyTextView.setVisibility(View.GONE);
                    }
                }

            }
            else{
                log("PhoneContactsTask: Phone contacts null");
                boolean hasReadContactsPermission = (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);
                if (!hasReadContactsPermission) {
                    log("PhoneContactsTask: No read contacts permission");
                }
            }
        }
    }

    public final static boolean isValidEmail(CharSequence target) {
        if (target == null) {
            return false;
        } else {
            log("isValid");
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_add_contact, menu);

        sendInvitationMenuItem = menu.findItem(R.id.action_send_invitation);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        log("onPrepareOptionsMenu");

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected");
        ((MegaApplication) getApplication()).sendSignalPresenceActivity();

        int id = item.getItemId();
        switch(id) {
            case android.R.id.home: {
                onBackPressed();
                break;
            }
            case R.id.action_send_invitation:{
                if (contactType == Constants.CONTACT_TYPE_DEVICE){
                    inviteContacts(addedContactsPhone);
                }
                else if (contactType == Constants.CONTACT_TYPE_MEGA){
                    setResultContacts(addedContactsMEGA, true);
                }
                else {
                    shareWith(addedContactsShare);
                }
                hideKeyboard();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void hideKeyboard () {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void refreshKeyboard() {

        String s = inputString;
        if (s != null) {
            if (s.length() == 0 && (!addedContactsMEGA.isEmpty() || !addedContactsPhone.isEmpty() || !addedContactsShare.isEmpty())){
                addContactEditText.setImeOptions(EditorInfo.IME_ACTION_SEND);
            }
            else {
                addContactEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            }
        }
        else if (!addedContactsMEGA.isEmpty() || !addedContactsPhone.isEmpty() || !addedContactsShare.isEmpty()) {
            addContactEditText.setImeOptions(EditorInfo.IME_ACTION_SEND);
        }
        else {
            addContactEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        }

        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            //inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            inputMethodManager.restartInput(view);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");

        super.onCreate(savedInstanceState);

        ((MegaApplication) getApplication()).sendSignalPresenceActivity();

        if (getIntent() != null){
            contactType = getIntent().getIntExtra("contactType", Constants.CONTACT_TYPE_MEGA);
            chatId = getIntent().getLongExtra("chatId", -1);
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                comesFromChat = bundle.getBoolean("chat");
            }
            log("comesFromchat: "+comesFromChat);

            if (contactType == Constants.CONTACT_TYPE_MEGA || contactType == Constants.CONTACT_TYPE_BOTH){
                multipleSelectIntent = getIntent().getIntExtra("MULTISELECT", -1);
                if(multipleSelectIntent==0){
                    nodeHandle =  getIntent().getLongExtra(EXTRA_NODE_HANDLE, -1);
                }
                else if(multipleSelectIntent==1){
                    log("onCreate multiselect YES!");
                    nodeHandles=getIntent().getLongArrayExtra(EXTRA_NODE_HANDLE);
                }
                sendToInbox= getIntent().getIntExtra("SEND_FILE", -1);
            }
        }

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        screenHeight = getResources().getDisplayMetrics().heightPixels;

        app = (MegaApplication)getApplication();
        megaApi = app.getMegaApi();
        if(megaApi==null||megaApi.getRootNode()==null){
            log("Refresh session - sdk");
            Intent intent = new Intent(this, LoginActivityLollipop.class);
            intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }
        if(Util.isChatEnabled()){
            if (megaChatApi == null){
                megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
            }

            if(megaChatApi==null||megaChatApi.getInitState()== MegaChatApi.INIT_ERROR){
                log("Refresh session - karere");
                Intent intent = new Intent(this, LoginActivityLollipop.class);
                intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return;
            }
        }

        addContactActivityLollipop = this;

        log("retryPendingConnections()");
        if (megaApi != null){
            log("---------retryPendingConnections");
            megaApi.retryPendingConnections();
        }

        dbH = DatabaseHandler.getDbHandler(this);
        handler = new Handler();
        setContentView(R.layout.activity_add_contact);

        tB = (Toolbar) findViewById(R.id.add_contact_toolbar);
        if(tB==null){
            log("Tb is Null");
            return;
        }

        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        log("aB.setHomeAsUpIndicator_1");
        aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
        aBHeight = aB.getHeight();

        max = ((screenHeight- aBHeight) / 3);

        relativeLayout = (RelativeLayout) findViewById(R.id.relative_container_add_contact);

        addContactEditText = (EditText) findViewById(R.id.addcontact_edittext);

        addContactEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                refreshKeyboard();
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String s = v.getText().toString();
                    if (s.isEmpty() || s.equals("null") || s.equals("")){
                        hideKeyboard();
                    }
                    else {
                        if (contactType == Constants.CONTACT_TYPE_MEGA){
                            if (!comesFromChat){
                                boolean isValid = isValidEmail(s.trim());
                                if(isValid){
                                    if (!heightMax){
                                        MegaContactAdapter contact = new MegaContactAdapter(null, null, s.trim());
                                        addContactMEGA(contact);
                                        addContactEditText.getText().clear();
                                        inputString = "";
                                    }
                                    else {
                                        showSnackbar(getResources().getString(R.string.max_add_contact));
                                        addContactEditText.getText().clear();
                                        inputString = "";
                                    }
                                }
                                else{
                                    setError();
                                }
                            }
                            else {
                                setError();
                            }
                            filterContactsMEGA();
                        }
                        else if (contactType == Constants.CONTACT_TYPE_DEVICE){
                            boolean isValid = isValidEmail(s.trim());
                            if(isValid){
                                if (!heightMax){
                                    PhoneContactInfo contact = new PhoneContactInfo(0, null, s.trim(), null);
                                    addContact(contact);
                                    addContactEditText.getText().clear();
                                    inputString = "";
                                }
                                else {
                                    showSnackbar(getResources().getString(R.string.max_add_contact));
                                    addContactEditText.getText().clear();
                                    inputString = "";
                                }
                            }
                            else{
                                setError();
                            }
                            cancelled = true;
                            new PhoneContactsTask().execute();
                        }
                        else {
                            boolean isValid = isValidEmail(s.trim());
                            if (isValid){
                                if (!heightMax) {
                                    ShareContactInfo contact = new ShareContactInfo(0, null, s.trim(), null,
                                            null, null, null,
                                            false, false);
                                    addShareContact(contact);
                                    addContactEditText.getText().clear();
                                    inputString = "";
                                }
                                else {
                                    showSnackbar(getResources().getString(R.string.max_add_contact));
                                    addContactEditText.getText().clear();
                                    inputString = "";
                                }
                            }
                            else {
                                setError();
                            }
                            cancelled = true;
                            new PhoneContactsTask().execute();
                            filterContactsMEGA();
                        }
                    }
                    return true;
                }

                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_SEND)) {
                    if (contactType == Constants.CONTACT_TYPE_DEVICE){
                        if (addedContactsPhone.isEmpty() || addedContactsPhone == null) {
                            hideKeyboard();
                        }
                        else {
                            inviteContacts(addedContactsPhone);
                        }
                    }
                    else if (contactType == Constants.CONTACT_TYPE_MEGA){
                        if (addedContactsMEGA.isEmpty() || addedContactsMEGA == null) {
                            hideKeyboard();
                        }
                        else {
                            setResultContacts(addedContactsMEGA, true);
                        }
                    }
                    else {
                        if (addedContactsShare.isEmpty() || addedContactsShare == null) {
                            hideKeyboard();
                        }
                        else {
                            shareWith(addedContactsShare);
                        }
                    }
                    return true;
                }

                return false;
            }
        });
        addContactEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        addContactEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                quitError();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                log("onTextChanged: " + s.toString() + "_ " + start + "__" + before + "__" + count);
                if (contactType == Constants.CONTACT_TYPE_MEGA){
                    if (s != null){
                        if (s.length() > 0) {
                            inputString = s.toString();
                            String temp = s.toString();
                            char last = s.charAt(s.length()-1);
                            if(last == ' '){
                                if (!comesFromChat){
                                    boolean isValid = isValidEmail(temp.trim());
                                    if(isValid){
                                        if (!heightMax){
                                            MegaContactAdapter contact = new MegaContactAdapter(null, null, temp.trim());
                                            addContactMEGA(contact);
                                        }
                                        else {
                                            showSnackbar(getResources().getString(R.string.max_add_contact));
                                        }
                                        addContactEditText.getText().clear();
                                        inputString = "";
                                    }
                                    else{
                                        setError();
                                    }
                                }
                                else {
                                    setError();
                                }
                            }
                            else{
                                log("Last character is: "+last);
                            }
                        }
                        else{
                            inputString = "";
                        }
                    }
                    filterContactsMEGA();
                }
                else if (contactType == Constants.CONTACT_TYPE_DEVICE){
                    if (s != null) {
                        if (s.length() > 0) {
                            inputString = s.toString();
                            String temp = s.toString();
                            char last = s.charAt(s.length()-1);
                            if(last == ' '){
                                boolean isValid = isValidEmail(temp.trim());
                                if(isValid){
                                    if (!heightMax){
                                        PhoneContactInfo contact = new PhoneContactInfo(0, null, temp.trim(), null);
                                        addContact(contact);
                                    }
                                    else {
                                        showSnackbar(getResources().getString(R.string.max_add_contact));
                                    }
                                    addContactEditText.getText().clear();
                                    inputString = "";
                                }
                                else{
                                    setError();
                                }
                            }
                            else{
                                log("Last character is: "+last);
                            }
                        }
                        else{
                            inputString = "";
                        }
                    }
                    cancelled = true;
                    new PhoneContactsTask().execute();
                }
                else {
                    if (s != null) {
                        if (s.length() > 0) {
                            inputString = s.toString();
                            String temp = s.toString();
                            char last = s.charAt(s.length()-1);
                            if (last == ' '){
                                boolean isValid = isValidEmail(temp.trim());
                                if (isValid){
                                    if (!heightMax) {
                                        ShareContactInfo contact = new ShareContactInfo(0, null, temp.trim(), null,
                                                null, null, null,
                                                false, false);
                                        addShareContact(contact);
                                    }
                                    else {
                                        showSnackbar(getResources().getString(R.string.max_add_contact));
                                    }
                                    addContactEditText.getText().clear();
                                    inputString = "";
                                }
                                else {
                                    setError();
                                }
                            }
                            else{
                                log("Last character is: "+last);
                            }
                        }
                        else {
                            inputString = "";
                        }
                    }
                    cancelled = true;
                    new PhoneContactsTask().execute();
                    filterContactsMEGA();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

                refreshKeyboard();
            }
        });

        contactErrorLayout = (RelativeLayout) findViewById(R.id.add_contact_email_error);
        contactErrorLayout.setVisibility(View.GONE);
        notPermitedAddContacts = (RelativeLayout) findViewById(R.id.not_permited_add_contact_error);
        notPermitedAddContacts.setVisibility(View.GONE);
        editTextBackground = addContactEditText.getBackground().mutate().getConstantState().newDrawable();
        addedContactsRecyclerView = (RecyclerView) findViewById(R.id.contact_adds_recycler_view);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN){

            mLayoutManager_2 = new LinearLayoutManager(this);
            addedContactsRecyclerView.setLayoutManager(mLayoutManager_2);

        }else{

            mLayoutManager = new FlowLayoutManager().setAlignment(Alignment.LEFT);
            mLayoutManager.setAutoMeasureEnabled(true);
            addedContactsRecyclerView.setLayoutManager(mLayoutManager);
        }

        addedContactsRecyclerView.setItemAnimator(new DefaultItemAnimator());

        detector = new GestureDetectorCompat(this, new RecyclerViewOnGestureListener());

//        headerContactsPhone = (RelativeLayout) findViewById(R.id.container_list_phone);
//        headerContactsMega = (RelativeLayout) findViewById(R.id.container_list_mega);
        headerContactsPhone = (RelativeLayout) findViewById(R.id.header_list_phone);
        headerContactsMega = (RelativeLayout) findViewById(R.id.header_list_mega);

        if (contactType == Constants.CONTACT_TYPE_MEGA) {
            headerContactsMega.setVisibility(View.VISIBLE);
            headerContactsPhone.setVisibility(View.GONE);
        }
        else if (contactType == Constants.CONTACT_TYPE_DEVICE) {
            headerContactsMega.setVisibility(View.GONE);
            headerContactsPhone.setVisibility(View.VISIBLE);
        }
        else if (contactType == Constants.CONTACT_TYPE_BOTH) {
            headerContactsMega.setVisibility(View.VISIBLE);
            headerContactsPhone.setVisibility(View.VISIBLE);
        }

        recyclerViewPhone = (RecyclerView) findViewById(R.id.add_contact_list_phone);
//        recyclerViewPhone.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
        recyclerViewPhone.setClipToPadding(false);
        recyclerViewPhone.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
        recyclerViewPhone.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager1 = new LinearLayoutManager(this);
        recyclerViewPhone.setLayoutManager(linearLayoutManager1);
        recyclerViewPhone.addOnItemTouchListener(this);
        recyclerViewPhone.setItemAnimator(new DefaultItemAnimator());

        recyclerViewMega = (RecyclerView) findViewById(R.id.add_contact_list_mega);
//        recyclerViewMega.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
        recyclerViewMega.setClipToPadding(false);
        recyclerViewMega.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
        recyclerViewMega.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager2 = new LinearLayoutManager(this);
        recyclerViewMega.setLayoutManager(linearLayoutManager2);
        recyclerViewMega.addOnItemTouchListener(this);
        recyclerViewMega.setItemAnimator(new DefaultItemAnimator());

        emptyImageView = (ImageView) findViewById(R.id.add_contact_list_empty_image);
        emptyTextView = (TextView) findViewById(R.id.add_contact_list_empty_text);
        emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
        emptyTextView.setText(R.string.contacts_list_empty_text_loading);

        progressBar = (ProgressBar) findViewById(R.id.add_contact_progress_bar);

        //Get MEGA contacts and phone contacts: first name, last name and email
        if (contactType == Constants.CONTACT_TYPE_MEGA) {

            if (adapterMEGAContacts == null){
                adapterMEGAContacts = new MegaAddContactsLollipopAdapter(this, addedContactsMEGA);
            }

            addedContactsRecyclerView.setAdapter(adapterMEGAContacts);

            aB.setTitle(getString(R.string.menu_choose_contact));

            if (sendInvitationMenuItem != null){
                sendInvitationMenuItem.setVisible(false);
            }
            contactsMEGA = megaApi.getContacts();
            visibleContactsMEGA.clear();

            if(chatId!=-1){
                log("Add participant to chat");
                if(megaChatApi!=null){
                    MegaChatRoom chat = megaChatApi.getChatRoom(chatId);
                    if(chat!=null){
                        long participantsCount = chat.getPeerCount();

                        for (int i=0;i<contactsMEGA.size();i++){
                            if (contactsMEGA.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){

                                boolean found = false;

                                for(int j=0;j<participantsCount;j++) {

                                    long peerHandle = chat.getPeerHandle(j);

                                    if(peerHandle == contactsMEGA.get(i).getHandle()){
                                        found = true;
                                        break;
                                    }
                                }

                                if(!found){
                                    MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contactsMEGA.get(i).getHandle()+""));
                                    String fullName = "";
                                    if(contactDB!=null){
                                        ContactController cC = new ContactController(this);
                                        fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contactsMEGA.get(i).getEmail());
                                    }
                                    else{
                                        //No name, ask for it and later refresh!!
                                        fullName = contactsMEGA.get(i).getEmail();
                                    }

                                    log("Added to list: " + contactsMEGA.get(i).getEmail() + "_" + contactsMEGA.get(i).getVisibility());
                                    MegaContactAdapter megaContactAdapter = new MegaContactAdapter(contactDB, contactsMEGA.get(i), fullName);
                                    visibleContactsMEGA.add(megaContactAdapter);
                                }
                                else{
                                    MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contactsMEGA.get(i).getHandle()+""));
                                    String fullName = "";
                                    if(contactDB!=null){
                                        ContactController cC = new ContactController(this);
                                        fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contactsMEGA.get(i).getEmail());
                                    }
                                    else{
                                        //No name, ask for it and later refresh!!
                                        fullName = contactsMEGA.get(i).getEmail();
                                    }

                                    log("Removed from list - already included on chat: "+fullName);
                                }
                            }
                        }
                    }
                    else{
                        for (int i=0;i<contactsMEGA.size();i++){
                            log("contact: " + contactsMEGA.get(i).getEmail() + "_" + contactsMEGA.get(i).getVisibility());
                            if (contactsMEGA.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){

                                MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contactsMEGA.get(i).getHandle()+""));
                                String fullName = "";
                                if(contactDB!=null){
                                    ContactController cC = new ContactController(this);
                                    fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contactsMEGA.get(i).getEmail());
                                }
                                else{
                                    //No name, ask for it and later refresh!!
                                    fullName = contactsMEGA.get(i).getEmail();
                                }

                                MegaContactAdapter megaContactAdapter = new MegaContactAdapter(contactDB, contactsMEGA.get(i), fullName);
                                visibleContactsMEGA.add(megaContactAdapter);

                            }
                        }
                    }
                }
            }
            else{
                for (int i=0;i<contactsMEGA.size();i++){
                    log("contact: " + contactsMEGA.get(i).getEmail() + "_" + contactsMEGA.get(i).getVisibility());
                    if (contactsMEGA.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){

                        MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contactsMEGA.get(i).getHandle()+""));
                        String fullName = "";
                        if(contactDB!=null){
                            ContactController cC = new ContactController(this);
                            fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contactsMEGA.get(i).getEmail());
                        }
                        else{
                            //No name, ask for it and later refresh!!
                            fullName = contactsMEGA.get(i).getEmail();
                        }

                        MegaContactAdapter megaContactAdapter = new MegaContactAdapter(contactDB, contactsMEGA.get(i), fullName);
                        visibleContactsMEGA.add(megaContactAdapter);

                    }
                }
            }

            Collections.sort(visibleContactsMEGA, new Comparator<MegaContactAdapter>(){

                public int compare(MegaContactAdapter c1, MegaContactAdapter c2) {
                    String name1 = c1.getFullName();
                    String name2 = c2.getFullName();
                    int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
                    if (res == 0) {
                        res = name1.compareTo(name2);
                    }
                    return res;
                }
            });

            for (int i= 0; i<visibleContactsMEGA.size(); i++){
                filteredContactMEGA.add(visibleContactsMEGA.get(i));
            }

            if (adapterMEGA == null) {
                adapterMEGA = new MegaContactsLollipopAdapter(this, null, filteredContactMEGA, recyclerViewMega, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
            } else {
                adapterMEGA.setContacts(filteredContactMEGA);
                adapterMEGA.setAdapterType(MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
            }

            adapterMEGA.setPositionClicked(-1);
            recyclerViewMega.setAdapter(adapterMEGA);

            if (adapterMEGA.getItemCount() == 0) {

                emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
                String textToShow = String.format(getString(R.string.context_empty_contacts), getString(R.string.section_contacts));
                try{
                    textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                }
                catch (Exception e){}
                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }
                emptyTextView.setText(result);
                headerContactsMega.setVisibility(View.GONE);
                recyclerViewMega.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);
            } else {
                headerContactsMega.setVisibility(View.VISIBLE);
                recyclerViewMega.setVisibility(View.VISIBLE);
                emptyImageView.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.GONE);
            }
        }
        else if (contactType == Constants.CONTACT_TYPE_DEVICE){
            if (adapterContacts == null){
                adapterContacts = new AddContactsLollipopAdapter(this, addedContactsPhone);
            }

            addedContactsRecyclerView.setAdapter(adapterContacts);

            aB.setTitle(getString(R.string.menu_add_contact));

            if (sendInvitationMenuItem != null){
                sendInvitationMenuItem.setVisible(false);
            }


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean hasReadContactsPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);
                if (!hasReadContactsPermission) {
                    log("No read contacts permission");
                    ActivityCompat.requestPermissions((AddContactActivityLollipop) this,
                            new String[]{Manifest.permission.READ_CONTACTS},
                            Constants.REQUEST_READ_CONTACTS);
                } else {
                    filteredContactsPhone.clear();

                    emptyImageView.setVisibility(View.VISIBLE);
                    emptyTextView.setVisibility(View.VISIBLE);

                    progressBar.setVisibility(View.VISIBLE);
                    cancelled = false;
                    new PhoneContactsTask().execute();

                }
            }
            else{
                filteredContactsPhone.clear();

                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);

                progressBar.setVisibility(View.VISIBLE);
                new PhoneContactsTask().execute();
            }
        }
        else {

            if (adapterShare == null) {
                adapterShare = new ShareContactsAdapter(this, addedContactsShare);
            }

            addedContactsRecyclerView.setAdapter(adapterShare);

            aB.setTitle(getString(R.string.menu_choose_contact));

            if (sendInvitationMenuItem != null){
                sendInvitationMenuItem.setVisible(false);
            }
            contactsMEGA = megaApi.getContacts();
            visibleContactsMEGA.clear();

            for (int i=0;i<contactsMEGA.size();i++){
                log("contact: " + contactsMEGA.get(i).getEmail() + "_" + contactsMEGA.get(i).getVisibility());
                if (contactsMEGA.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){

                    MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contactsMEGA.get(i).getHandle()+""));
                    String fullName = "";
                    if(contactDB!=null){
                        ContactController cC = new ContactController(this);
                        fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contactsMEGA.get(i).getEmail());
                    }
                    else{
                        //No name, ask for it and later refresh!!
                        fullName = contactsMEGA.get(i).getEmail();
                    }

                    MegaContactAdapter megaContactAdapter = new MegaContactAdapter(contactDB, contactsMEGA.get(i), fullName);
                    visibleContactsMEGA.add(megaContactAdapter);
                }
            }

            Collections.sort(visibleContactsMEGA, new Comparator<MegaContactAdapter>(){

                public int compare(MegaContactAdapter c1, MegaContactAdapter c2) {
                    String name1 = c1.getFullName();
                    String name2 = c2.getFullName();
                    int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
                    if (res == 0) {
                        res = name1.compareTo(name2);
                    }
                    return res;
                }
            });

            for (int i= 0; i<visibleContactsMEGA.size(); i++){
                filteredContactMEGA.add(visibleContactsMEGA.get(i));
            }

            if (adapterMEGA == null) {
                adapterMEGA = new MegaContactsLollipopAdapter(this, null, filteredContactMEGA, recyclerViewMega, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
            } else {
                adapterMEGA.setContacts(filteredContactMEGA);
                adapterMEGA.setAdapterType(MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
            }

            adapterMEGA.setPositionClicked(-1);
            recyclerViewMega.setAdapter(adapterMEGA);

            if (adapterMEGA.getItemCount() == 0) {
                headerContactsMega.setVisibility(View.GONE);
                recyclerViewMega.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);
            } else {
                headerContactsMega.setVisibility(View.VISIBLE);
                recyclerViewMega.setVisibility(View.VISIBLE);
                emptyImageView.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.GONE);
            }


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean hasReadContactsPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);
                if (!hasReadContactsPermission) {
                    log("No read contacts permission");
                    ActivityCompat.requestPermissions((AddContactActivityLollipop) this,
                            new String[]{Manifest.permission.READ_CONTACTS},
                            Constants.REQUEST_READ_CONTACTS);
                } else {
                    filteredContactsPhone.clear();

                    emptyImageView.setVisibility(View.VISIBLE);
                    emptyTextView.setVisibility(View.VISIBLE);

                    progressBar.setVisibility(View.VISIBLE);
                    cancelled = false;
                    new PhoneContactsTask().execute();

                }
            }
            else{
                filteredContactsPhone.clear();

                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);

                progressBar.setVisibility(View.VISIBLE);
                new PhoneContactsTask().execute();
            }

            if (adapterMEGA.getItemCount() == 0 && filteredContactsPhoneIsEmpty){
                emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
                String textToShow = String.format(getString(R.string.context_empty_contacts), getString(R.string.section_contacts));
                try{
                    textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                }
                catch (Exception e){}
                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }
                emptyTextView.setText(result);
            }
        }
    }

    private void filterContactsMEGA(){
        log("filterContactsMEGA");
        boolean found;

        filteredContactMEGA.clear();
        if (contactType == Constants.CONTACT_TYPE_MEGA){
            if (inputString != null && inputString.equals("")){
                for (int i=0; i<visibleContactsMEGA.size(); i++){
                    found = false;
                    for (int j=0; j<addedContactsMEGA.size(); j++){
                        if (visibleContactsMEGA.get(i).equals(addedContactsMEGA.get(j))){
                            found = true;
                            break;
                        }
                    }
                    if (!found){
                        filteredContactMEGA.add(visibleContactsMEGA.get(i));
                    }
                }
            }
            else {
                for (int i=0; i<visibleContactsMEGA.size(); i++){
                    found = false;
                    if ((visibleContactsMEGA.get(i).getFullName().toUpperCase().contains(inputString.toUpperCase()) || visibleContactsMEGA.get(i).getMegaUser().getEmail().toUpperCase().contains(inputString.toUpperCase()))){
                        for (int j=0; j<addedContactsMEGA.size(); j++){
                            if (visibleContactsMEGA.get(i).equals(addedContactsMEGA.get(j))){
                                found = true;
                                break;
                            }
                        }
                        if (!found){
                            filteredContactMEGA.add(visibleContactsMEGA.get(i));
                        }
                    }
                }
            }
        }
        else  if (contactType == Constants.CONTACT_TYPE_BOTH){
            if (inputString != null && inputString.equals("")){
                for (int i=0; i<visibleContactsMEGA.size(); i++){
                    found = false;
                    for (int j=0; j<addedContactsShare.size(); j++){
                        if (addedContactsShare.get(j).getMegaContactAdapter()){
                            if (visibleContactsMEGA.get(i).getMegaUser().getEmail().equals(addedContactsShare.get(j).getMegaUser().getEmail())){
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found){
                        filteredContactMEGA.add(visibleContactsMEGA.get(i));
                    }
                }
            }
            else {
                for (int i=0; i<visibleContactsMEGA.size(); i++){
                    found = false;
                    if ((visibleContactsMEGA.get(i).getFullName().toUpperCase().contains(inputString.toUpperCase()) || visibleContactsMEGA.get(i).getMegaUser().getEmail().toUpperCase().contains(inputString.toUpperCase()))){
                        for (int j=0; j<addedContactsShare.size(); j++){
                            if (addedContactsShare.get(j).getMegaContactAdapter()){
                                if (visibleContactsMEGA.get(i).getMegaUser().getEmail().equals(addedContactsShare.get(j).getMegaUser().getEmail())){
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found){
                            filteredContactMEGA.add(visibleContactsMEGA.get(i));
                        }
                    }
                }
            }
        }

        adapterMEGA.setContacts(filteredContactMEGA);

        if (adapterMEGA.getItemCount() == 0){
            headerContactsMega.setVisibility(View.GONE);
            recyclerViewMega.setVisibility(View.GONE);
        }
        else {
            headerContactsMega.setVisibility(View.VISIBLE);
            recyclerViewMega.setVisibility(View.VISIBLE);
        }
    }

    private void setError(){
        log("setError");
        if (comesFromChat){
            notPermitedAddContacts.setVisibility(View.VISIBLE);
        }
        else {
            contactErrorLayout.setVisibility(View.VISIBLE);
        }
        PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(getResources().getColor(R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
        Drawable background = editTextBackground.mutate().getConstantState().newDrawable();
        background.setColorFilter(porterDuffColorFilter);
        addContactEditText.setBackground(background);
    }

    private void quitError(){
        if(contactErrorLayout.getVisibility() != View.GONE){
            log("quitError");
            contactErrorLayout.setVisibility(View.GONE);
            addContactEditText.setBackground(editTextBackground);
        }
        if(notPermitedAddContacts.getVisibility() != View.GONE){
            log("quitError");
            notPermitedAddContacts.setVisibility(View.GONE);
            addContactEditText.setBackground(editTextBackground);
        }
    }


    public void getRelativeLayoutInfo(){
        recyclerLayoutAddContacts = (RelativeLayout) findViewById(R.id.recycler_layout_add_contacts);
        recyclerHeight = recyclerLayoutAddContacts.getHeight();
        log("height recycler: " + recyclerHeight + " height screen: "+screenHeight + "aB: " + aBHeight);
        if (recyclerHeight >= max) {
            heightMax = true;
        }
    }

    public void addShareContact (ShareContactInfo contact) {
        log("addShareContact");

        if (!heightMax){
            addedContactsShare.add(contact);
            adapterShare.setContacts(addedContactsShare);
            sendInvitationMenuItem.setVisible(true);
            addedContactsRecyclerView.setVisibility(View.VISIBLE);
            getRelativeLayoutInfo();
        }

        if (adapterPhone != null){
            if (adapterPhone.getItemCount() == 0){
                headerContactsPhone.setVisibility(View.GONE);
                recyclerViewPhone.setVisibility(View.GONE);
            }
        }

        if (adapterMEGA != null) {
            if (adapterMEGA.getItemCount() == 0){
                headerContactsMega.setVisibility(View.GONE);
                recyclerViewMega.setVisibility(View.GONE);
            }
        }

        if (adapterPhone != null && adapterMEGA != null){
            if (adapterPhone.getItemCount() == 0 && adapterMEGA.getItemCount() == 1){
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);

                String textToShow = String.format(getString(R.string.context_empty_contacts), getString(R.string.section_contacts));
                try{
                    textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                }
                catch (Exception e){}
                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }
                emptyTextView.setText(result);
            }
        }

        refreshKeyboard();
    }

    public void addContactMEGA (MegaContactAdapter contact) {
        log("addContactMEGA: " + contact.getFullName());

        if (!heightMax) {
            addedContactsMEGA.add(contact);
            adapterMEGAContacts.setContacts(addedContactsMEGA);
            sendInvitationMenuItem.setVisible(true);
            addedContactsRecyclerView.setVisibility(View.VISIBLE);
            getRelativeLayoutInfo();
        }

        if (adapterMEGA != null){
            if (adapterMEGA.getItemCount() == 1){
                headerContactsMega.setVisibility(View.GONE);
                recyclerViewMega.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);

                String textToShow = String.format(getString(R.string.context_empty_contacts), getString(R.string.section_contacts));
                try{
                    textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                }
                catch (Exception e){}
                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }
                emptyTextView.setText(result);
            }
        }

        refreshKeyboard();
    }

    public void addContact (PhoneContactInfo contact){
        log("addContact: " + contact.getName()+" mail: " + contact.getEmail());

        if (!heightMax){
            addedContactsPhone.add(contact);
            adapterContacts.setContacts(addedContactsPhone);
            sendInvitationMenuItem.setVisible(true);
            addedContactsRecyclerView.setVisibility(View.VISIBLE);
            getRelativeLayoutInfo();
        }

        if(adapterPhone!=null){
            if (adapterPhone.getItemCount() == 0){
                headerContactsPhone.setVisibility(View.GONE);
                recyclerViewPhone.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);

                String textToShow = String.format(getString(R.string.context_empty_contacts), getString(R.string.section_contacts));
                try{
                    textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                }
                catch (Exception e){}
                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }
                emptyTextView.setText(result);
            }
        }

        refreshKeyboard();
    }

    public void deleteContact (int position){
        log("deleteContact: " +position);

        if (contactType == Constants.CONTACT_TYPE_MEGA){
            MegaContactAdapter deleteContact = addedContactsMEGA.get(position);
            for (int i = 0; i<addedContactsMEGA.size(); i++){
                MegaContactAdapter contact = addedContactsMEGA.get(i);

                log("contact full name : " +contact.getFullName() + "delete contact full name: " +deleteContact.getFullName());
                if (deleteContact.getFullName().equals(contact.getFullName()) && deleteContact.getMegaContactDB() == null && deleteContact.getMegaUser() == null){
                    log("full name");
                    break;
                }
                else{
                    if(contact.getMegaUser() != null){
                        if (contact.getMegaUser().getEmail() != null){
                            if(deleteContact.getMegaUser() != null){
                                if (contact.getMegaUser().getEmail().equals(deleteContact.getMegaUser().getEmail())){
                                    addMEGAFilteredContact(contact);
                                    break;
                                }
                            }
                        }
                    }

                }
            }
            addedContactsMEGA.remove(position);
            if (addedContactsMEGA.size() == 0){
                sendInvitationMenuItem.setVisible(false);
            }
            else {
                sendInvitationMenuItem.setVisible(true);
            }
            adapterMEGAContacts.setContacts(addedContactsMEGA);
        }
        else if (contactType == Constants.CONTACT_TYPE_DEVICE){
            PhoneContactInfo deleteContact = addedContactsPhone.get(position);
            for (int i = 0; i<addedContactsPhone.size(); i++){
                PhoneContactInfo contact = addedContactsPhone.get(i);

                if (deleteContact.getEmail().equals(contact.getEmail())){
                    if (contact.getName() == null){
                        break;
                    }
                    else {
                        addFilteredContact(contact);
                        break;
                    }
                }
            }
            addedContactsPhone.remove(position);
            if (addedContactsPhone.size() == 0){
                sendInvitationMenuItem.setVisible(false);
            }
            else {
                sendInvitationMenuItem.setVisible(true);
            }
            adapterContacts.setContacts(addedContactsPhone);
        }
        else {
            ShareContactInfo deleteContact = addedContactsShare.get(position);
            for (int i=0; i<addedContactsShare.size(); i++){
                ShareContactInfo contact = addedContactsShare.get(i);
                if (!deleteContact.getMegaContactAdapter() && !deleteContact.getPhoneContactInfo()){
                    break;
                }
                else if (contact.getMegaContactAdapter()){
                    if(contact.getMegaUser() != null){
                        if (contact.getMegaUser().getEmail() != null){
                            if(deleteContact.getMegaUser() != null){
                                if (contact.getMegaUser().getEmail().equals(deleteContact.getMegaUser().getEmail())){
                                    MegaContactAdapter contactM = new MegaContactAdapter(contact.getMegaContactDB(), contact.getMegaUser(), contact.getFullName());
                                    addMEGAFilteredContact(contactM);
                                    break;
                                }
                            }
                        }
                    }
                }
                else {
                    if (deleteContact.getEmail().equals(contact.getEmail())){
                        PhoneContactInfo contactP = new PhoneContactInfo(contact.getId(), contact.getName(), contact.getEmail(), contact.getPhoneNumber());
                        addFilteredContact(contactP);
                        break;
                    }
                }
            }
            addedContactsShare.remove(position);
            if (addedContactsShare.size() == 0){
                sendInvitationMenuItem.setVisible(false);
            }
            else {
                sendInvitationMenuItem.setVisible(true);
            }
            adapterShare.setContacts(addedContactsShare);
        }
        heightMax = false;
        refreshKeyboard();
    }

    public void showSnackbar(String message) {
        hideKeyboard();
        Snackbar snackbar = Snackbar.make(relativeLayout, message, Snackbar.LENGTH_LONG);
        TextView snackbarTextView = (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbarTextView.setMaxLines(5);
        snackbar.show();
    }

    private void addMEGAFilteredContact (MegaContactAdapter contact) {

        filteredContactMEGA.add(contact);

        Collections.sort(filteredContactMEGA, new Comparator<MegaContactAdapter>(){

            public int compare(MegaContactAdapter c1, MegaContactAdapter c2) {
                String name1 = c1.getFullName();
                String name2 = c2.getFullName();
                int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
                if (res == 0) {
                    res = name1.compareTo(name2);
                }
                return res;
            }
        });

        adapterMEGA.setContacts(filteredContactMEGA);

        if (adapterMEGA.getItemCount() != 0){
            headerContactsMega.setVisibility(View.VISIBLE);
            recyclerViewMega.setVisibility(View.VISIBLE);
            emptyImageView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.GONE);
        }
    }

    private void addFilteredContact(PhoneContactInfo contact) {
        log("addFilteredContact");
        filteredContactsPhone.add(contact);
        Collections.sort(filteredContactsPhone);
        adapterPhone.setContacts(filteredContactsPhone);
        log("Size filteredContactsPhone: " +filteredContactsPhone.size());
        if(adapterPhone!=null){
            if (adapterPhone.getItemCount() != 0){
                headerContactsPhone.setVisibility(View.VISIBLE);
                recyclerViewPhone.setVisibility(View.VISIBLE);
                emptyImageView.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.GONE);
            }
        }
    }

    @SuppressLint("InlinedApi")
    //Get the contacts explicitly added
    private ArrayList<PhoneContactInfo> getPhoneContacts() {
        log("getPhoneContacts");
        ArrayList<PhoneContactInfo> contactList = new ArrayList<>();
        log("inputString empty");
        String filter = ContactsContract.CommonDataKinds.Email.DATA + " NOT LIKE ''  AND " + ContactsContract.Contacts.IN_VISIBLE_GROUP + "=1";

        try {
            ContentResolver cr = getBaseContext().getContentResolver();
            String SORT_ORDER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? ContactsContract.Contacts.SORT_KEY_PRIMARY : ContactsContract.Contacts.DISPLAY_NAME;

            Cursor c = cr.query(
                    ContactsContract.Data.CONTENT_URI,
                    null,filter,
                    null, SORT_ORDER);

            while (c.moveToNext()){
                if (isCancelled()){
                    break;
                }
                long id = c.getLong(c.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                String name = c.getString(c.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                String emailAddress = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                log("ID: " + id + "___ NAME: " + name + "____ EMAIL: " + emailAddress);

                if ((!emailAddress.equalsIgnoreCase("")) && (emailAddress.contains("@")) && (!emailAddress.contains("s.whatsapp.net"))) {
                    if (inputString == ""){
                        log("VALID Contact: "+ name + " ---> "+ emailAddress);
                        PhoneContactInfo contactPhone = new PhoneContactInfo(id, name, emailAddress, null);
                        contactList.add(contactPhone);
                    }
                    else if (!inputString.isEmpty() && (name.toUpperCase().contains(inputString.toUpperCase()) || emailAddress.toUpperCase().contains(inputString.toUpperCase()))){
                        log("VALID Contact: "+ name + " ---> "+ emailAddress + " inputString: " + inputString);
                        PhoneContactInfo contactPhone = new PhoneContactInfo(id, name, emailAddress, null);
                        contactList.add(contactPhone);
                    }
                }
            }
            c.close();

            log("contactList.size() = " + contactList.size());

            return contactList;

        } catch (Exception e) { log ("Exception: " + e.getMessage()); }

        return null;
    }

    private boolean isCancelled() {
        return cancelled;
    }

    public void onTextChanged(CharSequence s, int start, int before, int count){

    }

    public void itemClick(String email, int position){

        log("itemClick");
        ((MegaApplication) getApplication()).sendSignalPresenceActivity();

        if (contactType == Constants.CONTACT_TYPE_MEGA) {
            if (!heightMax) {

                for (int i = 0; i < filteredContactMEGA.size(); i++) {
                    if (filteredContactMEGA.get(i).getMegaUser().getEmail().equals(email)) {
                        addContactMEGA(filteredContactMEGA.get(i));
                        filteredContactMEGA.remove(i);
                        adapterMEGA.setContacts(filteredContactMEGA);
                        break;
                    }
                }
            }
            else {
                showSnackbar(getResources().getString(R.string.max_add_contact));
            }

            if (addedContactsMEGA.size() == 0){
                aB.setTitle(getString(R.string.menu_choose_contact));
                if (sendInvitationMenuItem != null){
                    sendInvitationMenuItem.setVisible(false);
                }
            }
        }
        else if (contactType == Constants.CONTACT_TYPE_BOTH){
            if (!heightMax) {
                ContactInfo c = new ContactInfo();

                for (int i = 0; i < filteredContactMEGA.size(); i++) {
                    if (filteredContactMEGA.get(i).getMegaUser().getEmail().equals(email)) {
                        ShareContactInfo contact = new ShareContactInfo(0, null, null, null,
                                filteredContactMEGA.get(i).getMegaContactDB(), filteredContactMEGA.get(i).getMegaUser(), filteredContactMEGA.get(i).getFullName(),
                                false, true);
                        addShareContact(contact);
                        filteredContactMEGA.remove(i);
                        adapterMEGA.setContacts(filteredContactMEGA);
                        break;
                    }
                }
            }
            else {
                showSnackbar(getResources().getString(R.string.max_add_contact));
            }

            if (addedContactsShare.size() == 0){
                aB.setTitle(getString(R.string.menu_choose_contact));
                if (sendInvitationMenuItem != null){
                    sendInvitationMenuItem.setVisible(false);
                }
            }
        }
        addContactEditText.setText("");
    }

    public void itemClick(View view, int position) {
        log("on item click");

        if (contactType == Constants.CONTACT_TYPE_DEVICE){

            if(adapterPhone==null){
                return;
            }

            final PhoneContactInfo contact = adapterPhone.getDocumentAt(position);
            if(contact == null) {
                return;
            }

            if (!heightMax){
                ContactInfo c = new ContactInfo();

                for (int i = 0; i < filteredContactsPhone.size(); i++) {
                    if (filteredContactsPhone.get(i).getEmail().compareTo(contact.getEmail()) == 0) {
                        c.setEmail(filteredContactsPhone.get(i).getEmail());
                        c.setName(contact.getName());
                        filteredContactsPhone.remove(i);
                        adapterPhone.setContacts(filteredContactsPhone);
                        break;
                    }
                }

                if (c.getEmail().compareTo("") != 0) {
                    itemClickPressed = true;
                }

                addContact(contact);
            }
            else {
                showSnackbar(getResources().getString(R.string.max_add_contact));
            }
        }
        else if (contactType == Constants.CONTACT_TYPE_BOTH){
            if(adapterPhone==null){
                return;
            }

            final PhoneContactInfo contact = adapterPhone.getDocumentAt(position);
            if(contact == null) {
                return;
            }
            if (!heightMax){
                ContactInfo c = new ContactInfo();

                for (int i = 0; i < filteredContactsPhone.size(); i++) {
                    if (filteredContactsPhone.get(i).getEmail().compareTo(contact.getEmail()) == 0) {
                        c.setEmail(filteredContactsPhone.get(i).getEmail());
                        c.setName(contact.getName());
                        filteredContactsPhone.remove(i);
                        adapterPhone.setContacts(filteredContactsPhone);
                        break;
                    }
                }

                if (c.getEmail().compareTo("") != 0) {
                    itemClickPressed = true;
                }
                ShareContactInfo contactShare = new ShareContactInfo(contact.getId(), contact.getName(), contact.getEmail(), contact.getPhoneNumber(),
                        null, null, null,
                        true, false);
                addShareContact(contactShare);
            }
            else {
                showSnackbar(getResources().getString(R.string.max_add_contact));
            }
        }
        addContactEditText.setText("");

    }

    @Override
    public void onClick(View v) {
        ((MegaApplication) getApplication()).sendSignalPresenceActivity();

    }

    private void setResultContacts(ArrayList<MegaContactAdapter> addedContacts, boolean megaContacts){
        log("setResultContacts");
        ArrayList<String> contactsSelected = new ArrayList<String>();
        String contactEmail;

        if (addedContacts != null) {
            for (int i = 0; i < addedContacts.size(); i++) {
                if (addedContacts.get(i).getMegaUser() != null && addedContacts.get(i).getMegaContactDB() != null) {
                    contactEmail = addedContacts.get(i).getMegaUser().getEmail();
                } else {
                    contactEmail = addedContacts.get(i).getFullName();
                }
                if (contactEmail != null) {
                    contactsSelected.add(contactEmail);
                }
            }
        }
        log("contacts selected: "+contactsSelected.size());
        Intent intent = new Intent();
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contactsSelected);

        if(multipleSelectIntent==0){
            intent.putExtra(EXTRA_NODE_HANDLE, nodeHandle);
            intent.putExtra("MULTISELECT", 0);
        }
        else if(multipleSelectIntent==1){
            intent.putExtra(EXTRA_NODE_HANDLE, nodeHandles);
            intent.putExtra("MULTISELECT", 1);
        }

        if(sendToInbox==0){
            intent.putExtra("SEND_FILE",0);
        } else {
            intent.putExtra("SEND_FILE",1);
        }
        intent.putExtra(EXTRA_MEGA_CONTACTS, megaContacts);
        setResult(RESULT_OK, intent);
        hideKeyboard();
        finish();
    }

    private void setResultContact(String email){
        log("setResultContact");
        ArrayList<String> contactsSelected = new ArrayList<String>();

        Intent intent = new Intent();
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contactsSelected);

        contactsSelected.add(email);
        log("user email: "+contactsSelected.get(0));

        if(multipleSelectIntent==0){
            log("multiselectIntent == 0");
            intent.putExtra(EXTRA_NODE_HANDLE, nodeHandle);
            intent.putExtra("MULTISELECT", 0);
        }
        else if(multipleSelectIntent==1){
            log("multiselectIntent == 1");
            if(nodeHandles!=null){
                log("number of items selected: "+nodeHandles.length);
            }
            intent.putExtra(EXTRA_NODE_HANDLE, nodeHandles);
            intent.putExtra("MULTISELECT", 1);
        }

        intent.putExtra(EXTRA_MEGA_CONTACTS, false);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void shareWith (ArrayList<ShareContactInfo> addedContacts){
        log("shareWith");

        String contactEmail = null;
        ArrayList<String> contactsSelected = new ArrayList<>();
        if (addedContacts != null){
            for (int i=0; i<addedContacts.size(); i++){
                if (addedContacts.get(i).getMegaContactAdapter()){
                    if (addedContacts.get(i).getMegaUser() != null && addedContacts.get(i).getMegaContactDB() != null) {
                        contactEmail = addedContacts.get(i).getMegaUser().getEmail();
                    } else {
                        contactEmail = addedContacts.get(i).getFullName();
                    }
                    if (contactEmail != null) {
                        contactsSelected.add(contactEmail);
                    }
                }
                else if (addedContacts.get(i).getPhoneContactInfo()){
                    contactEmail = addedContacts.get(i).getEmail();
                    if (contactEmail != null) {
                        contactsSelected.add(contactEmail);
                    }
                }
                else {
                    contactEmail = addedContacts.get(i).getEmail();
                    if (contactEmail != null){
                        contactsSelected.add(contactEmail);
                    }
                }
            }
        }

        Intent intent = new Intent();
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contactsSelected);
        if(multipleSelectIntent==0){
            intent.putExtra(EXTRA_NODE_HANDLE, nodeHandle);
            intent.putExtra("MULTISELECT", 0);
        }
        else if(multipleSelectIntent==1){
            intent.putExtra(EXTRA_NODE_HANDLE, nodeHandles);
            intent.putExtra("MULTISELECT", 1);
        }

        if(sendToInbox==0){
            intent.putExtra("SEND_FILE",0);
        } else {
            intent.putExtra("SEND_FILE",1);
        }
        intent.putExtra(EXTRA_MEGA_CONTACTS, false);
        setResult(RESULT_OK, intent);
        hideKeyboard();
        finish();
    }

    private void inviteContacts(ArrayList<PhoneContactInfo> addedContacts){
        log("inviteContacts");

        String contactEmail = null;
        ArrayList<String> contactsSelected = new ArrayList<>();
        if (addedContacts != null) {
            for (int i=0;i<addedContacts.size();i++) {
//                if (adapterPhone != null) {
                    contactEmail = addedContacts.get(i).getEmail();
                    if (contactEmail != null) {
                        contactsSelected.add(contactEmail);
                    }
//                }
            }
        }

        Intent intent = new Intent();
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contactsSelected);
        for(int i=0; i<contactsSelected.size();i++){
            log("setResultContacts: "+contactsSelected.get(i));
        }

        intent.putExtra(EXTRA_MEGA_CONTACTS, false);
        setResult(RESULT_OK, intent);
        hideKeyboard();
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        log("onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constants.REQUEST_READ_CONTACTS: {
                log("REQUEST_READ_CONTACTS");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    boolean hasReadContactsPermissions = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);
                    if (hasReadContactsPermissions) {
                        filteredContactsPhone.clear();
                        emptyImageView.setVisibility(View.VISIBLE);
                        emptyTextView.setVisibility(View.VISIBLE);

                        progressBar.setVisibility(View.VISIBLE);
                        new PhoneContactsTask().execute();
                    }
                }
                else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    boolean hasReadContactsPermissions = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED);
                    if (hasReadContactsPermissions) {
                        log("Permission denied");
                        filteredContactsPhone.clear();
                        emptyImageView.setVisibility(View.VISIBLE);
                        emptyTextView.setVisibility(View.VISIBLE);
                        emptyTextView.setText(R.string.no_contacts_permissions);

                        progressBar.setVisibility(View.GONE);
                    }
                }
                break;
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

    public static void log(String message) {
        Util.log("AddContactActivityLollipop", message);
    }
}
