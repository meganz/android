/*
 *  Customized RecyclerView based on:
 *
 *  https://github.com/chiuki/android-recyclerview/blob/master/app/src/main/java/com/sqisland/android/recyclerview/AutofitRecyclerView.java
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with the License. You may obtain a copy of the License in the LICENSE file, or at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package mega.privacy.android.app.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ViewGroup;

public class CustomizedGridRecyclerView extends RecyclerView {

	private CustomizedGridLayoutManager manager;
	public int columnWidth = -1;
	private boolean isWrapContent = false;
	private int widthTotal = 0;
	
	private int columnCount = 2;
    
    public CustomizedGridRecyclerView(Context context) {
		super(context);
		init(context, null);
	}

	public CustomizedGridRecyclerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public CustomizedGridRecyclerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}
	
	public void setColumnCount(int count) {
        this.columnCount = count;
    }

	private void init(Context context, AttributeSet attrs) {
		if (attrs != null) {
			int[] attrsArray = {
					android.R.attr.columnWidth
			};
			TypedArray array = context.obtainStyledAttributes(attrs, attrsArray);
			columnWidth = array.getDimensionPixelSize(0, -1);
			array.recycle();
		}

		manager = new CustomizedGridLayoutManager(getContext(), 1);
		setLayoutManager(manager);
	}

	@Override
	protected void onMeasure(int widthSpec, int heightSpec) {
		super.onMeasure(widthSpec, heightSpec);
		if(!isWrapContent){
			if (columnWidth > 0) {
				int spanCount = Math.max(1, getMeasuredWidth() / columnWidth);
				manager.setSpanCount(spanCount);
				if(spanCount == 1){
					manager.setSpanCount(columnCount);
				}else{
					manager.setSpanCount(spanCount);
				}
			}
		}
		else{
			ViewGroup.LayoutParams params = getLayoutParams();
			if (columnWidth > 0) {
				int spanCount = Math.max(1, getMeasuredWidth() / columnWidth);
				manager.setSpanCount(spanCount);
				if(spanCount == 1){
					manager.setSpanCount(columnCount);
				}else{
					manager.setSpanCount(spanCount);
				}
				params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
				setLayoutParams(params);
			}
		}
	}

	public void setWrapContent(){
		isWrapContent = true;
//		widthTotal = getMeasuredWidth();
//		ViewGroup.LayoutParams params = getLayoutParams();
//		params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
//		setLayoutParams(params);
		this.invalidate();
	}

	public int findFirstCompletelyVisibleItemPosition() {
		return getLayoutManager().findFirstCompletelyVisibleItemPosition();
	}

	public int findFirstVisibleItemPosition() {
		return getLayoutManager().findFirstVisibleItemPosition();
	}

	@Override
	public CustomizedGridLayoutManager getLayoutManager() {
		return manager;
	}
}