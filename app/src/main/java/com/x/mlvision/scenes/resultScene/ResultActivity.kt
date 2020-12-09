package com.x.mlvision.scenes.resultScene

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.x.mlvision.R
import com.x.mlvision.scenes.playerScene.VideoPlayerActivity
import kotlinx.android.synthetic.main.activity_result.*

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        result.setText(intent.getStringExtra("result"))
        or.setText(intent.getStringExtra("or"))
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    public fun playVideo(view: View){
        VideoPlayerActivity.openActivity(this,intent.getStringExtra("url"))

    }
}