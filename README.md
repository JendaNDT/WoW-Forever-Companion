# WoW Forever Companion

Nativní Android průvodce pro Hordu / Warlocka 1–60. Kotlin, Jetpack Compose, Room; kompletní offline obsah je přibalený v APK.

**Aktuální verze: 0.16.0-audit (17), data 8.** Audit veřejných zdrojů je v [docs/E10_AUDIT.md](docs/E10_AUDIT.md). Obsahuje 11 912 položek a 61 map. Žádná položka zatím není označená jako ověřená ve vydané hře. Dva nově nalezené rozpory zůstávají v aplikaci viditelné.

Velká offline data jsou uložena po částech `.wfpart`; Android je čte přímo. Pro běžné sestavení není nutné je ručně skládat. JDK 17, Android SDK 36 a Gradle wrapper: `./gradlew :app:assembleDebug :app:testDebugUnitTest`. Release vyžaduje původní soukromý podpisový klíč a proměnné popsané níže.

Níže jsou zachované historické pokyny k předchozím etapám; aktuální audit a stav mají přednost.

# Forever průvodce — E10, oprava spouštění

Nativní česká aplikace pro Android, Horda / Ork / Warlock. Kotlin, Jetpack Compose, Room a nativní mapa. Neoficiální osobní projekt.

## Jedno APK pro cestu 1–60

Nainstaluj **WoW_Forever_E10_oprava.apk přes E09 nebo novější verzi bez odinstalování**. Před aktualizací ulož postavu a vyexportuj JSON zálohu. Data 7 už jsou uvnitř: **12 352 položek, 18 441 poloh, 61 map / 1 103 dlaždic**. Obsah zahrnuje 2 818 úkolů, 104 třídních referencí Warlocka, související předměty a objekty, služby a přípravu na dungeony. Připraví se automaticky offline. Postup a stejný či novější import zůstávají zachované.

Oprava pádu: **0.15.1-e10-fix1 / kód 16**, návod `docs/E10_FIX_NAVOD.md`. Reprodukovaná paměťová chyba je opravená bez zmenšení obsahu. Aktuální výsledky: `docs/E10_FIX_ZPRAVA.md` a `docs/qa/e10fix`.

Společná telefonní přejímka: `docs/E10_FULL_NAVOD.md`. Rozsah, zdroje, omezení a obnova: `docs/E10_FULL_DATA.md`. Aktuální stav: `docs/POSTUP.md` a `docs/stage_status.json`. Výsledky sestavení jsou v `docs/E10_FULL_ZPRAVA.md` a `docs/qa/e10full`.

## Funkce a rozsah

- Profil, úkoly, odměny, získané předměty, záložky, poznámky, výprava, záloha a bezpečný návrat obsahu.
- Katalog se zdroji a filtry třídních úkolů, dungeonů, oblasti a úrovní. Vyhledávatelný atlas.
- Cíle úkolů a zdroje předmětů mají vlastní odkazy; objekty nejsou zaměněné za NPC nebo předměty.
- Pět instančních přehledů má samostatný obrazový rámec bez domýšlených NPC souřadnic.
- Classic a komunitní reference zůstávají označené. Nula RELEASE údajů a ověřených dopravních hran. Herní M2/M3 ani optimální XP/BiS nejsou tímto potvrzené; přesné mezery viz technický popis.

## Bezpečná aktualizace a provoz

Podpis E09+ a identifikátor `cz.jenda.wowforever.guide` jsou zachované. Základní kontrolu E12, E13 i E10.3 na telefonu Jenda potvrdil. Nová celková E10, měřený výkon E11, TalkBack a obnova na druhém skutečném zařízení mají samostatnou přejímku. Hostitelské testy skutečný telefon nenahrazují.

Přibalený archiv je podepsaná generace 7. Předchozí výběr se při automatickém přechodu zachová pro návrat. Záměrný návrat zůstane vybraný po restartu. Obnovit přibalený obsah vrací data 7; osobní historie se nemaže. Pro případ selhání přípravy je uvnitř zachovaný i původní nouzový základ 2. Detaily: `docs/E10_FULL_DATA.md`.

Aplikace nepotřebuje účet, GPS ani AI klíč. Při běžném prohlížení stažených map nepotřebuje internet. Samostatný HTTPS nebo souborový import zůstává volitelný pro budoucí aktualizace; veřejný aktualizační server, obchod a vzdálené CI se zde nepublikovaly.

## Sestavení

JDK 17, Android SDK 36, Build Tools 35.0.0, Gradle Wrapper 8.13 / AGP 8.13.2, Kotlin 2.1.21. Minimum Android API 26, compile/target 36. Závislosti jsou uzamčené konkrétními verzemi. `local.properties` vytvoř lokálně. Pro release nastav původní FOREVER_SIGNING_STORE a FOREVER_SIGNING_PASSWORD; klíče patří mimo repo. Bez vlastníka/čistých zdrojů se release odmítne. Debug slouží jen pro vývoj.

```sh
python3 -m pip install -r tools/requirements.txt
python3 tools/verify_bundled.py
python3 tools/verify_e10b_data.py
python3 tools/verify_e10c_data.py
./gradlew --max-workers=1 :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug :app:lintRelease
```

`connectedDebugAndroidTest` vyžaduje připojený Android. Chráněné CI je připravené ve zdrojích. Ověřování podpisu, zdrojů a archivace: `tools/verify_release.py`, `tools/package_sources.py` a `docs/E12_DATA.md`. Zdrojový archiv obsahuje `history.bundle` a `source-state.json` pro dohledatelnou obnovu; soukromé klíče se do něj nebalí.

## Herní data a starší dávky

`content/starter-v1.json` a stejné assets jsou původní nouzová generace 2. `content/e10-v4.json` a `content/e10-v5.json` uchovávají předchozí dávky; integrovaný WFP 6 je v assets/content. `tools/build_content.py` nadále reprodukuje původní základ, nesmí nahrazovat integrovanou distribuci. Nové dávky mají samostatné generátory a audit zdrojů. Starší instrukce k ručnímu importu v dokumentech E09/E10/E12 patří těmto tehdejším vydáním.

E10.3 přidává sever Durotaru, orčí cestu k Voidwalkerovi a vybrané předměty. Šest nových NPC má Classic polohu, čtyři beta pozorování. Viz `research/e10c/SOURCES.md`.

Výběr a původ obsahu: `docs/E10_DATA.md`, `docs/E10B_DATA.md` a `research`. Osobní Room schéma 2, herní Room 3, JSON záloha 1. Datum a beta build se řídí jednotlivými zdroji, nikoliv datem vydání APK.

Mapy © Blizzard Entertainment, původ warcraftforever.games. Podklady a omezení použití jsou v `THIRD_PARTY_NOTICES.md`. Nezávislé herní ověření ani licence k veřejné redistribuci grafiky nebyly tímto vydáním doplněné.
