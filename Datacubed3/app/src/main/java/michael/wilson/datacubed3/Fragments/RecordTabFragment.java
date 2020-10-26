package michael.wilson.datacubed3.Fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import michael.wilson.datacubed3.Activities.CameraActivity;
import michael.wilson.datacubed3.Other.Database;
import michael.wilson.datacubed3.R;

/** RecordTabFragment
 *
 * Fragment for the 'RECORD' tab in the home screen.
 */
public class RecordTabFragment extends Fragment
{
    /** Track if the user should be able to press the 'START RECORDING' button.
     *  Set to false when the button is clicked and set to true onResume.
     **/
    private boolean recordButtonEnabled = true;

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
        // Inflate 'fragment_record' layout
        View root = _inflater.inflate(R.layout.fragment_record, _container, false);

        // Get references to UI elements
        final Button btn_record = root.findViewById(R.id.btn_record);
        final EditText txtbx_name = root.findViewById(R.id.txtbx_name);
        final SeekBar seek_duration = root.findViewById(R.id.seek_duration);
        final TextView lbl_duration = root.findViewById(R.id.lbl_duration);

        // Set the duration label to display the current slider value in clock format
        lbl_duration.setText(Database.FORMAT_CLOCK(seek_duration.getProgress()));
        seek_duration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar _seekBar, int _progress, boolean _fromUser)
            {
                lbl_duration.setText(Database.FORMAT_CLOCK(_progress));
            }

            @Override public void onStartTrackingTouch(SeekBar _seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar _seekBar) { }
        });

        // Create listener for when user taps the 'START RECORDING' button.
        btn_record.setOnClickListener(new Button.OnClickListener()
        {
            @Override
            public void onClick(View _v)
            {
                if (recordButtonEnabled)
                {
                    // Disable button
                    recordButtonEnabled = false;

                    // Get recording name from UI. If its empty, use default name.
                    String name = txtbx_name.getText().toString();
                    if (name.length() < 1) name = getString(R.string.untitled);

                    // Get duration value from UI
                    int duration = seek_duration.getProgress();

                    // Start the Camera Activity. Pass it the recording name and duration values
                    Intent openCameraIntent = new Intent(getContext(), CameraActivity.class);
                    openCameraIntent.putExtra(CameraActivity.KEY_DURATION, duration);
                    openCameraIntent.putExtra(CameraActivity.KEY_NAME, name);
                    startActivity(openCameraIntent);
                }
            }
        });

        return root;
    }

    /** onResume()
     *
     * Called when this fragment comes back into focus.
     */
    @Override
    public void onResume()
    {
        super.onResume();

        // Allow user to press 'START RECORDING' button
        recordButtonEnabled = true;
    }

}