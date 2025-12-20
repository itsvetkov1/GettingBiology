package com.znam.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import android.os.Handler
import android.os.Looper
import android.widget.RelativeLayout


class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Enable edge-to-edge display and handle system window insets
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        val welcomeAnimationView = findViewById<ImageView>(R.id.welcomeAnimationView)
        val staticImageView = findViewById<RelativeLayout>(R.id.staticImageView)
        val startQuizButton = findViewById<Button>(R.id.startQuizButton)

        // Load the GIF animation
        Glide.with(this)
            .load(R.drawable.welcome_animation)
            .into(welcomeAnimationView)

        // Set a delay for the GIF, then show the static image and button
        Handler(Looper.getMainLooper()).postDelayed({
            welcomeAnimationView.visibility = View.GONE
            staticImageView.visibility = View.VISIBLE
        }, 3000) // 3000 milliseconds delay

        // Set the click listener for the button
        startQuizButton.setOnClickListener {
            navigateToQuiz()
        }
    }

    private fun navigateToQuiz() {
        val intent = Intent(this, SelectQuizActivity::class.java)
        startActivity(intent)
    }

}
