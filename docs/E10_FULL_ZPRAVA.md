# E10 celek — ověřená dodávka

25. 10. 2026. Aplikace 0.15.0-e10-full / versionCode 15. Jeden APK soubor obsahuje data 7 pro referenční cestu Hordy a Warlocka 1–60. Přesný rozsah a známé mezery: E10_FULL_DATA.md. Jedna společná telefonní přejímka: E10_FULL_NAVOD.md. E10.3 má podle uživatele základní test potvrzený; z toho se neodvozuje kontrola této nové verze.

## Výsledky

- Úplné čisté sestavení: **BUILD SUCCESSFUL, 8 min 4 s, 150 úloh (149 provedených, 1 aktuální)**. Debug APK, release APK a AndroidTest APK sestavené.
- **206 testů aplikace / 52 sad, 0 chyb, 0 selhání, 0 vynechaných.** Zahrnuje všechny starší regresní testy a šest nových testů ve třech sadách: celý katalog, typy objektů a referencí, minimální verze, zálohy, přechod z potvrzené generace 6, návrat a obnovení, skutečný automatický start aplikace a ovládání vyšších úrovní/map.
- **27 Python testů úspěšných.** Kontrolují zdrojové identity, literal parser, 64bitové masky, add/remove souřadnicové dvojice, Forever příznaky služeb, zachování staršího obsahu, předpokladové skupiny bez zkrácení a přímo převzaté souřadnice bez druhé transformace.
- Lint debug **0 chyb / 3 upozornění**, release **0 chyb / 5 upozornění**. Zbývají upozornění na novější závislosti, paměťový atribut pro nižší API a kontrolu volného místa; není přidána síťová aktualizace závislostí. Při kompilaci se objevují starší deprekační/nullability upozornění.
- Podepsaný archiv: **12 352 položek, 18 441 poloh, 61 map, 1 103 dlaždic / 1 104 datových souborů**. Ověřen podpis, kontrolní součty, úplnost a dekódování PNG. **56 nových map složeno pixelově přesně**, všech **85 publikovaných starších dlaždic** beze změny.
- Ověřen skutečný výsledný APK: release, bez debug příznaku, původní vlastník a stejné ID jako E10.3, vyšší versionCode 15 > 14. Všechny zabalené assets přesně odpovídají zdrojům. Žádný soukromý klíč není v APK ani zdrojovém archivu.

## Nové průchody

Automatický přechod z již potvrzených dat 6 zachovává dva profily, stav The Binding a původní poznámku. Nová oblast Winterspring se ukládá do zálohy i profilu, přežije návrat na data 6 a restart. Obnovení přibaleného obsahu vrací data 7 a nezasahuje do osobních záznamů.

Hostitelský nativní UI test vybírá City of Dalaran a Winterspring, hledá Dreadsteed of Xoroth s filtrem Warlock, ukládá novou oblast postavy a obnoví ji po znovuvytvoření aktivity. Snímky jsou v qa/e10full. Zachycují hostitelský UI, nikoli telefon; vykreslení mapových podkladů v tomto hostitelském prostředí není obrazovým důkazem skutečného telefonu. Samotné dlaždice jsou kontrolované odděleně z přesného archivu. Mapový renderer a shlukování zůstávají původní.

V testovacím logu se mohou objevit CloseGuard zprávy databází vlastněných testovací Application a hláška dekodéru „unimplemented“ ze záměrného negativního PNG testu. Výsledek určuje úplné XML jednotlivých testů; žádný není vynechaný.

## Identita

- Kód sestavení: c24720b011e37dec0cfb3a4b65002379273da7d1.
- Build inputs SHA-256: 120215610f3bfc027d01fea735a418e3596a5f1137b0add1ba782739c5bb4b3b.
- APK SHA-256: 41dc2122d61ff3940b5a820261b0147164048b7407628c940b13bbd96d55d33e.
- APK: 146 680 214 bajtů (přibližně 147 MB / 140 MiB).
- Data 7 SHA-256: ba94fb218ad63cae250c7d32b32ef16532c4daa5b8ab822685d5b623a8cb563e, 121 073 335 bajtů.
- Certifikát APK SHA-256: e422650dc10efbf7074ae880c0cbc0d7e6d09c8d3ba6a1b1e761f99f441c2f6b.

QA dokumentace může mít pozdější Git commit; zdrojový archiv zachovává jak revizi sestavení, tak konečnou revizi dokumentace. Hranice vstupů APK je ověřená hashem, nezaměňuje se s aktuálním titulkem reportu.

## Zbývající přejímka

Implementační/obsahová dodávka E10 je hotová jako referenční průvodce 1–60. Formální herní M2/M3 nejsou uzavřené: žádná nová RELEASE pozorování, žádné ověřené dopravní hrany, známé mezery jsou popsány. Aktuální telefonní test celé E10 čeká na Jendu; měřené cíle E11, TalkBack a obnova na druhém telefonu nejsou vydávány za splněné.

Další nejvhodnější krok: instalovat jediné APK přes předchozí verzi bez odinstalování a udělat jednu společnou kontrolu podle návodu. Poté potvrzovat konkrétní třídní návaznosti při hraní.
