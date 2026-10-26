# Rozložení pro Lenovo IdeaTab 11 5G

Pracovní verze **0.20.0-tablet / kód 21**, obsah 8. Jenda označil jako svůj tablet dříve probírané Lenovo IdeaTab 11 5G 8/256 GB. Výchozí parametry rodiny: 11″, 2560 × 1600, poměr 16:10 ([Lenovo PSREF](https://psref.lenovo.com/Product/Lenovo/Idea_Tab)). Přesná aktuální verze Androidu, hustota zobrazení a nastavení písma fyzického kusu nebyly načtené.

## Změny

- Ve velkém okně je vedle mapy trvale seznam bodů. Otevřený detail jej vystřídá, mapa zůstává přístupná.
- Při hledání a v úkolech se detail otevírá vedle původní obrazovky. Výsledky hledání označují vybranou položku a umožňují rovnou vybrat další.
- Nastavení a seznamy mají omezenou šířku pro pohodlné čtení.
- Užší okno, výška nebo velké písmo používají jednu plochu a původní detail. Výběr položky a uložitelný stav detailu přecházejí mezi rozloženími.
- Rozhoduje dostupná šířka okna po odečtení systémových okrajů a navigace, výška a velikost písma. Není zde výjimka podle značky ani požadavek na fyzické rozlišení.

## Ověření

[Android checks](https://github.com/JendaNDT/WoW-Forever-Companion/actions/runs/40390993795) prošly: **248 aplikačních testů v 65 sadách**, **39 Python testů**, žádná chyba ani vynechaný test. Lint: debug 0 chyb / 17 dosavadních varování, release 0 / 18. Ověřených je všech 93 přibalených souborů, obsahové balíčky zůstávají uložené pro streamování.

[Tabletový Android 15](https://github.com/JendaNDT/WoW-Forever-Companion/actions/runs/40390993822) úspěšně dokončil **3 nativní scénáře** při 2560 × 1600 a 320 dpi (1280 × 800 dp). Hostitelské testy navíc ověřily přechod do užšího okna, obnovení otevřeného a sbaleného detailu, uloženou pozici seznamu a 200% písmo. Snímky z emulátoru byly vizuálně zkontrolované. Nejde o měření výkonu fyzického Lenova.

[Nativní spolehlivost Androidu 8 a 16](https://github.com/JendaNDT/WoW-Forever-Companion/actions/runs/40390993758) prošla: na každém systému **5 testů**, dva vynucené konce procesu, obnova postupu a nejméně pět minut střídání všech 61 map / 17 330 bodů. API 26: 915 změn / 15 úplných cyklů / 316 201 ms; API 36: 793 změn / 13 cyklů / 305 001 ms. Nejvýše dvě ponechané mapové scény. Nejde o měření FPS nebo paměti fyzického Lenova. Skutečný stav vypnutých rádií nebyl samostatně ověřený.

## Instalační soubor

- Soubor: `Warlock_Forever_0.20.0_Tablet.apk`, **150553650 bajtů**.
- Zdroj: `19669d4c3543da6ced5f1324b83bfe62bbb85da0`, čisté produkční vstupy, **157 souborů**.
- SHA-256 APK: `f5abab4d6924d2653397ba193370cc8f2e4b824c20089025047b58887f67415a`.
- SHA-256 produkčních vstupů: `396333465b27f8937e537bda1d4166e2c3fe1dff1115b401f78b6a27d0b621dd`.
- Certifikát: `b9cda9f873ca045180313b52bc86abcd8b9df93da44b3b5be219056799bec452`.

Jde o vývojově podepsané APK s jiným certifikátem než Fáze 3. Před případnou čistou instalací existující aplikace je potřeba export postupu. Místní instalační nástroj se při rozdílu podpisu zastaví; aplikaci ani data sám nemaže. Aktuální APK nebylo cloudovým agentem instalované na fyzický tablet.

## Přímý místní vývoj

[Český postup pro USB](VYVOJ_TABLET_USB.md) a `tools/tablet_dev.py` připravují ověření zařízení, instalaci, spuštění, logy, snímky, čekání na debugger a nativní tabletové scénáře. Čtyři testy nástroje (součást 39 Python testů) používají náhradu příkazů; nepotvrzují skutečné USB spojení. Pro dokončení propojení otevři repozitář na větvi `codex/lenovo-tablet` v místním Codexu na Macu a spusť `python3 tools/tablet_dev.py doctor`.

Připojení fyzického tabletu k uživatelovu Macu a zapnuté USB ladění byly uživatelem oznámené. Pracovní cloudové prostředí nemá zpřístupněné USB zařízení; přímá instalace, měření výkonu, pero a výsledky na fyzickém Lenovu se tím nepotvrzují.

Herní obsah, zálohy, databáze a formát dat se nemění. Herní ověření zůstává až po vydání hry. Základní přijetí předchozí verze 20 se na tuto verzi automaticky nepřenáší.
