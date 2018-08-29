package mega.privacy.android.app.lollipop;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.OpenLinkActivity;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.adapters.MegaBrowserLollipopAdapter;
import mega.privacy.android.app.utils.Constants;
import nz.mega.sdk.MegaNode;


public class ContactSharedFolderFragment extends ContactFileBaseFragment {
    
    RecyclerView listView;
    Button moreButton;
    MegaBrowserLollipopAdapter adapter;
    
    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle savedInstanceState) {
        log("ContactSharedFolderFragment: onCreateView");
        
        View v = null;
        if (userEmail != null) {
            v = inflater.inflate(R.layout.fragment_contact_shared_folder_list,container,false);
            contact = megaApi.getContact(userEmail);
            
            //only show up to 5 folders in this page
            ArrayList<MegaNode> sharedFolders = megaApi.getInShares(contact);
            if (sharedFolders.size() > 5) {
                contactNodes = new ArrayList<>();
                for (int i = 0;i < 5;i++) {
                    contactNodes.add(sharedFolders.get(i));
                }
            } else {
                contactNodes = megaApi.getInShares(contact);
            }
            
            //set button text
            moreButton = (Button)v.findViewById(R.id.more_button);
            int foldersInvisible = sharedFolders.size() - contactNodes.size();
            moreButton.setText(getButtonLabel(foldersInvisible));
            
            //button is not clickable if no invisible folder
            if (foldersInvisible > 0) {
                moreButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(getContext(),ContactFileListActivityLollipop.class);
                        i.putExtra("name",userEmail);
                        getContext().startActivity(i);
                    }
                });
            } else {
                moreButton.setClickable(false);
            }
            
            //set up list view
            listView = (RecyclerView)v.findViewById(R.id.contact_shared_folder_list_view);
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(context);
            listView.setLayoutManager(mLayoutManager);
            listView.setItemAnimator(new DefaultItemAnimator());
            
            if (adapter == null) {
                adapter = new MegaBrowserLollipopAdapter(context,this,contactNodes,-1,listView,aB,Constants.CONTACT_SHARED_FOLDER_ADAPTER,MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
                
            } else {
                adapter.setNodes(contactNodes);
                adapter.setParentHandle(-1);
            }
            
            adapter.setMultipleSelect(false);
            listView.setAdapter(adapter);
        }
        
        return v;
    }
    
    //get button label in the format of x FOLDERS
    private String getButtonLabel(int foldersInvisible) {
        return foldersInvisible + " " + getResources().getQuantityString(R.plurals.general_num_folders,foldersInvisible).toUpperCase(Locale.getDefault());
    }
    
}
