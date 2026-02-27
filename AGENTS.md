# tfcmu2: 金属・鉱石アイテム形状一覧（派生元Mod別）

このドキュメントでいう「各アイテム」は、`ingot` や `sheet` のような形状（フォーム）を指す。

## 運用ルール

- 仕様追加や実装変更があった場合、この `AGENTS.md` を随時更新する。
- 形状の追加・削除、条件付き有効化の変更、ID規則の変更を優先して反映する。

## 1. 独自金属

### 1.1 金属アイテム形状

対象金属:
`compressed_iron`, `platinum`, `naquadah`, `iridium`, `osmium`, `osmiridium`, `mythril`, `refined_glowstone`, `refined_obsidian`, `antimony`, `titanium`, `tungsten`, `solder`, `tungsten_steel`, `netherite`

常時生成される形状:
- `ingot`
- `double_ingot`
- `sheet`（plate）
- `double_sheet`
- `rod`

ID規則:
- `tfcmu2:metal/ingot/<metal>`
- `tfcmu2:metal/double_ingot/<metal>`
- `tfcmu2:metal/sheet/<metal>`
- `tfcmu2:metal/double_sheet/<metal>`
- `tfcmu2:metal/rod/<metal>`

`tfc_items` 導入時のみ生成される追加形状:
- `foil`
- `gear`
- `heavy_sheet`
- `nail`
- `ring`
- `rivet`
- `screw`
- `stamen`
- `wire`

ID規則:
- `tfcmu2:metal/<form>/<metal>`

特殊:
- `tfcmu2:metal/ingot/high_carbon_tungsten_steel`

保管・建材形状:
- `block`
- `block_slab`
- `block_stairs`

ID規則:
- `tfcmu2:metal/block/<metal>`
- `tfcmu2:metal/block/<metal>_slab`
- `tfcmu2:metal/block/<metal>_stairs`

### 1.2 鉱石アイテム形状

対象鉱石:
- 品位あり: `native_platinum`, `native_naquadah`, `native_iridium`, `native_osmium`, `rutile`, `stibnite`, `wolframite`
- 品位なし: `fluorite`

鉱石アイテム形状:
- 品位あり鉱石: `poor`, `normal`, `rich`
- 品位なし鉱石: 基本形のみ

ID規則:
- `tfcmu2:ore/{poor|normal|rich}_<ore>`（品位あり）
- `tfcmu2:ore/<ore>`（品位なし）

鉱石ブロック形状:
- TFC岩石内: `/<tfc_rock>`
- バニラ石材内: `/netherrack`, `/endstone`

ID規則:
- `tfcmu2:ore/{poor|normal|rich}_<ore>/<rock_or_stone>`
- `tfcmu2:ore/<ore>/<rock_or_stone>`

地表サンプル形状:
- 品位あり鉱石: `small_<ore>`（ブロックアイテムあり）
- `small_fluorite`: groundcoverブロックのみ（ブロックアイテムなし）

ID規則:
- `tfcmu2:ore/small_<ore>`

`tfcorewashing` 導入時のみ（品位あり鉱石対象）:
- `pellet`
- `briquet`
- `chunks`
- `rocky_chunks`
- `dirty_dust`
- `dirty_pile`
- `powder`

ID規則:
- `tfcmu2:metal/<form>/<ore>`

## 2. `tfc` 由来（compat展開）

`Tfcmu2CompatOres.TFC_ORES` の鉱石に対して、以下の形状を追加する。

追加される形状:
- `netherrack` 内鉱石ブロック
- `endstone` 内鉱石ブロック

ID規則:
- `tfcmu2:ore/<ore_name>/netherrack`
- `tfcmu2:ore/<ore_name>/endstone`

補足:
- 一部の非品位鉱石ピースは `small_<ore_piece_name>` の groundcoverブロックを追加（ブロックアイテムなし）。
- このcompat層では `ingot` や `sheet` などの金属形状は追加しない。

## 3. `firmalife` 由来（compat展開）

対象:
`normal_chromite`, `poor_chromite`, `rich_chromite`

追加される形状:
- `netherrack` 内鉱石ブロック
- `endstone` 内鉱石ブロック

ID規則:
- `tfcmu2:ore/<ore_name>/netherrack`
- `tfcmu2:ore/<ore_name>/endstone`

補足:
- このcompat層では金属形状は追加しない。

## 4. `tfc_ie_addon` 由来（compat展開）

対象:
`normal_bauxite`, `poor_bauxite`, `rich_bauxite`
`normal_galena`, `poor_galena`, `rich_galena`
`normal_uraninite`, `poor_uraninite`, `rich_uraninite`

追加される形状:
- `netherrack` 内鉱石ブロック
- `endstone` 内鉱石ブロック

ID規則:
- `tfcmu2:ore/<ore_name>/netherrack`
- `tfcmu2:ore/<ore_name>/endstone`

補足:
- このcompat層では金属形状は追加しない。

## 5. 鉱石アイテム有無（重要）

- 単体の鉱石アイテム（`tfcmu2:ore/{poor|normal|rich}_<ore>`, `tfcmu2:ore/<ore>`）を持つのは `tfcmu2` 独自鉱石のみ。
- `tfc` / `firmalife` / `tfc_ie_addon` 由来は、このMod側では主に `netherrack` / `endstone` 向け鉱石ブロック展開。

## 6. 有効化条件

- `tfcmu2` 独自の金属・鉱石形状: 常時有効
- `firmalife` compat鉱石形状: `firmalife` ロード時のみ
- `tfc_ie_addon` compat鉱石形状: `tfc_ie_addon` ロード時のみ
- 追加金属形状（`foil`, `wire` など）: `tfc_items` ロード時のみ
- 鉱石洗浄形状: `tfcorewashing` ロード時のみ

## 7. 参照コード

- `src/main/java/net/claustra01/tfcmu2/Tfcmu2Metal.java`
- `src/main/java/net/claustra01/tfcmu2/Tfcmu2Ore.java`
- `src/main/java/net/claustra01/tfcmu2/Tfcmu2Items.java`
- `src/main/java/net/claustra01/tfcmu2/Tfcmu2Blocks.java`
- `src/main/java/net/claustra01/tfcmu2/Tfcmu2CompatOres.java`
- `src/main/java/net/claustra01/tfcmu2/Tfcmu2Mod.java`
