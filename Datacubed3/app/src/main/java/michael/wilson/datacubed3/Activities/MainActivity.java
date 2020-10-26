package michael.wilson.datacubed3.Activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;

import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import michael.wilson.datacubed3.Other.Database;
import michael.wilson.datacubed3.R;
import michael.wilson.datacubed3.Adapters.TabAdapter;

/** MainActivity
 *
 * Created on application launch.
 */
public class MainActivity extends AppCompatActivity
{
    /** onCreate()
     *
     * Called when this activity is created.
     * - Setup the tabbed home page
     * - Load videos from storage into global recordings list
     */
    @Override
    protected void onCreate(Bundle _savedInstanceState)
    {
        super.onCreate(_savedInstanceState);

        // Use 'activity_main' layout
        setContentView(R.layout.activity_main);

        // Get references to UI elements (viewPager and tabLayout)
        ViewPager viewPager = findViewById(R.id.view_pager);
        TabLayout tabs = findViewById(R.id.tabs);

        // Load videos from external storage to global 'VIDEOS_LIST'
        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                Database.LOAD_VIDEOS(getApplicationContext());
            }
        };
        thread.start();


        // Setup tabs
        TabAdapter adapter = new TabAdapter(this, getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        tabs.setupWithViewPager(viewPager);
    }

    /** onDestroy()
     *
     * Called when the activity is about to close.
     */
    @Override
    protected void onDestroy()
    {
        // Destroy global 'VIDEOS_LIST'
        Database.DESTROY();
        super.onDestroy();
    }
}