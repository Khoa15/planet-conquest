package com.planetconquest.game.screen;

import android.graphics.Canvas;

import com.planetconquest.game.audio.MusicTrack;
import com.planetconquest.game.ui.UiButton;

import java.util.List;

/**
 * Một màn hình của game (State pattern). GameView chỉ giữ màn đang hiện và chuyển lời gọi cho nó:
 * thêm màn mới chỉ cần thêm một lớp, không phải sửa các switch trong GameView.
 */
public interface Screen {
    /** Khi trở thành màn hiện tại. */
    void enter();
    /** Dựng lại bố cục và danh sách nút theo kích thước màn hình hiện tại. */
    void layout();
    /** Cập nhật mỗi khung hình (chỉ khi needsLoop). */
    void update(float dt);
    void draw(Canvas c);
    List<UiButton> buttons();

    // Cảm ứng ngoài các nút
    void onDown(float x, float y);
    void onMove(float x, float y);
    void onUp(float x, float y);
    void onCancel();

    /** Nút Back của hệ thống. Trả về false để thoát app. */
    boolean onBack();
    /** Ứng dụng bị đưa xuống nền. */
    void onHostPause();

    /** Có cần vòng lặp khung hình liên tục (hoạt ảnh, mô phỏng) hay chỉ vẽ khi có sự kiện. */
    boolean needsLoop();
    MusicTrack musicTrack();
    boolean duckMusic();
    /** Đồng hồ cho hoạt ảnh nền sao nhấp nháy. */
    float animTime();
    /** Tung độ của thông báo nổi. */
    float toastTop();
}
