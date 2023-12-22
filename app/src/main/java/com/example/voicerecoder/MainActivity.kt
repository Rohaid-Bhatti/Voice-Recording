package com.example.voicerecoder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.inputmethod.InputBinding
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.room.Room
import com.example.voicerecoder.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.Date

const val REQUEST_CODE = 200

class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {
    private lateinit var binding: ActivityMainBinding

    private lateinit var amplitudes: ArrayList<Float>
    private lateinit var mediaRecorder: MediaRecorder
    private var isRecording = false
    private var isPause = false
    private var permission = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false
    private var duration = ""

    private lateinit var db : AudioRecordDB

    private lateinit var timer: Timer
    private lateinit var vibrator: Vibrator
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private var dirPath = ""
    private var fileName = ""

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionGranted = ActivityCompat.checkSelfPermission(this, permission[0]) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted)
            ActivityCompat.requestPermissions(this, permission, REQUEST_CODE)

        db = Room.databaseBuilder(
            this,
            AudioRecordDB::class.java,
            "audioRecords"
        ).build()

        bottomSheetBehavior = BottomSheetBehavior.from(binding.includedBottomSheet.root)
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        timer = Timer(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        binding.btnPlay.setOnClickListener {
            when {
                isPause -> resumeRecording()
                isRecording -> pauseRecording()
                else -> startRecording()
            }
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }

        binding.btnList.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java,))
        }

        binding.btnDone.setOnClickListener {
            stopRecording()
            Toast.makeText(this, "Record Saved", Toast.LENGTH_SHORT).show()

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            binding.bottomSheetBG.visibility = View.VISIBLE
            binding.includedBottomSheet.filenameInput.setText(fileName)
        }

        binding.includedBottomSheet.btnCancel.setOnClickListener {
            File("$dirPath$fileName.mp3").delete()
            dismiss()
        }

        binding.includedBottomSheet.btnOk.setOnClickListener {
            dismiss()
            save()
        }

        binding.bottomSheetBG.setOnClickListener{
            File("$dirPath$fileName.mp3").delete()
            dismiss()
        }

        binding.btnDelete.setOnClickListener {
            stopRecording()
            File("$dirPath$fileName.mp3").delete()
            Toast.makeText(this, "Record Delete", Toast.LENGTH_SHORT).show()
        }

        binding.btnDelete.isClickable = false
    }

    private fun save() {
        val newFilename = binding.includedBottomSheet.filenameInput.text.toString()
        if (newFilename != fileName) {
            val newFile = File("$dirPath$newFilename.mp3")
            File("$dirPath$fileName.mp3").renameTo(newFile)
        }

        val filePath = "$dirPath$newFilename.mp3"
        val timeStamp = Date().time
        val ampsPath = "$dirPath$newFilename"

        try {
            val fos = FileOutputStream(ampsPath)
            val out = ObjectOutputStream(fos)
            out.writeObject(amplitudes)
            fos.close()
            out.close()
        } catch (_: IOException) {}

        val record = AudioRecord (newFilename, filePath, timeStamp, duration, ampsPath)

        GlobalScope.launch {
            db.audioRecordDao().insert(record)
        }
    }

    private fun dismiss() {
        binding.bottomSheetBG.visibility = View.GONE
        hideKeyboard(binding.includedBottomSheet.filenameInput)

        Handler(Looper.getMainLooper()).postDelayed({
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }, 100)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as  InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE)
            permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    private fun pauseRecording() {
        mediaRecorder.pause()
        isPause = true
        binding.btnPlay.setImageResource(R.drawable.ic_play)

        timer.pause()
    }

    private fun resumeRecording() {
        if (isRecording && isPause && mediaRecorder != null) {
            try {
                mediaRecorder.resume()
                isPause = false
                binding.btnPlay.setImageResource(R.drawable.ic_pause)

                timer.start()
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle the exception (log or show a message)
            }
        }
    }

    private fun startRecording() {
        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, permission, REQUEST_CODE)
            return
        }

        mediaRecorder = MediaRecorder()
        dirPath = "${externalCacheDir?.absolutePath}/"

        val simpleDateFormat = SimpleDateFormat("yyyy.MM.DD_hh.mm.ss")
        val date = simpleDateFormat.format(Date())

        fileName = "audio_record_$date"

        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$fileName.mp3")

            try {
                prepare()
            } catch (_: IOException) {}

            start()
        }

        binding.btnPlay.setImageResource(R.drawable.ic_pause)
        isRecording = true
        isPause = false

        timer.start()

        binding.btnDelete.isClickable = true
        binding.btnDelete.setImageResource(R.drawable.ic_delete)

        binding.btnList.visibility = View.GONE
        binding.btnDone.visibility = View.VISIBLE
    }

    private fun stopRecording() {
        timer.stop()

        mediaRecorder.apply {
            stop()
            release()
        }

        isRecording = false
        isPause = false

        binding.btnList.visibility = View.VISIBLE
        binding.btnDone.visibility = View.GONE
        binding.btnDelete.isClickable = false
        binding.btnDelete.setImageResource(R.drawable.ic_delete_disable)
        binding.btnPlay.setImageResource(R.drawable.ic_play)

        binding.tvTimer.text = "00.00.00"
        amplitudes = binding.waveFormView.clear()
    }

    override fun onTimerTick(duration: String) {
        binding.tvTimer.text = duration
        this.duration = duration.dropLast(3)
        binding.waveFormView.addAmplitude(mediaRecorder.maxAmplitude.toFloat())
    }
}