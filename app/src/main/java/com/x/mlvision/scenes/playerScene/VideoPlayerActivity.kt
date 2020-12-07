package com.x.mlvision.scenes.playerScene

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.x.mlvision.R
import com.x.mlvision.databinding.ShowImageActivityBinding
import dagger.android.AndroidInjection
import javax.inject.Inject

class VideoPlayerActivity : AppCompatActivity() {
    companion object {
        fun openActivity(
            ctx: Context,
            url: String?
        ) {
            val intent =
                Intent(ctx, VideoPlayerActivity::class.java)
            intent.putExtra("url", url)
            ctx.startActivity(intent)
        }
    }

    @Inject
    lateinit var simpleExoPlayer: SimpleExoPlayer

    @Inject
    lateinit var dataSourceFactory: DataSource.Factory

    @Inject
    lateinit var simpleCache: SimpleCache

    @Inject
    lateinit var cacheDataSourceFactory: CacheDataSourceFactory

    /**
     * I am using Data binding
     * */
    private lateinit var binding: ShowImageActivityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        initialiseView()
        setUpPlayerView()
    }

    /*
   * Initialising the View using Data Binding
   * */
    private fun initialiseView() {

        binding = DataBindingUtil.setContentView(
            this, R.layout.activity_video_player
        )
        binding.url = intent.getStringExtra("url")
    }

    fun setUpPlayerView() {
        val concatenatedSource = ConcatenatingMediaSource(
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(binding.url))
        )
        simpleExoPlayer.prepare(concatenatedSource)
        //binding.playerView.setUseController(false);
        simpleExoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        binding.playerView.player = simpleExoPlayer
        binding.playerView.keepScreenOn = true
        binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    override fun onResume() {
        super.onResume()
        simpleExoPlayer.playWhenReady = true

    }


    override fun onPause() {
        super.onPause()
        simpleExoPlayer.playWhenReady = true

    }

    override fun onStop() {
        super.onStop()
        simpleExoPlayer.playWhenReady = true
        binding.playerView.player = null
        simpleExoPlayer.stop()


    }
}