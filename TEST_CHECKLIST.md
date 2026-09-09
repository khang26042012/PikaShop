# PikaShop TEST CHECKLIST (R10) - chay sau khi cai jar len server test
# Dieu kien: Vault + Economy (EssentialsX) dang chay, OP de test.

## 1. Khoi dong
- [ ] Console co "PikaShop R9..." + "Nap N category shop" + "Nap N category sell, M muc gia" + "da noi Vault Economy"
- [ ] Khong bao "Bo vat lieu la" nao (neu co -> sua ten material trong multiplier yml)

## 2. /shop (mua)
- [ ] /shop mo menu SHOP, thay icon GEAR
- [ ] Click GEAR -> menu SHOP - GEAR, thay Obsidian $100
- [ ] Click Obsidian -> man hinh MUA, nut -64/-16/-1/+1/+16/+64 doi so luong
- [ ] XAC NHAN tru tien + nhan do; tien khong du bao loi; tui day bao loi + khong mat tien
- [ ] Nut QUAY LAI ve menu chinh; HUY ve category

## 3. /sell (ban)
- [ ] /sell mo GUI BAN DO 54 o; tha diamond vao, dong menu -> nhan tien + chat bao da ban
- [ ] Tha dat (dirt - khong gia) vao, dong menu -> dat tra lai tui
- [ ] Nut XEM GIA bao uoc tinh; nut HUY tra do, khong ban
- [ ] /worth (cam diamond) bao gia/cai; /worth all bao tong tui
- [ ] /sellhistory thay 10 GD gan nhat

## 4. /sellmulti
- [ ] Mo GUI thay 6 icon ORES/MOB/BLOCKS/CROPS/FISH/FOOD, he so 1.00x luc dau
- [ ] Ban 25000$ ores -> chat LEVEL UP, he so len 1.05x, payout tang
- [ ] /pikashop reload nap lai gia ma khong restart

## 5. Chong bug (bat buoc truoc live)
- [ ] Gia sell farmable da tran: scute/armadillo 5, iron/gold 5, string/bone 2, leather 8
- [ ] Khong muc nao sell > buy/3 so voi PRICE_BASELINE.json
- [ ] Test tren acc thuong (khong OP): khong mo duoc /pikashop
