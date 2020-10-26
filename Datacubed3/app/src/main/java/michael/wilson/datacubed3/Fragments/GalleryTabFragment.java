package michael.wilson.datacubed3.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import michael.wilson.datacubed3.Other.Database;
import michael.wilson.datacubed3.R;
import michael.wilson.datacubed3.Adapters.VideoItemAdapter;

/** GalleryTabFragment
 *
 * Fragment for the 'SAVED RECORDINGS' tab in the home screen.
 */
public class GalleryTabFragment extends Fragment
{
    /** 'VideoItem' array adapter for displaying the list of recordings **/
    private VideoItemAdapter mListAdapter = null;

    /** Toast message to appear when user taps on recording **/
    private Toast mPlaybackToast;

    /** onCreateView()
     *
     * Called when this fragment is crated.
     * @param _inflater Inflater object to inflate the layout.
     * @param _container The parent view of this fragment.
     * @param _savedInstanceState The saved instance state bundle.
     * @return Newly created view of this fragment.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater _inflater, ViewGroup _container, Bundle _savedInstanceState)
    {
        // Inflate 'fragment_gallery' layout
        View root = _inflater.inflate(R.layout.fragment_gallery, _container, false);

        // Set the UI to display recordings (VideoItems) from the global 'VIDEOS_LIST'
        mListAdapter = new VideoItemAdapter(getContext(), R.layout.layout_videoitem, Database.VIDEOS_LIST);
        final GridView ui_grid = root.findViewById(R.id.ui_grid);
        ui_grid.setAdapter(mListAdapter);

        // Set listener for when user taps one of the recordings
        ui_grid.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> _parent, View _view, int position, long _id)
            {
                // Inform the user that playback is not yet implemented
                if(mPlaybackToast != null) mPlaybackToast.cancel();
                mPlaybackToast = Toast.makeText(getContext(), R.string.toast_playback, Toast.LENGTH_SHORT);
                mPlaybackToast.show();
            }
        });

        return root;
    }

    /** onResume()
     *
     * Called when this fragment comes back into focus.
     * - Refreshes the list of recordings.
     */
    @Override
    public void onResume()
    {
        // Refresh the list of recordings. If there are no recordings, display the appropriate label
        if (mListAdapter != null)
        {
            mListAdapter.notifyDataSetChanged();
            if (!mListAdapter.isEmpty()) getActivity().findViewById(R.id.lbl_empty).setVisibility(View.INVISIBLE);
        }
        super.onResume();
    }

    /** onDestroy()
     *
     * Called when the fragment is about to close.
     */
    @Override
    public void onDestroy()
    {
        // Nullify member objects
        if (mListAdapter != null)
        {
            mListAdapter.clear();
            mListAdapter = null;
        }

        if (mPlaybackToast != null)
        {
            mPlaybackToast.cancel();
            mPlaybackToast = null;
        }

        super.onDestroy();
    }
}
