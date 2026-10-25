# E03 — profil, navigace a obnova

## Osobní profil

Existující tabulka `profiles` už od E02 obsahuje jméno, rasu, úroveň, aktuální mapu, volitelný režim a profese. E03 přidává pouze DAO `@Update`, nikoli nové sloupce. Schéma osobní i obsahové databáze zůstává ve verzi 1, včetně původní Room identity. Migrace není potřebná. Aktualizace neprovádí INSERT REPLACE ani smazání profilu, takže zachová podřízené záložky, postup a poznámky.

Ukládání profilu je výslovné tlačítkem. Úroveň je celé číslo 1–60, jméno neprázdné (nejvýše 60 znaků). Frakce Horda a povolání Warlock jsou pro tuto osobní aplikaci pevné. Rasy: Ork, Troll, Nemrtvý. Volby odpovídají oficiální tabulce Forever: [Blizzard, Create the Hero You Want to Be](https://news.blizzard.com/en-us/article/24304075/create-the-hero-you-want-to-be-in-world-of-warcraft-forever), kontrola 2026-10-23. Změna rasy nevytváří domnělé body v jiných startovních oblastech.

Režim je nepovinný vlastní text; profese jsou nepovinné názvy oddělené čárkou, uložené jako JSON pole. Nejde o seznam potvrzených herních pravidel. Tyto údaje zatím neovlivňují dostupnost úkolů, protože doporučení nejsou součástí E03. Prázdné volby používání neblokují.

## Drobné nastavení a mapa

Jedna instance typovaného DataStore na soubor `files/datastore/guide-session.json`. Závislost 1.1.7 je uzamčená a ověřená s Kotlin 2.1.21; zbytek nástrojů se neaktualizuje. Zdroje: [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) a [poznámky vydání 1.1.7](https://developer.android.com/jetpack/androidx/releases/datastore#1.1.7).

Neměnný objekt Session uchovává poslední prohlíženou mapu, vybraný bod, historii hlavních obrazovek, míru prozrazování a výřezy map. Aktuální oblast postavy je samostatná vlastnost profilu: prohlížení Azerothu ji samo nepřepíše. Uložení změněné oblasti postavy současně otevře příslušný podklad.

Zápisy nastavení zpracovává jedna aplikační coroutine, která serializuje zápisy a slučuje čekající snímky. Otočení či zánik ViewModelu přijatý zápis nezruší. Chyba nastavení se zobrazí uživateli; poškozená data se automaticky nemažou. Pohyb mapy se ukládá po 400 ms klidu a při opuštění obrazovky / onStop. Neznámé navigační hodnoty se filtrují, neplatný výřez se nepoužije.

SavedStateHandle navíc uchovává mapu, bod, historii a výřezy pro obnovu systémem. Compose SaveableStateHolder + rememberSaveable zachovávají rozepsaný profil, posun formuláře a otevřený katalog v uloženém stavu aktivity. Rozepsaný profil není potvrzeným profilem: při úplně novém spuštění bez systémového uloženého stavu se načte poslední uložená postava.

## Ovládání

Čtyři hlavní části: Mapa, Teď pro mě, Moje cesta, Postava. Telefon na výšku používá spodní navigaci; široké okno a nízké okno na šířku od 480 dp používají postranní navigaci. Detail mapového bodu je na šířku vedle mapy. Systémové okraje a klávesnice se respektují přes safeDrawingPadding a imePadding. Formulář je posuvný.

Zpět nejprve zavře dialog / detail a potom vrací předchozí hlavní obrazovku. Opakované přepínání záložek nevytváří neomezený zásobník. Mapa je kořen. Přepnutí obrazovky nemění ani znovu nevytváří mapový engine.

Volba „Jen názvy a místa“ skrývá české popisy katalogu. U konkrétní položky je možné jednorázové odkrytí. Názvy, kategorie, zdroje, nejistoty a polohy zůstávají viditelné. „Podrobné popisy“ odpovídá dosavadnímu chování E02.

„Moje cesta“ zobrazuje skutečné záložky. „Teď pro mě“ zobrazuje uloženou postavu a rozsah dostupných dat; nezaměňuje beta katalog za ověřená doporučení. Vrstvy a hledání mapy patří do E04, herní postup a export do E05, pravidla doporučení do E06.
