package com.a001.choimemo2

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
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
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import java.io.InputStreamReader
import java.io.OutputStreamWriter

// ViewPager2用のアダプタ（３枚固定）
class Fixed3ViewPagerAdapter(private val context: Context)
    : RecyclerView.Adapter<Fixed3ViewPagerAdapter.ViewHolder>() {

    // 表示する3つのXMLレイアウトを順番に定義する
    private val layouts = listOf(
        R.layout.item0_page_left,
        R.layout.item1_page_center,
        R.layout.item2_page_right
    )

    // ViewHolderをFixed3ViewPagerAdapterクラスの内部クラスとして定義する
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    // Viewを（１つずつ）作ってViewHolderに詰めて返す
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 要求された位置（viewType）のXMLをViewに変換（インフレート）する
        val view = LayoutInflater.from(context).inflate(layouts[viewType], parent, false)
        // viewをViewHolderに詰めて返す
        return ViewHolder(view)
    }

    // 各itemに対するデフォルト処理
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // 表示時に必ず何かするというようなデフォルト処理は特に無い
    }

    // item数を取得する（結果的に3が返る）
    override fun getItemCount(): Int = layouts.size

    // どのデザイン（レイアウト）を使うかを返す（３枚固定なので結果的にpositionをそのまま返す）
    override fun getItemViewType(position: Int): Int = position
}

class MainActivity : AppCompatActivity() {

    // パーツの宣言
    var gEditText: EditText? = null
    var gInformationTextView: TextView? = null
    var gUpdateTextView: TextView? = null
    var gUnderLinearLayout: LinearLayout? = null
    var gEditScrollView: ScrollView? = null
    var gPageTextView: TextView? = null
    var gViewPager2: ViewPager2? = null
    var gPageLeftTextView: TextView? = null
    var gPageRightTextView: TextView? = null

    // EditTextに読み込んだテキスト
    var gText: String = ""

    // 最大ページ数
    var gPageMax: Int = 5
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
        // gViewPager2に関連するgEditTextとgEditScrollViewなどはgViewPager2?.doOnLayoutで実態化する
        //gEditText = findViewById(R.id.editTextTextMultiLine)
        gInformationTextView = findViewById(R.id.InformationTextView)
        gUpdateTextView = findViewById(R.id.UpdateTextView)
        gUnderLinearLayout = findViewById(R.id.UnderLinearLayout)
        //gEditScrollView = findViewById(R.id.EditScrollView)
        gPageTextView = findViewById(R.id.PageTextView)

        // ViewPager2
        gViewPager2 = findViewById(R.id.EditViewPager2)
        // アダプタ（３枚固定）を生成する
        gViewPager2?.adapter = Fixed3ViewPagerAdapter(this)
        // 現在のページの「両隣に何枚のページをあらかじめ生成しておくか」を指定する
        gViewPager2?.offscreenPageLimit = 1
        // 現在のページを指定する
        gViewPager2?.setCurrentItem(1,false)
        // スワイプ時のコールバックを設定する（スクロール終了を検出してページ変更するのに使用する）
        gViewPager2?.registerOnPageChangeCallback(cViewPager2PageChangeCallback)

        // ステータスバーやナビゲーションバーなどのパディング調整
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            // システムバーの情報を取得する
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // EditViewが実態化されているか確認する（起動途中の場合は実態化がまだ）
            if (gEditScrollView != null) {
                // imeを取得
                var yIme = insets.getInsets(WindowInsetsCompat.Type.ime())
                // imeが表示されていれば
                if (yIme.bottom > 0) {
                    // Padding値を計算する
                    // yIme.bottomはimeの高さ+ナビゲーションBarの高さ、systemBars.bottomはナビゲーションBarの高さ
                    var yBottom =
                        yIme.bottom - systemBars.bottom - gUnderLinearLayout?.height!! - gInformationTextView?.height!!
                    // マージンをセットする
                    fSetMargin(gEditScrollView!!, yBottom)

                    // imeが非表示なら
                } else {
                    // マージンが変更されていれば
                    if (gEditScrollView!!.marginBottom != 0) {
                        // マージンを元に戻す
                        fSetMargin(gEditScrollView!!, 0)
                        // カーソルを消す
                        fCursorDelete()
                    }
                }
            }
            // パディング調整
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // レイアウトが確定した1回だけ実行される
        gViewPager2?.doOnLayout { _ ->
            // アクティビティのライフサイクルの確認用ログ出力
            fActivityTestLog("gViewPager2?.doOnLayout")

            // gViewPager2に関連するパーツの実態化
            gEditText = findViewById(R.id.editTextTextMultiLine)
            gEditScrollView = findViewById(R.id.EditScrollView)
            gPageLeftTextView = findViewById(R.id.PageLeftTextView)
            gPageRightTextView = findViewById(R.id.PageRightTextView)

            // テキストが変更されたことを検出するリスナー（gEditText実態化後に記述する）
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

            // テキスト読込（gEditText実態化後、addTextChangedListener後）
            fLoadText()
            // minLinesをセット
            fSetMinLines()
            // 終了前に保存しておいた情報を復元
            fStateRestore()
        }


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
            // 前ページ（item:0）にスワイプ（smoothScroll）する
            gViewPager2?.setCurrentItem(0, true)
        }
    }

    // 次ページボタン
    fun onNextButton(view: View){
        // ページ変更可能かチェックして可能なら
        if (fCheckPageChange() == true) {
            // 次ページ（item:2）にスワイプ（smoothScroll）する
            gViewPager2?.setCurrentItem(2, true)
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
        // ページ変更操作可否を判定しセットする（上書保存または変更破棄による操作可否の更新のため）
        fCheckPageChange(false)

        // ViewPager2の左ページのテキストをセット
        var yPageNo0 = gPageNo - 1
        if (yPageNo0 < 1) yPageNo0 = gPageMax
        var yText0 = fTextFileRead("memo${yPageNo0}.txt")
        gPageLeftTextView?.setText(yText0)
        // ViewPager2の右ページのテキストをセット
        var yPageNo2 = gPageNo + 1
        if (yPageNo2 > gPageMax) yPageNo2 = 1
        var yText2 = fTextFileRead("memo${yPageNo2}.txt")
        gPageRightTextView?.setText(yText2)
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

    // ページ変更可能かチェックする(戻り値：true=可、false=不可、xMsgFlag：メッセージ表示 true=する、false=しない)
    fun fCheckPageChange(xMsgFlag: Boolean = true): Boolean {
        // テキスト変更中なら
        if (gUpdateTextView?.text != ""){
            // メッセージ表示するなら
            if (xMsgFlag == true) {
                // メッセージ表示
                AlertDialog.Builder(this)
                    .setMessage(
                        "内容が変更されています\n" +
                                "上書保存か変更破棄を行ってから再度操作してください"
                    )
                    .setOnDismissListener { // メッセージ消去時に操作可に戻す
                        // ViewPager2を操作可にする（スワイプ可能）
                        gViewPager2?.isUserInputEnabled = true
                    }
                    .show()
            }
            // ViewPager2を操作不可にする（スワイプさせないよう）
            gViewPager2?.isUserInputEnabled = false
            // スクロール状態（SCROLL_STATE_SETTLING）を解除（SCROLL_STATE_IDLE）する
            gViewPager2?.setCurrentItem(gViewPager2!!.currentItem, false)
            // 不可
            return false
        }
        // ViewPager2を操作可にする（スワイプ可能）
        gViewPager2?.isUserInputEnabled = true
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

    // ViewPager2のスワイプ時のコールバック（スクロール終了を検出してページ変更するのに使用する）
    val cViewPager2PageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrollStateChanged(state: Int) {
            when (state) {
                // スクロール終了直後
                ViewPager2.SCROLL_STATE_IDLE -> {
                    // 以下の処理は「onPageSelected」だとタイミング的に遅くスムーズな表示にならないためここで行う
                    if (gViewPager2 != null) {
                        // 前ページ（スクロール終了後にitem:0を表示している状態）
                        if (gViewPager2!!.currentItem == 0) {
                            // 前ページにする（画面はitem0が表示されていてitem1は右に隠れている状態）
                            gPageNo = gPageNo - 1
                            if (gPageNo < 1) gPageNo = gPageMax
                            // テキスト読込
                            fLoadText()
                            // item1を表示する
                            gViewPager2!!.setCurrentItem(1, false)

                            // 次ページ（スクロール終了後にitem:2を表示している状態）
                        } else if (gViewPager2!!.currentItem == 2) {
                            // 次ページにする（画面はitem2が表示されていてitem1は左に隠れている状態）
                            gPageNo = gPageNo + 1
                            if (gPageNo > gPageMax) gPageNo = 1
                            // テキスト読込
                            fLoadText()
                            // item1を表示する
                            gViewPager2!!.setCurrentItem(1, false)

                        } else {
                            // 特に処理なし
                        }
                    }
                }
                // スクロール開始
                ViewPager2.SCROLL_STATE_DRAGGING -> {
                    // ページ変更操作可否を判定しセットする（元々操作不可の時はここに到達しない）
                    fCheckPageChange()
                }
                // スクロール中
                ViewPager2.SCROLL_STATE_SETTLING -> {
                    // 特に処理なし
                }
            }
        }
        // スクロール中
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            // （スクロール中の処理）特に処理なし
        }
        // ページが完全に選択された後
        override fun onPageSelected(position: Int) {
            // （ページが完全に選択された後の処理）特に処理なし
        }
    }

}









