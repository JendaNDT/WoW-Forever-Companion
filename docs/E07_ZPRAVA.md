# E07 — výsledek a předání obsahového pilotu

23. 10. 2026. **0.7.0-e07 / kód 7**, katalog generace **2**.

## Výsledek

Katalog má **53 položek, 27 poloh, 49 vazeb, 59 zdrojů a osm referenčních pravidel**. Oproti E06 přibylo 19 NPC/poloh, deset questů, tři profesní příručky a konkrétní kontrola Summon Imp podle Jendova zdroje ForeverChanges. Nový dialog v katalogu uvádí pokrytí a mezery. Hraug se dá najít česky a otevřít na mapě. Galgarovy tři příručky mají vlastní ID a volby odměny. Podklady z bety zůstávají k ověření.

Neměnné stabilní klíče zachovávají předchozí postup. Aktualizuje se pouze katalog generace 1 → 2, osobní a herní databázové schéma zůstávají ve verzi 2. Formát zálohy 1 je zachovaný. Nové údaje a zdrojové kompromisy popisuje E07_DATA.md.

## Ověření

- Čisté sestavení APK i instrumentační APK prošlo.
- **114 testů uspělo, 0 selhalo, 0 vynecháno**, z toho **9 nových pro E07**. Android Lint: **0 chyb a 1 varování**.
- Nové kontroly skutečného katalogu: původní ID, datovaná evidence, skutečné souřadnice Hrauga, žádné vymyšlené body cílů, rozlišení stejnojmenných úkolů, tři správné příručky, neaktivní Classic návaznosti, vyhodnocení bety jako nejistoty i po dokončení reference a oddělení nového buildu knihy kouzel od starších map.
- Z diskových databází se ověřil přechod katalogu generace 1 → 2, znovuotevření a shoda všech sedmi osobních kolekcí/profilu a přenosného exportu. Simulované selhání při vložení nového NPC vrátí celý starý katalog; opakovaný správný import uspěje a je idempotentní.
- Nativní grafické UI hostitele: přehled skutečného pokrytí, nulové potvrzené vydané návaznosti, české hledání trenéra démonů a otevření skutečné mapy; tři odměny Galgara, první výsledek po změně vyhledávání bez starého posunu seznamu, samostatné přání příručky a zachování po znovuvytvoření aktivity. Snímky jsou v qa/e07.
- Přesná shoda přibaleného katalogu s exportovanou kopií, validace Python/JSON schématem a opakovatelné přegenerování bez sítě. Původní archivovaný katalog pro regresní testy je bajtově shodný s E06.
- Všech 68 mapových dlaždic znovu prošlo kontrolou integrity. Podpis APK je shodný s E06, bez nových oprávnění k síti, poloze či plošnému úložišti. Syntetické testy nejsou v uživatelském katalogu ani assets APK.

První pokus o souběžné sestavení narazil na poškozený mezilehlý classes.jar. Výsledná kontrola používá nové čisté sestavení s jedním pracovníkem; nešlo o chybu získání nových knihoven. Lint upozorňuje na novější dostupnou Activity Compose proti připnuté 1.10.1. E07 závislosti nemění; tato informace nebrání úspěšnému sestavení. Soukromý podpisový klíč není v archivu.

## M1 ještě čeká na skutečnou přejímku

**M1 není označené za dokončené.** E07 je připravený obsahový pilot k instalaci. Jenda zatím výslovně potvrdil obecné fungování E05; E06 a E07 nemají samostatně potvrzený celý telefonní průchod. Zde se nehrál klient, nebyl ověřen skutečný Android process kill ani přenos zálohy na druhém fyzickém zařízení. Hostitelské UI a dvě databáze tyto kontroly nenahrazují.

27 bodů je nad číselným cílem pilotu, ale všech 27 jsou beta polohy NPC. Katalog zastupuje i questy, předměty, kouzlo a dopravní referenci. Není splněný cíl dvou nezávisle herně ověřených návazností. 0 položek a 0 návazností má důkaz z vydané hry. Neověřené minimum, poloha nebo kategorie se nedoplňují odhadem. Podmínky, trenérské nabídky, jeskynní cíle, kompletní doprava a další herní pokrytí zůstávají omezené.

## Předání

Nainstaluj WoW_Forever_E07.apk přes stávající verzi bez odinstalování, po vlastním exportu osobních dat z telefonu. Zdrojový ZIP obsahuje projekt, testy, dokumentaci, výsledky a Git historii. Zdrojový ZIP neobsahuje uživatelův profil z telefonu.

Nejvhodnější další krok je **průchod E07_TELEFON.md** včetně druhého Androidu a zaznamenání chybějících herních důkazů. Potom E08: řazení zastávek a číslování mapy, následně jen doložené dopravní hrany.
