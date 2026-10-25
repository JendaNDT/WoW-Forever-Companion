# Aktuální stav projektu

Oprava E10: aplikace **0.15.1-e10-fix1 / kód 16**, nezměněná data 7. Jenda nahlásil zavření původní celkové E10 při přípravě obsahu. E10.3 a starší E12/E13 měly dříve základní uživatelské testy potvrzené; tato oprava čeká na nové otevření na telefonu.

Reprodukovaný problém: původní RecommendationEngine při limitu 256 MiB skončil OutOfMemoryError. Sdílené zdroje předmětů vytvářely miliony odkazů NPC mezi sebou. Oprava uchovává skutečné přímé vazby. Nově je postupné dekódování katalogu, opětovné použití dekódovaného obsahu, dávkové zápisy do databáze, úsporné čtení záznamů a zdrojových odkazů. Stav balíčků už nečte celý katalog. Velký WFP je uložený bez druhé komprese a čte se přes souborový deskriptor; obnovení používá existující ověřenou generaci.

Po opravě prošel původně padající výpočet i bezpečnostní testy se 256 MiB. S 384 MiB prošlo první spuštění celé aplikace i přechod z dat 6, návrat a obnovení dat 7. Jde o hostitelské testy, nikoli měření paměti Jendova telefonu. Úplná regrese a sestavení jsou zaznamenané v E10_FIX_ZPRAVA.md a qa/e10fix.

Obsah E10 zůstává referenčním průvodcem Horda / Warlock 1–60: 12 352 položek, 18 441 poloh a 61 map. Zdrojové nejistoty, nula RELEASE údajů a neuzavřené herní M2/M3 se nemění. Data 7 mají stejný podpis a SHA-256 jako před opravou. Klíče zůstávají původní z E09.

Další nejvhodnější krok: instalovat jediné opravené APK přes původní verzi bez odinstalování a ověřit přípravu, zachování postupu a opětovné otevření. Poté pokračovat ve společném testu obsahu E10; nejsou potřeba nové malé obsahové dávky.
