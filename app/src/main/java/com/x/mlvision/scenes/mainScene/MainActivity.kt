package com.x.mlvision.scenes.mainScene

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import com.x.mlvision.R
import com.x.mlvision.databinding.MainActivityBinding
import com.x.mlvision.scenes.scanScene.ScanActivity

class MainActivity : AppCompatActivity() {
    /**
     * I am using Data binding
     * */
    private lateinit var binding: MainActivityBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialiseView();
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