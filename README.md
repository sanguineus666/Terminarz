# Kontrola Dostaw — Android 1.0.1

Prosta aplikacja dla małej firmy do pilnowania zamówień, terminów dostaw i pieniędzy zapłaconych za towar, który jeszcze nie dotarł.

## Co robi

- zapisuje zamówienia lokalnie na telefonie,
- pokazuje liczbę dostaw po terminie,
- pokazuje dostawy przypadające w ciągu 2 dni,
- sumuje kwotę zapłaconą za niedostarczone zamówienia,
- przypomina lokalnym powiadomieniem dzień przed dostawą i w dniu dostawy,
- pozwala jednym przyciskiem przygotować wydarzenie w kalendarzu telefonu,
- pozwala oznaczyć zamówienie jako dostarczone,
- pozwala edytować i usuwać zamówienia.

## Budowanie tylko z telefonu

Projekt zawiera gotowy workflow GitHub Actions: `.github/workflows/build-apk.yml`.
Po umieszczeniu projektu w repozytorium GitHub workflow uruchamia testy, buduje APK i publikuje je jako artefakt `KontrolaDostaw-APK`.

Pełna instrukcja: **BUILD_Z_TELEFONU.md**.

## Prywatność

Dane zamówień są przechowywane lokalnie w pamięci aplikacji. Aplikacja nie ma konta, reklam ani analityki i nie wysyła danych na własny serwer.
Systemowy backup Androida jest wyłączony (`allowBackup=false`), aby dane biznesowe nie były automatycznie kopiowane poza urządzenie.

Integracja z kalendarzem używa systemowego ekranu dodawania wydarzenia. Aplikacja nie prosi o stałe uprawnienie do odczytu całego kalendarza.

## Konfiguracja techniczna

- Android Gradle Plugin 9.3.0
- Gradle 9.5.0
- JDK 17
- compileSdk / targetSdk 37
- minSdk 26
- Jetpack Compose BOM 2026.08.00
- WorkManager 2.11.2

## Powiadomienia

WorkManager może przesunąć wykonanie przypomnienia w zależności od polityki oszczędzania baterii Androida. Aplikacja celowo nie żąda specjalnego uprawnienia do dokładnych alarmów.
