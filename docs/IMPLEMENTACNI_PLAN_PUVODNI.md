# Implementační plán průvodce WoW Forever pro Android

Verze plánu 1.0 | 23. října 2026 | Připraveno pro Jendu

## 1 Záměr a dohodnuté zadání

Vytvoříme nativní aplikaci pro Android, která Jendovi při hraní WoW Forever pomůže najít důležité úkoly, odměny, místa a příležitosti. Hlavní obrazovkou bude herní mapa propojená s osobním průvodcem. Aplikace bude vycházet z aktuální postavy a ručně zaznamenaného postupu. Jejím účelem je podpořit objevování a ohlídat podstatné věci během cesty postavy.

Výchozí profil bude Horda, Warlock, nejspíše ork, začátek na úrovni 1. Rasa zůstane změnitelná. Ork může podle zveřejněné tabulky Blizzardu ve Forever hrát za Warlocka. [S8] Režim hraní a profese zatím nejsou vybrané; aplikace je nebude vyžadovat při prvním spuštění.

### Závazná rozhodnutí

- Nativní Android aplikace v Kotlinu s rozhraním Jetpack Compose. Součástí řešení nebude Capacitor ani webová mapa ve WebView.
- České ovládání a vysvětlení; původní názvy úkolů, předmětů a postav zůstanou dohledatelné.
- Telefon i tablet, orientace na výšku i na šířku, podpora tlačítek systémové navigace i gest.
- Stažené mapy, vyhledávání, poznámky a postup použitelné bez internetu.
- První verze bez účtu, reklam, analytického sledování a placených AI služeb.
- Aktualizace herního obsahu oddělené od aktualizací APK. Záloha postupu do souboru.
- Herní data ověřovaná přímo pro Forever. Zdroj z Classicu se sám nestává potvrzením pro Forever.

### Co plán zatím nepředpokládá

Aplikace nebude sama znát polohu hráče, obsah inventáře ani splněné úkoly ve hře. Uživatel zadá úroveň a oblast a označí hotové položky. Případný import přes herní doplněk je samostatné budoucí rozšíření. Součástí první verze nebude automatické řízení postavy, bojový asistent, sledování živého výskytu nepřátel ani verze pro počítač.

**Výsledkem tohoto dokumentu je plán. Repozitář ani aplikace v této etapě ještě nevznikají.**

<!-- PAGE -->

## 2 Rozsah a postupné vydávání

Kompletní aplikaci budeme skládat po ověřitelných částech. Každé vydání uvede zvlášť verzi programu, verzi herních dat a pokrytí oblastí. Funkční aplikace s několika body se nebude vydávat za úplného průvodce světem.

| Milník | Funkce a skutečný rozsah |
| --- | --- |
| M0 Ověření podkladů | Zvolený zdroj skutečné mapy, ověřená souřadnicová soustava, vzorek herních záznamů a nativní mapový prototyp. |
| M1 První použitelná APK | Profil postavy, přehled světa, úvodní oblast pro orka, vyhledávání, filtr doporučení, detaily, ruční postup, poznámky a záloha. Obsah pro začátek cesty přibližně na úrovních 1 až 10. |
| M2 Průvodce nižšími úrovněmi | Navazující oblasti a důležitá místa Hordy přibližně do úrovně 30, příprava na dostupné dungeony, aktualizace dat a plán výpravy. |
| M3 Ucelená první verze | Důležitý obsah pro Hordu a Warlocka na cestě 1 až 60, mapy potvrzených oblastí a relevantních dungeonů, evidence pokrytí, ověřené návaznosti a doprava. |
| M4 Další rozvoj | Další povolání a rasy, import postupu, pokročilé porovnání výbavy a případná synchronizace zařízení. Každé jako samostatná změna rozsahu. |

Blizzard pro Forever popisuje nové oblasti, úkoly a úpravy povolání v průběhu cesty 1 až 60. [S9] Při vydávání proto musíme ověřovat i změny oproti starým mapám a návodům.

### Jak budeme měřit pokrytí

Pro každou oblast vznikne seznam kontrolovaných kategorií: mapový podklad, úkoly Warlocka, významné odměny, doprava, služby, dungeonové návaznosti a zajímavosti. U každé kategorie bude stav: nezpracováno, rozpracováno, zkontrolováno vůči uvedeným zdrojům. Počet ověřených bodů bude doplněn datem kontroly a seznamem zbývajících nejasností.

Text „zkontrolováno vůči zdroji“ nebude tvrdit, že už nemůže existovat neobjevený obsah. Po změně hry se dotčené záznamy vrátí ke kontrole. Pro oblasti bez podkladu se zobrazí přehled a stav dostupnosti; přesná mapa se nebude nahrazovat vymyšlenou grafikou.

### První obsahová oblast

Pracovní volba je Durotar a začátek orčí cesty. Při etapě E00 ověříme současný začátek orka ve Forever, hranice oblasti a mapové podklady. Pokud se údaje oproti předpokladu liší, upravíme první balíček před plněním databáze.

<!-- PAGE -->

## 3 Obrazovky a chování při hraní

| Obrazovka | Obsah a hlavní akce |
| --- | --- |
| Mapa | Výběr oblasti a patra, hledání, vrstvy, souřadnice, značky, vlastní poznámky, označené cíle. |
| Teď pro mě | Krátký seznam doporučení podle profilu, úrovně, oblasti a splněných podmínek. Každý tip vysvětlí svůj důvod. |
| Moje cesta | Rozpracované návaznosti, seznam přání, dokončené cíle a plán výpravy. |
| Postava a nastavení | Úroveň, rasa, režim, profese, míra prozrazování, vzhled, balíčky oblastí, aktualizace a zálohy. |

### Mapa a detail bodu

Na telefonu bude mapa využívat co největší plochu. Detail bodu se vysune odspodu a půjde rozšířit. Na dostatečně širokém displeji bude detail vedle mapy. Rozložení se bude řídit skutečnou šířkou okna, včetně rozdělené obrazovky, nikoliv pouze označením zařízení jako tablet. Android pro tento typ rozhraní nabízí adaptivní uspořádání seznamu a detailu. [S7]

Detail uvede české vysvětlení, původní název, typ cíle, souřadnice, zdroj a stav ověření. Podle typu přidá požadavky, návazné úkoly, odměnu, potřebnou skupinu nebo způsob získání předmětu. Akce: Hotovo, Rozpracováno, Chci získat, Později, Poznámka a Zobrazit související místa. Změna půjde vrátit.

### Vrstvy a přehlednost

Samostatně zapneme úkoly, cíle důležité pro Warlocka, předměty a jejich zdroje, služby, dopravu, dungeony a zajímavosti. Profese přibudou po jejich výběru. Filtr „Doporučené pro mě“ bude výchozí; pohled „Všechny známé body“ zůstane dostupný.

Při oddálení se blízké značky sloučí do skupin. Důležitost nevyjádříme pouze barvou: pomůže i tvar ikony, text a legenda. Hotové body půjde skrýt nebo zesvětlit. Chybějící internet nevyvolá opakované chybové hlášky nad staženou mapou.

### Modelový průchod

Uživatel vybere oblast a úroveň. Aplikace nabídne dostupný úkol a vysvětlí jeho přínos. Kliknutím zobrazí zadavatele a potřebné předchozí kroky. Uživatel úkol dokončí a označí odměnu jako získanou. Změna se uloží a projeví v doporučeních. Před odchodem z oblasti si otevře kontrolní seznam zbývajících důležitých věcí.

Vstupní profil bude předvyplněný, bez dlouhého úvodního dotazníku. Nastavení rasy, profese a režimu půjde upravit i později.

<!-- PAGE -->

## 4 Technické řešení

| Část | Zvolené řešení a důvod |
| --- | --- |
| Jazyk a rozhraní | Kotlin a Jetpack Compose, nativní prvky Androidu. [S1] |
| Stav obrazovek | ViewModel, coroutines a Flow; obnova vybrané oblasti a rozpracovaných akcí po znovuvytvoření obrazovky. |
| Mapa | Nejprve ověřit nativní MapCompose za odděleným rozhraním mapového prohlížeče. Podporuje dlaždice, značky, cesty a jejich seskupování. [S3] |
| Herní katalog a osobní data | Room nad SQLite. Herní obsah a uživatelský postup budou v oddělených databázích. [S2] |
| Drobná nastavení | DataStore pro vzhled, poslední oblast, filtry a aktivní generaci obsahu. [S5] |
| Síť a balíčky | OkHttp pro stahování, Kotlin serialization pro popis balíčků a výměnná data. Konkrétní kompatibilní verze uzamknout v E01. |
| Práce pokračující mimo obrazovku | WorkManager pro správu delších stahování a jejich opakování podle podmínek Androidu. [S6] |
| Sestavení | Gradle Wrapper, automatické kontroly a sestavení APK; následně vhodný proces přes GitHub Actions. |

### Uspořádání projektu

Začneme jedním aplikačním modulem s jasně oddělenými balíčky: app, core, data, domain a feature. V rámci feature budou map, guide, journey, character a settings. Testovatelná pravidla doporučení, převody souřadnic a validace dat nebudou záviset na obrazovkách. Samostatné Gradle moduly přidáme až při skutečné potřebě.

Mapová knihovna bude schovaná za adaptérem. Pokud E01 prokáže zásadní problém s kompatibilitou nebo výkonem, půjde vyměnit bez přepisování katalogu, souřadnic a doporučení. Vlastní dlaždicový renderer je záložní varianta s vyšší pracností, nikoliv souběžně vyvíjený druhý systém.

### Podporovaný Android

Pracovní minimum je Android 8, API 26. V E01 ověříme, zda ho podporuje zvolená sada knihoven; případnou změnu zapíšeme. compileSdk a targetSdk zvolíme podle aktuálně stabilní a vzájemně kompatibilní sady nástrojů v okamžiku implementace. Nebudeme používat plovoucí verze závislostí.

Výchozí cíl je standardní APK pro přímou instalaci. Obchod Google Play není podmínkou používání. Aplikace nebude vyžadovat GPS ani přístup k účtu Battle.net. GPS telefonu by herní polohu neposkytla.

<!-- PAGE -->

## 5 Nativní mapa a souřadnice

### Mapové podklady

Pro svět, kontinent, oblast, město a patro dungeonu založíme samostatný záznam MapDefinition. Ponese identifikátor, nadřazenou mapu, patro, rozměry, rozsah přiblížení, verzi podkladu a vazbu na souřadnicovou soustavu. Změna geometrie mapy vytvoří novou verzi jejího souřadnicového popisu.

Zdrojové mapové obrázky se při přípravě obsahu rozřežou do dlaždic po 256 nebo 512 obrazových bodech podle výsledku E01. Vznikne několik úrovní detailu. Aplikace načte viditelné části a malé okolí, s omezenou vyrovnávací pamětí. Rozměr dlaždic a paměťový limit zvolíme měřením. Celá velká mapa se nebude načítat do jediného obrázku v paměti.

Existence fanouškovské mapy není automaticky dostupným zdrojem obrázků nebo strojového rozhraní. V E00 zaznamenáme, odkud můžeme podklad skutečně získat, v jaké kvalitě a za jakých podmínek jej lze použít. Kandidáty k porovnání jsou AzerPug a MetaForge. [S10, S11] Přístup k neveřejnému rozhraní nepředpokládáme.

### Převod herních souřadnic

Každá poloha uloží mapId, floorId a x/y v herní soustavě oblasti, obvykle 0 až 100. Pro vykreslení se převede do normalizovaného rozsahu mapové knihovny. Základní převod x/100 a y/100 smíme použít pouze u podkladu se shodnými hranicemi. U oříznuté nebo posunuté mapy definujeme výslovnou transformaci. Přepnutí oblasti nikdy nebude interpretovat stejné x/y jako polohu v jiném území.

Přesnost ověříme nejméně pěti rozloženými kontrolními místy, z nichž část nepoužijeme při kalibraci. Cíl pro přesné body: odchylka nejvýše 1 bod herních souřadnic; matematický převod tam a zpět nejvýše 0,1 bodu. Nevyhovující podklad se označí a opraví před zveřejněním přesných značek.

### Body a oblasti výskytu

Jedna bytost může mít více poloh. Putující nepřítel nebo oblast sběru může být zobrazená plochou či trasou. U předmětu označíme místo jeho získání: NPC, úkol, obchodníka nebo doloženou oblast výskytu. Přibližná poloha bude odlišitelná od přesné. Chybějící souřadnice nebudou nahrazené středem mapy.

<!-- PAGE -->

## 6 Datový model a příprava obsahu

### Herní katalog

| Záznam | Co uchovává |
| --- | --- |
| MapDefinition a Location | Mapu, patro, souřadnicový systém, geometrii, přesnost polohy a verzi podkladu. |
| Quest a QuestRelation | Požadovanou úroveň, zadavatele, cíle, odevzdání, odměny, podmínky ALL nebo ANY, výlučné volby a opakovatelnost. |
| NPC a Service | Postavu nebo bytost, její polohy a služby, vazby na úkoly a prodej. |
| Item a AcquisitionSource | Předmět, vlastnosti a způsoby získání. Pravděpodobnost získání jen s doloženým zdrojem. |
| PointOfInterest a TravelEdge | Místo, kategorii, podmínky přístupu, dopravu a ověřené propojení míst. |
| GuideRule a Evidence | Důvod doporučení, podmínky, důležitost, zdroj, stav ověření a případné riziko zmeškání. |

Osobní databáze obsahuje CharacterProfile, QuestProgress, CollectedItem, Bookmark, UserNote, PlannedStop a DiscoveredTravel. Splněný úkol, získaný předmět a právě používaná výbava jsou odlišné skutečnosti. V první verzi budeme evidovat splnění a získání; nebudeme předstírat znalost aktuálně nasazené výbavy.

### Identita a původ záznamů

Stabilní klíč bude obsahovat variantu hry, typ záznamu a identifikátor. Chybí-li oficiální ID, použijeme vlastní trvalé ID. Stejné číslo nebo podobný název z Classicu nebude samo o sobě důvodem ke sloučení. Změna názvu ani českého překladu nesmí ztratit osobní postup.

Evidence ponese URL nebo jiný dohledatelný odkaz, datum kontroly, verzi hry či bety, původ souřadnic a poznámku k ověření. Stav: ověřeno ve vydané Forever, potvrzeno oznámením, ověřeno v betě, převzato z Classicu k prověření, sporné nebo zastaralé. Oznámení obecné funkce nepotvrdí konkrétní souřadnice nebo odměnu.

### Příprava a kontrola balíčků

Pracovní katalog bude v čitelných JSON souborech v projektu, upravovaných bez nutnosti administrátorského webu. Pomocný nástroj obsah sjednotí, zkontroluje a vytvoří distribuční balíček. Ověří rozsahy souřadnic, existence odkazovaných ID, duplicity, návaznosti, neplatné cykly a úplnost zdrojů. Opakovatelné úkoly mají výslovné pravidlo, aby nebyly chybně považované za porušenou návaznost.

První balíček sestavíme z menšího ručně prověřeného souboru. Import většího katalogu přidáme až po ověření jeho varianty hry a struktury. Web WoW Forever Quests sám popisuje základ z Classic Era a čekání na vlastní Forever data. [S12] Je proto kandidátem k porovnání, nikoliv automaticky potvrzeným katalogem Forever.

<!-- PAGE -->

## 7 Doporučení a hlídání důležitých věcí

Doporučení v první verzi budou vysvětlitelná pravidla nad ověřenými daty. Aplikace nebude potřebovat jazykový model ani internetový AI klíč.

### Podmínky dostupnosti

Pravidla vyhodnotí frakci, povolání, rasu, úroveň, režim, profesi, oblast a stav předchozích úkolů. Podmínky rozliší „všechny musí platit“ a „stačí některá“. Stav úkolu bude Nezahájeno, Rozpracováno, Hotovo nebo Odloženo. Dostupnost je oddělený výsledek: dostupné, chybí podmínka, později, vyloučeno volbou nebo neověřeno.

Odložení úkolu nebude znamenat jeho splnění. U neznámé podmínky zobrazíme, co chybí k ověření. Pouhé dosažení vyšší úrovně neudělá z nesplněného úkolu automaticky ztracenou příležitost.

### Řazení a vysvětlení

Přednost dostanou doložené nevratné volby a důležité kroky pro Warlocka. Následují odemknutí návazného obsahu, významné odměny, příprava na dungeon, doprava a zajímavosti. Blízkost pomůže až po zohlednění podmínek a významu. Uživatel může dát přednost objevování nebo vybavení.

U každého doporučení bude stručný důvod, například „Otevírá další dva kroky“ nebo „Úkol si vezmi před návštěvou dungeonu“. Tyto věty se smí zobrazit jen při odpovídajících datech. Čísla návazných kroků se vypočítají z katalogu.

### Zmeškatelnost a odměny

Označení Nevratné vyžaduje konkrétní pravidlo: vzájemně se vylučující volba, doložená změna stavu světa nebo časové omezení. Jinak použijeme Praktické udělat teď, Lze se vrátit nebo Neověřeno. U časové události ukládáme časové pásmo a platnost; zastaralá událost nezůstane aktivním doporučením.

Odměna pro Warlocka dostane popis přínosu a zdroj. Absolutní pořadí „nejlepší předmět“ bez zohlednění specializace a výbavy nebude součástí první verze. Volbu questové odměny půjde zaznamenat a aplikace zohlední její případné důsledky.

### Před odchodem z oblasti

Kontrolní seznam spojí dostupné důležité cíle, chybějící kroky návazností, dopravu a uživatelova přání. Dokončené věci odpadnou. Označené nejistoty budou dostupné zvlášť. Hlášení nebude tvrdit, že je oblast kompletně hotová, pokud její obsah ještě nebyl zkontrolován.

<!-- PAGE -->

## 8 Plán výpravy a ochrana postupu

### Výprava a doprava

Nejdříve vytvoříme uživatelem seřazený seznam cílů s očíslovanými značkami. Schématické spojení bodů se výslovně označí jako pořadí zastávek. Nebude se vydávat za průchozí cestu terénem.

Následně přidáme graf ověřených cest, vstupů, lodí, vzducholodí a leteckých bodů. Každé propojení ponese podmínky přístupu a směr. Letovou trasu doporučíme jen při zaznamenaném odemknutí potřebných bodů. Vzdálenost vzdušnou čarou neprokáže průchod přes horu nebo uzavřený prostor. Časy přepravy budou uvedené jako odhad, dokud nemají ověřený podklad.

Pro první použitelnou APK je dostatečný očíslovaný plán; pro automatické vedení po cestách musí projít samostatná etapa E08.

### Ukládání a nezávislost na aktualizacích

Osobní změny se budou ukládat průběžně do vlastní databáze. Poloha mapy a filtry se obnoví po otočení i znovuspuštění aplikace. Hotové úkoly se nesmí ztratit při výměně herního katalogu. Obě databáze mají vlastní verzi schématu a ověřené migrace; nebudeme používat automatické smazání databáze jako řešení změny struktury.

U záznamu odstraněného z katalogu zachováme osobní historii s jeho stabilním ID a posledním známým názvem. V aplikaci bude označen jako vyřazený nebo momentálně nedohledatelný. Změnu herního ID řeší výslovná převodní tabulka.

### Záloha a obnova

Export vytvoří přenositelný soubor s verzí formátu, profily, postupem, poznámkami, přáními a plánem výpravy. Herní obrázky se do zálohy nekopírují. Soubor půjde vybrat a uložit systémovým výběrem dokumentů bez plošného přístupu ke všem souborům zařízení.

Před importem se ukáže obsah a počet změn. Výchozí bezpečná volba je import do nového profilu. Nahrazení existujícího profilu vytvoří vratný místní snímek a proběhne v transakci. Neplatný nebo novější nepodporovaný formát se odmítne s vysvětlením. Ověříme přenos mezi dvěma zařízeními, nikoliv pouze export a zpětné načtení v témže procesu.

Odinstalování nebo ruční smazání dat může odstranit místní postup. Export je proto součástí M1. Účet a cloudová synchronizace nejsou předpokladem této verze.

<!-- PAGE -->

## 9 Aktualizace obsahu a správa stažených map

Datová část bude navržena pro použití bez připojení: obrazovky čtou místní ověřený obsah a síť pouze dodává nové balíčky. Tento postup odpovídá doporučení Androidu pro aplikace fungující offline. [S4]

### Balíček oblasti

Každý balíček bude mít identifikátor oblasti, verzi obsahu, variantu a verzi hry, verzi schématu, závislé balíčky, souřadnicovou verzi, velikost, kontrolní součty a soupis původu podkladů. Mapové dlaždice a textový katalog mohou být samostatné soubory, ale aktivují se jen ve vzájemně kompatibilní kombinaci.

Aktualizace budou přicházet z předem nastaveného důvěryhodného HTTPS umístění. Konkrétní hosting vybereme až podle projektu a dostupných podkladů. Pro první distribuci postačí ručně importovaný ověřený balíček. Aplikace nebude za běhu záviset na rozebírání HTML cizích webů.

### Bezpečné přepnutí generace obsahu

1. Zjistit dostupnou verzi a zobrazit velikost stažení.
2. Zkontrolovat volné místo včetně prostoru pro rozbalení a zachování předchozí generace.
3. Stáhnout do dočasného umístění; umožnit zrušení a opakování přerušeného přenosu.
4. Ověřit součty, formát, očekávané cesty, velikosti po rozbalení a kompatibilitu. Zamezit zápisu mimo cílovou složku.
5. Založit a prověřit novou generaci katalogu. Označit případné změny důležité pro existující profily.
6. Přepnout ukazatel na aktivní generaci až po úspěchu všech kontrol. Zachovat předchozí funkční generaci pro návrat.

Přerušení v libovolném kroku musí ponechat původní obsah použitelný. Osobní databáze se při tom nevyměňuje. Součet kontroluje neporušenost; důvěryhodnost určuje schválený zdroj. Pokud později dovolíme libovolné externí zdroje, přidáme ověřování podpisu distribučního manifestu před jejich povolením.

### Správce map

Uživatel uvidí stažené oblasti, velikost, verzi a dostupné aktualizace. Výchozí stahování větších map nabídne Wi-Fi. Smazání balíčku nikdy nesmaže postup a poznámky. Oblast půjde znovu stáhnout. Databáze a dlouhodobě stažené mapy budou v trvalém úložišti, dočasné soubory odděleně.

<!-- PAGE -->

## 10 Etapy E00 až E03

### E00 Ověření zdrojů a rozsahu prvního balíčku

**Vstup:** tento plán a výchozí profil postavy.

**Práce:** ověřit začátek orka ve Forever, seznam úvodních úkolů a dostupnost skutečné mapy. Sestavit přehled zdrojů a odlišit Forever, betu a Classic. Vybrat podklad pro první oblast a přehled světa. Prověřit vzorek NPC, úkolu, předmětu, návaznosti a dopravy. Založit seznam nejasností a registr pokrytí.

**Výstup:** dokument zdrojů, inventář map, vzorek katalogu a rozsah prvního balíčku. **Dokončeno:** mapa je dostupná jako použitelný podklad, původ souřadnic je doložený a každý vzorový záznam má původ a stav ověření. Pokud podklad chybí, označíme blokovanou mapovou část a pokračujeme s ostatními částmi; nevydáváme náhradní obrázek za skutečnou mapu.

### E01 Základ projektu a ověření nativní mapy

**Závisí na:** vzorku podkladů E00. **Práce:** založit Android projekt a Git historii, zvolit identifikátor aplikace a uzamknout nástroje. Integrovat MapCompose přes adaptér. Ověřit skutečný obrázek, dlaždice, značky, přiblížení, otočení zařízení a zátěž. Zkušební značky pro měření držet pouze ve vývojové variantě a výslovně je označit.

**Výstup:** sestavitelné technické APK a záznam volby mapové komponenty. **Dokončeno:** čisté sestavení projde; mapa a stejné kontrolní body zůstávají na správném místě při přiblížení i změně rozložení. Případný náhradní renderer se rozhodne zde.

### E02 Databáze a nástroje pro herní obsah

**Závisí na:** E00 a datových rozhodnutích E01. **Práce:** založit obě databáze, stabilní identifikátory, schémata JSON, validátor katalogu, zdroje a souřadnicové transformace. Připravit první import a test aktualizace. Rozlišit český popis, původní název a vyhledávací aliasy.

**Výstup:** instalovatelný balíček vzorového obsahu a osobní profil. **Dokončeno:** neplatné vazby, souřadnice nebo varianty hry jsou zachycené; přejmenování položky zachová postup.

### E03 Ovládání a profil postavy

**Závisí na:** E01 a E02. **Práce:** vytvořit navigaci, předvyplněný profil, zadání úrovně a oblasti, nastavení prozrazování, rozložení telefonu a tabletu. Zajistit české texty a návrat do předchozího místa.

**Výstup:** použitelná kostra aplikace. **Dokončeno:** hlavní průchod funguje na výšku i šířku, při systémových tlačítkách i gestech a po obnovení procesu. Nezvolená profese nebo režim neblokují používání.

<!-- PAGE -->

## 11 Etapy E04 až E07

### E04 Mapa propojená s herním katalogem

**Závisí na:** E01 až E03. **Práce:** zavést vrstvy, seskupování značek, vyhledání bodu, přepínání oblasti a patra, detail, více poloh jednoho objektu a vlastní poznámku dlouhým stiskem. Rozlišit přesnou a přibližnou polohu. Omezit načítání na oblast výřezu a relevantní kategorie.

**Výstup:** nativní atlas první oblasti. **Dokončeno:** vyhledaný záznam otevře správnou mapu a značku, filtr skutečně mění viditelné body a poznámka přežije restart. Souřadnice kontrolních míst splňují toleranci z oddílu 5.

### E05 Úkoly a osobní postup

**Závisí na:** E02 až E04. **Práce:** implementovat stavy úkolů, návaznosti ALL a ANY, výlučné volby, evidenci získané odměny, přání, odložení a vrácení změny. Každý krok z detailu propojit s mapou a osobní cestou. Přidat export a obnovu profilu již v této etapě.

**Výstup:** použitelný herní deník. **Dokončeno:** dokončení kroku správně zpřístupní návaznost; odložení ji nezpřístupní; změna volby neproběhne potichu. Záloha funguje na druhém zařízení a chybný import zachová data.

### E06 Osobní doporučení

**Závisí na:** E05 a zdrojovaných pravidlech. **Práce:** vyhodnotit dostupnost, důležitost a ověření obsahu; vytvořit obrazovku Teď pro mě a seznam před odchodem z oblasti. Doplnit míru prozrazování a srozumitelné vysvětlení doporučení.

**Výstup:** průvodce podle profilu. **Dokončeno:** známé scénáře pro nízkou úroveň, nesplněný předpoklad, výlučnou odměnu a změnu rasy dávají očekávané výsledky. Nejistota nebo převod z Classicu se nezmění na potvrzené upozornění.

### E07 První obsah a vydání M1

**Závisí na:** E04 až E06. **Práce:** naplnit úvodní oblast, služby a důležité kroky Warlocka pouze ověřenými záznamy. Cíl prvního pilotu je alespoň 25 doložených bodů napříč dostupnými kategoriemi a dvě ověřené návaznosti, pokud je herní obsah v oblasti poskytuje. Pokud zdroje cílový počet nebo kategorii nepotvrdí, uvést skutečný menší rozsah; nedoplňovat záznamy pro splnění kvóty.

**Výstup:** první APK k reálnému hraní, záloha, popis pokrytí a seznam nedořešeného obsahu. **Dokončeno:** projde celý průchod z oddílu 14 na skutečném Androidu. Nedostupná kategorie nebo neověřená návaznost má výslovně přiznané omezení. Testovací data nejsou v uživatelském katalogu.

<!-- PAGE -->

## 12 Etapy E08 až E12 a závislosti

### E08 Plán výpravy a ověřená doprava

**Závisí na:** E05 až E07. Nejprve přidat řazení zastávek a číslování mapy. Potom graf ověřených cest a dopravy, pravidla přístupnosti a zohlednění objevených leteckých bodů. **Dokončeno:** schématické pořadí a ověřená cesta jsou rozlišitelné; výpočet nepoužije zamčenou dopravu a neprohlásí spojnici přes překážku za cestu.

### E09 Balíčky a aktualizace obsahu

**Závisí na:** E02, E05 a formátu obsahu ověřeném v M1. Přidat správce map, manifesty, stahování, validaci, přepínání generací a návrat. Aktualizace v pozadí zavádět až po funkčním ručním stažení. **Dokončeno:** přerušení přenosu, chybný součet, nekompatibilní schéma a nedostatek místa ponechají poslední obsah i postup použitelný.

### E10 Rozšíření obsahu k M2 a M3

**Závisí na:** M1, E09 a pro dopravní doporučení E08. Postupovat po oblastech navazujících na skutečnou cestu postavy: nejprve nižší úrovně, poté střední a vyšší úrovně, příslušné dungeony a nové Forever lokace. Každá oblast projde stejným zpracováním: podklad, souřadnice, katalog, návaznosti, pravidla, kontrola zdrojů a evidence pokrytí. **Dokončeno:** vymezený obsah je prověřený a katalog uvádí, co ještě chybí. Podpora jiných povolání do této etapy nepatří.

### E11 Spolehlivost a výkon

**Závisí na:** E07 až E10 v rozsahu příslušného vydání. Ověřit větší katalog, paměť mapy, obnovu po ukončení procesu, přístupnost, velké písmo, migrace databází a dlouhé používání. Opravit doložená slabá místa. **Dokončeno:** nejsou otevřené chyby ztráty postupu, chybného přepnutí dat nebo nesprávných souřadnic; měření a známá omezení jsou zaznamenaná.

### E12 Podepsané vydání a provoz

**Závisí na:** příslušném obsahovém milníku a E11. Připravit stabilní podpis APK, instalaci aktualizací, návod, historii změn, informaci o pokrytí a postup obnovy. Zálohu podpisového klíče uloží vlastník projektu mimo zdrojový kód. Automatické sestavení může klíč používat přes chráněná tajemství. **Dokončeno:** aktualizace předchozí verze zachová profil; APK je dohledatelná k verzi zdrojů a obsahu.

**Hlavní pořadí:** E00 → E01 → E02 → E03 → E04 → E05 → E06 → E07. Potom E08 a E09, opakované obsahové E10, kontrola E11 a vydání E12. Základní testy a kontrola dat probíhají průběžně v každé etapě; E11 je souhrnné ověření, nikoliv první testování.

<!-- PAGE -->

## 13 Ověření správnosti a výkonové cíle

Testy mají ověřovat skutečné dopady chyb. Prioritu mají souřadnice, dostupnost úkolů, uchování postupu, importy a přerušení aktualizací. Jednoduchý textový popisek nevyžaduje samostatný test.

| Oblast kontroly | Povinné scénáře |
| --- | --- |
| Souřadnice | Různé oblasti a patra, okraje mapy, kalibrace, přiblížení a zpětný převod. |
| Doporučení | ALL a ANY, úroveň, rasa, frakce, neznámé podmínky, opakovatelnost, výlučná volba, odložení a dokončení. |
| Osobní data | Restart, nucené ukončení procesu, otočení, změna profilu a aktualizace schématu i obsahu. |
| Zálohy | Obnova na druhém zařízení, poškozený soubor, novější formát, neznámá ID, nahrazení profilu a návrat. |
| Balíčky | Přerušená síť, nedostatek místa, neplatné součty a cesty, nekompatibilita a neúplná generace. |
| Rozhraní | Telefon a tablet, výška a šířka, rozdělené okno, systémová tlačítka a gesta, zvětšené písmo a čtečka obrazovky. |

### Pracovní cíle pro výkon

Měřit v sestavení určeném k měření nebo vydání. Výchozí referenční telefon bude dostupný Android, přednostně Jendův Samsung S24 Ultra; tablet ověříme na dostupném zařízení a emulátoru. Konkrétní referenční zařízení a velikost balíčku zapíšeme při E01.

- Otevření stažené výchozí oblasti po studeném spuštění přibližně do 3 sekund na referenčním telefonu.
- Filtrování a hledání v katalogu 10 000 testovacích záznamů s odezvou přibližně do 300 ms po zadání dotazu.
- Plynulé posouvání mapy s cílem 60 snímků za sekundu na referenčním zařízení. Sledovat dlouhé snímky a zamrzání; samotný průměr nestačí.
- Zátěžový scénář s 5 000 kandidátními body v oblasti zpracovat filtrováním a seskupováním, bez vykreslení tisíců detailních prvků současně.
- Dvacet minut přepínání oblastí a přibližování nesmí způsobit neustálý růst paměti ani pád kvůli jejímu nedostatku.

Jde o návrhové cíle, nikoliv o naměřené vlastnosti hotové aplikace. Pokud neprojdou, opravíme příčinu nebo před vydáním doložíme úpravu cíle. Syntetická zátěžová data budou oddělená od herního katalogu.

### Výstup ověření

Každý milník ponese verzi kódu, verzi dat, použitá zařízení, výsledky významných scénářů a otevřené chyby. Kontrola známých míst na skutečné herní mapě doplní automatické testy; test matematického převodu sám neprokáže správnost převzaté polohy NPC.

<!-- PAGE -->

## 14 Převzetí prvního APK a hlavní rizika

### Přejímací průchod M1

1. Nainstalovat APK, otevřít předvyplněného orčího Warlocka a změnit úroveň i oblast.
2. Otevřít staženou skutečnou mapu, přiblížit kontrolní místo a zapnout dvě vrstvy.
3. Vyhledat původní název úkolu i český popis; otevřít správnou mapu a detail.
4. Zkontrolovat předchozí kroky, odměnu, zdroj a stav ověření; označit úkol jako hotový.
5. Ověřit změnu doporučení a seznam před odchodem. Změnu vrátit a znovu provést.
6. Přidat vlastní poznámku, uložit předmět do přání, otočit zařízení a ukončit aplikaci.
7. Spustit aplikaci bez internetu a ověřit stejný postup, mapu, hledání i poznámku.
8. Vyexportovat profil a obnovit ho na druhém zařízení bez ručního přepisování údajů.
9. Nainstalovat další sestavení nebo datovou generaci a ověřit zachování osobních dat.

### Rizika a konkrétní reakce

| Riziko | Jak s ním naložíme |
| --- | --- |
| Chybí aktuální mapa nebo podmínky jejího získání | Vyřešit v E00. Dočasně otevřít seznam oblasti se stavem dostupnosti; mapovou úplnost neslibovat. |
| Smíchání Classicu a Forever | Výslovná varianta hry a evidence zdroje u každého záznamu; nepotvrzený převod držet mimo ověřená doporučení. |
| Změna souřadnic nebo odměn po vydání | Verze mapy a dat, kontrolní body, historie změn a opětovná kontrola dotčených záznamů. |
| Ztráta postupu při aktualizaci | Oddělená osobní databáze, stabilní ID, testované migrace, zálohy a vratné přepnutí generace. |
| Závislost na mapové knihovně | Ověřit v E01 a oddělit ji adaptérem. Záložní renderer řešit jen při doloženém problému. |
| Nepřehledná mapa | Výchozí filtr, seskupování, úrovně detailu a jasné ikonky. |
| Nepřesná automatická trasa | Nejprve seznam zastávek; skutečnou navigaci počítat jen po ověřeném grafu. |
| Rozšiřování projektu před prvním použitím | Dovést M1 do použitelného stavu před dalšími povoláními, účtem nebo synchronizací. |

### Co nejvíce ovlivní pracnost

Programovou část rozdělí výše uvedené etapy. Nejistou částí rozsahu je dostupnost a ruční kontrola herních dat. První rozumný časový odhad zpřesníme po E00 a E01, kdy známe mapový zdroj, velikost katalogu a chování mapové komponenty. Vydání hry nepovažujeme automaticky za termín dokončení celého atlasu.

<!-- PAGE -->

## 15 Jak zahájíme implementaci a budeme navazovat

### První pracovní blok

1. Založit záznam rozhodnutí a ověřit podklady E00. Před výběrem repozitáře zkontrolovat dostupný projektový kontext; nový pracovní název může být WoW Forever Průvodce.
2. Připravit nový nativní Android projekt. Při skutečném založení repozitáře zkontrolovat existující názvy a zvolit jednoznačný identifikátor aplikace.
3. Zapsat konkrétní verze nástrojů a vytvořit reprodukovatelné sestavení.
4. Připojit skutečnou mapu první oblasti, převod souřadnic a malý zdrojovaný katalog.
5. Ověřit mapu, detail úkolu a uložení jedné změny. Dovést tento průchod do sestavitelného technického APK.

Technické APK z E01 ještě nemusí splňovat M1. Jeho účelem je vyřešit včas mapu a sestavení. K uživatelskému hraní předáme M1 až po celém přejímacím průchodu a s popsaným obsahovým rozsahem.

### Dokumenty uvnitř projektu

| Soubor nebo adresář | Účel |
| --- | --- |
| docs/IMPLEMENTACNI_PLAN.md | Tento plán, jeho verze a odůvodněné změny rozsahu. |
| docs/ROZHODNUTI.md | Zvolená technologie, verze, souřadnicová pravidla a důvody změn. |
| docs/POSTUP.md | Aktuální etapa, hotové části, poslední úspěšné sestavení a přesný další krok. |
| docs/ZDROJE_A_POKRYTI.md | Původ map, datové zdroje, pokrytí oblastí a otevřené nejasnosti. |
| content a tools/content | Čitelný katalog a nástroje pro jeho kontrolu a balení. |
| docs/OVERENI | Výsledky přejímacích scénářů a měření významných milníků. |

Po každé dokončené etapě vznikne samostatný přehled změn, odpovídající kontrola a dohledatelný commit. Hlavní větev musí zůstat sestavitelná. GitHub Actions může sestavovat průběžné APK, kontrolovat katalog a spouštět testy. Přístupové a podpisové klíče nepatří do zdrojových souborů.

### Zadání pro navázání

„Pokračuj podle implementačního plánu WoW Forever Průvodce pro Android. Zachovej Kotlin, Jetpack Compose a nativní mapu. Začni etapou E00 a poté E01. První ověřitelný výsledek je skutečný mapový podklad, zdrojované body a sestavitelné technické APK. Rozlišuj herní obsah Forever, betu a Classic. Uchovávej postup projektu a neoznačuj malý pilot za kompletní databázi světa.“

**Následující práce:** E00 a E01. Ostatní etapy na ně navazují v uvedeném pořadí. Rozhodnutí o rase, profesích a režimu mohou zůstat měnitelná a zahájení práce neblokují.

<!-- PAGE -->

## 16 Zdroje a stav podkladů

Kontrola odkazů a technických podkladů: 23. října 2026. Technologie a postupy v tomto plánu jsou návrhem pro danou aplikaci. Zdroje potvrzují schopnosti nástrojů nebo oznámené vlastnosti hry; nepotvrzují, že už byla aplikace postavena či otestována. Konkrétní herní záznamy a možnost distribuce mapových podkladů ověří E00.

### Oficiální technická dokumentace

- **S1 Jetpack Compose.** Doporučený nástroj pro nativní rozhraní Androidu. [Android Developers](https://developer.android.com/compose)
- **S2 Room.** Místní databáze, strukturovaná data a migrace. [Android Developers](https://developer.android.com/training/data-storage/room)
- **S3 MapCompose.** Dokumentace autora knihovny, dlaždice, značky, cesty, normalizované souřadnice a seskupování. V E01 ověříme konkrétní verzi a její kompatibilitu. [Repozitář p-lr MapCompose](https://github.com/p-lr/MapCompose)
- **S4 Aplikace fungující offline.** Místní zdroj dat a oddělení síťových aktualizací. [Android Developers](https://developer.android.com/topic/architecture/data-layer/offline-first)
- **S5 DataStore.** Ukládání malého množství nastavení. [Android Developers](https://developer.android.com/topic/libraries/architecture/datastore)
- **S6 WorkManager.** Trvalejší práce a podmínky běhu na pozadí. [Android Developers](https://developer.android.com/develop/background-work/background-tasks/persistent)
- **S7 Adaptivní rozhraní.** Uspořádání seznamu a detailu podle dostupného místa. [Android Developers](https://developer.android.com/develop/ui/compose/layouts/adaptive/list-detail)

### Oznámení hry a kandidáti pro obsah

- **S8 Create the Hero You Want to Be in World of Warcraft Forever.** Oficiální tabulka kombinací ras a povolání včetně orka a Warlocka. [Blizzard](https://news.blizzard.com/en-us/article/24304075/create-the-hero-you-want-to-be-in-world-of-warcraft-forever)
- **S9 World of Warcraft Forever Whats Next Panel Recap.** Oznámený směr hry, nové oblasti, úkoly a změny povolání. [Blizzard](https://news.blizzard.com/en-gb/article/24303862/world-of-warcraft-forever-whats-next-panel-recap)
- **S10 AzerPug Forever.** Vlastní popis fanouškovské mapy a katalogu. Kandidát k porovnání a ověřování dostupnosti podkladů. [AzerPug](https://forever.azerpug.com/)
- **S11 MetaForge Forever Map.** Existující mapový atlas. Kandidát k porovnání mapových hranic a bodů; tento plán nepředpokládá přístup k jeho interním datům. [MetaForge](https://metaforge.app/wow-forever/map/azeroth)
- **S12 WoW Forever Quests.** Stránka popisující výchozí katalog z Classic Era a sledování budoucích změn. Doklad potřeby rozlišovat skutečný původ dat. [WoW Forever Quests](https://www.wowforeverquests.com/new-quests)

### Otevřené položky pro E00 a E01

Zdroj a rozlišení map, současný začátek orka, ověřitelné souřadnice, rozsah dostupného Forever katalogu, kompatibilní verze knihoven, identifikátor aplikace, umístění repozitáře a referenční testovací tablet. Jde o konkrétní úkoly úvodních etap; tento plán je nepovažuje za vyřešené.
