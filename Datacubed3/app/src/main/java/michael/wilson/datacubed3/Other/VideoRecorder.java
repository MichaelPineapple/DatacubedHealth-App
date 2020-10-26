package michael.wilson.datacubed3.Other;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/** VideoRecorder
 *
 * Helper class to handle camera management and recording logic.
 */
public class VideoRecorder
{
    /* ~~~MEMBER VARIABLES~~~ */

    /** Listener object for callback methods **/
    private VideoRecorderListener mListener;

    /** Activity where this 'VideoRecorder' is being used **/
    private Activity mActivity;

    /** Texture view for displaying camera feed **/
    private AutoFitTextureView mTextureView;

    /** The camera **/
    private CameraDevice mCameraDevice;

    /** Preview capture session **/
    private CameraCaptureSession mPreviewSession;

    /** Media recorder object for recording audio and video **/
    private MediaRecorder mMediaRecorder;

    /** Thread to handle background processes **/
    private HandlerThread mBackgroundThread;

    /** Handler to be executed on the background thread **/
    private Handler mBackgroundHandler;

    /** Semaphore to ensure threadsafe camera usage **/
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /** Capture request builder for the camera preview **/
    private CaptureRequest.Builder mPreviewBuilder;

    /** Path to the video file to be saved **/
    private String mFilePath;

    /** Keep track if we are recording or not **/
    private boolean mIsRecording = false;

    /** Keep track if this video recorder has started recording at all **/
    private boolean hasStartedRecording = false;

    /** Orientation  for the video to be saved in **/
    private int mInitialOrientationAngle;

    /** Dimensions of the video and preview window **/
    private Size mPreviewSize;
    private Size mVideoSize;

    /* ~~~PUBLIC METHODS~~~ */

    /** VideoRecorder()
     *
     * Instantiates this video recorder.
     * @param _activity The activity where this video recorder is being used.
     * @param _textureView Texture view for the camera feed to be shown.
     * @param _filePath Path to where the video file will be saved.
     * @param _orientation Orientation angle of the video.
     * @param _listener Listener object for callback methods.
     */
    public VideoRecorder(Activity _activity, AutoFitTextureView _textureView, String _filePath, int _orientation, VideoRecorderListener _listener)
    {
        this.mActivity = _activity;
        this.mTextureView = _textureView;
        this.mListener = _listener;
        this.mFilePath = _filePath;
        this.mInitialOrientationAngle = _orientation;

        // Start the background thread
        startBackgroundThread();

        // If the preview texture is available, open the camera. Otherwise start a listener.
        if (mTextureView.isAvailable()) openCamera();
        else mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }


    /** Shuts down this video recorder
     *
     * - If recording, stop.
     * - Close the camera.
     * - Stop the background thread.
     */
    public void shutdown()
    {
        // If currently recording, stop recording.
        if (mIsRecording) stopRecordingVideo();

        try
        {
            // acquire lock on camera semaphore
            mCameraOpenCloseLock.acquire();

            // cancel and nullify preview session
            if (mPreviewSession != null)
            {
                mPreviewSession.stopRepeating();
                mPreviewSession.abortCaptures();
                mPreviewSession = null;
            }

            // Close and nullify camera device
            if (null != mCameraDevice)
            {
                mCameraDevice.close();
                mCameraDevice = null;
            }

            // Release and nullify media recorder
            if (null != mMediaRecorder)
            {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            mListener.onFail();
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
            mListener.onFail();
        }
        finally
        {
            // release lock on camera semaphore
            mCameraOpenCloseLock.release();
        }


        // Stop the background thread
        stopBackgroundThread();

        // nullify remaining member objects
        recordingCaptureSessionCallback = null;
        previewCaptureSessionCallback = null;
        mStateCallback = null;
        mSurfaceTextureListener = null;
        mListener = null;
        mActivity = null;
        mTextureView = null;
        mPreviewBuilder = null;
        mFilePath = null;
        mPreviewSize = null;
        mVideoSize = null;
    }


    /** startRecordingVideo()
     *
     * Starts recording a video.
     */
    public void startRecordingVideo()
    {
        if (mCameraDevice != null && mTextureView.isAvailable())
        {
            try
            {
                closePreviewSession();
                setUpMediaRecorder();
                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                List<Surface> surfaces = new ArrayList<>();

                // Set up surface for the camera preview
                SurfaceTexture texture = mTextureView.getSurfaceTexture();
                texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                Surface previewSurface = new Surface(texture);
                surfaces.add(previewSurface);
                mPreviewBuilder.addTarget(previewSurface);

                // Set up surface for the media recorder
                Surface recorderSurface = mMediaRecorder.getSurface();
                surfaces.add(recorderSurface);
                mPreviewBuilder.addTarget(recorderSurface);

                // Start a capture session
                mCameraDevice.createCaptureSession(surfaces, recordingCaptureSessionCallback, mBackgroundHandler);
            }
            catch (CameraAccessException | IOException e)
            {
                e.printStackTrace();
                mListener.onFail();
            }
        }
    }


    /** startPreview()
     *
     * Start the camera preview feed.
     */
    public void startPreview()
    {
        if (mCameraDevice != null && mTextureView.isAvailable())
        {
            try
            {
                // Get surface from preview textureView
                SurfaceTexture texture = mTextureView.getSurfaceTexture();
                texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                Surface previewSurface = new Surface(texture);

                // Apply surface to builder
                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewBuilder.addTarget(previewSurface);

                // Start preview capture session
                mCameraDevice.createCaptureSession(Collections.singletonList(previewSurface), previewCaptureSessionCallback, mBackgroundHandler);
            }
            catch (CameraAccessException e)
            {
                e.printStackTrace();
                mListener.onFail();
            }
        }
    }

    /** stopRecordingVideo()
     *
     * Stop and reset the media recorder
     */
    public void stopRecordingVideo()
    {
        try
        {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mIsRecording = false;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    /** Getter methods **/
    public boolean hasStartedRecording() { return this.hasStartedRecording; }
    public boolean isRecording() { return this.mIsRecording; }





    /* ~~~PRIVATE METHODS~~~ */

    /** setUpMediaRecorder()
     *
     * Configures the media recorder. CALLS MUST HAPPEN IN THIS ORDER!
     *
     * @throws IOException Throws and IO exception if the media recorder is configured incorrectly.
     */
    private void setUpMediaRecorder() throws IOException
    {
        // Set audio and video sources.
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);

        // Set the output format as mp4
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        // Set the output file path
        mMediaRecorder.setOutputFile(mFilePath);

        // Set the encoding bitrate
        mMediaRecorder.setVideoEncodingBitRate(10000000);

        // Set the frame rate to 30FPS
        mMediaRecorder.setVideoFrameRate(30);

        // Set the video size
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());

        // Set audio and video encoders
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        // Set video orientation
        mMediaRecorder.setOrientationHint(mInitialOrientationAngle);

        // Prepare media recorder
        mMediaRecorder.prepare();
    }

    /** openCamera()
     *
     *  Gets the front facing camera and prepares it for recording.
     */
    private void openCamera()
    {
        if (mActivity != null && !mActivity.isFinishing())
        {
            // Get camera manager from activity
            CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
            try
            {
                // Acquire lock on camera semaphore. If unable, throw exception.
                if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS))
                {
                    throw new RuntimeException("Time out waiting to lock camera opening.");
                }

                // Get ID of primary front facing camera
                String cameraId = manager.getCameraIdList()[0];

                // Choose the sizes for camera preview and video recording
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) throw new RuntimeException("Cannot get available preview/video sizes");
                mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), mTextureView.getWidth(), mTextureView.getHeight(), mVideoSize);
                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());

                // Instantiate media recorder
                mMediaRecorder = new MediaRecorder();

                // Open camera if granted permission
                if (mActivity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;
                manager.openCamera(cameraId, mStateCallback, null);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                mListener.onFail();
            }
        }
    }


    /** startBackgroundThread()
     *
     * Instantiates the background thread and starts it with the process handler
     */
    private void startBackgroundThread()
    {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /** stopBackgroundThread()
     *
     * Stops the background thread and nullifies it safely.
     */
    private void stopBackgroundThread()
    {
        // Stop and nullify background thread.
        // If an exception occurs, call the 'onFail()' callback
        mBackgroundThread.quitSafely();
        try
        {

            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            mListener.onFail();
        }
    }


    /** updatePreview()
     *
     * Updates the current preview session.
     */
    private void updatePreview()
    {
        if (mCameraDevice != null)
        {
            try
            {
                mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
            }
            catch (CameraAccessException e)
            {
                e.printStackTrace();
                mListener.onFail();
            }
        }
    }

    /** closePreviewSession()
     *
     *  Safely close and nullify the preview session.
     */
    private void closePreviewSession()
    {
        if (mPreviewSession != null)
        {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    /** chooseVideoSize()
     *
     * From an array of sizes, choose one which has 3x4 aspect ratio and
     * is less than or equal to 1080p
     *
     * @param choices Array of available sizes
     * @return Chosen video size
     */
    private Size chooseVideoSize(Size[] choices)
    {
        for (Size size : choices)
        {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) return size;
        }
        return choices[choices.length - 1];
    }

    /** chooseOptimalSize()
     *
     * Return the optimal video size based on the given minimum dimensions,
     * desired aspect ratio, and supported camera sizes.
     *
     * @param choices     List of supported camera sizes
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return Optimal video size.
     */
    private Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio)
    {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices)
        {
            if (option.getHeight() == option.getWidth() * h / w && option.getWidth() >= width && option.getHeight() >= height)
            {
                bigEnough.add(option);
            }
        }

        // Comparator for comparing sizes
        Comparator<Size> sizeComparator = new Comparator<Size>()
        {
            @Override
            public int compare(Size lhs, Size rhs)
            {
                return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
            }
        };

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) return Collections.min(bigEnough, sizeComparator);
        else return choices[0];
    }


    /* ~~~LISTENERS AND CALLBACKS~~~ */

    /** Listener for preview capture session state callbacks **/
    private CameraCaptureSession.StateCallback previewCaptureSessionCallback = new CameraCaptureSession.StateCallback()
    {
        /** onConfigured()
         *
         * Called when a CameraCaptureSession has configured successfully.
         *
         * @param _session The CameraCaptureSession which called this.
         */
        @Override
        public void onConfigured(@NonNull CameraCaptureSession _session)
        {
            // Store the CameraCaptureSession in a member variable
            mPreviewSession = _session;

            // Update the preview session
            updatePreview();
        }

        /** onConfigureFailed()
         *
         * Called when a CameraCaptureSession fails to configure properly.
         *
         * @param _session The CameraCaptureSession which called this.
         */
        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession _session)
        {
            // Call 'onFail()' callback
            mListener.onFail();
        }
    };

    /** Listener for recording capture session state callbacks **/
    private CameraCaptureSession.StateCallback recordingCaptureSessionCallback = new CameraCaptureSession.StateCallback()
    {
        /** onConfigured()
         *
         * Called when a CameraCaptureSession has configured successfully.
         *
         * @param _session The CameraCaptureSession which called this.
         */
        @Override
        public void onConfigured(@NonNull CameraCaptureSession _session)
        {
            // Store the CameraCaptureSession in a member variable
            mPreviewSession = _session;

            // Update the preview session
            updatePreview();

            // Start a new UI thread
            mActivity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    /// START RECORDING!
                    mMediaRecorder.start();
                    mIsRecording = true;
                    hasStartedRecording = true;
                }
            });
        }

        /** onConfigureFailed()
         *
         * Called when a CameraCaptureSession fails to configure properly.
         *
         * @param _session The CameraCaptureSession which called this.
         */
        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession _session)
        {
            // Call 'onFail()' callback
            mListener.onFail();
        }
    };

    /** Listener for the preview surface texture callbacks **/
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener()
    {

        /** onSurfaceTextureAvailable()
         *
         * Called when a TextureView becomes available.
         *
         * @param surfaceTexture The SurfaceTexture which called this.
         * @param width Width of the SurfaceTexture
         * @param height Height of the SurfaceTexture
         */
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height)
        {
            // Open the camera
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) { }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) { return true; }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) { }

    };

    /** Listener for the camera state callbacks **/
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback()
    {

        /** onOpened()
         *
         * Called when a CameraDevice successfully opens.
         *
         * @param cameraDevice The CameraDevice which called this.
         */
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice)
        {
            // Save camera device to member variable, release camera semaphore, and call 'onReady()' callback.
            mCameraDevice = cameraDevice;
            mCameraOpenCloseLock.release();
            mListener.onReady();
            // THIS IS WHERE THE VIDEO RECORDER IS READY TO START RECORDING!
        }

        /** onDisconnected((
         *
         * Called when a CameraDevice is disconnected.
         *
         * @param cameraDevice The CameraDevice which called this.
         */
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice)
        {
            // Release camera semaphore lock, close camera, and nullify camera device.
            mCameraOpenCloseLock.release();

        }

        /** onError()
         *
         * Called when a CameraDevice encounters an error.
         *
         * @param cameraDevice The CameraDevice which called this.
         * @param error The error code.
         */
        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error)
        {
            // Release camera semaphore, close camera, nullify camera, and call 'onFail()' callback.
            mCameraOpenCloseLock.release();
            if (mCameraDevice != null)
            {
                cameraDevice.close();
                mCameraDevice = null;
            }
            mListener.onFail();
        }

    };

    /** VideoRecorderListener
     *
     * Interface which provides VideoRecorder callback methods.
     */
    public interface VideoRecorderListener
    {
        /** oneReady()
         *
         * Abstract method to be called when a VideoRecorder has finished setup and is ready to record
         */
        void onReady();

        /** onFail()
         *
         * Abstract method to be called when a VideoRecorder has encountered an error.
         */
        void onFail();
    }


}