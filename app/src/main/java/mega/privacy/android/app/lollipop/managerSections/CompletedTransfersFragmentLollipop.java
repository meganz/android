package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
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

import mega.privacy.android.app.AndroidCompletedTransfer;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaCompletedTransfersAdapter;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;


public class CompletedTransfersFragmentLollipop extends Fragment {

	Context context;
	RecyclerView listView;
	MegaCompletedTransfersAdapter adapter;
	
	MegaApiAndroid megaApi;
	TextView contentText;
	ImageView emptyImage;
	TextView emptyText;

	float density;
	DisplayMetrics outMetrics;
	Display display;
	
	LinearLayoutManager mLayoutManager;

	DatabaseHandler dbH;
	
	CompletedTransfersFragmentLollipop transfersFragment = this;
	
//	SparseArray<TransfersHolder> transfersListArray = null;

	public ArrayList<AndroidCompletedTransfer> tL = null;

	private Handler handler;
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		tL = new ArrayList<AndroidCompletedTransfer>();
		dbH = DatabaseHandler.getDbHandler(context);

		super.onCreate(savedInstanceState);
		log("onCreate");		
	}

	public static CompletedTransfersFragmentLollipop newInstance() {
		log("newInstance");
		CompletedTransfersFragmentLollipop fragment = new CompletedTransfersFragmentLollipop();
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {  
		
		super.onCreateView(inflater, container, savedInstanceState);
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
	    
		View v = inflater.inflate(R.layout.fragment_transfers, container, false);

		listView = (RecyclerView) v.findViewById(R.id.transfers_list_view);
		listView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
		mLayoutManager = new LinearLayoutManager(context);
		listView.setHasFixedSize(true);
		listView.setItemAnimator(new DefaultItemAnimator());
		listView.setLayoutManager(mLayoutManager);
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

//		emptyImage.setImageResource(R.drawable.ic_no_active_transfers);
//		emptyText.setText(getString(R.string.completed_transfers_empty));


		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			emptyImage.setImageResource(R.drawable.ic_zero_landscape_saved_for_offline);
		}else{
			emptyImage.setImageResource(R.drawable.ic_zero_portrait_transfers);
		}

		String textToShow = String.format(context.getString(R.string.completed_transfers_empty_new));
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


		setCompletedTransfers();

		adapter = new MegaCompletedTransfersAdapter(context, this, tL, listView);

		listView.setAdapter(adapter);

		return v;
	}

	public void setCompletedTransfers(){
		log("setCompletedTransfers");
		tL.clear();
		tL.addAll(dbH.getCompletedTransfers());

		if(tL!=null){
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
		else{
			emptyImage.setVisibility(View.VISIBLE);
			emptyText.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);
		}
	}

	public void updateCompletedTransfers(){
		log("updateCompletedTransfers");

		setCompletedTransfers();
		adapter.notifyDataSetChanged();
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

	public int onBackPressed(){
		
		if (adapter == null){
			return 0;
		}
		
		if (adapter.getPositionClicked() != -1){
			adapter.setPositionClicked(-1);
			adapter.notifyDataSetChanged();
			return 1;
		}
		else{
			return 0;
		}
	}

    public void transferFinish(AndroidCompletedTransfer transfer){
		log("transferFinish");
		if(tL!=null){
			tL.add(0,transfer);
		}
		else{
			tL = new ArrayList<AndroidCompletedTransfer>();
			tL.add(transfer);
		}

		if(tL.size()==1){
			((ManagerActivityLollipop)context).invalidateOptionsMenu();
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

	public boolean isAnyTransferCompleted (){
		if(tL!=null){
			if(tL.isEmpty()){
				return false;
			}
			else{
				return true;
			}
		}
		else{
			return false;
		}
	}
	private static void log(String log) {
		Util.log("CompletedTransfersFragmentLollipop", log);
	}

}
