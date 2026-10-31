# 🎮 EDUSLOP - 2D Retro Educational Content Generator (Powered by Google ADK)

An educational micro-lesson platform tailored for teachers creating bite-sized, 5–15 minute interactive learning modules using the **Google Agent Development Kit (ADK)** and **Kotlin 21**.

Input any prompt (e.g. *"How does Domain Driven Design work"*), and **EDUSLOP** generates:
1. **Google ADK Agent Orchestration**:
   - Uses `com.google.adk.agents.LlmAgent` and `InMemoryRunner` from Google's official Agent Development Kit (`com.google.adk:google-adk`).
   - Generates structured 2D pixel animations, interactive cue cards, and multiple-choice quizzes with zero-downtime fallback.
2. **2D Pixel Art Animation (~1 minute)**:
   - Procedural 2D retro canvas rendering (pixelated avatars, bounded context boundaries, aggregate roots, database cylinders, event packets, animated connection arrows).
   - RPG-style dialogue box displaying synchronized narration text (typewriter effect, no audio required).
   - Play, pause, restart, scrub timeline, and 1.0x / 1.5x / 2.0x playback speed toggles.
3. **Interactive Cue Cards Stack**:
   - Scroll down to review a stacked deck of flashcards.
   - Click/tap to flip between the concept prompt and key takeaway.
   - Previous / Next navigation controls with card index counter.
4. **Stepped Multiple-Choice Quiz**:
   - Scroll further down for the knowledge check.
   - Designed for focus: questions are **stepped one at a time** (not endless scrolling).
   - "Next Question" transitions between questions.
   - The final question transforms into a **"Submit Test 🚀"** button.
   - Detailed retro evaluation report showing correct vs incorrect answers, explanations, and score percentage.
5. **Type-Safe In-Memory Backend (Kotlin + Ktor)**:
   - Built on Kotlin 21 and Ktor with Netty.
   - Fully in-memory concurrent repository for lessons and test submissions.

---

## 🚀 Quick Start

### Prerequisites
* **Java 21** installed (`java -version`).
* Gemini API Key (optional — leave blank in UI or set `GEMINI_API_KEY`).

### Run the Application

```powershell
.\gradlew.bat run
```

Once started, open your browser at:
👉 **[http://localhost:8080](http://localhost:8080)**

---

## 🕹️ Architecture & Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/` | Creator App (prompt input, presets, API key, terminal status) |
| `GET` | `/lesson/{id}` | Lesson Experience (Pixel animation, cue cards stack, stepped quiz) |
| `POST` | `/api/lessons/generate` | Generates a complete micro-lesson via Gemini API or fallback |
| `GET` | `/api/lessons` | Lists all in-memory lessons |
| `GET` | `/api/lessons/{id}` | Fetches a single lesson by ID |
| `POST` | `/api/lessons/{id}/quiz/submit` | Submits student answers, calculates score & records submission |
| `GET` | `/api/lessons/{id}/quiz/results` | Returns all submissions for a lesson |
| `GET` | `/api/health` | Server health probe |

---

## 🎨 Retro 2D Pixel Engine Spec

The animation is driven by a structured JSON timeline:
```json
{
  "totalDurationSeconds": 60,
  "scenes": [
    {
      "id": 1,
      "title": "Ubiquitous Language",
      "durationSeconds": 15,
      "narration": "In complex systems, developers and domain experts...",
      "background": "blueprint_blue",
      "entities": [
        { "id": "dev", "name": "Developer", "sprite": "character", "x": 18.0, "y": 52.0, "color": "#00f0ff" }
      ],
      "actions": [
        { "type": "connect", "target": "dev", "toX": 48.0, "toY": 28.0 }
      ]
    }
  ]
}
```
The HTML5 canvas renders these entities using custom retro pixel matrices (`image-rendering: pixelated`) at crisp virtual resolutions with retro CRT scanline overlays.
