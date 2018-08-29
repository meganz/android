package mega.privacy.android.app.lollipop;

import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.adapters.MegaBrowserLollipopAdapter;
import mega.privacy.android.app.utils.Constants;


public class ContactSharedFolderFragment extends ContactFileBaseFragment {
    
    RecyclerView listView;
    MegaBrowserLollipopAdapter adapter;
    
    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle savedInstanceState) {
        log("ContactSharedFolderFragment: onCreateView");
        View v = null;
        if (userEmail != null){
            v = inflater.inflate(R.layout.fragment_contact_shared_folder_list, container, false);
            contact = megaApi.getContact(userEmail);
            if(contact == null)
            {
                return null;
            }
        
            contactNodes = megaApi.getInShares(contact);
        
            listView = (RecyclerView) v.findViewById(R.id.contact_shared_folder_list_view);
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(context);
            listView.setLayoutManager(mLayoutManager);
            listView.setItemAnimator(new DefaultItemAnimator());
        
            if (adapter == null) {
                adapter = new MegaBrowserLollipopAdapter(context, this, contactNodes, -1,listView, aB,Constants.CONTACT_SHARED_FOLDER_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
            
            } else {
                adapter.setNodes(contactNodes);
                adapter.setParentHandle(-1);
            }
        
            adapter.setMultipleSelect(false);
            listView.setAdapter(adapter);
    
            Log.d("yuan", "done");
        }
    
        return v;
    }
    
}
