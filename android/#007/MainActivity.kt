package com.a001.choimemo

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class MainActivity : AppCompatActivity() {

    var gEditText: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        gEditText = findViewById(R.id.editTextTextMultiLine)
    }

    // 上書保存
    fun onSaveButton(view: View) {
        Toast.makeText(this, "上書保存ボタンを押しました", Toast.LENGTH_SHORT).show()
        // ファイルオープン
        var yOutputStreamWriter = OutputStreamWriter(openFileOutput("memo1.txt", MODE_PRIVATE))
        // EditTextに入力されているメモ内容をファイルに書き込む
        yOutputStreamWriter.write(gEditText?.text.toString())
        // ファイルを閉じる
        yOutputStreamWriter.close()

    }
    // 読込
    fun onLoadButton(view: View) {
        Toast.makeText(this, "読込ボタンを押しました", Toast.LENGTH_SHORT).show()
        // ファイルオープン
        var yInputStreamReader = InputStreamReader(openFileInput("memo1.txt"))
        // ファイルの内容を読み込む
        var yText: String = ""
        yText = yInputStreamReader.readText()
        // ファイルを閉じる
        yInputStreamReader.close()
        // EditTextに出力
        gEditText?.setText(yText)
    }
}