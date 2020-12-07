package com.x.mlvision.di

import android.app.Application
import android.os.Environment
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import com.x.mlvision.R
import com.x.mlvision.application.AppController
import dagger.Module
import dagger.Provides
import java.io.File
import javax.inject.Singleton


/*
 * Module to provide api call functionality
 */
@Module
class ExoPlayerModule {
    companion object {
        fun getCacheDirectory(): File {
            val f = File(
                Environment.getExternalStorageDirectory()
                    .toString() + "/" + AppController.getInstance()
                    .getString(R.string.app_name) + "/.cache"
            )
            f.mkdirs()
            return f
        }

        const val MIN_BUFFER_DURATION = 2000

        //Max Video you want to buffer during PlayBack
        const val MAX_BUFFER_DURATION = 5000

        //Min Video you want to buffer before start Playing it
        const val MIN_PLAYBACK_START_BUFFER = 1500

        //Min video You want to buffer when user resumes video
        const val MIN_PLAYBACK_RESUME_BUFFER = 2000
    }

    /*
     * The method returns the Cache object
     * */
    @Provides
    @Singleton
    fun provideSimpleExoPlayer(application: Application): SimpleExoPlayer {
        /* Instantiate a DefaultLoadControl.Builder. */
        val builder = DefaultLoadControl.Builder()

        /*How many milliseconds of media data to buffer at any time. */
        val loadControlBufferMs =
            60000 ////DefaultLoadControl.MAX_BUFFER_MS; /* This is 50000 milliseconds in ExoPlayer 2.9.6 */

        /* Configure the DefaultLoadControl to use the same value for */builder.setBufferDurationsMs(
            loadControlBufferMs,
            loadControlBufferMs,
            1500,
            2000
        ) //.setTargetBufferBytes(-1)
            .setPrioritizeTimeOverSizeThresholds(false)
        return SimpleExoPlayer.Builder(application.applicationContext)
            .setLoadControl(builder.createDefaultLoadControl()).build()
    }

    /*
     * The method returns the Okhttp object
     * */
    @Provides
    @Singleton
    fun provideDataSourceFactory(application: Application): DataSource.Factory {
        return DefaultDataSourceFactory(
            application.applicationContext,
            Util.getUserAgent(
                application.applicationContext,
                application.getString(R.string.app_name)
            )
        )
    }

    /*
     * The method returns the SimpleCache object
     * release simplecache after storage permission
     * check home activity
     * */
    @Provides
    @Singleton
    fun provideSimpleCache(application: Application): SimpleCache {
        val cacheSize = 700 * 1024 * 1024 //100 mb;
        return SimpleCache(
            getCacheDirectory(),
            LeastRecentlyUsedCacheEvictor(cacheSize.toLong()),
            ExoDatabaseProvider(application)
        )
    }


    @Provides
    @Singleton
    fun provideCacheSourceFactory(
        simpleCache: SimpleCache,
        defaultDataSourceFactory: DataSource.Factory
    ): CacheDataSourceFactory {
        return CacheDataSourceFactory(simpleCache, defaultDataSourceFactory)
    }

}