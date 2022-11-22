package com.example.mymp3playerdb

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Build.VERSION_CODES.P
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.SearchView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.PagerAdapter
import com.example.mymp3playerdb.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {
    companion object {
        val REQ_READ = 99
        val DB_NAME = "musicDB"
        var VERSION = 1
    }

//    lateinit var toggle: ActionBarDrawerToggle
    lateinit var binding: ActivityMainBinding
    lateinit var adapter: Mp3Adapter
    private var Click = false
    private var rotateOpen: Animation? = null
    private var rotateClose: Animation? = null
    private var fromBottom: Animation? = null
    private var toBottom: Animation? = null
    private var musicList: MutableList<Music>? = mutableListOf<Music>()

    //승인받을 퍼미션 항목 요청
    val permission = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rotateOpen = AnimationUtils.loadAnimation(this, R.anim.from_bottom_anim)
        rotateClose = AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim)
        fromBottom = AnimationUtils.loadAnimation(this,R.anim.from_bottom_anim)
        toBottom = AnimationUtils.loadAnimation(this,R.anim.to_bottom_anim)

        binding.efab.setOnClickListener {
            onAddButtonClicked()
        }

        //액션바 툴바로 대체
        setSupportActionBar(binding.toolbar)

        //승인 점검
        if (isPermitted()) {
            startProcess()
        }else{
            // 외부저장소 읽기 권한이 없다면, 유저에게 읽기권한 신청
            ActivityCompat.requestPermissions(this, permission, REQ_READ)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_READ && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startProcess()
            Log.d("mymp3playerdb" ," 가능? ")
        }else{
            Log.d("mymp3playerdb" ," 확인 ")
            Toast.makeText(this, "권한요청", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun startProcess() {
        // 데이터베이스 음원정보 가져오기 없으면 공유메모리 음원정보 가져오기
        val dbHelper = DBHelper(this, MainActivity.DB_NAME, MainActivity.VERSION)
        musicList = dbHelper.selectMusicAll()
        //데이터 베이스에 없으면 콘텐트 리졸버를 통해 공유메모리 음원정보를 가져온다.
        if (musicList == null) {
            val playMusicList = getMusicList()
            if (playMusicList != null) {
                for (i in 0..playMusicList.size - 1) {
                    val music = playMusicList.get(i)
                    dbHelper.insertMusic(music)
                }
                musicList = playMusicList
            } else {
                Log.d("mymp3playerdb", "MainActivity.startProcess() :외장메모리 음원파일없음")
            }
        }
        //4. 리사이클러뷰 제공
        adapter = Mp3Adapter(this, musicList)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun getMusicList(): MutableList<Music>? {
        var imsiMusicList: MutableList<Music>? = mutableListOf<Music>()
        // 1. 공유메모리 음원정보 주소
        val musicURL = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        // 2. 음원에서 가져올 정보 배열
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
        )
        // 3. 콘텐트리졸버 쿼리 작성 통해서 musicList
        val cursor = contentResolver.query(musicURL, projection, null, null, null)
        if (cursor?.count!! > 0) {
            while (cursor!!.moveToNext()) {
                val id = cursor.getString(0)
                val title = cursor.getString(1).replace("'", "")
                val artist = cursor.getString(2).replace("'", "")
                val albumId = cursor.getString(3)
                val duration = cursor.getInt(4)
                val music = Music(id, title, artist, albumId, duration, 0)
                imsiMusicList?.add(music)
            }
        } else {
            imsiMusicList = null
        }
        return imsiMusicList
    }

    //사용하는 앱이 외부저장소를 읽을권한이 있는지 체크
    fun isPermitted(): Boolean {
        //승인이 되었는지 점검
        return ContextCompat.checkSelfPermission(
            this,
            permission[0]
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        //메뉴에서 서치항목을 찾아야함
        val searchMenu = menu?.findItem(R.id.menu_search)
        val searchView = searchMenu?.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                val dbHelper =
                    DBHelper(applicationContext, MainActivity.DB_NAME, MainActivity.VERSION)
                if(query.isNullOrBlank()){
                    dbHelper.selectMusicAll()?.let { musicList?.addAll(it) }
                    adapter.notifyDataSetChanged()
                }else{
                    musicList?.clear()
                    dbHelper.searchMusic(query)?.let { musicList?.addAll(it) }
                    adapter.notifyDataSetChanged()
                }
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    fun onAddButtonClicked() {
        setVisibility(Click)
        setAnimation(Click)
        Click = !Click
    }

    fun setVisibility(click: Boolean) {
        if (!click) {
            binding.flLove.visibility = View.VISIBLE
            binding.flAll.visibility = View.VISIBLE
        } else {
            binding.flLove.visibility = View.GONE
            binding.flAll.visibility = View.GONE
        }
    }

    fun setAnimation(click: Boolean) {
        if (!click) {
            binding.flLove.startAnimation(fromBottom)
            binding.flAll.startAnimation(fromBottom)
            binding.efab.startAnimation(rotateOpen)
        } else {
            binding.flLove.startAnimation(toBottom)
            binding.flAll.startAnimation(toBottom)
            binding.efab.startAnimation(rotateClose)
        }
    }

    fun onClickView (view: View){
        val dbHelper = DBHelper(applicationContext, MainActivity.DB_NAME, MainActivity.VERSION)
        when(view.id){
            R.id.fl_love ->{
                musicList?.clear()
                dbHelper.selectMusicLike()?.let { musicList?.addAll(it) }
                adapter.notifyDataSetChanged()
            }
            R.id.fl_all -> {
                musicList?.clear()
                dbHelper.selectMusicAll()?.let { musicList?.addAll(it) }
                adapter.notifyDataSetChanged()
            }
        }
    }
}