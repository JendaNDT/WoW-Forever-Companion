# E04 — mapa propojená s katalogem

## Rozsah skutečného obsahu

Obsahový balíček se nemění: čtyři mapy povrchu, osm beta poloh postav v Durotaru a 21 položek katalogu. Vrstvy obsahují dostupné kategorie a vlastní poznámky. Přímé polohy úkolů, předmětů, kouzel a dopravy v tomto balíčku chybí; odkazy na mapu se u nich nevymýšlejí. Vazby převzaté z jiných podkladů se nepoužívají jako potvrzené souřadnice.

## Identity, více poloh a patra

Mapový bod odděluje vlastní `id` od `entryKey`. Jedna položka katalogu může mít více poloh, včetně různých map a pater. První poloha v pořadí stabilních klíčů zachovává původní ID položky pro kompatibilitu s osmi body E03; další mají složené ID položky a polohy. Záložky se vztahují k položce, takže zahrnou všechny její polohy.

Mapový rámec identifikuje dvojice `uiMapId:floorId`. Oddělené jsou stav rendereru, výřez, převody souřadnic a seznam bodů každého rámce. Přepínač pater se zobrazí, pokud má mapa více podkladů. Současný balíček má pouze Povrch: žádný obrázek patra dungeonu není dodaný. Více pater a transformace jsou ověřeny oddělenými testovacími daty, která nejsou součástí uživatelského katalogu.

Konvence podkladů: povrch `maps/<uiMapId>/<zoom>/<row>/<col>.png`, další patra `maps/<uiMapId>/floor_<floorId>/<zoom>/<row>/<col>.png`. Povolování dalších obsahových balíčků vyžaduje také rozšíření dnešního záměrně úzkého validátoru a kontrolu nových podkladů v obsahové etapě. Aktuální import stále připouští jen čtyři prověřené mapy původního balíčku.

## Vrstvy a načítání

Vrstva skutečně mění množinu značek mapového enginu i seznam Body. Vyřazené značky se odstraňují, nikoli pouze překrývají. Kategorie se ukládají do DataStore společně s volbou Jen záložky katalogu. Poznámka není záložkou katalogu a tento filtr ji může skrýt. Vyhledání konkrétní polohy zapne její kategorii a v případě potřeby vypne filtr záložek, aby byl zvolený bod viditelný. Skrytí vybraného bodu zavře jeho detail.

Dlaždice načítá MapCompose pro výřez. Strategie Clustering současně provádí odložené vykreslování značek podle viditelné oblasti s okrajem a jejich seskupování. Toto chování bylo ověřeno ve zdrojích přesně použité verze 3.2.0, soubor `ui/markers/Clusterer.kt`, a native zátěžovými testy. Po přepnutí filtrů se nemění přiblížení a mapový engine se nevytváří znovu.

Malý katalog 21 položek a osm souřadnic se po spuštění načte jako metadata do paměti. E04 nepřidává prostorový SQL index pro tento malý vzorek; omezení se týká především dlaždic a skutečně vykreslených značek. Výkon kompletního světa touto etapou prokázán není.

## Vyhledávání a propojení

Hledání sjednotí velikost písmen a diakritiku. V katalogu musí být nalezena všechna hledaná slova; názvy a aliasy mají přednost před českými popisy. V poznámkách se hledá zadaná fráze. Stejnojmenné úkoly zůstávají samostatné položky. Mapa umožňuje hledat ve všech položkách a poznámkách nebo jen v aktuální oblasti a patře. Katalog má vlastní vyhledávací pole.

Jediná přímá poloha z výsledku otevře mapu, vybere značku a vycentruje ji. Více poloh nabídne detail katalogu s jednotlivými mapovými odkazy. Bez doložené přímé polohy se otevře detail s vysvětlením. Detail mapového bodu odkazuje zpět do katalogu; tím jsou dostupné popis, záložka a zdroje stejné položky.

Nastavení prozrazování z E03 dál skrývá popisy. Hledání zobrazuje názvy a informaci o poloze, nikoli automaticky odkryté úryvky skrytých popisů.

## Přesnost a původ

Každá doložená poloha nese `precisionPercent` a stav zdroje. Hodnota do 1 bodu souřadnic se označuje jako podrobná poloha; nad 1 jako přibližná s výraznější značkou a symbolem ≈. Zobrazené ± je údaj podkladu, nikoli nezávisle změřená přesnost ve hře. Všech osm současných poloh má z podkladu 0,1 a stav beta. Stav beta se nemění na ověření ve vydané hře.

## Vlastní poznámky

Dlouhý stisk mapy nebo značky používá normalizované mapové souřadnice vrácené nativním rendererem. Inverzní transformace je převede do herní soustavy 0–100 daného rámce. Zápis mimo platný rozsah se nenabízí. Poznámka obsahuje text do 2000 znaků, vlastní UUID, profil, mapu, patro, souřadnice a čas změny.

Poznámky mají zelenou hranatou značku a jsou přístupné i přes Moje cesta. Úprava zachová ID a polohu. Smazání má potvrzení a SQL podmínku na profil. Uložení a odstranění probíhají mimo hlavní vlákno, neúspěch ponechá rozpracovaný text a ukáže chybu. Prázdný text nelze uložit.

Rozepsaný text a poloha editoru přežijí znovuvytvoření aktivity pomocí rememberSaveable a SavedStateHandle. Potvrzené poznámky jsou v existující tabulce `notes` osobní databáze. Room schémata a identity zůstávají shodné s E03 (verze 1), takže není potřeba datová migrace. Původní JSON DataStore z E03 načte výchozí vrstvy a patro 0; původní výřezy povrchu mají zpětně kompatibilní čtení. Nové výřezy se ukládají podle dvojice mapa/patro.

Použitá nativní komponenta: [MapCompose 3.2.0](https://github.com/p-lr/MapCompose/tree/v3.2.0), bez nových závislostí a bez WebView. Oprávnění k internetu ani k poloze telefonu nejsou potřeba.
