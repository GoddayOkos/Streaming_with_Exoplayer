/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
* limitations under the License.
 */
package com.example.exoplayer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.exoplayer.databinding.ActivityPlayerBinding
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.util.Util

/**
 * A fullscreen activity to play audio or video streams.
 */
class PlayerActivity : AppCompatActivity() {

    private var player: SimpleExoPlayer? = null

    /**
     * These states(fields) allows you to resume playback from where the user left off.
     * All you need to do is supply these states information when you initialize your player.
     */
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition = 0L

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityPlayerBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
    }

    /**
     * Android API level 24 and higher supports multiple windows. As your app can be visible,
     * but not active in split window mode, you need to initialize the player in onStart.
     */
    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24) {
            initializePlayer()
        }
    }

    /**
     * Android API levels lower than 24 require you to wait as long as possible until
     * you grab resources, so you wait until onResume before initializing the player.
     */
    override fun onResume() {
        super.onResume()
        hideSystemUi()
        if ((Util.SDK_INT < 24 || player == null)) {
            initializePlayer()
        }
    }

    /**
     * With API Level 24 and lower, there is no guarantee of onStop being called, so you have
     * to release the player as early as possible in onPause
     */
    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }

    /**
     * With API Level 24 and higher (which brought multi- and split-window mode),
     * onStop is guaranteed to be called. In the paused state, your activity is still visible,
     * so you wait to release the player until onStop.
     */
    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    /**
     * Method for enabling full screen
     */
    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        viewBinding.videoView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    private fun initializePlayer() {
        player = SimpleExoPlayer.Builder(this)
            .build()
            .also { exoPlayer ->
                viewBinding.videoView.player = exoPlayer
                // Add media item(mp3) from remote source to the player
                val mediaItem = MediaItem.fromUri(getString(R.string.media_url_mp3))
                exoPlayer.setMediaItem(mediaItem)

                /**
                 * *   Supply the states to begin from where the user stopped
                 *
                 ***  playWhenReady tells the player whether to start playing as soon as
                 *     all resources for playback have been acquired. Because playWhenReady is
                 *     initially true, playback starts automatically the first time the app is run.
                 *
                 ***  seekTo tells the player to seek to a certain position within a specific window.
                 *    Both currentWindow and playbackPosition are initialized to zero so that
                 *    playback starts from the very start the first time the app is run.
                 *
                 ***  prepare tells the player to acquire all the resources required for playback.
                 */
                exoPlayer.playWhenReady = playWhenReady
                exoPlayer.seekTo(currentWindow, playbackPosition)
                exoPlayer.prepare()
            }
    }

    private fun releasePlayer() {
        player?.run {
            // Current playback position using currentPosition
            playbackPosition = this.currentPosition
            // Current window index using currentWindowIndex
            currentWindow = this.currentWindowIndex
            // Play/pause state using playWhenReady
            this@PlayerActivity.playWhenReady = this.playWhenReady
            release()   // Release the player when it's no longer needed.
        }
        player = null
    }
}