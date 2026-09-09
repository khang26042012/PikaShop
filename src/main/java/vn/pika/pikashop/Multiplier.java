package vn.pika.pikashop;

import java.util.List;
import java.util.UUID;

/**
 * Tu viet: multiplier theo category.
 * level = so moc level-prices da vuot (dua tren tong spent).
 * he so = base-multiplier + level * level-bonus-per-level.
 */
public class Multiplier {

    private final PikaShop plugin;

    public Multiplier(PikaShop plugin) {
        this.plugin = plugin;
    }

    public List<Double> levels() {
        List<Double> out = plugin.getConfig().getDoubleList("level-prices");
        if (out.isEmpty()) out = java.util.Arrays.asList(25000.0, 150000.0, 500000.0, 1000000.0, 5000000.0, 25000000.0);
        return out;
    }

    public double bonusPerLevel() {
        return plugin.getConfig().getDouble("level-bonus-per-level", 0.05);
    }

    public int level(UUID uuid, String category) {
        double spent = plugin.store().getSpent(uuid, category);
        int lv = 0;
        for (double mark : levels()) {
            if (spent >= mark) lv++;
            else break;
        }
        return lv;
    }

    /** He so ap dung cho category cua player (R9: chua co cheat, chi tu spent). */
    public double factor(UUID uuid, SellData.Category c) {
        if (c == null) return 1.0;
        if (!plugin.getConfig().getBoolean("sell-multiplier.enabled", true)) return c.baseMultiplier;
        return c.baseMultiplier + level(uuid, c.id) * bonusPerLevel();
    }

    /** Moc ke tiep can dat, -1 neu da max. */
    public double nextMark(UUID uuid, String category) {
        double spent = plugin.store().getSpent(uuid, category);
        for (double mark : levels()) {
            if (spent < mark) return mark;
        }
        return -1;
    }

    /** Thanh tien trinh 20 o: # da dat, - chua dat. */
    public String bar(UUID uuid, String category) {
        double spent = plugin.store().getSpent(uuid, category);
        double next = nextMark(uuid, category);
        List<Double> marks = levels();
        int lv = level(uuid, category);
        double prev = lv == 0 ? 0 : marks.get(lv - 1);
        double target = next < 0 ? prev : next;
        double pct = target <= 0 ? 1.0 : Math.min(1.0, Math.max(0.0, (spent - prev) / (target - prev)));
        if (next < 0) pct = 1.0;
        int filled = (int) Math.round(pct * 20);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) sb.append(i < filled ? '#' : '-');
        return sb.toString();
    }

    public int percent(UUID uuid, String category) {
        double spent = plugin.store().getSpent(uuid, category);
        double next = nextMark(uuid, category);
        if (next < 0) return 100;
        List<Double> marks = levels();
        int lv = level(uuid, category);
        double prev = lv == 0 ? 0 : marks.get(lv - 1);
        if (next <= prev) return 100;
        return (int) Math.round(100.0 * (spent - prev) / (next - prev));
    }
}
