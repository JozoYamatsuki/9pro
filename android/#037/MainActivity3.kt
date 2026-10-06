package com.a001.choimemo3

import android.annotation.SuppressLint
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupWindow

// 行移動のポップアップを表示する（buttonView:行移動ボタン）
fun MainActivity.fOnRowMoveButton(buttonView: View)  {
    // sub_window4.xml をインフレート（実体化）
    @SuppressLint("InflateParams")
    val yPopupWindowView = LayoutInflater.from(this).inflate(R.layout.sub_window4, null)
    // 各アイテムを実態化
    val yRowUpButton = yPopupWindowView.findViewById<Button>(R.id.RowUpButton)
    val yRowDownButton = yPopupWindowView.findViewById<Button>(R.id.RowDownButton)
    val yRowExitButton = yPopupWindowView.findViewById<Button>(R.id.RowExitButton)

    // PopupWindowが表示前でも そのサイズ（高さ）を正確に計算できるようおまじない
    yPopupWindowView.measure(
        View.MeasureSpec.UNSPECIFIED,
        View.MeasureSpec.UNSPECIFIED
    )
    // ポップアップウィンドウの高さ
    val yPopupWindowHeight = yPopupWindowView.measuredHeight
    // ボタンの高さ
    val yButtonHeight = buttonView.height
    // 表示するY座標を計算する
    // 基準座標はボタンの左下で、そこからボタンの高さと、ポップアップウィンドウの高さと、少し隙間を開ける分を引く
    val yYOffset = -yPopupWindowHeight -yButtonHeight -10

    // yPopupWindowViewをポップアップウィンドウ化（gRowMovePopupWindowはMainActivity.ktで宣言）
    gRowMovePopupWindow = PopupWindow(
        yPopupWindowView,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        false // focusable: 外部タップや戻るボタンで閉じるかどうか
    )

    // ポップアップウィンドウ内の「▲」ボタンが押された時の処理
    yRowUpButton.setOnClickListener {
        // １行上に移動する
        fRowMoveUp()
    }

    // ポップアップウィンドウ内の「▼」ボタンが押された時の処理
    yRowDownButton.setOnClickListener {
        // １行下に移動する
        fRowMoveDown()
    }

    // ポップアップウィンドウ内の「閉じる」ボタンが押された時の処理
    yRowExitButton.setOnClickListener {
        // 行移動ポップアップを閉じる
        fRowMovePopupClose()
    }

    // 選択中の行番号をクリア
    fRowSelectNoClear()

    // ポップアップウィンドウを表示する
    // 基準座標「anchor」はページ番号（gPageTextView）の左下
    // showAsDropDownのGravity指定可能なオーバーロードを使う（Gravity.END：右端に揃えて表示）
    gRowMovePopupWindow?.showAsDropDown(buttonView, 0, yYOffset, Gravity.END)
}

// 行移動ポップアップを閉じる
fun MainActivity.fRowMovePopupClose(){
    // 選択行の色を解除する
    fSelectRowRemoveColor()
    // 選択中の行番号をクリア
    fRowSelectNoClear()
    // ポップアップを閉じる
    gRowMovePopupWindow?.dismiss()
}

// 選択行の色を解除する
fun MainActivity.fSelectRowRemoveColor(){
    if (gEditText==null) return
    // 文字装飾全解除（単なるテキストに上書するすることで装飾を無くす）
    gEditText!!.setText(gEditText!!.text.toString())
}

// 選択中の行番号をクリア
fun MainActivity.fRowSelectNoClear(){
    // 選択中の行番号をクリア
    gRowSelectNo1 = 0
    gRowSelectNo2 = 0
}

// 行移動ポップアップ表示中に行選択した時の処理
fun MainActivity.fSelectRowByTouch(){
    // クリックした行番号（１始まり）を取得する
    val yRowNo = fGetLineNoFromCursorPosition()
    // クリックした行を選択行とする
    fSelectRowByNo(yRowNo)
}

// 指定行を選択行とする（xRowNo=0の時は選択行の色表示を再表示するのみ）
fun MainActivity.fSelectRowByNo(xRowNo: Int){
    // xRowNoが指定有りなら（xRowNo=0の時は選択行の色表示を再表示するのみ）
    if (xRowNo > 0) {
        // 選択中の行番号が両方セット済みの時は一旦クリアする
        if (gRowSelectNo1 != gRowSelectNo2) {
            // 選択中の行番号をクリア
            fRowSelectNoClear()
        }
        // クリックした行番号を選択中の行番号にセットする（未セット時は１と２を同じにする、２個目は２のみセット）
        if (gRowSelectNo1 == 0 && gRowSelectNo2 == 0) {
            gRowSelectNo1 = xRowNo
            gRowSelectNo2 = xRowNo
        } else {
            gRowSelectNo2 = xRowNo
        }
        // 行番号が逆転している場合は
        if (gRowSelectNo1 > gRowSelectNo2) {
            // gRowSelectNo1 と gRowSelectNo2 の値を入れ替える
            gRowSelectNo1 = gRowSelectNo2.also { gRowSelectNo2 = gRowSelectNo1 }
        }
    }
    // 選択行（１始まり）の開始カラム位置と終了カラム位置を取得する（２つとも）
    val (yRowStartColumn1,yRowEndColumn1) = fRowToStartEnd(gRowSelectNo1)
    val (yRowStartColumn2,yRowEndColumn2) = fRowToStartEnd(gRowSelectNo2)
    // 選択行に色を付ける
    fSelectRowAddColor(yRowStartColumn1,yRowEndColumn2)
}

// 指定行（１始まり）の開始カラム位置と終了カラム位置を取得
fun MainActivity.fRowToStartEnd(zRowNo: Int): Pair<Int, Int> {
    if (gEditText==null) return Pair(0,0)
    // 指定行の先頭カラム位置
    var yPosition: Int = 0
    // 見つかった改行コード数
    var yCR: Int = 0
    // テキストを先頭から検索する
    for (yCount in 0..<gEditText!!.text.length){
        // 改行コードが指定個数見つかったなら検索終了
        if (yCR >= (zRowNo-1)) break
        // yPosition目の文字が改行コードなら
        if (gEditText!!.text[yCount] =='\n') {
            // 見つかった改行コード数をカウントアップ
            yCR += 1
            // 改行コードの次の文字が、次の行の先頭カラム位置
            yPosition = yCount + 1
        }
    }
    // 安全対策：yCursorPositionがテキスト長を超えていたらテキスト長に矯正する
    yPosition = yPosition.coerceAtMost(gEditText!!.text.length)
    // 行の開始カラム位置を取得
    // 　カーソル位置より前の文字列の中で最後の改行コード位置を取得して、その改行コード分の+1する
    // 　先頭行などで見つからない場合は-1になるので+1により0となる
    val yRowStartColumn: Int = gEditText!!.text.substring(0,yPosition).lastIndexOf('\n') + 1
    // 行の終了カラム位置を取得
    // 　カーソル位置から後ろの改行コード位置を取得する
    // 　最終行などで見つからない場合は-1となるので元のテキストの長さに合わせる
    var yRowEndColumn: Int = gEditText!!.text.indexOf('\n',yPosition)
    if (yRowEndColumn < yRowStartColumn) yRowEndColumn = gEditText!!.text.length
    // 開始カラム位置と終了カラム位置を返す
    return Pair(yRowStartColumn,yRowEndColumn)
}

// 選択行に色を付ける（指定文字位置[開始カラム～終了カラム]のBG色を変更する）
fun MainActivity.fSelectRowAddColor(xStartColumn: Int, xEndColumn: Int){
    if (gEditText==null) return
    // 選択行の色を解除する
    fSelectRowRemoveColor()
    // 安全対策：開始カラムと終了カラムが逆転している場合は無効とする
    if (xStartColumn > xEndColumn) return
    // 装飾付きのテキストの器に元のテキストをコピーする
    val ySpannableString = SpannableString(gEditText!!.text)
    // 装飾を付加する（指定文字位置[開始～終了]のBG色を変更する）
    ySpannableString.setSpan(
        BackgroundColorSpan(getColor(R.color.color_update)),
        xStartColumn,
        xEndColumn,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    // 装飾付きのテキストに置き換える
    gEditText!!.setText(ySpannableString)
}

// 選択中の行を１行上に移動する
fun MainActivity.fRowMoveUp(){
    if (gEditText==null) return
    // 選択中でなければ処理なし
    if (gRowSelectNo1 <= 0) return
    // 先頭行選択中は処理なし
    if (gRowSelectNo1 == 1) return
    // １つ上の行（１始まり）の開始カラム位置と終了カラム位置を取得する
    val (yRowStartColumnN,yRowEndColumnN) = fRowToStartEnd(gRowSelectNo1-1)
    // 選択行（１始まり）の開始カラム位置と終了カラム位置を取得する
    val (yRowStartColumnS1,yRowEndColumnS1) = fRowToStartEnd(gRowSelectNo1)
    val (yRowStartColumnS2,yRowEndColumnS2) = fRowToStartEnd(gRowSelectNo2)
    // １つ上の行より前のテキストを取得する
    var yStringA = ""
    if (yRowStartColumnN > 0){
        yStringA = gEditText!!.text.substring(0,yRowStartColumnN)
    }
    // １つ上の行のテキストを取得する
    val yStringB = gEditText!!.text.substring(yRowStartColumnN,yRowEndColumnN)
    // 選択行のテキストを取得する
    val yStringC = gEditText!!.text.substring(yRowStartColumnS1,yRowEndColumnS2)
    // 選択行より後ろのテキストを取得する
    var yStringD = ""
    if (yRowEndColumnS2 < gEditText!!.text.length){
        yStringD= gEditText!!.text.substring(yRowEndColumnS2)
    }
    // 合体する
    val yText = yStringA + yStringC + '\n' +yStringB + yStringD
    // 表示する
    gEditText!!.setText(yText)
    // 選択行を1行上にする
    gRowSelectNo1 -= 1
    gRowSelectNo2 -= 1
    // 選択行を更新する
    fSelectRowByNo(0)
}

// 選択中の行を１行下に移動する
fun MainActivity.fRowMoveDown(){
    if (gEditText==null) return
    // 選択中でなければ処理なし
    if (gRowSelectNo1 <= 0) return
    // 最終行選択中は処理なし
    if (gRowSelectNo2 >= gEditText!!.lineCount) return
    // 選択行（１始まり）の開始カラム位置と終了カラム位置を取得する
    val (yRowStartColumnS1,yRowEndColumnS1) = fRowToStartEnd(gRowSelectNo1)
    val (yRowStartColumnS2,yRowEndColumnS2) = fRowToStartEnd(gRowSelectNo2)
    // １つ下の行（１始まり）の開始カラム位置と終了カラム位置を取得する
    val (yRowStartColumnN,yRowEndColumnN) = fRowToStartEnd(gRowSelectNo2+1)
    // 選択行より前のテキストを取得する
    var yStringA = ""
    if (yRowStartColumnS1 > 0){
        yStringA = gEditText!!.text.substring(0,yRowStartColumnS1)
    }
    // 選択行のテキストを取得する
    val yStringB = gEditText!!.text.substring(yRowStartColumnS1,yRowEndColumnS2)
    // １つ下の行のテキストを取得する
    val yStringC = gEditText!!.text.substring(yRowStartColumnN,yRowEndColumnN)
    // １つ下の行より後ろのテキストを取得する
    var yStringD = ""
    if (yRowEndColumnN < gEditText!!.text.length){
        yStringD= gEditText!!.text.substring(yRowEndColumnN)
    }
    // 合体する
    val yText = yStringA + yStringC + '\n' +yStringB + yStringD
    // 表示する
    gEditText!!.setText(yText)
    // 選択行を1行下にする
    gRowSelectNo1 += 1
    gRowSelectNo2 += 1
    // 選択行を更新する
    fSelectRowByNo(0)
}






