package com.example.mymp3playerdb

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mymp3playerdb.databinding.ActivityMusicplayerBinding
import kotlinx.coroutines.*
import java.text.SimpleDateFormat

class MusicplayerActivity : AppCompatActivity() {
    companion object {
        val ALBUM_SIZE = 80
    }

    private lateinit var binding: ActivityMusicplayerBinding
    private var playList: MutableList<Parcelable>? = null
    private var position: Int = 0
    private var music: Music? = null
    private var mediaPlayer: MediaPlayer? = null
    private var messengerJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMusicplayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playList = intent.getParcelableArrayListExtra("playList")
        position = intent.getIntExtra("position", 0)
        music = playList?.get(position) as Music

        // 화면에 바인딩 하기
        binding.tvAlbumTitle.text = music?.title
        binding.tvAlbumArtist.text = music?.artist
        binding.totalDuration.text = SimpleDateFormat("mm:ss").format(music?.duration)
        binding.playDuration.text = "00:00"
        val bitmap = music?.getAlbumImage(this, ALBUM_SIZE)
        if (bitmap != null) {
            binding.ivAlbumImage.setImageBitmap(bitmap)
        } else {
            binding.ivAlbumImage.setImageResource(R.drawable.music3)
        }

        //음악 등록
        mediaPlayer = MediaPlayer.create(this, music?.getMusicUri())

        //시크바 음악재생위치 변경
        binding.seekBar.max = mediaPlayer!!.duration
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                Log.d("mymp3playerdb", "시크바 움직임")
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                Log.d("mymp3playerdb", "시크바 멈충")
            }
        })

        //이벤트 설정 목록 버튼
        binding.listButton.setOnClickListener {
            mediaPlayer?.stop()
            messengerJob?.cancel()
            finish()
        }

        //정지버튼 이벤트 설정
        binding.stopButton.setOnClickListener {
            mediaPlayer?.stop()
            messengerJob?.cancel()
            mediaPlayer = MediaPlayer.create(this, music?.getMusicUri())
            binding.seekBar.progress = 0
            binding.playDuration.text = "00:00"
            binding.playButton.setImageResource(R.drawable.ic_play_24)
        }
        //재생버튼 이벤트 설정
        binding.playButton.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                binding.playButton.setImageResource(R.drawable.ic_play_24)
            } else {
                mediaPlayer?.start()
                binding.playButton.setImageResource(R.drawable.ic_pause_24)

                val backgroundScope = CoroutineScope(Dispatchers.Default + Job())
                messengerJob = backgroundScope.launch {
                    while (mediaPlayer?.isPlaying == true) {
                        runOnUiThread {
                            var currentPosition = mediaPlayer?.currentPosition!!
                            binding.seekBar.progress = currentPosition
                            val currentDuration =
                                SimpleDateFormat("mm:ss").format(mediaPlayer!!.currentPosition)
                            binding.playDuration.text = currentDuration
                        }
                        try {
                            // 딜레이 설정
                            delay(1000)
                        } catch (e: Exception) {
                            Log.d("로그", "스레드 오류 발생")
                        }
                    }//end of while
                    runOnUiThread{
                        if(mediaPlayer!!.currentPosition >= (binding.seekBar.max - 1000)){
                            binding.seekBar.progress = 0
                            binding.playDuration.text = "00:00"
                        }
                        binding.playButton.setImageResource(R.drawable.ic_play_24)
                    }
                }//end of messengerJob
            }
        }
        binding.btnNext.setOnClickListener{
            nextMusic(1)
        }
        binding.btnBack.setOnClickListener{
            nextMusic(2)
        }
    }
    fun nextMusic(type : Int) {
        val playList: ArrayList<Parcelable> = playList as ArrayList<Parcelable>
        val intent = Intent(binding.root.context, MusicplayerActivity::class.java)
        intent.putExtra("playList", playList)
        val position =
            when(type){
                1 -> {position - 1}
                else -> {position + 1}
            }
        when (position){
            -1 -> Toast.makeText(this, "처음곡", Toast.LENGTH_SHORT).show()
            else -> {
                intent.putExtra("position", position)
                binding.root.context.startActivity(intent)
                mediaPlayer?.stop()
                messengerJob?.cancel()
                finish()
            }
        }
    }
}