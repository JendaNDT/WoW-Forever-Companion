# E11 — spolehlivost a výkon

25. 10. 2026 · 0.11.0-e11 / kód 11 · obsah beze změny: fallback 2 a samostatná distribuce 4.

## Konkrétní změny

- Neměnný CatalogSearchIndex vzniká pro každou generaci na pracovním vlákně. Česká diakritika a názvy/aliasy se normalizují jednou. Hledání už znovu netřídí celý katalog porovnávací funkcí, která opakovaně přepočítává text. Zachovává shody, pořadí názvů před popisy a stabilní pořadí remíz. UI si pamatuje výsledek do změny dotazu, filtru nebo generace. Vazby položka → polohy jsou indexované; místní hledání neprochází všechny polohy pro každou nalezenou položku.
- MapComposeEngine drží nejvýše dva mapové stavy: aktuální a poslední. Před uvolněním uloží výřez a vypne pracovníky starého stavu. Návrat obnoví původní souřadnice, měřítko a otočení. Při oznámení nedostatku paměti uvolní i neaktivní sousední stav. Nejvýše dva stavy nejsou limit celkové paměti procesu; PSS a životní cyklus Androidu se měří odděleně.
- Mapové značky mají plochu 48 dp a český sémantický popis; seskupení používá odpovídající prahovou vzdálenost. Výprava a vybraný bod zůstávají mimo seskupení. Seznam bodů a katalog poskytují alternativu k ovládání značek.
- Vyhledávací panely se posouvají jako jeden seznam včetně ovládání. Boční navigace je posuvná pro nízké okno a velké písmo. Popisky hledání se mohou zalomit nad vstupním polem. V malém okně s velkým písmem se zkrátí záhlaví, nástroje mapy jsou posuvné a detail vybraného bodu se otevře samostatně; trvalá karta tak nevytlačí samotnou mapu.
- Poškozený soubor nastavení obnoví DataStore na výchozí nastavení a zobrazí srozumitelné upozornění. Osobní Room databáze ani aktivní ukazatel obsahu se nemažou. Starší soubor nastavení zůstává čitelný; volitelné settingsRecovered je pouze příznak obnovy nastavení. Záloha postupu i Room schémata se nemění.
- Chyba následného úklidu po již potvrzeném přepnutí dat nesmí vrátit diskový ukazatel a současně ponechat nový katalog v UI. Úklid je oddělený od potvrzení změny, jeho neúspěch ponechá nový ověřený celek a oznámí odložený úklid. Příští spuštění jej zkusí znovu.

## Měření a jeho hranice

Hostitelský benchmark porovnává doslovný algoritmus E10 s indexem E11 ve stejném JVM. Obsahuje 10 000 oddělených syntetických položek, 16 zahřívacích dotazů, 24 měřených dotazů v každé variantě a porovnání všech výsledků. Pořadí měření obou variant se střídá. Zaznamenává vytvoření indexu a P50/P95/maximum dotazů. Výstup qa/e11/e11-search.json je měření jádra hledání na počítači, nikoli celková odezva klávesnice a UI na Samsungu. Syntetická sada není importovaný herní katalog; limit katalogu 2 MiB zůstává zachovaný.

Nativní grafický test ověřuje 5 000 kandidátních bodů v oblasti, skutečné seskupení do menšího počtu vykreslených prvků a opakované přiblížení, zaměření, sever a oddálení. Druhý scénář přepne 40krát mapu, hlídá nejvýše dva stavy, návrat k uloženému výřezu a uvolnění neaktivního stavu při nedostatku paměti. Stejná kontrola je připravená i v instrumentačním APK pro Android; E11SearchDeviceTest doplňuje měření jádra indexu pro 10 000 testovacích položek. Tyto testy spouštěj na odděleném testovacím zařízení, nikoli s očekáváním, že všechny starší testy budou odpovídat libovolnému osobnímu stavu. Krátký test neprokazuje dvacetiminutovou stabilitu PSS ani 60 FPS.

Hostitelské UI kontroly používají dvojnásobné písmo a malé výchozí telefonní okno; krátké okno na šířku má samostatnou konfiguraci. Dialogy záměrně nepoužívají explicitní w/h qualifiers: známá chyba Robolectric #8460 způsobuje oscilaci omezení Dialog/TextField i u nezávislé reprodukce bez mapy (https://github.com/robolectric/robolectric/issues/8460). Nenastavuje se delší timeout, nepřeskakuje se hledání ani se nemění produkční rozměry kvůli tomuto omezení testů. API 28 v Robolectricu má lineární změnu velikosti; nenahrazuje nelineární škálování Androidu 14+, TalkBack ani fyzická systémová gesta. Databázové migrace, importy, souřadnice, vadné balíčky a vratné generace ověřuje také celá dosavadní regresní sada.

## Příprava měření Androidu

Dodané APK je podepsané release sestavení bez debug režimu, s lokálním profileable shell pro Android 10+. Neobsahuje analytiku ani sběr dat. Stejný vlastnický certifikát a identifikátor jako E09/E10 umožňují běžnou aktualizaci bez odinstalování. Syntetické položky, testovací datové sady a soukromé klíče nejsou v APK.

Trace forever.startup vymezuje načítání aplikace od zahájení po připravení modelu. ReportDrawnWhen ohlašuje vykreslení připraveného UI; negarantuje, že již dorazily všechny viditelné mapové dlaždice. forever.catalog.search označuje synchronní průchod indexem v UI. Tyto události lze zobrazit v Perfettu.

Nástroj tools/e11_device_capture.py vyžaduje výslovný adb serial a měřitelné release sestavení. Nevybírá telefon automaticky, neinstaluje APK, nemění systémové písmo, osobní profil ani databáze. Měření spuštění cíleně ukončuje pouze proces průvodce. Bez zařízení uloží stav pending_no_selected_adb_device a nevyrobí výkonnostní čísla.

```sh
python3 tools/e11_device_capture.py --serial SERIAL --output ../e11-phone preflight
python3 tools/e11_device_capture.py --serial SERIAL --output ../e11-phone-startup startup
python3 tools/e11_device_capture.py --serial SERIAL --output ../e11-phone-map --seconds 1200 map-session
```

Před startup měřením ručně otevři cílovou oblast a zavři dialogy. Startup zaznamená pět spuštění po force-stop, syrová am start -W čísla a vlastní log připraveného UI. Map-session sbírá prvních 30 sekund Perfetta, vzorky dumpsys meminfo po minutě a gfxinfo/framestats za celý scénář, zatímco člověk opakuje přepínání a přibližování. Rozdíl FrameCompleted − IntendedVsync se neoznačuje za FPS. Vadné či neúplné řádky se nepočítají, chybějící PSS není nula. Závěry o trvalém růstu paměti, 60 FPS a třísekundovém startu vyžadují vyhodnocení skutečných záznamů. Stav completed u sběru znamená dokončený záznam, nikoli automaticky splněné výkonové cíle.

Herní obsah zůstává generace 4 (139 položek, 62 poloh, pět map) po samostatném importu E10. Verze APK 11 není generace obsahu. Není potřeba vytvářet nový balíček ani podpisový klíč.

## Primární technické podklady

- https://developer.android.com/guide/topics/manifest/profileable-element
- https://developer.android.com/topic/performance/tracing/custom-events
- https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview
- https://developer.android.com/guide/topics/ui/accessibility/apps
- https://github.com/p-lr/MapCompose/tree/v3.2.0
