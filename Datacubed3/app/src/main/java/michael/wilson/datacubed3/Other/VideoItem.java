package michael.wilson.datacubed3.Other;

import java.util.Date;

/** VideoItem
 *
 * Represents a recording to be shown in the 'Saved Recordings' tab.
 */
public class VideoItem
{
    /** String path to the video file */
    private String mFilepath;

    /** Name of the recording */
    private String mName;

    /** Date and time this recording was made **/
    private Date mTimestamp;

    /** Duration of this recording in seconds **/
    private int mDuration;

    /** VideoItem()
     *
     * Instantiates this 'VideoItem' object.
     *
     * @param _filepath Path to the video file this recording will represent.
     * @param _name Name of the recording.
     * @param _duration Duration of the recording in seconds.
     * @param _timestamp Date and time the recording was made.
     */
    public VideoItem(String _filepath, String _name, int _duration, Date _timestamp)
    {
        this.mFilepath = _filepath;
        this.mName = _name;
        this.mDuration = _duration;
        this.mTimestamp = _timestamp;
    }

    /** destroy()
     *
     * Destroys this VideoItem but nullifying all member objects.
     */
    public void destroy()
    {
        mDuration = -1;
        mTimestamp = null;
        mName = null;
        mFilepath = null;
    }

    /** Getter methods **/
    public String getName() { return this.mName; }
    public int getDuration() { return this.mDuration; }
    public Date getTimestamp() { return this.mTimestamp; }
    public String getFilepath() { return this.mFilepath; }
}
