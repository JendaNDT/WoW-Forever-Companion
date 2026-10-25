# E03 — výsledek a předání

Datum: 23. 10. 2026. Verze **0.3.0-e03**, kód **3**.

## Výsledek

Implementovaná navigace Mapa / Teď pro mě / Moje cesta / Postava, úprava předvyplněné postavy a úrovně 1–60, ruční volba oblasti, volitelné profese a režim, nastavení prozrazování se skutečným skrytím popisů, obnovení poslední obrazovky a mapových výřezů. Moje cesta zobrazuje osobní záložky. Teď pro mě ukazuje profil a rozsah dat; nevytváří nepotvrzená doporučení.

Telefon má spodní navigaci, široká a nízká okna na šířku postranní lištu. Malá obrazovka na výšku používá krátký detail bodu; mapa s otevřeným detailem má v hostitelské kontrole minimálně 100 dp výšky. Tlačítka detailu se v úzkém panelu zalamují do dalšího řádku. Formulář i detail lze posouvat.

Profil se aktualizuje v existující tabulce bez náhrady řádku; podřízená osobní data zůstávají. Obě Room schémata a jejich identity jsou shodné s E02, verze 1. Drobný stav používá samostatný typovaný DataStore. Podrobnosti: `E03_DATA.md`.

## Ověření

- Čisté sestavení aplikačního APK i instrumentačního testovacího APK prošlo.
- **43 hostitelských testů uspělo, 0 selhalo.** Původních 37 testů zůstalo zachovaných a přibylo šest testů profilu, navigace a ukládání.
- Kontrola neplatné úrovně, zachování rozepsaného profilu při přepnutí a znovuvytvoření aktivity, uložení nové rasy / oblasti, návrat systémovým Zpět, skutečné skrytí a odkrytí popisu a otevření záložky z Moje cesta.
- Široké okno 800 × 400 dp a nízké telefonní okno 560 × 360 dp mají přístupnou navigaci, mapu a její ovládání. Převody souřadnic a zátěž 500 značek nadále procházejí.
- Nový ViewModel bez starého SavedStateHandle načetl uloženou navigaci a mapu. Samostatný test ukončil a znovu vytvořil DataStore nad stejným souborem. Další test zavřel a znovu otevřel osobní databázi po úpravě profilu a ověřil zachování záložky, postupu i poznámky.
- Android Lint: **0 chyb, 1 varování**. Případné varování GradleDependency upozorňuje na novější testovací AndroidX JUnit; ověřená sada nástrojů zůstává uzamčená. Nejde o chybu běhu aplikace.
- Podpis APK ověřen a shodný s E02; ID `cz.jenda.wowforever`, minimum API 26. APK nemá oprávnění k internetu ani k poloze.

Protokoly, JUnit XML, podpis, metadata a sedm skutečně vykreslených snímků: `qa/e03`. Snímky pocházejí z nativního grafického režimu Robolectric, nikoliv z telefonu či návrhového nástroje.

## Hranice ověření

Uživatel nyní obecně potvrdil fungování E02 na telefonu. E03 na telefonu zatím spuštěná nebyla. Skutečné zabití procesu systémem, systémové gesto Zpět, IME a navigační lišty konkrétního telefonu čekají na ruční kontrolu podle `E03_TELEFON.md`. Test nového modelu a opětovného otevření souboru není vydáván za skutečné ukončení Android procesu.

Herní katalog se nemění: 21 položek, osm beta poloh NPC v Durotaru, čtyři mapy. Žádná nezávislá kontrola uvnitř vydané hry neproběhla. Volba jiné rasy nepřidává neexistující pokrytí jiného startu.

## Předání a další krok

`WoW_Forever_E03.apk` lze nainstalovat přes E02 bez odinstalování. Zdrojový archiv obsahuje projekt, dokumentaci, kontroly a `history.bundle`, nikoli soukromý podpisový klíč nebo místní SDK. Nejprve ověř uložení postavy a záložek po zavření na telefonu; poté **E04 — mapa propojená s katalogem, vyhledávání a vrstvy**.
