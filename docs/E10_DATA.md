# E10 — první rozšíření obsahu

24. 10. 2026 · aplikace 0.10.0-e10, kód 10 · distribuční obsah generace 4.

## Rozsah první dávky

E10 je podle plánu průběžná práce po oblastech. Tato dávka navazuje na Durotar službami v Orgrimmaru a výběrem severních Barrens: Far Watch Post, Crossroads, Ratchet a příprava na Wailing Caverns. Přidává také přípravu Ragefire Chasm a pět milníků Warlockovy knihy kouzel. Neuzavírá M2/M3 ani celou cestu 1–60. Mapy vnitřků dungeonů, úplné vybavení a úplný seznam úkolů chybí.

| Záznam | Přibalená generace 2 / kontrolní 3 | Rozšířená generace 4 |
|---|---:|---:|
| Položky | 53 | 139 |
| NPC | 28 | 61 |
| Úkoly | 19 | 57 |
| Předměty | 4 | 11 |
| Kouzla | 1 | 6 |
| Dopravní reference | 1 | 4 |
| Přímé polohy | 27 | 62 |
| Mapy / PNG dlaždice | 4 / 68 | 5 / 85 |
| Vztahy / zdroje / pravidla průvodce | 49 / 59 / 8 | 106 / 140 / 36 |
| Ověřené dopravní hrany | 0 | 0 |

Přímé polohy: 27 Durotar, 24 Barrens (22 NPC + 2 letové body), 11 Orgrimmar (10 NPC + 1 letový bod). Úkoly nemají vymyšlené vlastní souřadnice. Vazby na zadavatele/cílové NPC a explicitní oblast pravidla slouží k vysvětlení kontextu. Jeden úkol může souviset s více oblastmi. Počet v dialogu Pokrytí proto není počet questových špendlíků.

## Původ a nejistoty

Fakta jsou uložená v content/e10-additions.json a dohledatelná přes jednotlivé sourceIds; zkrácený audit URL, získaných údajů a SHA-256 vstupů je v research/e10/facts-audit.json. Nejsou kopírované celé cizí questové příběhy nebo kód stránek. Primární komunitní pozorování pocházejí z warcraftforever.games, doplňující klientské a Classic reference z foreverchanges.pro. Ani jeden není nezávislé herní ověření vydané verze.

- Mapový obrázek Barrens: beta 1.60.1.69893, 1002 × 668. Pozorování NPC/úkolů nemá vždy známý build. Předmětové tooltipy uvádějí 1.60.1.70009, kniha kouzel 1.60.1.70532. Tyto verze se neslučují do jediného buildu obsahu.
- Ebru 5768 má zdrojovou polohu jen na kontinentu Kalimdor 1414. Nemá špendlík na Barrens ani automaticky přepočtenou místní polohu. Gest 3327 je Rogue Trainer, proto není zařazen jako Warlockův trenér. Ak’Zeloth má doložené ID 3521.
- Raptor Horns 865 byl zaznamenán v quest logu v Orgrimmaru. To není poloha zadavatele Mebok Mizzyrix 3446: zdroj NPC jej umisťuje do Barrens. Souřadnice pocházejí z NPC stránky, nikoli ze záhlaví záznamu questu.
- 37 nových beta úkolů má minimumLevel=null; udaná úroveň úkolu zůstává oddělenou hodnotou. Jeden nový úkol 5726 je pouze Classic reference. Hidden Enemies 5726, 5727, 5728, 5729, 5730 jsou pět různých záznamů. Čtyři návaznosti řetězce a Smart Drinks po Raptor Horns zůstávají CLASSIC, routingEnabled=false.
- Pět nových spellbook milníků: Summon Voidwalker 10, Drain Soul 10, Create Healthstone 10, Create Soulstone 18, Ritual of Summoning 20. Vlastní stabilní klíče nemají přisouzené odhadnuté Classic spell ID. Udaná úroveň neprokazuje kompletní podmínky nebo quest k odemčení; konkrétní Warlockův řetězec není doplněn odhadem.
- Sedm nových kandidátů na odměny má beta vlastnosti a vazbu na skutečný quest: Sizzle Stick 8071, Staff of Orgrimmar 15444, Kris of Orgrimmar 15443, Grassland Sash 6477, Banshee Armor 5420, Wayfaring Gloves 5337, Barkeeper’s Cloak 5343. Nejde o hotový BiS seznam.
- Devrak, Doras a Bragok jsou tři beta uzly grafu. Nemají doložené hrany; osobní označení jako objevené nevytvoří linku ani jízdní řád.

Všech 53 starších položek, 27 poloh, 49 vazeb a osm pravidel je zachovaných beze změny. Žádný nový ani starší záznam se automaticky neposunul na RELEASE. Nula ověřených doporučení neznamená, že už v oblasti nic nezbývá.

## Oddělené APK a obsah

APK zachovává bajtově stejný záložní katalog generace 2 a všech 68 původních dlaždic. Nová data se importují samostatným podepsaným souborem WoW_Forever_E10_data_4.wfp přes správce E09. Samotná aktualizace APK nepřepne aktivní generaci 2/3 a nesmaže starší generaci potřebnou pro návrat. Balíček je úplný: katalog + 85 dlaždic, 86 datových souborů. Případná chybějící nová dlaždice se nesmí vypůjčit z jiné generace.

Data 4 vyžadují minimumAppCode=10; E09 je odmítne před importem katalogu. Aplikace E10 nadále přijímá dosavadní generace 2/3. Identita balíčku wow-forever:starter:durotar je historický stabilní klíč, nikoliv aktuální seznam oblastí. Schéma katalogu a manifestu zůstává 1. MapRecord získává volitelný scopeCs; obsah a počty v Pokrytí se berou z aktivní generace.

Podporované souřadnicové rámce schvaluje app/src/main/assets/content/supported-maps.json. Přidán je přesně uiMapId 1413, Povrch, vlastní coordinateVersion a doložený převod. Nejde o area ID ani libovolně přijímanou mapu. Změna geometrie nebo převodu je odmítnutá i s jinak platným podpisem.

17 nových dlaždic vzniká doplněním průhledného okraje před změnou měřítka stejně jako u starých map. Nejvyšší úroveň rekonstruuje zdrojový obrázek pixelově přesně. Pět NPC záznamů kontroluje převod zdrojových souřadnic; nejsou to nezávislé kontrolní body změřené ve hře.

Osobní databáze zůstává verze 2, herní databáze verze 3, záloha verze 1; exportované Room schéma se nemění. Záloha E10 připouští uloženou oblast 1413 i při aktivní starší generaci. Záloha postavy z Barrens vyžaduje pro obnovu E10; E09 ji ještě nepodporuje. Při návratu z 4 na 2/3 se původní oblast, poznámka, hotové úkoly, výprava a vybrané odměny nemažou. Chybějící mapa nevytvoří falešný špendlík. Formulář oblasti upozorní na chybějící rámec a bez výslovného výběru dostupné oblasti neuloží změnu.

## Reprodukce dat

```sh
python3 -m pip install -r tools/requirements.txt
python3 tools/content_schema.py
python3 tools/build_e10_content.py
python3 tools/prepare_e10_map.py
python3 tools/verify_e10_data.py
python3 tools/verify_assets.py
python3 tools/build_package.py --catalog content/e10-v4.json --output ../WoW_Forever_E10_data_4.wfp --manifest-prefix content/e10-v4.manifest --signing-key /cesta/k/soukromemu/content-publisher.pem
python3 tools/validate_package.py ../WoW_Forever_E10_data_4.wfp
```

Sestavení distribučního podpisu potřebuje odděleně uchovaný vlastnický klíč E09. Public manifest a podpis jsou součástí zdrojů pro nezávislou kontrolu. Hostitelské integrační testy používají skutečný veřejný vydavatelský pin, manifest a dlaždice; nevyžadují soukromý klíč. Starší generace 3 má samostatnou veřejnou testovací sadu. Základní tools/build_content.py nadále sestavuje původní záložní katalog, nikoli novou distribuci.
