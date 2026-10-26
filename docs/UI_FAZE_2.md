# Jednodušší ovládání — Fáze 2

Verze 0.18.0-ui2 (19), data 8. Navazuje na telefonem potvrzenou Fázi 1.

## Rozsah

- Tři hlavní záložky Mapa / Úkoly / Hledat. Nastavení je dostupné ozubeným kolečkem ze všech obrazovek.
- Úkoly obsahují Co dál / Deník. Co dál má rozpracované a osobní cíle, Warlock, Tady, sbalené Brzy a neúplné údaje. Podmínky a nejistota patří do společného detailu; osobní záznamy, záložky, poznámky a výprava do Deníku.
- Jediné celostránkové hledání pro katalog i mapu: typ, Warlock, dungeony, vlastní záložky, oblast, moje úroveň i původní pásma úrovní. Položky mají společný detail a odkazy Na mapě jen při doložené poloze. Prázdné hledání ukazuje vlastní poznámky a pět posledních dotazů. Rozepsaný dotaz a filtry zůstávají při přepnutí záložek.
- Jeden vytahovací detail pro hledání, úkoly, mapové body i výpravu. Související záznamy nahrazují obsah téhož panelu a mají historii Zpět. Na běžné obrazovce dvě výšky, při velkém písmu nebo malém okně celá dostupná výška. Stav úkolu je nahoře; dále Kde, návaznosti a odměna, popis a zdroje. Míra spoilerů a ochrany navazujících úkolů jsou zachované.
- Jedno Nastavení: Postava, Profily a záloha, Zobrazení, Balíčky, Pokrytí, Návod, O aplikaci. Při otevření pomocného okna je předchozí okno nahrazené; společný detail nevrství další dialogy.
- Stará uložená navigace NOW / JOURNEY / CHARACTER se převádí na QUESTS / SETTINGS s respektováním posledního navštíveného cíle. Nové údaje jsou jen ve zpětně kompatibilním nastavení Session; databáze, herní balíčky ani zálohy se nemění.

## Ověření

Probíhá Android checks a E11 reliability ve větvi codex/ui-faze-2. Zachovaná sada testů je upravená na nové uživatelské cesty; neodstraňuje se ověřování osobních záznamů, odměn, poznámek, balíčků nebo obnovy. Nové scénáře ověřují tři záložky, společný detail a návrat přes související záznamy, hledání po přepnutí a otočení, pět posledních hledání, přechod staré navigace, velké písmo a shodný zápis úkolu z různých obrazovek.

Telefonní přejímka této verze zatím neproběhla. Fáze 3 nezačala.

## Doporučená kontrola na telefonu

1. Otevřít Mapa / Úkoly / Hledat a Nastavení. Zkontrolovat, že postava, záložky, poznámky a postup zůstaly zachované.
2. Najít úkol, otevřít detail, označit Beru / Hotovo a porovnat záznam v Deníku.
3. Otevřít související NPC, vrátit se Zpět a použít Na mapě. Zavřít panel a ověřit návrat k rozepsanému hledání.
4. Vyzkoušet panel v poloviční a celé výšce, otočení telefonu, velké písmo a dostupnost zálohy v Nastavení.

Další krok po přijetí: Fáze 3 — značky mapy podle významu a pohodlí při hraní.
