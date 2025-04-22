package com.example.lab4

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var videoView: VideoView
    private lateinit var mediaControls: LinearLayout
    private lateinit var urlLayout: LinearLayout
    private lateinit var pickMediaLauncher: ActivityResultLauncher<Intent>
    private lateinit var seekBar: SeekBar

    private var isVideo = false
    private var isPaused = false
    private var userIsSeeking = false
    val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val openButton = findViewById<Button>(R.id.open_button)
        val closeButton = findViewById<Button>(R.id.close_button)
        val toggleButton = findViewById<Button>(R.id.toggle_button)
        val urlInput = findViewById<EditText>(R.id.url_input)
        val urlButton = findViewById<Button>(R.id.url_button)
        val confirmUrl = findViewById<Button>(R.id.confirm_url_button)
        videoView = findViewById(R.id.video_view)
        mediaControls = findViewById(R.id.media_controls)
        urlLayout = findViewById(R.id.url_layout)
        seekBar = findViewById(R.id.seek_bar)

        openButton.setOnClickListener {
            openFile()
        }

        closeButton.setOnClickListener {
            stopPlayback()
        }

        urlButton.setOnClickListener {
            urlLayout.visibility = View.VISIBLE
        }

        toggleButton.setOnClickListener {
            if (isVideo) {
                if (videoView.isPlaying) {
                    videoView.pause()
                    isPaused = true
                } else {
                    videoView.start()
                    isPaused = false
                }
            } else {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.pause()
                        isPaused = true
                    } else {
                        it.start()
                        isPaused = false
                    }
                }
            }
        }

        confirmUrl.setOnClickListener {
            val url = urlInput.text.toString().trim()
            if (url.isNotEmpty()) {
                val uri = url.toUri()
                if (url.endsWith(".mp4")) {
                    playVideo(uri)
                } else if (url.endsWith(".mp3")) {
                    playAudio(uri)
                } else {
                    Toast.makeText(this, "Unsupported media type", Toast.LENGTH_SHORT).show()
                }
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    if (isVideo) {
                        videoView.seekTo(progress)
                    } else {
                        mediaPlayer?.seekTo(progress)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                userIsSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                userIsSeeking = false
            }
        })

        pickMediaLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                uri?.let {
                    val mimeType = contentResolver.getType(it)
                    if (mimeType?.startsWith("video") == true) {
                        playVideo(it)
                    } else if (mimeType?.startsWith("audio") == true) {
                        playAudio(it)
                    } else {
                        Toast.makeText(this, "Undefined type of file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun openFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/mpeg", "video/mp4"))
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        pickMediaLauncher.launch(Intent.createChooser(intent, "Open file"))
    }

    private fun stopPlayback() {
        handler.removeCallbacks(updateSeekBarRunnable)

        mediaControls.visibility = View.GONE

        seekBar.progress = 0

        if (isVideo) {
            videoView.stopPlayback()
            videoView.visibility = View.GONE
        } else {
            mediaPlayer?.release()
            mediaPlayer = null
        }

        isPaused = false
    }

    private fun playVideo(uri: Uri) {
        mediaPlayer?.release()
        mediaPlayer = null
        isVideo = true
        isPaused = false
        videoView.setVideoURI(uri)
        videoView.visibility = View.VISIBLE
        mediaControls.visibility = View.VISIBLE
        urlLayout.visibility = View.GONE
        seekBar.progress = 0

        videoView.setOnPreparedListener {
            seekBar.max = videoView.duration
            videoView.start()
            handler.post(updateSeekBarRunnable)
        }
    }

    private fun playAudio(uri: Uri) {
        videoView.stopPlayback()
        videoView.visibility = View.GONE
        mediaControls.visibility = View.VISIBLE
        urlLayout.visibility = View.GONE
        mediaPlayer?.release()
        isVideo = false
        isPaused = false
        seekBar.progress = 0

        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@MainActivity, uri)
            setOnPreparedListener {
                seekBar.max = it.duration
                it.start()
                handler.post(updateSeekBarRunnable)
            }
            prepareAsync()
        }
    }

    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            if (!userIsSeeking) {
                val currentPos = if (isVideo) videoView.currentPosition else mediaPlayer?.currentPosition ?: 0
                seekBar.progress = currentPos
            }
            handler.postDelayed(this, 500)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        handler.removeCallbacks(updateSeekBarRunnable)
    }
}
