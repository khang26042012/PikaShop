# PikaShop — SPEC plugin shop riêng của PikaMC (tự viết, clean-room)

> Nguồn hiểu biết: giải mã DonutShop 1.5.6 (eTYcGc1j) các vòng 1–5 trong `DonutShop_decode/NOTES.md`.
> Nguyên tắc: CHỈ học ý tưởng/cấu trúc/config schema. KHÔNG copy code, KHÔNG copy y nguyên giá/message, KHÔNG bypass license của bản gốc.
> Mục tiêu: thay EconomyShopGUI + DonutShop gốc, không phụ thuộc license server ngoài, dễ quản lý, cân bằng theo economy PikaMC.

## 1. Scope MVP (v1.0)
- [x] SHOP mua 2 bước: menu chính → category → chọn số lượng (-64/-16/-1/+1/+16/+64) → CONFIRM/CANCEL
- [x] SELL thả-đồ: GUI mở bằng /sell, đóng inventory = bán hết đồ hợp lệ, tiền qua Vault
- [x] Multiplier theo category: base-multiplier + level-prices (mốc tổng $ đã bán), GUI tiến trình WORKING/INCOMPLETE/COMPLETE
- [x] Lệnh: /shop /sell /sellmulti /sellhistory /worth (+ admin /pikashop reload, /sellprice)
- [x] SHARDS (v2, R13): số dư data.yml, giết player base 15 + bonus perm (cooldown farm 300s/cặp) + AFK zone, /pshard balance/pay/shop/top/afk (+admin set/give/take/see/setafk)
- [x] Không có license, không gọi mạng ra ngoài, không bStats (code đã rà: không import java.net/http)

## 2. Lệnh & quyền
| Lệnh | Quyền | Mô tả |
|---|---|---|
| /shop | pikashop.use (true) | Mở menu chính |
| /sell | pikashop.use | Mở GUI thả đồ bán |
| /sellmulti | pikashop.use | Xem tiến trình multiplier |
| /sellhistory [page] | pikashop.use | Lịch sử bán |
| /worth [hand/all] | pikashop.use | Xem giá trị đồ |
| /pikashop reload | pikashop.admin (op) | Reload config |
| /sellprice <giá> | pikashop.admin | Đặt giá đồ đang cầm |
| /shard ... | pikashop.use | (v2) |

## 3. Schema SQLite (file plugins/PikaShop/data.db)
```sql
CREATE TABLE IF NOT EXISTS sell_levels(uuid TEXT NOT NULL, category TEXT NOT NULL, level INTEGER DEFAULT 0, progress REAL DEFAULT 0, PRIMARY KEY(uuid, category));
CREATE TABLE IF NOT EXISTS sell_spent(uuid TEXT NOT NULL, category TEXT NOT NULL, spent REAL DEFAULT 0, PRIMARY KEY(uuid, category)); -- UPSERT ON CONFLICT
CREATE TABLE IF NOT EXISTS sell_history(id INTEGER PRIMARY KEY AUTOINCREMENT, uuid TEXT NOT NULL, item TEXT NOT NULL, amount INTEGER NOT NULL, total REAL NOT NULL, time INTEGER DEFAULT (strftime('%s','now')));
-- v2: CREATE TABLE IF NOT EXISTS shard_balances(uuid TEXT PRIMARY KEY, balance INTEGER DEFAULT 0);
```

## 4. Cấu trúc config (tự thiết kế, học ý tưởng DonutShop)
```
config.yml                  # messages chung, level-prices (mốc multiplier), prefix
shop/
  main.yml                  # menu chính: Title/Size/categories(slot, icon, file)
  categories/
    gear.yml food.yml nether.yml end.yml   # items: {slot, material, name, price, max-stack}
sell/
  config.yml                # sell-multiplier.enabled, level-prices[20], menu text, progress-items
  multiplier/
    ores.yml mob.yml ...    # base-multiplier, inventory-slot, progress-title, allowed-types {price-per-unit}
```

## 5. Công thức multiplier (học từ DonutShop, tự cân số)
- Mỗi category: `base` (vd 1.0) + `level-prices[]` 20 mốc (mặc định copy ý tưởng thang 25k→640B, SẼ cân lại theo economy server trước khi live).
- `spent[uuid][cat]` = tổng $ đã bán ở category. `level` = số mốc đã vượt. `payout = unit_price × amount × (base + level_bonus)`.
- GUI: WORKING (vàng, đang cày mốc hiện tại) / COMPLETE (xanh, max) + progress-bar `%current-spent%/%needing-spent%`.
- Placeholder nội bộ: %multiplier% %progress% %progress-bar%.

## 6. Giá trị (data) — TỰ CÂN, không copy
- Dùng `shop_backup_full/all_items.txt` (1596 items EconomyShopGUI hiện tại) làm baseline giá server.
- Quy tắc chống bug kiểu wolf_armor (buy 26712/sell 8904): mọi giá sell mới phải qua review: sell ≤ buy/3, item farmable hàng loạt (armor, iron...) giá trần thấp.
- Không copy nguyên bảng giá ores.yml của DonutShop (netherite block 250k...) — chỉ tham khảo thứ tự độ hiếm.

## 7. Shards v2 (khi MVP ổn)
- Kiếm: giết player base 15 + bonus permission (+10/+20), cooldown 300s cùng cặp (chống xoay acc); AFK zone (setafk, radius 10, 10 shard/phút, countdown 5s hủy khi động).
- Shop shard: item = chạy lệnh console (khớp server: crates/spawner...), trừ trước refund sau, hết slot rơi đất.
- Leaderboard đầu player phân trang + PAPI nếu có.

## 8. Kỹ thuật
- Fork mẫu build từ PikaOrbit: Maven + JDK21 + GitHub Actions (push main → build → release jar). paper-api provided.
- Không shade lib lạ. SQLite qua JDBC có sẵn của server (hoặc sqlite-jdbc shade khi cần).
- Vault API cho tiền. Không gọi HTTP ra ngoài.
- Tương thích Geyser/Bedrock: chỉ dùng ItemStack vanilla, tên/lore có màu cơ bản, GUI ≤54 slot.

## 9. Lộ trình
- R6 (xong): SPEC + scaffold biên dịch được.
- R7: Shop 2-bước đọc config + mua Vault.
- R8: Sell thả-đồ + history + worth.
- R9: Multiplier + GUI tiến trình.
- R10: Cân giá từ all_items.txt + test checklist + build Actions ra jar (xong, 1.0.0).
- R11: Stage jar lên server + SMOKE_TEST (xong).
- R12: Đổi lệnh p-prefix (pshop/psell/.../pshard) chống trùng EconomyShopGUI (xong, build-4).
- R13: Shards v2 (xong, build-5).
- R14: Đồng bộ SMOKE_TEST theo lệnh p- + shard v2 (docs, xong).
- R15: /psellprice admin + onDisable lưu shard + reload nạp shard shop (xong, build-6, SPEC đủ 100%).
- R16: Audit bảo mật tiền/đồ — check Vault response, lỗi hoàn tiền/trả đồ (xong, build-7).
- Sau event thứ 7 mới thay EconomyShopGUI (hiện shop cũ đang KHÓA). Chờ restart để smoke test thật.
