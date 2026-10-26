# E11 — zbývající ověření na skutečném telefonu

Aktuálně 0.16.0-audit (17), data 8. Uživatel dne 26. 10. 2026 potvrdil „Ok, funguje to“ po předání APK pro čistou instalaci. Základní funkční potvrzení je zapsané; kvůli doplnění automatických testů není nutné aplikaci znovu instalovat ani ručně importovat obsah.

Aktuální automatické scénáře a jejich meze popisuje [E11_AKTUALNI_OVERENI.md](E11_AKTUALNI_OVERENI.md). Starší zprávy E11 zachycují tehdejší menší katalog a původní způsob aktualizace.

## Funkční kontrola telefonu

Při nejbližším běžném používání lze společně ověřit: zapsat stav úkolu a poznámku, uložit zálohu přes Postava, v systémovém nastavení vynutit ukončení průvodce a znovu jej otevřít. Postava, úkol, poznámka, plán a vybraná oblast mají zůstat zachované. Samotné odstranění z přehledu posledních aplikací nemusí proces ukončit.

Obnovu zálohy zkoušet přednostně jako nové profily; původní zůstanou zachované. Obnova na druhém skutečném zařízení zůstává samostatnou kontrolou. Záloha obsahuje osobní záznamy, nikoli mapové podklady; současné APK má celý katalog a mapy přibalené.

## Čitelnost a systémové ovládání

Zvětšit systémové písmo, vyzkoušet hledání a uložení postavy na výšku i na šířku a projít ovládání pomocí TalkBack. Potom písmo vrátit podle vlastní preference. Hostitelské testy rozložení ani běh v emulátoru nenahrazují TalkBack na konkrétním telefonu.

## Výkon a delší používání

Při dvacetiminutovém používání střídat oblasti, posouvat a přibližovat mapu. Pád nebo postupné zpomalování uvést s oblastí a provedenou akcí. Pocitová kontrola sama nepotvrdí FPS, přesnou odezvu nebo růst paměti.

Pro skutečné měření jsou připravené nástroje v [E11_DATA.md](E11_DATA.md). Vyžadují výslovně vybraný telefon připojený přes adb a sestavení určené k měření. Nynější vývojové APK není podkladem pro potvrzení výkonových cílů vydané varianty. Dokud není záznam ze Samsungu, zůstávají 3sekundový start, 300ms celková odezva hledání, 60 FPS a dlouhodobý vývoj PSS nepotvrzené.

Správnost herních údajů se ověřuje samostatně při hraní; technické testy ji nepotvrzují.
