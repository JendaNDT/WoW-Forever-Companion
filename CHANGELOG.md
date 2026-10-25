# E10.3 · 0.14.0-e10.3 · 2026-10-25

## 2026-10-25 — Oprava pádu E10 / 0.15.1-e10-fix1 / kód 16

- Oprava paměťové chyby doporučení: sdílené dropy nepropojují každé NPC se všemi ostatními zdroji téhož předmětu.
- Postupné čtení velkého katalogu, reuse dekódovaných dat, dávkové zápisy a čtení databáze bez duplicitních kopií.
- Offline archiv uložený bez druhé komprese; čtení z APK přes deskriptor a obnovení existující ověřené generace.
- Obsah dat 7, podpisy, profily a postup zůstávají zachované. Doplněné testy s omezenou pamětí.


## 2026-10-25 — E10 celek / 0.15.0-e10-full / kód 15

- Jeden automaticky přibalený referenční katalog Horda / Warlock 1–60: 12 352 položek, 2 818 úkolů, 18 441 poloh a 61 map.
- Připnuté QuestieDB vstupy se statickým Forever rozdílem, bezpečný literal parser, audit mezer a zachované licence.
- Herní objekty, cíle úkolů, zdroje předmětů a příprava na dungeony. Neznámé odměny se nemění v potvrzenou volbu.
- Vyhledávatelný výběr map, filtry katalogu, indexy vazeb a výpočty velkých doporučení mimo hlavní vlákno.
- Zachování původních dat a postupu; nová společná přejímka na telefonu. Zdroje zůstávají referencemi, ne herním důkazem.


- Nová přibalená generace 6: dalších 16 úkolů, deset NPC s polohou a osm předmětů; celkem 216 položek a 84 poloh na pěti mapách.
- Sever Durotaru, orčí cesta k Voidwalkerovi, Rezlak, protijed, Margoz, obojky a samostatné oko; vybrané hole a šestímístná beta brašna.
- Šest Classic poloh zůstává označených, čtyři nové polohy pocházejí z beta pozorování. Podmínky a návaznosti neprohlašují ověření vydané hry.
- Automatická aktualizace i z již potvrzeného integrovaného obsahu E13; zachování osobního postupu, návratu a záloh.
- Základní fungování E13 na telefonu potvrzené uživatelem. E10.3 má vlastní kontrolu po instalaci.

# Historie vydání

## 2026-10-25 — E13 / 0.13.0-e13 / kód 13

- Jediné APK obsahuje všechna dosavadní data 5 a při prvním spuštění je připraví automaticky bez internetu a výběru souboru.
- Přechod ze starších dat zachovává osobní záznamy a předchozí obsah pro návrat. Stejná nebo novější importovaná generace zůstává vybraná.
- Ruční návrat se při dalším spuštění nepřepisuje. Obnovení přibaleného obsahu vybírá data 5.
- Aktuální návod a ověření archivu přímo z podepsaného APK. Podpis aplikace, datové rámce, Room schémata a herní fakta se nemění.


## 2026-10-25 — obsah 5 / E10, druhá dávka

- Aplikace zůstává 0.12.0-e12. Úplný podepsaný obsah lze importovat bez nového APK.
- Startovní výběr 1–10 rozšířený o 18 úkolů, 12 NPC/nepřátel, pět látkových odměn a osm prvních milníků kouzel.
- Celkem 182 položek, 74 poloh a pět map. Starší záznamy, rámy map a osobní postup jsou zachované.
- Legging It / Forgotten Loa Idols mají správná rozdílná NPC při odevzdání. The Admiral’s Orders 830/831 se neslučují a jejich řetězec zůstává Classic referencí.
- Samostatný český startovní kontrolní seznam, zdroje a kontrola skutečného importu, návratu a osobní zálohy.
- Základní telefonní kontrola E12 potvrzená uživatelem; nové data 5 a měřené cíle vyžadují vlastní kontrolu.


## 0.12.0-e12 — 25. 10. 2026

Testovací vydání pro dosavadní obsahový rozsah. Návod je dostupný offline v aplikaci, s přímými odkazy do záloh, balíčků a pokrytí. Uvádí skutečnou aktivní generaci a zachová se po znovuvytvoření obrazovky. APK obsahuje dohledatelnou revizi a otisk zdrojových vstupů; samostatný ověřovací nástroj kontroluje skutečný podpis, shodu kódu a kompatibilitu s předchozí verzí. Release sestavení odmítne chybějící nebo cizí klíč a neuložené změny kódu. Připravený ručně spouštěný workflow používá samostatné chráněné prostředí; vzdáleně nebyl spuštěný.

Herní data zůstávají generace 2 v APK a volitelná generace 4 z E10. Žádné nové ověření herních údajů nebo dokončení obsahu 1–60. Telefonní přejímka M1/E11 a výkonové cíle jsou otevřené. Instalace přes E09–E11 bez odinstalování; E08 se přenáší JSON zálohou do souběžné větve.

## 0.11.0-e11 — 25. 10. 2026

Indexované hledání, nejvýše dva uchovávané stavy map, uvolnění neaktivní mapy při nedostatku paměti, úpravy velkého písma, obnova poškozeného nastavení a bezpečné dokončení změny obsahu při neúspěšném úklidu. 169 hostitelských testů, měření na počítači a připravené nástroje pro Android.

## 0.10.0-e10 — 24. 10. 2026

První rozšířená obsahová dávka, podepsaný balíček generace 4: výběr Orgrimmaru a severních Barrens, příprava RFC/WC, odměny a Warlockovy milníky. 139 položek, 62 poloh, pět map; minimum aplikace E10. Přibalený fallback zůstává generace 2.

## 0.9.0-e09 — 24. 10. 2026

Správa podepsaných datových balíčků, náhled, atomické přepnutí, návrat a kontrola celistvosti. Nový stálý vlastnický podpis a identifikátor cz.jenda.wowforever.guide po ztrátě původního dočasného klíče prostředí. Starší větev se zachovává souběžně. Kontrolní generace 3 mění jen číslo generace.

## Dřívější etapy

E01–E08 přidaly nativní mapu, datový katalog, profil, hledání, filtry, poznámky, osobní postup, zálohy, doporučení a plán výpravy. Podrobné výsledky jsou v docs/E01_ZPRAVA.md až docs/E08_ZPRAVA.md. Poslední obecné potvrzení fungování telefonu od uživatele je E05; automatické dokončení pozdějších etap není fyzická přejímka.
