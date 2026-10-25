# E02 — data, identity a první import

## Dvě oddělené databáze

`forever-content.db` obsahuje herní katalog, `forever-personal.db` obsahuje profily a osobní historii. Obě používají Room/SQLite a začínají schématem **1**. Exporty jsou v `app/schemas`. Na žádném builderu není povolená destruktivní obnova databáze při chybě migrace.

| Herní databáze | Účel |
|---|---|
| package_metadata | Aktivní balíček, generace, verze formátu, SHA-256 a datum kontroly |
| sources | Původní URL, druh zdroje a úplná metadata |
| maps | UI map ID, patro, souřadnicová verze a popis transformace |
| entries | Stabilní identita, typ, původní název, neznámá/známá minimální úroveň, vyhledávací text a úplný záznam |
| entry_sources | Kontrolované vazby položek na důkazy |
| locations | Více poloh položky, mapový rámec, patro, procenta a přesnost |
| relations | Druh vazby, vlastník, ALL/ANY, stav ověření a povolení pro budoucí vedení |
| relation_targets | Cíle jedné vazby; cizí klíče na existující položky |
| relation_sources | Zdroje konkrétní vazby |

| Osobní databáze | Účel |
|---|---|
| profiles | Výchozí profil Horda / Ork / Warlock / úroveň 1, dosud nezvolený režim a profese |
| quest_progress | Nezahájeno, rozpracováno, hotovo nebo odloženo; poslední známý název |
| collected_items | Získaný předmět odděleně od splnění úkolu |
| bookmarks | Funkční osobní záložky |
| notes | Text, volitelná poloha a odkaz na položku |
| planned_stops | Seřazené cíle výpravy |
| discovered_travel | Zaznamenané dopravní body |

Cizí klíče osobních tabulek odkazují na profil. Herní položka je odkazovaná stabilním textovým klíčem, nikoli cizím klíčem do druhého souboru. Smazání nebo přejmenování herní položky tím nevymaže osobní záznam. Poslední známý název je uchovaný jako snímek. Historie vyřazených položek bude zpřístupněna s ovládáním deníku v E05.

## Balíček a jeho smlouva

- `schemaVersion`: 1.
- `packageId`: `wow-forever:starter:durotar`.
- `generation`: kladné číslo, první vzorek 1.
- `gameVariant`: `wow-forever`.
- První vzorek: **21 položek, 8 poloh, 25 vazeb, 26 zdrojů a 4 mapy**.
- Vstup do aplikace: `app/src/main/assets/content/starter.json`.
- Export stejné první generace: `content/starter-v1.json`.
- Formální kontrakt: `content/schema-v1.json`, JSON Schema Draft 2020-12.

Pro E02 jsou podporovány čtyři přibalené mapové obrázky s rozměrem 1002 × 668, třemi úrovněmi a beta sestavením 1.60.1.69893. Nové mapové obrázky a obecná správa obsahových balíčků patří do E09. Samotný popis nové mapy nemůže nahradit chybějící dlaždice.

Herní položky mají klíč `wow-forever:<typ>:<herní ID>`. Záznam bez doloženého číselného ID může mít trvalou vlastní příponu. Název, překlad, alias ani pořadí v seznamu nejsou identitou. Tři položky Vile Familiars proto zůstávají různými úkoly s ID 792, 1485 a 1499.

Český popis, původní název a aliasy mají vlastní pole. SQL index vyhledávání je normalizovaný bez diakritiky a s malými písmeny. Původní zdrojový záznam v poli `payload` zachovává i podrobnosti, pro které ještě není specializované ovládání: například obě tvrzení o počtu předmětů nebo podmínky povolání. Tato pole se nepoužívají jako automaticky potvrzené doporučení.

## Instalace a aktualizace

1. `decodePackage` odmítne chybný JSON, nepodporovaná pole a soubor nad 2 MiB.
2. `ContentValidator` ověří verzi, variantu hry, identity, zdroje, mapové podklady, rozsahy, patra, transformace a typy vazeb.
3. Návaznosti se zkontrolují na cykly. Režimy ALL a ANY zůstávají výslovné; žádná ze současných neověřených vazeb není aktivní pro vedení hráče.
4. Uvnitř transakce se zkontroluje generace a SHA-256. Stejný obsah je beze změny; jiný obsah se stejnou generací nebo pokus o návrat na starší generaci se odmítne.
5. Nový obsah se uloží do herní databáze v jediné transakci. Osobní databáze se nemění.
6. Selhání zápisu vrátí celý herní import zpět. Aplikace nesmaže databázi jako nápravu chyby.

První import proběhne automaticky z APK. Při dalším spuštění se stejná generace znovu nepřepisuje; již nainstalovaný novější obsah se nepřepíše starším přibaleným vzorkem. Vstupní API pro další balíček je `CatalogInstaller.install(bytes)`; výběr souborů, stahování a obecná správa balíčků přijdou v E09.

Přejmenování položky při zachovaném klíči zachová postup. Změnu skutečného herního ID nelze potichu považovat za přejmenování; taková změna bude potřebovat výslovnou převodní tabulku. Databázová migrace při změně SQL schématu je jiný krok než aktualizace obsahu. E02 zavádí první schéma; skutečná migrace 1 → 2 bude vytvořena a otestována až s první změnou struktury.

## Souřadnice a důkazy

Poloha odkazuje na mapový klíč, patro a souřadnicovou verzi. Transformace používá `x / 100 × scaleX + offsetX` a stejně pro osu y; umí i zpětný převod. Vzorek má totožný rámec s mapovým podkladem, tedy měřítko 1 a posun 0. Vykreslení i zaměření bodu používají tutéž transformaci. Area ID 14 nenahrazuje UI map ID 1411.

Neznámá minimální úroveň zůstává `null`. Úroveň uvedená u zadání ji nenahrazuje. Žádná z 21 položek není potvrzená ve vydané hře. Beta, Classic reference, komunitní reference a sporné záznamy se rozlišují. Označení RELEASE navíc vyžaduje zdroj typu `in_game_release_observation`; oznámení o povolání nepotvrzuje konkrétní úkol.

Nartok zůstává v katalogu s neověřenou Forever polohou, takže není přidán jako beta značka. Údaj o dopravě Durotar → Undercity je informativní; nemá potvrzené nástupní místo, příjezd ani dobu cesty.

## Ověření

`ContentValidationTest` obsahuje 17 kontrol. `DatabaseTest` obsahuje 9 kontrol, včetně přejmenování, vyřazení položky, simulované SQL chyby během importu, samostatnosti profilů, skutečného zavření a otevření databázového souboru a odmítnutí neznámé verze bez ztráty řádků. `CatalogFlowTest` ověřuje záložku přes skutečné rozhraní a znovuvytvoření Activity. Zbývající testy ověřují původní mapovou funkčnost. Celkem **37 úspěšných hostitelských testů**.

Technické zdroje: [Room](https://developer.android.com/jetpack/androidx/releases/room), [migrace Room](https://developer.android.com/training/data-storage/room/migrating-db-versions), [KSP](https://github.com/google/ksp), [Kotlin serialization](https://github.com/Kotlin/kotlinx.serialization). Konkrétní verze jsou v Gradle; nejsou používány plovoucí verze.
