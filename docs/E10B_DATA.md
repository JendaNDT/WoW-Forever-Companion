# E10 — druhá obsahová dávka, generace 5

25. 10. 2026 · beze změny aplikace E12 / 0.12.0-e12 / kód 12.

## Výsledek a rozsah

Startovní výběr pro Hordu / orka / Warlocka 1–10: Valley of Trials, Sen’jin Village, Kolkar Crag, Echo Isles, Razor Hill a Tiragarde Keep. Dávka přidává 43 položek: 18 úkolů (16 beta pozorování a dvě Classic reference), 12 NPC/nepřátel, pět látkových odměn a osm prvních milníků kouzel. Obsahuje také Nazgrela jako bod odevzdání v Orgrimmaru. Je to rozšíření praktického startovního výběru; úplnost Durotaru ani celé cesty 1–60 se netvrdí.

| Část | Generace 4 | Generace 5 |
|---|---:|---:|
| Položky | 139 | 182 |
| NPC a nepřátelé | 61 | 73 |
| Úkoly | 57 | 75 |
| Předměty | 11 | 16 |
| Kouzla | 6 | 14 |
| Dopravní reference | 4 | 4 |
| Přímé polohy | 62 | 74 |
| Mapy / dlaždice | 5 / 85 | 5 / 85 |
| Vztahy / zdroje / pravidla | 106 / 140 / 36 | 147 / 176 / 67 |
| Ověřené dopravní hrany / RELEASE položky | 0 / 0 | 0 / 0 |

Všech 139 starších položek, původní zdroje, polohy, vztahy a pravidla zůstávají strukturálně shodné. Na mapě Durotaru se mění jen vysvětlující scopeCs. Mapové rozměry, rámce, souřadnicové verze a PNG se nemění. APK, hlavní zdrojové vstupy, podpis aplikace, Room schémata a záloha formátu 1 zůstávají stejné.

## Zdrojování a věcné pojistky

Kurátorský vstup research/e10b/facts.json obsahuje vybraná fakta a vlastní české popisy. Generátor tools/build_e10b_content.py přidá sourceIds, URL, datum kontroly, veřejné payloady, vztahy a bezpečná pravidla. Nepřebírají se celé cizí příběhy ani kód webu. V sources je každý nový úkol, NPC a předmět samostatně dohledatelný; kouzla mají společný explicitní zdroj knihy. Audit popisuje přesně získané údaje, neprovádí neexistující nezávislou kontrolu ve hře.

- Beta úkoly a NPC pocházejí z přímých stránek pozorovatele Warcraft Forever / ForeverLog. Sestavení každého pozorování není známé. Polohy NPC se berou z jejich vlastních stránek, ne ze záhlaví quest logu nebo z Classic mapy. Nové polohy: 11 v Durotaru a Nazgrel v Orgrimmaru.
- For Honor 96822, Legging It 96821, This Fruit Could Bite Back 96825, Bloodtalon Matriarch 97223, Lost in the Shadows 99123 a Forgotten Loa Idols 97225 jsou výběr nových Forever úkolů pozorovaných v betě. Žádné podmínky jejich odemknutí se nevymýšlejí.
- Legging It: start Vel’rin Fang 3194, konec Gar’Thok 3139. Lost in the Shadows: start Pal’juh 275811, konec Vornal 3304. Forgotten Loa Idols: start Vornal 3304, konec Gadrin 3188.
- The Admiral’s Orders jsou dvě samostatná ID 830 a 831. Zdroj ForeverChanges výslovně uvádí Classic základ a řetězec 830 → 831. Obě položky i jejich vztahy zůstávají CLASSIC a routingEnabled=false. ID 830 začíná předmětem Aged Envelope, proto nemá START_NPC. ID 831 končí u Nazgrela, nikoli Orgnila. Aktuální beta poloha Nazgrela neověřuje tento Classic quest.
- Všech 18 nových úkolů má minimumLevel=null; level je jen reportedLevel. Pro Classic reference je původní minimální úroveň zachovaná uvnitř payloadu jako referenceMinimumLevel. Podobně itemLevel není minimumLevel; pět předmětů má skutečný beta materiál Cloth, build 1.60.1.70009. Soft Wool Vest patří k Sarkoth 804, nikoli první části 790. Ripped Pants 6713 se nezaměňují s koženým item 4921.
- Osm nových spellbook záznamů: první ranky Demon Skin / Immolate / Shadow Bolt 1, Corruption / Curse of Weakness 4, Life Tap 6, Bane of Agony / Fear 8. Zdroj ForeverChanges beta klient 1.60.1.70532. Stabilní vlastní klíče book:warlock nemají odhadnutá číselná spell ID. Úroveň v knize sama neprokazuje trenéra nebo celý způsob získání. Stávající milníky od úrovně 10 a Summon Imp zůstávají beze změny.
- Každé nové pravidlo zachovává unknown podmínku a BETA/CLASSIC stav. Nevzniká potvrzená dostupnost, hlášení nevratné volby, odemykání dalších úkolů ani dopravní linka. V Teď pro mě patří příslušné údaje do K ověření.
- Vile Familiars 1485 zůstává původní konflikt. Nové body nepřátel neřeší počet hlav, živé spawny ani vnitřní patra jeskyní.

## Distribuce a reprodukce

Úplný WFP obsahuje katalog a všech 85 dosavadních PNG (86 datových souborů). Má stejnou historickou packageId a původního vydavatele E09, novou generaci 5, stejné schéma 1 a minimumAppCode=10. Funguje přímo přes fallback 2 i jako aktualizace dat 4; import musí uživatel potvrdit. Předchozí generace zůstává dostupná pro návrat. Nové APK není součástí této dávky.

```sh
python3 tools/build_e10b_content.py
python3 tools/verify_e10b_data.py
python3 tools/build_package.py --catalog content/e10-v5.json --output ../WoW_Forever_E10_data_5.wfp --manifest-prefix content/e10-v5.manifest --signing-key /soukroma/cesta/content-publisher.pem
python3 tools/validate_package.py ../WoW_Forever_E10_data_5.wfp
python3 -m unittest discover -s tools -p 'test_*.py' -v
./gradlew --max-workers=1 :app:testDebugUnitTest
```

Podpisový klíč se nebalí do zdrojů. Kotlin testy sestavují skutečný distribuční ZIP z veřejného katalogu, zveřejněného podepsaného manifestu a původních dlaždic; soukromý klíč nepotřebují. Publikovaný WFP má stejné manifestové a datové bajty jako testovaný balíček. Přehled provedených kontrol je v E10B_ZPRAVA.md a qa/e10b.

## Stav telefonní kontroly

Uživatel 25. 10. 2026 uvedl: „Na telefonu už jsem to zkontroloval“. Základní kontrola aplikace E12 je tedy potvrzená; dřívější obecný záznam E05 není aktuální. Výrok sám nepotvrzuje měřená 3 s / 300 ms / 60 FPS, 20minutový paměťový test, TalkBack ani obnovu na druhém skutečném zařízení. Formální M1 a herní ověření nejsou automaticky uzavřené. Import právě vzniklé generace 5 ještě nemohl být tímto výrokem ověřený.

Nejvhodnější další krok: import dat 5 a krátká kontrola zachovaného postupu. Další obsahová práce: pokračovat Durotarem k Orgrimmaru/Barrens a ověřovat Warlockovy třídní návaznosti z konkrétních zdrojů.
