package com.example.voicerecoder

import android.media.MediaPlayer
import android.media.PlaybackParams
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.core.content.res.ResourcesCompat
import com.example.voicerecoder.databinding.ActivityAudioPlayerBinding
import com.google.android.material.chip.Chip

class AudioPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAudioPlayerBinding

    private lateinit var mediaPlayer : MediaPlayer
    private lateinit var runnable: Runnable
    private lateinit var handler: Handler
    private var delay = 200L
    private var jumpValue = 1000

    private var playSpeed = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var filepath = intent.getStringExtra("filepath")
        var filename = intent.getStringExtra("filename")

        mediaPlayer = MediaPlayer()
        mediaPlayer.apply {
            setDataSource(filepath)
            prepare()
        }

        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            binding.seekBar.progress = mediaPlayer.currentPosition
            handler.postDelayed(runnable, delay)
        }

        binding.btnPlay.setOnClickListener{
            playPausePlayer()
        }

        playPausePlayer()
        binding.seekBar.max = mediaPlayer.duration

        mediaPlayer.setOnCompletionListener {
            binding.btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_play_media, theme)
            handler.removeCallbacks(runnable)
        }

        binding.btnForward.setOnClickListener{
            mediaPlayer.seekTo(mediaPlayer.currentPosition + jumpValue)
            binding.seekBar.progress += jumpValue
        }

        binding.btnBackward.setOnClickListener{
            mediaPlayer.seekTo(mediaPlayer.currentPosition - jumpValue)
            binding.seekBar.progress -= jumpValue
        }

        binding.chip.setOnClickListener{
            if (playSpeed !=2f)
                playSpeed += 0.5f
            else
                playSpeed = 0.5f

            mediaPlayer.playbackParams = PlaybackParams().setSpeed(playSpeed)
            binding.chip.text = "% $playSpeed"
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (p2)
                    mediaPlayer.seekTo(p1)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })
    }

    private fun playPausePlayer() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
            binding.btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_pause_media, theme)
            handler.postDelayed(runnable, delay)
        } else {
            mediaPlayer.pause()
            binding.btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_play_media, theme)
            handler.removeCallbacks(runnable)
        }
    }
}