# E06 — vysvětlitelná osobní doporučení

## Rozsah dat a výsledků

Obrazovka Teď pro mě vyhodnocuje uloženou postavu a osobní postup. Má pět pohledů: Teď, Před odchodem, K ověření, Podmínky a Odložené. Výsledek odděluje dostupnost od ručního stavu úkolu. Hotový úkol, získaný předmět a zaznamenaná objevená doprava odpadnou podle stabilního ID; pouhá záložka ani odložení neznamená splnění. Osobní cíle zahrnují rozpracované úkoly a nesplněné záložky/přání, včetně historie mimo aktuální katalog.

Skutečný přibalený herní balíček se nemění: 21 položek, osm beta poloh NPC, čtyři mapy povrchu a 25 vazeb. **Obsahuje nula úplných ověřených pravidel doporučení.** E06 proto tyto podklady ukazuje k ověření a netvrdí, že nějaký quest už určitě lze přijmout. Smysluplná osobní část funguje i nyní podle vlastních záložek a rozpracovaných úkolů. Doplnění skutečných ověřených pravidel a obsahu je úkolem E07. Syntetická pravidla z testů nejsou součástí APK.

## Podmínky a nejistota

`RecommendationEngine` pracuje bez síťového připojení, jazykového modelu a API klíče. Dostupnost má stavy Dostupné podle podkladů, Chybí podmínka, Později nebo jinde, Vyloučené podmínkou a K ověření.

Podmínky mohou být vnořené ALL a ANY. ALL vyžaduje všechny kroky, ANY jednu splněnou možnost. Neznámá alternativa v ANY brání tvrzení, že všechny možnosti chybí. Dokončený quest je odlišný od rozpracovaného a odloženého. Volba odměny je odlišná od získání předmětu. Zápis jiné volby může vyloučit variantu vyžadující konkrétní odměnu; chybějící záznam volby je nejistota.

Podporované listy podmínek: frakce, povolání, rasa, minimální a výslovně doložená maximální úroveň, režim, profese, oblast, dokončení questů, získání předmětů a volba odměny. Profese a režim se vyžadují jen u pravidla, které je obsahuje. Prázdný údaj se nepovažuje za potvrzený nesoulad. Textové režimy/profese se porovnávají bez diakritiky, velikosti písmen a okrajových mezer; nepřekládají se ani nedoplňují odhadem.

Úplné pravidlo může výslovně říci `always`, nebo přiznat neznámou podmínku pomocí `unknown`. Chybějící pravidlo znamená neověřenou dostupnost. Kladné potvrzení vyžaduje potvrzenou položku, úplné potvrzené pravidlo a splnění známých podmínek. Neověřená nebo vypnutá návaznost v katalogu zůstává nejistotou, i když je zaznamenaná jako dokončená. Vyšší úroveň sama o sobě nevytváří varování, že byla příležitost zmeškaná.

Ověřená výlučná skupina z E05 se uplatňuje i zde. Potvrzená jiná dokončená větev vyloučí neslučitelnou možnost. Neověřená skupina takový závěr nevytváří. Vlastní úkoly mimo katalog mají zachovaný název a ID, ale žádné automaticky potvrzené podmínky.

## Pravidla v obsahovém balíčku

JSON formát katalogu 1 rozšiřuje volitelné pole `guideRules`; chybějící pole má význam prázdného seznamu. Původní balíčky se nadále načítají. Každé pravidlo má stabilní klíč `wow-forever:rule:…`, ID položky, podmínku, důležitost, český důvod, stav ověření, zdroje a volitelné oblasti, časové okno a odkaz na doloženou výlučnou skupinu.

Podmínka používá JSON rozlišovač `type`: `always`, `unknown`, `all`, `any`, `faction`, `class`, `race`, `level`, `mode`, `profession`, `area`, `quest_completed`, `item_acquired`, `reward_selected`. Přesná struktura je v `content/schema-v1.json` a třídách `content/GuideRule.kt`. Do produkčních podkladů se nepřidávají cvičná pravidla jen pro zaplnění obrazovky.

Validátor vyžaduje existující typově správné cíle a zdroje, jediné pravidlo pro jednu položku a smysluplné rozsahy. Potvrzené pravidlo musí mít herní důkaz z vydané hry a potvrzenou vlastní položku. Odkazované questy/předměty musí být v potvrzeném pravidle také potvrzené; pravidlo o odměně vyžaduje odpovídající aktivní potvrzenou vazbu odměny. Chybné nebo neznámé podmínky se neignorují.

Limit je 1000 pravidel, 200 uzlů na pravidlo a hloubka podmínky 8. Před dekódováním se navíc kontroluje strukturální hloubka celého JSON nejvýše 32, s ignorováním závorek uvnitř řetězců. Limit celého obsahového balíčku zůstává 2 MiB. Kotlin validátor i Python nástroj kontrolují stejný formát a význam; společná serializovaná testovací sada prochází oběma.

## Řazení a konkrétní důvod

Nejprve se řadí dostupná doložená výlučná nebo časová volba, potom důležité kroky Warlocka. Dále se zohlední rozpracování, uživatelova přednost, kategorie a shoda s oblastí. Volba Vyváženě / Objevování / Vybavení mění pořadí, nikoli splnění podmínek nebo věrohodnost. Nevzniká tvrzení o nejlepší výbavě.

Důvod doporučení pochází ze zdrojovaného pravidla. Počet dalších návazností se počítá z potvrzených ALL/ANY vazeb, které konkrétní dokončení skutečně nově splní. Počet nezahrnuje hotové nebo výlučnou volbou vyloučené následníky a nepřičítá již splněnou ANY větev. Text výslovně odlišuje doplnění předchůdců od všech ostatních podmínek následníka.

Vysvětlení nabízí odkazy do podmínek a souvisejících záznamů. Ty vedou do dosavadního katalogu a odtud na doloženou mapovou polohu. Poloha NPC se nevydává za souřadnice odměny nebo questu. Oblast doporučení lze převzít z výslovného pravidla, přímé polohy nebo doložené souvislosti s NPC/questem. Neznámá oblast je viditelně označená; poznámka o oblasti nepředstavuje ověřenou trasu. Přesná blízkost se nepočítá, protože aplikace nezná aktuální herní souřadnice hráče.

## Časová platnost

Volitelné časové okno obsahuje začátek a konec jako absolutní čas v milisekundách a platné časové pásmo pro zobrazení. Začátek je zahrnutý, konec už ne. Před začátkem se ukáže Později, po konci Vyloučené podmínkou a časové zvýraznění zmizí. Potvrzená aktivní časová volba může dostat označení Časově omezené; neověřená reference nikdy nedostane potvrzené naléhavé varování.

ViewModel plánuje přepočet na nejbližší změnu okna; po návratu aplikace do popředí se stav znovu posoudí. Nejde o oznámení na pozadí ani budík. Současný balíček žádné časové okno neobsahuje. Změny stavu světa nemají vlastní automatický model; aplikace z nich nevytváří nevratná upozornění bez konkrétní podporované podmínky a důkazu.

## Před odchodem a prozrazování

Kontrola spojuje dostupné důležité cíle/přání, potvrzené chybějící předchozí kroky a samostatné nejistoty, včetně dopravních podkladů. Hotové věci odpadnou. Odložené věci jsou samostatně. Každý výsledek připomíná omezené pokrytí: ani prázdný seznam nedokazuje dokončení celé oblasti.

Režim Jen názvy a místa skryje důvod, seznam podmínek i související cíle ve vysvětlení. Jednotlivé vysvětlení lze odkrýt, aniž se změní celkové nastavení. Stav dostupnosti a věrohodnost zůstávají viditelné. Volba řazení se ukládá do DataStore a je společná pro aplikaci. Přenosná záloha nadále přenáší osobní data, ne toto globální nastavení.

## Databáze a kompatibilita

Herní Room databáze přechází 1 → 2 automatickou migrací s novou tabulkou `guide_rules`, odkazem na položku a jedinečným indexem položky. Původní řádky a metadata se zachovají. Import pravidel i katalogu je stále jediná transakce; selhání zápisu vrátí celý původní katalog.

Osobní Room databáze zůstává přesně ve verzi 2 z E05, přenosná záloha zůstává ve formátu 1. E06 neprovádí výměnu ani smazání osobních dat. Starší DataStore dostane výchozí Vyváženě a zachová aktivní profil, mapu a prozrazování. Nový obsah se při spuštění načítá z místního katalogu; živé síťové aktualizace nejsou součástí E06.

Přesné výsledky a omezení měření: `E06_ZPRAVA.md`, telefonní kontrola: `E06_TELEFON.md`.
