# Jarvis — asistent personal Android (MVP)

## Ce face acum
- Chat text + voce (STT/TTS în română)
- Research real pe web prin Claude API (tool de web search inclus)
- Poate deschide aplicații, apăsa Back/Home, prin AccessibilityService
- Cheia API se salvează local, în SharedPreferences (nu e trimisă nicăieri altundeva)

## Cum îl pui în funcțiune

1. **Instalează Android Studio** (gratuit): https://developer.android.com/studio
2. **Open Project** → alege folderul `JarvisApp`
3. Lasă Android Studio să descarce Gradle/dependențele (prima sincronizare durează câteva minute)
4. Conectează telefonul prin USB cu **Developer Options** + **USB Debugging** activate
   (Setări → Despre telefon → apasă de 7 ori pe "Build number" → apoi Developer Options)
5. Apasă **Run ▶** — aplicația se instalează pe telefon

## Prima configurare pe telefon
1. La prima pornire, introdu **cheia API Anthropic** (o generezi gratuit pe https://console.anthropic.com → Settings → API Keys)
2. Apasă butonul **"Accesibilitate"** din aplicație → activează manual "Jarvis" din lista de servicii de accesibilitate Android
   (Android cere asta manual, din motive de siguranță — nicio aplicație nu poate activa automat controlul ecranului)
3. La prima comandă vocală, acordă permisiunea de microfon

## Cum ceri o acțiune
Scrie sau spune, de exemplu:
- "Caută pe internet cine a câștigat ultimul Champions League"
- "Deschide-mi WhatsApp"
- "Du-mă înapoi"

Modelul decide singur când trebuie research (folosește tool-ul web search automat) și când trebuie o acțiune pe telefon
(adaugă intern un cod `[ACTION:...]` pe care aplicația îl execută și îl șterge din ce citește cu voce tare).

## Limitări reale, ca să nu ai surprize
- **Deschide aplicații după package name** — momentan nu poate "aduce niște poze din galerie" sau "apasă butonul X din Instagram";
  poate deschide aplicații, naviga back/home, și citi textul de pe ecran. Interacțiuni fine (click pe un buton anume dintr-o
  aplicație terță) se pot adăuga, dar cer cod suplimentar per-aplicație.
- **Cheia API costă bani** — folosești API-ul Anthropic cu cheia ta, plătești per cerere (foarte ieftin pentru uz personal,
  dar nu e gratuit nelimitat).
- **Accesibilitatea trebuie activată manual** — restricție de siguranță Android, nu se poate evita.
- Nu există momentan "wake word" (ex: "Hey Jarvis") — apeși butonul de microfon. Se poate adăuga cu o librărie
  de detecție offline (ex: Porcupine), dar e un pas separat.

## Ce poți adăuga în continuare
- Wake word / ascultare continuă în fundal (foreground service + Porcupine sau Vosk)
- Acțiuni mai fine: click pe elemente specifice din alte aplicații (folosind `getScreenText()` + coordonate din
  `AccessibilityNodeInfo` pentru a găsi și apăsa un buton anume)
- Widget pe ecranul principal / Quick Settings tile pentru acces rapid
- Istoric persistent al conversației (momentan se pierde la restart)
