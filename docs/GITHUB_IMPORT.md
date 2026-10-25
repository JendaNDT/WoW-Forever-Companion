# Obnova úplného projektu na GitHub — 25. října 2026

## Původ

- Repozitář: `JendaNDT/WoW-Forever-Companion`, původní jediný commit `584effd4005ebf7e6fe24f6fc32246d1e80f23a8` obsahoval pouze README.
- Projekt je obnoven z původní knihovny ChatGPT, souboru `WoW_Forever_Audit_zdroje_checkpoint.zip` (284 518 077 bajtů).
- SHA-256 archivu: `e438df3beeb05fe6b8e4e08e9966be84262bafa647ebe709e57e28d5d16596f9`.
- Původní revize uvedená archivem: `a5051b8af0f7451f63f4b60ed35ff5135a5f266a`. Archiv je označený `checkpointOnly`, nikoli jako ověřené vydané APK.
- Všech 150 vstupů sestavení souhlasilo s přibaleným `source-state.json`; otisk vstupů: `572304f16108952fb5ac4446a6fc19496fc4fad4bba23924ccb150db7137af2b`.
- Skutečné nastavení aplikace: `cz.jenda.wowforever.guide`, verze `0.16.0-audit`, kód 17, data 8.

`source-state.json` je záznam původního archivu. Nová sestavení z GitHubu používají aktuální commit repozitáře. Původní historie v archivu opravy zůstává v místní záloze; do nové historie se nenahrávají staré velké archivy ani soukromé klíče.

## Porovnání s opravenou verzí 0.15.1

Samostatně byl získán původní `WoW_Forever_E10_oprava_zdroje.zip` (314 802 814 bajtů), který uvádí revizi sestavení `f0afabc601dc1cc415a804d0fcacc55654b3c0d4` a revizi archivu `68b6f8d77729de16f9a098226ebbf97de561a779`.

Produkční `Recommendations.kt`, `ContentPackage.kt` a `CatalogInstaller.kt` jsou v novějším archivu shodné s opravou 0.15.1. Proudové čtení z `PackageManager.kt` je přesunuté do `BundledAssets.kt` a rozšířené o postupné čtení částí `.wfpart`. Starší balíček 7 je zachovaný pro test aktualizace a návratu; aplikace používá balíček 8. Tím je doložen původ změn, nikoli fyzický test nového APK na telefonu.

## Ověření při obnově

- Všech 32 Python testů prošlo.
- Prošly kontroly schématu, původního katalogu, map a dávek E10, E10.2 a E10.3.
- Balíček 8 má platný podpis vydavatele: 11 912 položek, 17 330 poloh, 61 map, 1 103 dlaždic. Všechny dříve vydané dlaždice zůstaly shodné; 56 nových obrazových podkladů bylo přesně rekonstruováno.
- SHA-256 celého balíčku 8: `8c805d7f25a7d0e33b760026c32bb82216f153ed27dadfde2e3faa7f47d2b94e`.
- Gradle wrapper JAR souhlasí s oficiálním kontrolním součtem Gradle 8.13; distribuční ZIP má kontrolní součet v nastavení wrapperu.
- Kontrola dodaných souborů nenašla soukromé podpisové soubory ani běžné vzory přístupových klíčů. Přibalený `publisher.der` je veřejný klíč pro ověření obsahu; otisk certifikátu také není tajný údaj.
- Historické záznamy v `docs/qa` jsou původní důkazy předchozích etap. Aktuální sestavení a testy se ověřují zvlášť v GitHub Actions; jejich výsledek nelze odvozovat ze starých zpráv.

## Konfigurace GitHub Actions

`android.yml` používá JDK 17, SDK 36 a Build Tools 35.0.0. Kontroluje obsah, sestaví Debug i AndroidTest APK, spustí jednotkové testy a lint obou variant. Ověří identitu hotového APK vůči aktuálnímu commitu, podpis APK, všechny přibalené soubory a uložení offline dat bez další komprese. APK a protokoly uloží na 14 dní.

`release.yml` je ruční úloha, která vyžaduje původní vlastnický podpis v chráněném prostředí `android-pilot`. Při obnově se soukromé klíče nezískávaly, neměnily ani nenahrávaly. Vývojový podpis nenahrazuje podpis původního vydání.
