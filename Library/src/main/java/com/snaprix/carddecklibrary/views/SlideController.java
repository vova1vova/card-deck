package com.snaprix.carddecklibrary.views;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.snaprix.carddecklibrary.CardDeckLibrary;
import com.snaprix.carddecklibrary.R;

import java.util.HashMap;

/**
 * Created by vladimirryabchikov on 10/4/13.
 */
public class SlideController extends LinearLayout{

    private static final String TAG = SlideController.class.getSimpleName();
    private static final boolean DEBUG = CardDeckLibrary.DEBUG;

    /**
     * since JELLY_BEAN map view implemented using TextureView
     * it fixes several bugs with black background behind the map
     * and allows map to be moved, transformed and animated in more smooth way
     */
    public static int BUILD_MAP_AS_TEXTURE_VIEW = Build.VERSION_CODES.JELLY_BEAN;
    private static final int SLIDE_DURATION = 333;

    // keys for saving/restoring instance state
    private final static String KEY_MENUSHOWN = "menuWasShown";
    private final static String KEY_SUPERSTATE = "superState";

    // this tells whether the menu is currently shown
    private boolean menuIsShown = false;
    // this just tells whether the menu was ever shown
    private boolean menuWasShown = false;

    private int mCollapsedWidth;

    /**
     * Slide in/out duration in milliseconds.
     */
    private int mSlideDuration;

    private SparseArray<SlideLayer> mLayers;
    private HashMap<SlideLayer, View> mLayerToContainer;
    private HashMap<SlideLayer, SlideLayer> mLayerToChild;

    private int mCurrentScrollX;

    private int mSide;

    private SlideLayer mBaseLayer;
    private SlideLayer mChildLayer;

    public SlideController(Activity act, int side) {
        super(act);

        mSide = side;

        // set size
        mCollapsedWidth = getResources().getDimensionPixelSize(R.dimen.menu_width_collapsed);
        mSlideDuration = SLIDE_DURATION;

        if (DEBUG) Log.v(TAG, String.format("init mCollapsedWidth {%d} slideDuration {%d}",
                mCollapsedWidth, mSlideDuration));

        mLayers = new SparseArray<>();
        mLayerToContainer = new HashMap<>();
        mLayerToChild = new HashMap<>();

        LayoutInflater inflater = (LayoutInflater) act.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarOverlay = (View) act.findViewById(android.R.id.content).getParent();

        FrameLayout activityParentView = (FrameLayout) actionBarOverlay.getParent();
        View slideContainerView = inflater.inflate(R.layout.slide_container, activityParentView, true);

        // create layers
        // todo may be do it in code (in cycle) do not load them from XML
        FrameLayout card1Container = (FrameLayout) slideContainerView.findViewById(R.id.card_1_container);
        FrameLayout card2Container = (FrameLayout) slideContainerView.findViewById(R.id.card_2_container);

        mBaseLayer = (SlideLayer) slideContainerView.findViewById(R.id.card_1_layer);
        mChildLayer = (SlideLayer) slideContainerView.findViewById(R.id.card_2_layer);
        SlideLayer card3Layer = (SlideLayer) slideContainerView.findViewById(R.id.card_3_layer);

        // return action bar
        activityParentView.removeView(actionBarOverlay);
        mBaseLayer.addView(actionBarOverlay);

        setupLayer(card1Container, mBaseLayer, mChildLayer, side);
        setupLayer(card2Container, mChildLayer, card3Layer, side);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state)	{
        super.onRestoreInstanceState(state != null ? ((Bundle) state).getParcelable(KEY_SUPERSTATE) : null);
        try{
            if (state instanceof Bundle) {
                Bundle bundle = (Bundle) state;

                if(bundle.getBoolean(KEY_MENUSHOWN)){
//                    show(false); // show without animation
                }
            }
        }
        catch(NullPointerException e) {
            // in case the menu was not declared via XML but added from code
        }
    }

    @Override
    protected Parcelable onSaveInstanceState()	{
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_SUPERSTATE, super.onSaveInstanceState());
        bundle.putBoolean(KEY_MENUSHOWN, menuIsShown);

        return bundle;
    }

    /**
     *
     * @param layer handles user gestures
     * @param container actually slides
     */
    private void setupLayer(FrameLayout container, SlideLayer layer, SlideLayer childLayer, int side){
        mLayerToContainer.put(layer, container);
        mLayerToChild.put(layer, childLayer);

        int layerNumber = layer.getLayerNumber();
        mLayers.put(layerNumber, layer);

        layer.setupLayer(side);
        layer.addDelegate(mLayerDelegate);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) childLayer.getLayoutParams();
        final int gravity;
        switch (side){
            case Side.LEFT:
                gravity = Gravity.LEFT;
                break;
            default:
                gravity = Gravity.RIGHT;
                break;
        }
        params.gravity = gravity;
        childLayer.setLayoutParams(params);
        setupPaddingTop(childLayer);
    }

    private void setupPaddingTop(SlideLayer layer){
        /*
         * todo it does not work properly all the time, because status bar could be on the bottom
         * of the screen (on tablets) but we apply padding on the top :(
         */

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
            // on Nexus S (with android 2.3) extra paddingTop NOT needed
        } else {
            // on Nexus 4 (with android 4.4 KitKat) and it's necessary
            // to add extra paddingTop to get proper layout, because the top part
            // of layout is overlaid by status bar
            int statusHeight = getStatusBarHeight();
            layer.setPadding(0, statusHeight, 0, 0);
        }
    }

    private int getStatusBarHeight() {
        int statusHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusHeight = getResources().getDimensionPixelSize(resourceId);
        }
        return statusHeight;
    }

    public void switchState(final SlideLayer layer, boolean nextOpened){
        View container = mLayerToContainer.get(layer);
        View childLayer = mLayerToChild.get(layer);

        final int targetScrollX;
        if (nextOpened){
            switch (mSide){
                case Side.LEFT:
                    // move from left to right (open)
                    targetScrollX = -childLayer.getWidth();
                    break;
                default:
                    targetScrollX = childLayer.getWidth();
                    break;
            }
        } else {
            // move from right to left (close)
            targetScrollX = 0;
        }

        if (DEBUG) Log.v(TAG, String.format("switchState targetScrollX=%d", targetScrollX));

        mCurrentScrollX = container.getScrollX();
        int duration = mSlideDuration;

        if (Build.VERSION.SDK_INT < BUILD_MAP_AS_TEXTURE_VIEW){
            int totalDeltaX = targetScrollX - mCurrentScrollX;
            int tick = 30;
            final int tickDeltaX = (int)(totalDeltaX * ((float)tick / duration));
            new CountDownTimer(duration, tick) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int x = mCurrentScrollX + tickDeltaX;
                    applyAnimation(layer, x);
                }

                @Override
                public void onFinish() {
                    applyAnimation(layer, targetScrollX);
                }
            }.start();
        } else {
            ValueAnimator animator = ValueAnimator.ofInt(mCurrentScrollX, targetScrollX);
            animator.setDuration(duration);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int x = (Integer)animation.getAnimatedValue();
                    applyAnimation(layer, x);
                }
            });
            animator.start();
        }
    }

    private void applyAnimation(SlideLayer layer, int x){
        int deltaX = x - mCurrentScrollX;
        mCurrentScrollX = x;
//        if (DEBUG) Log.v(TAG, String.format("applyAnimation x=%d deltaX=%d", x, deltaX));
        onScroll(layer, -deltaX);
    }

    private SlideLayer getNextLayer(SlideLayer layer){
        SlideLayer childLayer = getChildLayer(layer);

        boolean hasChildLayer = (childLayer != null);
        boolean isChildLayerOpened = false;
        if (hasChildLayer){
            isChildLayerOpened = isOpened(childLayer);
        }

        if (DEBUG) Log.v(TAG, String.format("getNextLayer layerNumber=%d hasChildLayer=%b isChildLayerOpened=%b",
                layer.getLayerNumber(), hasChildLayer, isChildLayerOpened));

        if (isChildLayerOpened){
            return getNextLayer(childLayer);
        } else {
            return layer;
        }
    }

    private SlideLayer getChildLayer(SlideLayer layer){
        return mLayerToChild.get(layer);
    }

    public SlideLayer getBaseLayer() {
        return mBaseLayer;
    }

    public SlideLayer getChildLayer() {
        return mChildLayer;
    }

    private SlideLayer.Delegate mLayerDelegate = new SlideLayer.Delegate() {
        @Override
        public boolean shouldInterceptEvents(SlideLayer layer) {
            // TODO: return true when layer is opened
            boolean shouldIntercept = isOpened(layer);

            int layerNumber = layer.getLayerNumber();
            if (DEBUG) Log.v(TAG, String.format("shouldInterceptEvents number=%d shouldIntercept=%b",
                    layerNumber, shouldIntercept));

            return shouldIntercept;
        }

        @Override
        public void onActionMove(SlideLayer layer, float distanceX) {
//            if (DEBUG) Log.v(TAG, String.format("onActionMove layerNumber=%d isTouchModeEnabled=%b",
//                    layerNumber, layer.isTouchModeEnabled()));

            if (layer.isTouchModeEnabled()){
                // pass events to lower child layer
                SlideLayer childLayer = getNextLayer(layer);
                boolean hasChildLayer = (!childLayer.equals(layer));
                if (hasChildLayer){
                    onActionMove(childLayer, distanceX);
                } else {
                    onScroll(layer, distanceX);
                }
            }
        }

        @Override
        public void onActionUp(SlideLayer layer, float distanceX) {
            if (layer.isTouchModeEnabled()){
                // pass events to lower child layer
                SlideLayer childLayer = getNextLayer(layer);
                if (childLayer.equals(layer)){
                    boolean nextStateOpened;
                    switch (mSide){
                        case Side.LEFT:
                            nextStateOpened = (distanceX > 0);
                            break;
                        default:
                            nextStateOpened = (distanceX < 0);
                            break;
                    }
                    switchState(layer, nextStateOpened);
                } else {
                    onActionUp(childLayer, distanceX);
                }
            }
        }
    };

    private void onScroll(SlideLayer activeLayer, float distanceX){
        int layerNumber = activeLayer.getLayerNumber();
        if (DEBUG) Log.v(TAG, String.format("onScroll layerNumber=%d", layerNumber));

        View activeContainer = mLayerToContainer.get(activeLayer);

        for (int i = layerNumber; i > 0; i--){
            View layer = mLayers.get(i);
            if (layer != null){
                View container = mLayerToContainer.get(layer);
                View childLayer = mLayerToChild.get(layer);

                boolean activeCard = (i == layerNumber);
                int maxScrollX;

                int adjDeltaX = (int)distanceX;
                if (activeCard){
                    maxScrollX = childLayer.getWidth();
                } else {
                    final int activeLayerWidth = activeLayer.getWidth();
                    final int distanceSide;
                    final int viewMaxScroll = layer.getWidth() - i * mCollapsedWidth;

                    switch (mSide){
                        case Side.LEFT:
                            // distance from left side
                            distanceSide = activeLayerWidth - activeContainer.getScrollX();
                            if (distanceX < 0){
                                // move from right to left
                                int scrollX = container.getScrollX();
                                if (-scrollX < activeLayerWidth){
                                    adjDeltaX = 0;
                                }
                            }

                            if (distanceSide > viewMaxScroll){
                                maxScrollX = viewMaxScroll;
                            } else {
                                maxScrollX = distanceSide;
                            }
                            break;
                        default:
                            // todo check signs

                            // todo distance from right side
                            distanceSide = activeLayerWidth + activeContainer.getScrollX();
                            if (distanceX > 0){
                                // move from left to right
                                int scrollX = container.getScrollX();
                                if (-scrollX < activeLayerWidth){
                                    adjDeltaX = 0;
                                }
                            }

                            if (distanceSide > viewMaxScroll){
                                maxScrollX = viewMaxScroll;
                            } else {
                                maxScrollX = distanceSide;
                            }
                            break;
                    }

//                    if (DEBUG) Log.d(TAG, String.format("onScroll activeSlideView.getScrollX=%d width=%d distanceSide=%d viewMaxScroll=%d",
//                            activeSlideView.getScrollX(), interact.getWidth(), distanceSide, viewMaxScroll));
                }

                scrollByX(container, adjDeltaX, maxScrollX);
            } else {
                if (DEBUG) Log.w(TAG, String.format("onScroll v == null for tag %d", i));
            }
        }
    }

    /**
     *
     * @param container
     * @param delta - positive value, when moving from left to right;
     *              negative value, when moving from right to left
     * @param maxScrollX - positive value, max scroll available for this view
     */
    private void scrollByX(View container, int delta, int maxScrollX){
        int adjDelta = -delta;

        /**
         * scrollX - positive value, when left edge of the view is beyond the left edge of the screen
         *          negative value, when right edge is beyond the right edge of the screen
         */
        int scrollX = container.getScrollX();
        int nextScrollX = scrollX + adjDelta;
        switch (mSide){
            case Side.LEFT:
                if (nextScrollX > 0) {
                    // we will be beyond the left edge, return to the ground
                    adjDelta = -scrollX;
                } else {
                    // we will open up the lower layer too much, do not allow it
                    // nextScrollX = -600
                    // maxScrollX = 420
                    // scrollX = -400
                    if (nextScrollX < -maxScrollX){
                        adjDelta = -maxScrollX - scrollX;
                    }
                }
                break;
            default:
                if (nextScrollX < 0) {
                    // we will be beyond the right edge, return to the ground
                    adjDelta = -scrollX;
                } else {
                    // we will open up the lower layer too much, do not allow it
                    // nextScrollX = 600
                    // maxScrollX = 420
                    // scrollX = 400
                    if (nextScrollX > maxScrollX){
                        adjDelta = maxScrollX - scrollX;
                    }
                }
                break;
        }
//        if (DEBUG) Log.d(TAG, String.format("scrollByX delta=%d maxScrollX=%d scrollX=%d adjDelta=%d nextScrollX=%d",
//                delta, maxScrollX, scrollX, adjDelta, nextScrollX));
        container.scrollBy(adjDelta, 0);
    }

    public boolean isOpened(SlideLayer layer){
        View container = mLayerToContainer.get(layer);
        // container is null for the last layer in current implementation
        // this layer is always closed
        if (container == null) return true;

        int scrollX = container.getScrollX();
        boolean isClosed = (scrollX == 0);
        if (DEBUG) Log.v(TAG, String.format("isOpened layer=%d container scrollX=%d isOpened=%b",
                layer.getLayerNumber(), scrollX, isClosed));
        return !isClosed;

    }
}