# E10.3 — výsledek a předání

25. 10. 2026 · 0.14.0-e10.3 / kód 14 · cz.jenda.wowforever.guide.

## Výsledek

Jedno podepsané APK obsahuje nová data 6. Instalace přes E13 bez odinstalování automaticky připraví 216 položek a 84 poloh na pěti mapách. Není potřeba soubor WFP vybírat, importovat ani být online. Přibylo 16 úkolů, deset NPC s polohou a osm předmětů. Všech 182 předchozích položek i jejich původní zdroje, polohy, vztahy a pravidla zůstávají stejné.

Sever Durotaru, orčí Voidwalkerova řada a vybrané odměny jsou rozšíření výběru, nikoli kompletní Durotar nebo cesta 1–60. Nová data zahrnují šest explicitních Classic poloh a čtyři beta pozorování. Classic body mají značku C, příponu v názvu a stav v detailu i ve výpravě. Jejich spojení se nepočítá jako ověřené. Neověřené, konfliktní ani obecné komunitní souřadnice se na mapu nepouštějí.

Jenda potvrdil E13 na telefonu slovy „Ok, vše funguje“. Fyzická kontrola teprve vzniklé E10.3 zůstává po předání. Měřený výkon, TalkBack, druhé zařízení a herní ověření se netvrdí. M1/M2/M3 zůstávají otevřené.

## Provedené kontroly

- Úplné čisté sestavení debug APK, vlastnicky podepsaného release APK, instrumentačního APK, všech hostitelských testů a obou Lint variant prošlo za **7m 15s**. Všechny výsledky jsou ze stejného finálního běhu.
- **200 testů aplikace prošlo, 0 selhání/chyb/vynechání**, 49 testovacích sad. Osm nových scénářů E10.3. **17 Python testů prošlo**, čtyři nové negativní/obsahové scénáře. Dřívější testy automatického startu E13 nyní kontrolují aktuální produkční archiv 6; historické distribuční testy 2/3/4/5 zůstávají s původními počty.
- Lint debug: 0 chyb / 3 varování. Lint release: 0 chyb / 6 varování. Přesné zprávy jsou v XML. Stávající upozornění překladače na paměťovou konstantu a nullable Java File v testech nejsou potlačená; závislosti se neměnily.
- Skutečný výstupní APK je release a není debuggable, má vlastní původní podpis E09+. Proti skutečnému E13 APK má stejný identifikátor a certifikát a vyšší kód 14. Tím je ověřená způsobilost aktualizace, nikoli fyzická instalace na Jendově telefonu.
- Všech 126 vstupů APK odpovídá vložené identitě a skutečné čisté revizi Git. Z podepsaného APK byl znovu vyjmut právě nový WFP a ověřen vydavatelský podpis, katalog, manifest, všech 85 PNG, součty a shoda s veřejnými zdroji balíčku. Data 6 mají minimumAppCode=14; starší APK je odmítne. Katalog a manifest nemohou požadavek snížit.
- Přechod z již potvrzených E13 dat 5 respektuje staré bundled-receipt a automaticky aktivuje jiné ID manifestu 6. Zachovává oba profily, hotový úkol, výběr odměny, poznámku, plán výpravy i JSON export. Předchozí 5 zůstává pro návrat. Po návratu se nová historie nesmaže, chybějící cíl nemá falešnou polohu a návrat přetrvá přes znovuspuštění; obnovení přibaleného obsahu vrací 6 a cíl znovu ukáže.
- Upravený katalog s původním podpisem nepřepne generaci ani nezmění osobní data. Dosavadní negativní testy nadále pokrývají vadný archiv, nedostatek místa, přerušení aktivace a obnovu poškozené cache.
- Nativní hostitelské UI otevírá hůl, šestímístnou brašnu, správnou orčí Creature of the Void a Gan’rulovu Classic polohu na mapě Orgrimmaru. Automatický start, nápověda, pokrytí, převzetí osobního profilu a skutečná tlačítka návratu/obnovení jsou také otestované.

Osm snímků hostitelského nativního UI je v qa/e10c; nejde o screenshoty fyzického telefonu. Kompletní XML, log, Lint a ověřovací JSON jsou uchované. Negativní test vadného PNG může do logu vypsat hlášku dekodéru „unimplemented“; výsledky jednotlivých testů se řídí XML.

## Identita vydání

- APK: WoW_Forever_E10_3.apk, 25601834 bajtů.
- SHA-256 APK: 0fd576611165324f4d38f05e44544a6211024e34713df9c82b6bd790518036ac
- Zdrojová revize APK: 9e3467c2d6b7b0ed3b6f83675ba92b5c9468bf99
- SHA-256 vstupů: 7fdbeee5be9a1699cc685bd5c05c91b38a91c7484d46169ceea32ad906851d0d
- SHA-256 integrovaného WFP: 7aea05bc9b683b49616962f485195482b6ebe355e7efa52d8082cb7a91316411
- Certifikát APK SHA-256: e422650dc10efbf7074ae880c0cbc0d7e6d09c8d3ba6a1b1e761f99f441c2f6b

Skutečný receipt: release/E10_3_release.json. Následný dokumentační commit nemění vstupy APK. Zdrojový archiv obsahuje history.bundle a source-state.json; soukromé podpisové klíče E09 zůstávají oddělené a nebyly znovu vytvořené. Bitová reprodukovatelnost mezi různými nástroji není tvrzená. Vzdálené workflow je pouze připravené ve zdrojích, nebyl spuštěný veřejný hosting, obchod ani GitHub distribuce.

## Další krok

Instalovat E10.3 přes E13 bez odinstalování, otevřít, ověřit vlastní postup a vyhledat Creature of the Void nebo Makeshift Stormcaller. Potom doporučujeme další zdrojovanou dávku severních Barrens pro úrovně přibližně 10–20.
