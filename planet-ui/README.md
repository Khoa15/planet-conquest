# planet-ui

Dựng lại hành tinh của Planet Conquest bằng HTML + CSS + JS thuần (Canvas 2D), lấy số liệu trực tiếp từ
`WorldRenderer.drawPlanet/drawOrbit`, `Engine.orbitDots/orbitRings`, `Faction`, `StarField`.

Chạy: `python3 -m http.server` trong thư mục này rồi mở `http://localhost:8000` (hoặc mở thẳng `index.html`).

Bảng bên phải chỉnh phe, đá, giáp, cấp, tiến độ nâng cấp, trạng thái bị chiếm, sương mù.

Hiệu ứng đã dựng: nháy sáng khi chiếm/lên cấp/thiên thạch, vòng nạp đạn, vòng tiến độ nâng cấp, sương mù và lộ diện 3 giây,
đá bay kèm vệt đuôi, hạt vỡ khi trúng, chữ bay (Chiếm được / Bị chiếm / -dmg / Cấp x), thiên thạch,
đá vàng khi được chọn, vòng chọn, vòng đích kèm nhãn, vòng gợi ý đầu ván, vòng tầm bay, tự sinh đá.

## levels.html: màn chọn màn (lộ trình hành tinh)

Mở `http://localhost:8000/levels.html`. Bản đồ cuộn dọc, Hướng dẫn ở dưới, Endless ở trên cùng.

- Mỗi màn một hành tinh riêng, màu không trùng nhau (`LEVELS` trong `levels.js`): mầm lục, đại dương lam, kho vành đai cam, sa mạc vàng nứt, đỏ sọc tốc độ, tím vòng tầm ngắn, xám lam sương mù, nâu bãi thiên thạch, hồng vòng nạp đạn, lục lam xung tăng tốc. Endless là hố đen có đĩa bồi tụ.
- Chưa qua: hành tinh tối (`shade`, `DARK_AMOUNT`), không quầng sáng. Qua màn: sáng dần trong `LIT_SECONDS`, có quầng sáng. Màn kế tiếp có vòng vàng nét đứt.
- Đường nối (`buildPath`): nội suy giữa hai nút cộng lệch ngang bằng hai sóng sin, bao `sin(pi t)` để khớp hai đầu; nét đứt mờ khi chưa mở, sáng chuyển màu hai đầu kèm hạt chạy khi màn trước đã qua.
- Bảng bên phải: qua màn kế, qua tất cả, đặt lại, kỷ lục Endless, tạm dừng. Chạm hành tinh để xem hạn chế.
