package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.ListIterator;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaTransfersLollipopAdapter;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaTransfer;

import static mega.privacy.android.app.utils.LogUtil.*;


public class TransfersFragmentLollipop extends Fragment {

	Context context;
	RecyclerView listView;
	MegaTransfersLollipopAdapter adapter;
	
	MegaApiAndroid megaApi;
	TextView contentText;
	ImageView emptyImage;
	TextView emptyText;

	float density;
	DisplayMetrics outMetrics;
	Display display;
	
	LinearLayoutManager mLayoutManager;
	
	TransfersFragmentLollipop transfersFragment = this;
	
//	SparseArray<TransfersHolder> transfersListArray = null;

	ArrayList<MegaTransfer> tL = null;

	private Handler handler;
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		handler = new Handler();

		tL = new ArrayList<MegaTransfer>();

		super.onCreate(savedInstanceState);
		logDebug("onCreate");
	}

	public static TransfersFragmentLollipop newInstance() {
		logDebug("newInstance");
		TransfersFragmentLollipop fragment = new TransfersFragmentLollipop();
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {  
		
		super.onCreateView(inflater, container, savedInstanceState);
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

		display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
	    
		View v = inflater.inflate(R.layout.fragment_transfers, container, false);

		listView = (RecyclerView) v.findViewById(R.id.transfers_list_view);
		listView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
		mLayoutManager = new LinearLayoutManager(context);
		listView.setLayoutManager(mLayoutManager);
		listView.setHasFixedSize(true);
		listView.setItemAnimator(null);
		listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				if (listView != null) {
					if (listView.canScrollVertically(-1)) {
						((ManagerActivityLollipop) context).changeActionBarElevation(true);
					}
					else {
						((ManagerActivityLollipop) context).changeActionBarElevation(false);
					}
				}
			}
		});

		emptyImage = (ImageView) v.findViewById(R.id.transfers_empty_image);
		emptyText = (TextView) v.findViewById(R.id.transfers_empty_text);

		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			emptyImage.setImageResource(R.drawable.ic_zero_landscape_saved_for_offline);
		}else{
			emptyImage.setImageResource(R.drawable.ic_zero_portrait_transfers);
		}

//		emptyText.setText(getString(R.string.transfers_empty));

		String textToShow = String.format(context.getString(R.string.transfers_empty_new));
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
		emptyText.setText(result);

		setTransfers();
		
//		adapter = new MegaTransfersAdapter(context, transfersListArray, aB);
		adapter = new MegaTransfersLollipopAdapter(context, this, tL, listView);

		adapter.setMultipleSelect(false);
		listView.setAdapter(adapter);

		return v;
	}

	public void setTransfers(){
		logDebug("setTransfers");

		for(int i=0; i<((ManagerActivityLollipop)context).transfersInProgress.size();i++){
			MegaTransfer transfer = megaApi.getTransferByTag(((ManagerActivityLollipop)context).transfersInProgress.get(i));
			if (transfer != null) {
				if (!transfer.isStreamingTransfer()) {
					tL.add(transfer);
				}
			}
		}

		if (tL.size() == 0){
			emptyImage.setVisibility(View.VISIBLE);
			emptyText.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);
		}
		else{
			emptyImage.setVisibility(View.GONE);
			emptyText.setVisibility(View.GONE);
			listView.setVisibility(View.VISIBLE);
		}
	}

	public void refreshAllTransfers(){
		logDebug("refreshAllTransfers");
		tL.clear();

		for(int i=0; i<((ManagerActivityLollipop)context).transfersInProgress.size();i++){
			MegaTransfer transfer = megaApi.getTransferByTag(((ManagerActivityLollipop)context).transfersInProgress.get(i));
			if(!transfer.isStreamingTransfer()){
				tL.add(transfer);
			}
		}

		if (tL.size() == 0){
			emptyImage.setVisibility(View.VISIBLE);
			emptyText.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);
		}
		else{
			emptyImage.setVisibility(View.GONE);
			emptyText.setVisibility(View.GONE);
			listView.setVisibility(View.VISIBLE);
		}

		adapter.notifyDataSetChanged();
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

	public void transferUpdate(MegaTransfer transfer){
		logDebug("Node handle: " + transfer.getNodeHandle());
        try{
			ListIterator li = tL.listIterator();
			int index = 0;
			while(li.hasNext()) {
				MegaTransfer next = (MegaTransfer) li.next();
				if(next!=null){
					if(next.getTag() == transfer.getTag()){
						index=li.previousIndex();
						break;
					}
				}
			}
			tL.set(index, transfer);
			logDebug("Update the transfer with index: " + index + ", left: " + tL.size());

			adapter.updateProgress(index, transfer);
		}
        catch(IndexOutOfBoundsException e){
			logError("EXCEPTION", e);
		}
    }

	public void changeStatusButton(int tag){
		logDebug("tag: " + tag);

		ListIterator li = tL.listIterator();
		int index = 0;
		while(li.hasNext()) {
			MegaTransfer next = (MegaTransfer) li.next();
			if(next.getTag() == tag){
				index=li.previousIndex();
				break;
			}
		}
		MegaTransfer transfer = megaApi.getTransferByTag(tag);
		tL.set(index, transfer);
		logDebug("The transfer with index : " + index + "has been paused/resumed, left: " + tL.size());

		adapter.notifyItemChanged(index);
	}

    public void transferFinish(int transferTag){
		logDebug("transferTag is " + transferTag);
		int position = -1;
        for (int i=0; i<tL.size(); i++) {
            MegaTransfer transfer = tL.get(i);
            if(transfer != null && transfer.getTag() == transferTag){
                position = i;
                break;
            }
        }

		if(position == -1){
		    return;
        }

		if(!tL.isEmpty() && position < tL.size()){
			tL.remove(position);
		}

		adapter.removeItemData(position);

		if (tL.size() == 0){
			emptyImage.setVisibility(View.VISIBLE);
			emptyText.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);
		}
	}

	public void transferStart(MegaTransfer transfer){
		logDebug("Node handle: " + transfer.getNodeHandle());
		if(!transfer.isStreamingTransfer()){
			tL.add(transfer);
		}

		adapter.notifyItemInserted(tL.size()-1);

		if (tL.size() == 0){
			emptyImage.setVisibility(View.VISIBLE);
			emptyText.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);
		}
		else{
			emptyImage.setVisibility(View.GONE);
			emptyText.setVisibility(View.GONE);
			listView.setVisibility(View.VISIBLE);
		}
	}
}
