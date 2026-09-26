package com.a001.choimemo3

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.Toast

// フォント設定のポップアップを表示する（buttonView:設定ボタン）
fun MainActivity.fOnFontSetButton(buttonView: View)  {
    // sub_window3.xml をインフレート（実体化）
    @SuppressLint("InflateParams")
    val yPopupWindowView = LayoutInflater.from(this).inflate(R.layout.sub_window3, null)
    // 各アイテムを実態化
    val yFontApplyButton = yPopupWindowView.findViewById<Button>(R.id.FontApplyButton)
    val yFontCancelButton = yPopupWindowView.findViewById<Button>(R.id.FontCancelButton)
    val yFontSizeEditText = yPopupWindowView.findViewById<EditText>(R.id.FontSizeEditText)

    // 現在のgEditTextのフォントサイズを取得してpx単位系からsp単位系に変換する
    val yEditTextSizeSp = fFloatPxToFloatSp(gEditText?.textSize ?: 0f)
    // フォントサイズを表示する
    yFontSizeEditText.setText(yEditTextSizeSp.toInt().toString())

    // yPopupWindowViewをポップアップウィンドウ化
    val yPopupWindow = PopupWindow(
        yPopupWindowView,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        true // focusable: 外部タップや戻るボタンで閉じるかどうか
    )

    // ポップアップウィンドウ内の「適用」ボタンが押された時の処理
    yFontApplyButton.setOnClickListener {
        // 表示されている数値を取得する
        val yTextSize: Int = yFontSizeEditText.text.toString().toInt()
        // 設定範囲なら
        if (yTextSize >= 10 && yTextSize <= 60) {
            // 初期設定ファイルを更新する
            fIniFileWrite(yTextSize)
            // 再起動する
            fActivityRestart()
        }else{
            // メッセージ
            Toast.makeText(this, "設定範囲：10～60", Toast.LENGTH_SHORT).show()
        }
    }

    // ポップアップウィンドウ内の「閉じる」ボタンが押された時の処理
    yFontCancelButton.setOnClickListener {
        // ポップアップを閉じる
        yPopupWindow.dismiss()
    }

    // ポップアップウィンドウを表示する
    // 基準座標「anchor」はページ番号（gPageTextView）の左下
    // showAsDropDownのGravity指定可能なオーバーロードを使う（Gravity.END：右端に揃えて表示）
    yPopupWindow.showAsDropDown(gPageTextView, 0, 10, Gravity.END)
}

// 初期設定ファイルを読み込んで適用する（起動時のgViewPager2.doOnLayoutで呼び出す）
fun MainActivity.fIniFileRead(){
    // フォントサイズ変数宣言（設定ファイルが無いなどでの異常時のデフォルト値を30fとする）
    var yTextFontSize: Float = 30f
    // 初期設定ファイルmemo_ini.txtを読み込む（フォントサイズを取得する）
    val yText: String? = fTextFileRead("memo_ini.txt")
    // ファイルが存在したなら
    if (yText != null) {
        // ファイルの中身が壊れている（数値で無い）場合に備えて念のためtryで囲む
        try {
            // フォントサイズにセットする
            yTextFontSize = yText.toFloat()
        }catch (e: Exception){
        }
    }
    // 各editTextとTextViewのフォントサイズを設定する
    gEditText?.textSize = yTextFontSize
    gPageTitleEditText?.textSize = yTextFontSize
    gPageLeftTextView?.textSize = yTextFontSize
    gPageLeftTitleTextView?.textSize = yTextFontSize
    gPageRightTextView?.textSize = yTextFontSize
    gPageRightTitleTextView?.textSize = yTextFontSize
}

// 初期設定ファイルを更新する
fun MainActivity.fIniFileWrite(xTextSize: Int){
    // 初期設定ファイルmemo_ini.txtを更新する
    fTextFileWrite("memo_ini.txt",xTextSize.toString())
}
