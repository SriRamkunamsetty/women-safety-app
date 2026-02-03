package com.project.safety

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.project.safety.utils.SharedPrefManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Handler(Looper.getMainLooper()).postDelayed({
            val sharedPref = SharedPrefManager.getInstance(this)

            val nextActivity = if (sharedPref.isLoggedIn()) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }

            startActivity(nextActivity)
            finish()
        }, 2000)
    }
}
