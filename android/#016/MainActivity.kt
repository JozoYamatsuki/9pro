package com.a001.choimemo

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class MainActivity : AppCompatActivity() {

    // パーツの宣言
    var gEditText: EditText? = null
    var gInformationTextView: TextView? = null
    var gUpdateTextView: TextView? = null
    var gUnderLinearLayout: LinearLayout? = null
    var gEditScrollView: ScrollView? = null

    // EditTextに読み込んだテキスト
    var gText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // パーツの実態化
        gEditText = findViewById(R.id.editTextTextMultiLine)
        gInformationTextView = findViewById(R.id.InformationTextView)
        gUpdateTextView = findViewById(R.id.UpdateTextView)
        gUnderLinearLayout = findViewById(R.id.UnderLinearLayout)
        gEditScrollView = findViewById(R.id.EditScrollView)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // imeを取得
            var yIme = insets.getInsets(WindowInsetsCompat.Type.ime())
            // imeが表示されていれば
            if (yIme.bottom > 0) {
                // Padding値を計算する
                // yIme.bottomはimeの高さ+ナビゲーションBarの高さ、systemBars.bottomはナビゲーションBarの高さ
                var yBottom = yIme.bottom - systemBars.bottom - gUnderLinearLayout?.height!! - gInformationTextView?.height!!
                // マージンをセットする
                fSetMargin(gEditScrollView!!, yBottom)

            // imeが非表示なら
            }else{
                // マージンを元に戻す
                fSetMargin(gEditScrollView!!, 0)
                // カーソルを消す
                fCursorDelete()
            }

            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

        // レイアウトが確定した1回だけ実行される
        gEditScrollView?.doOnLayout { _ ->
            // minLinesをセット
            fSetMinLines()
        }

        // テキスト読込
        fLoadText()

        // 色設定
        // ナビゲーションバー
        window.navigationBarColor = Color.rgb(0xad,0xbf,0xd9)
        // ステータスバーのアイコンの色を暗い色にする
        WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightStatusBars =true
        // ナビゲーションバーのアイコンの色を暗い色にする
        WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightNavigationBars=true

    }

    // minLinesをセット
    fun fSetMinLines() {
        // ScrollViewの高さ÷EditTextの１行の高さ
        gEditText?.minLines = gEditScrollView?.height!! / gEditText?.lineHeight!! + 1
        gInformationTextView?.text = "${gEditScrollView?.height!!} / ${gEditText?.lineHeight!!}  = ${gEditText?.minLines}"
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
        try {
            // ファイルオープン
            var yInputStreamReader = InputStreamReader(openFileInput("memo1.txt"))
            // ファイルの内容を読み込む
            gText = yInputStreamReader.readText()
            // ファイルを閉じる
            yInputStreamReader.close()
            // EditTextに出力
            gEditText?.setText(gText)
        }catch (e: Exception){
            // EditTextに出力
            gEditText?.setText("")
            //情報表示
            fInformationTextView("読込失敗しました")
        }
    }

    // キーボードを非表示にする
    fun fKeyboardOff(){
        // キーボードを閉じる
        var yImm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        yImm.hideSoftInputFromWindow(gEditText?.windowToken, 0)
        // フォーカスを外す
        fCursorDelete()
    }

    // フォーカスを外す
    fun fCursorDelete(){
        // フォーカスを外す
        gUnderLinearLayout?.requestFocus()
    }

    // マージンを動的に変更する
    fun fSetMargin(view: View, xBottom: Int) {
        // MarginLayoutParamsにキャストする
        val yLayoutParams = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        // マージンのbottomをセットする
        yLayoutParams.bottomMargin = xBottom
        // 設定する
        view.layoutParams = yLayoutParams
    }

}









