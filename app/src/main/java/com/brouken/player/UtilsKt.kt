package com.brouken.player

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.Format
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.video.VideoFrameMetadataListener

object UtilsKt {

    @JvmStatic
    fun calculateFrameRateOnTheFly(player: ExoPlayer, onFrameRateCalculated: (Double) -> Unit) {
        // Ignore the first 30 frames (often unstable due to decoder spin-up/syncing)
        val ignoreSamples = 30

        val samplesToCollect = 60
        val totalSamplesNeeded = samplesToCollect + ignoreSamples
        val timestamps = ArrayList<Long>(totalSamplesNeeded)
        val mainHandler = Handler(Looper.getMainLooper())

        player.setVideoFrameMetadataListener(
            object : VideoFrameMetadataListener {
                override fun onVideoFrameAboutToBeRendered(
                    presentationTimeUs: Long,
                    releaseTimeNs: Long,
                    format: Format,
                    mediaFormat: android.media.MediaFormat?
                ) {
                    if (timestamps.size < totalSamplesNeeded) {
                        timestamps.add(presentationTimeUs)

                        if (timestamps.size == totalSamplesNeeded) {
                            val listener = this
                            mainHandler.post {
                                player.clearVideoFrameMetadataListener(listener)
                            }

                            // Calculate gaps between consecutive frames
                            var validGaps = 0
                            var totalValidDurationUs = 0L

                            for (i in (ignoreSamples + 1) until timestamps.size) {
                                val gap = timestamps[i] - timestamps[i - 1]

                                // Protect against dropped frames or discontinuities.
                                // A normal frame gap shouldn't be larger than ~50ms (20fps minimum).
                                // If gap is larger than 60ms, it's likely a dropped frame, so we ignore it in the math.
                                if (gap in 1..60_000) {
                                    totalValidDurationUs += gap
                                    validGaps++
                                }
                            }

                            if (validGaps > 0) {
                                val avgDurationUs = totalValidDurationUs.toDouble() / validGaps
                                var frameRate = 1_000_000.0 / avgDurationUs

                                when (frameRate) {
                                    in 23.95..23.988 -> frameRate = 24000.0 / 1001.0
                                    in 23.988..24.1 -> frameRate = 24.0
                                    in 24.9..25.1 -> frameRate = 25.0
                                    in 29.95..29.985 -> frameRate = 30000.0 / 1001.0
                                    in 29.985..30.1 -> frameRate = 30.0
                                    in 49.9..50.1 -> frameRate = 50.0
                                    in 59.9..59.97 -> frameRate = 60000.0 / 1001.0
                                }

                                Log.i("JustPlayer", "Calculated frame rate: $frameRate")
                                mainHandler.post {
                                    onFrameRateCalculated(frameRate)
                                }
                            }
                        }
                    }
                }
            }
        )
    }

}
