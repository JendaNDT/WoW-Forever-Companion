# E13 — dosavadní obsah přímo v aplikaci

25. 10. 2026 · 0.13.0-e13 / kód 13 · cz.jenda.wowforever.guide.

## Uživatelský výsledek

Stačí instalovat jedno APK přes E12 bez odinstalování. APK obsahuje přesně dříve vydanou generaci 5: 182 položek, 74 poloh, pět map a 85 PNG. Při prvním spuštění se připraví automaticky a bez internetu. Ruční výběr souboru ani potvrzení importu nejsou pro běžné použití potřeba. Aktuální údaje v nápovědě a dialogu Pokrytí odpovídají aktivnímu katalogu. Samostatné balíčky zůstávají možností pro budoucí obsahové aktualizace.

Nejde o další obsahovou dávku. Všechny herní údaje, původ zdrojů, souřadnice a míra ověření jsou stejné jako data 5 z E10.2. M2/M3, celý svět a cesta 1–60 zůstávají otevřené. Základní telefonní kontrolu E12 už Jenda potvrdil; E13 vyžaduje vlastní krátkou kontrolu instalace.

## Přibalený obsah a výběr generace

assets/content/bundled-data-5.wfp je bajtově shodný s publikovaným WoW_Forever_E10_data_5.wfp. SHA-256: 27b70643d39c0c832f12da4f4ceac1aa57fda19fbf7fac533d84fb3e73aab725. Má stejný podpis původního obsahového vydavatele E09. assets/content/starter.json a původních 68 dlaždic generace 2 zůstávají nouzovým základem; nic se nepřegenerovalo ani znovu neoznačilo jako RELEASE.

Produkční GuideApplication výslovně předává přibalený archiv do GuideStore/PackageManager. Správce po obnově výběru při prvním startu ověří podepsaný manifest. Je-li zvolený obsah starší než 5, spustí dosavadní omezený a ověřovaný import, sestaví samostatnou Room databázi, prověří všechny soubory a PNG a atomicky přepne ukazatel. Předchozí generaci zachová pro návrat. UI se zpřístupní až po dokončení této přípravy.

Importovaná generace 5 zůstává ve svém dosavadním adresáři. Novější podepsaný obsah se automaticky nesnižuje. Osobní databáze se tímto procesem neotevírá ani nepřepisuje; zůstává oddělená. Schémata Room 2/3 a záloha formátu 1 se nemění.

Samostatný AtomicFile bundled-receipt uchovává ID nabídnutého podepsaného manifestu. Uživatelův pozdější návrat 5 → 4 nebo 2 proto zůstane vybraný i po dalším spuštění. Budoucí jiné integrované vydání má jiné ID a může nabídnout novou generaci. Po zjištěném poškození výběru/katalogu se obsah obnoví z APK i přes existující potvrzení. Selhání pomocného zápisu po úspěšné aktivaci nezpůsobí vrácení dat; zápis se zopakuje při dalším startu.

Obnovit přibalený obsah výslovně vrátí data 5 i po ručním návratu nebo novější importované generaci. Starší běžný soubor se stále nedá importovat namísto návratu; povolení starší verze je soukromá cesta výslovného obnovení důvěryhodného archivu uvnitř APK. Nápověda nevede uživatele k importování dat 4.

Při přerušené přípravě, vadném archivu nebo nedostatku místa zůstane poslední funkční výběr a osobní historie zachovaná. Správce ukáže informaci k opakování přípravy. Zvolený starší obsah po vědomém návratu je záměrná volba, nikoliv automaticky chyba. Síť není součástí přípravy integrovaných dat.

## Ověřování a historické scénáře

E13BundledContentTest a E13BundledFlowTest používají skutečný produkční start včetně přibaleného archivu a veřejného vydavatelského klíče. Pokrývají první instalaci, přechod 2/3/4 → 5, zachování již importované 5, novějšího obsahu, návrat a jeho zachování po restartu, obnovení přibaleného obsahu, poškození cache, přerušení a nedostatek místa. UI běží přes GuideApplication; zvláštní E13UpgradeApplication jen před startem vyplní původní osobní databázi a dědí skutečný produkční store.

Starší regresní UI testy E04–E12 nadále představují tehdejší start bez integrovaného rozšíření. Jejich testovací LegacyGuideApplication výslovně používá původní inicializaci; nastavuje ji pouze testový robolectric.properties. Produkční kód nepřepíná chování podle testu/debug sestavení. Nové E13 testy explicitně používají produkční aplikaci. Samostatný test novější generace používá pouze dočasný testovací podpis a vlastní stream, nikdy soukromý produkční klíč nebo syntetickou produkčně podepsanou distribuci.

tools/verify_bundled.py kontroluje skutečný archiv, vydavatele, zveřejněné manifestové a katalogové bajty, všech 85 dlaždic a počty. tools/verify_release.py z E13 APK znovu vyjme a ověří právě tento archiv; uloží bundledContent do receipt. Identita sestavení zachovává formát E12 a otisk zahrnuje i celý nově přibalený soubor. Označení fallbackGeneration v technickém receipt nadále popisuje nouzový základ 2; uživatelský integrovaný obsah je bundledContent.generation=5.

## Reprodukce

```sh
python3 tools/verify_bundled.py
python3 tools/verify_e10b_data.py
python3 -m unittest discover -s tools -p 'test_*.py' -v
./gradlew --max-workers=1 clean :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug :app:lintRelease
python3 tools/verify_release.py --apk app/build/outputs/apk/release/app-release.apk --aapt /sdk/build-tools/35.0.0/aapt --apksigner /sdk/build-tools/35.0.0/apksigner --previous-apk /cesta/WoW_Forever_E12.apk --output ../WoW_Forever_E13_release.json
```

Release nadále vyžaduje původní vlastnický keystore, správný certifikát E09+ a čistou dohledatelnou revizi. Nové soukromé klíče se nevytvářejí. Podpis aplikace, minimum API 26, ostatní oprávnění a zálohování zůstávají stejné. Vzdálený workflow se pouze aktualizuje ve zdrojích; žádný veřejný hosting, GitHub nasazení ani obchod se tím nespouští. Konkrétní výsledky jsou v E13_ZPRAVA.md a qa/e13.
