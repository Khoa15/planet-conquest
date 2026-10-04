# Báo cáo hiệu năng Planet Conquest 0.5.0

Phiên bản app: 0.5.0 (versionCode 5), nhánh `feature/language-switch`, commit `8d0c89e`.

Ngày đo: 2026-10-04. Cách đo: `adb` (`/proc/<pid>/stat`, `dumpsys meminfo`, `dumpsys gfxinfo`, `top -H`).

## Môi trường đo

| Mục | Giá trị |
|---|---|
| Thiết bị | Emulator `vbox86p` (Android 15, x86, 8 lõi, RAM 8 GB, màn 570x1230) |
| App | `com.planetconquest.game` 0.5.0 (versionCode 5) |
| Kịch bản | Welcome (~15 s, đứng yên) rồi vào màn Hướng dẫn, vuốt/chạm liên tục ~20 s |

**Lưu ý độ tin cậy**: đây là emulator dùng GPU ảo (`local_opengl`), không phải máy thật. Số CPU và jank (khung hình giật) bị thổi phồng; số RAM và dung lượng đáng tin hơn. Lần mở đầu tiên app bị ANR do một frame treo 18 s ở `SwapBuffers`; log còn nhiều ANR từ hôm qua, nên đây là lỗi của emulator. Lần chạy lại không tái hiện. Nên đo lại trên máy thật trước khi kết luận về CPU/FPS.

## 1. CPU

| Trạng thái | CPU (% của 1 lõi) |
|---|---|
| Đang chơi (vuốt/chạm liên tục) | 49 - 56% (trung bình ~51%) |
| Chơi nhưng không thao tác / tạm dừng | ~0% |
| Thời gian khởi động nguội (`am start -W`) | 1,56 s (lần 1: 2,27 s) |

- Mức ~51% là tải của vòng lặp khung hình (Canvas, vẽ `StarField`, hạt, hành tinh). Khi dừng thao tác, CPU về ~0%, tức tạm dừng thật sự gỡ vòng lặp đúng thiết kế.
- Chi tiết khung hình trong 24 s chơi: 657 frame, **50th 53 ms, 90th 89 ms, 95th 105 ms, 99th 150 ms**, jank 69,7%, Slow UI thread 337 lần. Nút thắt nằm ở luồng UI (vẽ Canvas trên CPU), GPU gần như rảnh (~1 ms). Một phần do emulator, nhưng vẽ bằng `Canvas` mềm trên main thread là điểm nên tối ưu.

## 2. RAM

| Chỉ số | Giá trị |
|---|---|
| PSS tổng | **~22,6 MB** (23,6 - 24,4 MB khi chơi) |
| Java/Dalvik heap đang cấp phát | 2,0 MB (heap 4,0 MB) |
| Native heap đang cấp phát | 20,3 MB (heap 29,4 MB) |
| Graphics (GPU) | 0 MB |
| Code (mmap) | 1,0 MB PSS |
| RSS | ~146 - 152 MB (gồm thư viện hệ thống dùng chung, không phải bộ nhớ riêng của app) |
| Số thread | 27 - 32 |
| Rò rỉ | Không thấy: PSS ổn định 23,6 - 24,4 MB suốt phiên; 1 Activity, 5 View |

Native heap lớn hơn Java heap, nhiều khả năng do Bitmap nền `StarField` (ARGB_8888 cỡ màn hình, ~2,8 MB với 570x1230) và âm thanh phát nhạc nền. Mức dùng RAM rất thấp.

## 3. Disk

| Mục | Dung lượng |
|---|---|
| APK cài (`base.apk`) | **3,31 MB** (3.307.086 byte) |
| Trong đó nhạc nền WAV | `bgm_menu.wav` 1,76 MB + `bgm_game.wav` 1,28 MB = 3,04 MB (**~90% APK**) |
| `classes.dex` | 164 KB |
| 10 hiệu ứng `.ogg` | ~89 KB |
| Dữ liệu người dùng | Chỉ `SharedPreferences` (`planet_conquest`: tiến độ, ngôn ngữ, âm thanh), vài KB. Không có SQLite hay file ghi ra ổ đĩa (đối chiếu mã nguồn) |

Không đo trực tiếp được thư mục `/data/data/...` vì app không debuggable và emulator không root (`run-as` bị từ chối).

Đề xuất rẻ nhất: đổi 2 file WAV sang OGG/AAC có thể giảm APK từ ~3,3 MB xuống khoảng 0,5 MB.

## 4. Cache

| Mục | Giá trị |
|---|---|
| `cacheDir` / `getExternalCacheDir` | Mã nguồn không dùng (`grep getCacheDir` rỗng); ước tính ~0 |
| `code_cache` (ART/JIT) | Hệ thống tự tạo, nhỏ |
| Cache trong bộ nhớ | Chỉ Bitmap nền `StarField` (~2,8 MB, đã tính trong RAM); không có `LruCache` |

Không đọc được số byte cache thực từ máy vì lý do quyền như trên. Nếu cần số chính xác, build bản debuggable rồi chạy `adb shell run-as com.planetconquest.game du -sk cache code_cache files shared_prefs`.

## Kết luận

- **Tốt**: RAM ~23 MB, không rò rỉ, APK 3,3 MB, không ghi disk/cache đáng kể, CPU về 0% khi tạm dừng.
- **Cần xem**: tải CPU ~51% khi chơi và nhiều frame trên 16 ms. Cần đo lại trên máy thật; nếu vẫn giật, tối ưu vẽ (cache layer tĩnh, giảm cấp phát mỗi frame) hoặc chuyển sang `SurfaceView`/vẽ ở luồng riêng.
- **Dễ cải thiện**: nén nhạc nền WAV.
