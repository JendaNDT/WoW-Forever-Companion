# E06 — výsledek a předání

Datum: 23. 10. 2026. Verze **0.6.0-e06**, kód **6**.

## Výsledek

Teď pro mě ukazuje osobní cíle, dostupné kroky, podklady k ověření, chybějící podmínky a odložené úkoly. Rozlišuje dostupnost od ručního stavu questu. Podmínky zahrnují profil, úroveň, profesi/režim, oblast, ALL/ANY, předchůdce, získání a volbu odměny. Dokončené a získané věci odpadnou. Záložka nepředstírá splnění ani dostupnost. Historické přání mimo katalog zachová název a zůstane neověřené.

Řazení Vyváženě / Objevování / Vybavení se ukládá a mění pouze pořadí. Důvod, zdroje a podmínky lze otevřít z každé karty; související detaily navazují na katalog a doložené mapové polohy. Režim Jen názvy a místa skryje podrobnosti do jednotlivého odkrytí. Kontrola Před odchodem odděluje otevřené cíle, chybějící předchozí kroky a nejistoty a nikdy neprohlašuje celou oblast za kompletní.

Mechanismus načítá zdrojovaná pravidla z místního katalogu, nepotřebuje AI klíč ani internet. Potvrzení vyžaduje úplné pravidlo a herní důkazy; Classic ani beta se splněním reference nepovýší na potvrzenou dostupnost. Časové okno má přesné hranice a pásmo, po skončení přestává být aktivním doporučením. Výlučná volba se opírá o konkrétní ověřenou skupinu.

## Ověření

- Čisté sestavení aplikace a instrumentační APK prošlo.
- **105 hostitelských testů uspělo, 0 selhalo, 0 vynecháno**, z toho **29 nových pro E06**. Starší kontroly E01–E05 zůstávají součástí běhu.
- Doména: nízká úroveň a pozdější dostupnost, změna rasy, frakce/povolání/oblasti, volitelné i požadované profese/režim, vnořené ALL/ANY a neznámá alternativa, odložení, předchůdci, zvolená odměna versus získání, výlučná větev, řazení, historické přání a přesný počet nově doplněných návazností. Vyšší úroveň sama neznačí zmeškání.
- Čas: testy těsně před začátkem, v začátku a přesně v konci okna; konec odstraní časové zvýraznění, zobrazení obsahuje pásmo. Obnova po skutečném uspání telefonu se zde neměřila.
- Data: skutečný SQLite katalog vytvořený z původního Room schématu 1 se převede na 2 se zachováním původních položek, zdrojů, map, poloh, vazeb a metadat. Pravidla se instalují a obnoví po zavření a novém otevření databáze. Simulovaná chyba při jejich zápisu vrátí celý starý katalog.
- Validace: chybějící důkaz/cíl, špatné typy, duplicity, neověřený odkaz uvnitř potvrzeného pravidla, neověřená odměna, prázdná skupina, neplatná úroveň, časové okno, pásmo, riziko, přílišná hloubka/počet uzlů a neznámý typ. Python se schématem přijme tutéž Kotlin serializovanou sadu a odmítne osm vadných variant.
- UI v nativním grafickém režimu hostitele: skutečný beta katalog zůstává k ověření; úprava úrovně/rasy ve formuláři přepočítá výsledek; dokončení a odložení změní seznamy; získané přání odpadne; NPC otevře skutečnou mapu; důvod je skrytý do jednotlivého odkrytí; řazení a prozrazování přežijí znovuvytvoření aktivity i nový ViewModel v krátkém okně na šířku.
- Android Lint: **0 chyb, 0 varování**. Sestavovací protokol je přiložený.
- Podpis APK je shodný s E05, ID `cz.jenda.wowforever`, minimum Android 8/API 26. Nepřibyla oprávnění k internetu, poloze nebo plošnému úložišti.

Protokoly, JUnit XML, podpis a snímky jsou v `qa/e06`. Soubory `fixture-available.png`, `fixture-reason.png` a `synthetic-guide-fixture.json` výslovně ukazují **cvičná testovací pravidla**, nikoliv skutečně potvrzené herní údaje. Ostatní nové snímky ukazují skutečný přibalený katalog. Instrumentační APK byla sestavená, na zařízení zde spuštěná nebyla.

## Kompatibilita a hranice

Osobní databáze i její schémata zůstávají beze změny ve verzi 2, záloha má stále formát 1. Herní Room databáze přidává tabulku pravidel migrací 1 → 2 bez destruktivní náhrady. Starší nastavení dostane výchozí Vyváženě a zachová aktivní profil i dřívější nastavení.

Herní podklady jsou **bajtově stejné jako E05**: 21 položek, osm beta poloh a čtyři mapy. Obsahují **0 úplných ověřených pravidel doporučení** a 0 nezávislých nových ověření ve hře. Aktuální APK tudíž nevydává cvičná dostupná doporučení za fakta: nabízí vlastní cíle a zřetelně oddělené podklady k ověření. Úplnost oblasti ani nejlepší výbavu neslibuje. Rozsah a formát popisuje `E06_DATA.md`.

Uživatel potvrdil obecné fungování E05 na telefonu. Přenos zálohy mezi dvěma skutečnými zařízeními nebyl výslovně potvrzen. Nová E06 čeká na telefonní přejímku podle `E06_TELEFON.md`, zvlášť systémové Zpět, orientace, obnovení po ukončení a čitelnost. Hostitelské testy nejsou měření výkonu telefonu ani skutečné ukončení Android procesu.

## Předání a další krok

`WoW_Forever_E06.apk` nainstaluj přes stávající verzi bez odinstalování. Zdrojový archiv obsahuje projekt, dokumentaci, výsledky a Git historii, bez soukromého podpisového klíče, SDK a sestavovací cache.

Nejdřív ověř osobní cíl, přepočet po dokončení, vysvětlení a kontrolu před odchodem na telefonu. Potom **E07 — zdrojovaný první obsah pro Durotar/Warlocka a přejímka prvního vydání M1**. Ověřená pravidla a počet doložených bodů musí vycházet ze skutečných podkladů.
