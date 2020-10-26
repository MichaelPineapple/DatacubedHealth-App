package michael.wilson.datacubed3.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import michael.wilson.datacubed3.Other.AutoFitTextureView;
import michael.wilson.datacubed3.Other.Database;
import michael.wilson.datacubed3.R;
import michael.wilson.datacubed3.Other.VideoRecorder;

/** CameraActivity
 *
 * Displays the camera preview and records a video for a specified duration.
 */
public class CameraActivity extends AppCompatActivity
{
    /* ~~~ MEMBER VARIABLES AND CONSTANTS ~~~ */

    /** Static string keys for the intent extras */
    public static final String KEY_DURATION = "KEY_DURATION", KEY_NAME = "KEY_NAME";

    /** Array of permissions this activity requires **/
    private final String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    /** Unique request code for the permissions request */
    private final int PERMISSION_REQUESTCODE = 69;

    /** Duration of the recording in seocnds, default to minimum **/
    private int mDuration = R.integer.min_recording_duration;

    /** Reference to label for displaying the remaining time **/
    private TextView mLbl_timer;

    /** Reference to view which contains the timer label **/
    private ConstraintLayout mLayout_timer;

    /** Recording timer **/
    private CountDownTimer mTimer;

    /** Timer for camera setup **/
    private CountDownTimer mSetupTimer;

    /** Video Recorder for handling recoding work **/
    private VideoRecorder mRecorder;

    /** Path of video file **/
    private String mVideoFilePath;

    /** Reference to texture view for displaying camera preview **/
    private AutoFitTextureView mPreviewTexture;

    /** Device orientation angle when this activity is started **/
    private int mInitialOrientationAngle = 90;

    private OrientationEventListener mOrientationEventListener;

    /* ~~~ OVERRIDE METHODS ~~~ */

    /** onCreate()
     *
     * Called when this activity is created.
     * Gets data from the intent, handles orientation,
     * initializes the video recorder, checks for  permissions,
     * and starts recording.
     *
     * @param _savedInstanceState the saved instance state.
     */
    @Override
    protected void onCreate(Bundle _savedInstanceState)
    {
        super.onCreate(_savedInstanceState);

        // Use 'activity_camera' layout
        setContentView(R.layout.activity_camera);

        // Get recording duration and name from intent
        Intent _intent = getIntent();
        mDuration = _intent.getIntExtra(KEY_DURATION, R.integer.min_recording_duration);
        String name = _intent.getStringExtra(KEY_NAME);

        // Get references to UI elements in the layout
        mLbl_timer = findViewById(R.id.lbl_timer);
        mPreviewTexture = findViewById(R.id.previewTexture);
        mLayout_timer = findViewById(R.id.layout_timer);
        mLayout_timer.setVisibility(View.INVISIBLE);

        // Create unique filename from recording name
        File f;
        String filename = name;
        String filepath;
        int k = 0;
        do
        {
            filepath = Database.VIDEO_FOLDER(this) + "/" + filename + ".mp4";
            f = new File(filepath);
            filename = name + k;
            k++;
        }
        while (f.exists());
        mVideoFilePath = filepath;


        // Get the initial device orientation and store it in 'mInitialOrientationAngle'
        try
        {
            final int rotation = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            switch (rotation)
            {
                case Surface.ROTATION_0:
                    mInitialOrientationAngle = 90;
                    break;

                case Surface.ROTATION_90:
                    mInitialOrientationAngle = 0;
                    break;

                case Surface.ROTATION_180:
                    mInitialOrientationAngle = 270;
                    break;

                case Surface.ROTATION_270:
                    mInitialOrientationAngle = 180;
                    break;
            }
        }
        catch (Exception ex) { ex.printStackTrace(); }

        // Set this activity to always be in portrait mode
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Create listener for when the device orientation changes
        mOrientationEventListener = new OrientationEventListener(this, 100000)
        {
            final int threshold = 25;
            int previousAngle = 0;
            @Override
            public void onOrientationChanged(int orientation)
            {
                // Determine which way to rotate timer label based on device orientation
                if (orientation != -1)
                {
                    int angle = previousAngle;
                    if (orientation > (315 + (45 - threshold)) || orientation < threshold) angle = 0;
                    else if (orientation > (90 - threshold) && orientation < (90 + threshold)) angle = 270;
                    else if (orientation > (180 - threshold) && orientation < (180 + threshold)) angle = 180;
                    else if (orientation > (270 - threshold) && orientation < (270 + threshold)) angle = 90;
                    if (mLayout_timer != null) mLayout_timer.setRotation(angle);
                    previousAngle = angle;
                }
            }
        };
        mOrientationEventListener.enable();

        // Check permissions. (Implicitly starts recording if permissions are granted)
        handlePermissions();
    }

    /** onDestroy()
     *
     * Called when this activity is about to be closed.
     */
    @Override
    protected void onDestroy()
    {
        // Shut down video recorder (implicitly stops recording if started)
        if (mRecorder != null)
        {
            mRecorder.shutdown();
            mRecorder = null;
        }

        // Cancel and nullify recording timer
        if (mTimer != null)
        {
            mTimer.cancel();
            mTimer = null;
        }

        // Cancel and nullify startup timer
        if (mSetupTimer != null)
        {
            mSetupTimer.cancel();
            mSetupTimer = null;
        }

        // Disable and nullify orientation listener
        if (mOrientationEventListener != null)
        {
            mOrientationEventListener.disable();
            mOrientationEventListener = null;
        }

        // nullify member objects
        mLbl_timer = null;
        mLayout_timer = null;
        mLayout_timer = null;
        mVideoFilePath = null;
        mPreviewTexture = null;
        
        super.onDestroy();
    }

    /** onBackPressed()
     *
     * Called when the user presses the back button
     */
    @Override
    public void onBackPressed()
    {
        // stop recording and save video file
        stopAndSave();
        super.onBackPressed();
    }

    /** onRequestPermissionsResult()
     *
     * Called when the user either accepts or denies a permissions request.
     * If all permissions have been accepted, call 'onPermissionsAccepted()'
     * otherwise close the activity
     *
     * @param _requestCode Unique identifier of the request
     * @param _permissions Permissions which have been accepted/denied
     * @param _grantResults Results of the permissions request
     */
    @Override
    public void onRequestPermissionsResult(int _requestCode, @NonNull String[] _permissions, @NonNull int[] _grantResults)
    {
        super.onRequestPermissionsResult(_requestCode, _permissions, _grantResults);
        if (_requestCode == PERMISSION_REQUESTCODE)
        {
            boolean good = true;
            for (int grantResult : _grantResults)
            {
                if (grantResult != PackageManager.PERMISSION_GRANTED) good = false;
            }

            if (good) onPermissionsGranted();
            else finish();
        }
    }

    /* ~~~ PRIVATE METHODS ~~~ */

    /** handlePermissions()
     *
     * Checks to see if the user has already granted permissions.
     * If so, call 'onPermissionsGranted().'
     * Otherwise, request permissions from the user.
     */
    private void handlePermissions()
    {
        if (!checkPermissions(PERMISSIONS))  ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUESTCODE);
        else onPermissionsGranted();
    }

    /** checkPermissions()
     *
     * Checks if all the provided permissions have been granted by the user.
     *
     * @param _permissions Array of permissions to check
     * @return Whether or not the given permissions have been granted.
     */
    private boolean checkPermissions(String[] _permissions)
    {
        boolean output = true;
        for (String p : _permissions)
        {
            if (getApplicationContext().checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) output = false;
        }
        return output;
    }

    /** onPermissionsGranted()
     *
     * Called when user has accepted all permissions
     */
    private void onPermissionsGranted()
    {
        // Initialize video recorder
        mRecorder = new VideoRecorder(this, mPreviewTexture, mVideoFilePath, mInitialOrientationAngle, recorderListener);
    }

    /** startTimer()
     *
     * Starts the recording timer.
     */
    private void startTimer()
    {
        // If timer does not already exist
        if (mTimer == null)
        {
            // crate a new timer for the recording duration.
            mTimer = new CountDownTimer(1000 * (mDuration+1), 1000)
            {
                int time = mDuration+1;
                public void onTick(long millisUntilFinished)
                {
                    // Every second, update the timer label
                    time--;
                    if (time < 0) time = 0;
                    mLbl_timer.setText(Database.FORMAT_CLOCK(time));
                }

                public void onFinish()
                {
                    // When the timer is finished, stop recording and save the video
                    stopAndSave();

                    // close the activity
                    finish();
                }
            }.start();

            // Set timer label to be visible
            mLayout_timer.setVisibility(View.VISIBLE);
        }
    }

    /** stopAndSave()
     *
     * Stops recording, saves video file to external storage and global 'VIDEOS_LIST'
     */
    private void stopAndSave()
    {
        // Stop recording (implicitly saves video file to external storage)
        if (mRecorder != null)
        {
            if (mRecorder.isRecording()) mRecorder.stopRecordingVideo();
        }

        // Check if video file has been created. If so, add to global 'VIDEOS_LIST'
        File videoFile = new File(mVideoFilePath);
        if (mRecorder.hasStartedRecording())
        {
            if (mVideoFilePath != null)
            {
                File savedFile = new File(mVideoFilePath);
                if (savedFile.exists())
                {
                    Database.ADD_VIDEO(Database.VIDEO_FROM_FILE(this, videoFile));
                    Toast.makeText(this, R.string.toast_videoSaved, Toast.LENGTH_SHORT).show();
                }
                else Toast.makeText(this, R.string.toast_saveFailed, Toast.LENGTH_SHORT).show();
            }
            else Toast.makeText(this, R.string.toast_saveFailed, Toast.LENGTH_SHORT).show();
        }
    }


    /* ~~~ LISTENERS AND CALLBACKS ~~~ */

    /** Listener object for the video recorder */
    VideoRecorder.VideoRecorderListener recorderListener = new VideoRecorder.VideoRecorderListener()
    {
        /** onReady()
         *
         * Called when the video recorder has finished setup and is ready to start recording
         */
        @Override
        public void onReady()
        {
            // Start the camera preview
            mRecorder.startPreview();

            // Delay recording so the camera has enough time to fully open.
            mSetupTimer = new CountDownTimer(200, 100)
            {
                @Override
                public void onTick(long millisUntilFinished) { }

                @Override
                public void onFinish()
                {
                    // When the delay is finished:

                    // Start the recording timer
                    startTimer();

                    // Start recording
                    mRecorder.startRecordingVideo();
                }
            }.start();
        }

        /** onFail()
         *
         * Called when the video recorder has failed to setup or encountered an error.
         */
        @Override
        public void onFail()
        {
            // Notify the user that an error has occurred.
            Toast.makeText(CameraActivity.this, R.string.error, Toast.LENGTH_SHORT).show();

            // Close the activity
            finish();
        }
    };


}