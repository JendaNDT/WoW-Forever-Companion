# E04 — výsledek a předání

Datum: 23. 10. 2026. Verze **0.4.0-e04**, kód **4**.

## Výsledek

Nativní mapa je propojená s katalogem. Hledání podle původního názvu, českých aliasů a popisu otevře správnou mapu a doloženou polohu. Více poloh zůstává oddělených a lze je vybrat v detailu katalogu. Položka bez souřadnic má vysvětlení, nikoli vymyšlenou značku. Stejnojmenné úkoly zůstávají oddělené.

Vrstvy skutečně odstraňují nepovolené značky z rendereru a filtrují seznam Body. Lze zobrazit pouze záložky katalogu. Nastavení se ukládá, obnovení vrstev je přístupné v pevné liště dialogu. Vyhledáním konkrétního bodu se jeho vrstva zapne.

Dlouhým stiskem mapy vznikne vlastní poznámka v dané oblasti a souřadnicích. Má zelenou značku, je dostupná i v Moje cesta, lze ji upravit a po potvrzení smazat. Rozepsaný text se obnovuje při znovuvytvoření aktivity. Potvrzené poznámky jsou v osobní databázi.

Mapový stav rozlišuje mapu a patro; současné podklady ale obsahují pouze Povrch. Seskupování a vykreslování respektují výřez a kategorie, dlaždice zůstávají nativní. Podrobnosti a hranice dat: `E04_DATA.md`.

## Ověření

- Čisté sestavení aplikace i testovací APK prošlo.
- **51 hostitelských testů uspělo, 0 selhalo**, z toho osm nových pro E04.
- Nové testy: hledání českých aliasů bez diakritiky; rozlišení stejnojmenných úkolů; více poloh a oddělená patra s transformací; chybějící polohy a filtrování podle ID položky; poznámka po zavření a novém otevření databáze včetně ochrany jiného profilu; čtení původního nastavení E03; průchod hledání → mapa → katalog → mapa; reálné skrytí a obnovení vrstev a zobrazení uložené položky; nativní dlouhý stisk, rozepsaný text po otočení, uložení, úprava a potvrzené smazání poznámky. Některé scénáře jsou součástí jednoho společného testu.
- Dlouhý stisk středu výřezu má po inverzním převodu odchylku nejvýše 1 bod souřadnic. Původní nativní kontroly osmi kotev a zátěž 500 značek nadále procházejí.
- Nový ViewModel bez starého SavedStateHandle obnovil poznámku, vybranou mapu, bod a vypnutou vrstvu. Samostatná kontrola zavřela a znovu otevřela osobní databázi. Nejde o tvrzení, že byl ukončen skutečný Android proces.
- Zůstávají testy profilu E03, katalogu E02 a kompaktního i širokého rozložení.
- Android Lint: **0 chyb, 0 varování**. Případná upozornění na novější závislosti jsou v přiloženém protokolu; nástroje a MapCompose zůstávají uzamčené na kompatibilních verzích.
- Obě Room schémata včetně identity jsou přesně shodná s E03, verze 1. Stará data profilu a záložky se nepřevádějí ani nemažou.
- Podpis APK ověřen a shodný s E03; ID `cz.jenda.wowforever`, minimum API 26. APK nevyžaduje internet ani polohu telefonu.

Protokoly, JUnit XML, metadata podpisu a osm skutečných snímků rozhraní: `qa/e04`. Snímky jsou z nativního grafického režimu Robolectric. Testovací instrumentační APK bylo sestavené; na telefonu ani emulátoru zde spuštěné nebylo.

## Obsah a hranice ověření

Balíček zůstává na 21 položkách, osmi beta polohách NPC v Durotaru a čtyřech mapách povrchu. Data se nedoplňují pro splnění počtu kategorií. Podrobnost souřadnic z podkladu nepotvrzuje jejich shodu s vydanou hrou. Nové nezávislé ověření uvnitř hry neproběhlo.

Uživatel potvrdil fungování E01 a E02 na telefonu. Pro E03 před zadáním E04 nepřišlo výslovné potvrzení. Novou E04 je třeba ověřit na telefonu podle `E04_TELEFON.md`, zejména systémová gesta, klávesnici a obnovení po ukončení. Hostitelské kontroly nenahrazují měření výkonu telefonu nebo přesnost celé hry.

## Předání

`WoW_Forever_E04.apk` lze nainstalovat přes E03 bez odinstalování. Zdrojový archiv obsahuje projekt, dokumentaci, výsledky a Git historii; neobsahuje soukromý podpisový klíč, místní SDK ani sestavovací cache.

Doporučený další krok: ověř v telefonu hledání Ruzana, filtr vrstev a poznámku po opětovném spuštění. Poté **E05 — stavy úkolů, osobní postup a záloha profilu**.
