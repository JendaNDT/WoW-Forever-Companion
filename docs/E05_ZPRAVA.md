# E05 — výsledek a předání

Datum: 23. 10. 2026. Verze **0.5.0-e05**, kód **5**.

## Výsledek

Detail úkolu ukládá čtyři stavy a Moje cesta je filtruje v deníku. ALL a ANY počítají jen dokončené předchůdce. Odložené úkoly návaznost nesplní. Neověřené nebo vypnuté pravidlo zůstává neověřené i po splnění uvedených předchůdců. Změna dokončeného předchůdce s navazujícími záznamy vyžádá potvrzení a nesmaže jejich historii.

Získání předmětu, záložka/přání a volba odměny mají oddělené záznamy. Změna existující odměny vyžaduje potvrzení. Poslední změnu úkolu, získání nebo odměny lze vrátit i po novém otevření databáze; novější poznámky se tím nepřepisují. Detail nabízí vazby do souvisejících úkolů, NPC, doložených poloh na mapě a Moje cesta.

Export uloží všechny profily do přenosného JSON formátu 1 přes systémový výběr dokumentu. Před obnovou je náhled. Výchozí import vytváří nové profily a nové identity poznámek, zachová stávající data. Výslovné nahrazení profilu a vytvoření místního snímku jsou jediná transakce. Poslední tři snímky lze po potvrzení obnovit. Starší osobní databáze se migruje z verze 1 na 2 bez smazání údajů.

## Ověření

- Čisté sestavení aplikace a instrumentační APK prošlo.
- **76 hostitelských testů uspělo, 0 selhalo, 0 vynecháno**, včetně **25 nových pro E05**.
- Doménové testy: ALL/ANY, odložení a rozpracování, přepočet po vrácení, neověřené i vypnuté pravidlo, ověřené/neověřené výlučné skupiny, větvení a cyklus při hledání dopadu, oddělení stejnojmenných úkolů podle ID.
- Datové testy: skutečný soubor vytvořený podle původního schématu 1 přešel na 2; zachoval profil i všech šest původních osobních kolekcí a dovolil zápis do nové tabulky odměn. Schéma katalogu i původní schéma osobních dat zůstávají nezměněné.
- Přenos zálohy: zdrojová databáze se dvěma profily se uzavře, přenosný soubor se načte do jiné nezávislé databáze. Ověří se všech sedm kolekcí osobních řádků, neznámá historická ID a názvy, oddělení profilů a kolize původního ID poznámky. **Nejde o test dvou fyzických zařízení.**
- Obnova: simulovaná chyba SQL při zápisu poznámky vrátí všechny tabulky, metadata i částečný snímek. Samostatně prochází nahrazení, vrácení snímku, zachování jiného profilu a limit tří snímků.
- Validace: novější formát, jiná varianta, cizí vlastník, duplicity, neúplná poloha, poškozené UTF-8, neznámé pole, uříznutý soubor, velikost a hloubka JSON. Odmítnutí importu a překročení počtu profilů zachová data.
- Nativní grafické UI hostitele: stavy → znovuvytvoření aktivity → vrácení → filtr deníku; potvrzení změny předchůdce a odměny; samostatné získání a záložka předmětu; vytvoření/otevření dokumentu přes skutečné Android kontrakty s hostitelsky dodaným URI; náhled bez zápisu → nový profil → přepnutí zpět; obnova snímku v krátkém okně na šířku; zrušení výběru a vadný soubor bez zápisu.
- Android Lint: **0 chyb, 0 varování**. Protokol případných aktualizací závislostí je přiložený.
- Podpis APK shodný s E04; ID `cz.jenda.wowforever`, minimum Android 8/API 26. Bez oprávnění k internetu, poloze nebo plošnému čtení/zápisu úložiště. Soukromý klíč se nedodává.

Protokoly, JUnit XML, podpis a snímky rozhraní: `qa/e05`. Testy běžely hostitelsky, UI přes Robolectric Native Graphics. Instrumentační APK se sestavila, na telefonu/emulátoru zde spuštěná nebyla. Skutečné ukončení OS procesu, poskytovatel dokumentů a přenos mezi fyzickými zařízeními se tím nepotvrzují.

## Obsah a zbývající přejímka

Herní balíček se nemění: 21 položek, osm beta poloh NPC a čtyři mapy povrchu. Žádná minimální úroveň ani nové herní pravidlo nebyly odhadnuty. Žádná návaznost se nepoužívá jako potvrzené automatické vedení.

Výlučné questové skupiny se načítají z instalovaného katalogu a vyhodnocují symetricky. Import odmítá chybné typy, sebeodkaz, neplatný režim a chybějící herní důkaz aktivního pravidla. Test skutečné instalace do Room, načtení i blokování ve ViewModelu prošel se syntetickými pravidly. Současný uživatelský balíček neobsahuje doloženou skupinu; žádná smyšlená větev proto nebyla přidána. Potvrzení změny questové odměny je v aplikaci funkční už nyní. Úplný seznam odměn ani nejlepší výbavu současné podklady nedokládají.

Uživatel dříve potvrdil E01 a E02. Nová E05 vyžaduje telefonní přejímku podle `E05_TELEFON.md`, zejména skutečný export, přenos na druhé zařízení a obnovu. Do té doby není splněné fyzické přejímací kritérium zálohy z implementačního plánu. Technický popis a formát: `E05_DATA.md`.

## Předání a doporučený další krok

`WoW_Forever_E05.apk` instaluj přes předchozí verzi bez odinstalování. Zdrojový ZIP obsahuje projekt, dokumentaci, výsledky a `history.bundle`, bez soukromého klíče a sestavovací cache.

Nejdřív ověř jeden úkol a přenos zálohy do nového profilu na druhém Android zařízení. Potom **E06 — osobní doporučení Teď pro mě a seznam před odchodem z oblasti**, s vysvětlením podmínek a nejistoty zdrojů.
