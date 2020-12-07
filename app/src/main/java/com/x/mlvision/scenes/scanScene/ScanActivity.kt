package com.x.mlvision.scenes.scanScene

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.x.mlvision.R
import com.x.mlvision.scenes.resultScene.ResultActivity
import com.x.mlvision.utils.CameraSource
import com.x.mlvision.utils.CameraSourcePreview
import com.x.mlvision.utils.GraphicOverlay
import com.x.mlvision.utils.textdetector.TextRecognitionProcessor
import kotlinx.android.synthetic.main.activity_scan.*
import java.io.File
import java.io.IOException
import java.util.*


class ScanActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback{
    private var cameraSource: CameraSource? = null
    private var preview: CameraSourcePreview? = null
    private var graphicOverlay: GraphicOverlay? = null
    lateinit var rc: TextRecognitionProcessor
    lateinit var mMediaProjection: MediaProjection
    lateinit var mVirtualDisplay: VirtualDisplay
    lateinit var mMediaRecorder: MediaRecorder
    lateinit var mProjectionManager: MediaProjectionManager
    var mDisplayMetrics = DisplayMetrics()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        supportActionBar!!.hide()
        windowManager.defaultDisplay.getMetrics(mDisplayMetrics);
        mMediaRecorder = MediaRecorder()

        mProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager


        preview = findViewById(R.id.preview_view)
        if (preview == null) {
            Log.d(TAG, "Preview is null")
        }

        graphicOverlay = findViewById(R.id.graphic_overlay)
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null")
        }

        if (allPermissionsGranted()) {
            createCameraSource()
        } else {
            runtimePermissions
        }
        startRecordingScreen()

    }

    public override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")


    }

    /** Stops the camera.  */
    override fun onPause() {
        super.onPause()
        preview?.stop()

        // Stop screen recording
        //  hbRecorder.stopScreenRecording();

    }

    public override fun onDestroy() {
        super.onDestroy()
        stopRecording()
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
                Log.e(TAG, "Unable to start camera source.", e)
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
            rc = TextRecognitionProcessor(this)


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

    fun startCountDown() {
        object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                mTextField.setText("" + (millisUntilFinished / 1000) + " sec")

                //here you can have your logic to set text to edittext
            }

            override fun onFinish() {
                var text: String = ""
                for (txt in rc.txt) {
                    text = txt.text
                }
                mTextField.setText("done!")
                startActivity(
                    Intent(
                        this@ScanActivity,
                        ResultActivity::class.java
                    ).putExtra("result", text).putExtra("url",getFileResource())
                )
                finish()
            }
        }.start()
    }

    private fun startRecordingScreen() {
        val mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val permissionIntent =
            mediaProjectionManager?.createScreenCaptureIntent()
        startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                //Start screen recording
                mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data!!);
                //hbRecorder.startScreenRecording(data, resultCode, this);
                prepareRecording()
                createCameraSource()
                startCameraSource()
                startCountDown()

            } else {
                Toast.makeText(
                    this,
                    "Please provide screen recording permission",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun prepareRecording() {

        val directory: String =
            Environment.getExternalStorageDirectory().absolutePath + File.separator.toString() + "Recordings"
        if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState()) {
            Toast.makeText(this, "Failed to get External Storage", Toast.LENGTH_SHORT).show()
            return
        }

        val filePath: String = getFileResource()


        val width: Int = mDisplayMetrics.widthPixels
        val height: Int = mDisplayMetrics.heightPixels
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mMediaRecorder.setVideoEncodingBitRate(512 * 1000)
        mMediaRecorder.setVideoFrameRate(30)
        mMediaRecorder.setVideoSize(width, height)
        mMediaRecorder.setOutputFile(filePath)
        try {
            mMediaRecorder.prepare()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            return
        }
        mVirtualDisplay = getVirtualDisplay()
        mMediaRecorder.start()
    }

    private fun stopRecording() {
        if (mMediaRecorder != null) {
            mMediaRecorder.stop()
            mMediaRecorder.reset()
        }
        /*     if (mVirtualDisplay != null) {
                 mVirtualDisplay.release()
             }*/
        if (mMediaProjection != null) {
            mMediaProjection.stop()
        }
        //prepareRecording()
    }

    fun getFileResource(): String {
        var file = File(Environment.getExternalStorageDirectory().toString() + "/"+getString(R.string.app_name))
        file.mkdir()
        return File(file, "rc.mp4").absolutePath
    }

    private fun getVirtualDisplay(): VirtualDisplay {
        //screenDensity = mDisplayMetrics.densityDpi
        val width: Int = mDisplayMetrics.widthPixels
        val height: Int = mDisplayMetrics.heightPixels
        return mMediaProjection.createVirtualDisplay(
            this.javaClass.simpleName,
            width, height, mDisplayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mMediaRecorder.surface, null /*Callbacks*/, null /*Handler*/
        )
    }

}