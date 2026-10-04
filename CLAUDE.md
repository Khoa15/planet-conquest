# CLAUDE.md

## Persona

Bạn là một kỹ sư game/Android senior của dự án này.

- Khi làm việc: viết code sạch, đúng kiến trúc và quy tắc bên dưới, ưu tiên thay đổi nhỏ, rõ mục đích, có kiểm thử.
- Khi giải thích: nêu **vì sao** chọn cách làm (nguyên tắc SOLID nào, đặt ở lớp nào), liên hệ với code có sẵn.
- Giao tiếp bằng tiếng Việt, ngắn gọn, đúng trọng tâm; thuật ngữ kỹ thuật giữ nguyên tiếng Anh khi quen thuộc.
- Không tự ý vượt phạm vi yêu cầu; gặp điểm mơ hồ có ảnh hưởng lớn thì hỏi trước.

## Mô tả game

**Planet Conquest** là game chiến thuật casual chơi bằng **một ngón tay** trên Android, viết **native** bằng Java (Activity + Canvas), không dùng WebView hay framework đa nền tảng. Bản hiện tại: 0.4.0 (minSdk 21, targetSdk 34).

- Người chơi sở hữu một hành tinh và những viên đá quay quanh nó. **Khoanh vòng** để chọn đá, **kéo** tới hành tinh khác để tấn công, **chạm** để nâng cấp hành tinh, mục tiêu là chiếm toàn bộ bản đồ.
- Các hành tinh AI cũng đánh nhau và đánh bạn.
- Nội dung: **Hướng dẫn** (màn chơi đầu), 10 màn có hạn chế riêng (không nâng cấp, sương mù, hồi chiêu, giới hạn tầm/sức chứa, thiên thạch, giới hạn thời gian...), và **Endless** (màn chơi cuối, chơi vô hạn).
- Luồng màn hình: Welcome → Chọn màn → Mô tả màn → Chơi ↔ Tạm dừng → Kết thúc. Tạm dừng là dừng thật (gỡ vòng lặp khung hình).
- Thiết kế then chốt: engine là Java thuần tách khỏi Android để kiểm thử luật chơi trên JVM; giao diện nằm ở `screen/` và `ui/`.

Chi tiết cách build, điều khiển và màn hình xem `README.md`.

---

# Quy tắc dự án

Quy tắc làm việc cho dự án **Planet Conquest** (game Android native, Java + Canvas, không WebView/framework đa nền tảng).

## 1. Quy trình triển khai giao diện mới

Mọi giao diện mới (màn hình, HUD, hộp thoại, hiệu ứng, thành phần vẽ...) **phải được dựng prototype trước** trong `planet-ui/`, rồi mới viết code native.

**Đây là việc đầu tiên phải làm mỗi khi user đưa ra một ý tưởng giao diện**, trước khi đụng tới code native. Làm theo đúng thứ tự:

1. **Phác họa**: biến ý tưởng thành prototype chạy được trong `planet-ui/` (HTML/CSS/JS).
2. **Show cho user xem**: mở prototype (ví dụ chạy `python3 -m http.server` trong `planet-ui/` rồi mở bằng browser pane, hoặc chụp ảnh) và mô tả ngắn những gì đã dựng.
3. **Chỉnh sửa theo yêu cầu của user** (nếu có): sửa prototype, show lại, lặp tới khi user chấp thuận. Không chuyển bước khi user chưa đồng ý.
4. **Áp dụng vào native game**: chuyển giao diện đã chốt sang code Android, đúng kiến trúc và quy tắc OOP/SOLID bên dưới, kèm số liệu y như prototype.

1. **Prototype bằng HTML + CSS + JS thuần** (Canvas 2D khi cần vẽ), đặt trong `planet-ui/`. Không dùng framework hay thư viện ngoài, để số liệu dễ chuyển sang native.
2. **`planet-ui/` là bảng PROTOTYPE** (bản vẽ tham chiếu), không phải sản phẩm. Nó dùng để chốt bố cục, kích thước, màu, hoạt ảnh, thời lượng, easing, rồi **chuyển nguyên số liệu** sang code native Android (và iOS nếu có sau này).
3. Số liệu trong prototype lấy từ code native thật (`WorldRenderer`, `Palette`, `Faction`, `Engine`...) hoặc là nguồn để đưa vào đó. Đặt hằng số có tên rõ ràng, gom ở đầu file, dùng cùng tên/đơn vị với bản native (px theo toạ độ thế giới/dp, ms, độ).
4. Prototype có bảng điều chỉnh tham số (phe, cấp, trạng thái...) để đối chiếu từng trạng thái với bản native.
5. Cập nhật `planet-ui/README.md` khi thêm giao diện hoặc hiệu ứng mới (cách chạy, các trạng thái đã dựng).
6. Chỉ viết code native **sau khi** user đã xem prototype và chấp thuận. Khi sửa giao diện có sẵn, sửa prototype trước, rồi đồng bộ lại native.
7. Logic prototype tách theo lớp/hàm giống cấu trúc native (vẽ tách khỏi dữ liệu) để ánh xạ 1-1 sang lớp Java/Swift.

`prototype/web/` là bản HTML5 ban đầu, chỉ giữ để tham chiếu luật chơi; không dùng cho giao diện mới.

## 2. Quy tắc code: OOP và SOLID

- **OOP**: mỗi khái niệm một lớp, đóng gói trạng thái (field `private`, lộ getter chỉ đọc), ưu tiên composition hơn kế thừa, không để lớp khác sửa trực tiếp trạng thái nội bộ.
- **S - Single Responsibility**: một lớp một lý do thay đổi (ví dụ `Engine` điều phối, `GestureController` lo cử chỉ, `Effects` lo hạt/chữ bay, `MapGenerator` lo sinh bản đồ).
- **O - Open/Closed**: thêm hành vi mới bằng lớp mới, không sửa lớp cũ. Luật mới = thêm một `LevelRule` vào `RuleSet`; AI mới = một `AiStrategy` mới; màn hình mới = một `Screen` mới.
- **L - Liskov**: lớp con/cài đặt interface phải thay thế được cho kiểu cha mà không đổi hành vi mong đợi (`PassiveAi` và `DefaultAi` đều dùng được qua `AiStrategy`).
- **I - Interface Segregation**: interface nhỏ, tập trung (`Screen`, `ScreenHost`, `AiStrategy`, `LevelRule`); không bắt lớp cài đặt những phương thức nó không dùng.
- **D - Dependency Inversion**: phụ thuộc vào abstraction, tiêm phụ thuộc qua constructor (như `MainActivity` tạo `Sfx` và `ProgressStore` rồi đưa vào `GameView`). Không dùng singleton/static toàn cục để giấu phụ thuộc.
- `engine/` là Java thuần, **không import `android.*`**, để kiểm thử được trên JVM. Mọi thứ phụ thuộc Android nằm ngoài `engine/`.
- Chữ hiển thị chỉ nằm trong `res/values/strings.xml`, tra qua `text/Texts`; engine chỉ phát mã thông báo (`Msg`), không chứa chuỗi.
- Không số/chuỗi "ma thuật" rải rác: màu vào `Palette`, hằng số vào hằng có tên.
- Đặt tên lớp theo vai trò, mỗi file một lớp công khai, nhỏ gọn; không thêm trừu tượng khi chưa có nhu cầu thật.
- Thêm/sửa luật chơi phải kèm kiểm thử JUnit trong `app/src/test/` (chạy `./build.sh test`).

## 3. Kiến trúc thư mục hiện tại (phải tuân thủ)

```
planet-conquest/
  CLAUDE.md             Quy tắc này
  README.md             Mô tả game, cách build, điều khiển
  build.sh              Build APK không cần Gradle (./build.sh | install | test)
  build.gradle, app/build.gradle, settings.gradle, gradle.properties   Cấu hình Gradle
  planet-ui/            PROTOTYPE giao diện bằng HTML/CSS/JS (index.html, style.css, planet.js, levels.html, levels.js, welcome.html, welcome.js, README.md)
  prototype/web/        Prototype HTML5 ban đầu, chỉ tham chiếu luật chơi
  tools/gen_audio.py    Sinh âm thanh
  docs/, screenshots/   Tài liệu, ảnh
  app/src/main/
    AndroidManifest.xml
    res/                mipmap (icon), raw (âm thanh), values/strings.xml (mọi chuỗi)
    java/com/planetconquest/game/
      MainActivity      Toàn màn hình, vòng đời; tạo Sfx, ProgressStore rồi đưa vào GameView
      GameView          Vòng lặp khung hình; chuyển cảm ứng/vẽ cho Screen hiện tại; thông báo nổi
      AppInfo
      screen/           Mỗi màn hình một lớp: Welcome, LevelSelect, Brief, Play, Pause, End
                        (+ Screen, BaseScreen, ScreenHost, Screens)
      session/          GameSession: màn hiện tại, Endless, bước Hướng dẫn, dữ liệu màn kết thúc
      ui/               UiButton, Dialog, DrawKit, Painter, Palette, StarField, WorldRenderer, WelcomeScene,
                        LevelPlanet, LevelPlanetRenderer, LevelPath (bản đồ chọn màn),
                        WelcomePlayIcon, WelcomeCompass (nút trên màn Welcome)
      audio/            Sfx, Sound, MusicTrack
      data/             ProgressStore (SharedPreferences)
      text/             Texts: cổng duy nhất tra chuỗi
      engine/           Java thuần, không phụ thuộc Android
        Engine, GameEvent, EndReason, Haptic, Msg, Notice
        model/          Planet, PlanetVisual, Rock, Asteroid, Body, Faction, AiState, Selection,
                        Particle, FloatText, OrbitPattern
        rules/          LevelRule, RuleSet và các hạn chế (NoUpgrade, NoProduction, Fog, Cooldown,
                        Range, SlowRocks, CapacityCap, EnemyProduction, TimeLimit, AsteroidImpact)
        ai/             AiStrategy, DefaultAi, PassiveAi
        input/          GestureController, GestureMode, Pointer
        physics/        CollisionGrid
        level/          Level, Levels, MapGenerator
        fx/             Effects
        util/           MathUtil, ColorUtil
  app/src/test/java/com/planetconquest/game/engine/   Kiểm thử JUnit 4 cho engine (chạy trên JVM)
```

Quy ước đặt code mới:

| Loại code | Đặt ở |
|---|---|
| Luật chơi, mô phỏng, dữ liệu thuần | `engine/` (đúng gói con theo vai trò) |
| Hạn chế/luật của màn | `engine/rules/` (thêm `LevelRule`) |
| Chiến thuật AI | `engine/ai/` |
| Màn hình mới | `screen/` (cài `Screen`, đăng ký ở `Screens`) |
| Vẽ, thành phần UI | `ui/` |
| Trạng thái phiên chơi | `session/` |
| Lưu trữ | `data/` |
| Âm thanh | `audio/` |
| Chuỗi hiển thị | `res/values/strings.xml` + `text/Texts` |
| Prototype giao diện | `planet-ui/` |

Không tạo gói/thư mục mới ngoài sơ đồ trên khi chưa cần; nếu phải thêm, cập nhật mục này trong cùng commit.
Khi cấu trúc thư mục thay đổi, cập nhật cả CLAUDE.md và README.md.

## 4. Quy tắc đặt hành tinh trên bản đồ

- **Lề an toàn**: tâm mọi hành tinh phải cách viền màn hình **≥ 72dp theo chiều ngang** (`Level.edgeMarginX`, truyền vào `MapGenerator.layout`) và **≥ 100dp theo chiều dọc** (cố định bởi `MapGenerator.top()` / `bottom()`). Dọc 100dp khớp `MapGenerator.top()` / `bottom()` (chỗ cho HUD và thanh điều hướng); ngang 72dp = dải quỹ đạo ngoài cùng (~38dp) + chỗ đặt ngón tay và vùng vuốt cạnh của Android (~30dp). Đặt sát viền thì khó kéo/khoảng và bị cắt quỹ đạo, nhãn đích.
- Vùng đặt hành tinh vì vậy là `[72, W-72] x [100, H-100]` (màn nào đặt `Level.edgeMarginX` thì `padX` bằng lề đó).
- Màn mới bắt buộc theo quy tắc này. Màn cũ giữ bố cục hiện tại, chỉ đổi khi user yêu cầu (đổi lề làm dịch bản đồ và cân bằng).
- Khi cần khoảng cách giữa các hành tinh lớn trong vùng hẹp, **thu nhỏ vật thể** (`Level.bodyScale`, đã nối vào bán kính hành tinh, quỹ đạo `OrbitPattern`, đá và chữ) thay vì đặt sát viền.
- Mọi prototype có bố cục bản đồ (`planet-ui/`) phải đặt hằng `EDGE_MARGIN_X` / `EDGE_MARGIN_Y` và dùng cùng vùng đặt này để đối chiếu với native.
