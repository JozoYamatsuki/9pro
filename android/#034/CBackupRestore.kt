package com.a001.choimemo3

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


// zipファイル名（内部保存用のファイル名）
private const val ARCHIVE_ZIP_FILE_NAME = "archive.zip"

// バックアップするファイル名パターンを"*.txt"とする（"^.*\\.txt$"は"*.txt"の正規表現）
private const val FILE_NAME_PATTERN = "^.*\\.txt$"


// バックアップ／リストアのクラス宣言（引数：activity=親クラス）
class CBackupRestore(private val activity: ComponentActivity) {

    // バックアップ用のLauncher（ファイル保存ダイアログ表示～保存）
    private val gZipSaveLauncher: ActivityResultLauncher<String> =
        // ファイル保存ダイアログ表示
        activity.registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            // ファイルが指定されたら
            if (uri != null) {
                // 梱包
                fZip()
                // 書き込み
                fZipFileWriteToUri(uri)
                // メッセージを表示
                fMessageOut("バックアップしました")
            }
        }

    // リストア用のLauncher（ファイル選択ダイアログ表示～展開、再起動）
    private val gZipOpenLauncher: ActivityResultLauncher<Array<String>> =
        // ファイル選択ダイアログ表示
        activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            // ファイルが選択されたら
            if (uri != null) {
                // 読み込み
                fZipFileReadUri(uri)
                // 展開
                fUnzip()
                // メッセージを表示
                fMessageOut("復元しました")
                // Activity再起動
                fActivityRestart()
            }
        }

    // --- MainActivityから呼び出す関数 ---

    // バックアップ処理
    fun fBackupFiles() {
        // zipファイル名に使用する年月日のフォーマットをyyyyMMddにする
        val yFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        // 本日の年月日を取得する
        val yFormattedToday = LocalDateTime.now().format(yFormatter)
        // ファイル名を生成（ちょいメモ_yyyyMMdd.zip）
        val yFileName: String = "ちょいメモ_${yFormattedToday}.zip"
        // ファイル保存ダイアログ表示～保存
        gZipSaveLauncher.launch(yFileName)
    }

    // リストア処理
    fun fRestoreFiles() {
        // ファイル選択ダイアログ表示～展開、再起動
        gZipOpenLauncher.launch(arrayOf("application/zip"))
    }

    // --- CBackupRestoreの内部で使う関数 ---

    // zip
    private fun fZip(xZipFileName:String = ARCHIVE_ZIP_FILE_NAME) {
        // 検索パターンに一致するファイルの一覧を取得する
        val yTxtFiles = activity.filesDir.listFiles { file ->
            file.isFile && file.name.matches(Regex(FILE_NAME_PATTERN))
        }
        // ファイルが見つからなければ
        if (yTxtFiles.isNullOrEmpty()) {
            // エラーメッセージを表示する
            fMessageOut("fZip 現在のフォルダに .txt ファイルが見つかりませんでした", true)
            return
        }
        // zipファイル名を生成
        val yZipFile = File(activity.filesDir, xZipFileName)
        //
        try {
            // 梱包
            ZipOutputStream(FileOutputStream(yZipFile)).use { zipOut ->
                // ファイル一覧
                for (inputFile in yTxtFiles) {
                    // ファイルが存在することを念のため確認しておく
                    if (inputFile.exists() && inputFile.isFile) {
                        // 梱包用データにファイルをセット
                        val yEntry = ZipEntry(inputFile.name)
                        // 梱包用データにファイルの最終更新日時を設定
                        yEntry.time = inputFile.lastModified()
                        // zip内に梱包エリアを確保
                        zipOut.putNextEntry(yEntry)
                        // zip内の梱包エリアにファイルをコピー
                        inputFile.inputStream().use { input -> input.copyTo(zipOut) }
                        // zip内の梱包エリアをclose
                        zipOut.closeEntry()
                    } else {
                        // エラーメッセージを表示する
                        fMessageOut("fZip ${inputFile.name} が見つかりません", true)
                        break
                    }
                }
            }
        } catch (e: Exception) {
            // エラーメッセージを表示する
            fMessageOut("fZip エラーが発生しました: ${e.message}", true)
        }
    }

    // unzip
    private fun fUnzip(xZipFileName:String = ARCHIVE_ZIP_FILE_NAME):Boolean {
        // 結果
        var xRts = true
        // zipファイル
        val yZipFile = File(activity.filesDir, xZipFileName)
        // zipファイルが存在するなら
        if (yZipFile.exists() == true) {
            // 展開
            ZipInputStream(FileInputStream(yZipFile)).use { zipIn ->
                // zipから先頭の梱包データを取得
                var yEntry: ZipEntry? = zipIn.nextEntry
                // 梱包されているデータ全てを処理
                while (yEntry != null) {
                    // ファイル名を取得
                    val yEntryName = yEntry.name
                    // 展開後のファイルパスを設定
                    val yOutputFile = File(activity.filesDir, yEntryName)
                    // ファイル名のパターンに一致しているか確認する
                    if (yOutputFile.name.matches(Regex(FILE_NAME_PATTERN)) == true) {
                        //
                        try {
                            FileOutputStream(yOutputFile).use { outputStream ->
                                // 読込処理用の一時バッファを用意
                                val yTempBuffer = ByteArray(4096)
                                // 読込処理用のサイズ用のワークエリア
                                var yLength: Int
                                // 読み込む（ファイルの大きさが4096を超えていたら4096単位で繰り返し読んで合体させる）
                                while (zipIn.read(yTempBuffer).also { yLength = it } != -1) {
                                    outputStream.write(yTempBuffer, 0, yLength)
                                }
                                // ZipEntryから更新日時を取得してファイルの最終更新日時を設定
                                val lastModifiedTime = yEntry.time
                                // 取得できなかったら
                                if (lastModifiedTime != -1L) {
                                    // 展開したファイルの最終更新日時を設定
                                    yOutputFile.setLastModified(lastModifiedTime)
                                }
                            }
                        } catch (e: IOException) {
                            // 展開失敗あり
                            xRts = false
                            // エラーメッセージを表示する
                            fMessageOut("fUnzip ファイルの復元中にエラーが発生しました: $yEntryName: ${e.message}", true)
                            break
                        }
                    }
                    // 処理した梱包データをclose
                    zipIn.closeEntry()
                    // 次の梱包データ
                    yEntry = zipIn.nextEntry
                }
            }
        } else {
            // 展開失敗
            xRts = false
            // エラーメッセージを表示する
            fMessageOut("fUnzip zipファイルが存在しません", true)
        }
        return xRts
    }

    // 指定zipファイルを保存する
    private fun fZipFileWriteToUri(uri: Uri) {
        // 内部zipファイル（内部に保存されているzipファイル）
        val yInternalZipFile = File(activity.filesDir, ARCHIVE_ZIP_FILE_NAME)
        //
        try {
            // 内部zipファイル(InputStream)を
            FileInputStream(yInternalZipFile).use { internalFile ->
                // 指定zipファイル(URI)へ
                activity.contentResolver.openOutputStream(uri)?.use { externalFile ->
                    // コピーする
                    internalFile.copyTo(externalFile)
                }
            }
        } catch (e: Exception) {
            // エラーメッセージを表示する
            fMessageOut("fZipFileWriteToUri エラーが発生しました: ${e.message}", true)
        }
    }

    // 指定zipファイルを読込む
    private fun fZipFileReadUri(uri: Uri) {
        // 内部zipファイル（内部に保存されているzipファイル）
        val yInternalZipFile = File(activity.filesDir, ARCHIVE_ZIP_FILE_NAME)
        //
        try {
            // 指定zipファイル(URI)を
            activity.contentResolver.openInputStream(uri)?.use { externalFile ->
                // 内部zipファイル(OutputStream)に
                FileOutputStream(yInternalZipFile).use { internalFile ->
                    // コピーする
                    externalFile.copyTo(internalFile)
                }
            }
        } catch (e: IOException) {
            // エラーメッセージを表示する
            fMessageOut("fZipFileReadUri エラーが発生しました: ${e.message}", true)
        }
    }

    // Activity再起動
    fun fActivityRestart(){
        // startActivity()に渡す情報
        val yIntent = Intent(activity, MainActivity::class.java).apply {
            // 状態をクリアして、まっさらな状態で新規作成
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        // 新たなActivityを起動する
        activity.startActivity(yIntent)
        // 新しいActivityを起動した直後に自身を閉じる
        activity.finish()
    }

    // メッセージ表示
    private fun fMessageOut(xMsg: String, xLongFlag: Boolean = false) {
        // 表示時間をSHORTに設定する
        var yDuration: Int = android.widget.Toast.LENGTH_SHORT
        // 指定があれば、表示時間をLONGに設定する
        if (xLongFlag == true) yDuration = android.widget.Toast.LENGTH_LONG
        // ActivityのContextを使ってToastで表示する
        android.widget.Toast.makeText(activity, xMsg, yDuration).show()
    }
}