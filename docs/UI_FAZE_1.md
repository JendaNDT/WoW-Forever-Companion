# Jednodušší ovládání — Fáze 1

Verze 0.17.0-ui1 (18), data 8. Realizace první fáze z NAVRH_UI.md.

- Horní lišta: − / + pro úroveň 1–60 a výběr oblasti. Série klepnutí se sloučí po 300 ms; přijaté klepnutí během zápisu se uloží následně. Další změny osobních dat při čekajícím zápisu nejsou povolené.
- Detail: Beru / Hotovo / Později nad popisem, původní ochrany návazností a odměn zachované. Beru a Hotovo také přímo na kartě doporučení.
- Seznam podle úrovně a oblasti zahrnuje i nepotvrzené reference. Samostatně sbalené Brzy (+3 až +5) a záznamy s neznámou úrovní nebo oblastí. NPC a předměty patří do katalogu a osobního deníku.
- Dostupnost UNVERIFIED se nepřepisuje na AVAILABLE. Vysoká úroveň nebo neznámá oblast se nevydává za vhodný místní úkol. Výslovně rozpracované a osobní cíle zůstávají viditelné.
- Jedno zavíratelné upozornění na nejistotu podkladů v doporučeních; rozpory a potvrzení jsou rozlišeny na kartě, plné zdroje a nejistota zůstávají v detailu.
- Režim a profese se zobrazí jen pokud je používají aktivní pravidla; uložené hodnoty se nemažou.
- Otáčení mapy vypnuté, historické natočení se při vytvoření mapy vynuluje. Tlačítko N a trvalá nápověda pod mapou odstraněné. Přiblížení i copyright zachované.
- Návod a O aplikaci používají skutečnou verzi a aktivní generaci dat.

Navigace má nadále čtyři záložky. Herní katalog, balíčky, databázové schéma i formát JSON záloh zůstávají beze změny.

## Ověření

[Android checks 40331668550](https://github.com/JendaNDT/WoW-Forever-Companion/actions/runs/40331668550) prošel: **221 aplikačních a 35 Python testů, 0 chyb a 0 vynechaných testů**, Lint debug i release bez chyb, sestavení obou testovacích APK a ověření všech 93 přibalených souborů. [E11 reliability 40331668566](https://github.com/JendaNDT/WoW-Forever-Companion/actions/runs/40331668566) prošel na Androidu 8 / API 26 i Androidu 16 / API 36: na každém 5 nativních testů, 2 skutečná ukončení procesu, obnova osobních dat a pětiminutový průchod všemi 61 mapami. Výsledek neprokazuje fyzický telefon ani skutečně vypnutá rádia emulátoru.

Sestavení pochází z revize `546da51a899810fe6a5822055cacdddd326ef209`, otisk produkčních vstupů `14f59d50e4ea3a57874a50e473e81467b1deeca512aae7c6aeaa44be2fdff92a`. APK má SHA-256 `eb31185fc5002e7dbcacf82fd1a2a0d49aa1987536eae8e06017559de164fb7c`, velikost 150 471 718 B a vývojový podpis. Podpis se nemusí shodovat s předchozí instalací; před čistou instalací vyexportovat JSON zálohu. Souhrny důkazů jsou v [qa/ui1](qa/ui1).

Regresní test nad skutečnými daty 8 má pro Orka úrovně 1 v Durotaru 13 návrhů v běžném seznamu, 28 v Brzy a 2 s neznámými údaji. Dostupnost zůstává UNVERIFIED. Snímky mapy, detailu a seznamu z testů byly vizuálně zkontrolované.

 Přidané regresní scénáře ověřují reference v datech 8, hranice úrovní, neznámé údaje, rychlá klepnutí, uložení oblasti, zachování dalších polí, rekreaci obrazovky a dokončení úkolu přímo ze seznamu. Telefonní přejímka nové verze zatím neproběhla.

## Vyzkoušení na telefonu

1. Několikrát rychle klepnout na +, změnit oblast a znovu spustit aplikaci. Úroveň i oblast musí zůstat uložené.
2. V Teď pro mě porovnat úroveň 1 a 10 v Durotaru; na úrovni 1 se v běžném seznamu nesmějí objevit úkoly pro 60.
3. Otevřít úkol a použít Beru / Hotovo nahoře nebo přímo v seznamu; zkontrolovat záznam v Moje cesta.
4. Ověřit mapu, velké písmo a orientaci telefonu. Před změnou instalace vyexportovat JSON zálohu.

Další krok po potvrzení na telefonu: Fáze 2 (Mapa / Úkoly / Hledat a společné Nastavení).
