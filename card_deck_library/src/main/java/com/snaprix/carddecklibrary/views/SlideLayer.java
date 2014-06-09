package com.snaprix.carddecklibrary.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import com.snaprix.carddecklibrary.CardDeckLibrary;
import com.snaprix.carddecklibrary.R;


/**
 * Created by vladimirryabchikov on 10/12/13.
 */
public class SlideLayer extends FrameLayout {
    /**
     * class implementing this interface will be responsible for scrolling this layer
     */
    interface Delegate {
        boolean shouldInterceptEvents(SlideLayer layer);
        void onActionMove(SlideLayer layer, float distanceX);
        void onActionUp(SlideLayer layer, float distanceX);
    }

    private static final String TAG = SlideLayer.class.getSimpleName();
    private static final boolean DEBUG = CardDeckLibrary.DEBUG;

    private GestureDetector gd;

    private boolean isScrollHorizontal;

    private float mLastMotionX;
    private float mInitialMotionX;

    private Delegate mDelegate;

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

//        if (DEBUG){
//            int colorResId;
//            switch (mLayerNumber){
//                case 1:
//                    colorResId = R.color.holo_layer_1;
//                    break;
//                case 2:
//                    colorResId = R.color.holo_layer_2;
//                    break;
//                case 3:
//                    colorResId = R.color.holo_layer_3;
//                    break;
//                default:
//                    colorResId = R.color.holo_layer_4;
//                    break;
//            }
//            setForeground(new ColorDrawable(getResources().getColor(colorResId)));
//        }
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

    public void setTouchModeEnabled(boolean isEnabled) {
        mIsTouchModeEnabled = isEnabled;
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

        // delegate is set not for all layers
        boolean willDelegateIntercept = mDelegate != null && mDelegate.shouldInterceptEvents(this);
        boolean intercept = isScrollHorizontal || willDelegateIntercept;
        if (intercept){
            onCancel();
        }
        if (DEBUG) Log.d(TAG, String.format("onInterceptTouchEvent this=%s %s intercept=%b",
                this, actionToString(ev.getAction()), intercept));
        return intercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();

        if (DEBUG) Log.v(TAG, String.format("onTouchEvent this=%s %s rawX %f X %f",
                this, actionToString(action), event.getRawX(), event.getX()));
        float x;
        float distanceX;
        switch (action){
            case MotionEvent.ACTION_MOVE:
                x = event.getRawX();
                distanceX = x - mLastMotionX;
                mLastMotionX = x;

                mDelegate.onActionMove(this, distanceX);
                break;
            case MotionEvent.ACTION_UP:
                x = event.getRawX();
                distanceX = x - mInitialMotionX;

                mDelegate.onActionUp(this, distanceX);
                break;
        }
        return true;
    }

    private void onCancel(){
        isScrollHorizontal = false;
    }

    public void setupLayer(int side){
        mSide = side;
    }

    public void addDelegate(Delegate delegate){
        if (DEBUG) Log.v(TAG, String.format("addDelegate this=%s delegate=%s", this, delegate));
        mDelegate = delegate;
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
