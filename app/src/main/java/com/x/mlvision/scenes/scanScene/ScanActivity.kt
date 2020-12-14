package com.x.mlvision.scenes.scanScene

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.util.Pair
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.x.mlvision.R
import com.x.mlvision.scenes.resultScene.ResultActivity
import com.x.mlvision.utils.CameraSource
import com.x.mlvision.utils.CameraSourcePreview
import com.x.mlvision.utils.GraphicOverlay
import com.x.mlvision.utils.VisionImageProcessor
import com.x.mlvision.utils.textdetector.TextRecognitionProcessor
import kotlinx.android.synthetic.main.activity_scan.*
import java.io.File
import java.io.IOException


class ScanActivity : AppCompatActivity() {
    private var cameraSource: CameraSource? = null
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
                ).putExtra("result", "No data found").putExtra("or", or)
                    .putExtra("url", getFileResource())
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

        //   startRecordingScreen()

    }


    public override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            createCameraSource()
            startCameraSource()
        } else {
            Toast.makeText(this, "Please provide all permissions", Toast.LENGTH_SHORT).show()
            recorderStopped = true
            finish()
        }
        Log.d(TAG, "onResume")

    }

    /** Stops the camera.  */
    override fun onPause() {
        super.onPause()
        hasPaused = true
        activityFinished = isFinishing

    }

    public fun initMediaRecoder() {
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


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.i(TAG, "Permission granted!")

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (allPermissionsGranted()) {
        }
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


    val callback = Camera.PictureCallback { data, camera ->
        println("gid ")
        processBitmapFrame2(data)
    }

    var runnable2 = Runnable {
        if (cameraSource != null && cameraSource!!.camera != null)
            cameraSource!!.camera.takePicture(null, null, callback)

    }

    fun takePicture() {
        Handler().post(runnable2)

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
                takePicture()
                //here you can have your logic to set text to edittext
            }

            override fun onFinish() {
                if (countDownStopped) {
                    return
                }
                countDownStopped = true

                mTextField.setText("done!")
                if (!activityFinished) {
                    //   pb.show()
                    stopRecording()
                    // createFrameFromVideo()
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
        //cameraSource!!.setCallback()
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        val cpHigh: CamcorderProfile = CamcorderProfile
            .get(CamcorderProfile.QUALITY_720P)

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


    fun processBitmapFrame2(data: ByteArray) {


        //  imageProcessor!!.processBitmap(resizedBitmap, graphicOverlay)

        var imageBitmap: Bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
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
        //runOnUiThread(Runnable { img.setImageBitmap(resizedBitmap) })
        if (imageProcessor != null) {
            graphicOverlay!!.setImageSourceInfo(
                resizedBitmap.width, resizedBitmap.height, /* isFlipped= */false
            )
            imageProcessor!!.processBitmap(resizedBitmap, graphicOverlay)


        }

    }

    public fun addText() {

        finishingHandler.removeCallbacksAndMessages(null)
        finishingHandler.postDelayed(runnable, 1000 * 3)
        if (processFinished) {
            return
        }
        parseFrame()
        if (text.contains("DOB")) {
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
        if (graphicOverlay!!.txt.size > 0) {
            text = graphicOverlay!!.txt[graphicOverlay!!.txt.size - 1].text
            or = graphicOverlay!!.txt[graphicOverlay!!.txt.size - 1].text
        }

        if (text.contains("DOB")) {
            try {
                val i = text.lastIndexOf("DOB", text.indexOf("DOB"))
                var bday = text.subSequence(i, text.indexOf("\n", i))

                    if (bday.split("/").size != 3) {
                        text = "No data found"
                        return
                    }
                    val x = text . lastIndexOf ("\n", i)
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