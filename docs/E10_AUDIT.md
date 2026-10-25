# Audit před vydáním – 25. 10. 2026

Aplikace **0.16.0-audit (17)**, data **8**. Audit veřejných podkladů nevyžaduje přístup ke hře a není potvrzením pravidel vydané hry.

## Výsledek

- **11 912 položek, 2 799 úkolů, 17 330 poloh, 10 299 skupin vazeb, 61 map, 1 103 dlaždic**. Index rozpoznává **115 warlockových úkolů**.
- Opraveno pořadí importu: základ → zděděné statické opravy → generovaný Forever rozdíl → beta traces → ruční Forever opravy. Dříve chyběly zděděné opravy a traces. Parser bezpečně čte literály a dlouhé komentáře, cizí Lua se nespouští.
- Proti datům 7 přibylo 303 klíčů a 743 přestalo odpovídat opravenému výběru. To neznamená odstranění těchto entit ze hry. U společných položek se změnilo 45 minimálních úrovní, 23 uváděných úrovní a 9 názvů. Přesný rozdíl: `research/e10audit/audit-results.json`.
- Warcraft Recorder obsahuje 1 609 questových záznamů. S katalogem se překrývá 749; všech 749 názvů souhlasí. Dva rozpory v úrovni zůstávají v aplikaci jako **CONFLICT**: 95328 Whispering Horror Residue (13 / 10) a 96130 Chakuyak (5 / 8), s oběma zdroji a vysvětlením.
- Všech 14 přibalených kouzel bylo porovnáno s klientským přehledem ForeverChanges, build 1.60.1.70532 (174 ranků). Úrovně odpovídají; doplněna chybějící úroveň Summon Imp.
- Spor o počet předmětů u 1485 Vile Familiars zůstává otevřený. Seznam úkolů ani klientský přehled počty cílů nedokládají.
- Osobní databáze, stabilní klíče a formát záloh se nemění. Vyřazení reference nemaže její záznam z deníku. Test zahrnuje aktualizaci 7 → 8 i návrat na starší data se zachováním takového záznamu.

## Meze ověření

Vydáním potvrzených položek je stále **0** a ověřených dopravních hran také **0**. Dynamické opravy závislé na postavě a fázi se neprovádějí. Úplné podmínky, dropy, počty cílů, odměny, aktuální výskyty a doprava se ověří až po spuštění 4. listopadu. Chybějící beta záznam neznamená nedostupnost. Shoda dvou webů se společným Classic původem není nezávislý důkaz živé hry. Ručně zpracované beta údaje se nepřepisují pouze kvůli rozdílu oproti Classic podkladu.

Úplný questový JSON ForeverChanges nebylo možné načíst; audit netvrdí jeho úplné porovnání. Mapové obrazy zůstávají původní. Již převedené souřadnice se netransformují podruhé a přehledy instancí nedostávají vymyšlené vnitřní polohy. Obrazová integrita není důkaz správnosti ve vydané hře.

## Zdroje a reprodukce

- [QuestieDB](https://github.com/Questie/QuestieDB/tree/5c094f6e9a264f0dc54e2fde9731a37a97ed197c), 29 původních a 14 doplněných souborů. Revize, Git blob identity a SHA-256 jsou v obou `upstream/snapshot.json`.
- [Warcraft Recorder – úkoly](https://warcraftforever.games/quests), faktický snímek `recorder-quest-index.json`.
- [ForeverChanges – Warlock](https://foreverchanges.pro/spellbook/warlock), faktický snímek `foreverchanges-warlock-ranks.json`, bez kopírování popisů kouzel.
- Protokol a notebook: `research/e10audit/audit-results.json`, `research/e10audit/audit.ipynb`.

`python tools/audit_content.py` reprodukuje katalog. `python -m unittest discover -s tools -p 'test_*.py'` ověří data; `python tools/verify_bundled.py` kontroluje podpis a obrazové podklady.

Podepsaný WFP je bezeztrátově rozdělený na `.wfpart` soubory kvůli limitu GitHubu. Android je čte postupně, celý obsah zůstává uvnitř APK a platnost podpisu se nemění. Historický balíček 7 je pod `research/e10full` pro regresní testy a nezvětšuje APK. Katalogy 7 a 8 jsou uložené jako `.json.gz`, testy je čtou přímo. Sestavení Androidu nepotřebuje Python ani stahování herních dat.

GitHub import obsahuje úplný sestavitelný pracovní strom, testy, licence a zdroje. Původní historie s nadlimitními WFP je v dříve předaných zdrojových archivech. Nová historie GitHubu začíná úplným importem. Soukromé podpisové klíče do repozitáře nepatří.

Uživatel potvrdil kompletní funkční kontrolu verze 16 po opravě pádu. To neznamená test nové verze 17, měřené FPS, TalkBack, druhý telefon ani herní přejímku. Původní archiv neobsahoval protokoly nového sestavení. Obnova a následné ověření jsou popsané v [záznamu importu](GITHUB_IMPORT.md); aktuální výsledky jsou v GitHub Actions.
