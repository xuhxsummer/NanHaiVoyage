# Gemini image prompts — 南海航程

## Shared style block (paste at the start of EVERY prompt)

```
Game asset for a Tang-dynasty South China Sea trading mobile game called NanHaiVoyage.
Art style: refined Chinese historical illustration with warm ink-and-mineral pigments, soft lighting, clear silhouette, readable as a small UI icon or portrait.
NO modern objects, NO firearms, NO English text, NO Chinese characters drawn on the image, NO watermark, NO UI chrome, NO collage, NO photo-real skin pores.
Transparent background if the model supports alpha; otherwise clean solid light parchment (#F3E6C8) background.
```

## Naming when you send files
`avatar_01.png`, `beast_精卫.png`, `herb_人参.png`, `ship_楼船.png`, `port_广州.png`, `island_南澳屿.png`, `good_丝绸.png`

---

## A) Captain / player avatars (square 1024)

Template:
```
[SHARED STYLE]
Square 1024x1024 character portrait bust for a sailing-game avatar.
SUBJECT: {DESCRIPTION}
Tang-era clothing, confident expression, facing slightly three-quarter view, head and shoulders only.
Soft rim light, painterly but sharp edges for UI cropping into a circle.
```

Examples:
1. Young Chinese merchant-captain, indigo robe, hair in topknot, calm smile
2. Veteran female navigator, weather-tanned, hairpin, looking toward horizon
3. Scholar-trader with scroll tube on back, gentle eyes
4. Tough boatswain with sun-scarred cheeks, rope over shoulder

---

## B) 《山海经》异兽 (transparent, 1024, full body)

Beasts in game: 精卫, 九尾狐, 蛊雕, 文鳐鱼, 长右, 狌狌, 白泽, 三足龟

Template:
```
[SHARED STYLE]
Full-body mythical creature icon for a bestiary, centered, three-quarter view, transparent background.
Creature: {NAME} from Classic of Mountains and Seas (Shan Hai Jing).
{MYTH_DETAIL}
Slightly stylized for a trading adventure game, not horror. Soft ground shadow optional. Square 1024x1024.
```

Fill-ins:
- 精卫: small mythical bird with patterned feathers, carrying a twig, determined eyes, flying pose
- 九尾狐: elegant nine-tailed fox, white-gold fur, mystical, graceful standing pose
- 蛊雕: fierce horned eagle-beast hybrid with dark feathers and sharp talons, perched
- 文鳐鱼: flying fish with ornate wing-fins and patterned scales, leaping
- 长右: ape-like Shan Hai Jing beast with elongated arms, alert on a rock
- 狌狌: ape-like intelligent beast, curious posture, soft brown fur
- 白泽: sacred white beast that knows all things, lion-qilin hybrid, luminous white fur, calm majesty
- 三足龟: divine three-legged turtle, mossy shell patterns, serene

---

## C) 草药 (transparent, 768–1024)

Herbs: 人参, 灵芝, 茯苓, 当归, 何首乌, 桂枝, 甘草, 菊花

Template:
```
[SHARED STYLE]
Single medicinal herb product icon, centered, studio-like soft light, transparent background.
Herb: {NAME} ({PINYIN}) as used in classical Chinese medicine.
Show the recognizable plant / root form clearly for inventory UI. No pot, no hand, no text. Square 1024x1024.
```

---

## D) 船只 (side or 3/4 view, transparent, 1536x1024 wide OK)

Ships: 小商船, 楼船, 蒙冲, 漕舫, 斗舰, 走舸, 货舶, 游艇, 海鹘

Template:
```
[SHARED STYLE]
Single Tang / Song-era Chinese sailing junk ship icon for a ship shop UI.
Ship type: {NAME} — {ROLE}.
View: clean three-quarter view from front-starboard, full ship visible, no ocean photo plate (optional soft water line only).
Wooden hull, cloth sails, historically flavored but readable silhouette. Transparent background. Wide 1536x1024 or square 1024.
NO crew faces large, NO modern yacht parts.
```

Roles:
- 小商船: small starter merchant junk, simple sails
- 楼船: tall multi-deck tower ship, imposing
- 蒙冲: covered assault ship, sleeker profile
- 漕舫: wide cargo transport, big hold, stable
- 斗舰: war junk with fighting walls / battlements
- 走舸: fast light pursuit boat
- 货舶: huge ocean freighter, cargo-focused
- 游艇: elegant pleasure craft, lighter and sleek
- 海鹘: elite bird-like warship, sharp prow, most martial

---

## E) 港口图标 (top-down or isometric card, 1024)

Ports: 广州, 潮州, 雷州, 琼州, 崖州, 合浦, 交州, 占城, 真腊, 佛逝, 泉州, 福州, 明州, 钦州, 邕州, 暹罗, 渤泥, 吕宋, 苏禄, 爪哇, 扬州

Template:
```
[SHARED STYLE]
Isometric harbor city icon for a sea-trading map.
Port: {NAME}, Tang maritime trade era.
Show tiled roofs, pier, a few junk masts, coastal hills or tropical cues matching the region.
Centered, readable at map-pin size, soft shadow. Square 1024x1024. No labels.
```

---

## F) 岛屿图标 (top-down, 1024)

Islands: 南澳屿, 琼东岛, 西沙礁, 东沙, 中沙, 永兴, 黄岩, 万山, 担杆, 川山, 涠洲, 海陵, 硇洲

Template:
```
[SHARED STYLE]
Top-down tropical / South China Sea island icon for exploration map.
Island: {NAME}.
Sandy shores, green canopy, reef tips in cyan water, no buildings or tiny hut only. Square 1024x1024, transparent or ocean-free edges. No text.
```

---

## G) 商货 icons (optional, 512–768)

Goods: 丝绸, 瓷器, 茶叶, 盐, 铁器, 米粮, 蔗糖, 沉香, 苏木, 胡椒, 象牙, 珍珠, 玳瑁, 槟榔, 棉布, 漆器, 铜器, 琉璃, 乳香, 没药, 豆蔻, 丁香, 珊瑚, 犀角

Template:
```
[SHARED STYLE]
Single trade-good inventory icon, centered object on transparent background.
Good: {NAME} as a Tang-era traded commodity, neatly arranged bundle or vessel.
Square 768x768, no price tags, no text.
```
