package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
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
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.lollipop.adapters.PhoneContactsLollipopAdapter;
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


public class InviteContactActivityLollipop extends PinActivityLollipop implements View.OnClickListener, RecyclerView.OnItemTouchListener, TextWatcher, TextView.OnEditorActionListener, MegaRequestListenerInterface {
    
    public static final int SCAN_QR_FOR_ADD_CONTACTS = 1111;
    
    private DisplayMetrics outMetrics;
    private MegaApplication app;
    private MegaApiAndroid megaApi;
    private DatabaseHandler dbH;
    private Toolbar tB;
    private ActionBar aB;
    private RelativeLayout containerContacts, scanQRButton, container;
    private RecyclerView recyclerViewList;
    private LinearLayoutManager linearLayoutManager;
    private ImageView emptyImageView;
    private TextView emptyTextView, emptySubTextView;
    private ProgressBar progressBar;
    private String inputString;
    private PhoneContactsLollipopAdapter adapterPhone;
    private ArrayList<PhoneContactInfo> phoneContacts, addedContactsPhone, filteredContactsPhone;
    private ArrayList<MegaUser> contactsMEGA;
    private FilterContactsTask filterContactsTask;
    private FastScroller fastScroller;
    private FloatingActionButton fabButton;
    private RelativeLayout typeContactLayout;
    private EditText typeContactEditText;
    
    private void visibilityFastScroller() {
        fastScroller.setRecyclerView(recyclerViewList);
        if (adapterPhone == null) {
            fastScroller.setVisibility(View.GONE);
        } else {
            if (adapterPhone.getItemCount() < 20) {
                fastScroller.setVisibility(View.GONE);
            } else {
                fastScroller.setVisibility(View.VISIBLE);
            }
        }
    }
    
    private void setPhoneAdapterContacts(ArrayList<PhoneContactInfo> contacts) {
//        if (filteredContactsPhone != null) {
//            if (filteredContactsPhone.size() == 0) {
//                showEmptyTextView();
//            } else {
//                emptyTextView.setText(R.string.contacts_list_empty_text_loading);
//            }
//        }
//
//        if (adapterPhone == null) {
//            adapterPhone = new PhoneContactsLollipopAdapter(this,contacts);
//
//            recyclerViewList.setAdapter(adapterPhone);
//
//            adapterPhone.SetOnItemClickListener(new PhoneContactsLollipopAdapter.OnItemClickListener() {
//
//                @Override
//                public void onItemClick(View view,int position) {
//                    itemClick(view,position);
//                }
//            });
//        } else {
//            adapterPhone.setContacts(contacts);
//        }
//
//        if (adapterPhone != null) {
//            if (adapterPhone.getItemCount() == 0) {
//                setEmptyStateVisibility(true);
//            } else {
//                setEmptyStateVisibility(false);
//            }
//        }
    }
    
    private void showEmptyTextView() {
        String textToShow = String.format(getString(R.string.context_empty_contacts)).toUpperCase();
        try {
            textToShow = textToShow.replace("[A]","<font color=\'#000000\'>");
            textToShow = textToShow.replace("[/A]","</font>");
            textToShow = textToShow.replace("[B]","<font color=\'#7a7a7a\'>");
            textToShow = textToShow.replace("[/B]","</font>");
        } catch (Exception e) {
        }
        Spanned result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(textToShow);
        }
        emptyTextView.setText(result);
    }
    
    private void itemClick(View view,int position) {
        log("on item click");
        inputString = typeContactEditText.getText().toString();
        if (adapterPhone == null) {
            return;
        }
        
        final PhoneContactInfo contact = adapterPhone.getItem(position);
        if (contact == null) {
            return;
        }

//        if (inputString != null && !inputString.equals("")) {
//            for (int i = 0;i < queryContactsPhone.size();i++) {
//                if (queryContactsPhone.get(i).getEmail().equals(contact.getEmail())) {
//                    filteredContactsPhone.remove(queryContactsPhone.get(i));
//                    queryContactsPhone.remove(i);
//                    adapterPhone.setContacts(queryContactsPhone);
//                }
//            }
//        } else {
//            for (int i = 0;i < filteredContactsPhone.size();i++) {
//                if (filteredContactsPhone.get(i).getEmail().equals(contact.getEmail())) {
//                    filteredContactsPhone.remove(i);
//                    adapterPhone.setContacts(filteredContactsPhone);
//                    break;
//                }
//            }
//        }
    }
    
    private void setEmptyStateVisibility(boolean visible) {
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
    
    public static void log(String message) {
        Util.log("AddContactActivityLollipop",message);
    }
    
    private String getMegaContactMail(MegaContactAdapter contact) {
        String mail = null;
        if (contact != null) {
            if (contact.getMegaUser() != null && contact.getMegaUser().getEmail() != null) {
                mail = contact.getMegaUser().getEmail();
            } else if (contact.getMegaContactDB() != null && contact.getMegaContactDB().getMail() != null) {
                mail = contact.getMegaContactDB().getMail();
            }
        }
        return mail;
    }
    
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
                    null,SORT_ORDER);
            
            while (c.moveToNext()) {
                long id = c.getLong(c.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                String name = c.getString(c.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                String emailAddress = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                log("ID: " + id + "___ NAME: " + name + "____ EMAIL: " + emailAddress);
                
                if ((!emailAddress.equalsIgnoreCase("")) && (emailAddress.contains("@")) && (!emailAddress.contains("s.whatsapp.net"))) {
                    if (inputString == "" || inputString == null) {
                        log("VALID Contact: " + name + " ---> " + emailAddress);
                        PhoneContactInfo contactPhone = new PhoneContactInfo(id,name,emailAddress,null);
                        contactList.add(contactPhone);
                    } else if (!inputString.isEmpty() && (name.toUpperCase().contains(inputString.toUpperCase()) || emailAddress.toUpperCase().contains(inputString.toUpperCase()))) {
                        log("VALID Contact: " + name + " ---> " + emailAddress + " inputString: " + inputString);
                        PhoneContactInfo contactPhone = new PhoneContactInfo(id,name,emailAddress,null);
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
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected");
        
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                onBackPressed();
                break;
            }
            case R.id.action_scan_qr: {
                initScanQR();
                break;
            }
            case R.id.action_send_invitation: {
                Util.hideKeyboard(this,0);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        retryConnectionsAndSignalPresence();
        super.onBackPressed();
    }
    
    private void initScanQR() {
        Intent intent = new Intent(this,QRCodeActivity.class);
        intent.putExtra("inviteContacts",true);
        startActivityForResult(intent,SCAN_QR_FOR_ADD_CONTACTS);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");
        
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(ContextCompat.getColor(this,R.color.dark_primary_color));
        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        app = (MegaApplication)getApplication();
        megaApi = app.getMegaApi();
        if (megaApi == null || megaApi.getRootNode() == null) {
            log("Refresh session - sdk");
            Intent intent = new Intent(this,LoginActivityLollipop.class);
            intent.putExtra("visibleFragment",Constants.LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }
        
        dbH = DatabaseHandler.getDbHandler(this);
        setContentView(R.layout.activity_invite_contact);
        
        phoneContacts = new ArrayList<>();
        addedContactsPhone = new ArrayList<>();
        filteredContactsPhone = new ArrayList<>();
        contactsMEGA = megaApi.getContacts();//todo
        
        tB = findViewById(R.id.add_contact_toolbar);
        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setTitle("");
        aB.setSubtitle("");
        setTitleAB();
        
        container = findViewById(R.id.relative_container_invite_contact);
        
        fabButton = findViewById(R.id.fab_button_next);
        fabButton.setOnClickListener(this);
        
        typeContactLayout = findViewById(R.id.layout_type_mail);
        typeContactEditText = findViewById(R.id.type_mail_edittext);
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Util.px2dp(40,outMetrics));
            typeContactLayout.setLayoutParams(params1);
        }
        typeContactEditText.addTextChangedListener(this);
        typeContactEditText.setOnEditorActionListener(this);
        typeContactEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        typeContactEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v,boolean hasFocus) {
                if (hasFocus) {
                    if (filterContactsTask != null && filterContactsTask.getStatus() == AsyncTask.Status.RUNNING) {
                        filterContactsTask.cancel(true);
                    }
                    filterContactsTask = new FilterContactsTask();
                    filterContactsTask.execute();
                }
            }
        });
        scanQRButton = findViewById(R.id.layout_scan_qr);
        scanQRButton.setOnClickListener(this);
        
        linearLayoutManager = new LinearLayoutManager(this);
        recyclerViewList = findViewById(R.id.add_contact_list);
        recyclerViewList.setClipToPadding(false);
        recyclerViewList.setHasFixedSize(true);
        recyclerViewList.addOnItemTouchListener(this);
        recyclerViewList.setItemAnimator(new DefaultItemAnimator());
        recyclerViewList.setLayoutManager(linearLayoutManager);
        recyclerViewList.addItemDecoration(new SimpleDividerItemDecoration(this,outMetrics));
        adapterPhone = new PhoneContactsLollipopAdapter(this,phoneContacts);
        recyclerViewList.setAdapter(adapterPhone);
        containerContacts = findViewById(R.id.container_list_contacts);
        setRecyclersVisibility();
        
        fastScroller = findViewById(R.id.fastScroller);
        fastScroller.setRecyclerView(recyclerViewList);
        
        emptyImageView = findViewById(R.id.add_contact_list_empty_image);
        emptyTextView = findViewById(R.id.add_contact_list_empty_text);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
        } else {
            emptyImageView.setImageResource(R.drawable.contacts_empty_landscape);
        }
        emptyTextView.setText(R.string.contacts_list_empty_text_loading_share);
        emptySubTextView = findViewById(R.id.add_contact_list_empty_subtext);
        
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LinearLayout.LayoutParams params1 = (LinearLayout.LayoutParams)emptySubTextView.getLayoutParams();
            params1.setMargins(Util.px2dp(34,outMetrics),0,Util.px2dp(34,outMetrics),0);
            emptyTextView.setLayoutParams(params1);
        }
        
        progressBar = findViewById(R.id.add_contact_progress_bar);
        
        //Get MEGA contacts and phone contacts: first name, last name and email
//        if (savedInstanceState != null) {
//            addedContactsPhone = savedInstanceState.getParcelableArrayList("addedContactsPhone");
//            filteredContactsPhone = savedInstanceState.getParcelableArrayList("filteredContactsPhone");
//            phoneContacts = savedInstanceState.getParcelableArrayList("phoneContacts");
//        }
        
        queryIfHasReadContactsPermissions();
//        if (phoneContacts.size() == 0) {
//        } else {
//            setPhoneAdapterContacts(filteredContactsPhone);
//        }
    }
    
    private void setTitleAB() {
        log("setTitleAB");
        if (aB != null) {
            aB.setTitle(getString(R.string.invite_contacts).toUpperCase());
            if (addedContactsPhone.size() > 0) {
                aB.setSubtitle(addedContactsPhone.size() + " " + getResources().getQuantityString(R.plurals.general_num_contacts,addedContactsPhone.size()));
            } else {
                aB.setSubtitle(null);
            }
        }
    }
    
    private void setRecyclersVisibility() {
        if (phoneContacts.size() > 0) {
            containerContacts.setVisibility(View.VISIBLE);
        } else {
            containerContacts.setVisibility(View.GONE);
        }
    }
    
    private void queryIfHasReadContactsPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasReadContactsPermission = (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);
            if (hasReadContactsPermission) {
                prepareToGetPhoneContacts();
            } else {
                ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.READ_CONTACTS},Constants.REQUEST_READ_CONTACTS);
            }
        } else {
            prepareToGetPhoneContacts();
        }
    }
    
    private void prepareToGetPhoneContacts() {
        setEmptyStateVisibility(true);
        progressBar.setVisibility(View.VISIBLE);
        new GetContactsTask().execute();
    }
    
    @Override
    public void beforeTextChanged(CharSequence s,int start,int count,int after) {
        refreshKeyboard();
    }
    
    private void refreshKeyboard() {
        
        String s = typeContactEditText.getText().toString();
        int imeOptions = typeContactEditText.getImeOptions();
        typeContactEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        
        int imeOptionsNew = typeContactEditText.getImeOptions();
        if (imeOptions != imeOptionsNew) {
            View view = getCurrentFocus();
            if (view != null) {
                InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.restartInput(view);
            }
        }
    }
    
    @Override
    public void onTextChanged(CharSequence s,int start,int before,int count) {
        log("onTextChanged: " + s.toString() + "_ " + start + "__" + before + "__" + count);
        if (s != null) {
            if (s.length() > 0) {
                String temp = s.toString();
                char last = s.charAt(s.length() - 1);
                if (last == ' ') {
                    boolean isValid = isValidEmail(temp.trim());
                    if (isValid) {
                        typeContactEditText.getText().clear();
                    }
                    if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        Util.hideKeyboard(this,0);
                    }
                } else {
                    log("Last character is: " + last);
                }
            }
        }
        
        if (filterContactsTask != null && filterContactsTask.getStatus() == AsyncTask.Status.RUNNING) {
            filterContactsTask.cancel(true);
        }
        filterContactsTask = new FilterContactsTask();
        filterContactsTask.execute();
        refreshKeyboard();
    }
    
    private boolean isValidEmail(CharSequence target) {
        if (target == null) {
            return false;
        } else {
            log("isValid");
            return Constants.EMAIL_ADDRESS.matcher(target).matches();
        }
    }
    
    @Override
    public void afterTextChanged(Editable editable) {
        refreshKeyboard();
    }
    
    @Override
    public boolean onEditorAction(TextView v,int actionId,KeyEvent event) {
        refreshKeyboard();
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            String s = v.getText().toString();
            log("s: " + s);
            if (s.isEmpty() || s.equals("null") || s.equals("")) {
                Util.hideKeyboard(this,0);
            } else {
                boolean isValid = isValidEmail(s.trim());
                if (isValid) {
                    typeContactEditText.getText().clear();
                    Util.hideKeyboard(this,0);
                }
                if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    Util.hideKeyboard(this,0);
                }
                if (filterContactsTask != null && filterContactsTask.getStatus() == AsyncTask.Status.RUNNING) {
                    filterContactsTask.cancel(true);
                }
                filterContactsTask = new FilterContactsTask();
                filterContactsTask.execute();
            }
            return true;
        }
        if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_SEND)) {
            if (addedContactsPhone.isEmpty() || addedContactsPhone == null) {
                Util.hideKeyboard(this,0);
            } else {
                inviteContacts(addedContactsPhone);
            }
            return true;
        }
        return false;
    }
    
    private void inviteContacts(ArrayList<PhoneContactInfo> addedContacts) {
        log("inviteContacts");
        
        String contactEmail;
        ArrayList<String> contactsSelected = new ArrayList<>();
        if (addedContacts != null) {
            for (int i = 0;i < addedContacts.size();i++) {
                contactEmail = addedContacts.get(i).getEmail();
                if (contactEmail != null) {
                    contactsSelected.add(contactEmail);
                }
            }
        }
        
        Intent intent = new Intent();
        for (int i = 0;i < contactsSelected.size();i++) {
            log("setResultContacts: " + contactsSelected.get(i));
        }
        
        setResult(RESULT_OK,intent);
        Util.hideKeyboard(this,0);
        finish();
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
                inviteContacts(addedContactsPhone);
                Util.hideKeyboard(this,0);
                break;
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent intent) {
        log("onActivityResult");
        super.onActivityResult(requestCode,resultCode,intent);
        if (requestCode == SCAN_QR_FOR_ADD_CONTACTS && resultCode == Activity.RESULT_OK && intent != null) {
            String mail = intent.getStringExtra("mail");
        } else if (requestCode == Constants.REQUEST_INVITE_CONTACT_FROM_DEVICE && resultCode == RESULT_OK) {
            log("onActivityResult REQUEST_INVITE_CONTACT_FROM_DEVICE OK");
            
            if (intent == null) {
                log("Return.....");
                return;
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults) {
        log("onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
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
    public boolean onInterceptTouchEvent(RecyclerView rv,MotionEvent e) {
        return false;
    }
    
    @Override
    public void onTouchEvent(RecyclerView rv,MotionEvent e) {
        
    }
    
    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        
    }
    
    @Override
    public void onRequestStart(MegaApiJava api,MegaRequest request) {
        log("onRequestStart: " + request.getRequestString());
    }
    
    @Override
    public void onRequestUpdate(MegaApiJava api,MegaRequest request) {
        
    }
    
    @Override
    public void onRequestFinish(MegaApiJava api,MegaRequest request,MegaError e) {
        if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT) {
            log("MegaRequest.TYPE_INVITE_CONTACT finished: " + request.getNumber());
            
            if (request.getNumber() == MegaContactRequest.INVITE_ACTION_REMIND) {
                showSnackbar(getString(R.string.context_contact_invitation_resent));
            } else {
                if (e.getErrorCode() == MegaError.API_OK) {
                    log("OK INVITE CONTACT: " + request.getEmail());
                    if (request.getNumber() == MegaContactRequest.INVITE_ACTION_ADD) {
                        showSnackbar(getString(R.string.context_contact_request_sent,request.getEmail()));
                    } else if (request.getNumber() == MegaContactRequest.INVITE_ACTION_DELETE) {
                        showSnackbar(getString(R.string.context_contact_invitation_deleted));
                    }
                } else {
                    log("Code: " + e.getErrorString());
                    if (e.getErrorCode() == MegaError.API_EEXIST) {
                        showSnackbar(getString(R.string.context_contact_already_exists,request.getEmail()));
                    } else if (request.getNumber() == MegaContactRequest.INVITE_ACTION_ADD && e.getErrorCode() == MegaError.API_EARGS) {
                        showSnackbar(getString(R.string.error_own_email_as_contact));
                    } else {
                        showSnackbar(getString(R.string.general_error));
                    }
                    log("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
                }
            }
        }
    }
    
    private void showSnackbar(String message) {
        Util.hideKeyboard(this,0);
        showSnackbar(container,message);
    }
    
    @Override
    public void onRequestTemporaryError(MegaApiJava api,MegaRequest request,MegaError e) {
    
    }
    
    private ArrayList<PhoneContactInfo> megaContactToPhoneContact(ArrayList<MegaUser> megaContacts) {
        ArrayList<PhoneContactInfo> result = new ArrayList<>();
        for (MegaUser user : megaContacts) {
            long id = 1;//user.getHandle();
            String name = "missing now";
            String email = user.getEmail();
            String phoneNumber = "missing now";
            
            PhoneContactInfo info = new PhoneContactInfo(id,name,email,phoneNumber);
            result.add(info);
        }
        
        return result;
    }
    
    private class GetContactsTask extends AsyncTask<Void, Void, Void> {
        
        @Override
        protected Void doInBackground(Void... voids) {
            
            phoneContacts.clear();
            filteredContactsPhone.clear();
            addedContactsPhone.clear();
            ArrayList<PhoneContactInfo> contactList = getPhoneContacts();
            phoneContacts.addAll(contactList);
//            for (int i = 0;i < phoneContacts.size();i++) {
//                filteredContactsPhone.add(phoneContacts.get(i));
//            }
//
//            boolean found;
//            PhoneContactInfo contactPhone;
//            MegaContactAdapter contactMEGA;
//
//            if (filteredContactsPhone != null && !filteredContactsPhone.isEmpty()) {
//                for (int i = 0;i < filteredContactsPhone.size();i++) {
//                    found = false;
//                    contactPhone = filteredContactsPhone.get(i);
//                    if (found) {
//                        filteredContactsPhone.remove(contactPhone);
//                        i--;
//                    }
//                }
//            }
            
            return null;
        }
        
        @Override
        protected void onPostExecute(Void avoid) {
            log("onPostExecute GetContactsTask");
            InviteContactActivityLollipop.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                
                }
            });
            progressBar.setVisibility(View.GONE);
            phoneContacts.clear();
            phoneContacts.addAll(megaContactToPhoneContact(contactsMEGA));
            adapterPhone.notifyDataSetChanged();
//            adapterPhone = new PhoneContactsLollipopAdapter(InviteContactActivityLollipop.this,phoneContacts);
//            recyclerViewList.setAdapter(adapterPhone);
//            if (filterContactsTask != null && filterContactsTask.getStatus() == Status.RUNNING) {
//                filterContactsTask.cancel(true);
//            }
//            filterContactsTask = new FilterContactsTask();
//            filterContactsTask.execute();
//            if (searchExpand) {
//
//            } else {
//                setPhoneAdapterContacts(filteredContactsPhone);
//                setTitleAB();
            setRecyclersVisibility();
            visibilityFastScroller();
//            }
        }
    }
    
    private class FilterContactsTask extends AsyncTask<Void, Void, Void> {
        
        private String query;
        
        private FilterContactsTask(String inputString) {
            query = inputString;
        }
        
        private FilterContactsTask() {
            query = null;
        }
        
        @Override
        protected Void doInBackground(Void... voids) {
            if (query != null && !query.equals("")) {
                PhoneContactInfo contactPhone;
                
                for (int i = 0;i < filteredContactsPhone.size();i++) {
                    contactPhone = filteredContactsPhone.get(i);
                    if (contactPhone.getEmail().toLowerCase().contains(query.toLowerCase())
                            || contactPhone.getName().toLowerCase().contains(query.toLowerCase())) {
                    }
                }
            }
            
            return null;
        }
        
        @Override
        protected void onPostExecute(Void voids) {
            log("onPostExecute FilterContactsTask");
           // setPhoneAdapterContacts(filteredContactsPhone);
            visibilityFastScroller();
        }
    }
}
