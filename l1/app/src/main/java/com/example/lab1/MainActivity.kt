package com.example.lab1

import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.lab1.calculator.CalculatorEngine
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    private val engine = CalculatorEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tvExpression = findViewById<TextView>(R.id.tvExpression)
        val tvDisplay = findViewById<TextView>(R.id.tvDisplay)

        fun render() {
            val s = engine.state()
            tvExpression.text = s.expression
            tvDisplay.text = s.display
        }

        tvDisplay.setOnClickListener {
            val value = engine.state().display
            if (value.isNotBlank() && value != "Error") {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, value)
                }
                val chooser = Intent.createChooser(
                    sendIntent,
                    getString(R.string.share_result_via)
                )
                startActivity(chooser)
            }
        }

        tvDisplay.setOnLongClickListener {
            val value = engine.state().display
            if (value.isNotBlank() && value != "Error") {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(getString(R.string.clipboard_label_result), value)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
            }
            true
        }

        fun bindDigit(id: Int, d: Char) {
            findViewById<MaterialButton>(id).setOnClickListener {
                engine.inputDigit(d)
                render()
            }
        }

        fun bindOp(id: Int, op: Char) {
            findViewById<MaterialButton>(id).setOnClickListener {
                engine.inputOperator(op)
                render()
            }
        }

        bindDigit(R.id.btn0, '0')
        bindDigit(R.id.btn1, '1')
        bindDigit(R.id.btn2, '2')
        bindDigit(R.id.btn3, '3')
        bindDigit(R.id.btn4, '4')
        bindDigit(R.id.btn5, '5')
        bindDigit(R.id.btn6, '6')
        bindDigit(R.id.btn7, '7')
        bindDigit(R.id.btn8, '8')
        bindDigit(R.id.btn9, '9')

        bindOp(R.id.btnAdd, '+')
        bindOp(R.id.btnSub, '-')
        bindOp(R.id.btnMul, '*')
        bindOp(R.id.btnDiv, '/')

        findViewById<MaterialButton>(R.id.btnDot).setOnClickListener {
            engine.inputDecimalPoint()
            render()
        }
        findViewById<MaterialButton>(R.id.btnEq).setOnClickListener {
            engine.equals()
            render()
        }
        findViewById<MaterialButton>(R.id.btnClear).setOnClickListener {
            engine.clear()
            render()
        }
        findViewById<MaterialButton>(R.id.btnDel).setOnClickListener {
            engine.deleteLast()
            render()
        }
        findViewById<MaterialButton>(R.id.btnSign).setOnClickListener {
            engine.toggleSign()
            render()
        }

        render()
    }
}