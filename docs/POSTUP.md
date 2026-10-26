# Aktuální stav projektu

Aktualizováno **26. 10. 2026**. Aktuální aplikace je **0.16.0-audit / kód 17**, přibalená data **8**. Celý sestavitelný projekt je ve větvi `main` repozitáře [JendaNDT/WoW-Forever-Companion](https://github.com/JendaNDT/WoW-Forever-Companion). Původ a obnovu popisuje [GITHUB_IMPORT.md](GITHUB_IMPORT.md).

## Potvrzeno na telefonu

Jenda po předání APK pro čistou instalaci uvedl **„Ok, funguje to“**. Tím je zaznamenané základní funkční potvrzení verze 17. Uživatel předtím výslovně přijal čistou instalaci místo aktualizace původní aplikace.

Předané APK odpovídá revizi `d99734dea435c517553d13442f1e0f89fd0b250e`; SHA-256 je `cc4776e0fde062985c62233fce65336d5575876a6f5625700e98fc00dcb86fa4`. Jde o vývojově podepsanou variantu. Čistá instalace nedokládá aktualizaci přes původní vlastnický podpis ani zachování postupu při takové aktualizaci.

Obecné potvrzení „funguje to“ není měření výkonu ani samostatný protokol každého scénáře. Telefonní měření startu, odezvy, snímků a paměti, TalkBack, obnova na druhém zařízení a správnost údajů ve hře zůstávají otevřené.

## Oprava pádu a obsah

Dříve hlášený pád při přípravě offline obsahu je opravený od verze **0.15.1 / kód 16**; její funkční kontrolu uživatel potvrdil již dříve. Oprava je zachovaná i ve verzi 17. Historické technické výsledky jsou v [E10_FIX_ZPRAVA.md](E10_FIX_ZPRAVA.md); problém se nevede jako nová nevyřešená chyba.

Aktuální referenční katalog Horda / Warlock 1–60 má **11 912 položek, 2 799 úkolů, 17 330 poloh, 61 map a 1 103 dlaždic**. Obsah je přímo v APK, ruční import není nutný. Rozsah auditu, rozdíly proti datům 7 a známé nejistoty jsou v [E10_AUDIT.md](E10_AUDIT.md). Počet údajů ověřených ve vydané hře a ověřených dopravních hran zůstává **0**; herní M2/M3 nejsou uzavřené.

## Automatické ověření

[Android checks, běh 40231787304](https://github.com/JendaNDT/WoW-Forever-Companion/actions/runs/40231787304) úspěšně ověřil revizi předaného APK: kontroly dat, 32 Python testů, sestavení Debug a testovacího APK, jednotkové testy, Lint obou variant, identitu APK a všech přibalených souborů. Sestavení testovacího APK není spuštění instrumentačních testů na telefonu.

## Další nejvhodnější krok

Dokončit zbývající ověření spolehlivosti E11 nad plným katalogem: uložení a obnovení postupu, export a obnova zálohy, opětovné spuštění po ukončení procesu a delší používání map. Funkční kontrolu oddělit od měřených výkonových cílů; pro ně je připravený postup v [E11_DATA.md](E11_DATA.md). Historické zprávy E11 popisují tehdejší sestavení a menší obsah, současný rozsah určuje tento dokument a [stage_status.json](stage_status.json).

Po technické kontrole pokračovat ověřováním konkrétních návazností úkolů, warlockových úkolů a dopravy přímo ve hře. Dosavadní funkční potvrzení neoznačovat za herní přejímku.
