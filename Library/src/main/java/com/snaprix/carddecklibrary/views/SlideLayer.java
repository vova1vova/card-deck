package com.snaprix.carddecklibrary.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import com.snaprix.carddecklibrary.R;


/**
 * Created by vladimirryabchikov on 10/12/13.
 */
public class SlideLayer extends FrameLayout {
    public interface FrameLayoutListener{
        void onActionMove(SlideLayer view, float distanceX);
        void onActionUp(SlideLayer view, float distanceX);
    }

    private static final String TAG = SlideLayer.class.getSimpleName();
    private static boolean DEBUG;

    private GestureDetector gd;

    private boolean isScrollHorizontal;

    private float mLastMotionX;
    private float mInitialMotionX;

    private FrameLayoutListener mListener;

    private int mPagingTouchSlop;
    private int mTouchRegionWidth;

    private int mSide;

    private int mLayerNumber;
    private boolean mIsTouchModeEnabled;

    public SlideLayer(Context context, AttributeSet attrs) {
        super(context, attrs);

        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mPagingTouchSlop = viewConfiguration.getScaledPagingTouchSlop();
        mTouchRegionWidth = getResources().getDimensionPixelSize(R.dimen.slide_layer_touch_width);

        gd = new GestureDetector(context, mGestureListener);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlideLayer);
        if (a != null) {
            int n = a.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = a.getIndex(i);

                if (attr == R.styleable.SlideLayer_layerNumber){
                    mLayerNumber = a.getInt(attr, -1);
                } else if (attr == R.styleable.SlideLayer_isTouchModeEnabled){
                    mIsTouchModeEnabled = a.getBoolean(attr, true);
                }
            }
        }
        a.recycle();
    }

    private GestureDetector.OnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener(){
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float absDeltaX = Math.abs(distanceX);

            boolean startedInTouchRegion;
            switch (mSide){
                case Side.LEFT:
                    startedInTouchRegion = (e1.getX() < mTouchRegionWidth);
                    break;
                default:
                    startedInTouchRegion = ((getWidth() - e1.getX()) < mTouchRegionWidth);
                    break;
            }

            if (DEBUG) Log.v(TAG, String.format("onScroll absDeltaX={%f} e1.X={%f} startedInTouchRegion %b",
                    absDeltaX, e1.getX(), startedInTouchRegion));
            if (startedInTouchRegion && absDeltaX > Math.abs(distanceY)) {
                isScrollHorizontal = true;
            }

            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    };

    public boolean isTouchModeEnabled() {
        return mIsTouchModeEnabled;
    }

    public int getLayerNumber() {
        return mLayerNumber;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        switch (action){
            case MotionEvent.ACTION_DOWN:
                isScrollHorizontal = false;
                mLastMotionX = mInitialMotionX = ev.getRawX();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                onCancel();
                break;
        }

        // called from here in order to find out event type, and decide should it be intercepted
        gd.onTouchEvent(ev);

        boolean intercept = isScrollHorizontal;
        if (intercept){
            onCancel();
        }
//        if (DEBUG) Log.d(TAG, String.format("onInterceptTouchEvent %s return %b", actionToString(ev.getAction()), intercept));
        return intercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();
//        if (DEBUG) Log.d(TAG, String.format("%s rawX %f X %f", actionToString(action), event.getRawX(), event.getX()));
        float x;
        float distanceX;
        switch (action){
            case MotionEvent.ACTION_MOVE:
                x = event.getRawX();
                distanceX = x - mLastMotionX;
                mLastMotionX = x;

                if (mListener != null){
                    mListener.onActionMove(this, distanceX);
                }
                break;
            case MotionEvent.ACTION_UP:
                x = event.getRawX();
                distanceX = x - mInitialMotionX;

                if (mListener != null) {
                    mListener.onActionUp(this, distanceX);
                }
                break;
        }
        return true;
    }

    private void onCancel(){
        isScrollHorizontal = false;
    }

    public void setupLayer(FrameLayoutListener listener, int side, boolean debug) {
        this.mListener = listener;
        mSide = side;
        DEBUG = debug;
    }

    public static String actionToString(int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return "ACTION_DOWN";
            case MotionEvent.ACTION_UP:
                return "ACTION_UP";
            case MotionEvent.ACTION_CANCEL:
                return "ACTION_CANCEL";
            case MotionEvent.ACTION_OUTSIDE:
                return "ACTION_OUTSIDE";
            case MotionEvent.ACTION_MOVE:
                return "ACTION_MOVE";
            case MotionEvent.ACTION_HOVER_MOVE:
                return "ACTION_HOVER_MOVE";
            case MotionEvent.ACTION_SCROLL:
                return "ACTION_SCROLL";
            case MotionEvent.ACTION_HOVER_ENTER:
                return "ACTION_HOVER_ENTER";
            case MotionEvent.ACTION_HOVER_EXIT:
                return "ACTION_HOVER_EXIT";
        }
        int index = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                return "ACTION_POINTER_DOWN(" + index + ")";
            case MotionEvent.ACTION_POINTER_UP:
                return "ACTION_POINTER_UP(" + index + ")";
            default:
                return Integer.toString(action);
        }
    }
}
