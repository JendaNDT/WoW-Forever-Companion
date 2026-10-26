# E11 — výsledek a předání

> Historický dokument k verzi 0.11.0 a tehdejším datům. Aktuální ověření plného katalogu verze 0.16.0 je v [E11_AKTUALNI_OVERENI.md](E11_AKTUALNI_OVERENI.md); dnešní rozsah určuje [POSTUP.md](POSTUP.md).

25. 10. 2026 · 0.11.0-e11 / kód 11 · cz.jenda.wowforever.guide.

## Výsledek

Implementační část E11 je dokončená. Hledání používá index připravený jednou pro generaci; mapa ponechává nejvýše dva stavy a při nedostatku paměti uvolní neaktivní. Před uvolněním ukládá výřez. Mapové značky mají plochu 48 dp a popisy pro přístupnost. Posuvné panely, boční navigace a zvláštní zobrazení detailu ve velmi malém okně umožňují práci s dvojnásobným písmem, aniž by detail vytlačil mapu. Poškozené nastavení se obnoví bez smazání osobních dat. Chyba úklidu po potvrzené aktualizaci už nevrátí disk na jinou generaci, než ukazuje rozhraní.

Fyzická část E11 zůstává otevřená: v prostředí není adb telefon ani emulátor. Nejde o potvrzení 3sekundového startu, 300ms celkové odezvy hledání, 60 FPS nebo dvacetiminutové stability paměti na Samsungu. Technické podrobnosti a připravený postup měření: E11_DATA.md. Kontrola pro Jendu: E11_TELEFON.md.

## Ověření

- Čisté sestavení debug APK, podepsaného release APK, instrumentačního APK, všech hostitelských testů a obou variant Lint uspělo: 148 provedených úloh, 6 min 46 s. V qa/e11/clean-build.log je zachovaný začátek klientského výpisu a závěrečný výsledek stejného běhu z logu démona, protože přesměrování klienta přestalo předávat další hlavičky úloh. Úplné XML výsledky testů a Lint jsou uložené samostatně.
- **169 hostitelských testů prošlo, 0 selhalo, 0 chyb, 0 vynechaných**, z toho **11 nových E11**. Tři další Python testy prověřily parser skutečných Android statistik, včetně neúplných a chybných vstupů. Připravené Android testy nebyly na zařízení spuštěné.
- Lint debug: **0 chyb / 5 varování**. Lint release: **0 chyb / 6 varování**. Tři upozornění na dostupné novější knihovny, dvě upozornění UsableSpace na témže místě. Release navíc uvádí, že profileable shell používá Android 10+; na API 26–28 se tato možnost nepoužije. Závislosti ani pravidla Lint se neměnily. Překladač také označuje starší konstantu paměťové úrovně za deprecated; callback dál slouží pro starší Android a uvolní neaktivní stav také při oznámení skrytí UI.
- Hledání v 10 000 oddělených syntetických položkách: 16 zahřívacích dotazů, 24 měřených pro každou variantu, střídání pořadí a stejné výsledky. E10 algoritmus ve stejném JVM: P50 **46,11 ms**, P95 **64,62 ms**. E11 index: P50 **0,765 ms**, P95 **1,402 ms**; příprava indexu **52,03 ms**. Jde o jádro hledání na zdejším počítači, nikoli odezvu mobilního UI. Úplná maxima, prostředí a omezení jsou v e11-search.json a environment.json.
- Pět tisíc syntetických mapových kandidátů se při oddálení zobrazilo jako **21 sémantických prvků**. Dvanáct kol zaměření, přiblížení, severu a oddálení doběhlo s platným výřezem. Tím se ověřilo seskupování, nikoli 60 FPS.
- Čtyřicet přepnutí čtyř přibalených map drželo nejvýše **dva stavy**. Uvolněná oblast se vrátila do původního výřezu. Paměťový callback ponechal jeden aktivní stav; opakované zavření uvolnilo všechny. Nejde o měření celkového PSS nebo dvacetiminutového používání.
- Skutečné zdroje velikosti písma jsou nastavené na 200 % před vytvořením okna. Test otevře katalog, hledá a zobrazí výsledek; druhý hledá na mapě, ověří viditelnou mapu a otevření posuvného detailu; třetí dosáhne na Postavu a uložení v nízkém okně na šířku. Výchozí malé telefonní okno se používá kvůli ověřenému omezení Robolectric #8460: explicitní w/h qualifiers a Dialog/TextField způsobují čekání na ustálení i v reprodukci bez mapy. Není prodloužený timeout ani přeskočená kontrola. Lineární API 28 nenahrazuje Android 14+ a TalkBack.
- Vadný soubor nastavení prošel skutečným DataStore corruption handlerem. Postava v Barrens, hotový úkol, poznámka a export zálohy zůstaly stejné. Zápis nového nastavení funguje a starší formát nastavení zůstává čitelný.
- Vynucená chyba úklidu po aktivaci skutečně podepsaných dat 4 ponechala UI, index, diskový ukazatel a znovu otevřenou Room databázi na stejné generaci 4. Osobní profil se nezměnil. Celá dosavadní sada zároveň ověřila migrace, podpisy, souřadnice, neúplné generace, návrat a osobní historii.
- Samostatná kontrola finálního release APK potvrdila kód 11, původní identifikátor a vlastnický certifikát E09/E10, vypnutý debug režim a lokální profileable. Všechny obsahové assets včetně veřejného vydavatelského klíče jsou bajtově stejné jako E10. Room schémata, oprávnění a formát zálohy zůstaly stejné. Testovací data a soukromé klíče nejsou v APK ani ve zdrojovém archivu.

Pět finálních snímků nativního hostitelského UI bylo vizuálně zkontrolovaných: čtyři e11-large-text-*.png a barrens-map.png z průchodu skutečnou generací 4. Nejsou to makety ani screenshoty fyzického telefonu. Hláška dekodéru „unimplemented“ při celém běhu pochází z úmyslně vadného PNG v negativním testu E09; všechny testy uspěly.

## Podpis, obsah a instalace

Dodané APK je release sestavení určené také pro měření, bez debug režimu; obfuskace/R8 není zapnutá. Certifikát SHA-256 e422650dc10efbf7074ae880c0cbc0d7e6d09c8d3ba6a1b1e761f99f441c2f6b je stejný jako E09/E10. Instaluje se **přes E09/E10 bez odinstalování**. Nové podpisové klíče nevznikly. Starší E08 zůstává souběžná a přenáší se z ní záloha, stejně jako v E09/E10.

Herní data se v E11 nemění. APK ponechává fallback 2: 53 položek, 27 poloh a čtyři mapy. Aktivní data 4 zůstanou po aktualizaci aktivní: 139 položek, 62 poloh a pět map. Kdo už má data 4, znovu je neimportuje. Kdo je dosud nemá, použije již vydaný WoW_Forever_E10_data_4.wfp. Nenavyšuje se generace ani datum kontroly herních zdrojů pouze kvůli vydání programu.

## Zbývá na zařízení

Přejímka podle E11_TELEFON.md: skutečné vynucené ukončení a nové spuštění, přenos zálohy na druhý telefon, systémová navigace, nelineární velké písmo, TalkBack a měření studeného startu, hledání, dlouhých snímků a paměti. Nástroj pro sběr Android statistik je připravený a jeho parsování testované; žádný telefonní výsledek se zde nedoplňuje odhadem. Živý HTTPS provoz ani jeho fyzický výpadek se také netestovaly.

M1 zůstává otevřený, poslední obecné potvrzení telefonu je E05. Obsah E10 je stále první dávka, ne celá cesta 1–60 nebo dokončené M2/M3. Nejvhodnější další krok je fyzická kontrola E11 na Samsungu; po ní E12 — podepsané vydání a provoz v přiznaném rozsahu obsahu.
