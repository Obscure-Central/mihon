package eu.kanade.tachiyomi.ui.reader.viewer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerNotificationManager
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.ui.main.MainActivity
import tachiyomi.core.common.util.system.logcat

@OptIn(UnstableApi::class)
class MusicPlayer(private val context: Context) {

//    private val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private var playerNotificationManager: PlayerNotificationManager? = null

    init {
        player = ExoPlayer.Builder(context).build()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        playerNotificationManager = PlayerNotificationManager.Builder(
            context,
            NOTIFICATION_ID,
            NOTIFICATION_CHANNEL_ID
        )
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): CharSequence {
                    return "Tachiyomi - Music Player"
                }

                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    val intent = Intent(context, MainActivity::class.java)
                    return PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }

                override fun getCurrentContentText(player: Player): CharSequence? {
                    return "Playing"
                }

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback,
                ): Bitmap? {
                    return null
                }
            })
            .setNotificationListener(
                object : PlayerNotificationManager.NotificationListener {
                    override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {}
                },
            )
            .build()

        playerNotificationManager?.setPlayer(player)
    }

    fun playAudio(url: String) {
        val uri = Uri.parse(url)
        val mediaItem = MediaItem.fromUri(uri)
        player!!.setMediaItem(mediaItem)
        player!!.prepare()
        player!!.play()
        isPlaying = true

        updateNotificationProgress()

        player!!.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        releasePlayer()
                    }
                }

                override fun onPositionDiscontinuity(
                    reason: Int
                ) {
                    super.onPositionDiscontinuity(reason)
                    updateNotificationProgress()
                }

                override fun onIsLoadingChanged(isLoading: Boolean) {
                    super.onIsLoadingChanged(isLoading)
                    updateNotificationProgress()
                }

                override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) {
                    super.onSeekBackIncrementChanged(seekBackIncrementMs)
                    updateNotificationProgress()
                }
            }
        )
    }

    private fun updateNotificationProgress() {
        val currentPosition = player!!.currentPosition
        val duration = player!!.duration
        val progress = if (duration > 0) {
            (currentPosition.toFloat() / duration.toFloat() * 100).toInt()
        } else {
            0
        }

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Tachiyomi - Music Player")
            .setContentText("Playing")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setContentIntent(createPendingIntent())
            .build()

        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun stopAudio() {
        if (isPlaying) {
            player?.stop()
            isPlaying = false
        }
    }

    fun releasePlayer() {
        player?.release()
        playerNotificationManager?.setPlayer(null)
        isPlaying = false
    }

    private fun createNotificationChannel() {
        val name = "Music Playback"
        val descriptionText = "Notifications for music playback"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        notificationManager?.createNotificationChannel(channel)
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "media_playback_channel"
        private var player: ExoPlayer? = null
        private var notificationManager: NotificationManager? = null

        private var isPlaying: Boolean = false

        fun isPlaying(): Boolean {
            return isPlaying
        }

        fun setPlaying(playing: Boolean) {
            isPlaying = playing
        }

        fun stopPlaying() {
            player?.stop()
            player?.release()
            notificationManager?.cancel(NOTIFICATION_ID)
            notificationManager?.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID)
            notificationManager = null
            player = null
            isPlaying = false
        }
    }
}
