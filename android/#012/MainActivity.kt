package com.a001.choimemo

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class MainActivity : AppCompatActivity() {

    // パーツの宣言
    var gEditText: EditText? = null
    var gInformationTextView: TextView? = null
    var gUpdateTextView: TextView? = null

    // EditTextに読み込んだテキスト
    var gText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // パーツの実態化
        gEditText = findViewById(R.id.editTextTextMultiLine)
        gInformationTextView = findViewById(R.id.InformationTextView)
        gUpdateTextView = findViewById(R.id.UpdateTextView)

        // テキスト読込
        fLoadText()

        // テキストが変更されたことを検出する
        gEditText?.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // ファイルに保存されている内容と比較して一致していたら
                if (gText == s.toString()) {
                    gUpdateTextView?.text = ""
                // 不一致だったら
                }else{
                    gUpdateTextView?.text = "＊"
                }
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }
        })

        // 色設定
        // ナビゲーションバー
        window.navigationBarColor = Color.rgb(0xad,0xbf,0xd9)
        // ステータスバーのアイコンの色を暗い色にする
        WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightStatusBars =true
        // ナビゲーションバーのアイコンの色を暗い色にする
        WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightNavigationBars=true
    }

    // 上書保存
    fun onSaveButton(view: View) {
        //Toast.makeText(this, "上書保存ボタンを押しました", Toast.LENGTH_SHORT).show()
        fInformationTextView("上書保存ボタンを押しました")
        // ファイルオープン
        var yOutputStreamWriter = OutputStreamWriter(openFileOutput("memo1.txt", MODE_PRIVATE))
        // EditTextに入力されているメモ内容をファイルに書き込む
        yOutputStreamWriter.write(gEditText?.text.toString())
        // ファイルを閉じる
        yOutputStreamWriter.close()
        // テキスト読込
        fLoadText()
        // キーボードを非表示にする
        fKeyboardOff()

    }
    // 読込
    fun onLoadButton(view: View) {
        //Toast.makeText(this, "読込ボタンを押しました", Toast.LENGTH_SHORT).show()
        fInformationTextView("読込ボタンを押しました")
        // テキスト読込
        fLoadText()
        // キーボードを非表示にする
        fKeyboardOff()
    }

    // 情報表示
    fun fInformationTextView(aMojiretsu: String){
        gInformationTextView?.text = aMojiretsu
    }

    // テキスト読込
    fun fLoadText(){
        // ファイルオープン
        var yInputStreamReader = InputStreamReader(openFileInput("memo1.txt"))
        // ファイルの内容を読み込む
        gText = yInputStreamReader.readText()
        // ファイルを閉じる
        yInputStreamReader.close()
        // EditTextに出力
        gEditText?.setText(gText)
    }

    // キーボードを非表示にする
    fun fKeyboardOff(){
        // キーボードを閉じる
        var yImm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        yImm.hideSoftInputFromWindow(gEditText?.windowToken, 0)
        // フォーカスを外す
        gUpdateTextView?.requestFocus()
    }
}









