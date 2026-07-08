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

    override fun onStart() {
        // アクティビティのライフサイクルの確認用ログ出力
        fActivityTestLog("onStart")
        // 本来の処理を最初に実行する
        super.onStart()
    }

    override fun onResume() {
        // アクティビティのライフサイクルの確認用ログ出力
        fActivityTestLog("onResume")
        // 本来の処理を最初に実行する
        super.onResume()
    }

    override fun onPause() {
        // アクティビティのライフサイクルの確認用ログ出力
        fActivityTestLog("onPause")
        // 本来の処理を最初に実行する
        super.onPause()
    }

    override fun onStop() {
        // アクティビティのライフサイクルの確認用ログ出力
        fActivityTestLog("onStop")
        // 本来の処理を最初に実行する
        super.onStop()
        // 終了前に現在の情報を保存
        fStateSave()
    }

    override fun onDestroy() {
        // アクティビティのライフサイクルの確認用ログ出力
        fActivityTestLog("onDestroy")
        // 本来の処理を最初に実行する
        super.onDestroy()
    }

    override fun onRestart() {
        // アクティビティのライフサイクルの確認用ログ出力
        fActivityTestLog("onRestart")
        // 本来の処理を最初に実行する
        super.onRestart()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // アクティビティのライフサイクルの確認用ログ出力
        fActivityTestLog("onSaveInstanceState")
        // 本来の処理を最初に実行する
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // アクティビティのライフサイクルの確認用ログ出力
        fActivityTestLog("onRestoreInstanceState")
        // 本来の処理を最初に実行する
        super.onRestoreInstanceState(savedInstanceState)
        // 回転時は必ず復元するようxupdate.txtに復元要のサインを保存する
        // （元が""でも"＊"でも"回転"で上書きする）
        fTextFileWrite("xupdate.txt", "回転")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // アクティビティのライフサイクルの確認用ログ出力
        fActivityTestLog("onCreate")
        // 本来の処理を最初に実行する
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
            // アクティビティのライフサイクルの確認用ログ出力
            fActivityTestLog("gEditScrollView?.doOnLayout")
            // minLinesをセット
            fSetMinLines()
            // 終了前に保存しておいた情報を復元
            fStateRestore()
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
        // EditTextに入力されているメモ内容をファイルに書き込む
        fTextFileWrite("memo${gPageNo}.txt",gEditText?.text.toString())
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
        // ファイルの内容を読み込む
        var yText: String? = null
        yText = fTextFileRead("memo${gPageNo}.txt")
        // ファイルが存在するなら
        if (yText != null) {
            // ファイルの内容を取得する
            gText = yText
            // EditTextに出力
            gEditText?.setText(gText)
            //情報表示
            fInformationTextView("ページ[${gPageNo}]です")
            // ファイルが存在しなければ
        }else{
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

    // ファイル書き込み
    fun fTextFileWrite(aFileName: String, aText: String){
        // ファイルオープン
        var yOutputStreamWriter = OutputStreamWriter(openFileOutput(aFileName, MODE_PRIVATE))
        // EditTextに入力されているメモ内容をファイルに書き込む
        yOutputStreamWriter.write(aText)
        // ファイルを閉じる
        yOutputStreamWriter.close()
    }

    // ファイル読み込み
    fun fTextFileRead(aFileName: String):String? {
        var yText: String? = null
        try {
            // ファイルオープン
            var yInputStreamReader = InputStreamReader(openFileInput(aFileName))
            // ファイルの内容を読み込む
            yText = yInputStreamReader.readText()
            // ファイルを閉じる
            yInputStreamReader.close()
        }catch (e: Exception){
        }
        return yText
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

    // アクティビティのライフサイクルの確認用ログ出力
    fun fActivityTestLog(aLine: String){
//        // memo1.txtをオープン（アペンド）
//        var yOutputStreamWriter = OutputStreamWriter(openFileOutput("memo1.txt",MODE_APPEND))
//        // 追記する
//        yOutputStreamWriter.write("\n" + aLine)
//        // 閉じる
//        yOutputStreamWriter.close()
    }

    // 情報を保存
    fun fStateSave(){
        // xupdate.txtに変更中状態を保存する（"＊"ならページが復元要、""なら復元不要のサインとして使う）
        fTextFileWrite("xupdate.txt", gUpdateTextView?.text.toString())
        // xpageno.txtにgPageNoを保存する
        fTextFileWrite("xpageno.txt", "${gPageNo}")
        // xtext.txtに現在のEditTextの内容を保存する
        fTextFileWrite("xtext.txt", gEditText?.text.toString())
    }

    // 情報を復元
    fun fStateRestore(){
        // ファイルアクセスに失敗する状態に陥ると二度と起動できなくなるので念のためtryで囲む
        try {
            // xupdate.txtを読み込む
            var yUpdate: String? = fTextFileRead("xupdate.txt")
            // xpageno.txtを読み込む
            var yPageNo: String? = fTextFileRead("xpageno.txt")
            // xtext.txtを読み込む
            var yText: String? = fTextFileRead("xtext.txt")
            // 全てのファイルが存在していれば
            if (yUpdate != null && yPageNo != null && yText != null){
                // update.txtが空白でなければ（"＊"もしくは"回転"）
                if (yUpdate != "") {
                    // ページ番号を更新
                    gPageNo = yPageNo.toInt()
                    // 保存しておいたページ番号で一旦ページを読み込んで画面に反映する
                    fLoadText()
                    // 保存してあった情報で画面(EditText)を上書きする
                    gEditText?.setText(yText)
                }
            }
            // xupdate.txtをクリアする
            fTextFileWrite("xupdate.txt", "")
            // エラーを検出したとき
        }catch (e: Exception){
            // メッセージを表示する
            fInformationTextView("情報の復元に失敗しました")
        }
    }
}









