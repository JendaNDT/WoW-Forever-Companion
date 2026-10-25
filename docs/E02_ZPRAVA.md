# E02 — databáze a nástroje herního obsahu

**23. října 2026. Implementováno a ověřeno hostitelskými testy.** Uživatel před zahájením této etapy potvrdil, že E01 funguje. U E02 zbývá vyzkoušení aktualizace a ukládání na telefonu.

## Výsledek

Vzniklo nové APK **0.2.0-e02**, kód verze **2**, ID `cz.jenda.wowforever`. Obsahuje dosavadní nativní mapu a nově dvě oddělené databáze, předvyplněný osobní profil, herní katalog a funkční záložky. Katalog obsahuje **21 položek, 25 vazeb, 8 poloh a 26 zdrojů**. První obsahový balíček se importuje při spuštění automaticky.

Nové tlačítko Katalog otevře seznam, detail položky, stav ověření a odkazy na původní zdroje. Uživatel může přidat záložku a zobrazit pouze své uložené položky. U úkolů se rozlišuje původní název i herní ID, aby stejnojmenná zadání nesplynula.

Záložky a herní postup mají oddělený význam. Ovládání dokončení úkolů, poznámek a získaných odměn přijde v dalších etapách; E02 připravuje jejich osobní datové schéma a testuje jeho zachování.

## Kontroly

| Kontrola | Výsledek |
|---|---|
| Čisté sestavení APK a instrumentačního APK | Prošlo, 91 úloh provedeno |
| Hostitelské JUnit / Robolectric testy | **37 prošlo, 0 chyb** |
| Android Lint včetně testů | **0 chyb a 0 varování** |
| Formát, varianty hry, identity, zdroje, souřadnice, typy vazeb a cykly | 17 úspěšných kontrol |
| Import, aktualizace a oddělení osobních dat | 9 úspěšných databázových testů |
| Přejmenování herního záznamu | Zachovalo postup, záložku, poznámku a získaný předmět |
| Vyřazení položky z katalogu | Zachovalo osobní záložku a poslední známý název |
| SQL chyba během zápisu nového katalogu | Celý import se vrátil zpět, původní generace zůstala použitelná |
| Zavření a otevření osobního databázového souboru | Záložka zůstala uložená |
| Neznámá databázová verze | Otevření se odmítlo bez smazání uložené záložky |
| Katalog a přidání/odebrání záložky přes rozhraní | Prošlo, včetně znovuvytvoření Activity |
| Dosavadní mapa, souřadnice a obě rozložení | Prošlo |
| JSON Schema a příkazový validátor prvního balíčku | Prošlo |
| 68 dlaždic, kontrolní součty a rekonstrukce čtyř původních map | Prošlo |
| Podpis nového APK proti E01 | **Shodný podpis**, nový kód verze umožňuje aktualizaci |

Všechny hostitelské testy se vztahují k aplikaci v této etapě. Snímky obrazovek vznikly jejím nativním vykreslením v Robolectric a byly vizuálně zkontrolované. Doplňující běh testu katalogu zachytil přímo jeho dialogové okno. Protokoly a snímky jsou v `docs/qa/e02`.

Instrumentační APK se sestavilo, ale na emulátoru ani fyzickém telefonu v této etapě spuštěné nebylo. Předchozí obecné potvrzení fungování E01 neznamená provedenou zkoušku nového ukládání E02. Neuvádíme měření FPS nebo paměti skutečného telefonu.

## Co zůstává otevřené

- Ověřit instalaci aktualizace a záložku po úplném zavření aplikace na telefonu.
- Zadat a upravovat úroveň, rasu a oblast v E03.
- Propojit plné mapové vrstvy a hledání v E04.
- Zpřístupnit plný postup, poznámky, odměny a zálohy v E05.
- Nezávisle ověřovat herní data v odpovídajícím klientu Forever. V této etapě je takových kontrol stále **0**.

Neznámé minimální úrovně zůstávají prázdné, tři úkoly Vile Familiars mají oddělenou identitu a rozpor 6/8 předmětů u ID 1485 zůstává zachovaný. Classic reference se nepřevádějí na potvrzené Forever údaje. Obsah není úplným průvodcem a neuvádí neověřené návaznosti jako doporučení.

## Předání

- `WoW_Forever_E02.apk`: 18 836 896 bajtů, přibližně 18 MiB, Android 8+.
- SHA-256 APK: `d48aee31c47bfdf319ad9803b64da0b2f60f626cfef90a9c639e48323a295f70`.
- Zdrojový archiv obsahuje kód, obě schémata Room, JSON Schema, vzorový balíček, validátory, protokoly a lokální Git historii.
- Soukromý vývojový podpisový klíč není součástí zdrojového archivu. Vzdálený GitHub repozitář ani spuštění workflow zde nevznikly.

**Doporučený další krok:** na telefonu přidat záložku, aplikaci úplně zavřít a ověřit ji po novém spuštění. Po potvrzení navázat E03 — ovládáním a úpravou profilu postavy.
