# E08 — výsledek a předání

23. 10. 2026. **0.8.0-e08 / kód 8**. Obsah generace 2.

## Výsledek

Výprava přidává hledání a přidávání katalogových cílů, opakované návštěvy, šipky pořadí, odebrání a otevření detailu nebo správné mapy. Čísla vycházejí z celého plánu a plánované značky zůstávají mimo seskupování. Bez jednoznačné polohy se zachová seznam a název; žádné souřadnice se nevymýšlejí. Změna výpravy nemění postup úkolů ani získání předmětů. Osobní objevení dopravy lze zaznamenat a zrušit zvlášť pro profil.

Dopravní model, validace a výpočet pracují se směry, doloženými podmínkami a oběma odemknutými letovými body. Výsledek rozlišuje vlastní pořadí od ověřeného spojení. Nepočítá vzdálenost vzdušnou čarou ani falešný pěší průchod přes překážku. Detaily a omezení jsou v E08_DATA.md.

## Ověření

- Čisté sestavení aplikace i instrumentační APK prošlo.
- **129 hostitelských testů uspělo, 0 selhalo, 0 vynecháno**, z toho **15 nových pro E08**. Android Lint: **0 chyb, 3 varování**. Přesná hlášení jsou v qa/e08; připnuté závislosti se v této etapě neměnily.
- Nativní grafické UI: skutečný katalog přidá Hrauga a Groska, plán se uloží, mapa ukáže čísla, zrušení/přesun opakované návštěvy přepočítá značky, seznam přežije znovuvytvoření aktivity a beta objevení se nezmění na ověřenou hranu. Cvičná potvrzená letová trasa se ukáže až po nastavení obou koncových bodů.
- Doména: směry a cykly, nejmenší počet doložených úseků, zamčený/odemknutý let, požadovaná úroveň, ALL/ANY, neznámé podmínky, profese a režim, hotový versus odložený předpoklad, beta důvěryhodnost, chybějící uzly, chybějící/víceznačné polohy a zachování globálního číslování.
- Data: opakované návštěvy a historie neznámých ID, normalizace děravého importovaného pořadí, nezávislost profilů, nové otevření diskové databáze, přenos starým formátem zálohy do jiné databáze, zrušení objevení a úplný rollback simulovaně poškozeného přepisu pořadí.
- Skutečný soubor podle původního Room schématu 2 se převede na 3 se zachováním metadat, položky a zdroje; prázdný graf se dekóduje. Potom se uloží validovaný graf vyšší generace a po novém otevření zůstává stejný. Starší úplná migrace katalogu 1 → aktuální 3 je rovněž součástí běhu.
- Kotlin odmítne falešný release důkaz, chybějící cíle/polohy/zdroje, duplicitní uzly/hrany, let bez obou discovery klíčů, neplatný čas a pěší přechod do jiného rámce. Explicitní doložený vstup mezi rámci je povolený. Python přijme tutéž Kotlin serializovanou sadu a odmítne devět vadných variant, včetně neznámé oblasti v podmínce.
- Osobní schémata 1/2 a stará herní schémata 1/2 zůstávají bajtově stejná. Nová herní databáze 3 přidává jediné dopravní metadata pole; záloha zůstává formát 1. Všech 68 mapových dlaždic prošlo kontrolou integrity.
- Katalog přibalený v APK je bajtově shodný s E07. Podpis odpovídá E07 a starším verzím. Nevznikla oprávnění k internetu, poloze nebo plošnému úložišti. Cvičné dopravní údaje nejsou v APK.

Snímky synthetic-verified.png a synthetic-transport-fixture.json v qa/e08 obsahují **výslovně cvičný testovací graf**, nikoli ověřený obsah hry. Ostatní nové snímky ukazují skutečný katalog a vlastní plán. Instrumentační APK byla sestavená, zde na zařízení spuštěná nebyla.

## Omezení a M1

Produkční graf má **0 uzlů a 0 ověřených hran**. Dostupné zdroje bety a Classicu neprokazují celou reálnou cestu s oběma konci a všemi podmínkami. E08 tedy poskytuje použitelný očíslovaný plán 27 doložených bodů, ale netvrdí, že již naviguje po ověřených herních trasách. Víceznačnou polohu ani vlastní poznámku zatím nelze vybrat jako samostatný přesný cíl.

Jenda výslovně požádal E08 bez potvrzení celého průchodu M1. Přejímka M1 zůstává otevřená; poslední obecné potvrzení telefonu je E05. Neproběhlo fyzické ověření E08, skutečný process kill, výkon telefonu, přenos mezi dvěma fyzickými zařízeními ani nezávislá kontrola herních podmínek. Žádná starší nejistota nebyla tímto vývojem označená za vyřešenou.

## Předání a další krok

WoW_Forever_E08.apk nainstaluj přes stávající verzi bez odinstalování, po exportu osobní zálohy z telefonu. Zdrojový ZIP obsahuje projekt, výsledky, dokumentaci a Git historii; neobsahuje soukromý podpisový klíč ani tvůj profil z telefonu.

Nejvhodnější další krok: E08_TELEFON.md a otevřená přejímka M1. Potom **E09 — správce balíčků, kompatibilita, kontrolované aktualizace a návrat k poslední fungující generaci**.
