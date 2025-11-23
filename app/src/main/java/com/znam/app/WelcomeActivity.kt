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

        val welcomeAnimationView = findViewById<ImageView>(R.id.welcomeAnimationView)
        val staticImageView = findViewById<View>(R.id.staticImageView)
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
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

}
