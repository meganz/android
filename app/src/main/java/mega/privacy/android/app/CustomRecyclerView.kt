package mega.privacy.android.app

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewParent
import androidx.annotation.Nullable
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2


class CustomRecyclerView : RecyclerView {
    private var mTouchSlop: Int = 0

    internal var move_x: Int = 0
    internal var move_y: Int = 0
    internal var x = 0f//初始化按下时坐标变量
    internal var y = 0f//初始化按下时坐标变量

    constructor(context: Context) : super(context) {
        val vc = ViewConfiguration.get(context)
        mTouchSlop = vc.scaledTouchSlop
    }

    constructor(context: Context, @Nullable attrs: AttributeSet) : super(context, attrs) {
        val vc = ViewConfiguration.get(context)
        mTouchSlop = vc.scaledTouchSlop
    }

//    override fun onTouchEvent(e: MotionEvent): Boolean {
//        Log.e("motion_event_mTouchSlop", mTouchSlop.toString())
//        when (e.action) {
//
//            MotionEvent.ACTION_DOWN -> {
//                move_x = e.x.toInt()
//                move_y = e.y.toInt()
//                parent.requestDisallowInterceptTouchEvent(true)
//                Log.e("motion_event", "down   x==y  $move_x ==== $move_y")
//            }
//            MotionEvent.ACTION_MOVE -> {
//                Log.e("motion_event", "move   x==y  $move_x ==== $move_y")
//                val y = e.y.toInt()
//                val x = e.x.toInt()
//                if (Math.abs(y - move_y) > mTouchSlop || Math.abs(x - move_x) < mTouchSlop * 2) {
//                    Log.i("Alex", "false")
//                    parent.requestDisallowInterceptTouchEvent(false)
//                } else {
//                    //告诉父控件不要拦截 子控件的操作
//                    Log.i("Alex", "true")
//                    parent.requestDisallowInterceptTouchEvent(true)
//                }
//            }
//            MotionEvent.ACTION_UP -> {
////                performClick()
////                parent.requestDisallowInterceptTouchEvent(true)
//                Log.e("motion_event", "up   x==y  $move_x ==== $move_y")
//            }
//        }
//        return super.onTouchEvent(e)
//    }

    override fun dispatchTouchEvent(e: MotionEvent?): Boolean {
//        if (!canScrollHorizontally(1)) {
//            Log.i("Alex", "return")
//            parent?.requestDisallowInterceptTouchEvent(false)
//            return super.dispatchTouchEvent(e)
//        }

        parent?.requestDisallowInterceptTouchEvent(true)
        when (e?.action) {
            MotionEvent.ACTION_DOWN -> {
                move_x = e.x.toInt()
                move_y = e.y.toInt()
                Log.e("motion_event", "down   x==y  $move_x ==== $move_y")
            }
            MotionEvent.ACTION_MOVE -> {
                Log.e("motion_event", "move   x==y  $move_x ==== $move_y")
                val y = e.y.toInt()
                val x = e.x.toInt()
                if (Math.abs(y - move_y) > mTouchSlop) {
                    Log.i("Alex", "false")
                    parent?.requestDisallowInterceptTouchEvent(false)
                }

                if ((move_x - x > mTouchSlop) && !canScrollHorizontally(1)) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    Log.i("Alex", "scroll right")
                }
            }
        }

        val res = super.dispatchTouchEvent(e)
//        Log.i("Alex", "dispatch return:" + res)
        return res
    }

}
