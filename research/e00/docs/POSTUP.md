# Postup projektu WoW Forever průvodce

Stav k 23. říjnu 2026. Navazuje na Implementační plán verze 1.0 ze stejného dne.

## Dokončeno

E00 Ověření zdrojů a rozsahu prvního balíčku. Jsou připravené čtyři mapové podklady s uvedenou beta verzí, inventář, původ souřadnic, výzkumný vzorek katalogu, osm kontrolních poloh a seznam nejasností. Strukturální kontrola obsahu a otisků souborů prošla. Není vytvořený Android projekt ani APK.

## Výchozí nastavení

Nativní Android, Kotlin, Jetpack Compose. Horda, Warlock, úroveň 1, rasa předběžně ork. Profese a režim nevybrané. Čeština, původní herní názvy, offline podklady, ruční postup a záloha. Žádný Capacitor ani mapa ve WebView.

## Přesný další krok

E01 Základ projektu a ověření nativní mapy. Založit projekt a repozitář, uzamknout verze nástrojů, ověřit MapCompose přes oddělený adaptér a zobrazit Durotar z maps/reference. Připojit osm doložených beta poloh s viditelným stavem ověření. Ověřit přiblížení, otočení a správnost převodu souřadnic. Připravit sestavitelné technické APK. Durotar používá uiMapId 1411; areaId 14 patří jiné soustavě identifikátorů.

## Co nepředpokládat

Body nebyly kalibrované ve vytvořené Android aplikaci, protože ta ještě neexistuje. Mapy nejsou potvrzené proti vydání hry. V katalogu není žádný úkol označený jako ověřený ve vydané hře. Převzatá pravidla z CLASSIC_QUEST_AVAILABLE jsou reference, nikoliv potvrzená Forever pravidla. Questy 792, 1485 a 1499 mají shodný název a odlišná ID. U úkolu 1485 nerozhodovat počet hlav mezi hodnotami 6 a 8 bez dalšího důkazu.

## Rozsah první použitelné verze

Přehled světa a úvodní orčí oblast, profil, mapa, vyhledávání, detail úkolu, postup, doporučení a export. Tento předávací vzorek není kompletní databáze oblasti. Další obsah se doplní v E07 a E10 podle ověření konkrétních záznamů.
