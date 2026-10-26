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

Všechny automatické kontroly prošly na revizi `e2193db6e75fed937d85f897fbff8449e7e48acb`:

- [Android checks 40346572619](https://github.com/JendaNDT/WoW-Forever-Companion/actions/runs/40346572619): **228 aplikačních testů / 59 sad**, bez selhání, chyb nebo přeskočení; **35 Python testů**. Lint debug i release bez chyb (17 / 18 upozornění). Sestavení debug APK, testovací APK a závěrečná kontrola identity a všech 93 přibalených souborů prošly.
- [E11 reliability 40346573124](https://github.com/JendaNDT/WoW-Forever-Companion/actions/runs/40346573124): **Android 8 / API 26 a Android 16 / API 36**, v každém 5 instrumentačních testů, dva skutečné force-stop/restarty a obnova zálohy. Zátěž všech 61 map / 17 330 bodů trvala 315 401 ms (915 změn oblastí / 15 cyklů) a 316 035 ms (854 změn / 14 cyklů); současně nejvýše dva zachované snímky mapy.
- Nové scénáře ověřují tři záložky, společný detail a návrat přes související záznamy, uchování hledání po přepnutí a otočení, pět posledních hledání, přechod staré navigace, detail při písmu 200 % a shodný zápis úkolu z různých obrazovek. Původní scénáře pro osobní záznamy, odměny, poznámky, balíčky a obnovu zůstávají zachované.
- Při přestavbě se opravilo i ukládání ručně vybrané oblasti při souběžném načtení profilu a zobrazení Co dál po návratu na balíček, který uloženou oblast neobsahuje. Uložená oblast se tím nepřepisuje.
- Vizuálně zkontrolované snímky Nastavení, Deníku, společného detailu a detailu při velkém písmu.

Souhrny skutečných výsledků: [qa/ui2](qa/ui2). Jde o emulátory a vývojové APK; stav vypnutých rádií není samostatně prokázaný. Výkon na fyzickém telefonu, TalkBack, druhé fyzické zařízení a správnost herních údajů nejsou tímto měřené.

## Předané APK

- Soubor: `Warlock_Forever_0.18.0_UI2.apk`, **150 471 714 B**.
- SHA-256: `74b16005dcf4da5bffa4eb2eb8adfe47b7131f66f8bb9262b0b28c0b06b97c29`.
- Otisk produkčních vstupů: `81c30e1da0d47a37e45773965c9ca579b5e98dc62a680bc0df5d605d5e99d58c`; 152 souborů, čistá a dohledatelná revize zdrojů. Následný zápis dokumentace produkční vstupy nemění.
- Vývojový podpis `ec5843957fe4444d55ab3adfb61b5b3ff423ac7f72fb323ae27508d6ea6fa98f` se liší od předaného APK Fáze 1. **Před odinstalací exportovat zálohu, poté provést čistou instalaci a obnovit zálohu.** Databáze, formát zálohy 1 a herní data 8 jsou zachované; přímá aktualizace přes předchozí podpis není možná.

Telefonní přejímka verze 19 zatím neproběhla. Potvrzení verze 18 je historické. Fáze 3 nezačala.

## Doporučená kontrola na telefonu

1. Otevřít Mapa / Úkoly / Hledat a Nastavení. Zkontrolovat, že postava, záložky, poznámky a postup zůstaly zachované.
2. Najít úkol, otevřít detail, označit Beru / Hotovo a porovnat záznam v Deníku.
3. Otevřít související NPC, vrátit se Zpět a použít Na mapě. Zavřít panel a ověřit návrat k rozepsanému hledání.
4. Vyzkoušet panel v poloviční a celé výšce, otočení telefonu, velké písmo a dostupnost zálohy v Nastavení.

Další krok po přijetí: Fáze 3 — značky mapy podle významu a pohodlí při hraní.
