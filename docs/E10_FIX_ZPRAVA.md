# Oprava pádu E10 — 0.15.1-e10-fix1

Jenda 25. 10. 2026 nahlásil zavření aplikace při přípravě offline obsahu. Z telefonu není k dispozici systémový crash log; příčina na konkrétním zařízení tedy není přímo doložená jeho logem. Podařilo se však reprodukovat skutečnou paměťovou chybu produkčního výpočtu doporučení a opravit celý průchod přípravou při omezené paměti.

## Reprodukovaná chyba a oprava

Původní RecommendationEngine vytvářel u každé cílové položky také odkazy na všechny ostatní cíle společné skupiny. Například skupina zdrojů předmětu měla 1 491 NPC; vznikala nepotřebná propojení každého zdroje se všemi dalšími. Celý katalog tak mohl vytvářet miliony dočasných i uchovávaných odkazů. Původní test `largeCatalogRecommendationsAreStillReferencesAndNewMapBackupsRoundTrip` s limitem 256 MiB skončil `OutOfMemoryError` v `RecommendationEngine.evaluate` při růstu ArrayList. Důkaz je v qa/e10fix/before-recommendations-256m.xml.

Nyní jsou uchovávané přímé vazby: předmět → jeho zdroje a zdroj → související předmět. Společný drop nedělá z jiných NPC přímé související položky. Skutečné úkoly, vazby, přehled zdrojů a herní obsah se nezkracují. Test 1 500 společných zdrojů očekává celkem 3 000 skutečných odkazů, nikoli miliony vazeb zdrojů mezi sebou.

## Příprava obsahu

- JSON se dekóduje přímo z omezeného UTF-8 streamu. Zůstává limit 32 MiB, maximální hloubka 32, odmítnutí neznámých polí i dokumentů za koncem. Testy zahrnují sedmibajtové bloky s češtinou/escapováním, příliš hlubokou strukturu a překročení limitu přes koncové mezery.
- Podepsaný katalog se při instalaci podruhé nedekóduje. Databáze dostává už dekódovaný obsah spolu s hashem ověřeného souboru z manifestu; validace obsahu zůstává zachovaná.
- Zápisy zdrojů, záznamů, poloh a cílů vazeb probíhají v malých dávkách v jedné transakci. Záznamy se pro UI čtou kurzorem; pro URL zdrojů se nečtou celé payloady.
- Počáteční mapa a katalog používají stejné dekódované položky. Stav balíčku čte malý podepsaný manifest a ověřené mapové řádky databáze, nikoli všech 25,9 MB katalogu.
- Automatická aktivace neuchovává plný kandidátní katalog během další kontroly. O tom, co se aktivuje, rozhoduje ověřený manifest na disku.
- Již komprimovaný WFP je v APK uložený bez druhé komprese. Primární čtení používá `AssetFileDescriptor`; starší komprimované assety mají proudovou záložní cestu. Výsledný APK to ověřuje samostatně.
- Obnovení po návratu znovu použije přesně odpovídající existující generaci po kompletní kontrole podpisu, hashů, katalogu a databáze; nevyžaduje další kopii 121 MB archivu.

Datové a zálohovací formáty, Room schéma, identifikátor aplikace, vlastník podpisu a celý podepsaný datový balíček 7 se nemění. Nevyžaduje se odinstalování ani smazání osobních dat. Nezvyšuje se Android `largeHeap` jako náhrada opravy.

## Omezená paměť — hotové výsledky

- **256 MiB:** původně padající výpočet nyní prošel; spolu s čtyřmi novými regresními/bezpečnostními testy 5 testů, bez chyby. Výsledek a XML jsou v qa/e10fix/after-256m-*.
- **384 MiB:** celá skutečná přibalená aplikace včetně prvního spuštění, vyšších oblastí, třídního úkolu a opětovného otevření prošla. Druhý průchod testuje upgrade z potvrzených dat 6, oba profily, poznámku, nový region v záloze, návrat a obnovení dat 7. Dva testy ve dvou oddělených procesech, bez chyby; XML after-384m-*.
- Hostitelské prostředí Robolectric při původním nízkopaměťovém testu také vyhazovalo chybu FileMap při kopírování celého velkého assetu. Jde o jeho implementaci assetů, nikoli o telefonní stack trace. Výše uvedená doporučovací chyba je prokázaná zvlášť v produkčním kódu bez tohoto assetového prostředí.
- **27 Python kontrol** dat a identity nadále prochází. Herní data se nemění.

## Finální ověření

- **Čisté sestavení úspěšné, 7 min 45 s, 150 úloh (149 provedených / 1 aktuální).** Sestavené Debug, Release a AndroidTest APK.
- **210 testů aplikace / 53 sad, 0 chyb, 0 selhání a 0 vynechaných**, včetně všech starších regresí. Výsledné XML je v qa/e10fix.
- Lint debug 0 chyb / 5 upozornění, release 0 chyb / 4 upozornění. Zbývají dřívější kategorie upozornění na novější závislosti, kontrolu volného místa a atribut pro nižší API. Nejsou vypnuté kontroly prostředků nebo bezpečnosti.
- **Skutečný APK ověřený:** původní vlastník a ID, bez debug příznaku, kód 16 > 15, všechny assets shodné se zdroji. Kontrola skutečného ZIP záznamu potvrzuje WFP jako ZIP_STORED, nikoli opakovaně komprimovaný asset.
- **Datový balíček 7 zůstává přesně stejný:** SHA-256 ba94fb218ad63cae250c7d32b32ef16532c4daa5b8ab822685d5b623a8cb563e; 12 352 položek, 18 441 poloh a 61 map. Znovu ověřený podpis, dlaždice a pixelová rekonstrukce 56 nových podkladů. Schémata Room se nezměnila.

APK: 146 726 550 bajtů. SHA-256: `683a2a3072ee6a1b05ec1b8f2c8ae0c04d89c819dca556a321ce0c74edb59593`. Revize sestavení: `f0afabc601dc1cc415a804d0fcacc55654b3c0d4`. Build inputs SHA-256: `8796129975a9c68bb88f262d01e64497529fe9de363c028c62594ee35a99b7f8`. Certifikát APK je původní E09+.

Zdrojový archiv zachovává revizi APK i pozdější revizi dokumentace a úplnou Git historii. Soukromé klíče nejsou součástí archivu. Samotný datový archiv, profily ani historie úkolů se neopravují mazáním.

Následuje jedna opakovaná telefonní kontrola přípravy a opětovného otevření. Hostitelské limity nejsou vydávány za měření konkrétního telefonu ani za herní ověření referenčních údajů.
