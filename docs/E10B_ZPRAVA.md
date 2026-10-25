# E10 — druhá dávka, výsledek

25. 10. 2026 · datový balíček 5 pro dosavadní E12.

## Předání

WoW_Forever_E10_data_5.wfp: úplný původním vydavatelem podepsaný katalog a 85 mapových dlaždic. Má 182 položek, 74 poloh a pět map. Přidává 18 úkolů, 12 NPC/nepřátel, pět látkových odměn a osm milníků kouzel. Český návod a startovní kontrolní seznam: E10B_NAVOD.md. Rozsah a zdroje: E10B_DATA.md, research/e10b.

APK se nemění, zůstává vlastnicky podepsaná E12 / 0.12.0-e12 / kód 12. Všech 125 vstupů APK bylo znovu porovnáno s identitou podepsaného E12 i skutečným Git commitem; shoda prošla. SHA-256 APK stále 462e17f1ddb3e246307618294d00bd9e7f7ea2cedebe1211cc063ab3587ccb9d. Nové release APK ani nové klíče se nevytvářely.

## Skutečně provedené kontroly

- Celá sada hostitelských testů aplikace z aktuálních zdrojů: **179 prošlo, 0 selhání, 0 chyb, 0 vynechaných**. Pět nových testů druhé dávky. Gradle :app:testDebugUnitTest: BUILD SUCCESSFUL, 1 min 30 s. XML všech 44 sad a log stejného úplného běhu jsou v qa/e10b.
- **13 Python testů prošlo**, včetně tří nových kontrol: zachování starších ID, zákaz přiřadit vestu ke špatné části Sarkoth a zákaz odhadnout minimum nebo zapnout Classic návaznost.
- Skutečný veřejný vydavatelský podpis, 86 datových souborů, SHA-256, rozměry a dekódování PNG prošly validací distribučního WFP. Veřejné manifestové a katalogové bajty používané testy jsou shodné s publikovaným balíčkem. Všech 85 PNG je bajtově shodných s daty 4. Původní fallback, schémata, mapové rámce a osobní formát se nezměnily.
- Nativní UI test E12 importoval skutečně podepsanou generaci 5 přes ContentResolver/ViewModel přímo z fallback 2. Zachoval hotový Cutting Teeth, ukázal aktuální počty i rozsah, našel Legging It a správný popis odevzdání. Přes hledání Pal’juha a skutečnou akci Ukázat na mapě otevřel jeho bod v Durotaru; obnovení aktivity zachovalo výběr a osobní data.
- Diskový Room test provedl aktualizaci 4 → 5, znovuotevření úložiště a návraty 5 → 4 → 5. Dva profily, hotový úkol, nová volba odměny, poznámka a výprava zůstaly stejné. JSON export obsahuje nové osobní záznamy. Cíl Pal’juh ve starší generaci nezíská náhradní polohu a po návratu dat 5 ji znovu má.
- Pozměněný katalog pod původním podpisem/součty byl odmítnut; aktivní data 4 a postup zůstaly stejné. Starší testy nadále kontrolují chybějící dlaždice, špatné mapové rámce a obnovu záloh.
- Datové kontroly hlídají rozdílná místa odevzdání, identitu 830/831, přijetí prvního úkolu z předmětu, látkový materiál, odlišný item level/minimum, milníky knihy a zachování konfliktu 1485. Beta a Classic data nevytvářejí potvrzené odemčení, nevratnost ani ověřenou dopravu.

Snímky coverage.png a quest.png jsou skutečné nativní UI z hostitelského grafického režimu; byly vizuálně zkontrolované. Nejde o nové snímky Jendova telefonu. Testovací prostředí bylo při přípravě opravené na nativní dekódování PNG a skutečné akce detailu/katalogu; žádný test se nevypnul ani nezměkčil. Hláška dekodéru „unimplemented“ z úplného běhu patří dosavadnímu negativnímu testu vadného PNG. Nový Lint ani celé sestavení APK se kvůli čistě obsahové změně neopakovaly; poslední E12 Lint má nula chyb a známá varování.

## Identita a omezení

- WFP velikost: 9 684 063 bajtů.
- WFP SHA-256: 27b70643d39c0c832f12da4f4ceac1aa57fda19fbf7fac533d84fb3e73aab725.
- Podpis: původní obsahový vydavatel E09; minimumAppCode 10; katalogové a manifestové schéma 1; generace 5.
- Zdrojový archiv uchovává aktuální testy, fakta, veřejné podepsané manifesty a Git historii. Soukromé klíče do něj nepatří. Vazba archivu na nezměněné APK je v source-state.json; aktuální datový receipt je release/E10_data_5_release.json.

Jenda výslovně potvrdil základní kontrolu E12 na telefonu. Toto potvrzení je zapsané v POSTUP.md a stage_status.json. Nová generace 5 na jeho telefonu ještě importovaná/ověřená není. Neproběhlo nezávislé ověření herního klienta, měření výkonových cílů, TalkBack nebo potvrzená obnova na druhém skutečném zařízení. M1 se obecnou zprávou automaticky neuzavírá. Celý svět, úplný Durotar, cesta 1–60 a vnitřky dungeonů stále zbývají. Dopravní hrany a RELEASE údaje mají počet nula.

Další krok: importovat data 5 přes Postava → Balíčky a mapy a zkontrolovat známý postup i Legging It. Potom navázat další zdrojovanou dávkou Durotaru a ověřením Warlockových třídních kroků.
