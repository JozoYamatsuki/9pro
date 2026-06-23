package com.a001.choimemo2

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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.marginBottom
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class MainActivity : AppCompatActivity() {

    // パーツの宣言
    var gEditText: EditText? = null
    var gInformationTextView: TextView? = null
    var gUpdateTextView: TextView? = null
    var gUnderLinearLayout: LinearLayout? = null
    var gEditScrollView: ScrollView? = null
    var gPageTextView: TextView? = null

    // EditTextに読み込んだテキスト
    var gText: String = ""

    // 最大ページ数
    var gPageMax: Int = 3
    // 現在表示中のページ番号
    var gPageNo: Int = 1

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
        gPageTextView = findViewById(R.id.PageTextView)

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
                // マージンが変更されていれば
                if (gEditScrollView!!.marginBottom != 0) {
                    // マージンを元に戻す
                    fSetMargin(gEditScrollView!!, 0)
                    // カーソルを消す
                    fCursorDelete()
                }
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
    }

        // 上書保存
    fun onSaveButton(view: View) {
        //Toast.makeText(this, "上書保存ボタンを押しました", Toast.LENGTH_SHORT).show()
        // ファイルオープン
        var yOutputStreamWriter = OutputStreamWriter(openFileOutput("memo${gPageNo}.txt", MODE_PRIVATE))
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
        // テキスト読込
        fLoadText()
        // キーボードを非表示にする
        fKeyboardOff()
    }

    // 前ページボタン
    fun onBeforeButton(view: View){
        // ページ変更可能かチェックして可能なら
        if (fCheckPageChange() == true) {
            // ページ番号を-1する
            gPageNo = gPageNo - 1
            // 1ページ未満になったら
            if (gPageNo < 1) {
                // ページ番号を最大ページ番号にする
                gPageNo = gPageMax
            }
            // ページ読込
            fLoadText()
        }
    }

    // 次ページボタン
    fun onNextButton(view: View){
        // ページ変更可能かチェックして可能なら
        if (fCheckPageChange() == true) {
            // ページ番号を+1する
            gPageNo = gPageNo + 1
            // 最大ページ番号を超えたら
            if (gPageNo > gPageMax) {
                // ページ番号を1にする
                gPageNo = 1
            }
            // ページ読込
            fLoadText()
        }
    }

    // 情報表示
    fun fInformationTextView(aMojiretsu: String){
        gInformationTextView?.text = aMojiretsu
    }

    // テキスト読込
    fun fLoadText(){
        try {
            // ファイルオープン
            var yInputStreamReader = InputStreamReader(openFileInput("memo${gPageNo}.txt"))
            // ファイルの内容を読み込む
            gText = yInputStreamReader.readText()
            // ファイルを閉じる
            yInputStreamReader.close()
            // EditTextに出力
            gEditText?.setText(gText)
            //情報表示
            fInformationTextView("ページ[${gPageNo}]です")
        }catch (e: Exception){
            // 読込前の内容をクリア
            gText = ""
            // EditTextに出力
            gEditText?.setText("")
            //情報表示
            fInformationTextView("新しいページ[${gPageNo}]です")
        }
        // ページ番号表示
        fPageTextView()
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

    // ページ番号表示
    fun fPageTextView(){
        // ページ番号を表示する
        gPageTextView?.text = "${gPageNo}"
    }

    // ページ変更可能かチェックする(true=可、false=不可)
    fun fCheckPageChange(): Boolean {
        // テキスト変更中なら
        if (gUpdateTextView?.text != ""){
            AlertDialog.Builder(this)
                .setMessage("内容が変更されています\n"+
                            "上書保存か変更破棄を行ってから再度操作してください")
                .show()
            // 不可
            return false
        }
        // 可
        return true
    }
}









