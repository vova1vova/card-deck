package com.redwayapps.carddecksample.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;

import com.redwayapps.carddecksample.R;
import com.redwayapps.carddecksample.fragments.MainFragment;
import com.redwayapps.carddecksample.fragments.MenuFragment;
import com.redwayapps.carddecksample.fragments.SubmenuFragment;
import com.snaprix.carddecklibrary.views.Side;
import com.snaprix.carddecklibrary.views.SlideContainer;

public class SampleActivity extends ActionBarActivity{
    private static final String FRAGMENT_MAIN = "fragment_main";
    private static final String FRAGMENT_MENU = "fragment_menu";
    private static final String FRAGMENT_SUB_MENU = "fragment_sub_menu";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SlideContainer slideContainer = new SlideContainer(this, Side.RIGHT);

        if (savedInstanceState == null){
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.content, new MainFragment(), FRAGMENT_MAIN)
                    .add(com.snaprix.carddecklibrary.R.id.card_2_interact, new MenuFragment(), FRAGMENT_MENU)
                    .add(com.snaprix.carddecklibrary.R.id.card_3_interact, new SubmenuFragment(), FRAGMENT_SUB_MENU)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
