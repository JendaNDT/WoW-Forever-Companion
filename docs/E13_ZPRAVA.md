# E13 — výsledek a předání

25. 10. 2026 · 0.13.0-e13 / kód 13 · cz.jenda.wowforever.guide.

## Co dostává uživatel

Jedno vlastnicky podepsané APK s dosavadními daty 5 přímo uvnitř. Nová instalace i aktualizace přes E09–E12 připraví obsah automaticky při otevření, bez internetu a výběru souboru. K dispozici je 182 položek, 74 poloh, pět map a 85 dlaždic. Osobní záznamy zůstávají zachované. Již importovaná stejná nebo novější data se nepřepisují.

Jenda potvrdil základní kontrolu E12 na telefonu. Samotná instalace této nové E13 na jeho telefonu zůstává k ověření. M1, herní prověření, výkonnostní cíle E11, TalkBack a druhé skutečné zařízení se z obecných zpráv neodvozují. Celý svět ani cesta 1–60 nejsou dokončené. Herní fakta a jejich označení beta/Classic zůstávají stejná jako v E10.2.

## Ověření finálního vydání

- Kompletní čisté sestavení debug APK, podepsaného release APK a instrumentačního APK, všech hostitelských testů a obou variant Lint uspělo za **7m 11s**. Log a XML jsou ze stejného finálního běhu v qa/e13.
- **192 testů aplikace prošlo, 0 selhalo, 0 chyb, 0 vynechaných**, celkem 46 sad. Třináct nových scénářů E13, včetně tří nativních UI průchodů. **13 Python testů prošlo.**
- Lint debug: **0 chyb / 2 varování**. Lint release: **0 chyb / 4 varování**. Přesné zprávy jsou ve validation-summary.json a obou lint XML. Stávající upozornění překladače na starší paměťovou konstantu a nullable Java File v testech nejsou potlačená; závislosti se neměnily.
- Skutečný podpis APK prošel ověřovačem; aplikace není debuggable. Oproti skutečnému E12 APK má stejný identifikátor a vlastnický certifikát, vyšší kód 13. To dokládá kryptografickou způsobilost aktualizace; nejde o fyzicky provedenou instalaci.
- Všech **126 vstupů APK** odpovídá metadatům uvnitř podepsaného souboru a skutečné uvedené revizi v Git historii. Celý přibalený WFP z výsledného APK byl vyjmut a znovu ověřen: původní vydavatelský podpis, všech 86 datových souborů, součty, katalog, všech 85 PNG a shoda s již publikovanými daty 5.
- Nové testy provádějí přechod 2/3/4 → 5, opakované otevření, zachování dvou profilů, hotových úkolů, předmětu, poznámky, výpravy a JSON zálohy. Již importovaná 5 se pouze potvrdí; novější podepsaná 6 s testovacím klíčem se nesnižuje. Test nepotřebuje soukromý produkční podpis.
- Návrat 5 → 4 zůstává vybraný i po znovuspuštění. Explicitní obnovení vybírá integrovanou 5 také z novější generace. Poškozená místní databáze se obnoví z APK přes dřívější potvrzení, osobní historie zůstává stejná. Přerušení aktivace, nedostatek místa a vadný přibalený archiv ponechají poslední funkční obsah.
- Produkční UI poprvé otevře všechna data bez importu a ukáže je v nápovědě/Pokrytí. Druhý UI průchod přebírá postavu, hotový úkol a poznámku z předchozí databáze. Třetí přes skutečná tlačítka vrací obsah na 2 a obnovuje přibalenou 5 bez výběru souboru a bez změny osobních záznamů.

Čtyři snímky skutečného hostitelského nativního UI (integrated-coverage, integrated-help, preserved-profile a restored-bundled) byly vizuálně zkontrolované. Nejde o nové screenshoty fyzického telefonu. Starší regresní testy záměrně zachovávají tehdejší manuální start v LegacyGuideApplication; nové E13 testy výslovně používají skutečnou produkční aplikaci. Žádný test nebyl vynechaný. Hláška dekodéru „unimplemented“ patří původnímu negativnímu testu poškozeného PNG.

## Identita a obnova zdrojů

- APK: WoW_Forever_E13.apk, **25588842 bajtů**.
- SHA-256 APK: edc1740c4165835f95824dea34afe23b1df0a774b7243fab40f1e96f5d43dec8.
- Revize kódu: b98abed9cfb1542806446786c30d0cc21cec0505.
- Otisk vstupů APK: 09bf4b6d9b21955c1a67618e24cf95d1a33d25941fbe3b2fe8efdc0ee8894c6b.
- Vlastnický certifikát SHA-256: e422650dc10efbf7074ae880c0cbc0d7e6d09c8d3ba6a1b1e761f99f441c2f6b.
- Přibalený WFP 5: 27b70643d39c0c832f12da4f4ceac1aa57fda19fbf7fac533d84fb3e73aab725, přesně původní obsahový vydavatel E09.

Receipt: release/E13_release.json. Pozdější dokumentační commit může obsahovat tyto výsledky; zdrojová revize APK zůstává výše uvedená. Archiv má úplný history.bundle a source-state.json pro ověření kódu, obnovu historie i sestavení. Soukromé klíče E09 jsou beze změny, uchované odděleně; do APK/archivu se nebalí. Bitová reprodukovatelnost mezi různými nástroji není tvrzená.

README a NAVOD.md popisují aktuální instalaci jednoho APK. Technický postup: E13_DATA.md. Nouzový základ 2 zůstává uvnitř pro případ selhání přípravy; běžný integrovaný obsah je 5. Schémata Room a formát zálohy zůstávají stejné. Vzdálené workflow je pouze připravené, žádný veřejný distribuční server ani obchod nejsou tímto nasazené.

## Další krok

Nainstalovat E13 přes E12 bez odinstalování, otevřít ji a ověřit známý hotový úkol a poznámku. Data se připraví sama. Potom pokračovat další zdrojovanou dávkou Durotaru a Warlockových třídních kroků.

## Následné potvrzení uživatele

25. 10. 2026 po předání APK Jenda uvedl: „Ok, vše funguje“. Základní kontrola E13 na telefonu je potvrzená uživatelem. Měření výkonu, TalkBack, druhé zařízení a herní ověření se tímto výrokem nepotvrzují. Následující obsahová dávka je E10.3.
