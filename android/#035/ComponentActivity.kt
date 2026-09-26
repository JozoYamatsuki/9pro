package com.a001.choimemo3

import android.content.Intent
import androidx.activity.ComponentActivity

// Activity再起動
fun ComponentActivity.fActivityRestart(){
    // startActivity()に渡す情報
    val yIntent = Intent(this, MainActivity::class.java).apply {
        // 状態をクリアして、まっさらな状態で新規作成
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    // 新たなActivityを起動する
    startActivity(yIntent)
    // 新しいActivityを起動した直後に自身を閉じる
    finish()
}

// Floatのpx値をsp値に単位変換する
fun ComponentActivity.fFloatPxToFloatSp(px: Float): Float{
    // pxを画面密度で割るとspになる
    return px / resources.displayMetrics.density
}
