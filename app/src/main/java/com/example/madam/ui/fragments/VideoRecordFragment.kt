package com.example.madam.ui.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
import android.hardware.camera2.CameraCharacteristics.SENSOR_ORIENTATION
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.hardware.camera2.CameraDevice.TEMPLATE_RECORD
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.example.madam.BuildConfig
import com.example.madam.R
import com.example.madam.databinding.FragmentVideoRecordBinding
import com.example.madam.ui.activities.LoginActivity
import com.example.madam.ui.activities.MainActivity
import com.example.madam.ui.viewModels.VideoViewModel
import com.example.madam.ui.views.AutoFitTextureView
import com.opinyour.android.app.data.utils.Injection
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_video_record.*
import java.io.File
import java.io.IOException
import java.lang.Long.signum
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.jvm.Throws

/*
    USED RESOURCE : https://github.com/googlearchive/android-Camera2Video/tree/master/kotlinApp
 */

class VideoRecordFragment : Fragment(), View.OnClickListener, View.OnTouchListener {

    private lateinit var binding: FragmentVideoRecordBinding
    private lateinit var videoViewModel: VideoViewModel
    private val SENSOR_ORIENTATION_DEFAULT_DEGREES = 90
    private val SENSOR_ORIENTATION_INVERSE_DEGREES = 270
    private val DEFAULT_ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 90)
        append(Surface.ROTATION_90, 0)
        append(Surface.ROTATION_180, 270)
        append(Surface.ROTATION_270, 180)
    }
    private val INVERSE_ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 270)
        append(Surface.ROTATION_90, 180)
        append(Surface.ROTATION_180, 90)
        append(Surface.ROTATION_270, 0)
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit

    }

    private var cameraId: String = "0"
    private var lastVideoPath: String = ""
    private lateinit var textureView: AutoFitTextureView
    private lateinit var videoButton: ImageView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var previewSize: Size
    private lateinit var videoSize: Size
    private var isRecordingVideo = false
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private val cameraOpenCloseLock = Semaphore(1)
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private var sensorOrientation = 0
    private var flashSupported = false

    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            this@VideoRecordFragment.cameraDevice = cameraDevice
            startPreview()
            configureTransform(textureView.width, textureView.height)
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@VideoRecordFragment.cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@VideoRecordFragment.cameraDevice = null
            activity?.finish()
        }

    }
    private var nextVideoAbsolutePath: String? = null
    private var mediaRecorder: MediaRecorder? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_video_record, container, false
        )
        binding.lifecycleOwner = this
        videoViewModel =
            ViewModelProvider(this, Injection.provideViewModelFactory(requireContext()))
                .get(VideoViewModel::class.java)
        Log.i(TAG, "Init constructor")

        binding.model = videoViewModel

        videoViewModel.error.observe(viewLifecycleOwner) {
            Toasty.error(requireContext(), it, LENGTH_LONG).show()
        }

        videoViewModel.success.observe(viewLifecycleOwner) {
            Toasty.success(requireContext(), it, LENGTH_LONG).show()
        }

        videoViewModel.userManager.refreshTokenSuccess.observe(viewLifecycleOwner) {
            if (it) {
                when(videoViewModel.apiCallFunctionFailed) {
                    "loadVideos" -> {videoViewModel.loadVideos()}
                    "uploadVideo" -> {videoViewModel.uploadVideo(videoViewModel.videoFile!!)}
                    else -> {}
                }
            } else {
                videoViewModel.userManager.logoutUser()
                (activity as MainActivity).goToActivity(LoginActivity::class.java)
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        textureView = view.findViewById(R.id.texture_view)
        videoButton = view.findViewById<ImageView>(R.id.recordVideo).also {
            it.setOnTouchListener(this)
        }
        view.findViewById<ImageView>(R.id.flip_camera).also {
            it.setOnClickListener(this)
        }
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (textureView.isAvailable) {
            openCamera(textureView.width, textureView.height)
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.flip_camera -> switchCamera()
        }
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (v.id == R.id.recordVideo) {

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {

                    startRecordingVideo()

                    recordVideo.backgroundTintList =
                        ColorStateList.valueOf(resources.getColor(R.color.accent))
                        overlay.setBackgroundResource(R.drawable.recording_background)
                }

                MotionEvent.ACTION_UP -> {

                    stopRecordingVideo()

                    recordVideo.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#66BDBDBD"))
                    overlay.background = null

                    startActivityForResult(Intent().apply {
                        action = Intent.ACTION_VIEW
                        type = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension("mp4")
                        val authority = "${BuildConfig.APPLICATION_ID}.provider"
                        data = FileProvider.getUriForFile(v.context, authority, File(lastVideoPath))
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }, VIDEO_UPLOAD_REQUEST_CODE)
                }
            }
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != VIDEO_UPLOAD_REQUEST_CODE) return
        val dialogClickListener: DialogInterface.OnClickListener =
            DialogInterface.OnClickListener { _, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        videoViewModel.uploadVideo(File(lastVideoPath))
                    }
                    DialogInterface.BUTTON_NEGATIVE -> {
                    }
                }
                (activity as MainActivity).view_main_pager.currentItem = 1
            }

        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder
            .setMessage("Do you want to add a video ?")
            .setPositiveButton("Yes", dialogClickListener)
            .setNegativeButton("No", dialogClickListener)
            .show()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread?.looper!!)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, e.toString())
        }
    }

    private fun areDimensionsflip_cameraped(displayRotation: Int): Boolean {
        var flip_camerapedDimensions = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    flip_camerapedDimensions = true
                }
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    flip_camerapedDimensions = true
                }
            }
            else -> {
                Log.e(TAG, "Display rotation is invalid: $displayRotation")
            }
        }
        return flip_camerapedDimensions
    }

    private fun setUpCameraOutputs(width: Int, height: Int) {
        val manager = requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                // We don't use a front facing camera in this sample.
                val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (cameraDirection != null &&
                    cameraDirection == CameraCharacteristics.LENS_FACING_FRONT
                ) {
                    continue
                }

                val map = characteristics.get(
                    SCALER_STREAM_CONFIGURATION_MAP
                ) ?: continue

                // For still image captures, we use the largest available size.
                val largest = Collections.max(
                    listOf(*map.getOutputSizes(ImageFormat.JPEG)),
                    CompareSizesByArea()
                )

                // Find out if we need to flip_camera dimension to get the preview size relative to sensor
                // coordinate.
                val displayRotation = requireActivity().windowManager.defaultDisplay.rotation
                val flip_camerapedDimensions = areDimensionsflip_cameraped(displayRotation)

                val displaySize = Point()
                requireActivity().windowManager.defaultDisplay.getSize(displaySize)
                val rotatedPreviewWidth = if (flip_camerapedDimensions) height else width
                val rotatedPreviewHeight = if (flip_camerapedDimensions) width else height
                var maxPreviewWidth = if (flip_camerapedDimensions) displaySize.y else displaySize.x
                var maxPreviewHeight =
                    if (flip_camerapedDimensions) displaySize.x else displaySize.y


                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                previewSize = chooseOptimalSize(
                    map.getOutputSizes(SurfaceTexture::class.java),
                    rotatedPreviewWidth, rotatedPreviewHeight,
                    maxPreviewWidth, maxPreviewHeight,
                    largest
                )

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(maxPreviewWidth, maxPreviewHeight)
                } else {
                    textureView.setAspectRatio(maxPreviewHeight, maxPreviewWidth)
                }

                // Check if the flash is supported.
                flashSupported =
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
        }

    }

    @SuppressLint("MissingPermission")
    private fun openCamera(width: Int, height: Int) {
        val cameraActivity = activity
        if (cameraActivity == null || cameraActivity.isFinishing) return

        val manager = cameraActivity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }

            // Choose the sizes for camera preview and video recording
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(SCALER_STREAM_CONFIGURATION_MAP)
                ?: throw RuntimeException("Cannot get available preview/video sizes")
            sensorOrientation = characteristics.get(SENSOR_ORIENTATION)!!
            videoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder::class.java))
            setUpCameraOutputs(width, height)
            configureTransform(width, height)
            mediaRecorder = MediaRecorder()
            manager.openCamera(cameraId, stateCallback, null)
        } catch (e: CameraAccessException) {
            showToast("Cannot access the camera.")
            cameraActivity.finish()
        } catch (e: NullPointerException) {
            Toasty.warning(requireContext(), "Camera is just using", Toast.LENGTH_LONG).show()
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.")
        }
    }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            closePreviewSession()
            cameraDevice?.close()
            cameraDevice = null
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    private fun startPreview() {
        if (cameraDevice == null || !textureView.isAvailable) return

        try {
            closePreviewSession()
            val texture = textureView.surfaceTexture
            texture!!.setDefaultBufferSize(previewSize.width, previewSize.height)
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(TEMPLATE_PREVIEW)

            val previewSurface = Surface(texture)
            previewRequestBuilder.addTarget(previewSurface)

            cameraDevice?.createCaptureSession(
                listOf(previewSurface),
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        updatePreview()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        if (activity != null) showToast("Failed")
                    }
                }, backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun updatePreview() {
        if (cameraDevice == null) return

        try {
            setUpCaptureRequestBuilder(previewRequestBuilder)
            HandlerThread("CameraPreview").start()
            captureSession?.setRepeatingRequest(
                previewRequestBuilder.build(),
                null, backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun setUpCaptureRequestBuilder(builder: CaptureRequest.Builder?) {
        builder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        activity ?: return
        val rotation = (activity as FragmentActivity).windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = Math.max(
                viewHeight.toFloat() / previewSize.height,
                viewWidth.toFloat() / previewSize.width
            )
            with(matrix) {
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        }
        textureView.setTransform(matrix)
    }

    @Throws(IOException::class)
    private fun setUpMediaRecorder() {
        val cameraActivity = activity ?: return

        if (nextVideoAbsolutePath.isNullOrEmpty()) {
            nextVideoAbsolutePath = getVideoFilePath(cameraActivity)
        }

        val rotation = cameraActivity.windowManager.defaultDisplay.rotation
        when (sensorOrientation) {
            SENSOR_ORIENTATION_DEFAULT_DEGREES ->
                mediaRecorder?.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation))
            SENSOR_ORIENTATION_INVERSE_DEGREES ->
                mediaRecorder?.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation))
        }

        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(nextVideoAbsolutePath)
            setVideoEncodingBitRate(10_000_000)
            setVideoFrameRate(30)
            setVideoSize(videoSize.width, videoSize.height)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            prepare()
        }
    }

    private fun getVideoFilePath(context: Context?): String {
        val filename = "${System.currentTimeMillis()}.mp4"
        val dir = context?.getExternalFilesDir(null)

        return if (dir == null) {
            filename
        } else {
            "${dir.absolutePath}/$filename"
        }
    }

    private fun startRecordingVideo() {
        if (cameraDevice == null || !textureView.isAvailable) return

        try {
            closePreviewSession()
            setUpMediaRecorder()
            val texture = textureView.surfaceTexture.apply {
                this!!.setDefaultBufferSize(previewSize.width, previewSize.height)
            }

            // Set up Surface for camera preview and MediaRecorder
            val previewSurface = Surface(texture)
            val recorderSurface = mediaRecorder!!.surface
            val surfaces = ArrayList<Surface>().apply {
                add(previewSurface)
                add(recorderSurface)
            }
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(TEMPLATE_RECORD).apply {
                addTarget(previewSurface)
                addTarget(recorderSurface)
            }

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            cameraDevice?.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        captureSession = cameraCaptureSession
                        updatePreview()
                        activity?.runOnUiThread {
                            isRecordingVideo = true
                            mediaRecorder?.start()
                        }
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        if (activity != null) showToast("Failed")
                    }
                }, backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun closePreviewSession() {
        captureSession?.close()
        captureSession = null
    }

    private fun stopRecordingVideo() {
        isRecordingVideo = false
        mediaRecorder?.apply {
            stop()
            reset()
        }

        lastVideoPath = nextVideoAbsolutePath.toString()
        nextVideoAbsolutePath = null
    }

    private fun showToast(message: String) =
        Toasty.error(requireContext(), message, LENGTH_LONG).show()

    private fun chooseVideoSize(choices: Array<Size>) = choices.firstOrNull {
        it.width == it.height * 4 / 3 && it.width <= 1080
    } ?: choices[choices.size - 1]

    private fun chooseOptimalSize(
        choices: Array<Size>,
        width: Int,
        height: Int,
        aspectRatio: Size
    ): Size {

        // Collect the supported resolutions that are at least as big as the preview Surface
        val w = aspectRatio.width
        val h = aspectRatio.height
        val bigEnough = choices.filter {
            it.height == it.width * h / w && it.width >= width && it.height >= height
        }

        // Pick the smallest of those, assuming we found any
        return if (bigEnough.isNotEmpty()) {
            Collections.min(bigEnough, CompareSizesByArea())
        } else {
            choices[0]
        }
    }

    companion object {
        private const val VIDEO_UPLOAD_REQUEST_CODE = 202
        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }

        private val TAG = "VideoRecordFragment"

        @JvmStatic
        private fun chooseOptimalSize(
            choices: Array<Size>,
            textureViewWidth: Int,
            textureViewHeight: Int,
            maxWidth: Int,
            maxHeight: Int,
            aspectRatio: Size
        ): Size {

            // Collect the supported resolutions that are at least as big as the preview Surface
            val bigEnough = ArrayList<Size>()
            // Collect the supported resolutions that are smaller than the preview Surface
            val notBigEnough = ArrayList<Size>()
            val w = aspectRatio.width
            val h = aspectRatio.height
            for (option in choices) {
                if (option.width <= maxWidth && option.height <= maxHeight &&
                    option.height == option.width * h / w
                ) {
                    if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }

            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // largest of those not big enough.
            if (bigEnough.size > 0) {
                return Collections.min(bigEnough, CompareSizesByArea())
            } else if (notBigEnough.size > 0) {
                return Collections.max(notBigEnough, CompareSizesByArea())
            } else {
                Log.e(TAG, "Couldn't find any suitable preview size")
                return choices[0]
            }
        }

        @JvmStatic
        fun newInstance(): VideoRecordFragment = VideoRecordFragment()
    }

    private fun switchCamera() {
        if (cameraId == "0") {
            cameraId = "1"
            closeCamera()
            reopenCamera()
        } else if (cameraId == "1") {
            cameraId = "0"
            closeCamera()
            reopenCamera()
        }
    }

    private fun reopenCamera() {
        if (textureView.isAvailable) {
            openCamera(textureView.width, textureView.height)
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    internal class CompareSizesByArea : Comparator<Size> {
        // We cast here to ensure the multiplications won't overflow
        override fun compare(lhs: Size, rhs: Size) =
            signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
    }
}