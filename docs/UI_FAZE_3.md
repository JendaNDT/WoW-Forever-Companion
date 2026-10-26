# Mapa a pohodlí při hraní — Fáze 3

Verze **0.19.0-ui3 (20)**, data **8**. Navazuje na telefonem potvrzenou Fázi 2. Telefonní přejímka nové verze zatím neproběhla.

## Rozsah

- Mapa využívá celou dostupnou šířku. Nad ní je výběr oblasti, Hledat a doplňující filtry. Vrstvy Úkoly / Postavy / Doprava / Moje jsou přímo na obrazovce. Postavy zahrnují také předměty a objekty; Úkoly mohou zobrazit i související NPC. Zůstává přiblížení gesty, přístupné akce Přiblížit / Oddálit a tlačítko Celá mapa. Copyright je v rohu mapy a celý v O aplikaci.
- Značky mají vizuálně 32 dp a dotykovou plochu 48 dp: ! zadání, ? odevzdání, ◆ cíl nebo zdroj předmětu, ✎ poznámka, čísla zastávek výpravy. Role se odvozují pouze z existujících vazeb. Nemění původ ani ověření dat a nedokazují dostupnost nebo připravenost úkolu k odevzdání.
- Hotové úkoly jsou ve výchozím stavu skryté. V panelu oblasti a doplňujících filtrech je lze zapnout. U NPC s více úkoly zůstávají role ostatních nedokončených úkolů; při zapnuté vrstvě Postavy může NPC zůstat jako běžná postava. Výslovné Na mapě dočasně ukáže i jinak skrytý vybraný bod do zavření výběru, bez přepsání volby hotových úkolů.
- Panel V této oblasti nahrazuje dialog Body. Sdílí filtry s mapou, má legendu a odkazy na mapu a společný detail. Při otevření detailu se předchozí panel zavře. Prohlížení jiné mapy nemění oblast postavy; čip Jsem tady ji změní až po klepnutí a uloží.
- Po změně stavu úkolu, získání předmětu nebo odměny je 5 sekund lišta Vrátit. Je i ve společném detailu. Akce se vztahuje ke konkrétnímu profilu a konkrétní poslední změně, vyprší i přes otočení a nevrací novější změnu. Potvrzení změn navazujících úkolů a odměn zůstává zachované.
- Úkoly → Co dál končí tlačítkem Než odejdeš z oblasti. Panel ukáže rozpracované záznamy, nedokončené třídní úkoly do úrovně postavy + 2 a vlastní přání se známou vazbou na aktuální oblast. Skupiny se neduplikují. Prázdný seznam nepotvrzuje vyčištění oblasti.
- Při první instalaci je jedna úvodní obrazovka: rasa (Ork), úroveň 1, Durotar a Začít. Umožňuje rovnou obnovit vlastní zálohu. Existující uložené nastavení nebo osobní záznamy přeskočí úvod, aby nepřepsal postavu. Nápovědu k poznámce lze na mapě jednou zavřít.
- Nastavení ukazuje datum poslední úspěšné zálohy. Po sedmi dnech nabídne jemnou připomínku, pokud od poslední zálohy vznikly změny. Zaznamenávají se i mazání, změny profilu a obnova; neúspěšný export datum nezmění. Záloha je dál ve formátu 1 a obsahuje všechny profily; metadata připomínky jsou pouze v Session.
- Nastavení → Zobrazení má volbu Nezhasínat displej na mapě (výchozí vypnuto). Po odchodu z mapy, pozastavení aplikace a odstranění obrazovky se požadavek uvolní. Volba se ukládá.

## Ověření

Všechny automatické kontroly prošly na revizi `891372daaec42b515cd6e0ab15d004a6fa32e1d4`:

- [Android checks 40358040776](https://github.com/JendaNDT/WoW-Forever-Companion/actions/runs/40358040776): **241 aplikačních testů / 62 sad**, bez selhání, chyb nebo přeskočení; **35 Python testů**. Lint debug a release bez chyb (17 / 18 upozornění). Prošlo sestavení i ověření identity APK a všech 93 přibalených souborů.
- [E11 reliability 40358041033](https://github.com/JendaNDT/WoW-Forever-Companion/actions/runs/40358041033): **Android 8 / API 26 a Android 16 / API 36**, každý 5 instrumentačních testů, 2 skutečná ukončení procesu a restarty, obnova zálohy a zátěž všech **61 map / 17 330 bodů**. API 26: 318 614 ms, 915 změn oblastí / 15 cyklů. API 36: 320 384 ms, 854 změn / 14 cyklů. V obou nejvýše dva zachované snímky mapy.
- Nových 13 scénářů pokrývá role u NPC s více úkoly, dokončené úkoly a výslovné zobrazení bodu, staré Session a novou instalaci, připomínku zálohy, první spuštění s otočením a validací úrovně, Jsem tady, společný detail, přesné a vypršené Vrátit, nezhasínání mapy, úspěšný i neúspěšný export a velké písmo. Původní sady předchozích etap zůstávají zachované.
- Vizuálně zkontrolované snímky prvního spuštění, mapy, panelu oblasti, lišty Vrátit, Než odejdeš a panelu při písmu 200 %. Lišta má vlastní místo v rozložení, aby nezakrývala tlačítka. Jsem tady zachová prohlížené patro a datum zálohy se přepočítá i po návratu do aplikace.

Skutečné souhrny výsledků: [qa/ui3](qa/ui3). Jde o emulátory; stav vypnutých rádií není samostatně prokázaný.

## Předané APK

- Soubor: `Warlock_Forever_0.19.0_UI3.apk`, **150 520 870 B**.
- SHA-256: `85341e92b50213bcb8a5c81323fbf75b58014c1357f9a802a581a26a19e2f118`.
- Zdrojová revize: `891372daaec42b515cd6e0ab15d004a6fa32e1d4`, čistá a dohledatelná; produkční vstupy (156 souborů): `79bc42102d38a1b6ea943206325cbaf73e77b36e73a92339c16d12fcd6fd14ff`. Následné zapsání dokumentace tyto vstupy nemění.
- Vývojový podpis: `13745b7a4c824c1875d41f0926a65b55dfb8faf12fde906a92017830273bb89c`. Liší se od předaného APK Fáze 2. **Před odinstalací exportovat zálohu, poté čistě nainstalovat novou verzi a zálohu obnovit.** Přímá aktualizace přes předchozí podpis není možná.

Telefonní přejímka verze 20 zatím neproběhla. Potvrzení Fáze 2 / verze 19 je historické.

Databáze, formát zálohy, balíčky ani herní data se nemění. Samostatné měření výkonu na fyzickém telefonu, TalkBack, obnova na druhém zařízení a herní potvrzení údajů nejsou automatickými běhy nahrazené.

## Doporučená kontrola na telefonu

1. Před čistou instalací exportovat zálohu a po instalaci ji obnovit. Zkontrolovat postavu a osobní záznamy.
2. Prohlédnout mapu, vrstvy a V této oblasti; vyzkoušet zadání a odevzdání, hotové úkoly i čísla výpravy.
3. Otevřít jinou mapu a až čipem Jsem tady změnit oblast postavy.
4. Změnit úkol a do 5 sekund použít Vrátit. Otevřít Než odejdeš, detail a vrátit se do seznamu.
5. Zapnout nezhasínání na mapě, přejít do Hledat a aplikaci odložit. Zkontrolovat velké písmo, orientaci a datum uložené zálohy.

Další doporučený krok po předání je funkční kontrola celé Fáze 3 na telefonu. Herní přejímka zůstává až po vydání hry.
