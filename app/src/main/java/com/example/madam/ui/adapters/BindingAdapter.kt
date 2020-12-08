package com.example.madam.ui.adapters

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.example.madam.R
import com.example.madam.data.db.repositories.model.VideoItem
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@BindingAdapter("ownerPhoto")
fun ImageView.setPostOwnerPhoto(item: VideoItem) {
    if (item.user_image_url.isEmpty() || item.user_image_url == "") {
        Glide.with(this)
            .load(R.drawable.user)
            .override(80, 80)
            .circleCrop()
            .into(this)
    } else {
        Glide.with(this)
            .load(item.user_image_url)
            .override(80, 80)
            .circleCrop()
            .into(this)
    }
}

@BindingAdapter("formattedCreatedAt")
fun TextView.setFormattedCreatedAt(item: VideoItem) {
    this.text = LocalDateTime.parse(item.created_at, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.MEDIUM))
}

@BindingAdapter("formattedUsername")
fun TextView.setFormattedUsername(item: VideoItem) {
    this.text = item.username
}