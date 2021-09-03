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
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.exoplayer.databinding.ActivityPlayerBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util

private const val TAG = "PlayerActivity"

/**
 * A fullscreen activity to play audio or video streams.
 */
class PlayerActivity : AppCompatActivity() {

    private var player: SimpleExoPlayer? = null
    private lateinit var playbackStateListener: Player.EventListener

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
        playbackStateListener = playbackStateListener(this)
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
        /**
         *  Create a DefaultTrackSelector, which is responsible for choosing tracks in the media item.
         *  Then, tell your trackSelector to only pick tracks of standard definition or lower this
         *  is a good way of saving your user's data at the expense of quality.
         */
        val trackSelector = DefaultTrackSelector(this).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }
        player = SimpleExoPlayer.Builder(this)
            /**
             * pass your trackSelector to your builder so that it is used when building
             * the SimpleExoPlayer instance.
             */
            .setTrackSelector(trackSelector)
            .build()
            .also { exoPlayer ->
                viewBinding.videoView.player = exoPlayer

                // Build an adaptive media item using dash and MIME
                val mediaItem = MediaItem.Builder()
                    .setUri(getString(R.string.media_url_dash))
                    .setMimeType(MimeTypes.APPLICATION_MPD)
                    .build()

                exoPlayer.setMediaItem(mediaItem)
                // Add another media item to the player, thereby creating a playlist with two items.
//                val secondMediaItem = MediaItem.fromUri(getString(R.string.media_url_mp3))
//                exoPlayer.addMediaItem(secondMediaItem)

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
                exoPlayer.addListener(playbackStateListener) // Register the listener
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
            removeListener(playbackStateListener) // detach the listener
            release()   // Release the player when it's no longer needed.
        }
        player = null
    }
}

/**
 * PlaybackStateListener is used to listen to and respond to state changes of the exoPlayer.
 */
private fun playbackStateListener(context: Context) = object : Player.EventListener {
    override fun onPlaybackStateChanged(state: Int) {
        val stateString: String = when (state) {
            ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE              _"
            ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
            ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
            ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
            else -> "UNKNOWN_STATE             -"
        }
        if (state == ExoPlayer.STATE_BUFFERING) {
            Toast.makeText(context, "Loading...", Toast.LENGTH_LONG).show()
        }
        Log.d(TAG, "change state to $stateString")
    }
}