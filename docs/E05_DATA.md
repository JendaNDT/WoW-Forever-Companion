# E05 — osobní postup a přenosná záloha

## Úkoly a návaznosti

Detail úkolu nabízí Nezahájeno, Rozpracováno, Hotovo a Odloženo. Zápis je ruční podle skutečné hry a ukládá se ihned mimo hlavní vlákno. Neexistující záznam se zobrazuje jako Nezahájeno; výslovný záznam tohoto stavu lze také uchovat v historii. Stabilní ID oddělují různé úkoly stejného názvu.

Pravidlo ALL vyžaduje všechny dokončené předchůdce, ANY alespoň jednoho. Odložený ani rozpracovaný předchůdce se nepočítá. Vyhodnocení se přepočítává z aktuálních stavů, včetně vrácení změny. Potvrzené nesplněné pravidlo hlásí chybějící krok; referenční nebo vypnuté pravidlo nikdy nezmění dostupnost na potvrzenou. Ani splněné předchůdce samy o sobě nedokazují všechny podmínky dostupnosti úkolu.

Současný katalog má pouze neověřené návaznosti z Classicu, bez povolení pro automatické vedení. U nich se ukáže orientační počet dokončených předchůdců a označení „Návaznost k ověření“. Minimální úrovně se nevymýšlejí. Výlučné skupiny se načítají z vazeb `EXCLUSIVE_QUESTS` instalovaného katalogu. Vlastník a cíle tvoří symetricky výlučnou skupinu úkolů, režim musí být ANY a vlastník nesmí být mezi cíli. Aktivní skupina vyžaduje doložené ověření ve vydané hře u pravidla i všech úkolů a herní zdroj pravidla. Dokončený člen blokuje zahájení/dokončení jiné větve; odložení neblokuje. Neověřená nebo vypnutá skupina se jen zobrazí k ověření. Současné podklady nedokládají žádnou takovou skupinu, takže aplikace žádnou skutečnou větev svévolně nezablokuje. Syntetické ověřené skupiny existují pouze v testech.

JSON schéma formátu katalogu 1 rozšiřuje povolené druhy vazeb o EXCLUSIVE_QUESTS; původní balíčky zůstávají platné. Room už ukládá druh vazby jako text, proto nepotřebuje změnu herního schématu. Stejnou sémantiku kontroluje Kotlin i vývojový Python validátor.

Změna dokončeného předchůdce s rozpracovanými nebo hotovými navazujícími záznamy vyžádá potvrzení. Následníci zůstávají zachovaní, pouze se přepočítají podmínky. Návaznosti vedou do detailů úkolů; doložené vazby zadání a odevzdání vedou do detailů NPC a odtud na existující mapové polohy. Souřadnice NPC se nevydávají za přímou polohu úkolu.

## Předměty, odměny a přání

Získání předmětu, jeho záložka a zvolená questová odměna jsou samostatné údaje. Získaný předmět neznamená aktuálně nasazenou výbavu. Záložka předmětu slouží jako přání; po označení získání zůstává uložená a seznam ji ukazuje jako získaný předmět.

U jednoho úkolu lze uchovat jedinou volbu odměny. Změna nebo zrušení existující volby vyžaduje potvrzení; automaticky se nemění získání předmětu ani dokončení úkolu. Podklady zatím obsahují jen Primitive Walking Stick u vybraných variant Vile Familiars. Není to tvrzení úplného seznamu odměn ani doporučení nejlepší výbavy.

Moje cesta přidává filtrovaný deník a přání/získané předměty. Úkoly i jednotlivé položky používají líný seznam. Záznam mimo aktuální katalog zachovává poslední název a ID a nabízí detail osobní historie. Katalog a osobní databáze zůstávají oddělené.

## Ukládání a migrace

Osobní Room databáze přechází z verze 1 na 2 automatickou migrací, bez destruktivního náhradního vytvoření. Původních sedm tabulek zůstává včetně polí a dat. Nové tabulky: `reward_choices`, `undo_actions`, `personal_snapshots`. Exportovaná schémata 1 a 2 jsou součástí zdrojů. Herní Room databáze a JSON katalogu zůstávají ve verzi 1.

Poslední změnu úkolu, získání nebo odměny lze vrátit. Databáze uchovává předchozí a výsledný řádek a kontroluje, zda je výsledek stále aktuální. Vrácení upraví jen dotčený řádek, zachová novější poznámky, profil a záložky a potom záznam vrácení odstraní. Je dostupné i po novém otevření databáze. Nejde o neomezenou historii změn. Import a nahrazení mají vlastní ochranu, nikoli tento jednoúrovňový příkaz.

Aktivní profil se ukládá do DataStore. Starší nastavení dostane výchozí `profile:default`. Volba profilu je v okně Profily a záloha; změny se vždy vztahují k aktivnímu profilu. Během zápisu je přepnutí blokováno. Nastavení mapy, filtrů a prozrazování je společné pro aplikaci, nikoli samostatné pro každou postavu.

## Soubor zálohy, formát 1

Nešifrovaný UTF-8 JSON obsahuje `formatVersion: 1`, `appId: cz.jenda.wowforever`, `gameVariant: WOW_FOREVER`, čas exportu, ID aktivního profilu a pole profilů. Každý profil nese postavu, úkoly, předměty, záložky/přání, poznámky, plánované zastávky, objevenou dopravu a odměny. I tabulky, které ještě nemají ovládání v UI plánovaných etap, se přenášejí. Neobsahuje herní katalog, obrázky map, místní snímky ani příkaz pro vrácení poslední změny. Nastavení mapy/prozrazování není součástí přenosné zálohy.

Export čte všechny profily v jediné databázové transakci, připraví celý platný soubor a teprve potom zapisuje přes Android ContentResolver. Dokument vybírají `CreateDocument` a `OpenDocument`; aplikace nepotřebuje plošné oprávnění úložiště. Přístup je jednorázový, bez trvale uchovávaného URI. Selhání zápisu může ponechat nedokončený dokument u poskytovatele; aplikace výslovně vyzve k novému výběru a nepovažuje jej za hotovou zálohu.

Čtení je omezené na 5 MiB, 100 profilů, 50 000 osobních řádků a hloubku JSON 32. Dekodér odmítá neplatné UTF-8, neznámá pole, nepodporovanou verzi/variantu, duplicitní identity, cizí vlastníky, neúplné či neplatné souřadnice a neplatné stavy. Kontroluje délky textů a metadata postavy. Historická ID položek mohou chybět v dnešním katalogu. Oblast postavy musí být jednou ze čtyř právě podporovaných map; poznámky z dalších map lze uchovat jako momentálně neumístěné. Novější nepodporovaná záloha má vysvětlení s číslem verze; nic se nezapisuje před úplnou validací.

Náhled před obnovou uvádí profily a počty záznamů. Výchozí import vytváří nové profily s novými UUID profilů i poznámek; původní data se nepřepisují. Záložky a další řádky se převážou na nový profil. Celkem lze uchovat nejvýše 100 profilů. Existující profil lze výslovně nahradit vybraným profilem ze zálohy; ostatní profily zůstanou zachované.

Nahrazení v jedné Room transakci nejprve uloží úplný místní snímek cíle, nahradí jeho osobní řádky a vymaže starý příkaz pro vrácení změny. Jakákoliv chyba transakce vrací i částečné zápisy a snímek. Poslední tři snímky lze po potvrzení obnovit; i před jejich obnovou vznikne snímek současného stavu. Časové pořadí snímků je monotónní i při rychlém opakování. Místní snímek nenahrazuje soubor uložený mimo aplikaci: odinstalování nebo smazání dat odstraní i snímky.

## Rozsah ověření

Ověření a přesné výsledky jsou v `E05_ZPRAVA.md` a `qa/e05`. Hostitelský přenos uzavře zdrojovou databázi a načte soubor do jiné nezávislé databáze, se dvěma profily a kolizí původního ID poznámky. Ověření dvou fyzických Android zařízení a skutečných poskytovatelů dokumentů zůstává součástí telefonního přejímacího postupu. Hostitelská simulace není vydávaná za takový test.

Primární podklady k použitému rozhraní: [Android Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files), [migrace Room](https://developer.android.com/training/data-storage/room/migrating-db-versions), [transakce Room](https://developer.android.com/reference/androidx/room/RoomDatabaseKt#withTransaction(androidx.room.RoomDatabase,kotlin.coroutines.SuspendFunction0)). K implementaci se nepřidávají nové závislosti.
