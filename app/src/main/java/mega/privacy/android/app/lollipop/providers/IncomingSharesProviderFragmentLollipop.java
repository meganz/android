package mega.privacy.android.app.lollipop.providers;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MegaLinearLayoutManager;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.providers.FileProviderActivity;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;


public class IncomingSharesProviderFragmentLollipop extends Fragment implements RecyclerView.OnItemTouchListener{

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
	int deepBrowserTree = -1;

	public static IncomingSharesProviderFragmentLollipop newInstance() {
		log("newInstance");
		IncomingSharesProviderFragmentLollipop fragment = new IncomingSharesProviderFragmentLollipop();
		return fragment;
	}

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
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		log("onCreateView");
	
		View v = inflater.inflate(R.layout.fragment_clouddriveprovider, container, false);	
		
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);

		listView = (RecyclerView) v.findViewById(R.id.provider_list_view_browser);
		listView.addItemDecoration(new SimpleDividerItemDecoration(context, metrics));
		mLayoutManager = new MegaLinearLayoutManager(context);
		listView.setLayoutManager(mLayoutManager);
		listView.addOnItemTouchListener(this);
		listView.setItemAnimator(new DefaultItemAnimator()); 		
		
		contentText = (TextView) v.findViewById(R.id.provider_content_text);
		contentText.setVisibility(View.GONE);
		
		emptyImageView = (ImageView) v.findViewById(R.id.provider_list_empty_image);
		emptyTextView = (TextView) v.findViewById(R.id.provider_list_empty_text);

		if (context instanceof FileProviderActivity){
			parentHandle = ((FileProviderActivity)context).getIncParentHandle();
			deepBrowserTree = ((FileProviderActivity)context).getIncomingDeepBrowserTree();
			log("The parent handle is: "+parentHandle);
			log("The browser tree deep is: "+deepBrowserTree);
		}
		
		if (adapter == null){
			adapter = new MegaProviderLollipopAdapter(context, this, nodes, parentHandle, listView, emptyImageView, emptyTextView);
		}
		listView.setAdapter(adapter);

		if (parentHandle == -1){
			findNodes();
			setNodes(nodes);
			adapter.setParentHandle(-1);
		}
		else{
			adapter.setParentHandle(parentHandle);
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			if(parentNode!=null){
				nodes = megaApi.getChildren(parentNode);
				setNodes(nodes);
				log("INCOMING is in: "+parentNode.getName());
				changeActionBarTitle(parentNode.getName());
			}
			else{
				log("ERROR parentNode is NULL");
				findNodes();
				setNodes(nodes);
				parentHandle = -1;
				adapter.setParentHandle(-1);
				changeActionBarTitle(getString(R.string.title_incoming_shares_explorer));
				if (context instanceof FileProviderActivity){
					((FileProviderActivity)context).setParentHandle(parentHandle);
					log("PArentHandle change to: "+parentHandle);
				}
			}
		}
	
		adapter.setPositionClicked(-1);

		return v;
	}
	
	public void findNodes(){
		log("findNodes");

		deepBrowserTree=0;
		if (context instanceof FileProviderActivity){
			((FileProviderActivity)context).setIncomingDeepBrowserTree(deepBrowserTree);
			log("The browser tree change to: "+deepBrowserTree);
		}
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

		changeActionBarTitle(getString(R.string.title_incoming_shares_explorer));
	}
	
	
	public void changeActionBarTitle(String folder){
		if (context instanceof FileProviderActivity){
			int tabShown = ((FileProviderActivity)context).getTabShown();

			if(tabShown==FileProviderActivity.INCOMING_TAB){
				((FileProviderActivity) context).changeTitle(folder);
			}
		}
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    public void itemClick(int position) {
		log("------------------onItemClick: "+deepBrowserTree);
		
		if (nodes.get(position).isFolder()){
					
			deepBrowserTree = deepBrowserTree+1;
			if (context instanceof FileProviderActivity){
				((FileProviderActivity)context).setIncomingDeepBrowserTree(deepBrowserTree);
				log("The browser tree change to: "+deepBrowserTree);
			}

			MegaNode n = nodes.get(position);			
			
			String path=n.getName();	
			String[] temp;
			temp = path.split("/");
			name = temp[temp.length-1];

			changeActionBarTitle(name);
			
			parentHandle = nodes.get(position).getHandle();
			if (context instanceof FileProviderActivity){
				((FileProviderActivity)context).setIncParentHandle(parentHandle);
				log("The parent handle change to: "+parentHandle);
			}
			adapter.setParentHandle(parentHandle);
			nodes = megaApi.getChildren(nodes.get(position));
			setNodes(nodes);
			listView.scrollToPosition(0);
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
		if (context instanceof FileProviderActivity){
			((FileProviderActivity)context).setIncomingDeepBrowserTree(deepBrowserTree);
			log("The browser tree change to: "+deepBrowserTree);
		}

		if(deepBrowserTree==0){
			parentHandle=-1;
			if (context instanceof FileProviderActivity){
				((FileProviderActivity)context).setIncParentHandle(parentHandle);
				log("The parent handle change to: "+parentHandle);
			}
			changeActionBarTitle(getString(R.string.title_incoming_shares_explorer));
			findNodes();
			
			setNodes(nodes);
			listView.scrollToPosition(0);
			adapter.setParentHandle(parentHandle);

			return 3;
		}
		else if (deepBrowserTree>0){
			parentHandle = adapter.getParentHandle();
			//((ManagerActivity)context).setParentHandleSharedWithMe(parentHandle);			
			if (context instanceof FileProviderActivity){
				((FileProviderActivity)context).setIncParentHandle(parentHandle);
				log("The parent handle change to: "+parentHandle);
			}
			MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));				

			if (parentNode != null){
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);

				changeActionBarTitle(parentNode.getName());
				
				parentHandle = parentNode.getHandle();
				if (context instanceof FileProviderActivity){
					((FileProviderActivity)context).setIncParentHandle(parentHandle);
					log("The parent handle change to: "+parentHandle);
				}
				nodes = megaApi.getChildren(parentNode);

				setNodes(nodes);
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
			if (context instanceof FileProviderActivity){
				((FileProviderActivity)context).setIncomingDeepBrowserTree(deepBrowserTree);
				log("The browser tree change to: "+deepBrowserTree);
			}
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
		log("setParentHandle");
		this.parentHandle = parentHandle;
		if (context instanceof FileProviderActivity){
			((FileProviderActivity)context).setIncParentHandle(parentHandle);
			log("The parent handle change to: "+parentHandle);
		}
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
