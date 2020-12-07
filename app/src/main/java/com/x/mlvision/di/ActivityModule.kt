package com.x.mlvision.di

import com.x.mlvision.scenes.playerScene.VideoPlayerActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

/*
 *  Module to inject specified list of activities
 */
@Module
public abstract class ActivityModule {
  @ContributesAndroidInjector
    abstract fun contributeVideoPlayerActivity(): VideoPlayerActivity


}