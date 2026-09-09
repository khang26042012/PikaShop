# PikaShop 1.0.0 - Smoke test sau restart (R13, lenh p-)

> PikaShop dung lenh p-prefix: /pshop /psell /psellmulti /psellhistory /pworth /pshard + /pikashop reload.
> EconomyShopGUI giu /shop /sell cu -> khong trung, test song song duoc.

## 0. Console khoi dong
- [ ] Co dong `PikaShop R9...` / `Nap N category shop` / `Nap N category sell` / `Nap N mon shard shop` / `da noi Vault Economy`
- [ ] Khong stack trace do

## 1. /pshop (mua)
- [ ] `/pshop` mo menu SHOP, thay icon GEAR
- [ ] Click GEAR -> Obsidian $100 -> chon so luong -64/-16/-1/+1/+16/+64 -> XAC NHAN tru tien nhan do
- [ ] Tien khong du bao loi; tui day bao loi + khong mat tien; QUAY LAI/HUY dung

## 2. /psell /pworth /psellhistory
- [ ] `/psell`: tha diamond -> dong menu -> nhan tien + chat bao da ban
- [ ] Tha dat (khong gia) -> tra lai tui; XEM GIA bao uoc tinh; HUY tra do khong ban
- [ ] `/pworth` (tay) / `/pworth all` (tui); `/psellhistory` thay giao dich vua ban

## 3. /psellmulti
- [ ] Thay 6 icon ORES/MOB/BLOCKS/CROPS/FISH/FOOD, he so 1.00x luc dau
- [ ] Ban du 25000$ mot category -> LEVEL UP + he so 1.05x

## 4. /pshard (v2)
- [ ] `/pshard` bao so du 0; admin `/pshard give <ten> 100` -> so du 100
- [ ] `/pshard pay <nguoi2> 10` tru/nguoi nhan dung; tu gui bao loi
- [ ] `/pshard shop` mo shop, mua mon re tru shard; thieu shard bao loi
- [ ] `/pshard top` thay dau player; OP `/pshard setafk` dat diem; `/pshard afk` teleport dem nguoc
- [ ] Giet player (test khu PvP) +shard; giet lap lai <5 phut bi chan farm

## 5. /pikashop reload (OP)
- [ ] Reload khong loi, gia moi an ngay

## 6. Rollback neu hong
- [ ] Xoa `plugins/PikaShop-1.0.0.jar` + restart -> server ve nhu cu (EconomyShopGUI van nguyen)
- [ ] Khong mat data EconomyShopGUI trong suot qua trinh test
