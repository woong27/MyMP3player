package com.example.mymp3playerdb

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mymp3playerdb.databinding.RecyclerItemBinding
import java.text.SimpleDateFormat

class Mp3Adapter(val context: Context, val musicList: MutableList<Music>?) :
    RecyclerView.Adapter<Mp3Adapter.CustomViewHolder>() {
    companion object{
        var ALBUM_SIZE = 80
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val binding = RecyclerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val binding = (holder as CustomViewHolder).binding
        val music = musicList?.get(position)
        //바인딩
        binding.tvArtist.text = music?.artist
        binding.tvTitle.text = music?.title
        binding.tvDuration.text = SimpleDateFormat("mm:ss").format(music?.duration)
        val bitmap = music?.getAlbumImage(context, Mp3Adapter.ALBUM_SIZE)
        if(bitmap != null){
            binding.ivAlbumArt.setImageBitmap(bitmap)
        }else{
            binding.ivAlbumArt.setImageResource(R.drawable.music3)
        }
        when (music?.likes){
            0 -> {
                binding.ivItemLike.setImageResource(R.drawable.ic_favorite_24)
            }
            1 -> {
                binding.ivItemLike.setImageResource(R.drawable.ic_love_24)
            }
        }
        binding.tvTitle.isSingleLine = true
        binding.tvTitle.ellipsize = TextUtils.TruncateAt.MARQUEE
        binding.tvTitle.isSelected = true
        //이벤트 처리
        binding.root.setOnClickListener{
            val playList: ArrayList<Parcelable>? = musicList as ArrayList<Parcelable>
            val intent = Intent(binding.root.context, MusicplayerActivity::class.java)
            intent.putExtra("playList", playList)
            intent.putExtra("position", position)
            intent.putExtra("music", music)
            binding.root.context.startActivity(intent)
        }
        // 좋아요 데이터 베이스 저장
        binding.ivItemLike.setOnClickListener{
            if(music?.likes == 0){
                binding.ivItemLike.setImageResource(R.drawable.ic_love_24)
                music?.likes = 1
            }else {
                binding.ivItemLike.setImageResource(R.drawable.ic_favorite_24)
                music?.likes = 0
            }
            if(music != null){
                val dbHelper = DBHelper(context, MainActivity.DB_NAME, MainActivity.VERSION)
                val flag = dbHelper.updateLike(music)
                if (flag == false){
                    Log.d("mymp3playerdb", "onBindViewHolder() 업데이트 실패")
                }else{
                    notifyDataSetChanged()
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return musicList?.size ?: 0
    }

    class CustomViewHolder(val binding: RecyclerItemBinding) : RecyclerView.ViewHolder(binding.root)
}