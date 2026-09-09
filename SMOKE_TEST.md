# PikaShop 1.0.0 - Smoke test sau restart

> Chay ngay sau khi restart server (PikaShop + EconomyShopGUI dang song song).
> Chu y: ca 2 plugin deu dang ky /shop /sell -> CAN test xung dot lenh (muc 5).

## 0. Console khoi dong
- [ ] Co dong `PikaShop R9...` / `Nap N category shop` / `Nap N category sell` / `da noi Vault Economy`
- [ ] Khong stack trace do

## 1. /shop (PikaShop)
- [ ] `/shop` mo menu SHOP (neu ra menu EconomyShopGUI -> xem muc 5)
- [ ] Click GEAR -> Obsidian $100 -> chon so luong -> XAC NHAN tru tien nhan do

## 2. /sell /worth /sellhistory
- [ ] `/sell`: tha diamond -> dong menu -> nhan tien
- [ ] `/worth` tay dang cam; `/worth all` tong tui
- [ ] `/sellhistory` thay giao dich vua ban

## 3. /sellmulti
- [ ] Thay 6 icon ORES/MOB/BLOCKS/CROPS/FISH/FOOD, he so 1.00x

## 4. /pikashop reload (OP)
- [ ] Reload khong loi

## 5. XUNG DOT LENH (quan trong)
- [ ] Neu /shop hoac /sell mo nham plugin cu: quyet dinh giu plugin nao, doi lenh plugin kia
  (doi PikaShop: sua plugin.yml commands shop->pshop... hoac tat EconomyShopGUI sau event)

## 6. Rollback neu hong
- [ ] Xoa `plugins/PikaShop-1.0.0.jar` + restart -> server ve nhu cu (EconomyShopGUI van nguyen)
- [ ] Khong mat data EconomyShopGUI trong suot qua trinh test
