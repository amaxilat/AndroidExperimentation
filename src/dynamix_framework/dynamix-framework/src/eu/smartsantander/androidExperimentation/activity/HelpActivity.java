package eu.smartsantander.androidExperimentation.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

import org.ambientdynamix.core.R;

import eu.smartsantander.androidExperimentation.util.CustomPagerAdapter;

/**
 * Help for new users.
 */
public class HelpActivity extends Activity {
    private CustomPagerAdapter mCustomPagerAdapter;
    private ViewPager mViewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);

        mCustomPagerAdapter = new CustomPagerAdapter(this);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mCustomPagerAdapter);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
