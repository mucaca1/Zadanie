package com.example.madam.ui.adapters

import android.net.Uri
import android.widget.Toast
import androidx.databinding.BindingAdapter
import com.example.madam.data.db.repositories.model.VideoItem
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory


class VideoPlayerBindingAdapter {
    companion object {

        private var playersMap: MutableMap<Int, SimpleExoPlayer>  = mutableMapOf()

        private var currentPlayingVideo: Pair<Int, SimpleExoPlayer>? = null
        fun pauseCurrentPlayingVideo(){
            if (currentPlayingVideo != null){
                currentPlayingVideo?.second?.playWhenReady = false
            }
        }

        fun releaseRecycledPlayers(index: Int){
            playersMap[index]?.release()
        }

        fun playIndexThenPausePreviousPlayer(index: Int){
            if (playersMap.get(index)?.playWhenReady == false) {
                pauseCurrentPlayingVideo()
                playersMap.get(index)?.playWhenReady = true
                currentPlayingVideo = Pair(index, playersMap.get(index)!!)
            }
        }

        fun togglePlayingState(player: Player) {
            if (currentPlayingVideo != null) {
                if (currentPlayingVideo!!.second.playWhenReady) {
                    currentPlayingVideo!!.second.playWhenReady = false
                } else {
                    playIndexThenPausePreviousPlayer(playersMap.values.indexOf(player))
                }
            }
        }

        fun releaseAll() {
            playersMap.map { it.value.release() }
        }

        fun removeFromPlayers(player: Player) {
            val key = playersMap.entries.first { e -> e.value == player }.key
            playersMap.remove(key)
        }

        @JvmStatic
        @BindingAdapter(value = ["playerObject", "item_index"])
        fun PlayerView.setPlayerObject(item: VideoItem, item_index: Int? = null) {
            val player = SimpleExoPlayer.Builder(context).build()
            val mediaSource = ProgressiveMediaSource.Factory(DefaultHttpDataSourceFactory("Demo")).createMediaSource(
                MediaItem.fromUri(Uri.parse(item.video_url)))

            player.playWhenReady = true
            this.useController = false
            setKeepContentOnPlayerReset(true)
            player.repeatMode = Player.REPEAT_MODE_ALL

            player.setMediaSource(mediaSource)
            player.prepare()

            this.player = player

            if (playersMap.containsKey(item_index))
                playersMap.remove(item_index)
            if (item_index != null)
                playersMap[item_index] = player

            this.player!!.addListener(object : Player.EventListener {
                override fun onPlayerError(error: ExoPlaybackException) {
                    super.onPlayerError(error)
                    Toast.makeText(
                        this@setPlayerObject.context,
                        "Whoops! Your stone age phone does not carry this much load at once.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }
}

