# E09 — výsledek a předání

24. 10. 2026 · 0.9.0-e09 / kód 9 · cz.jenda.wowforever.guide.

## Výsledek

Správce v Postava → Balíčky a mapy umí ručně přijmout a ověřit podepsaný soubor nebo po zjištění velikosti stáhnout přímý HTTPS odkaz. Výchozí stahování vyžaduje Wi-Fi. Přípravu lze zrušit; chyba nahradí jen dočasný soubor. Náhled ukáže generaci, mapy a dopad na osobní historii. Potvrzené přepnutí je atomické a ponechává předchozí generaci. Návrat a smazání stažených generací zachovávají osobní postup.

Mapa po přepnutí čte dlaždice z odpovídající nové generace. Při novém spuštění se kontroluje celistvost a vrací předchozí nebo přibalený celek. Jde o úplný kompatibilní balíček čtyř současných map; jednotlivé části uvnitř aktivní generace se samostatně nemažou. Nové souřadnicové soustavy a mapy aplikace zatím odmítne. Podrobný formát, omezení a obnova jsou v E09_DATA.md.

## Ověření

- Čisté sestavení aplikace i instrumentačního APK prošlo. Finální ověření po zpřesnění kontroly načteného terénu také prošlo.
- **144 automatických hostitelských testů uspělo, 0 selhalo, 0 vynecháno**, včetně **15 nových testů E09**. Hostitelská nativní grafika a Compose nejsou fyzický telefon.
- Android Lint: **0 chyb, 6 varování**. Čtyři upozornění hlásí dostupné novější knihovny. Dvě hlášení UsableSpace se týkají téhož místa; správce záměrně počítá se skutečně volným prostorem a rezervou, nespoléhá na možné vyčištění cizích mezipamětí. Úplná hlášení jsou v qa/e09. Závislosti se neměnily.
- Podepsaná generace se připraví odděleně, otevře se skutečná disková Room databáze, přežije zavření a nové otevření, vrací se na předchozí generaci a lze ji smazat bez změny osobních záznamů.
- Negativní kontroly: cizí klíč a poškozený podpis, chybný součet nebo velikost, chybějící dlaždice, cizí cesta, přebývající soubor, neplatné PNG, neplatný katalog, novější aplikace/schéma, jiná hra, závislost, jiný mapový rámec a změněná stejná generace.
- Přerušení vstupního proudu a zrušení coroutine zavřou proudy a ponechají aktivní generaci. Nedostatek místa se ověřuje před přenosem i rozbalením. Simulované přerušení po vytvoření databáze, po přesunu složky i před zápisem ukazatele po novém spuštění zachová původní obsah.
- Poškozené aktivní dlaždice i odvozená databáze vyvolají návrat na předchozí funkční obsah, potom případně na přibalený celek. Úklid udržuje potřebné dvě generace a odstraňuje opuštěnou přípravu.
- Řízený HTTPS adaptér ověřuje HEAD a velikost, Wi-Fi, nepovolené URL, zákaz přesměrování, chybu serveru, neúplný přenos, změněnou délku a úspěšné opakování. Následuje skutečné kryptografické ověření a příprava dat; živý veřejný server se netestoval.
- Nativní UI ověřuje náhled a potvrzení, obnovení aktivity, mapové značky **i skutečně vykreslený terén z nové generace**, návrat, chybný podpis, zachování dokončeného úkolu a výpravy, historii mizející položky i smazání stažených generací.
- Samostatný kontrolní balíček má 69 datových souborů. Veřejný klíč z APK ověřil jeho skutečný distribuční podpis nezávisle v Java SHA256withRSA. Python zkontroloval podpis, součty, PNG a katalog a odmítl osm vadných variant. Nástroje pro sestavení i samostatnou kontrolu balíčku jsou součástí zdrojů.
- Všechna osobní i herní schémata a bajty přibaleného katalogu z E08 zůstávají stejné. Všech 68 dlaždic a rekonstrukce čtyř map prošly kontrolou. Soukromé klíče ani cvičné herní záznamy nejsou v APK nebo zdrojovém archivu.

Snímky preview.png, active.png a updated-map.png zachycují oddělenou cvičnou změnu názvu NPC v hostitelském testu. Takto přejmenovaný záznam **není v APK ani v kontrolním distribučním balíčku**. Výpis dekodéru v testovacím logu pochází z úmyslně poškozeného PNG v negativním testu.

## Podpis a instalace

Původní dočasný klíč E01–E08 byl ztracen při údržbě pracovního prostředí a není ve zdrojovém archivu. E09 má nový vlastnický podpis, jehož certifikát je ověřen proti vlastnickému keystore, a nový stálý identifikátor. **Instaluje se vedle staré aplikace. Starou aplikaci před přenosem postupu neodinstalovávat.** Profil se přenese již existujícím exportem a importem zálohy. Soukromý obnovovací archiv je oddělený od zdrojů a má být zachovaný pro budoucí aktualizace.

## Obsah a otevřené body

APK obsahuje generaci 2: 53 položek, 27 beta poloh a 0 ověřených dopravních hran. Kontrolní balíček generace 3 mění pouze číslo generace; herní obsah ani mapy se nemění. Umožňuje fyzicky vyzkoušet import a návrat bez vymyšlených herních údajů.

M1 zůstává otevřený. Jenda výslovně požádal pokračovat E09 bez potvrzení E08; poslední obecné potvrzení telefonu je E05. Neproběhla fyzická přejímka E09, skutečné ukončení Android procesu, živý výpadek HTTPS, druhé fyzické zařízení ani nezávislá kontrola herních návazností. Instrumentační APK byla sestavená, na zařízení zde spuštěná nebyla. Výkonové cíle E11 nejsou tímto měřením splněné. Automatické aktualizace a veřejný hosting nebyly nasazeny.

## Další krok

Podle E09_TELEFON.md přenést profil do souběžné E09, použít kontrolní generaci 3, zkusit návrat na 2 a opětovné spuštění bez internetu. Dokončit otevřené body M1. Potom **E10 — rozšíření doloženého herního obsahu do navazujících oblastí**.
