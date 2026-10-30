import os
import re
import json
import tkinter as tk
from tkinter import messagebox
from tkinterdnd2 import TkinterDnD, DND_FILES
import subprocess

# --- 設定：出力フォルダの固定 ---
OUTPUT_DIR = r"C:\file\obs_output\douga_"
FPS = 30

if not os.path.exists(OUTPUT_DIR):
    os.makedirs(OUTPUT_DIR)


def get_video_fps(file_path):
    """ffprobeを使って動画のFPSを取得する関数"""
    cmd = (
        f'ffprobe -v error -select_streams v:0 '
        f'-show_entries stream=r_frame_rate '
        f'-of json "{file_path}"'
    )
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True, check=True)
    data = json.loads(result.stdout)
    r_frame_rate = data['streams'][0]['r_frame_rate']
    
    num, den = map(int, r_frame_rate.split('/'))
    fps = num / den
    return round(fps)


def parse_time_format(time_str):
    match = re.match(r"(\d+):(\d+)\.(\d+)", time_str)
    if not match:
        raise ValueError(f"不正な時間フォーマットです: {time_str}")
    minutes, seconds, frames = map(int, match.groups())
    return minutes * 60 + seconds + (frames / FPS)


def format_time_format(total_seconds):
    minutes = int(total_seconds // 60)
    seconds = int(total_seconds % 60)
    frames = int(round((total_seconds - (minutes * 60 + seconds)) * FPS))
    if frames >= FPS:
        seconds += 1
        frames -= FPS
    return f"{minutes:02d}:{seconds:02d}.{frames:02d}"


class VideoCutEditorApp:
    def __init__(self, root):
        self.root = root
        self.root.title("VideoEditor動画カット編集ツール (edit.txtに従い動画を分割する)")
        self.root.geometry("650x300")
        self.mkv_path = ""
        self.txt_path = ""
        self.create_widgets()

    def create_widgets(self):
        main_frame = tk.Frame(self.root, padx=20, pady=20)
        main_frame.pack(fill=tk.BOTH, expand=True)

        self.mkv_label = tk.Label(main_frame, text="動画ファイル (.mkv/.mp4) をドロップ", bg="#e1f5fe", height=3,
                                  relief="groove")
        self.mkv_label.pack(fill=tk.X, pady=5)
        self.mkv_label.drop_target_register(DND_FILES)
        self.mkv_label.dnd_bind('<<Drop>>', self.drop_mkv)

        self.txt_label = tk.Label(main_frame, text="指示用テキストファイル (edit.txt) をドロップ", bg="#e8f5e9",
                                  height=3, relief="groove")
        self.txt_label.pack(fill=tk.X, pady=5)
        self.txt_label.drop_target_register(DND_FILES)
        self.txt_label.dnd_bind('<<Drop>>', self.drop_txt)

        self.status_label = tk.Label(main_frame, text=f"出力先: {OUTPUT_DIR}", fg="black")
        self.status_label.pack(pady=10)

        btn_frame = tk.Frame(main_frame)
        btn_frame.pack(pady=10)

        self.run_btn = tk.Button(btn_frame, text="実行",
                                 command=self.process_video, state=tk.DISABLED,
                                 bg="#9e9e9e", fg="white", font=("Arial", 11, "bold"), padx=20, pady=5)
        self.run_btn.pack(side=tk.LEFT, padx=10)

    def drop_mkv(self, event):
        global FPS
        path = event.data.strip('{}')
        if path.lower().endswith('.mkv') or path.lower().endswith('.mp4'):
            try:
                FPS = get_video_fps(path)
                self.mkv_path = path
                self.mkv_label.config(text=f"動画: {os.path.basename(path)} (FPS={FPS})", bg="#b3e5fc")
                self.check_ready()
            except Exception as e:
                messagebox.showerror("エラー", f"動画のFPS解析に失敗しました:\n{str(e)}")

    def drop_txt(self, event):
        path = event.data.strip('{}')
        if path.lower().endswith('.txt'):
            self.txt_path = path
            self.txt_label.config(text=f"指示書: {os.path.basename(path)}", bg="#c8e6c9")
            self.check_ready()

    def check_ready(self):
        if self.mkv_path and self.txt_path:
            self.run_btn.config(state=tk.NORMAL, bg="#2196f3")

    def process_video(self):
        self.status_label.config(text="処理中... (長尺対応モード)", fg="orange")
        self.root.update()

        try:
            verify_lines = []
            current_timeline = 0.0
            concat_list_path = os.path.join(OUTPUT_DIR, "concat_list.txt")

            with open(self.txt_path, 'r', encoding='utf-8') as f:
                lines = f.readlines()
                line_count = len(lines) - 1

            with open(concat_list_path, 'w', encoding='utf-8') as cl_f:
                for line in lines:
                    line_str = line.strip()
                    if not line_str or line_str.startswith("no"):
                        continue

                    parts = line_str.split()
                    if len(parts) < 4:
                        continue

                    no = int(parts[0])
                    v_type = parts[1]
                    out_filename = f"douga_{no:04d}.mp4"
                    out_path = os.path.join(OUTPUT_DIR, out_filename)
                    self.status_label.config(text=f"処理中... (長尺対応モード) {no:d} / {line_count:d}", fg="orange")
                    self.root.update()

                    duration = 0.0

                    if v_type == "wide":
                        t1_sec = parse_time_format(parts[2])
                        t2_sec = parse_time_format(parts[3])
                        duration = t2_sec - t1_sec + (1 / FPS)
                        cmd = f'ffmpeg -y -ss {t1_sec} -i "{self.mkv_path}" -t {duration} -c:v h264_nvenc -c:a aac -async 1 -v error "{out_path}"'
                        subprocess.run(cmd, shell=True, check=True)

                    start_str = format_time_format(current_timeline)
                    end_str = format_time_format(current_timeline + duration - (1 / FPS))
                    verify_lines.append(f"{line_str} // {start_str} - {end_str}\n")
                    current_timeline += duration

                    cl_f.write(f"file '{out_filename}'\n")

            with open(os.path.join(OUTPUT_DIR, "verify.txt"), 'w', encoding='utf-8') as vf:
                vf.writelines(verify_lines)

            self.status_label.config(text="全体動画を結合中...", fg="orange")
            self.root.update()

            temp_combined_path = os.path.join(OUTPUT_DIR, "temp_combined.mp4")
            cmd_concat = f'ffmpeg -y -f concat -safe 0 -i "{concat_list_path}" -c copy -v error "{temp_combined_path}"'
            subprocess.run(cmd_concat, shell=True, check=True)

            final_out_path = os.path.join(OUTPUT_DIR, "douga.mp4")
            if os.path.exists(final_out_path):
                os.remove(final_out_path)
            os.rename(temp_combined_path, final_out_path)

            for temp_file in ["concat_list.txt", "temp_combined.mp4"]:
                tp = os.path.join(OUTPUT_DIR, temp_file)
                if os.path.exists(tp):
                    os.remove(tp)

            self.status_label.config(text="すべての処理が完了しました！", fg="green")
            messagebox.showinfo("完了", f"処理が完了しました。\n保存先: {OUTPUT_DIR}")

        except Exception as e:
            self.status_label.config(text="エラー発生", fg="red")
            messagebox.showerror("エラー", f"詳細: {str(e)}")


if __name__ == "__main__":
    root = TkinterDnD.Tk()
    app = VideoCutEditorApp(root)
    root.mainloop()