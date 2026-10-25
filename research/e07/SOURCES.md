# Audit zdrojů E07

23. 10. 2026. Nové záznamy jsou kurátorované z konkrétních veřejných stránek NPC, questů a předmětů na warcraftforever.games. Přesné URL, datum a faktické výpisy jsou v sources[].payload v content/e07-additions.json. Stránky označují záznamy ForeverLog jako betu; jde o neoficiální fanouškovský web. Při kontrole pomocí webového čtení nebyl přístup ke hernímu klientu. Celé cizí stránky nejsou přibalené.

Souřadnice 19 nových NPC byly porovnané s mapovými značkami stejného vydavatele z E00. Odkaz na původní mapu: https://warcraftforever.games/maps/durotar . Otisk původního HTML je v research/e00/evidence/retrieval_manifest.json; nejde o dva nezávislé zdroje. Faktuální převod zachovává uiMapId 1411, povrch 0 a přesnost publikované desetiny procenta.

Nové Classic předchůdce dokládají konkrétní záznamy [789]={788}, [804]={790}, [6394]={5441} v připnutém EverythingQuests zdroji eq_available z E00. Ověření ve Forever z toho neplyne. Minimální úroveň 1485 byla znovu kontrolovaná přes Wowhead Forever, https://www.wowhead.com/forever/quest=1485/vile-familiars ; rozpor původních podkladů zůstává zachovaný.

Jendův zdroj ForeverChanges byl prověřen přes /about, /class/warlock, /spellbook/warlock a /quests. První dvě stránky při čtení uváděly build 1.60.1.70450, kniha kouzel 1.60.1.70532. Zdroj se proto verzuje po stránkách. Katalog obsahuje konkrétní zdroj knihy kouzel pro Summon Imp a internetový odkaz na změny povolání. Jeho Classic questy se hromadně nepřebírají jako potvrzené Forever.

Všechny přírůstky mají beta/referenční důvěryhodnost, nikoli in_game_release_observation. Kompletní popis a mezery: docs/E07_DATA.md; strojový přehled: coverage.json. Dokumentace neprohlašuje aktuální data za stabilní vydané herní podmínky.
