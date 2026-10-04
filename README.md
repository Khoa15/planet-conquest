# Planet Conquest

Game chiến thuật casual chơi một ngón tay trên Android, viết **native** bằng Java (Activity + Canvas),
không dùng WebView hay framework đa nền tảng. Bản hiện tại: **0.5.1**.

Bạn sở hữu một hành tinh và những viên đá quay quanh nó. Khoanh vòng để chọn đá, kéo tới hành tinh khác để
tấn công, nâng cấp hành tinh, và chiếm toàn bộ bản đồ. Các hành tinh AI cũng đánh nhau và đánh bạn.

## Cài nhanh

```bash
adb install -r planet-conquest-0.5.1.apk
```

APK là bản debug (ký bằng debug keystore), minSdk 21 (Android 5.0), targetSdk 34.

## Build

### Cách 1: `build.sh` (không cần Gradle)

Cần JDK 17 và Android SDK có `platforms/android-*` cùng `build-tools` (aapt, dx hoặc d8, zipalign, apksigner).

```bash
./build.sh            # build/planet-conquest-0.5.1.apk
./build.sh install    # build, cài và mở app qua adb
./build.sh test       # chạy kiểm thử engine trên JVM, không cần thiết bị
```

Đặt `ANDROID_HOME`, `PLATFORM_JAR` hoặc `BUILD_TOOLS` nếu SDK không ở `/usr/lib/android-sdk`.
Đây là cách đã dùng để build APK đi kèm.

### Cách 2: Android Studio / Gradle

Mở thư mục gốc bằng Android Studio (AGP 8.5, Gradle 8.7, JDK 17), Studio sẽ tự tạo Gradle wrapper.
Hoặc nếu đã cài Gradle: `gradle wrapper --gradle-version 8.7 && ./gradlew assembleDebug`.
Cấu hình Gradle chưa được chạy thử trong môi trường build APK đi kèm (môi trường đó không truy cập được Google Maven).

## Cấu trúc

```
```
app/src/main/java/com/planetconquest/game/
  MainActivity.java     Toàn màn hình, chuyển vòng đời, tạo Sfx, ProgressStore và LanguageStore rồi đưa vào GameView
  GameView.java         Vòng lặp khung hình, chuyển cảm ứng/vẽ cho Screen đang hiện, thông báo nổi
  screen/               Mỗi màn hình một lớp (State): Welcome, LevelSelect, Brief, Play, Pause, End
  session/GameSession   Phiên chơi: màn hiện tại, Endless, bước Hướng dẫn, dữ liệu màn kết thúc
  ui/                   UiButton, Dialog, DrawKit/Painter, StarField, WorldRenderer, WelcomeScene, Palette,
                        LevelPlanet, LevelPlanetRenderer, LevelPath (bản đồ chọn màn),
                        WelcomePlayIcon, WelcomeCompass (nút trên màn Welcome)
  audio/                Sfx (SoundPool + AudioTrack), Sound, MusicTrack
  data/ProgressStore    Tiến độ lưu trong SharedPreferences
  data/LanguageStore    Ngôn ngữ đã chọn (VI/EN), lưu trong SharedPreferences
  text/Texts            Cổng duy nhất tra chuỗi (res/values/ tiếng Anh mặc định, res/values-vi/ tiếng Việt), đổi ngôn ngữ lúc chạy
  text/Language         Ngôn ngữ hỗ trợ, Java thuần
  engine/               Logic Java thuần, không phụ thuộc Android
    Engine.java         Luật chơi và vòng mô phỏng, điều phối các thành phần dưới đây
    model/              Planet, Rock, Asteroid, Selection, Faction...
    rules/              LevelRule + RuleSet và các hạn chế (NoUpgrade, Fog, Cooldown, ...)
    ai/                 AiStrategy, DefaultAi, PassiveAi
    input/              GestureController (khoanh vòng, kéo thả, chạm), Pointer
    physics/            CollisionGrid     level/  Level, Levels, MapGenerator
    fx/Effects          Hạt vỡ và chữ bay    util/  MathUtil, ColorUtil
app/src/test/java/      Kiểm thử JUnit 4 cho engine, chạy trên JVM (./build.sh test)
prototype/web/          Bản prototype HTML5 ban đầu, giữ làm tham chiếu luật chơi
```

Engine tách khỏi Android nên toàn bộ luật chơi được kiểm thử trên máy tính, còn giao diện nằm ở `screen/` và `ui/`. Mọi chữ hiển thị nằm trong `strings.xml`; engine chỉ phát mã thông báo (`Msg`).

## Màn hình

| Màn hình | Nội dung |
|---|---|
| Welcome | Tiêu đề, hoạt ảnh hành tinh, nút **Chơi** và **Hướng dẫn**, nút đổi ngôn ngữ **VI/EN** góc trên phải |
| Chọn màn | Danh sách Hướng dẫn, 10 màn, Endless; đánh dấu màn đã qua |
| Mô tả màn | Hạn chế của màn và cách vượt qua |
| Chơi | HUD gọn (hành tinh, đá, đồng hồ/bản đồ), nút tạm dừng |
| Tạm dừng | Tiếp tục, Chơi lại, Thoát ra menu |
| Kết thúc | Màn tiếp theo, Chơi lại, Chọn màn |

**Tạm dừng là dừng thật:** vòng lặp khung hình bị gỡ hẳn khỏi `Choreographer`, nên không cập nhật logic,
không đếm giờ, không chạy hoạt ảnh. Game tự tạm dừng khi rời app (Home, cuộc gọi) và khi bấm Back.
Tiếp tục không bị nhảy thời gian.

## Điều khiển (một ngón tay)

| Thao tác | Kết quả |
|---|---|
| Kéo từ hành tinh của bạn tới hành tinh khác | Gửi nửa số đá: tấn công hoặc chuyển quân |
| Kéo từ hành tinh ra chỗ trống | Đá bay tới đó và chờ |
| Khoanh vòng quanh đá | Chọn đá (một phần hoặc toàn bộ, cả đá đang bay/chờ) |
| Khoanh xong kéo tiếp tới đích, hoặc nhấc tay rồi chạm đích | Điều động số đá đã chọn |
| Chạm hành tinh của bạn | Nâng cấp |

Toàn bộ hướng dẫn nằm trong màn **Hướng dẫn** (2 hành tinh, bạn nhiều đá hơn, đối thủ không tấn công),
dẫn qua 5 bước có gợi ý trực quan. Giao diện khi chơi không còn chữ mô tả thừa.

## Luật chính

- Máu hành tinh = số đá + giáp. Mỗi lần lên cấp cộng dồn 10 giáp, tăng tốc sinh đá và sức chứa.
- Đòn tấn công trừ giáp trước, hết giáp mới mất đá. Hết đá thì viên tấn công kế tiếp chiếm hành tinh.
- Hành tinh bị chiếm tụt về cấp 1 và từ đó chỉ lên tối đa cấp 4.
- Đá khác phe va nhau thì cùng vỡ; đá cùng phe đi xuyên qua nhau.

## Màn chơi

| Màn | Hành tinh | Hạn chế |
|---|---|---|
| Hướng dẫn | 2 | Không có; đối thủ đứng yên |
| 1. Làm quen | 3 | Không thể nâng cấp |
| 2. Kho nhỏ | 4 | Mỗi hành tinh chứa tối đa 30 đá |
| 3. Mùa khô | 4 | Không sinh thêm đá |
| 4. Chạy đua | 5 | 100 giây |
| 5. Tay ngắn | 6 | Tầm bay giới hạn |
| 6. Sương mù | 7 | Không thấy đá/máu đối thủ (đánh để lộ 3 giây) |
| 7. Bãi thiên thạch | 8 | 22 thiên thạch to chặn đường |
| 8. Nạp đạn | 9 | Mỗi hành tinh gửi quân 4 giây một lần |
| 9. Đối thủ tăng tốc | 10 | Đối thủ sinh đá gấp đôi |
| 10. Viễn chinh | 5 | Hành tinh cách rất xa (lề ngang 72dp, dọc 100dp), không giới hạn tầm bay, đá bay ×0.25, vật thể thu nhỏ ×0.4; hành tinh còn dưới 8 đá bị cảnh báo "hở sườn" |
| Endless | 4–10 ngẫu nhiên (vị trí hành tinh người chơi cũng ngẫu nhiên) | Không giới hạn tầm bay; hành tinh nhỏ ×0.4, cách xa (0.47 unit, tự hạ khi bản đồ đông), đá bay ×0.25; thiên thạch đâm hành tinh, càng to càng mất máu |

Thông số cân bằng nằm ở đầu `Engine.java`; cấu hình từng màn ở `engine/level/Levels.java`; hạn chế của màn là các `LevelRule` trong `engine/rules/`.

## Chưa có trong MVP

- Chưa chạy thử trên máy thật/giả lập trong môi trường build; logic đã kiểm thử trên JVM.
- Âm thanh, nhạc nền.
- Keystore release để phát hành Google Play (hiện là debug).
- Bố cục ngang, máy tính bảng.
