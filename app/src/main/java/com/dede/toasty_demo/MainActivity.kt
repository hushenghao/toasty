package com.dede.toasty_demo

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.dede.toasty.DialogToastyStrategy
import com.dede.toasty.Toasty

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        Toasty.init(
            newBase!!.applicationContext as Application,
            toastyStrategy = DialogToastyStrategy()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun toast(view: View) {
        Toasty.with().message("我是Toast！").show()
    }

    fun open(view: View) {
        startActivity(Intent(this, SecondActivity::class.java))
    }

    fun view(view: View) {
        val inflate = layoutInflater.inflate(R.layout.layout_custom_toast, null, false)
        Toasty.with().customView(inflate).gravity(Gravity.CENTER).show()
    }
}