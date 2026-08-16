# Jak zbudować APK tylko z telefonu

Ta wersja projektu ma gotowy GitHub Actions workflow: **Zbuduj APK**.
Nie potrzebujesz Android Studio ani komputera.

## Pierwsze uruchomienie

1. Na telefonie rozpakuj ZIP projektu.
2. Wejdź na GitHub i utwórz nowe prywatne repozytorium, np. `KontrolaDostaw`.
3. Wgraj do repozytorium **zawartość** folderu `KontrolaDostaw` (nie sam ZIP).
4. Po wgraniu GitHub powinien automatycznie uruchomić workflow **Zbuduj APK**.
5. Wejdź w zakładkę **Actions**.
6. Otwórz najnowsze uruchomienie **Zbuduj APK**.
7. Poczekaj aż `Testy i APK` będzie zielone.
8. Na dole ekranu, w sekcji **Artifacts**, wybierz **KontrolaDostaw-APK**.
9. Pobierz plik. GitHub pobierze archiwum zawierające `KontrolaDostaw.apk`.
10. Rozpakuj je na telefonie i uruchom `KontrolaDostaw.apk`.

Android może poprosić o zgodę na instalację aplikacji z przeglądarki lub aplikacji Pliki. Zezwól tylko dla źródła, z którego właśnie instalujesz własny APK.

## Kolejne wersje

Po każdej zmianie plików w gałęzi `main` lub `master` nowy APK buduje się automatycznie.
Możesz też uruchomić kompilację ręcznie:

**GitHub → repozytorium → Actions → Zbuduj APK → Run workflow**.

## Co robi workflow

- używa JDK 17,
- używa Gradle 9.5.0,
- zapewnia Android SDK 37 i Build Tools 36.0.0,
- uruchamia testy jednostkowe,
- buduje debug APK,
- udostępnia gotowy `KontrolaDostaw.apk` jako artefakt GitHub Actions.

## Ważne o wersji 1.0.1

To jest APK typu **debug**, przeznaczony do prywatnego używania i testowania na telefonie taty. Nie jest to jeszcze paczka przygotowana do publikacji w Google Play.

Dane zamówień są przechowywane lokalnie w aplikacji. Backup systemowy Androida został wyłączony, żeby nie kopiować danych biznesowych do chmury bez świadomej decyzji użytkownika.
