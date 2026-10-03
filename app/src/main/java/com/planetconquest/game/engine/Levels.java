package com.planetconquest.game.engine;

import java.util.Random;

/** Danh sách màn: [0] Hướng dẫn, [1..9] chiến dịch, cộng chế độ Endless sinh ngẫu nhiên. */
public final class Levels {
    private Levels() {}

    public static final Level[] ALL = build();
    public static final int CAMPAIGN_LAST = ALL.length - 1;

    private static Level make(String name, int planets, long seed, int playerN, int eMin, int eMax, int neutrals, String limit, String tip) {
        Level L = new Level();
        L.name = name; L.planets = planets; L.seed = seed; L.playerN = playerN;
        L.enemyMin = eMin; L.enemyMax = eMax; L.neutrals = neutrals; L.limit = limit; L.tip = tip;
        return L;
    }

    private static Level[] build() {
        Level intro = make("Hướng dẫn", 2, 7, 60, 20, 20, 2,
                "Không có hạn chế. Đối thủ đứng yên để bạn tập.",
                "Làm theo từng bước hiện trên màn hình.");
        intro.intro = true; intro.passiveAi = true; intro.enemyArmor = 30; intro.enemyProd = .3f;
        intro.fixed = new float[][]{{.5f, .9f, .09f}, {.5f, .28f, .075f}};

        Level l1 = make("Làm quen", 3, 1101, 30, 16, 20, 4, "Không thể nâng cấp hành tinh.",
                "Không có nâng cấp nên tốc độ là tất cả: chiếm hành tinh yếu trước để có thêm nguồn sinh đá, rồi mới đánh hành tinh mạnh.");
        l1.noUpgrade = true; l1.aiGrace = 14;

        Level l2 = make("Kho nhỏ", 4, 2203, 28, 18, 24, 5, "Mỗi hành tinh chỉ chứa tối đa 30 đá, dù cấp nào.",
                "Đá đầy là phí. Đừng tích trữ: liên tục gửi quân đi đánh, hoặc nạp đá nâng cấp để dùng hết đá dư.");
        l2.capCap = 30;

        Level l3 = make("Mùa khô", 4, 3307, 75, 16, 22, 4, "Hành tinh không sinh thêm đá. Số đá là có hạn.",
                "Mỗi viên đá đều quý. Đánh hành tinh yếu nhất trước, tránh đối đầu trực diện và để các đối thủ tự tiêu hao lẫn nhau.");
        l3.noProduction = true; l3.aiGrace = 8;

        Level l4 = make("Chạy đua", 5, 4409, 36, 16, 20, 5, "Chỉ có 100 giây để chiếm hết hành tinh.",
                "Khoanh vòng gom đá từ nhiều hành tinh để đánh dồn một mục tiêu. Đừng chờ đủ quân mới đánh.");
        l4.timeLimit = 100; l4.aiGrace = 20;

        Level l5 = make("Tay ngắn", 6, 5521, 32, 18, 24, 5, "Đá chỉ bay được một quãng ngắn (vòng nét đứt quanh hành tinh).",
                "Không đánh xa được. Chiếm hành tinh gần nhất làm bàn đạp rồi đánh tiếp từ đó.");
        l5.range = .62f;

        Level l6 = make("Sương mù", 7, 6613, 30, 18, 26, 5, "Không thấy số đá và máu của đối thủ.",
                "Gửi một đợt nhỏ để thăm dò: hành tinh bị đánh sẽ lộ số đá và máu trong 3 giây. Biết rồi mới dồn quân đánh thật.");
        l6.fog = true;

        Level l7 = make("Bãi thiên thạch", 8, 7717, 34, 20, 26, 22, "Thiên thạch dày đặc chặn đường: đá của bạn đụng vào sẽ vỡ.",
                "Chọn đường bay thông thoáng, gửi dư quân để bù đá vỡ, hoặc điều đá ra chỗ trống chờ thiên thạch trôi qua.");
        l7.neutralSize = 1.25f;

        Level l8 = make("Nạp đạn", 9, 8821, 34, 20, 28, 6, "Mỗi hành tinh chỉ gửi quân được một lần mỗi 4 giây.",
                "Khoanh vòng để gửi đồng loạt từ nhiều hành tinh trong một lần, và chọn đúng lúc để mỗi lần gửi đủ mạnh.");
        l8.cooldown = 4;

        Level l9 = make("Đối thủ tăng tốc", 10, 9931, 38, 16, 22, 6, "Mọi đối thủ sinh đá nhanh gấp đôi bạn.",
                "Đánh sớm trước khi họ tích lũy. Hành tinh bạn chiếm được sinh đá bình thường cho bạn, nên hạ từng đối thủ một.");
        l9.enemyProd = 2;

        Level[] all = {intro, l1, l2, l3, l4, l5, l6, l7, l8, l9};
        for (int i = 1; i < all.length; i++) all[i].boldShift = -0.02f * (i - 1);
        return all;
    }

    /** Endless: bản đồ ngẫu nhiên 3-10 hành tinh, không hạn chế, thiên thạch đâm vào hành tinh. */
    public static Level endless(int map, Random r) {
        Level L = new Level();
        L.endless = true; L.name = "Endless";
        L.planets = 3 + r.nextInt(8);
        L.seed = r.nextLong();
        int d = Math.min(map - 1, 8);
        L.playerN = 30; L.enemyMin = 18 + d; L.enemyMax = 26 + d;
        L.neutrals = 7 + L.planets / 2; L.aiGrace = 12;
        L.nrMin = .014f; L.nrMax = .05f; L.nsMin = .05f; L.nsMax = .11f;
        L.boldShift = -0.015f * (map - 1);
        L.asteroidHits = true;
        L.limit = "Không có hạn chế nào: nâng cấp, sinh đá, tầm bay đều bình thường.";
        L.tip = "Thiên thạch trôi nổi đâm vào hành tinh và trừ giáp rồi đến đá canh gác: càng to càng mất nhiều máu.";
        return L;
    }
}
