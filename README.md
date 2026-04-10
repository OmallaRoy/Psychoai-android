# PsychoAI Android App

The PsychoAI Android app is the mobile frontend for the PsychoAI trading 
psychology coaching system. It allows traders to upload their trade journals, 
view AI-detected behavioral patterns, and receive coaching from Plutus — 
an AI coach — all within a conversational chat interface.

## What It Does

After signing in, a trader uploads a CSV or Excel trade journal. The app 
sends it to the PsychoAI backend for analysis and displays the detected 
psychological trading mistakes alongside coaching messages from Plutus. 
Past sessions are accessible through a hamburger drawer that pulls the 
trader's full history from Firestore. Traders also receive a daily 
motivational push notification at 08:00 UTC.

## Tech Stack

- Kotlin — primary language
- Jetpack Compose — UI
- Firebase Authentication — user sign-in and session management
- Firebase Firestore — session and profile storage
- Firebase Cloud Messaging (FCM) — push notifications
- Retrofit — HTTP client for backend communication

## Key Screens

- Login / Register — Firebase Auth-powered sign-in
- Chat Screen — conversational interface with Plutus showing coaching 
  messages and pattern results
- Session Drawer — hamburger menu listing all past analysis sessions 
  pulled from the trader's Firestore profile

## Data Models

- PatternEntry — holds date, pattern name, confidence score, and 
  coaching snippet for each detected trading mistake
- TraderProfile — holds trader ID, total session count, and a list 
  of PatternEntry records

## Backend

This app communicates with the PsychoAI FastAPI backend deployed at:
https://psychoai-backend-production.up.railway.app

## Setup

1. Clone the repo and open in Android Studio
2. Add your google-services.json file to the app/ directory
3. Sync Gradle and build the project
4. Run on an emulator or physical Android device
