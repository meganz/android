package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.components.tokenautocomplete.TokenCompleteTextView;

import java.util.ArrayList;
import java.util.List;

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
import nz.mega.sdk.MegaUser;

public class AddContactActivityLollipop extends PinActivityLollipop implements TokenCompleteTextView.TokenListener<ContactInfo>, View.OnClickListener, RecyclerView.OnItemTouchListener {

    DisplayMetrics outMetrics;

    MegaApplication app;
    MegaApiAndroid megaApi;
    DatabaseHandler dbH = null;

    Toolbar tB;
    ActionBar aB;

    ImageView sendButton;
    ImageView backButton;
    ContactsCompletionView completionView;
    RecyclerView recyclerView;
    ImageView emptyImageView;
    TextView emptyTextView;

    ContactInfo[] people;
    MegaContactsLollipopAdapter adapter;
    GestureDetectorCompat detector;

    ArrayList<MegaUser> contacts;
    ArrayList<MegaUser> visibleContacts = new ArrayList<MegaUser>();
    ArrayList<MegaUser> filteredContacts = new ArrayList<MegaUser>();

    boolean itemClickPressed = false;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");

        super.onCreate(savedInstanceState);

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        float density  = getResources().getDisplayMetrics().density;

        app = (MegaApplication)getApplication();
        megaApi = app.getMegaApi();

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

        //Get MEGA contacts and phone contacts: first name, last name and email
        contacts = megaApi.getContacts();
        visibleContacts.clear();
        filteredContacts.clear();

        for (int i=0;i<contacts.size();i++){

            log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility());
            if (contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){
                visibleContacts.add(contacts.get(i));
            }
        }

        List<ContactInfo> tokens = completionView.getObjects();
        if (tokens != null){
            log("tokens.size() = " + tokens.size());

            if (tokens.size() == 0) {
                for (int i = 0; i < visibleContacts.size(); i++) {
                    log("filteredContacts.add(visibleContacts.get(" + i + ") = " + visibleContacts.get(i).getEmail());
                    filteredContacts.add(visibleContacts.get(i));
                }
            } else {
                for (int i = 0; i < visibleContacts.size(); i++) {
                    boolean found = false;
                    for (int j = 0; j < tokens.size(); j++) {
                        log("tokens.get(" + j + ").getEmail() = " + tokens.get(j).getEmail());
                        log("visibleContacts.get(" + i + ").getEmail() = " + visibleContacts.get(i).getEmail());
                        if (tokens.get(j).getEmail().compareTo(visibleContacts.get(i).getEmail()) == 0) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        log("!found -> filteredContacts.add(visibleContacts.get(" + i + ") = " + visibleContacts.get(i).getEmail());
                        filteredContacts.add(visibleContacts.get(i));
                    }
                }
            }
        }

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

        if (adapter == null){
            adapter = new MegaContactsLollipopAdapter(this, null, filteredContacts, emptyImageView, emptyTextView, recyclerView, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
        }
        else{
            adapter.setContacts(filteredContacts);
            adapter.setAdapterType(MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
        }

        adapter.setPositionClicked(-1);
        recyclerView.setAdapter(adapter);

        if (adapter.getItemCount() == 0){

            emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
            emptyTextView.setText(R.string.contacts_list_empty_text);
            recyclerView.setVisibility(View.GONE);
            emptyImageView.setVisibility(View.VISIBLE);
            emptyTextView.setVisibility(View.VISIBLE);
        }
        else{
            recyclerView.setVisibility(View.VISIBLE);
            emptyImageView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.GONE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasReadContactsPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);
            if (!hasReadContactsPermission) {
                log("No read contacts permission");
                ActivityCompat.requestPermissions((AddContactActivityLollipop)this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        Constants.REQUEST_READ_CONTACTS);
            }
            else{

            }
        }

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
            if ((s.toString().charAt(i) != ',') && (s.toString().charAt(i) != ' ')){
                text = text + s.toString().charAt(i);
            }
        }

        log("text: _" + text + "_");

        ArrayList<MegaUser> filteredContactsAfterText = new ArrayList<MegaUser>();
        if (filteredContacts != null){
            for (int i=0;i<filteredContacts.size();i++){
                if (filteredContacts.get(i).getEmail().startsWith(text.trim()) == true){
                    filteredContactsAfterText.add(filteredContacts.get(i));
                }
                else{
                    MegaContact contactDB = dbH.findContactByHandle(String.valueOf(filteredContacts.get(i).getHandle()));
                    if(contactDB!=null){
                        String name = contactDB.getName();
                        String lastName = contactDB.getLastName();
                        if (name.startsWith(text.trim()) == true){
                            filteredContactsAfterText.add(filteredContacts.get(i));
                        }
                        else if (lastName.startsWith(text.trim()) == true){
                            filteredContactsAfterText.add(filteredContacts.get(i));
                        }
                    }
                }
            }

            if (adapter != null){
                adapter.setContacts(filteredContactsAfterText);
                adapter.setAdapterType(MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);

                if (adapter.getItemCount() == 0){

                    emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
                    emptyTextView.setText(R.string.contacts_list_empty_text);
                    recyclerView.setVisibility(View.GONE);
                    emptyImageView.setVisibility(View.VISIBLE);
                    emptyTextView.setVisibility(View.VISIBLE);
                }
                else{
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyImageView.setVisibility(View.GONE);
                    emptyTextView.setVisibility(View.GONE);
                }
            }
        }
    }

    public void itemClick(String email){

        log("itemClick");

        ContactInfo c = new ContactInfo();

        for (int i=0;i<filteredContacts.size();i++){
            if (filteredContacts.get(i).getEmail().compareTo(email) == 0){
                c.setEmail(filteredContacts.get(i).getEmail());
                MegaContact contactDB = dbH.findContactByHandle(String.valueOf(filteredContacts.get(i).getHandle()));
                if(contactDB!=null){
                    String name = contactDB.getName();
                    String lastName = contactDB.getLastName();
                    c.setName(name + " "  + lastName);
                }

                filteredContacts.remove(i);
                break;
            }
        }

        if (c.getEmail().compareTo("") != 0){
            log("completionView.getText() =  " + completionView.getText());
            itemClickPressed = true;
            completionView.addObject(c);
        }
    }

    private void updateTokenConfirmation() {
        log("Current tokens: \n");
        for (Object token: completionView.getObjects()){
            log(token.toString() + "\n");
        }

        List<ContactInfo> tokens = completionView.getObjects();
        if ((tokens != null) && (visibleContacts != null) && (filteredContacts != null)){
            filteredContacts.clear();
            log("tokens.size() = " + tokens.size());

            if (tokens.size() == 0) {
                for (int i = 0; i < visibleContacts.size(); i++) {
                    log("filteredContacts.add(visibleContacts.get(" + i + ") = " + visibleContacts.get(i).getEmail());
                    filteredContacts.add(visibleContacts.get(i));
                }
            } else {
                for (int i = 0; i < visibleContacts.size(); i++) {
                    boolean found = false;
                    for (int j = 0; j < tokens.size(); j++) {
                        log("tokens.get(" + j + ").getEmail() = " + tokens.get(j).getEmail());
                        log("visibleContacts.get(" + i + ").getEmail() = " + visibleContacts.get(i).getEmail());
                        if (tokens.get(j).getEmail().compareTo(visibleContacts.get(i).getEmail()) == 0) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        log("!found -> filteredContacts.add(visibleContacts.get(" + i + ") = " + visibleContacts.get(i).getEmail());
                        filteredContacts.add(visibleContacts.get(i));
                    }
                }
            }

            if (adapter != null){
                adapter.setContacts(filteredContacts);
                adapter.setAdapterType(MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
            }
        }

        if (itemClickPressed){
            for (int i=0; i<completionView.getText().length();i++){
                if ((completionView.getText().charAt(i) != ',') && (completionView.getText().charAt(i) != ' ')){
                    log("Delete char: " + i + "__ " + completionView.getText().charAt(i));
                    completionView.getText().delete(i,i+1);
                    i--;
                }
            }
            itemClickPressed = false;
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
                int l = completionView.getText().length();
                completionView.getText().delete(l-1, l);
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
                log("REQUEST_READ_CONTACTS");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    boolean hasReadContactsPermissions = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);
                    if (hasReadContactsPermissions) {
                        //TODO CARGAR Y MOSTRAR PHONE
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
