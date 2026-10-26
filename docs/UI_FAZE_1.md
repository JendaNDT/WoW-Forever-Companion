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

Probíhá úplný Android checks a E11 reliability ve větvi codex/ui-faze-1. Přidané regresní scénáře ověřují reference v datech 8, hranice úrovní, neznámé údaje, rychlá klepnutí, uložení oblasti, zachování dalších polí, rekreaci obrazovky a dokončení úkolu přímo ze seznamu. Telefonní přejímka nové verze zatím neproběhla.

## Vyzkoušení na telefonu

1. Několikrát rychle klepnout na +, změnit oblast a znovu spustit aplikaci. Úroveň i oblast musí zůstat uložené.
2. V Teď pro mě porovnat úroveň 1 a 10 v Durotaru; na úrovni 1 se v běžném seznamu nesmějí objevit úkoly pro 60.
3. Otevřít úkol a použít Beru / Hotovo nahoře nebo přímo v seznamu; zkontrolovat záznam v Moje cesta.
4. Ověřit mapu, velké písmo a orientaci telefonu. Před změnou instalace vyexportovat JSON zálohu.

Další krok po potvrzení na telefonu: Fáze 2 (Mapa / Úkoly / Hledat a společné Nastavení).
