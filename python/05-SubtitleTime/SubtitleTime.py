import os
import re
import glob
import tkinter as tk
from tkinter import messagebox, ttk
from tkinterdnd2 import DND_FILES, TkinterDnD
from moviepy import VideoFileClip
import librosa


class SubtitleCheckerApp:
    def __init__(self, root):
        self.root = root
        self.root.title("SubtitleTime音声と字幕を紐付けて台本の再生時間を計算するツール")
        self.root.geometry("600x320")

        self.folder_path = ""
        self.txt_path = ""

        # UIの構築
        self.create_widgets()

    def create_widgets(self):
        # メインフレーム
        main_frame = ttk.Frame(self.root, padding="20")
        main_frame.pack(fill=tk.BOTH, expand=True)

        # 1. 音声フォルダのドロップエリア
        ttk.Label(main_frame, text="1. 音声ファイルのあるフォルダを指定").pack(anchor=tk.W, pady=(0, 2))
        self.folder_label = tk.Label(
            main_frame, text="ここに音声フォルダ（split_audio_）をドロップしてください",
            bg="#e1e1e1", relief="sunken", height=3, bd=2
        )
        self.folder_label.pack(fill=tk.X, pady=(0, 15))

        # フォルダ用のドロップ設定
        self.folder_label.drop_target_register(DND_FILES)
        self.folder_label.dnd_bind('<<Drop>>', self.drop_folder)

        # 2. 字幕テキストファイルのドロップエリア
        ttk.Label(main_frame, text="2. 字幕用テキストファイル(台本.txt)を指定").pack(anchor=tk.W, pady=(0, 2))
        self.txt_label = tk.Label(
            main_frame, text="ここに字幕用テキストファイル（台本.txt）をドロップしてください",
            bg="#e1e1e1", relief="sunken", height=3, bd=2
        )
        self.txt_label.pack(fill=tk.X, pady=(0, 20))

        # テキストファイル用のドロップ設定
        self.txt_label.drop_target_register(DND_FILES)
        self.txt_label.dnd_bind('<<Drop>>', self.drop_txt)

        # 実行ボタン
        self.run_btn = ttk.Button(main_frame, text="実行（#audio.txtを作成）", command=self.process_files, state=tk.DISABLED)
        self.run_btn.pack(ipadx=20, ipady=5)

    def clean_path(self, path):
        return path.strip('{}"\' ')

    def drop_folder(self, event):
        path = self.clean_path(event.data)
        if os.path.isdir(path):
            self.folder_path = path
            self.folder_label.config(text=path, bg="#d1e7dd")
            self.check_ready()
        else:
            messagebox.showerror("エラー", "フォルダを指定してください。")

    def drop_txt(self, event):
        path = self.clean_path(event.data)
        if os.path.isfile(path) and path.lower().endswith('.txt'):
            self.txt_path = path
            self.txt_label.config(text=path, bg="#d1e7dd")
            self.check_ready()
        else:
            messagebox.showerror("エラー", "テキストファイル(.txt)を指定してください。")

    def check_ready(self):
        if self.folder_path and self.txt_path:
            self.run_btn.config(state=tk.NORMAL)

    def get_audio_duration(self, file_path):
        try:
            return librosa.get_duration(path=file_path)
        except Exception:
            return 0.0

    def get_video_duration(self, file_path):
        try:
            with VideoFileClip(file_path) as clip:
                return clip.duration
        except Exception as e:
            print(f"WMV読み込みエラー ({file_path}): {e}")
            return 0.0

    def load_text_file(self, file_path):
        """UTF-8 または Shift-JIS でテキストファイルを読み込む"""
        encodings = ['utf-8', 'cp932']  # cp932はWindowsのShift-JIS拡張
        for enc in encodings:
            try:
                with open(file_path, 'r', encoding=enc) as f:
                    lines = f.readlines()
                return lines
            except (UnicodeDecodeError, UnicodeError):
                continue
        raise ValueError("ファイルの文字コード（UTF-8 / Shift-JIS）を判定できませんでした。")

    def process_files(self):
        # 1. 字幕テキストの読み込み（文字コード自動判別・空行無視）
        subtitles = []
        try:
            lines = self.load_text_file(self.txt_path)
            for line in lines:
                line_str = line.strip()
                if line_str:
                    subtitles.append(line_str)
        except Exception as e:
            messagebox.showerror("エラー", f"テキストファイルの読み込みに失敗しました:\n{e}")
            return

        # 2. 音声ファイルの解析とグループ化
        wav_files = glob.glob(os.path.join(self.folder_path, "audio_*.wav"))
        wmv_files = glob.glob(os.path.join(self.folder_path, "audio_*_blank.wmv"))

        all_files = wav_files + wmv_files
        groups = {}
        for fpath in all_files:
            fname = os.path.basename(fpath)
            match = re.match(r"audio_(\d{4})", fname)
            if match:
                num = int(match.group(1))
                if num not in groups:
                    groups[num] = []
                groups[num].append(fpath)

        sorted_nums = sorted(groups.keys())

        audio_blocks_durations = []
        current_block_duration = 0.0
        has_audio_in_block = False
        start_num = []

        for num in sorted_nums:
            filesInNum = groups[num]
            wavs = [f for f in filesInNum if f.endswith('.wav')]
            wmvs = [f for f in filesInNum if f.endswith('_blank.wmv')]

            for wav in wavs:
                current_duration = 0.0
                current_duration = self.get_audio_duration(wav)
                # フレーム誤差調整（30fpsのフレーム単位に合わせる）
                #current_block_duration += int((current_duration + 1 / 60) * 30) / 30
                current_block_duration += int((current_duration + 0.0162) * 30) / 30
                if not has_audio_in_block:
                    start_num.append(num)
                has_audio_in_block = True

            if wmvs:
                #current_block_duration += self.get_audio_duration(wmvs[0])
                current_block_duration += self.get_video_duration(wmvs[0])
                # 誤差調整（誤差を加味）
                #current_block_duration += 0.097
                if has_audio_in_block:
                    audio_blocks_durations.append(current_block_duration)
                current_block_duration = 0.0
                has_audio_in_block = False

        if has_audio_in_block and current_block_duration > 0:
            audio_blocks_durations.append(current_block_duration)

        # 3. マッチング処理
        audio_count = len(audio_blocks_durations)
        text_count = len(subtitles)
        match_count = min(audio_count, text_count)

        # 実際に出力される行の「時間の総合計」を計算
        total_time = sum(audio_blocks_durations[:match_count])

        # 分・秒への換算（ダイアログ表示用）
        total_minutes = int(total_time // 60)
        total_seconds = total_time % 60

        # アウトプットファイルの作成
        output_path = os.path.join(self.folder_path, "#audio.txt")
        try:
            with open(output_path, 'w', encoding='utf-8') as out_f:
                out_f.write("no\ta-no\ttime\tsubtitle\n")
                for i in range(match_count):
                    no_str = f"{i + 1:03d}"
                    start_num_str = f"({start_num[i]:04d})"
                    time_str = f"{audio_blocks_durations[i]:.3f}"
                    subtitle_str = subtitles[i]
                    out_f.write(f"{no_str}{start_num_str}\t{time_str}\t{subtitle_str}\n")
        except Exception as e:
            messagebox.showerror("エラー", f"出力ファイルの書き込みに失敗しました:\n{e}")
            return

        # 時間の表示フォーマットを作成 (例: 1分23.456秒 / 45.678秒)
        if total_minutes > 0:
            time_summary = f"{total_minutes}分{total_seconds:.3f}秒 ({total_time:.3f}秒)"
        else:
            time_summary = f"{total_time:.3f}秒"

        # 4. 完了通知と不一致ダイアログ（総合計時間を追加）
        if audio_count != text_count:
            messagebox.showwarning(
                "件数不一致通知",
                f"音声の区切り数（{audio_count}件）と字幕の行数（{text_count}件）が一致しませんでした。\n"
                f"短い方の件数（{match_count}件）に合わせて「#audio.txt」を出力しました。\n\n"
                f"【出力データの総合計時間】\n{time_summary}"
            )
        else:
            messagebox.showinfo(
                "完了",
                f"無事に一致しました！\n"
                f"{match_count}件のデータを「#audio.txt」に出力しました。\n\n"
                f"【総合計時間】\n{time_summary}"
            )


if __name__ == "__main__":
    root = TkinterDnD.Tk()
    app = SubtitleCheckerApp(root)
    root.mainloop()