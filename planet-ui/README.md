# planet-ui

Dựng lại hành tinh của Planet Conquest bằng HTML + CSS + JS thuần (Canvas 2D), lấy số liệu trực tiếp từ
`WorldRenderer.drawPlanet/drawOrbit`, `Engine.orbitDots/orbitRings`, `Faction`, `StarField`.

Chạy: `python3 -m http.server` trong thư mục này rồi mở `http://localhost:8000` (hoặc mở thẳng `index.html`).

Bảng bên phải chỉnh phe, đá, giáp, cấp, tiến độ nâng cấp, trạng thái bị chiếm, sương mù.

Hiệu ứng đã dựng: nháy sáng khi chiếm/lên cấp/thiên thạch, vòng nạp đạn, vòng tiến độ nâng cấp, sương mù và lộ diện 3 giây,
đá bay kèm vệt đuôi, hạt vỡ khi trúng, chữ bay (Chiếm được / Bị chiếm / -dmg / Cấp x), thiên thạch,
đá vàng khi được chọn, vòng chọn, vòng đích kèm nhãn, vòng gợi ý đầu ván, vòng tầm bay, tự sinh đá.
