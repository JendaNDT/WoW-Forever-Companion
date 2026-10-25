# E12 — podepsané testovací vydání

25. 10. 2026 · 0.12.0-e12 / kód 12 · cz.jenda.wowforever.guide.

## Předání

Připravené je vlastnicky podepsané release APK, offline návod přímo v aplikaci, samostatný český návod, historie změn a ověřitelný zdrojový archiv. APK se instaluje přes E09/E10/E11 bez odinstalování. Soukromý klíč E09 se zachoval; nevznikla nová podpisová identita.

Jde o testovací vydání dosavadního pilotu. Fyzická přejímka M1/E11, instalace aktualizace na Samsungu a výkonové cíle zůstávají otevřené. Celá cesta 1–60, M2/M3 a úplný svět nejsou hotové. E12 neoznačuje beta podklady jako nově ověřená herní data.

## Co bylo ověřené

- Čisté sestavení debug APK, podepsaného release APK a instrumentačního APK, všech hostitelských testů a Lint obou variant prošlo: **150 úloh / 7 min 51 s**.
- **174 testů aplikace prošlo, 0 selhalo, 0 chyb, 0 vynechaných**, z toho **5 nových E12**. Testy nástrojů: **10 úspěšných** včetně sedmi kontrol identity zdrojů a tří parserů Android měření.
- Lint debug: **0 chyb / 3 varování**, release: **0 chyb / 5 varování**. Debug hlásí novější MapCompose a dvě hlášení UsableSpace. Release navíc uvádí omezení profileable shell na API 29+ a hlásí novější serialization/Robolectric. Metadata o nových verzích závisí na dostupných repozitářích. Pravidla Lint ani závislosti se neměnily. Starší paměťová konstanta z E11 má nadále upozornění překladače na deprecated; release zůstává kompatibilní s API 26+.
- Návod otevře zálohy bez změny dokončeného úkolu a přežije obnovení aktivity. Ukazuje skutečná data 2 nebo po importu skutečně podepsanou generaci 4, otevře jejího správce a zachová osobní záznamy. Pokrytí se vrací do návodu a uzavírá vlastními akcemi. Při 200% písmu zůstává dosažitelná obnova i zavření.
- Identita v assets a BuildConfig váže program na stejnou revizi, otisk přibaleného katalogu, veřejného vydavatele a původního certifikátu. Kotlin a nezávislý Python nástroj vypočetly stejný otisk **125 zdrojových vstupů**. Python ověřil také skutečné soubory v uvedeném Git commitu.
- Sestavení bez vlastnických údajů bylo očekávaně odmítnuté úlohou checkReleasePrerequisites. Se správným keystore, certifikátem a čistým kódem preflight i skutečné release sestavení uspěly.
- Samostatný ověřovač zkontroloval skutečný kryptografický podpis, nedebug APK, identifikátor, kód verze a dohledatelné zdroje. E11 má stejný certifikát a identifikátor, kód 11 je nižší než nový 12. Jde o ověřenou kryptografickou kompatibilitu aktualizace, nikoli o zde provedenou fyzickou instalaci.
- Tři skutečné negativní kontroly: změněný bajt podepsaného APK, starší E11 bez identity E12 a vývojové debug APK. Všechny byly odmítnuté; nevznikl platný záznam vydání. Nástroj také odmítá změněné zdroje, neuložený kód, jinou revizi archivu, neplatný typ verze nebo cizí veřejné piny.
- Všechny původní obsahové assets jsou bajtově stejné jako v E11. Room schémata, oprávnění a formát zálohy se nemění. Přibyl pouze samostatný generovaný údaj o sestavení. APK neobsahuje testovací třídy/syntetická data ani soukromé klíče.
- Původní WFP generace 4 prošel opětovnou kontrolou podpisu, 86 datových souborů a obsahu. Herní katalog a mapy se nepřegenerovaly.

Úplné výsledky jsou v qa/e12. Čtyři snímky skutečného nativního UI byly vizuálně zkontrolované: help-start.png, help-data4.png, help-recovery.png a help-large-text.png. Část textu nad akcí je při zachyceném posunu mimo výřez; celý panel se posouvá. Nejde o screenshoty fyzického telefonu. Zpráva „unimplemented“ v logu pochází z negativního testu poškozeného PNG E09; všechny hostitelské testy uspěly.

## Identita vydání

- APK SHA-256: 462e17f1ddb3e246307618294d00bd9e7f7ea2cedebe1211cc063ab3587ccb9d.
- Kód sestavení: b5db01b48e7c7bd2fa5fa22ac61b17138ef1fc7d.
- Otisk zdrojových vstupů: 063fdb8c774137fad29eaac0d0a280c94ab3a3871e8bf401b81dd88f62361de5.
- Certifikát SHA-256: e422650dc10efbf7074ae880c0cbc0d7e6d09c8d3ba6a1b1e761f99f441c2f6b.

Přesný záznam a volitelný datový balíček: release/E12_release.json. Zdrojový archiv může mít pozdější dokumentační commit s těmito výsledky; kód APK zůstává přesně výše uvedená revize. history.bundle uchovává obě revize. source-state.json dovoluje porovnat zdroje i po rozbalení bez .git. Vydání netvrdí bitovou reprodukovatelnost mezi různými nástroji nebo systémy.

## Obsah a provoz

APK má záložní generaci 2: 53 položek, 27 poloh, čtyři mapy. Po dřívějším nebo novém importu distribuce 4 je k dispozici 139 položek, 62 poloh a pět map. Data 4 mají stále stejné jméno, SHA-256 i vydavatelský podpis. Při aktualizaci APK se zvolená generace nepřepne automaticky. Room 2/3 a záloha 1 zůstávají zachované.

Chráněný ruční workflow je připravený ve zdrojích. Vzdálené prostředí, tajemství nebo distribuční server se zde nezakládaly a GitHub workflow se zde nespustil. Veřejný hosting ani Google Play nejsou nasazené. Pro používání APK není potřeba účet, podpisový archiv ani vývojové nástroje.

## Další krok

Na S24 Ultra ověřit samotnou aktualizaci, osobní postup, offline start, vynucené ukončení, největší písmo, systémové ovládání a dlouhé používání. Zbývá také druhé skutečné zařízení, TalkBack, živý HTTPS provoz a měření cílů E11. Poslední obecné potvrzení telefonu od Jendy je E05; žádná pozdější přejímka se nedoplňuje odhadem.

Potom pokračovat další ověřenou obsahovou dávkou E10 podle potřeb Warlocka. Dokončení programového seznamu etap E00–E12 samo nedokončuje herní pokrytí.
