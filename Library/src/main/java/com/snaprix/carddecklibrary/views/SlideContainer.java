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
public class SlideContainer extends LinearLayout
        implements SlideLayer.Listener {

    private static final String TAG = SlideContainer.class.getSimpleName();
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

    private SparseArray<View> interactViews;
    private HashMap<View, View> interactToSlideViews;
    private HashMap<SlideLayer, SlideLayer> interactToNextViews;

    private int mCurrentScrollX;

    private int mSide;

    private SlideLayer mBaseInteractLayer;
    private SlideLayer mClildInteractLayer;

    public SlideContainer(Activity act, int side) {
        super(act);

        mSide = side;

        // set size
        mCollapsedWidth = getResources().getDimensionPixelSize(R.dimen.menu_width_collapsed);
        mSlideDuration = SLIDE_DURATION;

        if (DEBUG) Log.v(TAG, String.format("init mCollapsedWidth {%d} slideDuration {%d}",
                mCollapsedWidth, mSlideDuration));

        interactViews = new SparseArray<View>();
        interactToSlideViews = new HashMap<View, View>();
        interactToNextViews = new HashMap<SlideLayer, SlideLayer>();

        LayoutInflater inflater = (LayoutInflater) act.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarOverlay = (View) act.findViewById(android.R.id.content).getParent();

        FrameLayout activityParentView = (FrameLayout) actionBarOverlay.getParent();
        View slideContainerView = inflater.inflate(R.layout.slide_container, activityParentView, true);

        // create layers
        // todo may be do it in code (in cycle) do not load them from XML
        FrameLayout card1Slide = (FrameLayout) slideContainerView.findViewById(R.id.card_1_slide);
        FrameLayout card2Slide = (FrameLayout) slideContainerView.findViewById(R.id.card_2_slide);

        mBaseInteractLayer = (SlideLayer) slideContainerView.findViewById(R.id.card_1_interact);
        mClildInteractLayer = (SlideLayer) slideContainerView.findViewById(R.id.card_2_interact);
        SlideLayer card3Interact = (SlideLayer) slideContainerView.findViewById(R.id.card_3_interact);

        // return action bar
        activityParentView.removeView(actionBarOverlay);
        mBaseInteractLayer.addView(actionBarOverlay);

        setupLayer(card1Slide, mBaseInteractLayer, mClildInteractLayer, side);
        setupLayer(card2Slide, mClildInteractLayer, card3Interact, side);
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
     * @param interactView handles user gestures
     * @param slideView actually slides
     */
    private void setupLayer(FrameLayout slideView, SlideLayer interactView, SlideLayer lowerInteractView, int side){
        interactToSlideViews.put(interactView, slideView);
        interactToNextViews.put(interactView, lowerInteractView);

        int layerNumber = interactView.getLayerNumber();
        interactViews.put(layerNumber, interactView);

        interactView.setupLayer(side);
        interactView.addListener(this);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lowerInteractView.getLayoutParams();
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
        lowerInteractView.setLayoutParams(params);
        setupPaddingTop(lowerInteractView);
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

    @Override
    public void onActionMove(SlideLayer interactView, float distanceX) {
        int layerNumber = interactView.getLayerNumber();
        if (DEBUG) Log.v(TAG, String.format("onActionMove layerNumber=%d isTouchModeEnabled=%b",
                layerNumber, interactView.isTouchModeEnabled()));

        if (interactView.isTouchModeEnabled()){
            // pass events to lower child layer
            SlideLayer childInteract = getNextLayer(interactView);
            if (childInteract.equals(interactView)){
                onScroll(interactView, distanceX);
            } else {
                onActionMove(childInteract, distanceX);
            }
        }
    }

    @Override
    public void onActionUp(SlideLayer interactView, float distanceX) {
        if (interactView.isTouchModeEnabled()){
            // pass events to lower child layer
            SlideLayer childInteract = getNextLayer(interactView);
            if (childInteract.equals(interactView)){
                boolean nextStateOpened;
                switch (mSide){
                    case Side.LEFT:
                        nextStateOpened = (distanceX > 0);
                        break;
                    default:
                        nextStateOpened = (distanceX < 0);
                        break;
                }
                switchState(interactView, nextStateOpened);
            } else {
                onActionUp(childInteract, distanceX);
            }
        }
    }

    public void switchState(final SlideLayer interactView, boolean nextOpened){
        View slideView = interactToSlideViews.get(interactView);
        View nextView = interactToNextViews.get(interactView);

        final int targetScrollX;
        if (nextOpened){
            switch (mSide){
                case Side.LEFT:
                    // move from left to right (open)
                    targetScrollX = -nextView.getWidth();
                    break;
                default:
                    targetScrollX = nextView.getWidth();
                    break;
            }
        } else {
            // move from right to left (close)
            targetScrollX = 0;
        }

        if (DEBUG) Log.v(TAG, String.format("switchState targetScrollX=%d", targetScrollX));

        mCurrentScrollX = slideView.getScrollX();
        int duration = mSlideDuration;

        if (Build.VERSION.SDK_INT < BUILD_MAP_AS_TEXTURE_VIEW){
            int totalDeltaX = targetScrollX - mCurrentScrollX;
            int tick = 30;
            final int tickDeltaX = (int)(totalDeltaX * ((float)tick / duration));
            new CountDownTimer(duration, tick) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int x = mCurrentScrollX + tickDeltaX;
                    applyAnimation(interactView, x);
                }

                @Override
                public void onFinish() {
                    applyAnimation(interactView, targetScrollX);
                }
            }.start();
        } else {
            ValueAnimator animator = ValueAnimator.ofInt(mCurrentScrollX, targetScrollX);
            animator.setDuration(duration);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int x = (Integer)animation.getAnimatedValue();
                    applyAnimation(interactView, x);
                }
            });
            animator.start();
        }
    }

    private void applyAnimation(SlideLayer interactView, int x){
        int deltaX = x - mCurrentScrollX;
        mCurrentScrollX = x;
//        if (DEBUG) Log.v(TAG, String.format("applyAnimation x=%d deltaX=%d", x, deltaX));
        onScroll(interactView, -deltaX);
    }

    private SlideLayer getNextLayer(SlideLayer interactView){
        SlideLayer nextLayerInteractView = interactToNextViews.get(interactView);

        boolean hasNextLayer = (nextLayerInteractView != null);
        boolean isNextLayerScrolled = false;
        if (hasNextLayer){
            View nextLayerSlideView = interactToSlideViews.get(nextLayerInteractView);
            if (nextLayerSlideView != null){
                isNextLayerScrolled = (nextLayerSlideView.getScrollX() != 0);
            }
        }

        if (DEBUG) Log.v(TAG, String.format("getNextLayer layerNumber=%d hasNextLayer=%b isNextLayerScrolled=%b",
                interactView.getLayerNumber(), hasNextLayer, isNextLayerScrolled));

        if (isNextLayerScrolled){
            return getNextLayer(nextLayerInteractView);
        } else {
            return interactView;
        }
    }

    private void onScroll(SlideLayer interact, float distanceX){
        int layerNumber = interact.getLayerNumber();
        if (DEBUG) Log.v(TAG, String.format("onScroll layerNumber=%d", layerNumber));

        View activeSlideView = interactToSlideViews.get(interact);

        for (int i = layerNumber; i > 0; i--){
            View v = interactViews.get(i);
            if (v != null){
                View slideView = interactToSlideViews.get(v);
                View childView = interactToNextViews.get(v);

                boolean activeCard = (i == layerNumber);
                int maxScrollX;

                int adjDeltaX = (int)distanceX;
                if (activeCard){
                    maxScrollX = childView.getWidth();
                } else {
                    final int interactWidth = interact.getWidth();
                    final int distanceSide;
                    final int viewMaxScroll = v.getWidth() - i * mCollapsedWidth;

                    switch (mSide){
                        case Side.LEFT:
                            // distance from left side
                            distanceSide = interactWidth - activeSlideView.getScrollX();
                            if (distanceX < 0){
                                // move from right to left
                                int scrollX = slideView.getScrollX();
                                if (-scrollX < interactWidth){
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
                            distanceSide = interactWidth + activeSlideView.getScrollX();
                            if (distanceX > 0){
                                // move from left to right
                                int scrollX = slideView.getScrollX();
                                if (-scrollX < interactWidth){
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

                scrollByX(slideView, adjDeltaX, maxScrollX);
            } else {
                if (DEBUG) Log.w(TAG, String.format("onScroll v == null for tag %d", i));
            }
        }
    }

    /**
     *
     * @param slideView
     * @param delta - positive value, when moving from left to right;
     *              negative value, when moving from right to left
     * @param maxScrollX - positive value, max scroll available for this view
     */
    private void scrollByX(View slideView, int delta, int maxScrollX){
        int adjDelta = -delta;

        /**
         * scrollX - positive value, when left edge of the view is beyond the left edge of the screen
         *          negative value, when right edge is beyond the right edge of the screen
         */
        int scrollX = slideView.getScrollX();
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
        slideView.scrollBy(adjDelta, 0);
    }

    public SlideLayer getBaseInteractLayer() {
        return mBaseInteractLayer;
    }

    public SlideLayer getClildInteractLayer() {
        return mClildInteractLayer;
    }
}
