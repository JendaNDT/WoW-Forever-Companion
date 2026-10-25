# E10 — výsledek a předání

24. 10. 2026 · 0.10.0-e10 / kód 10 · cz.jenda.wowforever.guide.

## Výsledek

První obsahová dávka E10 přidává severní Barrens, služby v Orgrimmaru, vybrané odměny, přípravu Ragefire Chasm a Wailing Caverns a pět Warlockových milníků knihy kouzel. Podepsaná distribuční generace 4 má **139 položek, 62 přímých poloh a pět map / 85 dlaždic**. Rozsah je popsaný v každé nově naplněné oblasti a dialog Pokrytí se počítá z aktivního katalogu. Celá cesta 1–60, M2/M3 a vnitřky dungeonů nejsou dokončené. Podrobné počty, zdroje, výjimky a reprodukce jsou v E10_DATA.md.

Nová data se zapínají importem WoW_Forever_E10_data_4.wfp. APK zachovává stejnou záložní generaci 2 a při aktualizaci nepřepisuje aktivní 2/3. Nová verze schvaluje konkrétní souřadnicový rámec Barrens 1413; libovolné mapy nebo změněná geometrie se nadále odmítají. Import vyžaduje aplikaci E10, dřívější generace zůstávají kompatibilní.

## Ověření

- Čisté sestavení APK, instrumentačního APK, všech testů a Lint prošlo: **158 hostitelských testů, 0 selhání, 0 chyb, 0 vynechaných**, včetně **14 nových testů E10**. Celý běh je uložený v qa/e10/clean-build.log; XML výsledky jsou ze stejného úplného běhu.
- Android Lint: **0 chyb, 4 varování**. Dvě dostupné novější závislosti (DataStore a MapCompose) a dvě hlášení UsableSpace na stejném místě. Volné místo se nadále počítá konzervativně se skutečně volnými bajty a rezervou; nespoléhá se na možné vyčištění cizích mezipamětí. Závislosti ani nastavení Lint se v E10 neměnily; upozornění na dostupné verze závisejí i na metadatech repozitářů.
- Distribuce generace 4 a veřejná sada generace 3 prošly skutečným RSA ověřením s původním produkčním vydavatelským pinem E09. Test nepotřebuje přístup k soukromému klíči. Před potvrzením zůstává stará generace aktivní; po potvrzení a znovuotevření diskové Room databáze je aktivní 4 a návrat obnoví 3.
- Nativní Compose test importuje skutečnou distribuci přes ContentResolver a ViewModel, potvrdí aktivaci, přidá Sergra do výpravy a ověří Barrens, značku, číslo zastávky i skutečně vykreslený terén. Obnovení aktivity zachová výběr a plán.
- Osobní záznamy dvou profilů, poznámka Barrens, hotový úkol, plán, objevený letový bod, získaný předmět a výběr odměny zůstávají stejné při návratu 4 → 2 → 4. Záloha formátu 1 funguje i při chybějící mapě. Opravená kontrola zálohy připouští oblast 1413 bez závislosti na právě aktivním katalogu.
- UI kontrola ukládá oblast Barrens skutečným formulářem, vrací obsah na 2 a ověřuje varování chybějící oblasti, zakázané neúmyslné uložení a zachované osobní údaje. Chybějící poznámka/cíl nejsou přemístěné na jinou mapu.
- Chybějící nová dlaždice nemůže být nahrazená přibalenou grafikou ani přepnout katalog. Platně podepsaný balíček s pozměněným převodem souřadnic Barrens se odmítne. MinimumAppCode 10 odmítá E09 před dekódováním nového katalogu; katalog nesmí nepravdivě deklarovat minimum 9.
- Čistě datové kontroly hlídají všechny počty, původní záznamy, oddělené mapové rámce, chybějící místní polohu Ebru, skutečného zadavatele Raptor Horns, nesloučené Hidden Enemies, neznámé minimální úrovně, Classic návaznosti, beta kouzla a odměny a nula potvrzených linek/doporučení.
- Python ověřil 86 souborů WFP, skutečný podpis, součty a PNG. Nových 17 dlaždic má ověřené rozměry a součty; rekonstrukce Barrens je pixelově shodná se zdrojem. Původních 68 dlaždic prošlo dosavadní kontrolou. Pět zdrojových NPC kontroluje správný místní rámec, nikoli nezávislou přesnost v klientu.
- Finální APK má stejný certifikát a identifikátor jako E09, kód 10. Všechna exportovaná Room schémata, oprávnění, původní fallback katalog, soupis fallback souborů, veřejný vydavatelský klíč a 68 starých dlaždic jsou bajtově stejné. Nové dlaždice ani testovací katalogy nejsou v APK. Soukromé klíče jsou oddělené od zdrojů a distribuce.

Pět snímků skutečného UI v hostitelském nativním grafickém režimu bylo vizuálně zkontrolovaných: generation4.png, barrens-map.png, coverage-barrens.png, ebru-unlocated.png a missing-area.png. Nejde o makety nebo nezávislé screenshoty fyzického telefonu. Hláška dekodéru „unimplemented“ v logu patří k úmyslně vadnému PNG v negativním testu E09; všechny testy uspěly.

## Instalace a podpis

Nainstalovat APK **přes E09 bez odinstalování**, potom přes Postava → Balíčky a mapy importovat data 4 a potvrdit náhled. Samotné APK neaktivuje rozšířený katalog. Identifikátor cz.jenda.wowforever.guide a vlastnický certifikát E09 se zachovaly; SHA-256 certifikátu je e422650dc10efbf7074ae880c0cbc0d7e6d09c8d3ba6a1b1e761f99f441c2f6b. Dříve odděleně předané soukromé klíče E09 nadále slouží pro další aktualizace, nové nebyly vytvořené.

Má-li uživatel stále pouze E08 se starým identifikátorem, ponechá ji nainstalovanou a přenese profil JSON zálohou do souběžné E10. Před aktualizací doporučený export postupu. Telefonní scénáře: E10_TELEFON.md.

## Omezení a další krok

Uživatel výslovně zadal E10. Telefonní přejímka předchozích etap a celý M1 zůstávají otevřené; poslední obecné potvrzení telefonu je E05. Neproběhla zde fyzická instalace E10 na S24 Ultra, skutečné ukončení Android procesu, přenos na druhý fyzický telefon, živý výpadek HTTPS ani nezávislé ověření betových herních údajů. Instrumentační APK bylo sestavené, na zařízení nebylo spuštěné. E11 výkonové cíle nebyly měřené. Hosting ani automatické aktualizace nejsou nasazené.

Nejvhodnější další krok je kontrola E10_TELEFON.md: aktualizace přes E09, import dat 4, offline mapa a bezpečný návrat. Potom E11 — výkon a spolehlivost na skutečném telefonu, s dokončením otevřených bodů M1. Další oblasti E10 pokračují v samostatných zdrojovaných dávkách.
