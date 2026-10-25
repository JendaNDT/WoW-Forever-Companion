# WoW Forever průvodce pro Android

Předávací balíček etapy E00, stav k 23. říjnu 2026.

Etapa E00 je dokončena v rozsahu podkladů pro technický prototyp. Zdroje jsou dohledané, čtyři skutečné mapové soubory stažené a vzorek herního katalogu má zaznamenaný původ i nejistoty. E01 může začít bez čekání na další mapové soubory. Vlastní Android projekt a APK patří do E01.

## Obsah balíčku

- docs/E00_ZPRAVA.md: čitelná zpráva s výsledky a odkazy.
- docs/POSTUP.md a stage_status.json: návaznost na schválený plán a přesný další krok.
- maps/inventory.json: seznam map, verze, původ, rozměry a otisky.
- maps/reference: skutečné obrázky Azerothu, Kalimdoru, Durotaru a Orgrimmaru.
- maps/control_points.json: osm poloh NPC zveřejněných beta záznamníkem; kalibrace v nativní aplikaci čeká na E01.
- content/sample_catalog.json: devět NPC, devět úkolů, jeden předmět, jedno kouzlo, čtyři návaznosti a jeden dopravní záznam.
- content/sources.json: zdroje jednotlivých záznamů a jejich použití.
- evidence: otisky kontrolovaných stránek a dekódované komunitní reference.
- docs/open_issues.json a coverage.json: rozpory, chybějící kontroly a skutečné pokrytí.
- tools/verify_e00.py: znovuspustitelná kontrola integrity balíčku a vazeb ve vzorku.
- licenses: licenční oznámení komunitních zdrojů.

Pro kontrolu spusť z kořene balíčku příkaz `python3 tools/verify_e00.py`. Výsledek je uložen v docs/quality_report.json. Skript potřebuje pouze standardní Python 3.

Beta pozorování nejsou označená jako potvrzená ve vydané hře. Souřadnice z komunitních tabulek jsou oddělené od beta poloh. Neznámé hodnoty zůstávají null. U všech přesných herních rad bude před použitím potřeba odpovídající ověření.

Mapové obrázky jsou grafika World of Warcraft od Blizzard Entertainment. Zdrojové stránky a verze jsou uvedené v inventáři. Tento balíček slouží osobnímu technickému prototypu; samostatná licence pro veřejnou distribuci map zde není doložená.
