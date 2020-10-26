package michael.wilson.datacubed3.Other;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;

/** Database
 *
 * A singleton class to statically store the list of recordings to be shown in the 'Saved Recordings' tab.
 * Provides static methods to modify the list and some additional helper functions.
 */
public class Database
{
    /* ~~~ PUBLIC VARIABLES ~~~ */

    /** Global list of 'VideoItems' for use throughout the application **/
    public static ArrayList<VideoItem> VIDEOS_LIST;

    /* ~~~ PUBLIC METHODS ~~~ */

    /** LOAD_VIDEOS()
     *
     * Loads the global 'VIDEOS_LIST' with videos from external storage.
     *
     * @param _context The context.
     */
    public static void LOAD_VIDEOS(Context _context)
    {
        VIDEOS_LIST = new ArrayList<>();
        String path = VIDEO_FOLDER(_context);
        File directory = new File(path);
        File[] files = directory.listFiles();
        for (File file : files) ADD_VIDEO(VIDEO_FROM_FILE(_context, file));
    }

    /** VIDEO_FOLDER()
     *
     * @param _context The context.
     * @return Directory where all recordings should be saved.
     */
    public static String VIDEO_FOLDER(Context _context)
    {
        return _context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)+"";
    }

    /** VIDEO_FROM_FILE()
     *
     * @param _context The context.
     * @param _file File pointing to a video.
     * @return A 'VideoItem' object which represents the provided video file.
     */
    public static VideoItem VIDEO_FROM_FILE(Context _context, File _file)
    {
        VideoItem output = null;
        try
        {
            output = new VideoItem(_file.getAbsolutePath(), stripFiletype(_file.getName()),
                    getVideoLength(_context, _file), new Date(_file.lastModified()));
        }
        catch (Exception ex) { System.out.println("Invalid File!"); }
        return output;
    }

    /** ADD_VIDEO()
     *
     * @param _video 'VideoItem' to add to the global list of videos.
     */
    public static void ADD_VIDEO(VideoItem _video)
    {
        if (_video != null) VIDEOS_LIST.add(_video);
    }

    /** FORMAT_CLOCK()
     *
     * A utility function for use throughout the application.
     * It is unrelated to the rest of the functions in Database but It seems silly to create an entirely new class for one function.
     *
     * @param _duration A duration of time in seconds.
     * @return A string representing the given duration in minutes and seconds (mm:ss)
     */
    public static String FORMAT_CLOCK(int _duration)
    {
        int minutes = _duration / 60;
        int seconds = _duration - (minutes * 60);
        return minutes+":"+String.format("%02d", seconds);
    }

    /** DESTROY()
     *
     * Destroys all objects in global 'VIDEOS_LIST' and nullifies the list itself.
     */
    public static void DESTROY()
    {
        for (VideoItem v : VIDEOS_LIST)
        {
            if (v != null) v.destroy();
        }

        if (VIDEOS_LIST != null)
        {
            VIDEOS_LIST.clear();
            VIDEOS_LIST = null;
        }
    }

    /* ~~~ PRIVATE METHODS ~~~ */

    /** stripFiletype()
     *
     * @param _filename Filename string.
     * @return The given filename string without the file type. (For example '.mp4')
     */
    private static String stripFiletype(String _filename)
    {
        return _filename.substring(0, _filename.lastIndexOf('.'));
    }

    /** getVideoLength()
     *
     * @param _context The context.
     * @param _file File representing a video.
     * @return Duration of the provided video file in seconds.
     * @throws Exception If the given file is not a valid video, exception will be thrown.
     */
    private static int getVideoLength(Context _context, File _file) throws Exception
    {
        int output = 0;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(_context, Uri.fromFile(_file));
        long duration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        retriever.release();
        output = (int) (duration / 1000);
        return output;
    }
}
