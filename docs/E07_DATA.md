# E07 — obsah pilotu a původ dat

Stav kontroly zdrojů: **23. 10. 2026**. APK 0.7.0-e07 / kód 7. Katalog formátu 1 přechází z generace 1 na **2**. Herní a osobní Room databáze zůstávají ve verzi 2; přenosná záloha ve formátu 1. Původní ID a osobní historie se zachovávají.

| Druh | Celkem | Nové v E07 |
|---|---:|---:|
| NPC | 28 | 19 |
| Úkoly | 19 | 10 |
| Předměty | 4 | 3 |
| Kouzla | 1 | 0 |
| Dopravní reference | 1 | 0 |
| Přímé mapové polohy | 27 | 19 |
| Vazby | 49 | 24 |
| Zdroje | 59 | 33 |
| Referenční pravidla | 8 | 8 |

Všechny přímé polohy jsou NPC na povrchu Durotaru. Questy se otevírají přes související NPC; poloha zadavatele se nevydává za místo splnění. Nartok nemá doložený bod. Čtyři offline mapy mají původní build 1.60.1.69893, rozměr 1002 × 668 a 68 dlaždic. Ostatní oblasti mají pouze mapový podklad.

## Co bylo doplněno

Hraug jako beta trenér démonů v The Den; Brakk a camping; Duokna, Huklah a profesní služby ve Valley of Trials, Razor Hill a Sen’jin. U trenérských titulů nejsou odhadnuté seznamy schopností, recepty ani podmínky. Správce vzducholodi není automaticky potvrzená dopravní hrana.

Deset nových úkolů: Sting of the Scorpid 789, Galgar’s Cactus Apple Surprise 4402, Lazy Peons 5441, Thazz’ril’s Pick 6394, A Peon’s Burden 2161, Sarkoth 790 a 804, The Adventurer 96652, The Great Outdoors 96604 a Camping 101: Cooking 96655. Dvě varianty Sarkoth a tři Vile Familiars se nerozplývají do stejného záznamu.

Galgarův beta záznam rozlišuje jídlo jako odměnu a volbu jedné ze tří příruček. Pilot přidává tři příručky 247840 / 247841 / 247846 a pouze jejich doložené vazby REWARD_CHOICE na 4402. Nevytváří z nich skupinu vzájemně výlučných questů ani tvrzení o nejlepší profesi. Ruční evidence odměny, získání a přání zůstávají odlišné.

The Adventurer uvádí Lost Journal jako zdroj zadání. Nevytváříme NPC se jménem předmětu ani souřadnice deníku. U The Great Outdoors není doložený NPC odevzdání; vazba END_NPC se nedoplňuje odhadem.

## Co zdroje skutečně potvrzují

Warcraft Forever je **neoficiální fanouškovský web**, nikoli Blizzard. Stránky NPC a questů výslovně uvádějí záznamy bety pomocí ForeverLog. U každého nového záznamu je konkrétní URL, datum kontroly, stručný faktický výpis a stav BETA. Build jednotlivého pozorování zůstává null. Devatenáct nových poloh souhlasí s dříve archivovanými mapovými značkami téhož vydavatele; nejde o druhé nezávislé herní ověření.

EverythingQuests na připnutém commitu uvádí předchůdce 788 → 789, 790 → 804 a 5441 → 6394. Jde o referenční Classic tabulku. Všechny tři nové vazby jsou CLASSIC a routingEnabled=false; návaznosti ve Forever z toho neprohlašujeme za potvrzené. Starší čtyři referenční návaznosti zůstávají také neaktivní. Cíl dvou nezávisle herně ověřených návazností z plánu **dosud není splněný**.

Wowhead Forever u úkolu 1485 uvádí minimum 1, Hordu, Warlocka, Orka/Nemrtvého/Trolla a učení Summon Imp. Minimální úroveň tohoto jediného úkolu je nyní 1. Záznam je dál CONFLICT, protože dřívější komunitní komentář uváděl osm hlav proti šesti v klientském zadání. Staré rozporné podklady se nemažou. Minimum úrovně ostatních questů je null; hlášená úroveň úkolu se nepřevádí na minimum přijetí.

## ForeverChanges od Jendy

Prověřené stránky:

- https://foreverchanges.pro/about
- https://foreverchanges.pro/class/warlock
- https://foreverchanges.pro/spellbook/warlock
- https://foreverchanges.pro/quests

Web uvádí původ v beta klientu, Classic klientu, cMaNGOS a dalších databázích. Výslovně přiznává, že mnohé questové údaje zůstávají z Classicu. Proto nepřevádíme celou databázi na ověřený obsah Forever.

K Summon Imp přibyl konkrétní zdroj knihy kouzel build **1.60.1.70532**, která uvádí úroveň 1. To není důkaz úplných podmínek získání či nabídky konkrétního trenéra. Starší mapy se nepřeznačují novějším buildem. Přehled pokrytí v aplikaci má přímý odkaz na změny Warlocka; zdroj knihy kouzel je i v detailu Summon Imp. Celý web se do APK nekopíruje.

## Osm důvodů k ověření

Warlockovy kroky 1485 a 3090, Hraug, Galgarovy příručky, tři campingové questy a hostinský Grosk mají český zdrojovaný důvod. Každé pravidlo výslovně obsahuje neznámé podmínky. Nepoužívá se always, falešná minimální úroveň, smyšlené časové okno ani nevratné varování. Všechna pravidla zůstávají neověřená; Teď pro mě je zařadí K ověření. Dokončený předchůdce nezmění Classic referenci na jistotu. Vlastní cíle, splnění, přání a odložení se vyhodnocují běžným způsobem.

## Zbývající pokrytí a M1

Není potvrzená úplná dostupnost questů, dvě herní návaznosti, aktuální nabídka trenérů, poloha Nartoka/deníku/cílů v jeskyni, kompletní doprava, dungeony, tajná místa ani výbava oblasti. **0 položek a 0 návazností je nezávisle ověřeno ve vydané hře.** Počet 27 bodů není tvrzení o úplnosti. Průchod na skutečném Androidu a přenos osobní zálohy na druhé zařízení čeká na Jendu. E07 je sestavitelný obsahový pilot k přejímce, nikoli uzavřená přejímka M1.

Úpravy jsou kurátorované v content/e07-additions.json; tools/build_content.py je znovu spojí s neměnným E00. Historické testy používají přesnou kopii starého katalogu v test/resources/legacy-e06.json. Nové testy naopak kontrolují skutečný přibalený katalog a aktualizaci generace 1 → 2. Hledání i přepnutí filtru záložek nyní vrátí seznam na začátek, aby nový výsledek nezůstal mimo výřez po starém posunu. Testovací data se do uživatelského katalogu neinstalují.
