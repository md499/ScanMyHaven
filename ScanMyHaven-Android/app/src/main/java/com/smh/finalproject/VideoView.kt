package com.smh.finalproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Button
import com.google.android.material.button.MaterialButton

class VideoView : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_view)


        val webVideo: WebView = findViewById(R.id.video)
        val backButton: Button = findViewById(R.id.backButton)

        val video: String =
            "<iframe width=\"400\" height=\"300\" src=\"https://www.youtube.com/embed/OEibM7djfxw\" title=\"How to use S.M.H\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"

        webVideo.loadData(video, "text/html", "utf-8")
        webVideo.getSettings().javaScriptEnabled = true
        webVideo.webChromeClient = WebChromeClient()


        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
