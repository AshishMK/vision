package com.x.mlvision.scenes.mainScene

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.x.mlvision.R
import com.x.mlvision.databinding.MainActivityBinding
import com.x.mlvision.scenes.scanScene.ScanActivity
import java.util.ArrayList

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback  {
    /**
     * I am using Data binding
     * */
    private lateinit var binding: MainActivityBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialiseView();

        if (allPermissionsGranted()) {
            //createCameraSource()
        } else {
            runtimePermissions
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
                    allNeededPermissions.toTypedArray(),1
                )
            }
        }
    private fun isPermissionGranted(
        context: Context,
        permission: String?
    ): Boolean {
        if (ContextCompat.checkSelfPermission(context, permission!!)
            == PackageManager.PERMISSION_GRANTED
        ) {
            //Log.i(ScanActivity.//TAG, "Permission granted: $permission")
            return true
        }
        //Log.i(ScanActivity.TAG, "Permission NOT granted: $permission")
        return false
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
    /*
     * Initialising the View using Data Binding
     * */
    private fun initialiseView() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    public fun onStartScanClicked(view: View) {
        startActivity(Intent(this@MainActivity, ScanActivity::class.java))
    }
}