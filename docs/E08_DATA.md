# E08 — výprava a dopravní graf

23. 10. 2026. Aplikace 0.8.0-e08 / kód 8. Přibalený obsah je bajtově stejný jako E07: generace 2, 53 položek, 27 beta poloh, čtyři mapy, 49 vazeb, 59 zdrojů, osm referenčních pravidel. Nevzniká nový smyšlený herní obsah.

## Co výprava ukládá

Seznam je samostatný pro každý profil. Cíl se přidá z detailu katalogu nebo přes Výprava → Přidat cíl. Šipky mění pořadí; odebrání řádku nemění splnění úkolu, získání předmětu, záložku, odměnu ani poznámku. Je dovolená opakovaná návštěva stejného NPC. Aplikace přidává nejvýše 200 cílů; delší starší import nekrátí a dovolí v něm řadit/odebírat.

Zápis používá existující planned_stops: profileId, position, entryKey, nameSnapshot. Přidání, přesun a odebrání se provedou v jediné databázové transakci. Při přepisu se pořadí sjednotí na 0…N−1 a nevznikají kolize primárních klíčů. Selhání vložení vrací celý původní seznam. Neznámé historické ID a jeho uložený název se zachovají; aktuální katalog může po přejmenování zobrazit nový název.

Uživatelská čísla 1…N vycházejí z celého seznamu, včetně cílů v jiných mapách a bez polohy. Na jednotlivé mapě se nečísluje znovu. Značka se přidá pouze ke konkrétní katalogové položce s jedinou doloženou polohou a přijatelným beta/release zdrojem. Quest automaticky nedostane souřadnice svého zadavatele; uživatel si může v souvisejících záznamech vybrat konkrétní NPC jako vlastní cíl.

Chybějící, odstraněná nebo víceznačná poloha nevytvoří bod ve středu mapy. Více poloh jedné položky E08 nezvolí odhadem; individuální výběr polohy a vlastní poznámky jako samostatné cíle zatím nejsou podporované. Pořadí cíle v seznamu se přesto zachová.

## Čísla na nativní mapě

Plánovaný bod má číslo a odlišný tvar značky. V přístupném popisu je název a všechna pořadová čísla návštěv. Dvě návštěvy jednoho NPC používají například 1·3, více návštěv zkrácený štítek s úplným přístupným popisem. Plánované značky se neseskupují; zaměření jiného bodu jejich výjimky nezruší.

Vrstvy a filtr záložek nadále platí. Tlačítko Mapa u zastávky zapne její vrstvu a otevře správnou oblast/patro i zaměření. Přepnutí profilu překreslí čísla podle jeho vlastního plánu. Mapový popisek vysvětluje, že jde o pořadí cílů. **Nekreslí se přímé spojnice, které by mohly působit jako průchozí terén.**

## Dopravní data

Obsahový JSON formátu 1 dostává volitelný transportGraph. Pokud chybí, čte se prázdný graf. Nový dopravní balíček vyžaduje E08 nebo novější aplikaci; kompatibilitu v distribučním manifestu řeší navazující E09. Staré soubory se dál načítají.

| Záznam | Podstatná pole |
|---|---|
| TravelNode | Stabilní klíč travel-node, český název, přesné locationKey, sourceIds, volitelné discoveryKey typu TRAVEL. |
| TravelEdge | Stabilní klíč travel-edge, vlastní TRAVEL položka, from/to, režim, úplná podmínka, stav ověření, zdroje, volitelný odhad sekund. |
| TransportGraph | Nejvýše 1000 uzlů a 1000 směrovaných hran. |

Režimy: pěšky, letový bod, loď, vzducholoď, portál a vstup. Opačný směr má vlastní záznam a důkaz. Stejná poloha nemá dva různé uzly. Každá směrovaná hrana má vlastní identitu položky pro dostupnost. Pěší spoj nesmí beze změny režimu přecházet mezi souřadnicovými rámci nebo patry; explicitní vstup takové propojení umožňuje.

Potvrzená hrana vyžaduje skutečný release důkaz u spoje, uzlů a jejich poloh, potvrzené vlastní položky i odkazované questy/předměty/objevení. Podmínky kontroluje stejný validátor jako pravidla doporučení: ALL/ANY, úroveň, frakce, povolání, rasa, režim, profese, oblast, splněný quest, získaný předmět a konkrétní odměna. Chybný cíl, typ, duplicita, chybějící zdroj, neplatný čas nebo falešný release příznak odmítne celý balíček. Zdrojované always je výslovné pravidlo; unknown zůstává neprůchozí.

Herní Room databáze přechází **2 → 3** přidáním transportJson do package_metadata s výchozím prázdným objektem. Původní schémata 1/2 se nepřepisují. Graf se uloží ve stejné transakci jako katalog a jeho metadata. DataStore se nerozšiřuje.

## Jak se hledá spojení

Výpočet použije pouze RELEASE hrany s platnými podmínkami. Neznámý režim/profese ani odložený či rozpracovaný quest předpoklad nesplní. U FLIGHT musí být ve vlastním profilu zaznamenané odemknutí **obou koncových bodů**. Záznam objevené vzducholodi se nepoužije jako odemknutí cizího leteckého bodu.

Prohledávání do šířky dodrží směr, vyhne se cyklům a z dostupných možností vybere nejméně doložených spojů; stabilní pořadí klíčů řeší shody. Neoptimalizuje reálnou vzdálenost, dobu čekání ani rychlost. Nehledá nejbližší uzel vzdušnou čarou a nedoplní pěší spoj přes horu nebo vodu.

Zastávka se do grafu připojí pouze přes totožné locationKey. Seznam rozlišuje stejné uložené místo, chybějící polohu, chybějící síťový uzel a neexistenci doložené dostupné cesty. Poslední dva stavy neznamenají fyzickou nemožnost dostat se na místo ve hře. Ověřená posloupnost zobrazí režimy, zdroje a odhad přepravy jen tehdy, mají-li jej všechny úseky. Odhad pěší vzdálenosti se nevytváří.

## Objevená doprava a záloha

Ruční evidence používá existující discovered_travel a je zvlášť pro profil. Zapnutí a vypnutí se uloží ihned, přežije nové načtení a přenáší se zálohou. Záznam uživatele nepromění beta/Classic zdroj na potvrzený spoj.

**Osobní Room databáze zůstává ve verzi 2, záloha ve formátu 1.** Beze změny přenášejí pořadí cílů, opakované návštěvy i historické názvy a objevenou dopravu. Plán se při dokončení questu automaticky nemaže; uživatel může plánovat i návrat na již navštívené místo.

## Skutečné pokrytí

Produkční graf má **0 uzlů a 0 hran**. Znovu prověřená stránka Frezzy https://warcraftforever.games/npcs/9564 publikuje beta polohu a dialog zmiňující Undercity, nikoli kompletní release ověřený úsek s oběma konci a podmínkami. https://foreverchanges.pro/about přiznává kombinaci beta klienta a Classic/cMaNGOS. Tyto reference se nepovýší na ověřenou cestu.

E08 proto umožňuje reálné plánování 27 dostupných bodů a ukládání jejich pořadí. Samotný směrovací mechanismus se ověřuje na jasně označené cvičné sadě mimo APK. Reálné dopravní spoje musí přinést další ověření obsahu, ne vývojářský odhad. Převzetí M1 na telefonu, druhém zařízení a ve hře zůstává otevřené; uživatel výslovně požádal pokračovat E08.
