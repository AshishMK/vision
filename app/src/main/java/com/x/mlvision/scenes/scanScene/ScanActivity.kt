package com.x.mlvision.scenes.scanScene

import Jni.TrackUtils
import VideoHandle.EpEditor
import VideoHandle.OnEditorListener
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.*
import android.media.projection.MediaProjection
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.util.Pair
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.x.mlvision.R
import com.x.mlvision.scenes.resultScene.ResultActivity
import com.x.mlvision.utils.*
import com.x.mlvision.utils.textdetector.TextRecognitionProcessor
import kotlinx.android.synthetic.main.activity_scan.*
import java.io.File
import java.io.IOException
import java.util.*


class ScanActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {
    private var cameraSource: CameraSource? = null
    lateinit var pb: ProgressDialog
    private var preview: CameraSourcePreview? = null
    private var graphicOverlay: GraphicOverlay? = null
    lateinit var rc: TextRecognitionProcessor
    lateinit var mMediaProjection: MediaProjection
    var recorderStopped = false
    var processFinished = false
    lateinit var mMediaRecorder: MediaRecorder
    lateinit var imageProcessor: VisionImageProcessor
    private var selectedSize = ScanActivity.SIZE_1024_768
    private var imageMaxWidth = 0
    private var hasPaused = false
    var activityFinished = false
    //finish activity when no data found for adhar
    val finishingHandler = Handler()
    val runnable = Runnable {
        if (!activityFinished && !processFinished) {

        startActivity(
            Intent(
                this@ScanActivity,
                ResultActivity::class.java
            ).putExtra("result", "No data found").putExtra("or", or).putExtra("url", getFileResource())
        )
        finish()
    }
    }

    // Max height (portrait mode)
    private var imageMaxHeight = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        supportActionBar!!.hide()
        pb = ProgressDialog(this)
        pb.setTitle("Processing...")
        pb.setMessage("FFmpeg process to each frame")
        root.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    imageMaxWidth = root.width
                    imageMaxHeight = root.height
                    // processBitmapFrame()
                }
            })
        rc = TextRecognitionProcessor(this)
        imageProcessor = rc
        mMediaRecorder = MediaRecorder()
        preview = findViewById(R.id.preview_view)
        preview!!.setScanActivity(this)
        if (preview == null) {
            Log.d(TAG, "Preview is null")
        }

        graphicOverlay = findViewById(R.id.graphic_overlay)
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null")
        }

        if (allPermissionsGranted()) {
            //createCameraSource()
        } else {
            runtimePermissions
        }
        //   startRecordingScreen()

    }


    public override fun onResume() {
        super.onResume()
        if (hasPaused) {
            createCameraSource()
            startCameraSource()
            initMediaRecoder()
            prepareRecording()

            startCountDown()
        }
        Log.d(TAG, "onResume")

    }

    /** Stops the camera.  */
    override fun onPause() {
        super.onPause()
        hasPaused = true
        activityFinished = isFinishing

    }

    private fun initMediaRecoder() {
        initRecording()
        prepareRecording()

        startCountDown()
    }

    public override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        //  mMediaRecorder.stop();
        preview?.stop()
        if (cameraSource != null) {
            cameraSource?.release()
        }
    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private fun startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null")
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null")
                }
                preview!!.start(cameraSource, graphicOverlay)
            } catch (e: IOException) {
                cameraSource!!.release()
                cameraSource = null
            }
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in requiredPermissions) {
            if (!isPermissionGranted(this, permission)) {
                return false
            }
        }
        return true
    }

    private val requiredPermissions: Array<String?>
        get() = try {
            val info = this.packageManager
                .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.isNotEmpty()) {
                ps
            } else {
                arrayOfNulls(0)
            }
        } catch (e: Exception) {
            arrayOfNulls(0)
        }

    private val runtimePermissions: Unit
        get() {
            val allNeededPermissions: MutableList<String?> = ArrayList()
            for (permission in requiredPermissions) {
                if (!isPermissionGranted(this, permission)) {
                    allNeededPermissions.add(permission)
                }
            }
            if (allNeededPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    allNeededPermissions.toTypedArray(),
                    PERMISSION_REQUESTS
                )
            }
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.i(TAG, "Permission granted!")
        if (allPermissionsGranted()) {
            createCameraSource()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun createCameraSource() {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = CameraSource(this, graphicOverlay)
        }
        try {
            cameraSource!!.setMachineLearningFrameProcessor(rc)

        } catch (e: Exception) {
            Log.e(TAG, "Can not create image processor: ", e)
            Toast.makeText(
                applicationContext, "Can not create image processor: " + e.message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        private const val TAG = "ScanActivity"
        private const val PERMISSION_REQUESTS = 1
        private const val SIZE_SCREEN = "w:screen" // Match screen width
        private const val SIZE_1024_768 = "w:1024" // ~1024*768 in a normal ratio
        private const val SIZE_640_480 = "w:640" // ~640*480 in a normal ratio
        const val SCREEN_RECORD_REQUEST_CODE = 1001
        private fun isPermissionGranted(
            context: Context,
            permission: String?
        ): Boolean {
            if (ContextCompat.checkSelfPermission(context, permission!!)
                == PackageManager.PERMISSION_GRANTED
            ) {
                Log.i(TAG, "Permission granted: $permission")
                return true
            }
            Log.i(TAG, "Permission NOT granted: $permission")
            return false
        }
    }

    var countDownStopped = false
    private fun startCountDown() {
        object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (countDownStopped) {
                    return
                }
                mTextField.setText("" + (millisUntilFinished / 1000) + " sec")

                //here you can have your logic to set text to edittext
            }

            override fun onFinish() {
                if (countDownStopped) {
                    return
                }
                countDownStopped = true

                mTextField.setText("done!")
                if (!activityFinished) {
                    pb.show()
                    stopRecording()
                    createFrameFromVideo()
                }
            }
        }.start()
    }


    private fun initRecording() {

        val directory: String =
            Environment.getExternalStorageDirectory().absolutePath + File.separator.toString() + "Recordings"
        if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState()) {
            Toast.makeText(this, "Failed to get External Storage", Toast.LENGTH_SHORT).show()
            return
        }

        val filePath: String = getFileResource()

        cameraSource!!.camera.unlock()
        mMediaRecorder.setCamera(cameraSource!!.camera)
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        val cpHigh: CamcorderProfile = CamcorderProfile
            .get(CamcorderProfile.QUALITY_720P)
        /*       mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
               mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
               mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
               mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
               mMediaRecorder.setVideoEncodingBitRate(512 * 1000)
               mMediaRecorder.setVideoFrameRate(30)
               mMediaRecorder.setVideoSize(width, height)*/
        mMediaRecorder.setProfile(cpHigh);
        mMediaRecorder.setOrientationHint(90)
        mMediaRecorder.setOutputFile(filePath)
    }

    private fun prepareRecording() {

        mMediaRecorder.setPreviewDisplay(preview!!.surfaceView.holder.surface)//capture.holder.surface);

        //  mMediaRecorder.setPreviewDisplay(preview_view.surfaceView.holder.surface);
        try {
            mMediaRecorder.prepare()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            return
        }
        //  mVirtualDisplay = getVirtualDisplay()
        mMediaRecorder.start()
    }

    private fun stopRecording() {
        if (mMediaRecorder != null && !recorderStopped) {
            recorderStopped = true
            mMediaRecorder.stop()
            mMediaRecorder.reset()
            //mMediaRecorder = null
        }

    }

    private fun getFileResource(): String {
        var file = File(
            Environment.getExternalStorageDirectory()
                .toString() + "/" + getString(R.string.app_name)
        )
        file.mkdir()
        return File(file, "rc.mp4").absolutePath
    }

    private fun getFileBitmapResource(): String {
        var file = File(
            Environment.getExternalStorageDirectory()
                .toString() + "/" + getString(R.string.app_name)
        )
        file.mkdir()
        return File(file, "pic%03d.jpg").absolutePath
    }


    fun createFrameFromVideo() {
        var height = getVideoHeight(this, Uri.parse(getFileResource()))
        var width = getVideoWidth(this, Uri.parse(getFileResource()))
        EpEditor.video2pic(
            getFileResource(),
            getFileBitmapResource(),
            height,
            width,
            2f,
            object : OnEditorListener {
                override fun onSuccess() {
                    processBitmapFrame()
                }

                override fun onFailure() {
                }

                override fun onProgress(progress: Float) {
                }

            })

    }

    private fun getVideoHeight(context: Context?, uri: Uri): Int {

        try {
            val mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(uri.toString())
            var videoExt = TrackUtils.selectVideoTrack(mediaExtractor)
            if (videoExt == -1) {
                videoExt = TrackUtils.selectAudioTrack(mediaExtractor)
                if (videoExt == -1) {
                    return 0
                }
            }
            val mediaFormat = mediaExtractor.getTrackFormat(videoExt)
            val res =
                if (mediaFormat.containsKey(MediaFormat.KEY_HEIGHT)) mediaFormat.getInteger(
                    MediaFormat.KEY_HEIGHT
                ) else 0
            mediaExtractor.release()
            if (res == 0) {
                getVideoHeightMediaRetriver(context, uri)
            }
            return res
        } catch (e: java.lang.Exception) {
        }
        return getVideoHeightMediaRetriver(context, uri)
    }

    private fun getVideoHeightMediaRetriver(
        context: Context?,
        uri: Uri?
    ): Int {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val time =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        val height = time.toInt()
        retriever.release()
        return height
    }

    private fun getVideoWidth(context: Context?, uri: Uri): Int {

        try {
            val mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(uri.toString())
            var videoExt = TrackUtils.selectVideoTrack(mediaExtractor)
            if (videoExt == -1) {
                videoExt = TrackUtils.selectAudioTrack(mediaExtractor)
                if (videoExt == -1) {
                    return 0
                }
            }
            val mediaFormat = mediaExtractor.getTrackFormat(videoExt)
            val res =
                if (mediaFormat.containsKey(MediaFormat.KEY_WIDTH)) mediaFormat.getInteger(
                    MediaFormat.KEY_WIDTH
                ) else 0
            mediaExtractor.release()
            if (res == 0) {
                getVideoWidthMediaRetriver(context, uri)
            }
            return res
        } catch (e: java.lang.Exception) {
        }
        return getVideoWidthMediaRetriver(context, uri)
    }

    private fun getVideoWidthMediaRetriver(
        context: Context?,
        uri: Uri?
    ): Int {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val time =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val height = time.toInt()
        retriever.release()
        return height
    }

    fun processBitmapFrame() {
        var files = File(
            Environment.getExternalStorageDirectory()
                .toString() + "/" + getString(R.string.app_name)
        ).listFiles()

        println("ffmpeg su bitmap " + files.size)
        for (f in files.reversed()) {
            if (!f.name.contains(".jpg")) {
                continue
            }
            //  imageProcessor!!.processBitmap(resizedBitmap, graphicOverlay)

            val imageBitmap = BitmapUtils.getBitmapFromContentUri(contentResolver, Uri.fromFile(f))
                ?: return
            // Clear the overlay first
            graphicOverlay!!.clear()
            // Get the dimensions of the image view
            val targetedSize = targetedWidthHeight
            // Determine how much to scale down the image
            val scaleFactor = kotlin.math.max(
                imageBitmap.width.toFloat() / targetedSize.first.toFloat(),
                imageBitmap.height.toFloat() / targetedSize.second.toFloat()
            )
            val resizedBitmap = Bitmap.createScaledBitmap(
                imageBitmap,
                (imageBitmap.width / scaleFactor).toInt(),
                (imageBitmap.height / scaleFactor).toInt(),
                true
            )
            if (imageProcessor != null) {
                graphicOverlay!!.setImageSourceInfo(
                    resizedBitmap.width, resizedBitmap.height, /* isFlipped= */false
                )
                imageProcessor!!.processBitmap(resizedBitmap, graphicOverlay)


            }
        }
    }

    public fun addText() {

        finishingHandler.removeCallbacksAndMessages(null)
        finishingHandler.postDelayed(runnable,1000*5)
        if (processFinished) {
            return
        }
        parseFrame()
        if (text.contains("DOB")) {
            pb.dismiss()
            processFinished = true
            startActivity(
                Intent(
                    this@ScanActivity,
                    ResultActivity::class.java
                ).putExtra("result", text).putExtra("or", or).putExtra("url", getFileResource())
            )
            finish()

        }
    }


    var text = "No data found"
    var or = ""
    private fun parseFrame() {
        text = "No data found"
        or = ""
        //for (txt in rc.txt) {
        if (graphicOverlay!!.txt.size > 0) {
            text = graphicOverlay!!.txt[graphicOverlay!!.txt.size - 1].text
            or = graphicOverlay!!.txt[graphicOverlay!!.txt.size - 1].text
        }

        if (text.contains("DOB")) {
            try {
                val i = text.lastIndexOf("DOB", text.indexOf("DOB"))
                val x = text.lastIndexOf("\n", i)
                val t =
                    if (i <= 1) "" else text.subSequence(text.lastIndexOf("\n", x - 1), x)
                        .toString()
                text = t + "\n" + text.subSequence(i, text.indexOf("\n", i)).toString()

            } catch (e: Exception) {
                text = "No data found"
            }
        }
    }

    private val targetedWidthHeight: Pair<Int, Int>
        get() {
            val targetWidth: Int
            val targetHeight: Int
            when (selectedSize) {
                SIZE_SCREEN -> {
                    targetWidth = imageMaxWidth
                    targetHeight = imageMaxHeight
                }
                SIZE_640_480 -> {
                    targetWidth = 480
                    targetHeight = 640
                }
                SIZE_1024_768 -> {
                    targetWidth = 768
                    targetHeight = 1024
                }
                else -> throw IllegalStateException("Unknown size")
            }
            return Pair(targetWidth, targetHeight)
        }
}