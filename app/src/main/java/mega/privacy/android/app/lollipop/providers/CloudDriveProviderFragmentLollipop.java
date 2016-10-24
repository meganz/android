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
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MegaLinearLayoutManager;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.providers.FileProviderActivity;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;


public class CloudDriveProviderFragmentLollipop extends Fragment implements RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener{

	Context context;
	MegaApiAndroid megaApi;
	ArrayList<MegaNode> nodes;
	long parentHandle = -1;
	
	MegaProviderLollipopAdapter adapter;
	GestureDetectorCompat detector;
	MegaPreferences prefs;
	DatabaseHandler dbH;
	
	public String name;
	
//	boolean first = false;
//	private boolean folderSelected = false;
	RecyclerView listView;
	RecyclerView.LayoutManager mLayoutManager;
	ImageView emptyImageView;
	TextView emptyTextView;
	TextView contentText;
	
	long [] hashes;
	
	public class RecyclerViewOnGestureListener extends SimpleOnGestureListener{

		public void onLongPress(MotionEvent e) {
			View view = listView.findChildViewUnder(e.getX(), e.getY());
			int position = listView.getChildPosition(view);

			// handle long press
			if (adapter.getPositionClicked() == -1){
				//TODO: multiselect
				itemClick(position);
			}  
			super.onLongPress(e);
		}
	}

	public static CloudDriveProviderFragmentLollipop newInstance() {
		log("newInstance");
		CloudDriveProviderFragmentLollipop fragment = new CloudDriveProviderFragmentLollipop();
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
		
		parentHandle = -1;
		dbH = DatabaseHandler.getDbHandler(context);		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		log("onCreateView");
				
		View v = inflater.inflate(R.layout.fragment_clouddriveprovider, container, false);	
		
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);
		
		detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());

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
			parentHandle = ((FileProviderActivity)context).getParentHandle();
			log("The parent handle is: "+parentHandle);
		}
	
		if (parentHandle == -1)
		{			
			//Find in the database the last parentHandle
			prefs = dbH.getPreferences();
			if (prefs != null) {
				String lastFolder = prefs.getLastFolderCloud();
				if(lastFolder != null) {
					if (lastFolder.compareTo("") != 0){
						parentHandle = Long.parseLong(lastFolder);
					}
				}
			}
		}			
		
		MegaNode chosenNode = megaApi.getNodeByHandle(parentHandle);
		if(chosenNode == null)
		{
			parentHandle = megaApi.getRootNode().getHandle();
			nodes = megaApi.getChildren(megaApi.getRootNode());
			changeActionBarTitle(context.getString(R.string.section_cloud_drive));
		}
		else
		{
			nodes = megaApi.getChildren(chosenNode);
			if(chosenNode.getType() != MegaNode.TYPE_ROOT)
			{
				changeActionBarTitle(chosenNode.getName());
			}
			else
			{
				changeActionBarTitle(context.getString(R.string.section_cloud_drive));
			}
		}
		
		if (context instanceof FileProviderActivity){
			((FileProviderActivity)context).setParentHandle(parentHandle);
		}

		if (adapter == null){
			adapter = new MegaProviderLollipopAdapter(context, this, nodes, parentHandle, listView, emptyImageView, emptyTextView);
		}

		listView.setAdapter(adapter);
		adapter.setParentHandle(parentHandle);
		setNodes(nodes);
		
		adapter.setPositionClicked(-1);
		
		return v;
	}
	
//	public void setMode(int mode){
//		log("setMode: "+mode);
//		modeCloud=mode;
//		log("setMode: "+modeCloud);
//	}	
	
	
	public void changeActionBarTitle(String folder){
		log("changeActionBarTitle");
		if (context instanceof FileProviderActivity){
			int tabShown = ((FileProviderActivity)context).getTabShown();

			if(tabShown==FileProviderActivity.CLOUD_TAB){
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
		log("onItemClick");
		
		if (nodes.get(position).isFolder()){
					
			MegaNode n = nodes.get(position);			
			
			String path=n.getName();	
			String[] temp;
			temp = path.split("/");
			name = temp[temp.length-1];

			changeActionBarTitle(name);

			parentHandle = nodes.get(position).getHandle();
			if (context instanceof FileProviderActivity){
				((FileProviderActivity)context).setParentHandle(parentHandle);
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
		log("onBackPressed");
		
		parentHandle = adapter.getParentHandle();
		
		MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
		if (parentNode != null){
			
			if(parentNode.getType()==MegaNode.TYPE_ROOT){
				parentHandle=-1;
				changeActionBarTitle(context.getString(R.string.section_cloud_drive));
			}
			else{
				String path=parentNode.getName();	
				String[] temp;
				temp = path.split("/");
				name = temp[temp.length-1];
				changeActionBarTitle(name);
				parentHandle = parentNode.getHandle();
			}
			
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);			
			
			nodes = megaApi.getChildren(parentNode);
			setNodes(nodes);
			listView.scrollToPosition(0);
			adapter.setParentHandle(parentHandle);
			if (context instanceof FileProviderActivity){
				((FileProviderActivity)context).setParentHandle(parentHandle);
			}
			
			return 2;
		}
		else{
			return 0;
		}
	}
	
	
	private static void log(String log) {
		Util.log("CloudDriveProviderFragment", log);
	}
	
	public long getParentHandle(){
		return adapter.getParentHandle();
	}
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
		if (adapter != null){
			adapter.setParentHandle(parentHandle);
		}
		
		if (context instanceof FileProviderActivity){
			((FileProviderActivity)context).setParentHandle(parentHandle);
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

	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onInterceptTouchEvent(RecyclerView rV, MotionEvent e) {
		detector.onTouchEvent(e);
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
