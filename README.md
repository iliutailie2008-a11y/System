# System — proiect Android

Aplicație personală "System": asistent zilnic cu sferă de particule animată,
tracker Solo Leveling (Strength/Vitality/Agility/Intelligence/Perception),
chat, voce, notificări.

Acesta e scheletul funcțional al aplicației (UI complet, design final),
gata de compilat prin GitHub Actions — nu ai nevoie de Android Studio.

## Pași în Termux (o singură dată)

1. Instalează Termux (F-Droid recomandat, nu Play Store) și deschide-l.
2. Instalează git:
   ```
   pkg update && pkg upgrade -y
   pkg install git -y
   ```
3. Configurează identitatea Git (o singură dată):
   ```
   git config --global user.name "Numele tău"
   git config --global user.email "emailul_tau@exemplu.com"
   ```
4. Creează un repository nou și **gol** pe github.com din browser, ex. `System`
   (fără README, fără .gitignore — complet gol).
5. Dezarhivează acest proiect pe telefon (ex. în Downloads), apoi în Termux:
   ```
   termux-setup-storage
   cd ~/storage/downloads/System
   git init
   git add .
   git commit -m "Prima versiune System"
   git branch -M main
   git remote add origin https://github.com/NUMELE_TAU/System.git
   git push -u origin main
   ```
   La push, GitHub îți va cere autentificare — folosește un
   **Personal Access Token** (Settings → Developer settings → Personal
   access tokens pe github.com) în loc de parolă.

## Conectarea la Claude API (creierul lui System)

System are nevoie de o cheie API Anthropic ca să poată "gândi" și
răspunde. Cheia NU se scrie niciodată în cod sau în fișiere din repo —
se pune ca **secret** pe GitHub, și GitHub Actions o injectează automat
la compilare, în siguranță.

1. Obții o cheie API de pe [console.anthropic.com](https://console.anthropic.com)
   (secțiunea API Keys).
2. Pe repo-ul tău de pe github.com: **Settings** → **Secrets and variables**
   → **Actions** → **New repository secret**.
3. Name: `CLAUDE_API_KEY`, Value: cheia ta (începe cu `sk-ant-...`).
4. Salvezi. La următorul push, Actions o va folosi automat.

Fără acest secret configurat, System va porni și UI-ul va funcționa, dar
chat-ul va răspunde cu un mesaj de eroare (cheie lipsă).

## Ce se întâmplă după push

GitHub Actions pornește automat, compilează APK-ul în cloud (2-4 minute),
și îl pune disponibil la:
`github.com/NUMELE_TAU/System` → tab **Actions** → ultimul workflow run →
secțiunea **Artifacts** → `system-debug-apk` → descarcă, dezarhivează,
instalează pe telefon.

## Ce urmează

Acest schelet are UI-ul complet (sferă animată, mod Solo Leveling mov,
chat, quests, status) și include deja:
- **Chat conectat real la Claude** — vorbești cu System, el răspunde real
- **Voce** — System îți vorbește răspunsurile cu voce de bărbat, în
  română sau engleză (detectat automat din text)
- **"Wake word" System** — apeși microfonul o dată, apoi spui "System,
  [comanda ta]" — telefonul ascultă, recunoaște cuvântul "System" și
  trimite restul ca mesaj către Claude. (Notă: e o soluție simplă,
  bazată pe recunoașterea de voce nativă Android, nu un motor dedicat
  de wake-word — ascultă doar cât ții modul activ, nu 24/7 în fundal.
  Merge din prima, fără cont extern sau fișiere de configurare.)
- **Memorie persistentă** — level, XP, atribute și task-uri se salvează
  local pe telefon și nu se resetează la fiecare pornire
- **Task-uri noi** — poți adăuga quest-uri direct din tab-ul Quests;
  System ghicește automat cărui atribut (Strength/Vitality/Agility/
  Intelligence/Perception) îi corespunde, pe baza cuvintelor din titlu

Ce mai lipsește pentru viziunea completă (le adăugăm ulterior, la cerere):
- citire notificări din alte aplicații (WhatsApp etc.)
- research web live (acum Claude răspunde din cunoștințele proprii,
  fără căutare pe internet în timp real)
- clasificare de atribute mai inteligentă (acum e pe cuvinte-cheie;
  poate trece prin Claude pentru precizie mai mare)

La fiecare modificare: `git add . && git commit -m "mesaj" && git push`,
iar Actions recompilează automat.
