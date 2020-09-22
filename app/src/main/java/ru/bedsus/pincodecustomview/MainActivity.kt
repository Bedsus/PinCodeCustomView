package ru.bedsus.pincodecustomview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pinView = findViewById<PinView>(R.id.vPinView)
        val errorCode = findViewById<TextView>(R.id.vErrorCode)
        val colorSuccess = ContextCompat.getColor(this@MainActivity, R.color.colorSuccess)
        val colorError = ContextCompat.getColor(this@MainActivity, R.color.colorError)
        val colorBorder = ContextCompat.getColor(this@MainActivity, R.color.border)

        pinView.onPinFullListener = { inputCode ->
            if (inputCode == getString(R.string.correct_code)) {
                errorCode.setText(R.string.success_text)
                errorCode.setTextColor(colorSuccess)
                pinView.itemBorderColor = colorSuccess
            } else {
                errorCode.setText(R.string.error_text)
                errorCode.setTextColor(colorError)
                pinView.itemBorderColor = colorError
            }
        }
        pinView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                errorCode.text = ""
                pinView.itemBorderColor = colorBorder
            }

            override fun afterTextChanged(p0: Editable?) { }
        })
    }
}