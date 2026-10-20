import os
import re
import pygame
from send2trash import send2trash
import cv2
import numpy as np

import tkinter as tk
from tkinter import messagebox, simpledialog
from tkinterdnd2 import DND_FILES, TkinterDnD


class AudioPlayerApp:
    def __init__(self, root):
        self.root = root
        self.root.title("AudioSelector - フォルダ未指定")
        self.root.geometry("520x360")
        self.root.resizable(False, False)

        # Pygame Mixer 初期化
        pygame.mixer.init()

        # 内部状態
        self.current_dir = None
        self.current_filename = None
        self.is_playing = False
        self.hide_deleted_mode = True  # True: 削除予定の表示なし, False: 削除予定の表示あり

        self.delete_list_filename = "#delete_file.txt"

        # GUI構築
        self.create_widgets()

        # キーバインドの設定
        self.root.bind("<space>", lambda e: self.play_current())
        self.root.bind("<s>", lambda e: self.stop_play())
        self.root.bind("<Left>", lambda e: self.play_prev())
        self.root.bind("<Right>", lambda e: self.play_next())
        self.root.bind("<Return>", lambda e: self.add_blank_file())
        self.root.bind("<d>", lambda e: self.mark_delete())
        self.root.bind("<z>", lambda e: self.cancel_delete())

    def create_widgets(self):
        # 1. フォルダドロップ & 再生中ファイル表示エリア
        self.drop_frame = tk.Frame(self.root, bd=2, relief="groove", bg="#f0f0f0", height=80)
        self.drop_frame.pack(fill="x", padx=10, pady=10)
        self.drop_frame.pack_propagate(False)

        self.label_info = tk.Label(
            self.drop_frame,
            text="ここにフォルダをドロップしてください",
            font=("Meiryo", 11, "bold"),
            bg="#f0f0f0",
            wraplength=480,
            cursor="hand2"
        )
        self.label_info.pack(expand=True, fill="both")

        # クリックでファイル選択ダイアログを開く
        self.label_info.bind("<Button-1>", lambda e: self.open_select_dialog())

        # TkinterDnD のドロップターゲット登録（フォルダ指定用）
        self.drop_frame.drop_target_register(DND_FILES)
        self.drop_frame.dnd_bind('<<Drop>>', self.on_drop_folder)

        # 2. 操作ボタンエリア
        btn_frame = tk.Frame(self.root)
        btn_frame.pack(pady=5)

        # 1行目: [再生] [停止] [前] [次]
        row1 = tk.Frame(btn_frame)
        row1.pack(pady=2)
        tk.Button(row1, text="再生 <Space>", width=11, command=self.play_current).pack(side="left", padx=3)
        tk.Button(row1, text="停止 <s>", width=11, command=self.stop_play).pack(side="left", padx=3)
        tk.Button(row1, text="前 <←>", width=11, command=self.play_prev).pack(side="left", padx=3)
        tk.Button(row1, text="次 <→>", width=11, command=self.play_next).pack(side="left", padx=3)

        # 2行目: [blank]
        row2 = tk.Frame(btn_frame)
        row2.pack(pady=2)
        tk.Button(row2, text="blank <Enter>", width=48, command=self.add_blank_file).pack(padx=3)

        # 3行目: [削除] [取消]
        row3 = tk.Frame(btn_frame)
        row3.pack(pady=2)
        tk.Button(row3, text="削除 <d>", width=23, command=self.mark_delete).pack(side="left", padx=3)
        tk.Button(row3, text="取消 <z>", width=23, command=self.cancel_delete).pack(side="left", padx=3)

        # 4行目: [削除実行]
        row4 = tk.Frame(btn_frame)
        row4.pack(pady=2)
        tk.Button(row4, text="削除実行", width=48, bg="#ffcccc", command=self.execute_delete).pack(padx=3)

        # 5行目: [削除予定の表示モード切替]
        row5 = tk.Frame(btn_frame)
        row5.pack(pady=2)
        self.btn_mode = tk.Button(row5, text="削除予定の表示なし", width=48, command=self.toggle_delete_mode)
        self.btn_mode.pack(padx=3)

    # --- フォルダドロップ処理 ---
    def on_drop_folder(self, event):
        path = event.data.strip('{}')
        if os.path.isdir(path):
            self.current_dir = path
            self.root.title(f"AudioSelector - {self.current_dir}")
            target = self.get_target_file(direction="first")
            if target:
                self.start_play(target)
            else:
                self.current_filename = None
                self.update_display("対象ファイルが見つかりません")
        else:
            messagebox.showwarning("警告", "フォルダをドロップしてください。")

    # --- ファイル選択ダイアログ (要望4) ---
    def open_select_dialog(self):
        if not self.current_dir:
            messagebox.showinfo("案内", "先にフォルダをドロップして指定してください。")
            return

        dialog = tk.Toplevel(self.root)
        dialog.title("ファイル指定")
        dialog.geometry("380x160")
        dialog.resizable(False, False)
        dialog.grab_set()

        lbl = tk.Label(dialog, text="ファイル番号(例: 0150, 0001a)を入力\nまたはここにファイルをドロップしてください",
                       font=("Meiryo", 9))
        lbl.pack(pady=10)

        entry = tk.Entry(dialog, width=25, font=("Meiryo", 10))
        entry.pack(pady=5)
        entry.focus_set()

        def apply_selection(selected_file_or_num):
            cleaned = selected_file_or_num.strip('{}').strip()
            if not cleaned:
                return

            # ファイルパスが直接ドロップされた場合
            if os.path.exists(cleaned):
                fname = os.path.basename(cleaned)
            else:
                # 入力が番号または識別子の場合 (例: 0150 -> audio_0150.wav)
                if not cleaned.startswith("audio_"):
                    fname = f"audio_{cleaned}.wav"
                else:
                    fname = cleaned

            all_files = self.get_all_target_files()

            # 完全一致検索、または拡張子補完検索
            matched = None
            if fname in all_files:
                matched = fname
            else:
                # 該当するプレフィックスのファイルを探索
                base_target = fname.replace(".wav", "").replace(".wmv", "")
                for f in all_files:
                    if f.startswith(base_target):
                        matched = f
                        break

            if matched:
                dialog.destroy()
                self.start_play(matched)
            else:
                messagebox.showerror("エラー", f"指定されたファイルが見つかりません:\n{fname}", parent=dialog)

        # ドロップ対応
        dialog.drop_target_register(DND_FILES)
        dialog.dnd_bind('<<Drop>>', lambda e: apply_selection(e.data))

        btn_frame = tk.Frame(dialog)
        btn_frame.pack(pady=10)
        tk.Button(btn_frame, text="決定", width=10, command=lambda: apply_selection(entry.get())).pack(side="left",
                                                                                                       padx=5)
        tk.Button(btn_frame, text="キャンセル", width=10, command=dialog.destroy).pack(side="left", padx=5)

        dialog.bind("<Return>", lambda e: apply_selection(entry.get()))

    # --- ファイル検索・取得ロジック (要望1, 5) ---
    def get_all_target_files(self):
        """現在のフォルダから条件に合うファイルの一覧を取得し並び替える"""
        if not self.current_dir or not os.path.exists(self.current_dir):
            return []

        # audio_xxxx%.wav または audio_xxxx%_blank.wmv (xxxx:数字4桁, %:アルファベット可)
        pattern = re.compile(r"^audio_(\d{4}[a-zA-Z]*)(_blank\.wmv|\.wav)$", re.IGNORECASE)
        files = []
        for f in os.listdir(self.current_dir):
            match = pattern.match(f)
            if match:
                code = match.group(1).lower()  # 例: "0001", "0001a"
                is_blank = 1 if match.group(2).lower() == "_blank.wmv" else 0
                files.append((code, is_blank, f))

        # ソート規則: コード順 (0001 < 0001a), 同コードなら .wav -> _blank.wmv の順
        files.sort(key=lambda x: (x[0], x[1]))
        return [f[2] for f in files]

    def get_delete_list(self):
        """#delete_file.txt から削除予定ファイル名一覧を取得"""
        if not self.current_dir:
            return set()
        filepath = os.path.join(self.current_dir, self.delete_list_filename)
        if not os.path.exists(filepath):
            return set()
        with open(filepath, "r", encoding="utf-8") as f:
            lines = [line.strip() for line in f.readlines() if line.strip()]
        return set(lines)

    def save_delete_list(self, del_set):
        """#delete_file.txt に書き込み"""
        if not self.current_dir:
            return
        filepath = os.path.join(self.current_dir, self.delete_list_filename)
        with open(filepath, "w", encoding="utf-8") as f:
            for item in sorted(list(del_set)):
                f.write(item + "\n")

    def get_target_file(self, direction="next", from_filename=None):
        """現在のファイル位置とモードを基準に、次/前/最初の再生対象ファイルを算出 (要望7修正)"""
        all_files = self.get_all_target_files()
        if not all_files:
            return None

        del_set = self.get_delete_list()

        # 表示なしモードなら削除予定を除外
        if self.hide_deleted_mode:
            valid_files = [f for f in all_files if f not in del_set]
        else:
            valid_files = all_files

        if not valid_files:
            return None

        if direction == "first":
            return valid_files[0]

        ref_file = from_filename if from_filename else self.current_filename

        if ref_file not in valid_files:
            # 基準ファイルが有効リストに存在しない場合、元ファイルの位置に近い次のファイルを探索
            if ref_file in all_files:
                orig_idx = all_files.index(ref_file)
                # 以降で有効なものを探す
                for f in all_files[orig_idx:]:
                    if f in valid_files:
                        return f
            return valid_files[0]

        idx = valid_files.index(ref_file)
        if direction == "next":
            next_idx = (idx + 1) % len(valid_files)
            return valid_files[next_idx]
        elif direction == "prev":
            prev_idx = (idx - 1 + len(valid_files)) % len(valid_files)
            return valid_files[prev_idx]

        return None

    # --- 再生制御 (要望2, 3) ---
    def start_play(self, filename):
        if not filename or not self.current_dir:
            return

        self.stop_play()
        self.current_filename = filename
        filepath = os.path.join(self.current_dir, filename)

        if os.path.exists(filepath):
            # wmvファイルは表示のみで再生処理を行わない（エラー回避）
            if filename.lower().endswith(".wmv"):
                self.is_playing = True
            else:
                try:
                    pygame.mixer.music.load(filepath)
                    pygame.mixer.music.play()
                    self.is_playing = True
                except Exception as e:
                    print(f"再生エラー: {e}")
                    self.is_playing = False

        self.update_display()

    def play_current(self):
        """[再生] ボタン: 現在のファイルを頭から再生 (要望3)"""
        if not self.current_filename:
            return
        self.start_play(self.current_filename)

    def stop_play(self):
        """[停止] ボタン: 再生を停止 (要望3)"""
        pygame.mixer.music.stop()
        pygame.mixer.music.unload()
        self.is_playing = False

    def play_next(self):
        target = self.get_target_file(direction="next")
        if target:
            self.start_play(target)

    def play_prev(self):
        target = self.get_target_file(direction="prev")
        if target:
            self.start_play(target)

    # --- 表示更新 ---
    def update_display(self, custom_msg=None):
        if custom_msg:
            self.label_info.config(text=custom_msg)
            return

        if not self.current_filename:
            self.label_info.config(text="ファイルが選択されていません")
            return

        del_set = self.get_delete_list()
        text = self.current_filename
        if self.current_filename in del_set:
            text += " 【削除予定】"

        self.label_info.config(text=text)

    # --- 各種機能（削除・取消・blank追加） ---
    def mark_delete(self):
        """再生中のファイルを削除予定にする (要望7)"""
        if not self.current_filename:
            return

        target_to_del = self.current_filename

        del_set = self.get_delete_list()
        del_set.add(target_to_del)
        self.save_delete_list(del_set)

        if self.hide_deleted_mode:
            # 表示なしモード：直前のファイルを基準に次のファイルを探索して再生
            next_target = self.get_target_file(direction="next", from_filename=target_to_del)
            if next_target and next_target != target_to_del:
                self.start_play(next_target)
            else:
                self.stop_play()
                self.current_filename = None
                self.update_display("対象ファイルが見つかりません")
        else:
            # 表示ありモード：表示のみ更新
            self.update_display()

    def cancel_delete(self):
        """削除予定を取り消す"""
        if not self.current_filename:
            return

        del_set = self.get_delete_list()
        if self.current_filename in del_set:
            del_set.remove(self.current_filename)
            self.save_delete_list(del_set)

        self.update_display()

    def execute_delete(self):
        """削除予定のファイルをゴミ箱に移す (要望6: パス正規化対応)"""
        if not self.current_dir:
            return

        del_set = self.get_delete_list()
        if not del_set:
            messagebox.showinfo("情報", "削除予定のファイルはありません。")
            return

        if messagebox.askyesno("確認", f"{len(del_set)} 件のファイルをゴミ箱に移動しますか？"):
            for filename in del_set:
                # パスを絶対パスかつOS正規化（バックスラッシュ統一）する
                filepath = os.path.abspath(os.path.normpath(os.path.join(self.current_dir, filename)))
                if os.path.exists(filepath):
                    try:
                        send2trash(filepath)
                    except Exception as e:
                        print(f"ゴミ箱への移動に失敗: {filename} ({e})")

            # 再生中のファイルが削除対象だった場合は停止
            if self.current_filename in del_set:
                self.stop_play()
                self.current_filename = None

            # 次の再生可能なファイルを取得して再生
            target = self.get_target_file(direction="first")
            if target:
                self.start_play(target)
            else:
                self.update_display("対象ファイルが見つかりません")

    def toggle_delete_mode(self):
        """削除予定の表示モード切替"""
        self.hide_deleted_mode = not self.hide_deleted_mode
        if self.hide_deleted_mode:
            self.btn_mode.config(text="削除予定の表示なし")
        else:
            self.btn_mode.config(text="削除予定の表示あり")

        # モード切替時に現在表示中ファイルがスキップ対象なら次へ
        if self.hide_deleted_mode:
            del_set = self.get_delete_list()
            if self.current_filename in del_set:
                self.play_next()
        self.update_display()

    def add_blank_file(self):
        """blankファイル(audio_xxxx_blank.wmv)を生成・追加して再生 (要望1, 5)"""
        if not self.current_filename or not self.current_dir:
            return

        # 現在のファイル名からxxxx%（数値+アルファベット識別子）を取得
        match = re.search(r"audio_(\d{4}[a-zA-Z]*)", self.current_filename)
        if not match:
            return

        code_str = match.group(1)
        blank_filename = f"audio_{code_str}_blank.wmv"
        blank_filepath = os.path.join(self.current_dir, blank_filename)

        # 存在しなければ生成 (白背景 200ms 動画)
        if not os.path.exists(blank_filepath):
            self.generate_blank_wmv(blank_filepath)

        # 生成したblankファイルを再生対象にする
        self.start_play(blank_filename)

    def generate_blank_wmv(self, output_path):
        """OpenCVを使用して200msの背景白の動画ファイルを作成"""
        fps = 30
        duration_sec = 0.2  # 200ms
        total_frames = int(fps * duration_sec)
        width, height = 320, 240

        # WMV用コーディック設定
        fourcc = cv2.VideoWriter_fourcc(*'WMV2')
        out = cv2.VideoWriter(output_path, fourcc, fps, (width, height))

        # 白色フレーム (255, 255, 255)
        white_frame = np.full((height, width, 3), 255, dtype=np.uint8)

        for _ in range(total_frames):
            out.write(white_frame)

        out.release()


if __name__ == "__main__":
    root = TkinterDnD.Tk()
    app = AudioPlayerApp(root)
    root.mainloop()
