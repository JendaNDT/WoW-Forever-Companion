# Aktuální stav projektu

Aktualizováno **26. 10. 2026**. Automatické ověření spolehlivosti **E11 dokončeno**; [výsledky a meze ověření](E11_AKTUALNI_OVERENI.md). Aktuální aplikace je **0.16.0-audit / kód 17**, přibalená data **8**. Celý sestavitelný projekt je ve větvi `main` repozitáře [JendaNDT/WoW-Forever-Companion](https://github.com/JendaNDT/WoW-Forever-Companion). Původ a obnovu popisuje [GITHUB_IMPORT.md](GITHUB_IMPORT.md).

## Potvrzeno na telefonu

Jenda po předání APK pro čistou instalaci uvedl **„Ok, funguje to“**. Tím je zaznamenané základní funkční potvrzení verze 17. Uživatel předtím výslovně přijal čistou instalaci místo aktualizace původní aplikace.

Předané APK odpovídá revizi `d99734dea435c517553d13442f1e0f89fd0b250e`; SHA-256 je `cc4776e0fde062985c62233fce65336d5575876a6f5625700e98fc00dcb86fa4`. Jde o vývojově podepsanou variantu. Čistá instalace nedokládá aktualizaci přes původní vlastnický podpis ani zachování postupu při takové aktualizaci.

Obecné potvrzení „funguje to“ není měření výkonu ani samostatný protokol každého scénáře. Telefonní měření startu, odezvy, snímků a paměti, TalkBack, obnova na druhém zařízení a správnost údajů ve hře zůstávají otevřené.

## Oprava pádu a obsah

Dříve hlášený pád při přípravě offline obsahu je opravený od verze **0.15.1 / kód 16**; její funkční kontrolu uživatel potvrdil již dříve. Oprava je zachovaná i ve verzi 17. Historické technické výsledky jsou v [E10_FIX_ZPRAVA.md](E10_FIX_ZPRAVA.md); problém se nevede jako nová nevyřešená chyba.

Aktuální referenční katalog Horda / Warlock 1–60 má **11 912 položek, 2 799 úkolů, 17 330 poloh, 61 map a 1 103 dlaždic**. Obsah je přímo v APK, ruční import není nutný. Rozsah auditu, rozdíly proti datům 7 a známé nejistoty jsou v [E10_AUDIT.md](E10_AUDIT.md). Počet údajů ověřených ve vydané hře a ověřených dopravních hran zůstává **0**; herní M2/M3 nejsou uzavřené.

## Automatické ověření E11

[Android checks](https://github.com/JendaNDT/WoW-Forever-Companion/actions/runs/40291857619) úspěšně ověřil sestavení, data, **213 aplikačních testů a 35 Python testů**, obě varianty Lint a všechny přibalené soubory. Žádný test neselhal ani nebyl vynechaný.

[Nativní běh Androidu 8 a 16](https://github.com/JendaNDT/WoW-Forever-Companion/actions/runs/40291857502) ověřil skutečná ukončení procesu, všechny typy osobních záznamů, import záloh, nahrazení, návrat přes místní snímek a odmítnutí částečné obnovy při chybě zápisu. Oba systémy prošly pětiminutovým střídáním všech 61 map. [Rozšířený Android 8](https://github.com/JendaNDT/WoW-Forever-Companion/actions/runs/40289701001) navíc zvládl 3 477 změn map za 20 minut a 10,9 sekundy. Podrobnosti a původní důkazy jsou v [E11_AKTUALNI_OVERENI.md](E11_AKTUALNI_OVERENI.md).

Dlouhý běh Androidu 16 přerušilo vypnutí runneru; nepočítá se jako dokončený. Automatická přejímka nepředstírá měření výkonu Samsungu, TalkBack, obnovu na druhém fyzickém zařízení ani herní přejímku. Produkční zdrojové vstupy a obsah jsou shodné s předaným funkčním APK, **nová instalace není kvůli E11 potřebná**.

## Další nejvhodnější krok

Pokračovat herní přejímkou: ověřit návaznosti a podmínky úkolů v úvodní trase Hordy / Warlocka, zejména třídní úkoly a dopravu. Postupně doplňovat doložené herní výsledky a otevřené rozpory zdrojů. Technická spolehlivost sama správnost herních údajů nepotvrzuje.

Skutečné telefonní měření výkonu a přístupnosti zůstává samostatně popsané v [E11_TELEFON.md](E11_TELEFON.md); jeho neprovedené části se neoznačují za splněné.
