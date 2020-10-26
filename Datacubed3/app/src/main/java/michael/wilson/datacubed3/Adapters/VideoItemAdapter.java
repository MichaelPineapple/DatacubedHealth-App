package michael.wilson.datacubed3.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import michael.wilson.datacubed3.Other.Database;
import michael.wilson.datacubed3.Other.VideoItem;
import michael.wilson.datacubed3.R;

/** VideoItemAdapter
 *
 * Used to create a views which represent a list of 'VideoItem' objects
 */
public class VideoItemAdapter extends ArrayAdapter<VideoItem>
{
    /** The context **/
    private Context mContext;

    /** ID of the layout to represent a single 'VideoItem' **/
    private int mResourceId;

    /** VideoItemAdapter()
     *
     * @param _context The context.
     * @param _resource  ID of the layout to represent a single 'VideoItem'
     * @param _objects The list of 'VideoItem' objects.
     */
    public VideoItemAdapter(Context _context, int _resource, ArrayList<VideoItem> _objects)
    {
        super(_context, _resource, _objects);
        this.mContext = _context;
        this.mResourceId = _resource;
    }

    /** getView()
     *
     * @param _position Position within the list of 'VideoItem' objects
     * @param _convertView Layout to represent the 'VideoItem' at the given position
     * @param _parent Parent view which contains the list of layouts
     * @return Newly created/updated layout representing the 'VideoItem' at the given position
     */
    @NonNull
    @Override
    public View getView(int _position, @Nullable View _convertView, @NonNull ViewGroup _parent)
    {
        // If the layout has not been created, create a new one.
        if (_convertView == null)
        {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            _convertView = inflater.inflate(mResourceId, _parent, false);
        }

        // Reference to the recording (VideoItem) this layout will represent.
        VideoItem tmp = getItem(_position);

        // Set the name label to display the recording name and duration
        final TextView lbl_name = _convertView.findViewById(R.id.lbl_name);
        lbl_name.setText(tmp.getName() + " ("+ Database.FORMAT_CLOCK(tmp.getDuration())+")");

        // Set the timestamp label to display the date and time this recording was made
        final TextView lbl_timestamp = _convertView.findViewById(R.id.lbl_timestamp);
        String timeStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(tmp.getTimestamp());
        lbl_timestamp.setText(timeStr);

        // Set the thumbnail image to display the thumbnail of the recording
        final ImageView img_thumb = _convertView.findViewById(R.id.img_thumb);
        Glide.with(getContext()).load(tmp.getFilepath()).diskCacheStrategy(DiskCacheStrategy.NONE).into(img_thumb);

        return _convertView;
    }
}
