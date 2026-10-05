# planet-ui

Dựng lại hành tinh của Planet Conquest bằng HTML + CSS + JS thuần (Canvas 2D), lấy số liệu trực tiếp từ
`WorldRenderer.drawPlanet/drawOrbit`, `Engine.orbitDots/orbitRings`, `Faction`, `StarField`.

Chạy: `python3 -m http.server` trong thư mục này rồi mở `http://localhost:8000` (hoặc mở thẳng `index.html`).

Bảng bên phải chỉnh phe, đá, giáp, cấp, tiến độ nâng cấp, trạng thái bị chiếm, sương mù.

Hiệu ứng đã dựng: nháy sáng khi chiếm/lên cấp/thiên thạch, vòng nạp đạn, vòng tiến độ nâng cấp, sương mù và lộ diện 3 giây,
đá bay kèm vệt đuôi, hạt vỡ khi trúng, chữ bay (Chiếm được / Bị chiếm / -dmg / Cấp x), thiên thạch,
đá vàng khi được chọn, vòng chọn, vòng đích kèm nhãn, vòng gợi ý đầu ván, vòng tầm bay, tự sinh đá.

Quỹ đạo đá quanh hành tinh (`drawOrbit` / `orbitDotPos`, hằng `ORBIT_*`): mỗi viên bay hỗn loạn với tốc độ, chiều quay, dải bán kính, dao động hướng tâm và nhiễu góc riêng (hash theo chỉ số viên), không còn vòng đều.

## levels.html: màn chọn màn (lộ trình hành tinh)

Mở `http://localhost:8000/levels.html`. Bản đồ cuộn dọc, Hướng dẫn ở dưới, Endless ở trên cùng.

- Mỗi màn một hành tinh riêng, màu không trùng nhau (`LEVELS` trong `levels.js`): mầm lục, đại dương lam, kho vành đai cam, sa mạc vàng nứt, đỏ sọc tốc độ, tím vòng tầm ngắn, xám lam sương mù, nâu bãi thiên thạch, hồng vòng nạp đạn, lục lam xung tăng tốc, xanh ngọc Viễn chinh (hành tinh nhỏ, đoàn đá đi chậm trên đường chấm tới một hành tinh xa, `voyage`). Endless là hố đen có đĩa bồi tụ.
- Chưa qua: hành tinh tối (`shade`, `DARK_AMOUNT`), không quầng sáng. Qua màn: sáng dần trong `LIT_SECONDS`, có quầng sáng. Màn kế tiếp có vòng vàng nét đứt.
- Đường nối (`buildPath`): nội suy giữa hai nút cộng lệch ngang bằng hai sóng sin, bao `sin(pi t)` để khớp hai đầu; nét đứt mờ khi chưa mở, sáng chuyển màu hai đầu kèm hạt chạy khi màn trước đã qua.
- Bảng bên phải: qua màn kế, qua tất cả, đặt lại, kỷ lục Endless, tạm dừng. Chạm hành tinh để xem hạn chế.

## welcome.html: màn Welcome (bản hiện tại)

Mở `http://localhost:8000/welcome.html`. Dựng 1-1 màn Welcome đang chạy trên Android để làm nền chốt thiết kế nút:

- `WelcomeScene.java` → `Scene` trong `welcome.js` (cùng tên hàm/hằng: `drawPlanet`, `drawLaunch`, `drawBlackHole`, `drawTornPlanet`, `drawFragments`, `drawBelt`, `drawForeground`, camera `cam`/`zoom`/`sway`).
- `StarField.java` → `StarField`; `WelcomeScreen.draw` + `DrawKit.drawButton` → tiêu đề, chân trang, nút Chơi ngay / Bản đồ / Âm thanh.
- Bố cục ngẫu nhiên dùng `JRandom` (cài lại `java.util.Random`, hạt giống 7 và 11) nên vị trí thiên thạch, đĩa bồi tụ, vết nứt trùng hệt Android.
- Bảng điều khiển: kích thước màn hình, kéo thời gian cảnh, tốc độ, tạm dừng, nhảy tới lúc phóng đá.
- Khác biệt nhỏ còn lại: font (Android dùng Roboto, trình duyệt dùng font thay thế) và cách nội suy gradient về màu trong suốt.

### Hướng 3 trong welcome.html (chọn ở "Kiểu nút")

Hành tinh xanh của người chơi trong cảnh nền là nút **Chơi ngay**, la bàn là nút **Bản đồ**; "Bản hiện tại" vẫn chọn lại được để đối chiếu.

- Hành tinh xanh (`Scene.playPlanet()` trả tâm/bán kính đã tính camera, để chạm trúng dù cảnh đang parallax): biểu tượng play đúng tâm hành tinh, không có chữ: lõi kính xanh ngọc đậm (`PLAY_DISC_R`), tam giác trắng chuyển sang xanh nhạt có quầng sáng xanh ngọc (`PLAY_ICON_H`), lõi có nhịp thở `PLAY_PULSE` (không có dòng màn tiếp theo; chạm sẽ vào `Hướng dẫn` / màn kế / `Endless`). Vòng vàng nét đứt nhấp nháy ngoài vòng đá ngoài cùng (`HINT_RING_GAP`).
- Nhấn giữ: hành tinh nhỏ lại `PLAY_PRESS_SCALE`, sáng thêm `PLAY_PRESS_GLOW`. Thả trong vùng nút: phồng `TAP_POP`, chớp trắng, phóng `BURST_ROCKS` viên đá có vệt đuôi trong `TAP_SECONDS`.
- La bàn (không có nhãn chữ; dòng "Kỷ lục Endless" cũng bỏ ở hướng này): mặt kính, 12 vạch, ba hành tinh nhỏ, kim vàng lắc nhẹ; nhấn giữ nhỏ lại `COMPASS_PRESS_SCALE`; thả thì kim quay `SPIN_TURNS` vòng trong `SPIN_SECONDS` kèm quầng vàng. La bàn `COMPASS_R` = 34dp.
- Bố cục nút phụ (chọn ở "Bố cục nút phụ"): **bar** (mặc định) = âm thanh góc dưới trái và la bàn góc dưới phải cùng một hàng, cách mép `BAR_MARGIN`, tâm cách đáy `BAR_Y_FROM_BOTTOM`; **center** = la bàn giữa dưới, âm thanh góc trên phải. Màu: xanh ngọc = hành động chính (hành tinh, âm thanh đang bật), vàng = đường đi (vòng gợi ý, la bàn), xám = trạng thái tắt.
- Nút **Ngôn ngữ** (góc trên phải, pill `LANG_W`×`LANG_H` = 58×34dp, quả cầu và chữ cùng tâm dọc, cụm canh giữa nút theo mã rộng nhất trong `LANGS` (chữ canh trái, quả cầu cố định) nên không nhảy khi đổi VI/EN, cách mép `LANG_MARGIN`/`LANG_TOP`): quả cầu + mã `VI`/`EN`, viền xanh ngọc; bấm là đổi ngay giữa `LANGS` và lưu lại. Chữ trên màn (chân trang "Phiên bản"/"Version") đổi theo. Chọn ngôn ngữ ở ô "Ngôn ngữ" để đối chiếu.
- Nút Âm thanh là icon vuông 44dp (kiểu ICON như nút Back), hai trạng thái: loa có sóng (bật) và loa có dấu X (tắt); bấm để đổi.

- Khu vực dưới màn hình (trống sau khi bỏ nút chữ nhật) có thêm bốn hành tinh nhỏ trong `PL` (chỉ số 5-8; hồng, vàng, be, chàm), chừa góc hai nút phụ. Khi chuyển sang native thêm đúng các dòng này vào `WelcomeScene.PL` và cập nhật ghi chú "chừa khoảng 25% dưới cùng".
- Hành tinh đỏ và vàng: đá quay hỗn loạn (`MESSY_*`, đỏ 8 viên, vàng 6 viên; mỗi viên bán kính/tốc độ/chiều/độ lệch tâm riêng); hành tinh xanh giữ nguyên 3 vòng đều. Thêm vành đai thiên thạch phía dưới bay từ phải sang trái (`LOW_BELT`).
- Đã bỏ vòng vàng gợi ý quanh hành tinh xanh. Hành tinh xanh cũng có đá quay hỗn loạn (`MESSY_COUNT_PLAYER` = 36 viên); loạt phóng lấy đá số `i * LAUNCH_DOT_STEP` từ chính vị trí hỗn loạn lúc phóng.

## voyage.html: màn 10 · Viễn chinh (bản chơi thử)

Mở `http://localhost:8000/voyage.html`. Dùng lại đúng bộ vẽ của `planet.js` (`drawBody`, `drawOrbit`, nền sao, đá có vệt đuôi, vòng chọn/đích, `pill`), thêm mô phỏng tối thiểu và kéo-thả từ hành tinh của bạn sang hành tinh khác.

- Hằng số luật đặt đầu file, trùng tên bản native sẽ làm: `ROCK_SPEED_SCALE` (SlowRocksRule), `PLANET_GAP` (Level.gap; màn KHÔNG giới hạn tầm bay), `BODY_SCALE` (thu nhỏ hành tinh, quỹ đạo, đá, chữ; hành tinh nhỏ chỉ hiện số đá ở giữa), `THIN_GUARD`.
- Cue mới: vòng đỏ nhấp nháy quanh hành tinh của bạn khi số đá dưới `THIN_GUARD` ("hở sườn").
- Thanh trượt: tỉ lệ đá gửi, tốc độ đá. AI bắt đầu đánh sau `AI_GRACE` giây.
- HUD (thanh trên, chỉ icon, ô vuông 44dp kiểu `ICON`): **Back** (trái, `HUD_MARGIN`), **Âm thanh** (phải, cạnh Tạm dừng; bật = xanh ngọc, tắt = xám có dấu X, bấm để đổi) và **Tạm dừng** (phải cùng). Hằng `HUD_BTN`, `HUD_TOP`, `HUD_GAP`; `BACK_ACTION` ghi hành vi nút Back đang chờ chốt.

## speed.html: nút tốc độ khi chơi

Mở `http://localhost:8000/speed.html`. Nút `x1 / x1.25 / x1.5 / x2` nằm bên trái nút Tạm dừng (rộng 52dp, cách 8dp, cùng chiều cao). Chạm để xoay vòng; từ x1.25 trở lên chữ và viền chuyển vàng, nút phồng `POP_MS`. Tốc độ nhân vào `dt` của toàn bộ mô phỏng: quỹ đạo, sinh đá, đá bay, AI tấn công, đồng hồ giới hạn thời gian (3 hành tinh minh hoạ). Hằng: `SPEEDS`, `SPEED_BTN_*`, `COLOR_*`.
