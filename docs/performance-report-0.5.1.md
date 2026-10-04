# Báo cáo hiệu năng Planet Conquest 0.5.1

Phiên bản app: 0.5.1 (versionCode 6), nhánh `feature/language-switch`, dựa trên commit `8d0c89e` (chưa commit thay đổi).
Ngày đo: 2026-10-04. So sánh với [0.5.0](performance-report-0.5.0.md).

## Thay đổi so với 0.5.0

1. **Nhạc nền**: `bgm_menu.wav`, `bgm_game.wav` (3,04 MB) đổi sang Opus mono 48 kbps (`.ogg`, ~0,4 MB). Thêm `audio/PcmDecoder` giải mã ra PCM trên luồng nền (ưu tiên thấp), `Sfx` vẫn lặp vòng bằng `AudioTrack`. `tools/gen_audio.py` sinh `.ogg`.
2. **Bỏ cấp phát mỗi khung hình**: `DrawKit.dashed` dùng `DashPathEffect` dựng sẵn (56 bước), `DrawKit.dotted` dùng chung cho nét chấm vùng tầm và đường nối màn; `PlanetLabels` cache chuỗi số đá/cấp/máu; hằng `BANDS`.
3. **Cache shader hành tinh**: `PlanetShaders` giữ `RadialGradient` quầng sáng + thân theo (phe, bán kính), dựng quanh (0,0) và dịch canvas. Trước đây mỗi hành tinh tạo 2 `RadialGradient` mới mỗi frame.
4. **`StarField`**: Bitmap nền `RGB_565` + dither (nửa dung lượng ARGB_8888).

Cách 1 (cache lớp tĩnh vào Bitmap) đã có sẵn ở `StarField` và shader của `WelcomeScene`; phần còn thiếu của màn chơi được xử lý bằng cách 3. Không cache sprite từng hành tinh vì tốn thêm RAM.

## Môi trường và phương pháp

Emulator `vbox86p` (Android 15, 8 lõi, 570x1230). Mỗi bản đo 2 lượt: cài sạch, mở một lần cho nguội, mở lại, chờ 45 s (để giải mã nhạc xong), vào màn Hướng dẫn, vuốt/chạm liên tục 28 s. CPU lấy từ `/proc/<pid>/stat`, RAM từ `dumpsys meminfo`, frame từ `dumpsys gfxinfo`. Bản 0.5.0 được build lại từ commit `8d0c89e` để so sánh trên cùng emulator.

## Kết quả

| Chỉ số | 0.5.0 (2 lượt) | 0.5.1 (2 lượt) | Thay đổi |
|---|---|---|---|
| **APK** | 3.307.086 B (3,31 MB) | 673.358 B (0,67 MB) | **-80%** |
| **PSS** | 29,9 / 24,7 MB | 22,9 / 22,6 MB | thấp hơn, ổn định hơn |
| **Native heap (alloc)** | 20,9 / 20,9 MB | 18,6 / 18,2 MB | **-11%** |
| Dalvik heap (alloc) | 2,2 / 2,0 MB | 2,0 / 2,1 MB | không đổi |
| **CPU (% của 1 lõi)** | 15,5 / 13,9 | 17,0 / 26,5 | không kết luận được (nhiễu) |
| **Frame p50** | 150 / 97 ms | 97 / 150 ms | không kết luận được (nhiễu) |
| Khởi động nguội | 1,43 / 1,34 s | 1,37 / 1,29 s | tương đương |
| Disk dữ liệu / cache | chỉ `SharedPreferences`, không dùng `cacheDir` | như cũ | không đổi |

## Nhận xét

- **Chắc chắn, đo được**: APK nhỏ đi 80%; RAM và native heap giảm. Đây là kết quả của nén nhạc (cách 10) và bitmap 565 (cách 8) cộng bớt đối tượng tạm.
- **Không kết luận được về CPU/FPS**: cùng một bản chạy hai lần cho p50 150 ms rồi 97 ms (0.5.0) và ngược lại (0.5.1); CPU 14 - 27%. Dao động giữa các lượt lớn hơn mọi khác biệt giữa hai bản. Nguyên nhân là GPU ảo của emulator (frame treo hàng giây, jank ~95 - 100% ở cả hai bản). Phải đo lại trên máy thật mới biết các cách 2, 3 có giúp frame hay không. Về lý thuyết đã giảm ~4 đối tượng `RadialGradient`/`DashPathEffect` mỗi hành tinh mỗi frame và nhiều chuỗi tạm.
- **Chi phí mới**: giải mã nhạc trong nền ~2,6 MB PCM mỗi bài (48 kHz, trước là 1,8 MB ở 32 kHz) nên bộ nhớ tạm thời của bài nhạc lớn hơn. Trên emulator việc giải mã mất ~36 s nên nhạc vào chậm; trên máy thật Opus giải mã nhanh hơn thời gian thực nhiều lần, cần xác nhận trên máy thật.
- **Chưa kiểm tra bằng tai**: chất lượng và độ liền mạch khi lặp vòng của nhạc Opus. Cần nghe thử trên máy thật.
- Số CPU 51% trong báo cáo 0.5.0 lấy bằng phương pháp khác (cửa sổ 3 s, thao tác dày hơn), không so trực tiếp với bảng này.

## Việc nên làm tiếp

1. Đo lại cả hai bản trên máy thật (`dumpsys gfxinfo`, Android Studio Profiler).
2. Nghe thử nhạc nền Opus, xác nhận giải mã nhanh trên máy thật.
3. Nếu máy thật vẫn giật: tối ưu `LevelPlanetRenderer` (vẫn tạo gradient mỗi frame) và `WelcomeScene`, hoặc chuyển sang `SurfaceView` (cách 4).
