# E11 — ověření aktuálního plného obsahu

26. 10. 2026 · aplikace 0.16.0-audit (17) · data 8.

**Automatické ověření spolehlivosti E11 je dokončené.** Prošly zálohy, obnovení po ukončení procesu a mapové zátěže na Androidu 8 i 16. Fyzické výkonové cíle a přístupnost na Samsungu zůstávají samostatně nepotvrzené.

Ověření se týká současného katalogu: 11 912 položek, 61 map a 1 103 dlaždic. Nové soubory přidávají testy a jejich automatické spouštění; produkční kód, identita aplikace, databázová schémata a obsah se nemění. Uživatel již potvrdil základní funkčnost předaného APK po čisté instalaci.

## Scénáře

- Dva oddělené průchody skutečným vynuceným ukončením otevřené aplikace. Před každým ukončením se čeká na připravené rozhraní, zaznamená se PID a ověří jeho zánik. Další kontrola běží v novém procesu, bez vymazání dat.
- Zachování dvou profilů a všech sedmi typů osobních záznamů: úkoly, předměty, záložky, poznámky, plán výpravy, doprava a vybrané odměny. Kontroluje se i nedokončená akce Zpět a historický záznam úkolu.
- Export a import všech profilů jako nových kopií, unikátní poznámky a nezměněné původní profily. Nahrazení profilu a návrat přes místní snímek.
- Vynucená chyba zápisu uprostřed obnovy: nesmí vzniknout částečně obnovený profil ani nový neplatný snímek. Vadný JSON zálohy se odmítne.
- Dvě stě opakovaných dotazů nad skutečným katalogem. Ověřuje se stálost výsledků; zaznamenaný čas v emulátoru není měřením odezvy telefonu.
- Opakované střídání všech 61 map se skutečnými body a dlaždicemi. Běžné CI používá 5 minut; ručně spustitelná rozšířená varianta požaduje 20 minut. Kontroluje se obnovení výřezu, nejvýše dva ponechané mapové stavy, uvolnění neaktivní mapy při nedostatku paměti a opakované zavření rendereru.

Automatický běh používá oddělené čisté emulátory Android API 26 a 36. Testovací nástroj odmítá destruktivní scénáře na skutečném telefonu. Každý průchod vyžaduje dokončený výsledek Android instrumentation se správným počtem testů; návratový kód samotného adb nestačí.

## Výsledky

[Automatická kontrola Androidu](https://github.com/JendaNDT/WoW-Forever-Companion/actions/runs/40291857619) uspěla pro revizi `ba0fca609cda1ce171e69ff46ea52e1ff2559e43`:

- **213 aplikačních testů v 54 sadách**: 0 selhání, 0 chyb, 0 vynechaných.
- **35 Python testů** dat a pomocných nástrojů: 0 selhání.
- Sestavení aplikace i instrumentačního APK, kontroly dat a všech přibalených souborů uspěly.
- Lint: 0 chyb; debug 17 varování, release 18. Jde o dostupné novější závislosti/Android cíl, stávající doporučení pro volné místo a upozornění na profileable u starších Androidů. Závislosti ani pravidla Lint se v této etapě neměnily.
- SHA-256 150 produkčních vstupů `572304f16108952fb5ac4446a6fc19496fc4fad4bba23924ccb150db7137af2b` odpovídá vstupům APK, které Jenda již potvrdil. Novější sestavení má vlastní identitu revize a vývojový podpis; nejde o bajtově shodné APK ani o potvrzení aktualizace původního podpisu.

Přehled sad je uložený v [qa/e11-final/host-summary.json](qa/e11-final/host-summary.json).

[Běžný ověřovací běh na obou verzích Androidu](https://github.com/JendaNDT/WoW-Forever-Companion/actions/runs/40291857502) i [samostatný rozšířený běh Androidu 8](https://github.com/JendaNDT/WoW-Forever-Companion/actions/runs/40289701001) skončily úspěšně:

| Prostředí | Délka mapové zátěže | Přepnutí map | Celé průchody 61 map | Nativní testy | Důkaz |
| --- | ---: | ---: | ---: | --- | --- |
| Android 8 / API 26 | 5 min 17.3 s | 915 | 15 | 5 / 0 selhání | [protokol](qa/e11-final/android26/summary.json) |
| Android 16 / API 36 | 5 min 20.8 s | 915 | 15 | 5 / 0 selhání | [protokol](qa/e11-final/android36/summary.json) |
| Android 8 / API 26 | 20 min 10.9 s | 3 477 | 57 | 5 / 0 selhání | [protokol](qa/e11-final/android26-long/summary.json) |

Každý průchod pracoval s **17 330 skutečnými body**. Nejvýše zůstaly dva mapové stavy; kontrola při oznámení nedostatku paměti ponechala jeden a zavření nula. Pět dokončených testů v každém běhu zahrnuje tři oddělené fáze zachování a obnovy dat, mapovou zátěž a 200 opakovaných dotazů nad 11 912 položkami. Dvě vynucená ukončení běžící hlavní aplikace jsou doložená rozdílnými procesy, jejich ukončením a následným načtením z disku. Jde o stejnou sadu opakovanou v různých prostředích, nikoli o patnáct různých testovacích scénářů.

Snímky hlavní aplikace po obnovení byly vizuálně zkontrolované na obou verzích Androidu: mapový podklad Winterspringu, značky, profil a plán se zobrazily. Připravené UI ještě nemusí znamenat načtené všechny dlaždice; první časný snímek tuto mezeru zachycuje a není používán jako důkaz rychlosti vykreslení mapy.

### Přerušení a pomocné kroky

Původní dlouhý běh API 36 byl přerušen hlášením runner shutdown. Jeho mapová část se nepočítá jako dokončená. Z protokolu jsou doložené pouze tři ukončené testovací fáze a dvě ukončení procesu. Následující běžný pětiminutový průchod na API 36 uspěl. Dvacetiminutový funkční průchod je doložený na API 26.

Pomocné čištění protokolu a vypnutí rádia staršího emulátoru byly oddělené od povinných testovacích tvrzení. Některé starší systémové příkazy vracejí neúspěch; ten je v protokolu zachovaný. Z odmítnutého příkazu se nevyvozuje chyba aplikace ani potvrzení vypnuté sítě. Instrumentační testy, přesné počty výsledků a kontrola osobních dat zůstaly povinné. Běžné CI nově používá 300 sekund; ruční spuštění nabízí 1200 sekund.

Původní výsledky JSON a samostatné textové výsledky jednotlivých fází jsou uložené pod `docs/qa/e11-final`. Každý adresář obsahuje i původ revize, běhu a otisk staženého archivu. Úplné přílohy GitHub Actions mají omezenou dobu uchování; tento přehled a vybrané protokoly zůstávají v repozitáři. Po ověření byla v testovacím zápisu upravena pouze bezpečná práce s případným chybějícím rodičovským adresářem; produkční vstupy se nezměnily.

## Co tyto testy neprokazují

Emulátor s vývojovým APK neprokazuje 60 FPS, třísekundový start, celkovou odezvu hledání do 300 ms ani spotřebu a dlouhodobý růst paměti na Samsungu. Dvacetiminutová funkční zátěž a omezení počtu mapových stavů nejsou měřením celkového PSS. Samostatně zůstávají fyzický TalkBack, obnova na druhém skutečném zařízení a správnost údajů přímo ve hře.

Čistá instalace ani tyto scénáře nepotvrzují aktualizaci přes původní vlastnický podpis. Pro testovanou verzi není kvůli přidání testů nutná další instalace.
