import os
import re
import struct
import subprocess
import tkinter as tk
from tkinter import messagebox, filedialog

from librosa import cite
from tkinterdnd2 import DND_FILES, TkinterDnD


def get_mp4_duration(file_path):
    """
    ffprobe(またはffmpeg)を使って動画の長さを高精度に取得する。
    追加ライブラリは不要で、Windows環境に導入されたffmpegを利用します。
    """
    if not os.path.exists(file_path):
        return 0.0

    # まずは軽量な ffprobe を試す
    cmd_ffprobe = [
        "ffprobe",
        "-v", "error",
        "-show_entries", "format=duration",
        "-of", "default=noprint_wrappers=1:nokey=1",
        file_path
    ]

    # ffprobeが無い場合のフォールバックとして ffmpeg を試す
    cmd_ffmpeg = [
        "ffmpeg",
        "-i", file_path
    ]

    # Windowsの黒いコンソール画面がポップアップするのを防ぐ設定
    startupinfo = None
    if os.name == 'nt':
        startupinfo = subprocess.STARTUPINFO()
        startupinfo.dwFlags |= subprocess.STARTF_USESHOWWINDOW

    try:
        # 1. ffprobe による取得を試みる
        res = subprocess.run(
            cmd_ffprobe,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            startupinfo=startupinfo,
            text=True,
            timeout=5
        )
        if res.returncode == 0 and res.stdout.strip():
            return float(res.stdout.strip())
    except Exception:
        pass

    try:
        # 2. ffprobeが動かない・無い場合は、ffmpegの情報出力(stderr)からDurationを取り出す
        res = subprocess.run(
            cmd_ffmpeg,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            startupinfo=startupinfo,
            text=True,
            timeout=5
        )
        # ffmpeg -i は正常終了(returncode 0)しないので stderr から直接解析
        output = res.stderr
        # "Duration: 00:01:23.45," のようなパターンを探す
        match = re.search(r"Duration:\s+(\d+):(\d+):([\d\.]+)", output)
        if match:
            hours = int(match.group(1))
            minutes = int(match.group(2))
            seconds = float(match.group(3))
            return hours * 3600 + minutes * 60 + seconds
    except Exception:
        pass

    return 0.0


def create_space_wmv(duration, output_path):
    """
    ffmpegを使って、指定された秒数の真っ黒なwmv動画ファイルを作成する（上書き）
    """
    # 差分の絶対値
    duration_abs = abs(duration)
    # ffmpegコマンドの構築（静止画の黒画面から指定長さの動画を出力）
    # wmv8(wmv2)またはwmv9(msmpeg4v3)のフォーマットで書き出します
    cmd = [
        "ffmpeg", "-y",  # 自動上書き
        "-f", "lavfi",
        "-i", f"color=c=black:s=1280x720:d={duration_abs}:r=30",  # 解像度1280x720, 黒背景, フレームレート30
        "-c:v", "wmv2",
        "-b:v", "1000k",
        output_path
    ]

    try:
        # ffmpegを非表示(バックグラウンド)で実行
        startupinfo = None
        if os.name == 'nt':
            startupinfo = subprocess.STARTUPINFO()
            startupinfo.dwFlags |= subprocess.STARTF_USESHOWWINDOW

        subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, startupinfo=startupinfo, check=True)
    except Exception as e:
        raise RuntimeError(
            f"ffmpegによる動画作成に失敗しました。ffmpeg.exeが正しくインストールされているか確認してください。\n詳細: {e}")


def process_audio_file(audio_path, video_dir):
    """
    #audio.txt を解析してグループ情報を抽出し、#group.txt を書き出すメインロジック
    """
    # タブ区切り、または連続する空白に対応するための正規表現
    line_pattern = re.compile(r"^\d{3}\((\d{4})\)\s+([\d\.]+)\s+(.*)$")
    video_pattern = re.compile(r"@(\d+)@")

    try:
        with open(audio_path, "r", encoding="utf-8") as f:
            lines = f.readlines()

        raw_groups = []
        group_counter = 1
        current_group = None

        # 1. `#audio.txt` のパース
        for line in lines:
            line = line.strip()
            if not line or line.startswith("no\ta-no") or (line.startswith("001") and "subtitle" in line):
                continue

            match = line_pattern.match(line)
            if match:
                audio_no_str = match.group(1)  # カッコ内の4桁 (例: 0001)
                time_val = float(match.group(2))  # time列の数値
                subtitle = match.group(3)  # サブタイトル部分

                # サブタイトル内に @数字@ があるか判定（新しいグループの開始）
                video_match = video_pattern.search(subtitle)
                if video_match:
                    if current_group is not None:
                        raw_groups.append(current_group)

                    video_num = int(video_match.group(1))
                    audio_num = int(audio_no_str)

                    current_group = {
                        "group": group_counter,
                        "douga": video_num,
                        "audio": audio_num,
                        "audio_time_sum": time_val  # 音声合計時間の初期化
                    }
                    group_counter += 1
                else:
                    if current_group is not None:
                        current_group["audio_time_sum"] += time_val

        if current_group is not None:
            raw_groups.append(current_group)

        # 型の安全性を完全に担保するため、正常な辞書データのみを抽出
        groups = [g for g in raw_groups if isinstance(g, dict) and "douga" in g and "audio" in g]

        if not groups:
            raise ValueError("グループ情報が見つかりませんでした。#audio.txt のフォーマットを確認してください。")

        # 2. 各グループの動画時間（douga_time）の計算と差分計算
        for i in range(len(groups)):
            g = groups[i]
            start_video_num = g["douga"]

            # 次のグループの開始番号（存在しなければ最終グループ）
            if i + 1 < len(groups):
                end_video_num = groups[i + 1]["douga"]
            else:
                end_video_num = None

            total_video_time = 0.0
            video_num_to_check = start_video_num

            # 該当する連番の動画ファイルを合計
            while True:
                if end_video_num is not None and video_num_to_check >= end_video_num:
                    break

                video_file_name = f"douga_{video_num_to_check:04d}.mp4"
                video_file_path = os.path.join(video_dir, video_file_name)

                if os.path.exists(video_file_path):
                    total_video_time += get_mp4_duration(video_file_path)
                    video_num_to_check += 1
                else:
                    break

            g["video_time_sum"] = total_video_time
            g["diff_time"] = g["video_time_sum"] - g["audio_time_sum"]

        # 3. 差分動画（.wmv）の作成処理（最終グループを除く）
        for i in range(len(groups) - 1):  # 最終グループの手前までループ
            g = groups[i]
            next_group = groups[i + 1]
            diff = g["diff_time"]

            if abs(diff) < 0.001:  # 差分がほぼ0秒ならスキップ
                continue

            if diff > 0:
                # 【差分がプラスの場合】
                # 次のグループのaudioの開始番号から1引いた数値
                next_audio = next_group.get("audio")
                if next_audio is not None:
                    target_audio_num = next_audio - 1
                    file_name = f"audio_{target_audio_num:04d}_space.wmv"
                    # 出力フォルダは #audio.txt のあるフォルダ
                    output_folder = os.path.dirname(audio_path)
                    output_file_path = os.path.join(output_folder, file_name)

                    create_space_wmv(diff, output_file_path)

            elif diff < 0:
                # 【差分がマイナスの場合】
                # 次のグループのdougaの開始番号から1引いた数値
                next_douga = next_group.get("douga")
                if next_douga is not None:
                    target_douga_num = next_douga - 1
                    file_name = f"douga_{target_douga_num:04d}_space.wmv"
                    # 出力フォルダは ドロップした動画フォルダ
                    output_folder = video_dir
                    output_file_path = os.path.join(output_folder, file_name)

                    create_space_wmv(diff, output_file_path)

        # 4. #group.txt への一覧出力
        output_dir = os.path.dirname(audio_path)
        output_path = os.path.join(output_dir, "#group.txt")

        with open(output_path, "w", encoding="utf-8") as out_f:
            out_f.write("grp\tdo\tau\td-time\ta-time\tdiff-time\n")
            for g in groups:
                out_f.write(
                    f"{g['group']}\t{g['douga']}\t{g['audio']}\t"
                    f"{g['video_time_sum']:.3f}\t{g['audio_time_sum']:.3f}\t{g['diff_time']:.3f}\n"
                )

        return output_path

    except Exception as e:
        raise e


class GroupCheckApp:
    """
    GUIアプリケーションクラス
    """

    def __init__(self, root):
        self.root = root
        self.root.title("InsertSpace - 音声と動画の尺合わせのスペーサーを追加")
        self.root.geometry("600x400")

        self.audio_file_path = ""
        self.video_dir_path = ""

        # --- 1. #audio.txt 用ドロップエリア ---
        self.audio_frame = tk.LabelFrame(root, text="1. #audio.txt の指定", padx=10, pady=5)
        self.audio_frame.pack(pady=10, padx=20, fill=tk.X)

        self.audio_drop_label = tk.Label(
            self.audio_frame,
            text="ここに #audio.txt をドロップするか、右のボタンから選択してください",
            relief="groove",
            borderwidth=1,
            height=3,
            bg="#f9f9f9"
        )
        self.audio_drop_label.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(0, 10))
        self.audio_drop_label.drop_target_register(DND_FILES)
        self.audio_drop_label.dnd_bind('<<Drop>>', self.on_audio_drop)

        self.audio_btn = tk.Button(self.audio_frame, text="ファイル選択", command=self.select_audio_file)
        self.audio_btn.pack(side=tk.RIGHT)

        # --- 2. 動画フォルダ用ドロップエリア ---
        self.video_frame = tk.LabelFrame(root, text="2. 動画(douga_****.mp4)のあるフォルダ(douga_)の指定", padx=10, pady=5)
        self.video_frame.pack(pady=10, padx=20, fill=tk.X)

        self.video_drop_label = tk.Label(
            self.video_frame,
            text="ここに動画フォルダ（douga_）をドロップするか、右のボタンから選択してください",
            relief="groove",
            borderwidth=1,
            height=3,
            bg="#f9f9f9"
        )
        self.video_drop_label.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(0, 10))
        self.video_drop_label.drop_target_register(DND_FILES)
        self.video_drop_label.dnd_bind('<<Drop>>', self.on_video_drop)

        self.video_btn = tk.Button(self.video_frame, text="フォルダ選択", command=self.select_video_dir)
        self.video_btn.pack(side=tk.RIGHT)

        # --- 3. 選択情報表示 ---
        self.info_frame = tk.Frame(root)
        self.info_frame.pack(pady=10, padx=20, fill=tk.X)

        self.audio_path_lbl = tk.Label(self.info_frame, text="音声ファイル: 未設定", fg="gray", anchor="w",
                                       wraplength=550)
        self.audio_path_lbl.pack(fill=tk.X)

        self.video_path_lbl = tk.Label(self.info_frame, text="動画フォルダ: 未設定", fg="gray", anchor="w",
                                       wraplength=550)
        self.video_path_lbl.pack(fill=tk.X)

        # --- 4. 実行ボタン ---
        self.run_btn = tk.Button(
            root,
            text="開始（スペーサー[*_space.wmv]を作成）",
            command=self.run_process,
            bg="#4CAF50",
            fg="white",
            font=("MS Gothic", 12, "bold"),
            width=40,
            height=2,
            state=tk.DISABLED  # 両方揃うまで無効
        )
        self.run_btn.pack(pady=15)

    # ドラッグ＆ドロップ用処理
    def on_audio_drop(self, event):
        path = self.clean_path(event.data)
        if os.path.isfile(path):
            self.set_audio_path(path)
        else:
            messagebox.showwarning("警告", "ファイルをドロップしてください。")

    def on_video_drop(self, event):
        path = self.clean_path(event.data)
        if os.path.isdir(path):
            self.set_video_path(path)
        else:
            messagebox.showwarning("警告", "フォルダをドロップしてください。")

    # ファイル・フォルダ選択ダイアログ
    def select_audio_file(self):
        path = filedialog.askopenfilename(
            title="#audio.txtを選択してください",
            filetypes=[("Text files", "#audio.txt"), ("All files", "*.*")]
        )
        if path:
            self.set_audio_path(path)

    def select_video_dir(self):
        path = filedialog.askdirectory(title="動画（douga_****.mp4）があるフォルダを選択してください")
        if path:
            self.set_video_path(path)

    def clean_path(self, path):
        if path.startswith('{') and path.endswith('}'):
            path = path[1:-1]
        return path

    def set_audio_path(self, path):
        if os.path.basename(path) != "#audio.txt":
            messagebox.showwarning("警告", "ファイル名が '#audio.txt' ではありません。確認してください。")
        self.audio_file_path = path
        self.audio_drop_label.config(text=os.path.basename(path), bg="#e8f0fe")
        self.audio_path_lbl.config(text=f"音声ファイル: {path}", fg="black")
        self.check_ready()

    def set_video_path(self, path):
        self.video_dir_path = path
        self.video_drop_label.config(text=os.path.basename(path) if os.path.basename(path) else path, bg="#e8f0fe")
        self.video_path_lbl.config(text=f"動画フォルダ: {path}", fg="black")
        self.check_ready()

    def check_ready(self):
        if self.audio_file_path and self.video_dir_path:
            self.run_btn.config(state=tk.NORMAL)

    def run_process(self):
        if not os.path.exists(self.audio_file_path) or not os.path.exists(self.video_dir_path):
            messagebox.showerror("エラー", "指定されたファイル、またはフォルダが存在しません。")
            return

        try:
            output_path = process_audio_file(self.audio_file_path, self.video_dir_path)
            messagebox.showinfo(
                "完了",
                f"処理が正常に完了しました！\n\n・一覧を出力しました:\n{output_path}\n\n・差分動画(*_space.wmv)を生成・上書きしました。"
            )
        except Exception as e:
            messagebox.showerror("エラー", f"処理中にエラーが発生しました:\n{str(e)}")


if __name__ == "__main__":
    root = TkinterDnD.Tk()
    app = GroupCheckApp(root)
    root.mainloop()