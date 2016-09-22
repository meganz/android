package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.components.tokenautocomplete.TokenCompleteTextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContact;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.tokenautocomplete.ContactInfo;
import mega.privacy.android.app.components.tokenautocomplete.ContactsCompletionView;
import mega.privacy.android.app.components.MegaLinearLayoutManager;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaUser;

public class AddContactActivityLollipop extends PinActivityLollipop implements TokenCompleteTextView.TokenListener<ContactInfo>, View.OnClickListener, RecyclerView.OnItemTouchListener {

    public static String ACTION_PICK_CONTACT_SHARE_FOLDER = "ACTION_PICK_CONTACT_SHARE_FOLDER";
    public static String ACTION_PICK_CONTACT_SEND_FILE = "ACTION_PICK_CONTACT_SEND_FILE";

    DisplayMetrics outMetrics;

    MegaApplication app;
    MegaApiAndroid megaApi;
    DatabaseHandler dbH = null;
    int contactType = 0;
    int multipleSelectIntent;
    int sendToInbox;
    long nodeHandle = -1;
    long[] nodeHandles;

    AddContactActivityLollipop addContactActivityLollipop;

    Toolbar tB;
    ActionBar aB;

    ImageView sendButton;
    ImageView backButton;
    ContactsCompletionView completionView;
    RecyclerView recyclerView;
    ImageView emptyImageView;
    TextView emptyTextView;
    ProgressBar progressBar;

    ContactInfo[] people;
    MegaContactsLollipopAdapter adapterMEGA;
    GestureDetectorCompat detector;

    PhoneContactsLollipopAdapter adapterPhone;

    ArrayList<PhoneContactInfo> phoneContacts;
    ArrayList<PhoneContactInfo> filteredContactsPhone = new ArrayList<PhoneContactInfo>();

    ArrayList<MegaUser> contactsMEGA;
    ArrayList<MegaUser> visibleContactsMEGA = new ArrayList<MegaUser>();
    ArrayList<MegaUser> filteredContactsMEGA = new ArrayList<MegaUser>();

    boolean itemClickPressed = false;

    public static String EXTRA_MEGA_CONTACTS = "mega_contacts";
    public static String EXTRA_CONTACTS = "extra_contacts";
    public static String EXTRA_NODE_HANDLE = "node_handle";
    public static String EXTRA_EMAIL = "extra_email";
    public static String EXTRA_PHONE = "extra_phone";

    public class RecyclerViewOnGestureListener extends GestureDetector.SimpleOnGestureListener {

        public void onLongPress(MotionEvent e) {
//            View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
//            int position = recyclerView.getChildPosition(view);
//
//            // handle long press
//            if (!adapter.isMultipleSelect()){
//                adapter.setMultipleSelect(true);
//
//                actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
//
//                itemClick(position);
//            }
            super.onLongPress(e);
        }
    }

    private class PhoneContactsTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {

            phoneContacts = getPhoneContacts();

            return true;
        }

        @Override
        protected void onPostExecute(Boolean bool) {

            progressBar.setVisibility(View.GONE);

            List<ContactInfo> tokens = completionView.getObjects();
            if (tokens != null) {
                log("tokens.size() = " + tokens.size());

                if (tokens.size() == 0) {
                    for (int i = 0; i < phoneContacts.size(); i++) {
                        log("filteredContacts.add(visibleContacts.get(" + i + ") = " + phoneContacts.get(i).getEmail());
                        filteredContactsPhone.add(phoneContacts.get(i));
                    }
                } else {
                    for (int i = 0; i < phoneContacts.size(); i++) {
                        boolean found = false;
                        for (int j = 0; j < tokens.size(); j++) {
                            log("tokens.get(" + j + ").getEmail() = " + tokens.get(j).getEmail());
                            log("visibleContacts.get(" + i + ").getEmail() = " + phoneContacts.get(i).getEmail());
                            if (tokens.get(j).getEmail().compareTo(phoneContacts.get(i).getEmail()) == 0) {
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

            if (adapterPhone == null){
                adapterPhone = new PhoneContactsLollipopAdapter(addContactActivityLollipop, filteredContactsPhone);

                recyclerView.setAdapter(adapterPhone);

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

            if (adapterPhone.getItemCount() == 0){
                recyclerView.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);
            }
            else{
                recyclerView.setVisibility(View.VISIBLE);
                emptyImageView.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.GONE);
            }

            String text = completionView.getText().toString();
            text.replace(",", "");
            text.replace(" ", "");
            if (text.compareTo("") != 0){
                onTextChanged(text, -1, -1, -1);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");

        super.onCreate(savedInstanceState);

        if (getIntent() != null){
            contactType = getIntent().getIntExtra("contactType", Constants.CONTACT_TYPE_MEGA);

            if (contactType == Constants.CONTACT_TYPE_MEGA){
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
        float density  = getResources().getDisplayMetrics().density;

        app = (MegaApplication)getApplication();
        megaApi = app.getMegaApi();

        addContactActivityLollipop = this;

        log("retryPendingConnections()");
        if (megaApi != null){
            log("---------retryPendingConnections");
            megaApi.retryPendingConnections();
        }

        dbH = DatabaseHandler.getDbHandler(this);

        setContentView(R.layout.activity_add_contact);

//        people = new ContactInfo[]{
//                new ContactInfo("Marshall Weir", "marshall@example.com"),
//                new ContactInfo("Margaret Smith", "margaret@example.com"),
//                new ContactInfo("Max Jordan", "max@example.com"),
//                new ContactInfo("Meg Peterson", "meg@example.com"),
//                new ContactInfo("Amanda Johnson", "amanda@example.com"),
//                new ContactInfo("Terry Anderson", "terry@example.com"),
//                new ContactInfo("Siniša Damianos Pilirani Karoline Slootmaekers",
//                        "siniša_damianos_pilirani_karoline_slootmaekers@example.com")
//        };

        completionView = (ContactsCompletionView)findViewById(R.id.add_contact_chips);
//        adapter = new FilteredArrayAdapter<Person>(this, R.layout.person_layout, people) {
//            @Override
//            public View getView(int position, View convertView, ViewGroup parent) {
//                if (convertView == null) {
//
//                    LayoutInflater l = (LayoutInflater)getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
//                    convertView = l.inflate(R.layout.person_layout, parent, false);
//                }
//
//                Person p = getItem(position);
//                ((TextView)convertView.findViewById(R.id.name)).setText(p.getName());
//                ((TextView)convertView.findViewById(R.id.email)).setText(p.getEmail());
//
//                return convertView;
//            }
//
//            @Override
//            protected boolean keepObject(Person person, String mask) {
//                mask = mask.toLowerCase();
//                return person.getName().toLowerCase().startsWith(mask) || person.getEmail().toLowerCase().startsWith(mask);
//            }
//        };
//        completionView.setAdapter(adapter);

        completionView.setTokenListener(this);
        completionView.setTokenClickStyle(TokenCompleteTextView.TokenClickStyle.Select);
        completionView.setOnTextChangeListener(this);
//
//
//        completionView.addTextChangedListener(new TextWatcher() {
//
//            String text = "";
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                log("onTextChanged: " + s.toString() + "_ " + start + "__" + before + "__" + count);
//
//                String [] spl = s.toString().split(",,");
//                for (int i=0;i<spl.length;i++){
//                    log("SS: _" + spl[i] + "_");
//                }
//
//                log("INDEXOF: " + s.toString().indexOf(",, "));
//                text = "" + s.toString();
//                int ind = text.indexOf(",, ");
//                while(ind >= 0){
//                    log("IND: " + ind);
//                    if (ind >= 0){
//                        text = text.substring(ind+3, text.length());
//                    }
//                    ind = text.indexOf(",, ");
//                }
//
//                log("text: _" + text + "_");
//
//                ArrayList<MegaUser> filteredContactsAfterText = new ArrayList<MegaUser>();
//                if (filteredContacts != null){
//                    for (int i=0;i<filteredContacts.size();i++){
//                        if (filteredContacts.get(i).getEmail().startsWith(text.trim()) == true){
//                            filteredContactsAfterText.add(filteredContacts.get(i));
//                        }
//                        else{
//                            MegaContact contactDB = dbH.findContactByHandle(String.valueOf(filteredContacts.get(i).getHandle()));
//                            if(contactDB!=null){
//                                String name = contactDB.getName();
//                                String lastName = contactDB.getLastName();
//                                if (name.startsWith(text.trim()) == true){
//                                    filteredContactsAfterText.add(filteredContacts.get(i));
//                                }
//                                else if (lastName.startsWith(text.trim()) == true){
//                                    filteredContactsAfterText.add(filteredContacts.get(i));
//                                }
//                            }
//                        }
//                    }
//
//                    if (adapter != null){
//                        adapter.setContacts(filteredContactsAfterText);
//                        adapter.setAdapterType(MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
//
//                        if (adapter.getItemCount() == 0){
//
//                            emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
//                            emptyTextView.setText(R.string.contacts_list_empty_text);
//                            recyclerView.setVisibility(View.GONE);
//                            emptyImageView.setVisibility(View.VISIBLE);
//                            emptyTextView.setVisibility(View.VISIBLE);
//                        }
//                        else{
//                            recyclerView.setVisibility(View.VISIBLE);
//                            emptyImageView.setVisibility(View.GONE);
//                            emptyTextView.setVisibility(View.GONE);
//                        }
//                    }
//                }
//            }
//
//            @Override
//            public void afterTextChanged(Editable textEd) {
//                log("afterTextChanged: " + textEd);
//            }
//
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//                log("beforeTextChanged: " + s.toString() + "_" + start + "__" + after + "__" + count);
//            }
//        });

        sendButton = (ImageView) findViewById(R.id.add_contact_send);
        if (completionView.getObjects().size() > 0) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) sendButton.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_BOTTOM, completionView.getId());
            params.setMargins(0, Util.scaleWidthPx(24, outMetrics), 0, 0);
        }
        sendButton.setOnClickListener(this);

        backButton = (ImageView) findViewById(R.id.add_contact_back);
        backButton.setOnClickListener(this);

        detector = new GestureDetectorCompat(this, new RecyclerViewOnGestureListener());
        recyclerView = (RecyclerView) findViewById(R.id.add_contact_list);
        recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
        recyclerView.setClipToPadding(false);
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
        recyclerView.setHasFixedSize(true);
        MegaLinearLayoutManager linearLayoutManager = new MegaLinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addOnItemTouchListener(this);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        emptyImageView = (ImageView) findViewById(R.id.add_contact_list_empty_image);
        emptyTextView = (TextView) findViewById(R.id.add_contact_list_empty_text);

        emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
        emptyTextView.setText(R.string.contacts_list_empty_text_loading);

        progressBar = (ProgressBar) findViewById(R.id.add_contact_progress_bar);

        //Get MEGA contacts and phone contacts: first name, last name and email
        if (contactType == Constants.CONTACT_TYPE_MEGA) {
            contactsMEGA = megaApi.getContacts();
            visibleContactsMEGA.clear();
            filteredContactsMEGA.clear();

            for (int i = 0; i < contactsMEGA.size(); i++) {

                log("contact: " + contactsMEGA.get(i).getEmail() + "_" + contactsMEGA.get(i).getVisibility());
                if (contactsMEGA.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                    visibleContactsMEGA.add(contactsMEGA.get(i));
                }
            }

            List<ContactInfo> tokens = completionView.getObjects();
            if (tokens != null) {
                log("tokens.size() = " + tokens.size());

                if (tokens.size() == 0) {
                    for (int i = 0; i < visibleContactsMEGA.size(); i++) {
                        log("filteredContacts.add(visibleContacts.get(" + i + ") = " + visibleContactsMEGA.get(i).getEmail());
                        filteredContactsMEGA.add(visibleContactsMEGA.get(i));
                    }
                } else {
                    for (int i = 0; i < visibleContactsMEGA.size(); i++) {
                        boolean found = false;
                        for (int j = 0; j < tokens.size(); j++) {
                            log("tokens.get(" + j + ").getEmail() = " + tokens.get(j).getEmail());
                            log("visibleContacts.get(" + i + ").getEmail() = " + visibleContactsMEGA.get(i).getEmail());
                            if (tokens.get(j).getEmail().compareTo(visibleContactsMEGA.get(i).getEmail()) == 0) {
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            log("!found -> filteredContacts.add(visibleContacts.get(" + i + ") = " + visibleContactsMEGA.get(i).getEmail());
                            filteredContactsMEGA.add(visibleContactsMEGA.get(i));
                        }
                    }
                }
            }

            if (adapterMEGA == null) {
                adapterMEGA = new MegaContactsLollipopAdapter(this, null, filteredContactsMEGA, emptyImageView, emptyTextView, recyclerView, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
            } else {
                adapterMEGA.setContacts(filteredContactsMEGA);
                adapterMEGA.setAdapterType(MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
            }

            adapterMEGA.setPositionClicked(-1);
            recyclerView.setAdapter(adapterMEGA);

            if (adapterMEGA.getItemCount() == 0) {

                emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
                emptyTextView.setText(R.string.contacts_list_empty_text);
                recyclerView.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyImageView.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.GONE);
            }
        }
        else {
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
    }

    @SuppressLint("InlinedApi")
    //Get the contacts explicitly added
    private ArrayList<PhoneContactInfo> getPhoneContacts() {
        log("getPhoneContacts");
        ArrayList<PhoneContactInfo> contactList = new ArrayList<PhoneContactInfo>();

        try {
            ContentResolver cr = getBaseContext().getContentResolver();
            String SORT_ORDER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? ContactsContract.Contacts.SORT_KEY_PRIMARY : ContactsContract.Contacts.DISPLAY_NAME;
            String filter = ContactsContract.CommonDataKinds.Email.DATA + " NOT LIKE ''  AND " + ContactsContract.Contacts.IN_VISIBLE_GROUP + "=1";
            Cursor c = cr.query(
                    ContactsContract.Data.CONTENT_URI,
                    null,filter,
                    null, SORT_ORDER);

            while (c.moveToNext()){
                long id = c.getLong(c.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                String name = c.getString(c.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                String emailAddress = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                log("ID: " + id + "___ NAME: " + name + "____ EMAIL: " + emailAddress);

                if ((!emailAddress.equalsIgnoreCase("")) && (emailAddress.contains("@")) && (!emailAddress.contains("s.whatsapp.net"))) {
                    log("VALID Contact: "+ name + " ---> "+ emailAddress);
                    PhoneContactInfo contactPhone = new PhoneContactInfo(id, name, emailAddress, null);
                    contactList.add(contactPhone);
                }
            }

            c.close();

            log("contactList.size() = " + contactList.size());

            return contactList;

        } catch (Exception e) { log ("Exception: " + e.getMessage()); }

        return null;
    }

    public void onTextChanged(CharSequence s, int start, int before, int count){
        log("onTextChanged: " + s.toString() + "_ " + start + "__" + before + "__" + count);

//        String [] spl = s.toString().split(",,");
//        for (int i=0;i<spl.length;i++){
//            log("SS: _" + spl[i] + "_");
//        }
//
//        log("INDEXOF: " + s.toString().indexOf(",, "));
//        String text = "" + s.toString();
//        int ind = text.indexOf(",, ");
//        while(ind >= 0){
//            log("IND: " + ind);
//            if (ind >= 0){
//                text = text.substring(ind+3, text.length());
//            }
//            ind = text.indexOf(",, ");
//        }
//
//        log("text: _" + text + "_");

        String text = "";
        for (int i=0;i<s.toString().length();i++){
            if (s.toString().charAt(i) != ','){
                text = text + s.toString().charAt(i);
            }
        }

        log("text: _" + text + "_");

        if (contactType == Constants.CONTACT_TYPE_MEGA){
            ArrayList<MegaUser> filteredContactsAfterText = new ArrayList<MegaUser>();
            if (filteredContactsMEGA != null) {
                for (int i = 0; i < filteredContactsMEGA.size(); i++) {
                    try {
                        String email = filteredContactsMEGA.get(i).getEmail();
                        String emailPart = "";

                        if (email != null){
                            if (email.length() >= text.trim().length()){
                                emailPart = email.substring(0, text.trim().length());
                            }
                        }

                        Collator collator = Collator.getInstance(Locale.getDefault());
                        collator.setStrength(Collator.PRIMARY);

                        if (collator.compare(text.trim(), emailPart) == 0){
                            filteredContactsAfterText.add(filteredContactsMEGA.get(i));
                        }
                        else {
                            MegaContact contactDB = dbH.findContactByHandle(String.valueOf(filteredContactsMEGA.get(i).getHandle()));
                            if (contactDB != null) {
                                String name = contactDB.getName();
                                String lastName = contactDB.getLastName();
                                String namePart = "";
                                String lastNamePart = "";
                                String fullNamePart = "";

                                if (name != null){
                                    if (name.length() >= text.trim().length()){
                                        namePart = name.substring(0, text.trim().length());
                                    }
                                }

                                if (lastName != null){
                                    if (lastName.length() >= text.trim().length()){
                                        lastNamePart = lastName.substring(0, text.trim().length());
                                    }
                                }

                                if ((name != null) && (lastName != null)) {
                                    String fullName = name + " " + lastName;
                                    if (fullName != null) {
                                        if (fullName.trim().length() >= text.trim().length()){
                                            fullNamePart = fullName.substring(0, text.trim().length());
                                        }
                                    }
                                }

                                if (collator.compare(text.trim(), namePart) == 0){
                                    filteredContactsAfterText.add(filteredContactsMEGA.get(i));
                                }
                                else if (collator.compare(text.trim(), lastNamePart) == 0){
                                    filteredContactsAfterText.add(filteredContactsMEGA.get(i));
                                }
                                else if (collator.compare(text.trim(), fullNamePart) == 0){
                                    filteredContactsAfterText.add(filteredContactsMEGA.get(i));
                                }
                            }
                        }
                    }
                    catch (Exception e) { log ("Exception: " + e.getMessage()); }
                }

                if (adapterMEGA != null) {
                    adapterMEGA.setContacts(filteredContactsAfterText);
                    adapterMEGA.setAdapterType(MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);

                    if (adapterMEGA.getItemCount() == 0) {

                        emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
                        emptyTextView.setText(R.string.contacts_list_empty_text);
                        recyclerView.setVisibility(View.GONE);
                        emptyImageView.setVisibility(View.VISIBLE);
                        emptyTextView.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyImageView.setVisibility(View.GONE);
                        emptyTextView.setVisibility(View.GONE);
                    }
                }
            }
        }
        else if (contactType == Constants.CONTACT_TYPE_DEVICE){
            ArrayList<PhoneContactInfo> filteredContactsAfterText = new ArrayList<PhoneContactInfo>();
            if (filteredContactsPhone != null) {
                for (int i = 0; i < filteredContactsPhone.size(); i++) {
                    try {
                        String email = filteredContactsPhone.get(i).getEmail();
                        String name = filteredContactsPhone.get(i).getName();
                        String phoneNumber = filteredContactsPhone.get(i).getPhoneNumber();
                        String emailPart = "";
                        String namePart = "";
                        String phoneNumberPart = "";

                        if (email != null){
                            if (email.length() >= text.trim().length()){
                                emailPart = email.substring(0, text.trim().length());
                            }
                        }

                        if (name != null){
                            if (name.trim().length() >= text.trim().length()){
                                namePart = name.substring(0, text.trim().length());
                            }
                        }

                        if (phoneNumber != null){
                            if (phoneNumber.length() >= text.trim().length()){
                                phoneNumberPart = phoneNumber.substring(0, text.trim().length());
                            }
                        }

                        Collator collator = Collator.getInstance(Locale.getDefault());
                        collator.setStrength(Collator.PRIMARY);

                        if (collator.compare(text.trim(), emailPart) == 0){
                            filteredContactsAfterText.add(filteredContactsPhone.get(i));
                        }
                        else if (collator.compare(text.trim(), namePart) == 0){
                            filteredContactsAfterText.add(filteredContactsPhone.get(i));
                        }
                        else if (collator.compare(text.trim(), phoneNumberPart) == 0){
                            filteredContactsAfterText.add(filteredContactsPhone.get(i));
                        }
//                        if (filteredContactsPhone.get(i).getEmail().startsWith(text.trim()) == true) {
//                            filteredContactsAfterText.add(filteredContactsPhone.get(i));
//                        } else if (filteredContactsPhone.get(i).getName().startsWith(text.trim()) == true) {
//                            filteredContactsAfterText.add(filteredContactsPhone.get(i));
//                        } else if (filteredContactsPhone.get(i).getPhoneNumber().startsWith(text.trim()) == true) {
//                            filteredContactsAfterText.add(filteredContactsPhone.get(i));
//                        }
                    }
                    catch (Exception e) { log ("Exception: " + e.getMessage()); }
                }

                if (adapterPhone != null) {
                    adapterPhone.setContacts(filteredContactsAfterText);

                    if (adapterPhone.getItemCount() == 0) {

                        emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
                        emptyTextView.setText(R.string.contacts_list_empty_text);
                        recyclerView.setVisibility(View.GONE);
                        emptyImageView.setVisibility(View.VISIBLE);
                        emptyTextView.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyImageView.setVisibility(View.GONE);
                        emptyTextView.setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    public void itemClick(String email){

        log("itemClick");

        if (contactType == Constants.CONTACT_TYPE_MEGA) {
            ContactInfo c = new ContactInfo();

            for (int i = 0; i < filteredContactsMEGA.size(); i++) {
                if (filteredContactsMEGA.get(i).getEmail().compareTo(email) == 0) {
                    c.setEmail(filteredContactsMEGA.get(i).getEmail());
                    MegaContact contactDB = dbH.findContactByHandle(String.valueOf(filteredContactsMEGA.get(i).getHandle()));
                    if (contactDB != null) {
                        String name = contactDB.getName();
                        String lastName = contactDB.getLastName();
                        c.setName(name + " " + lastName);
                    }

                    filteredContactsMEGA.remove(i);
                    break;
                }
            }

            if (c.getEmail().compareTo("") != 0) {
                log("completionView.getText() =  " + completionView.getText());
                itemClickPressed = true;
                completionView.addObject(c);
            }
        }
    }

    public void itemClick(View view, int position) {
        log("on item click");

        final PhoneContactInfo contact = adapterPhone.getDocumentAt(position);
        if(contact == null) {
            return;
        }

        if (contactType == Constants.CONTACT_TYPE_DEVICE){

            ContactInfo c = new ContactInfo();

            for (int i = 0; i < filteredContactsPhone.size(); i++) {
                if (filteredContactsPhone.get(i).getEmail().compareTo(contact.getEmail()) == 0) {
                    c.setEmail(filteredContactsPhone.get(i).getEmail());
                    c.setName(contact.getName());

                    filteredContactsPhone.remove(i);
                    break;
                }
            }

            if (c.getEmail().compareTo("") != 0) {
                log("completionView.getText() =  " + completionView.getText());
                itemClickPressed = true;
                completionView.addObject(c);
            }
        }

//        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                switch (which){
//                    case DialogInterface.BUTTON_POSITIVE:
//                        inviteContact(contact.getEmail());
//                        break;
//
//                    case DialogInterface.BUTTON_NEGATIVE:
//                        //No button clicked
//                        break;
//                }
//            }
//        };
//
//        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
//        String message= getResources().getString(R.string.confirmation_add_contact,contact.getEmail());
//        builder.setMessage(message).setPositiveButton(R.string.contact_invite, dialogClickListener)
//                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    private void updateTokenConfirmation() {
        log("Current tokens: \n");
        for (Object token: completionView.getObjects()){
            log(token.toString() + "\n");
        }

        if (contactType == Constants.CONTACT_TYPE_MEGA) {
            List<ContactInfo> tokens = completionView.getObjects();
            if ((tokens != null) && (visibleContactsMEGA != null) && (filteredContactsMEGA != null)) {
                filteredContactsMEGA.clear();
                log("tokens.size() = " + tokens.size());

                if (tokens.size() == 0) {
                    for (int i = 0; i < visibleContactsMEGA.size(); i++) {
                        log("filteredContacts.add(visibleContacts.get(" + i + ") = " + visibleContactsMEGA.get(i).getEmail());
                        filteredContactsMEGA.add(visibleContactsMEGA.get(i));
                    }
                } else {
                    for (int i = 0; i < visibleContactsMEGA.size(); i++) {
                        boolean found = false;
                        for (int j = 0; j < tokens.size(); j++) {
                            log("tokens.get(" + j + ").getEmail() = " + tokens.get(j).getEmail());
                            log("visibleContacts.get(" + i + ").getEmail() = " + visibleContactsMEGA.get(i).getEmail());
                            if (tokens.get(j).getEmail().compareTo(visibleContactsMEGA.get(i).getEmail()) == 0) {
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            log("!found -> filteredContacts.add(visibleContacts.get(" + i + ") = " + visibleContactsMEGA.get(i).getEmail());
                            filteredContactsMEGA.add(visibleContactsMEGA.get(i));
                        }
                    }
                }

                if (adapterMEGA != null) {
                    adapterMEGA.setContacts(filteredContactsMEGA);
                    adapterMEGA.setAdapterType(MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
                }
            }

            if (itemClickPressed) {
                for (int i = 0; i < completionView.getText().length(); i++) {
                    if ((completionView.getText().charAt(i) != ',') && (completionView.getText().charAt(i) != ' ')) {
                        log("Delete char: " + i + "__ " + completionView.getText().charAt(i));
                        completionView.getText().delete(i, i + 1);
                        i--;
                    }
                }
                itemClickPressed = false;
            }
        }
        else if (contactType == Constants.CONTACT_TYPE_DEVICE){
            List<ContactInfo> tokens = completionView.getObjects();
            if ((tokens != null) && (phoneContacts != null) && (filteredContactsPhone != null)) {
                filteredContactsPhone.clear();
                log("tokens.size() = " + tokens.size());

                if (tokens.size() == 0) {
                    for (int i = 0; i < phoneContacts.size(); i++) {
                        log("filteredContacts.add(visibleContacts.get(" + i + ") = " + phoneContacts.get(i).getEmail());
                        filteredContactsPhone.add(phoneContacts.get(i));
                    }
                } else {
                    for (int i = 0; i < phoneContacts.size(); i++) {
                        boolean found = false;
                        for (int j = 0; j < tokens.size(); j++) {
                            log("tokens.get(" + j + ").getEmail() = " + tokens.get(j).getEmail());
                            log("visibleContacts.get(" + i + ").getEmail() = " + phoneContacts.get(i).getEmail());
                            if (tokens.get(j).getEmail().compareTo(phoneContacts.get(i).getEmail()) == 0) {
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

                if (adapterPhone != null) {
                    adapterPhone.setContacts(filteredContactsPhone);
                }
            }

            if (itemClickPressed) {
                for (int i = 0; i < completionView.getText().length(); i++) {
                    if ((completionView.getText().charAt(i) != ',') && (completionView.getText().charAt(i) != ' ')) {
                        log("Delete char: " + i + "__ " + completionView.getText().charAt(i));
                        completionView.getText().delete(i, i + 1);
                        i--;
                    }
                }
                itemClickPressed = false;
            }
        }
    }


    @Override
    public void onTokenAdded(ContactInfo token) {
        log("Added: " + token);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) sendButton.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_BOTTOM, completionView.getId());
        params.setMargins(0, Util.scaleWidthPx(24, outMetrics), 0, 0);
        updateTokenConfirmation();
    }

    @Override
    public void onTokenRemoved(ContactInfo token) {
        log("Removed: " + token);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) sendButton.getLayoutParams();
        params.setMargins(0, 0, 0, 0);
        updateTokenConfirmation();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.add_contact_back:{
                onBackPressed();
                break;
            }
            case R.id.add_contact_send:{
//                int l = completionView.getText().length();
//                completionView.getText().delete(l-1, l);

                List<ContactInfo> tokens = completionView.getObjects();
                if (contactType == Constants.CONTACT_TYPE_DEVICE){
                    inviteContacts(tokens);
                }
                else if (contactType == Constants.CONTACT_TYPE_MEGA){
                    setResultContacts(tokens, true);
                }
                break;
            }
        }
    }

    private void setResultContacts(List<ContactInfo> tokens, boolean megaContacts){
        ArrayList<String> contactsSelected = new ArrayList<String>();
        if (tokens != null) {
            for (int i = 0; i < tokens.size(); i++) {
                String contactEmail = tokens.get(i).getEmail();
                if (contactEmail != null){
                    contactsSelected.add(contactEmail);
                }
            }
        }

        Intent intent = new Intent();
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contactsSelected);

        if(contactsSelected!=null){
            for(int i=0; i<contactsSelected.size();i++){
                log("setResultContacts: "+contactsSelected.get(i));
            }
        }

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

        if(sendToInbox==0){
            intent.putExtra("SEND_FILE",0);
        }
        else
        {
            intent.putExtra("SEND_FILE",1);
        }
        intent.putExtra(EXTRA_MEGA_CONTACTS, megaContacts);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void inviteContacts(List<ContactInfo> tokens){
        ArrayList<String> contactsSelected = new ArrayList<String>();
        if (tokens != null) {
            for (int i = 0; i < tokens.size(); i++) {
                String contactEmail = tokens.get(i).getEmail();
                if (contactEmail != null){
                    contactsSelected.add(contactEmail);
                }
            }
        }

        Intent intent = new Intent();
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contactsSelected);
        for(int i=0; i<contactsSelected.size();i++){
            log("setResultContacts: "+contactsSelected.get(i));
        }

        intent.putExtra(EXTRA_MEGA_CONTACTS, false);
        setResult(RESULT_OK, intent);
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
