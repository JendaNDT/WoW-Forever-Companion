# E10 celek — referenční katalog Horda / Warlock 1–60

Rozsah sjednaný pro společné dokončení E10: jedna nativní aplikace s automaticky přibaleným obsahem pro cestu 1–60, rozšířený atlas, dohledatelné zdroje, třídní návaznosti, cíle úkolů, předměty, služby a příprava na instance. Jenda potvrdil bezproblémový základní test předchozí E10.3 a požádal o jednu společnou přejímku. Další drobné E10 dávky se nevyžadují.

## Obsah dat 7

- 12 352 záznamů: 6 039 NPC, 2 818 úkolů, 2 769 předmětů, 678 objektů, 14 dosavadních kouzel a 34 dopravních/přípravných položek.
- 104 úkolů s třídním zařazením Warlocka podle zdroje, včetně návazností pro vyšší úrovně a Dreadsteed of Xoroth. Nejde o 104 nových schopností.
- 18 441 explicitních referenčních poloh, 10 135 vazeb, 12 411 zdrojových záznamů a 2 848 pravidel průvodce. Původních 216 položek, 84 poloh a jejich zdroje, pravidla a vazby zůstávají přesně zachované.
- 61 offline map / 1 103 PNG dlaždic. Pět nových instančních map je pouze obrazový přehled bez souřadnicového převodu NPC. Jejich interní identifikátory 900000+ID nejsou vydávány za Blizzard uiMapId. Čtyři nové povrchové oblasti: Riverglades, Mount Hyjal, Shen’dralas a Zephras Isle.
- 30 přípravných záznamů pro Classic/Forever dungeony a raidy se zdrojovými vstupy a souvisejícími úkoly. Half-Pint Tavern má přehledovou mapu, bez vymyšleného vstupu.
- Filtry katalogu: Warlock, dungeony, předměty, oblast postavy, pásma 1–10 až 51–60. Pásma používají udanou úroveň, případně minimální; neznámá úroveň není odhadována. Vyhledávatelný výběr map nahrazuje dlouhou řadu oblastí.

## Reprodukovatelný původ

Questie/QuestieDB je připnutý na `5c094f6e9a264f0dc54e2fde9731a37a97ed197c` z 24. 10. 2026. `research/e10full/upstream/snapshot.json` obsahuje 29 přesných souborů, SHA-256 a Git blob identity porovnané s připnutým stromem repozitáře. Použitý je převedený Era základ, generovaný Forever rozdíl a autorské statické Forever opravy. Samotný základ by neobsahoval nový Forever obsah. Bezpečný parser čte pouze literály; nespouští cizí Lua ani pluginy. Pravidla operací add/remove zachovávají celé souřadnicové dvojice. Služby používají specifické Forever bity (např. letový mistr 8 a hostinský 128), nikoli Era bity.

Dostupnost je filtrována podle rasových a třídních masek pro podporované profily Hordy (Ork, Troll, Nemrtvý), úrovně 1–60 a třídního zařazení. Je to referenční výběr, ne důkaz, že konkrétní server právě nabízí každý úkol. Původní popisy příběhů se nekopírují; české popisy vznikají ze strukturovaných faktů a původních vlastních jmen. U odměn zdroj nerozlišuje pevnou odměnu a výběr; nová vazba QUEST_REWARD nikdy nezapíná výběr REWARD_CHOICE.

Obrazové podklady pocházejí z warcraftforever.games / Warcraft Recorder, build 1.60.1.69893. Originály, URL a kontrolní součty jsou v `research/e10full/maps/inventory.json`. Nejvyšší dlaždice všech 56 nových map lze složit do obrazu shodného pixel po pixelu s originálem; všech 85 starších dlaždic zůstává beze změny. Padding se provádí před zmenšením, aby odpovídaly souřadnice.

## Známé mezery a význam dokončení E10

Implementační a obsahová dodávka E10 je uzavřená jako referenční průvodce 1–60. Formální herní M2/M3 tím nejsou automaticky potvrzené. Nula RELEASE pozorování, nula aktivních ověřených návazností a nula ověřených dopravních hran.

Nespouštějí se zděděné starší opravy, generované trasovací vrstvy ani dynamické Lua podmínky Questie. Úplné podmínky reputace, profese, fází, sezón a skriptovaných událostí mohou chybět. Automatické doporučení je proto stále označené „K ověření“. Import obsahuje 79 chybějících odkazovaných řádků; audit zaznamenává 201 skupin s chybějícím cílem, z toho 81 celých předpokladových skupin je vynechaných. Neúplný ALL/ANY předpoklad se nikdy nezkracuje na zdánlivě úplnou podmínku. Zdrojový popis a původní data zůstávají dohledatelné.

U běžných nepřátel a objektů je nejvýše šest skutečných zdrojových poloh na mapu, u zadavatelů všechny doložené polohy. Nejde o úplný spawn atlas ani trasu pohybu. Neznámá AreaID a vnitřní patra se nepřekládají odhadem; přesný seznam je v `import-audit.json`. Transformace již převedeného základu se neaplikuje podruhé. Podklad mapy a komunitní souřadnice samy nepotvrzují shodu s aktuálním klientem.

Nezpracovaný zůstává úplný soupis tajemství, dropů, optimalizovaná XP trasa, kompletní výbava/BiS, všechny profese a herní důkaz všech pravidel. Prázdná kontrola odchodu nepotvrzuje dokončení zóny. U Vile Familiars 1485 zůstává starší rozpor. Classic body mají C; explicitně schválené komunitní reference mají ?. Ani jeden typ nevytváří ověřenou cestu.

## Technické změny a obnova

Aplikace 0.15.0-e10-full / versionCode 15, nativní Kotlin / Compose / Room. Datové schéma zůstává 1, Room databáze 3 a záloha 1. Nové typy objektů/vazeb, rámy a reference vyžadují aplikaci 15; podepsaný manifest brání načtení dat starší aplikací. Katalog je omezen na 32 MiB, archiv stále na 128 MiB a rozbalený obsah na 256 MiB. ZIP cesty, podpisy, kontrolní součty a PNG rozměry se nadále ověřují.

Vyhodnocování používá indexy vazeb; velký katalog počítá doporučení mimo hlavní vlákno. Starší výpočty se ruší a neuplatní opožděný výsledek. Cache oblastí je při souběhu chráněná. Mapový renderer zachovává shlukování a nejvýše dvě aktivní mapy. Všechny mapy nejsou načtené do bitmapové paměti současně.

Aktualizace ze stávajících dat 6 má zachovat oba profily, úkoly, předměty, poznámky a zálohy. Nová oblast smí zůstat v profilu i při návratu ke starším datům; neznámý podklad se nepřesouvá. Návrat balíčku nepřepisuje osobní historii a přežije restart. Obnovení přibaleného obsahu vrátí data 7. Nové zálohy s objekty či dalšími oblastmi jsou určené pro aplikaci 15 nebo novější. Zálohy předchozích verzí zůstávají čitelné.

`bundled-data-6.wfp` je zachovaný publikovaný referenční balíček pro regresní testy; produkční start používá výhradně data 7. Nouzová data 2 a jejich dlaždice se nemění. Podpisový vlastník ani soukromé klíče se nemění.

## Obnovení ze zdrojů

Zdrojový ZIP obsahuje přesný podepsaný datový balíček, výzkumné vstupy, licence a úplnou historii. Build tedy nevyžaduje stahování herních dat. Při změně dat: `python tools/build_e10_full.py`, `python tools/prepare_e10_full_maps.py --output /cesta/tiles`, následně `build_package.py` s `--tile-root` a vlastním původním podpisovým klíčem. Soukromé klíče nejsou v archivu. Kontrola: `python tools/verify_bundled.py` a `python -m unittest discover -s tools -p 'test_*.py'`.
