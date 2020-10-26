package michael.wilson.datacubed3.Adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import michael.wilson.datacubed3.Fragments.GalleryTabFragment;
import michael.wilson.datacubed3.Fragments.RecordTabFragment;
import michael.wilson.datacubed3.R;

/** TabAdapter
 *
 *  Used by a view pager to create a tabbed layout.
 *  This one creates the 'RECORD' and 'SAVED RECORDINGS' tabs.
 */
public class TabAdapter extends FragmentPagerAdapter
{
    /** Tab titles **/
    private static final int[] TAB_TITLES = new int[]{R.string.tab_name_0, R.string.tab_name_1};

    /** Context **/
    private final Context mContext;

    /** SectionsPagerAdapter()
     *
     *  Instantiates this 'TabAdapter'
     * @param _context The context
     * @param _fm The fragment manager?
     */
    public TabAdapter(Context _context, FragmentManager _fm)
    {
        super(_fm);
        mContext = _context;
    }

    /** getItem()
     *
     * @param _index Index of the tab.
     * @return View fragment of the given tab index.
     */
    @NonNull
    @Override
    public Fragment getItem(int _index)
    {
        Fragment output = null;

        // If index is 0, use 'RecordTabFragment'
        // If index is 1, use 'GalleryTabFragment'
        switch (_index)
        {
            case 0: output = new RecordTabFragment(); break;
            case 1: output = new GalleryTabFragment(); break;
        }

        return output;
    }

    /** getPageTitle()
     *
     * @param _index Index of the tab.
     * @return Title of the tab at the given index.
     */
    @Nullable
    @Override
    public CharSequence getPageTitle(int _index)
    {
        // Return tab title for the given index
        return mContext.getResources().getString(TAB_TITLES[_index]);
    }

    /** getCount()
     *
     * @return The number of tabs to create.
     */
    @Override
    public int getCount()
    {
        // 2 tabs
        return 2;
    }
}