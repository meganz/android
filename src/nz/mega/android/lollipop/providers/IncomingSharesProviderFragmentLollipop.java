package nz.mega.android.lollipop.providers;

import java.util.ArrayList;

import nz.mega.android.MegaApplication;
import nz.mega.android.R;
import nz.mega.android.providers.FileProviderActivity;
import nz.mega.android.utils.Util;
import nz.mega.components.SimpleDividerItemDecoration;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class IncomingSharesProviderFragmentLollipop extends Fragment implements OnClickListener, RecyclerView.OnItemTouchListener{

	Context context;
	MegaApiAndroid megaApi;
	ArrayList<MegaNode> nodes;
	long parentHandle = -1;
	
	long [] hashes;
	
	MegaProviderLollipopAdapter adapter;
	GestureDetectorCompat detector;
	public String name;
	
//	boolean first = false;
//	private boolean folderSelected = false;
	RecyclerView listView;
	RecyclerView.LayoutManager mLayoutManager;
	ImageView emptyImageView;
	TextView emptyTextView;
	TextView contentText;
	int deepBrowserTree = 0;

	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		log("onCreate");
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		if (megaApi.getRootNode() == null){
			return;
		}
		
		nodes = new ArrayList<MegaNode>();
		deepBrowserTree=0;
		parentHandle = -1;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		log("onCreateView");
	
		View v = inflater.inflate(R.layout.fragment_clouddriveprovider, container, false);	
		
//		detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
		
		listView = (RecyclerView) v.findViewById(R.id.provider_list_view_browser);
		listView.addItemDecoration(new SimpleDividerItemDecoration(context));
		mLayoutManager = new LinearLayoutManager(context);
		listView.setLayoutManager(mLayoutManager);
		listView.addOnItemTouchListener(this);
		listView.setItemAnimator(new DefaultItemAnimator()); 
		
		
		contentText = (TextView) v.findViewById(R.id.provider_content_text);
		contentText.setVisibility(View.GONE);
		
		emptyImageView = (ImageView) v.findViewById(R.id.provider_list_empty_image);
		emptyTextView = (TextView) v.findViewById(R.id.provider_list_empty_text);
		
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) listView.getLayoutParams();
		params.addRule(RelativeLayout.ABOVE, R.id.file_explorer_button);
		
		if (adapter == null){
			adapter = new MegaProviderLollipopAdapter(context, this, nodes, parentHandle, listView, emptyImageView, emptyTextView);
		}
		else{
			adapter.setParentHandle(parentHandle);
			adapter.setNodes(nodes);
		}
		
		String actionBarTitle = getString(R.string.title_incoming_shares_explorer);	
		
		if (parentHandle == -1){			
			findNodes();	
			adapter.setParentHandle(-1);
		}
		else{
			adapter.setParentHandle(parentHandle);
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			nodes = megaApi.getChildren(parentNode);
		}	

		if (deepBrowserTree != 0){		
			MegaNode infoNode = megaApi.getNodeByHandle(parentHandle);
		}						
	
		adapter.setPositionClicked(-1);		
		
		listView.setAdapter(adapter);		
		
		return v;
	}
	
	public void findNodes(){
		deepBrowserTree=0;
		ArrayList<MegaUser> contacts = megaApi.getContacts();
		nodes.clear();
		for (int i=0;i<contacts.size();i++){			
			ArrayList<MegaNode> nodeContact=megaApi.getInShares(contacts.get(i));
			if(nodeContact!=null){
				if(nodeContact.size()>0){
					nodes.addAll(nodeContact);
				}
			}			
		}
		
//		for (int i=0;i<nodes.size();i++){	
//			MegaNode folder = nodes.get(i);
//			int accessLevel = megaApi.getAccess(folder);
//			
//			if(accessLevel==MegaShare.ACCESS_READ) {
//				disabledNodes.add(folder.getHandle());
//			}
//		}
//		
//		this.setDisableNodes(disabledNodes);
		
	}
	
	
	public void changeActionBarTitle(String folder){
		((FileProviderActivity) context).changeTitle(folder);
	}
	
	public void changeBackVisibility(boolean backVisibility){
		((FileProviderActivity) context).changeBackVisibility(backVisibility);
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
//			case R.id.file_explorer_button:{
//				((FileProviderActivity) context).buttonClick(parentHandle);
//			}
		}
	}

    public void itemClick(int position) {
		log("------------------onItemClick: "+deepBrowserTree);
		
		
		if (nodes.get(position).isFolder()){
					
			deepBrowserTree = deepBrowserTree+1;
			
			MegaNode n = nodes.get(position);			
			
			String path=n.getName();	
			String[] temp;
			temp = path.split("/");
			name = temp[temp.length-1];

			changeActionBarTitle(name);
			changeBackVisibility(true);
			
			parentHandle = nodes.get(position).getHandle();
			adapter.setParentHandle(parentHandle);
			nodes = megaApi.getChildren(nodes.get(position));
			adapter.setNodes(nodes);
			listView.scrollToPosition(0);
			
			//If folder has no files
			if (adapter.getItemCount() == 0){
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				if (megaApi.getRootNode().getHandle()==n.getHandle()) {
					emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
					emptyTextView.setText(R.string.file_browser_empty_cloud_drive);
				} else {
					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
					emptyTextView.setText(R.string.file_browser_empty_folder);
				}
			}
			else{
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}
		}
		else{
			//File selected to download
			MegaNode n = nodes.get(position);
			hashes = new long[1];
			hashes[0]=n.getHandle();
			((FileProviderActivity) context).downloadTo(n.getSize(), hashes);
		}
	}	


	public int onBackPressed(){
		log("deepBrowserTree "+deepBrowserTree);
		deepBrowserTree = deepBrowserTree-1;
		
		if(deepBrowserTree==0){
			parentHandle=-1;
			changeActionBarTitle(getString(R.string.title_incoming_shares_explorer));
			changeBackVisibility(false);
			findNodes();
			
			adapter.setNodes(nodes);
			listView.scrollToPosition(0);
			adapter.setParentHandle(parentHandle);

//			adapterList.setNodes(nodes);
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);

			return 3;
		}
		else if (deepBrowserTree>0){
			parentHandle = adapter.getParentHandle();
			//((ManagerActivity)context).setParentHandleSharedWithMe(parentHandle);			
			
			MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));				

			if (parentNode != null){
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);

				changeActionBarTitle(parentNode.getName());
				changeBackVisibility(true);	
				
				parentHandle = parentNode.getHandle();
				nodes = megaApi.getChildren(parentNode);

				adapter.setNodes(nodes);
				listView.scrollToPosition(0);
				adapter.setParentHandle(parentHandle);
				return 2;
			}	
			return 2;
		}
		else{
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			deepBrowserTree=0;
			return 0;
		}
	}
	
	private static void log(String log) {
		Util.log("IncomingSharesProviderFragment", log);
	}
	
	public long getParentHandle(){
		return adapter.getParentHandle();
	}
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
		if (adapter != null){
			adapter.setParentHandle(parentHandle);
		}
	}
	
	public void setNodes(ArrayList<MegaNode> nodes){
		this.nodes = nodes;
		if (adapter != null){
			adapter.setNodes(nodes);
			if (adapter.getItemCount() == 0){
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				if (megaApi.getRootNode().getHandle()==parentHandle) {
					emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
					emptyTextView.setText(R.string.file_browser_empty_cloud_drive);
				} else {
					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
					emptyTextView.setText(R.string.file_browser_empty_folder);
				}
			}
			else{
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}
		}
	}
	
	public RecyclerView getListView(){
		return listView;
	}

	public int getDeepBrowserTree() {
		return deepBrowserTree;
	}

	public void setDeepBrowserTree(int deepBrowserTree) {
		this.deepBrowserTree = deepBrowserTree;
	}

	@Override
	public boolean onInterceptTouchEvent(RecyclerView arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onRequestDisallowInterceptTouchEvent(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTouchEvent(RecyclerView arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		
	}
}
