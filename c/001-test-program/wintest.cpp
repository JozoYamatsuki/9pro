#include <windows.h>

#pragma comment(lib, "user32.lib")

#define ID_OK_BUTTON 1001

// ウィンドウのメッセージ処理（プロシージャ）
LRESULT CALLBACK WndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
	switch (msg) {
	case WM_CREATE:
		// "OK" ボタンを作成
		CreateWindow(
			"BUTTON", "OK",
			WS_VISIBLE | WS_CHILD | BS_DEFPUSHBUTTON,
			100, 70, 80, 30, // X位置, Y位置, 幅, 高さ
			hwnd, (HMENU)ID_OK_BUTTON,
			((LPCREATESTRUCT)lParam)->hInstance, NULL
		);
		break;

	case WM_COMMAND:
		// OKボタンが押された場合にウィンドウを破棄
		if (LOWORD(wParam) == ID_OK_BUTTON) {
			DestroyWindow(hwnd);
		}
		break;

	case WM_DESTROY:
		// メッセージループを終了させる
		PostQuitMessage(0);
		break;

	default:
		return DefWindowProc(hwnd, msg, wParam, lParam);
	}
	return 0;
}

// エントリポイント
int WINAPI WinMain(_In_ HINSTANCE hInstance, _In_opt_ HINSTANCE hPrevInstance, _In_ LPSTR lpCmdLine, _In_ int nCmdShow) {
	const char CLASS_NAME[] = "MinWindowClass";

	WNDCLASS wc = { 0 };
	wc.lpfnWndProc = WndProc;
	wc.hInstance = hInstance;
	wc.lpszClassName = CLASS_NAME;
	wc.hbrBackground = (HBRUSH)(COLOR_WINDOW + 1); // 一般的なウィンドウの背景色：白

	RegisterClass(&wc);

	// ウィンドウの作成（タイトル: "Hello world"）
	HWND hwnd = CreateWindowEx(
		0, CLASS_NAME, "Hello world",
		WS_OVERLAPPEDWINDOW,
		CW_USEDEFAULT, CW_USEDEFAULT, 300, 170, // 表示位置・幅・高さ
		NULL, NULL, hInstance, NULL
	);

	ShowWindow(hwnd, nCmdShow);

	// メッセージループ
	MSG msg;
	while (GetMessage(&msg, NULL, 0, 0)) {
		TranslateMessage(&msg);
		DispatchMessage(&msg);
	}

	return (int)msg.wParam;
}
