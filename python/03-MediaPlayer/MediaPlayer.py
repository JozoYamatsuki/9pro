import sys
import os
from PySide6.QtWidgets import (QApplication, QMainWindow, QVBoxLayout, QHBoxLayout,
                               QWidget, QSlider, QPushButton, QLabel, QStyle)
from PySide6.QtMultimedia import QMediaPlayer, QAudioOutput, QMediaMetaData
from PySide6.QtMultimediaWidgets import QVideoWidget
from PySide6.QtCore import Qt, QUrl, QSharedMemory
from PySide6.QtNetwork import QLocalServer, QLocalSocket
from PySide6.QtGui import QGuiApplication, QPixmap

try:
    import win32gui
    import win32con
except ImportError:
    win32gui = None
    win32con = None


class ClickableSlider(QSlider):
    def mousePressEvent(self, event):
        if event.button() == Qt.MouseButton.LeftButton:
            val = QStyle.sliderValueFromPosition(self.minimum(), self.maximum(), event.pos().x(), self.width())
            self.setValue(val)
            self.sliderMoved.emit(val)
        super().mousePressEvent(event)


# 動画エリアでのクリックと、ここへのドロップを処理する
class CustomVideoWidget(QVideoWidget):
    def __init__(self, player_window):
        super().__init__()
        self.player_window = player_window
        self.setAcceptDrops(True)

    def dragEnterEvent(self, event):
        self.player_window.shared_drag_enter(event)

    def dragMoveEvent(self, event):
        self.player_window.shared_drag_enter(event)

    def dropEvent(self, event):
        self.player_window.shared_drop(event)

    def mousePressEvent(self, event):
        if event.button() == Qt.MouseButton.LeftButton:
            self.player_window.toggle_play()
        elif event.button() == Qt.MouseButton.RightButton:
            self.player_window.save_timestamp_to_txt()
        super().mousePressEvent(event)


class AdvancedPlayer(QMainWindow):
    def __init__(self):
        super().__init__()
        self.fps = 30.0
        self.setWindowTitle("MediaPlayer")
        self.setStyleSheet("background-color: #121212; color: white;")

        # メインウィンドウ全体でもドラッグ＆ドロップを受け付ける
        self.setAcceptDrops(True)

        self._setup_player()
        self._setup_ui()
        self.load_initial_geometry()

    def _setup_player(self):
        self.player = QMediaPlayer()
        self.audio_output = QAudioOutput()
        self.player.setAudioOutput(self.audio_output)

        self.player.positionChanged.connect(self.update_ui_state)
        self.player.durationChanged.connect(self.update_ui_state)
        self.player.metaDataChanged.connect(self.fetch_video_metadata)

    def _setup_ui(self):
        self.video_widget = CustomVideoWidget(self)
        self.video_widget.setStyleSheet("background-color: black;")
        self.video_widget.setAspectRatioMode(Qt.AspectRatioMode.KeepAspectRatio)

        self.slider = ClickableSlider(Qt.Orientation.Horizontal)
        self.slider.setFocusPolicy(Qt.FocusPolicy.NoFocus)
        self.slider.setStyleSheet("""
            QSlider::groove:horizontal {
                border: 1px solid #444;
                height: 6px;
                background: #888888;
                border-radius: 3px;
            }
            QSlider::sub-page:horizontal {
                background: #007acc;
                border-radius: 3px;
            }
            QSlider::handle:horizontal {
                background: #ffffff;
                border: 1px solid #ccc;
                width: 14px;
                margin-top: -4px;
                margin-bottom: -4px;
                border-radius: 7px;
            }
        """)
        self.slider.sliderMoved.connect(self.player.setPosition)
        self.slider.sliderPressed.connect(self.player.pause)

        self.play_button = QPushButton("Play")
        self.play_button.setFocusPolicy(Qt.FocusPolicy.NoFocus)
        self.play_button.setFixedWidth(80)
        self.play_button.clicked.connect(self.toggle_play)

        self.time_label = QLabel("00:00.00 / 00:00.00")
        self.time_label.setFixedWidth(160)

        self.edit_button = QPushButton("Edit")
        self.edit_button.setFocusPolicy(Qt.FocusPolicy.NoFocus)
        self.edit_button.setFixedWidth(60)
        self.edit_button.clicked.connect(self.generate_edit_txt)
        self.edit_button.setStyleSheet("background-color: #333; border: 1px solid #555; padding: 3px;")

        controls = QHBoxLayout()
        controls.setContentsMargins(10, 5, 10, 10)
        for w in [self.play_button, self.slider, self.time_label, self.edit_button]:
            controls.addWidget(w)

        layout = QVBoxLayout()
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(0)
        layout.addWidget(self.video_widget, stretch=1)
        layout.addLayout(controls, stretch=0)

        container = QWidget()
        container.setLayout(layout)
        self.setCentralWidget(container)
        self.player.setVideoOutput(self.video_widget)

    # --- 共通のドラッグ＆ドロップ処理（どこに落とされてもここへ集約する） ---
    def dragEnterEvent(self, event):
        self.shared_drag_enter(event)

    def dragMoveEvent(self, event):
        self.shared_drag_enter(event)

    def dropEvent(self, event):
        self.shared_drop(event)

    def shared_drag_enter(self, event):
        if event.mimeData().hasUrls():
            url = event.mimeData().urls()[0].toLocalFile()
            ext = os.path.splitext(url)[1].lower()
            if ext in ['.mp4', '.mkv']:
                event.acceptProposedAction()

    def shared_drop(self, event):
        file_path = event.mimeData().urls()[0].toLocalFile()
        self.play_file(file_path)

    def load_initial_geometry(self):
        try:
            exe_dir = os.path.dirname(sys.executable if getattr(sys, 'frozen', False) else __file__)
            ini_path = os.path.join(exe_dir, "MediaPlayer_ini.txt")
            if os.path.exists(ini_path):
                with open(ini_path, "r") as f:
                    x, y, w, h = map(int, f.readline().split())
                    self.setGeometry(x, y, w, h)
        except Exception:
            self.resize(1200, 800)

    # --- 最前面・アクティブ化ロジックの修正（×ボタンが壊れない安全版） ---
    def play_file(self, file_path):
        if file_path and os.path.exists(file_path):
            base_name = os.path.basename(file_path)
            self.setWindowTitle(base_name)
            self.player.setSource(QUrl.fromLocalFile(file_path))
            self.player.play()
            self.play_button.setText("Pause")

            # ×ボタンを破壊する原因だった self.setWindowFlags(...) は完全に廃止しました

            if self.isMinimized():
                self.setWindowState(self.windowState() & ~Qt.WindowState.WindowMinimized | Qt.WindowState.WindowActive)

            # Windows APIによる確実かつ安全なフォアグラウンド（最前面・アクティブ）処理
            if win32gui and win32con:
                hwnd = self.winId()
                # ウィンドウを表示状態にして強制的に手前に持ってくる
                win32gui.ShowWindow(hwnd, win32con.SW_SHOW)
                win32gui.SetForegroundWindow(hwnd)

            self.activateWindow()
            self.raise_()
            self.setFocus(Qt.FocusReason.ActiveWindowFocusReason)

    def fetch_video_metadata(self):
        fps = self.player.metaData().value(QMediaMetaData.Key.VideoFrameRate)
        if fps: self.fps = float(fps)
        title = self.windowTitle().split(" (")[0]
        self.setWindowTitle(f"{title} ({self.fps}fps)")

    def ms_to_units(self, ms):
        if ms < 0: ms = 0
        total_seconds = ms // 1000
        m, s = divmod(total_seconds, 60)
        f = int(round((ms % 1000) / (1000 / self.fps)))
        if f >= int(round(self.fps)):
            f = 0
            s += 1
            if s >= 60:
                s = 0
                m += 1
        return m, s, f

    def format_ts(self, ms):
        m, s, f = self.ms_to_units(ms)
        return f"{m:02}:{s:02}.{f:02}"

    def ts_to_total_frames(self, ts_str):
        try:
            minutes_str, sec_frame_str = ts_str.strip().split(':')
            seconds_str, frame_str = sec_frame_str.split('.')
            m, s, f = int(minutes_str), int(seconds_str), int(frame_str)
            fps_int = int(round(self.fps))
            total_frames = (m * 60 * fps_int) + (s * fps_int) + f
            return total_frames
        except Exception:
            return 0

    def total_frames_to_ts_str(self, total_frames):
        if total_frames < 0: total_frames = 0
        fps_int = int(round(self.fps))
        total_seconds = total_frames // fps_int
        f = total_frames % fps_int
        m, s = divmod(total_seconds, 60)
        return f"{m:02}:{s:02}.{f:02}"

    def toggle_play(self):
        if self.player.playbackState() == QMediaPlayer.PlaybackState.PlayingState:
            self.player.pause()
            self.play_button.setText("Play")
        else:
            self.player.play()
            self.play_button.setText("Pause")

    def update_ui_state(self):
        if not self.slider.isSliderDown():
            self.slider.setValue(self.player.position())
        self.slider.setRange(0, self.player.duration())
        curr = self.format_ts(self.player.position())
        total = self.format_ts(self.player.duration())
        self.time_label.setText(f"{curr} / {total}")

    def save_timestamp_to_txt(self):
        out_dir = r"C:\file\obs_output"
        os.makedirs(out_dir, exist_ok=True)
        txt_path = os.path.join(out_dir, "time.txt")
        curr_time_str = self.format_ts(self.player.position())
        with open(txt_path, "a", encoding="utf-8") as f:
            f.write(curr_time_str + "\n")
        print(f"Saved timestamp: {curr_time_str}")

    def generate_edit_txt(self):
        out_dir = r"C:\file\obs_output"
        time_path = os.path.join(out_dir, "time.txt")
        edit_path = os.path.join(out_dir, "edit.txt")

        if not os.path.exists(time_path):
            return

        with open(time_path, "r", encoding="utf-8") as f:
            lines = f.readlines()

        # 1. 読み込んだ時間をすべて総フレーム数に変換し、リスト化
        frame_list = []
        for line in lines:
            line = line.strip()
            if not line:
                continue
            base_frames = self.ts_to_total_frames(line)
            frame_list.append(base_frames)

        if not frame_list:
            return

        # 要望1：time.txtのデータをソート（昇順に並び替え）
        frame_list.sort()

        # 2. 各時間ごとに「t1（開始）」と「t2（終了）」のフレーム区間を作る
        fps_int = int(round(self.fps))
        intervals = []
        for base_frames in frame_list:
            t1_f = max(0, base_frames - (3 * fps_int))
            t2_f = base_frames + (1 * fps_int) - 1
            intervals.append([t1_f, t2_f])

        # 要望2：被っている時間区間をマージ（結合）する
        merged_intervals = []
        for current in intervals:
            # 最初のデータ、または前のデータの終了位置よりも今のデータの開始位置が後ろ（被っていない）場合
            if not merged_intervals or merged_intervals[-1][1] < current[0]:
                merged_intervals.append(current)
            else:
                # 時間が被っている場合は、終了位置をより遅い方に更新してマージする
                merged_intervals[-1][1] = max(merged_intervals[-1][1], current[1])

        # 3. マージされたフレーム区間を「分:秒.コマ」の文字列に変換して書き出し
        output_lines = ["no    type  t1        t2      \n"]
        idx = 1
        for t1_frames, t2_frames in merged_intervals:
            t1_str = self.total_frames_to_ts_str(t1_frames)
            t2_str = self.total_frames_to_ts_str(t2_frames)
            output_lines.append(f"{idx:04d}  wide  {t1_str}  {t2_str}\n")
            idx += 1

        with open(edit_path, "w", encoding="utf-8") as f:
            f.writelines(output_lines)
        print("Generated edit.txt with sorted and merged intervals successfully.")

    def keyPressEvent(self, event):
        k, p, f_ms = event.key(), self.player.position(), round(1000 / self.fps)
        if k == Qt.Key.Key_Space:
            self.toggle_play()
        elif k == Qt.Key.Key_Down:
            self.player.setPosition(p - 1000)
        elif k == Qt.Key.Key_Up:
            self.player.setPosition(p + 1000)
        elif k == Qt.Key.Key_Left:
            self.player.pause(); self.player.setPosition(p - f_ms)
        elif k == Qt.Key.Key_Right:
            self.player.pause(); self.player.setPosition(p + f_ms)
        elif k == Qt.Key.Key_Home:
            self.player.setPosition(0)
        elif k == Qt.Key.Key_End:
            self.player.setPosition(self.player.duration())
        elif k == Qt.Key.Key_P:
            # video_widget から直接現在の QVideoFrame を取得
            video_frame = self.video_widget.videoSink().videoFrame()
            if video_frame.isValid():
                # オリジナル解像度の QImage に変換
                image = video_frame.toImage()
                pixmap = QPixmap.fromImage(image)
                # クリップボードへセット
                QGuiApplication.clipboard().setPixmap(pixmap)
        super().keyPressEvent(event)


class SingleInstanceApp(QApplication):
    def __init__(self, argv):
        super().__init__(argv)
        self.server_name = "UniqueMediaPlayerSrv"
        self.shared_mem = QSharedMemory(self.server_name)
        self.window = None

        if self.shared_mem.attach():
            self._send_path_to_main()
            sys.exit(0)

        self.shared_mem.create(1)
        self.server = QLocalServer(self)
        self.server.newConnection.connect(self._handle_new_conn)
        self.server.listen(self.server_name)

    def _send_path_to_main(self):
        socket = QLocalSocket()
        socket.connectToServer(self.server_name)
        if socket.waitForConnected(500) and len(sys.argv) > 1:
            socket.write(sys.argv[1].encode('utf-8'))
            socket.waitForBytesWritten(500)

    def _handle_new_conn(self):
        conn = self.server.nextPendingConnection()
        if conn.waitForReadyRead(500):
            path = conn.readAll().data().decode('utf-8')
            if self.window: self.window.play_file(path)
        conn.disconnectFromServer()


if __name__ == "__main__":
    app = SingleInstanceApp(sys.argv)
    player = AdvancedPlayer()
    app.window = player
    player.play_file(sys.argv[1] if len(sys.argv) > 1 else "")
    player.show()
    sys.exit(app.exec())