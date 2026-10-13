import sys
import os
import subprocess

# ─── 【重要】PyInstaller --noconsole 時の遅延対策パッチ ───
# pydubなどのライブラリが内部で呼ぶ subprocess のウィンドウ生成を完全に抑制します
if os.name == "nt":  # Windows環境のみ適用
    orig_popen = subprocess.Popen

    def patched_popen(*args, **kwargs):
        # startupinfo を作成または取得
        startupinfo = kwargs.get("startupinfo") or subprocess.STARTUPINFO()
        # 画面を表示させないフラグを強制追加
        startupinfo.dwFlags |= subprocess.STARTF_USESHOWWINDOW
        startupinfo.wShowWindow = subprocess.SW_HIDE
        kwargs["startupinfo"] = startupinfo

        # `--noconsole` 時に入出力が詰まるのを防ぐため、標準入出力をDEVNULLに（指定がない場合）
        if "stdout" not in kwargs:
            kwargs["stdout"] = subprocess.DEVNULL
        if "stderr" not in kwargs:
            kwargs["stderr"] = subprocess.DEVNULL

        return orig_popen(*args, **kwargs)

    subprocess.Popen = patched_popen
# ───────────────────────────────────────────────────────

import tkinter as tk
from tkinter import filedialog, messagebox
from tkinterdnd2 import DND_FILES, TkinterDnD
from pydub import AudioSegment, silence
import datetime

class SrtGeneratorApp:
    def __init__(self, root):
        self.root = root
        self.root.title("AudioSplitSRT (分割 & SRT作成)")
        self.root.geometry("550x380")

        self.file_path = tk.StringVar()
        self.ms_threshold = tk.StringVar(value="380")
        self.start_num = tk.StringVar(value="1")

        # UIレイアウト
        tk.Label(root, text="音声/動画ファイル (m4a, mkv, wav) をドロップ").pack(pady=10)

        self.entry_file = tk.Entry(root, textvariable=self.file_path, width=60)
        self.entry_file.pack(pady=5, padx=20)
        self.entry_file.drop_target_register(DND_FILES)
        self.entry_file.dnd_bind('<<Drop>>', self.handle_drop)

        btn_browse = tk.Button(root, text="ファイル参照", command=self.browse_file)
        btn_browse.pack(pady=5)

        tk.Label(root, text="無音判定の間隔 (ms)").pack(pady=10)
        self.entry_ms = tk.Entry(root, textvariable=self.ms_threshold, width=15, justify="center")
        self.entry_ms.pack()

        # メイン操作エリア（ボタンと初期番号をまとめる親フレーム）
        main_op_frame = tk.Frame(root)
        main_op_frame.pack(pady=20)

        # --- 左側：分割関連のブロック ---
        split_block = tk.Frame(main_op_frame)
        split_block.pack(side=tk.LEFT, padx=10, anchor=tk.N)

        self.btn_split = tk.Button(split_block, text="分割", command=self.split_audio,
                                   bg="#f44336", fg="white", width=15, height=2)
        self.btn_split.pack(pady=(0, 10))  # ボタンの下に少し隙間をあける

        # 初期番号のラベルと入力欄を分割ボタンの真下にセンタリング
        tk.Label(split_block, text="出力ファイル初期番号").pack(anchor=tk.CENTER)
        self.entry_num = tk.Entry(split_block, textvariable=self.start_num, width=10, justify="center")
        self.entry_num.pack(anchor=tk.CENTER, pady=2)

        # --- 右側：SRT作成関連のブロック ---
        srt_block = tk.Frame(main_op_frame)
        srt_block.pack(side=tk.LEFT, padx=10, anchor=tk.N)

        self.btn_run = tk.Button(srt_block, text="SRT作成", command=self.process_srt,
                                 bg="#4CAF50", fg="white", width=15, height=2)
        self.btn_run.pack()

    def handle_drop(self, event):
        file_path = event.data.strip('{}')
        self.file_path.set(file_path)

    def browse_file(self):
        filename = filedialog.askopenfilename(filetypes=[("Audio/Video files", "*.m4a *.mkv *.wav")])
        if filename:
            self.file_path.set(filename)

    def format_time(self, ms):
        td = datetime.timedelta(milliseconds=ms)
        total_seconds = int(td.total_seconds())
        hours = total_seconds // 3600
        minutes = (total_seconds % 3600) // 60
        seconds = total_seconds % 60
        millis = int(ms % 1000)
        return f"{hours:02}:{minutes:02}:{seconds:02},{millis:03}"

    def get_audio_chunks(self):
        """共通の解析ロジック"""
        input_path = self.file_path.get()
        ms_val = self.ms_threshold.get()

        if not input_path or not os.path.exists(input_path):
            messagebox.showerror("Error", "ファイルが見つかりません。")
            return None, None

        try:
            min_silence_len = int(ms_val)
            audio = AudioSegment.from_file(input_path)
            chunks = silence.detect_nonsilent(audio, min_silence_len=min_silence_len, silence_thresh=-44)
            return audio, chunks
        except Exception as e:
            messagebox.showerror("Error", f"解析エラー: {str(e)}")
            return None, None

    def process_srt(self):
        audio, chunks = self.get_audio_chunks()
        if chunks is None: return

        output_path = "C:\\file\\START\\9pro-休プロ\\台本\\subtitle.srt"
        with open(output_path, "w", encoding="utf-8") as f:
            for i, (start, end) in enumerate(chunks, start=1):
                adjusted_start = start - 134
                if adjusted_start < 0:
                    adjusted_start = 0
                adjusted_end = end + 200
                f.write(f"{i}\n{self.format_time(adjusted_start)} --> {self.format_time(adjusted_end)}\n{i}\n\n")

        messagebox.showinfo("Success", f"SRT作成完了！\n{i}\n{os.path.abspath(output_path)}")

    def split_audio(self):
        """分割機能: 各チャンクを wav で保存"""
        audio, chunks = self.get_audio_chunks()
        if chunks is None: return

        try:
            current_num = int(self.start_num.get())
            if current_num < 0:
                raise ValueError
        except ValueError:
            messagebox.showerror("Error", "初期番号には0以上の整数を入力してください。")
            return

        out_dir = "c:\\file\\obs_output\\split_audio"
        if not os.path.exists(out_dir):
            os.makedirs(out_dir)

        try:
            for i, (start, end) in enumerate(chunks):
                adjusted_start = start - 134
                if adjusted_start < 0:
                    adjusted_start = 0
                adjusted_end = end + 200
                chunk_audio = audio[adjusted_start:adjusted_end]

                file_name = f"audio_{current_num:04d}.wav"
                save_path = os.path.join(out_dir, file_name)

                chunk_audio.export(save_path, format="wav")

                current_num += 1

            messagebox.showinfo("Success", f"分割完了！\n最終番号: {current_num-1}\nフォルダ: {os.path.abspath(out_dir)}")
        except Exception as e:
            messagebox.showerror("Error", f"分割処理エラー: {str(e)}")


if __name__ == "__main__":
    root = TkinterDnD.Tk()
    app = SrtGeneratorApp(root)
    root.mainloop()