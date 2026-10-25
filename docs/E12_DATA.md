# E12 — podepsané vydání a dohledatelný původ

25. 10. 2026 · aplikace 0.12.0-e12 / kód 12 · testovací vydání.

## Rozsah

E12 dokončuje programové a dokumentační předání dosavadního pilotu. M1 má dál otevřené fyzické kontroly; M2/M3 a celý obsah 1–60 nejsou dokončené. Z E11 se nepřebírají neprovedená měření telefonu jako úspěšná. Herní data, mapové souřadnice, Room verze 2/3 a záloha formátu 1 se nemění.

Návod přes O aplikaci je offline, posuvný, s přímými akcemi do záloh, správy balíčků a pokrytí. Počty jsou z aktivního katalogu. Zdrojový archiv obsahuje samostatný český NAVOD.md a CHANGELOG.md. Příručka vysvětluje oddělení APK a dat, režim offline, ruční postup, obnovu a přiznaný rozsah.

## Podpis a ochrana sestavení

Identifikátor cz.jenda.wowforever.guide a certifikát E09/E10/E11 zůstávají stejné. Očekávaný SHA-256 certifikátu je v release/owner-certificate.sha256. Úloha checkReleasePrerequisites ověří keystore, klíč forever, shodu certifikátu a čisté dohledatelné zdroje. Je závislostí packageRelease, assembleRelease a bundleRelease. Bez původního podpisu nevznikne distribuční release APK. Debug může používat místní vývojový podpis a není náhradou aktualizace vlastnické větve.

Soukromý archiv z E09 je dál platný a uchovaný odděleně. Nové klíče se nevytvářejí. Hodnoty FOREVER_SIGNING_STORE a FOREVER_SIGNING_PASSWORD zůstávají prostředím sestavení; nepatří do Git historie, APK, identity nebo zdrojového archivu. Žádná hesla se nevypisují.

## Vazba programu na zdroje

Gradle generuje assets/release-info.json, podepsaný společně s APK. Obsahuje verzi programu, kanál pilot, revizi zdrojů, příznak neuložených změn, otisk vstupů, přibaleného katalogu, veřejného vydavatele dat a očekávaného vlastnického certifikátu. Stejnou revizi a otisk má BuildConfig.

Otisk vstupů zahrnuje app/src/main, app/src/release, oba hlavní Gradle skripty, settings.gradle.kts, gradle.properties, Gradle Wrapper a veřejný podpisový pin. Seřazené řádky mají tvar relativní cesta + nulový oddělovač + SHA-256 souboru + konec řádku; SHA-256 celku tvoří buildInputsSha256. Generovaný obsah v build adresáři není svým vlastním vstupem.

Čistota se vztahuje na vstupy APK. Výsledky testů a návody mohou vzniknout po sestavení v samostatném dokumentačním commitu. Proto může být archiveRevision pozdější než sourceRevision; hlavní kód a jeho otisk musí být stejné. Ověřovač zkontroluje, že sourceRevision je předkem aktuálního HEAD a že skutečně obsahuje stejné vstupy. Jde o dohledatelný záznam sestavení; bitovou reprodukovatelnost mezi různými prostředími zde neměříme.

Zdrojový ZIP má úplný ověřený history.bundle a source-state.json. Po rozbalení bez .git lze sestavit pouze tehdy, pokud otisk vstupů odpovídá source-state.json. S Git historií lze původ zkontrolovat přímo. Ověření podpisu APK je samostatné a povinné před předáním; zdrojový otisk sám podpis nenahrazuje.

```sh
./gradlew --max-workers=1 :app:checkReleasePrerequisites :app:assembleRelease
python3 tools/verify_release.py --apk app/build/outputs/apk/release/app-release.apk --aapt /sdk/build-tools/35.0.0/aapt --apksigner /sdk/build-tools/35.0.0/apksigner --previous-apk /cesta/WoW_Forever_E11.apk --data-package /cesta/WoW_Forever_E10_data_4.wfp --output ../WoW_Forever_E12_release.json
python3 tools/package_sources.py --apk app/build/outputs/apk/release/app-release.apk --output ../WoW_Forever_E12_zdroje.zip
```

verify_release.py ověří kryptografický podpis, nedebug sestavení, identifikátor/verzi, revizi a pracovní vstupy, bajty přibalených assets, původního vydavatele a nepřítomnost soukromých či testovacích souborů. Předchozí APK musí mít stejný certifikát a identifikátor a nižší kód verze. Volitelný WFP se ověří skutečným vydavatelským podpisem a obsahovými kontrolami. Záznam rozlišuje kryptografickou kompatibilitu aktualizace a dosud neprovedenou fyzickou instalaci.

package_sources.py balí jen sledované čisté zdroje, přidá ověřenou Git historii a vazbu na SHA-256 APK. Odmítá klíčové soubory a soukromé údaje. Kontroluje se úplnost ZIP. Do zdrojů se nebalí SDK, mezipaměti nebo keystore.

## Připravené CI

Běžné kontroly v android.yml nepotřebují vlastnický klíč; jejich debug výstup je označený pouze pro vývoj. release.yml je ručně spouštěný workflow z main, používá prostředí android-pilot a ověřuje release před nahráním souborů do artifactu. Na vzdáleném repozitáři se zde nespouštěl.

Než vlastník vloží klíč do GitHubu, musí nastavit android-pilot jako chráněné prostředí s povolenou větví main a požadovaným schválením. Potom odděleně uložit FOREVER_SIGNING_STORE_B64 (base64 samotného JKS) a FOREVER_SIGNING_PASSWORD. Workflow zapisuje JKS pouze do dočasného adresáře runneru mimo checkout a na konci jej odstraní. Klíč se nenahrává jako artifact. Chráněné prostředí ani tajemství se zde nezakládaly; není deklarovaný běžící automatický distribuční server.

## Otevřené provozní kontroly

Na telefonu ověřit skutečnou instalaci aktualizace, nucené ukončení, export a druhé zařízení, systémové ovládání, TalkBack, nelineární velké písmo a výkonové cíle E11. Veřejný hosting, obchod Google Play a automatické aktualizace nejsou nasazené. Vydání nemění datum kontroly herních zdrojů ani jejich stav bety.

Primární podklad podpisového postupu: https://developer.android.com/studio/publish/app-signing .
