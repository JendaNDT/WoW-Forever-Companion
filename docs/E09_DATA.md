# E09 — balíčky a aktualizace

24. 10. 2026 · aplikace 0.9.0-e09 / kód 9.

## Rozsah

Nativní správce v Postava → Balíčky a mapy ukazuje čtyři offline mapy, jejich velikosti a aktivní generaci. Umožňuje ruční výběr souboru, zjištění velikosti přímého HTTPS stažení, ruční stažení s výchozí Wi-Fi, zrušení přípravy, náhled změn, potvrzené přepnutí, návrat a smazání stažených generací. Bez sítě nadále fungují mapy, hledání, průvodce, poznámky, výprava a osobní záloha.

Jeden balíček obsahuje celý kompatibilní katalog a mapy. Samostatná oblast se v tomto vydání nemaže uvnitř aktivní generace, protože by vznikla neúplná kombinace. Smazání stažených generací vrací bezpečný přibalený celek. Nové mapy a změna souřadnicové soustavy jsou zatím odmítnuté, aby se staré polohové poznámky neposunuly na jiné místo. Rozšíření do dalších oblastí patří E10 s doloženými podklady.

Veřejný aktualizační server nebyl založen ani nasazen. Uživatel může vybrat stažený soubor nebo zadat přímý HTTPS odkaz na podepsaný soubor. Automatické kontroly, WorkManager a stahování na pozadí nejsou zapnuté; tato etapa ověřuje ruční průchod. Přerušený přenos se opakuje od začátku, neobnovuje se po částech.

## Formát .wfp

ZIP bez adresářových položek obsahuje manifest.json, binární manifest.sig, catalog.json a přesný seznam mapových PNG. Manifest 1 nese packageId, generation, gameVariant, contentSchema, minimumAppCode, checkedOn, dependencies, souřadnicové rámce s buildem a seznam cest, velikostí a SHA-256. Neznámé závislosti, schéma nebo požadavek na novější APK se odmítnou.

Podpis SHA256withRSA se ověřuje proti veřejnému klíči přibalenému v APK. Neověřuje pouze součet ze stejného neznámého archivu. Tento podpis potvrzuje původ od vydavatele osobního průvodce; není podpisem Blizzardu a nezmění beta záznam na ověření vydané hry. Manifest a data nemohou při importu přepsat důvěryhodný veřejný klíč aplikace.

Omezení: archiv nejvýše 128 MiB, rozbalená data 256 MiB, manifest 512 KiB, katalog 2 MiB, jednotlivá dlaždice 4 MiB a nejvýše 8192 datových souborů. Povoleny jsou pouze catalog.json a určené cesty PNG. Kontrolují se duplicity, chybějící i přebývající soubory, rozbalená délka, součty, přesná pyramida dlaždic, formát a skutečné dekódování PNG 256 × 256. Zápis mimo soukromou dočasnou složku není povolený.

Po podpisu se spouští dosavadní úplná sémantická kontrola katalogu: hra, identity, zdroje, polohy, vazby, cykly, podmínky doporučení a doprava. Podpis sám neobchází žádnou herní validaci. Povoleny zůstávají čtyři dosavadní mapové rámce se stejnou geometrií a souřadnicovou verzí. Jejich obrazové dlaždice a katalog lze aktualizovat společně.

## Přenos a místo

Při HTTPS stažení se nejdřív vyžádá velikost metodou HEAD a uživatel teprve poté spustí přenos. Výchozí volba povolí pouze Wi-Fi; mobilní data se musí výslovně povolit vypnutím této volby. Síťová oprávnění jsou INTERNET a ACCESS_NETWORK_STATE, bez GPS nebo plošného přístupu k souborům. Soubor vybírá systémový výběr dokumentů.

Povoleno je HTTPS, název serveru místo číselné IP adresy, standardní port a žádné přihlašovací údaje v URL. Přesměrování se nenásledují. TLS se používá se standardním ověřením certifikátů; neexistuje výjimka „důvěřovat všem“. Server neposkytující velikost lze obejít ručním výběrem již staženého souboru. Při změně délky mezi HEAD a GET se nabídka musí zjistit znovu. Výpadek, neúplná délka a chyba serveru neaktivují přijatou část.

Volné místo se kontroluje před přenosem a znovu před rozbalením: celá nová generace, příprava databáze a rezerva 20 MiB. Předchozí generace se předem nemaže. Dočasné vstupy se při chybě nebo zrušení zavřou a odstraní. Při čekání na blokující síťové čtení se zrušení může projevit až po síťovém časovém limitu, nastaveném na 10 sekund.

## Aktivní generace a obnova

1. Příjem do odděleného souboru incoming v soukromém trvalém úložišti.
2. Podpis, kompatibilita, místo, bezpečné rozbalení, součty a úplná kontrola.
3. Založení samostatné Room databáze katalogu 3, kontrola a uzavření WAL. Uchová se i součet odvozené databáze.
4. Přesun úplné složky na stabilní jméno podle generace a součtu manifestu. Tím se sama neaktivuje.
5. Náhled verze a počtu položek, map a dotčené osobní historie. Potvrzení uživatele.
6. Další kontrola hotové generace a jediný atomický přepis ukazatele pomocí Android AtomicFile. Předchozí aktivní generace se uloží jako předchozí.
7. Znovunačtení katalogu, doporučení a dopravního modelu; nový mapový adaptér čte dlaždice právě této generace. Starý adaptér se uzavře. Neaktivní nepotřebné složky se odstraní až potom.

Při spuštění se ověřuje celistvost vybrané generace i SQLite quick_check a metadata. Chybějící soubor nebo poškozená databáze vrátí předchozí ověřenou generaci, případně přibalený celek. Opuštěná příprava se nepovažuje za instalaci. Uchovávají se nejvýše dvě potřebné stažené generace; přibalený celek zůstává v APK. Návrat přepíná úplné databáze, nesnižuje generaci přepisováním živého katalogu.

## Osobní záznamy

Osobní Room databáze zůstává ve verzi 2 a záloha ve formátu 1. Aktualizace katalogu nikdy neotevírá osobní databázi pro zápis. Náhled pouze spočítá, které záznamy odkazují na mizející katalogové položky, napříč profily. Historické názvy, úkoly, předměty, záložky, poznámky, opakované zastávky i objevená doprava zůstávají uložené. Zmizelý cíl se v seznamu zachová bez vymyšlené polohy. Návrat ke starším datům nevrací osobní postup do minulosti.

## Kontrolní aktualizace a příprava dalších dat

Přibalený katalog zůstává bajtově shodný s E08: generace 2, 53 položek, 27 beta poloh, 49 vazeb a 59 zdrojů. Soubor WoW_Forever_data_3_kontrola.wfp obsahuje tentýž herní obsah a stejné mapy; mění pouze číslo generace na 3. Slouží k reálnému vyzkoušení aktualizace a návratu. Nepřidává herní záznamy, ověřené trasy ani potvrzení vydané hry. Podpis patří novému vlastnickému klíči obsahu.

Nástroj tools/build_package.py sestaví balíček z validovaného katalogu a odpovídajících dlaždic a výsledek zkontroluje tools/validate_package.py. Vyžaduje OpenSSL a soukromý podpisový klíč mimo projekt; odmítne klíč neodpovídající veřejnému klíči v APK. Samostatný validátor nepotřebuje soukromý klíč. Zdrojový archiv ani Git historie soukromé klíče neobsahují. Pro vlastní úpravu obsahu je potřeba zvýšit generaci, zkontrolovat zdroje a sestavit nový podepsaný balíček.

## Podpis APK a souběžná instalace

Údržba pracovního prostředí odstranila původní dočasný klíč APK z E01–E08. Z veřejného certifikátu nebo APK ho nelze obnovit. E09 proto používá nový stálý identifikátor cz.jenda.wowforever.guide, nový vlastnický podpis a název Forever průvodce E09. Namespace zdrojů se nemění.

E09 se instaluje vedle staré aplikace. Starou aplikaci neodinstalovávat před exportem a ověřením přenosu profilu. Přenos probíhá již existující osobní zálohou, nikoli přímým čtením soukromé databáze druhé aplikace. Samostatný soukromý obnovovací archiv obsahuje vlastnický keystore, jeho údaje a klíč vydavatele dat pro budoucí aktualizace; nepatří do veřejného repozitáře. Další verze mají zachovat tento nový identifikátor i podpis.

## Hranice ověření

Výsledky jsou v E09_ZPRAVA.md a qa/e09. Hostitelské Android/Compose testy nejsou skutečné zařízení ani skutečný přenos přes poskytovatele dokumentů. Chyby HTTPS se ověřují řízeným síťovým adaptérem se skutečným následným ověřením podpisu a souborů. Veřejný živý server, fyzický výpadek sítě, vynucené ukončení procesu na telefonu a přenos zálohy mezi dvěma fyzickými přístroji zůstávají k telefonní přejímce. M1 zůstává otevřený; žádná beta reference nebyla povýšena na ověřený herní obsah.
