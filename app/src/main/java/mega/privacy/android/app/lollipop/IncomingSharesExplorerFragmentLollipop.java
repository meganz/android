package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Stack;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.adapters.MegaExplorerLollipopAdapter;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;


public class IncomingSharesExplorerFragmentLollipop extends Fragment implements OnClickListener{

	Context context;
	MegaApiAndroid megaApi;
	ArrayList<MegaNode> nodes = new ArrayList<MegaNode>();
	long parentHandle = -1;
	
	MegaExplorerLollipopAdapter adapter;
	
	int modeCloud;
	boolean selectFile;

	RecyclerView listView;
	LinearLayoutManager mLayoutManager;

	ImageView emptyImageView;
	LinearLayout emptyTextView;
	TextView emptyTextViewFirst;

	TextView contentText;
	View separator;
	Button optionButton;
	Button cancelButton;
	LinearLayout optionsBar;

	Stack<Integer> lastPositionStack;

	public static IncomingSharesExplorerFragmentLollipop newInstance() {
		log("newInstance");
		IncomingSharesExplorerFragmentLollipop fragment = new IncomingSharesExplorerFragmentLollipop();
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

		lastPositionStack = new Stack<>();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		log("onCreateView");

		Display display = getActivity().getWindowManager().getDefaultDisplay();
		
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		
		float density  = getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);

		View v = inflater.inflate(R.layout.fragment_fileexplorerlist, container, false);
		
		separator = (View) v.findViewById(R.id.separator);
		
		optionsBar = (LinearLayout) v.findViewById(R.id.options_explorer_layout);

		optionButton = (Button) v.findViewById(R.id.action_text);
		optionButton.setOnClickListener(this);
		//Left and Right margin
//		LinearLayout.LayoutParams optionTextParams = (LinearLayout.LayoutParams)optionButton.getLayoutParams();
//		optionTextParams.setMargins(0, 0, Util.scaleWidthPx(8, outMetrics), 0);
//		optionButton.setLayoutParams(optionTextParams);

		cancelButton = (Button) v.findViewById(R.id.cancel_text);
		cancelButton.setOnClickListener(this);
		cancelButton.setText(getString(R.string.general_cancel).toUpperCase(Locale.getDefault()));
		//Left and Right margin
//		LinearLayout.LayoutParams cancelTextParams = (LinearLayout.LayoutParams)cancelButton.getLayoutParams();
//		cancelTextParams.setMargins(Util.scaleWidthPx(10, outMetrics), 0, Util.scaleWidthPx(8, outMetrics), 0);
//		cancelButton.setLayoutParams(cancelTextParams);
		
		listView = (RecyclerView) v.findViewById(R.id.file_list_view_browser);

		listView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
		mLayoutManager = new LinearLayoutManager(context);
		listView.setLayoutManager(mLayoutManager);
		listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				if (listView.canScrollVertically(-1)){
					((FileExplorerActivityLollipop) context).changeActionBarElevation(true);
				}
				else {
					((FileExplorerActivityLollipop) context).changeActionBarElevation(false);
				}
			}
		});
		
		contentText = (TextView) v.findViewById(R.id.content_text);
		contentText.setVisibility(View.GONE);

		emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
		emptyTextView = (LinearLayout) v.findViewById(R.id.file_list_empty_text);
		emptyTextViewFirst = (TextView) v.findViewById(R.id.file_list_empty_text_first);
		parentHandle = ((FileExplorerActivityLollipop)context).parentHandleIncoming;

		modeCloud = ((FileExplorerActivityLollipop)context).getMode();
		selectFile = ((FileExplorerActivityLollipop)context).isSelectFile();

		if (parentHandle == -1){
			findNodes();
		}
		else{
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			nodes = megaApi.getChildren(parentNode);
		}
		
		if (adapter == null){
			adapter = new MegaExplorerLollipopAdapter(context, this, nodes, parentHandle, listView, selectFile);
			adapter.SetOnItemClickListener(new MegaExplorerLollipopAdapter.OnItemClickListener() {
				
				@Override
				public void onItemClick(View view, int position) {
					itemClick(view, position);
				}
			});
			listView.setAdapter(adapter);
		}
		else{
			adapter.setParentHandle(parentHandle);
			adapter.setNodes(nodes);
			adapter.setSelectFile(selectFile);
		}

		findDisabledNodes();

		adapter.setPositionClicked(-1);
		
		if (modeCloud == FileExplorerActivityLollipop.MOVE) {
			optionButton.setText(getString(R.string.context_move).toUpperCase(Locale.getDefault()));
		}
		else if (modeCloud == FileExplorerActivityLollipop.COPY){
			optionButton.setText(getString(R.string.context_copy).toUpperCase(Locale.getDefault()));

			if (((FileExplorerActivityLollipop)context).deepBrowserTree > 0){
				MegaNode parent = ((FileExplorerActivityLollipop)context).parentMoveCopy();
				if(parent != null){
					if(parent.getHandle() == parentHandle) {
						activateButton(false);
					}else{
						activateButton(true);
					}
				}else{
					activateButton(true);

				}
			}
		}
		else if (modeCloud == FileExplorerActivityLollipop.UPLOAD){
			optionButton.setText(getString(R.string.context_upload).toUpperCase(Locale.getDefault()));
		}
		else if (modeCloud == FileExplorerActivityLollipop.IMPORT){
			optionButton.setText(getString(R.string.add_to_cloud).toUpperCase(Locale.getDefault()));
		}
		else if (modeCloud == FileExplorerActivityLollipop.SELECT || modeCloud == FileExplorerActivityLollipop.SELECT_CAMERA_FOLDER){
			optionButton.setText(getString(R.string.general_select).toUpperCase(Locale.getDefault()));
		}
		else{
			optionButton.setText(getString(R.string.general_select).toUpperCase(Locale.getDefault()));
		}


		log("deepBrowserTree value: "+((FileExplorerActivityLollipop)context).deepBrowserTree);
		if (((FileExplorerActivityLollipop)context).deepBrowserTree <= 0){
			separator.setVisibility(View.GONE);
			optionsBar.setVisibility(View.GONE);
		}
		else{
			if(selectFile){
				separator.setVisibility(View.GONE);
				optionsBar.setVisibility(View.GONE);
			}
			else{
				separator.setVisibility(View.VISIBLE);
				optionsBar.setVisibility(View.VISIBLE);
			}
		}

		if (adapter.getItemCount() != 0){
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			listView.setVisibility(View.VISIBLE);

		}else{
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);
			if (parentHandle==-1) {
				if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					emptyImageView.setImageResource(R.drawable.incoming_empty_landscape);
				}else{
					emptyImageView.setImageResource(R.drawable.incoming_shares_empty);
				}
				String textToShow = String.format(context.getString(R.string.context_empty_incoming));
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
				emptyTextViewFirst.setText(result);

			}else{

//				emptyImageView.setImageResource(R.drawable.ic_empty_folder);
//				emptyTextViewFirst.setText(R.string.file_browser_empty_folder);
				if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
				}else{
					emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
				}
				String textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
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
				emptyTextViewFirst.setText(result);
			}

		}

		return v;
	}
	
	public void findNodes(){
		log("findNodes");
		((FileExplorerActivityLollipop)context).setDeepBrowserTree(0);

		separator.setVisibility(View.GONE);
		optionsBar.setVisibility(View.GONE);

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
	}

	public void findDisabledNodes (){
		log("findDisabledNodes");

		ArrayList<Long> disabledNodes = new ArrayList<Long>();

		for (int i=0;i<nodes.size();i++){
			MegaNode folder = nodes.get(i);
			int accessLevel = megaApi.getAccess(folder);

			if(selectFile){
				if(accessLevel!=MegaShare.ACCESS_FULL) {
					disabledNodes.add(folder.getHandle());
				}
			}
			else{
				if(accessLevel==MegaShare.ACCESS_READ) {
					disabledNodes.add(folder.getHandle());
				}
			}
		}

		this.setDisableNodes(disabledNodes);
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }
	
	@Override
	public void onClick(View v) {

		switch(v.getId()){
			case R.id.action_text:{
				((FileExplorerActivityLollipop) context).buttonClick(parentHandle);
				break;
			}
			case R.id.cancel_text:{
				((FileExplorerActivityLollipop) context).finishActivity();
				break;
			}
		}
	}

	public void navigateToFolder(long handle) {
		log("navigateToFolder");

		((FileExplorerActivityLollipop)context).increaseDeepBrowserTree();
		log("((FileExplorerActivityLollipop)context).deepBrowserTree value: "+((FileExplorerActivityLollipop)context).deepBrowserTree);
		if (((FileExplorerActivityLollipop)context).deepBrowserTree <= 0){
			separator.setVisibility(View.GONE);
			optionsBar.setVisibility(View.GONE);
		}
		else{
			if(selectFile){
				separator.setVisibility(View.GONE);
				optionsBar.setVisibility(View.GONE);
			}
			else{
				separator.setVisibility(View.VISIBLE);
				optionsBar.setVisibility(View.VISIBLE);
			}
		}

		int lastFirstVisiblePosition = 0;
		lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();

		log("Push to stack "+lastFirstVisiblePosition+" position");
		lastPositionStack.push(lastFirstVisiblePosition);

		parentHandle = handle;
		adapter.setParentHandle(parentHandle);
		nodes.clear();
		adapter.setNodes(nodes);
		listView.scrollToPosition(0);

		((FileExplorerActivityLollipop) context).changeTitle();

		//If folder has no files
		if (adapter.getItemCount() == 0){
			listView.setVisibility(View.GONE);
//			emptyImageView.setImageResource(R.drawable.ic_empty_folder);
//			emptyTextViewFirst.setText(R.string.file_browser_empty_folder);
			if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
				emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
			}else{
				emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
			}
			String textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
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
			emptyTextViewFirst.setText(result);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
		}
		else{
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
		}

		if (modeCloud == FileExplorerActivityLollipop.COPY){
			activateButton(true);
		}
	}

    public void itemClick(View view, int position) {
		log("------------------itemClick: "+((FileExplorerActivityLollipop)context).deepBrowserTree);
		if (nodes.get(position).isFolder()){

			((FileExplorerActivityLollipop)context).increaseDeepBrowserTree();
			log("deepBrowserTree value: "+((FileExplorerActivityLollipop)context).deepBrowserTree);
			if (((FileExplorerActivityLollipop)context).deepBrowserTree <= 0){
				separator.setVisibility(View.GONE);
				optionsBar.setVisibility(View.GONE);
			}
			else{
				if(selectFile){
					separator.setVisibility(View.GONE);
					optionsBar.setVisibility(View.GONE);
				}
				else{
					separator.setVisibility(View.VISIBLE);
					optionsBar.setVisibility(View.VISIBLE);
				}
			}

			int lastFirstVisiblePosition = 0;
			lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();

			log("Push to stack "+lastFirstVisiblePosition+" position");
			lastPositionStack.push(lastFirstVisiblePosition);

			parentHandle = nodes.get(position).getHandle();
			adapter.setParentHandle(parentHandle);
			nodes = megaApi.getChildren(nodes.get(position));
			adapter.setNodes(nodes);
			listView.scrollToPosition(0);

			((FileExplorerActivityLollipop) context).changeTitle();
			
			//If folder has no files
			if (adapter.getItemCount() == 0){
				listView.setVisibility(View.GONE);
//				emptyImageView.setImageResource(R.drawable.ic_empty_folder);
//				emptyTextViewFirst.setText(R.string.file_browser_empty_folder);
				if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
				}else{
					emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
				}
				String textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
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
				emptyTextViewFirst.setText(result);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				if (modeCloud == FileExplorerActivityLollipop.COPY){
					activateButton(true);
				}
			}
			else{
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);

				if (modeCloud == FileExplorerActivityLollipop.COPY){
					if (((FileExplorerActivityLollipop)context).deepBrowserTree > 0){
						MegaNode parent = ((FileExplorerActivityLollipop)context).parentMoveCopy();
						if(parent != null){
							if(parent.getHandle() == parentHandle) {
								activateButton(false);
							}else{
								activateButton(true);
							}
						}else{
							activateButton(true);

						}
					}
				}
			}
		}
		else
		{
			//Is file
			if(selectFile)
			{
				//Seleccionar el fichero para enviar...
				MegaNode n = nodes.get(position);
				log("Selected node to send: "+n.getName());
				if(nodes.get(position).isFile()){
					MegaNode nFile = nodes.get(position);
					
					MegaNode parentFile = megaApi.getParentNode(nFile);
					if(megaApi.getAccess(parentFile)==MegaShare.ACCESS_FULL)
					{
						((FileExplorerActivityLollipop) context).buttonClick(nFile.getHandle());
					}
					else{
						Toast.makeText(context, getString(R.string.context_send_no_permission), Toast.LENGTH_LONG).show();
					}					
				}		
			}
		}
		((FileExplorerActivityLollipop) context).supportInvalidateOptionsMenu();
	}	

	public int onBackPressed(){

		log("deepBrowserTree "+((FileExplorerActivityLollipop)context).deepBrowserTree);
		((FileExplorerActivityLollipop)context).decreaseDeepBrowserTree();

		if(((FileExplorerActivityLollipop)context).deepBrowserTree==0){
			parentHandle=-1;
//			uploadButton.setText(getString(R.string.choose_folder_explorer));
			findNodes();
			findDisabledNodes();
			
			adapter.setNodes(nodes);
			((FileExplorerActivityLollipop) context).changeTitle();

			int lastVisiblePosition = 0;
			if(!lastPositionStack.empty()){
				lastVisiblePosition = lastPositionStack.pop();
				log("Pop of the stack "+lastVisiblePosition+" position");
			}
			log("Scroll to "+lastVisiblePosition+" position");

			if(lastVisiblePosition>=0){
				mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
			}
			adapter.setParentHandle(parentHandle);

			separator.setVisibility(View.GONE);
			optionsBar.setVisibility(View.GONE);

			if (adapter.getItemCount() != 0){
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				listView.setVisibility(View.VISIBLE);
			}
			else{
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				listView.setVisibility(View.GONE);

				if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					emptyImageView.setImageResource(R.drawable.incoming_empty_landscape);
				}else{
					emptyImageView.setImageResource(R.drawable.incoming_shares_empty);
				}
				String textToShow = String.format(context.getString(R.string.context_empty_incoming));
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
				emptyTextViewFirst.setText(result);

			}
			((FileExplorerActivityLollipop) context).supportInvalidateOptionsMenu();
			return 3;
		}
		else if (((FileExplorerActivityLollipop)context).deepBrowserTree>0){
			parentHandle = adapter.getParentHandle();

			MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));				

			if (parentNode != null){
				
				parentHandle = parentNode.getHandle();
				nodes = megaApi.getChildren(parentNode);

				if (modeCloud == FileExplorerActivityLollipop.COPY){
					if (((FileExplorerActivityLollipop)context).deepBrowserTree > 0){
						MegaNode parent = ((FileExplorerActivityLollipop)context).parentMoveCopy();
						if(parent != null){
							if(parent.getHandle() == parentHandle) {
								activateButton(false);
							}else{
								activateButton(true);
							}
						}else{
							activateButton(true);

						}
					}
				}

				adapter.setNodes(nodes);
				int lastVisiblePosition = 0;
				if(!lastPositionStack.empty()){
					lastVisiblePosition = lastPositionStack.pop();
					log("Pop of the stack "+lastVisiblePosition+" position");
				}
				log("Scroll to "+lastVisiblePosition+" position");

				if(lastVisiblePosition>=0){
					mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
				}
				adapter.setParentHandle(parentHandle);

				((FileExplorerActivityLollipop) context).changeTitle();

				if (adapter.getItemCount() != 0){
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
					listView.setVisibility(View.VISIBLE);
				}
				else{
//					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
//					emptyTextViewFirst.setText(R.string.file_browser_empty_folder);
					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
					}else{
						emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
					}
					String textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
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
					emptyTextViewFirst.setText(result);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					listView.setVisibility(View.GONE);
				}
				((FileExplorerActivityLollipop) context).supportInvalidateOptionsMenu();
				return 2;
			}

			if(selectFile){
				separator.setVisibility(View.GONE);
				optionsBar.setVisibility(View.GONE);
			}
			else{
				separator.setVisibility(View.VISIBLE);
				optionsBar.setVisibility(View.VISIBLE);
			}

			return 2;
		}
		else{
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			separator.setVisibility(View.GONE);
			optionsBar.setVisibility(View.GONE);
			((FileExplorerActivityLollipop)context).deepBrowserTree=0;
			((FileExplorerActivityLollipop) context).supportInvalidateOptionsMenu();
			return 0;
		}
	}
	
	/*
	 * Disable nodes from the list
	 */
	public void setDisableNodes(ArrayList<Long> disabledNodes) {
		adapter.setDisableNodes(disabledNodes);
	}
	
	private static void log(String log) {
		Util.log("IncomingSharesExplorerFragmentLollipop", log);
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
					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.incoming_empty_landscape);
					}else{
						emptyImageView.setImageResource(R.drawable.incoming_shares_empty);
					}
					String textToShow = String.format(context.getString(R.string.context_empty_incoming));
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
					emptyTextViewFirst.setText(result);
				} else {
//					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
//					emptyTextViewFirst.setText(R.string.file_browser_empty_folder);

					if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
						emptyImageView.setImageResource(R.drawable.ic_zero_landscape_empty_folder);
					}else{
						emptyImageView.setImageResource(R.drawable.ic_zero_portrait_empty_folder);
					}
					String textToShow = String.format(context.getString(R.string.file_browser_empty_folder_new));
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
					emptyTextViewFirst.setText(result);

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

	public void activateButton(boolean show){
		optionButton.setEnabled(show);
		if(show){
			optionButton.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
		}else{
			optionButton.setTextColor(ContextCompat.getColor(context, R.color.invite_button_deactivated));
		}
	}
}
