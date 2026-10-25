# WeightTracker

WeightTracker is a daily body weight logging and milestone tracking Android application developed for **CS-360 (Mobile Architecture and Programming)**.

## Overview
WeightTracker allows users to track their body weight over time, set target goals, and receive automated milestone notifications. All user data remains private and is stored locally on the device using an SQLite database.

## Key Features
- **Local User Authentication**: Private login and user registration using an on-device SQLite database.
- **Weight Logging & History**: Log, view, edit, and delete daily weight entries with support for both pounds (lbs) and kilograms (kg).
- **Target Goals & SMS Alerts**: Set target goal weights and optionally receive automated congratulatory SMS notifications when milestones are achieved.
- **Ad Integration**: Integrated with non-intrusive AdMob banner ads on the main weight history screen.
- **Dark Slate UI**: Custom edge-to-edge dark theme with dual-tone Teal-to-Sage accents and brand scale logo styling.

## Technical Specifications
- **Course**: CS-360 Mobile Architecture and Programming
- **Language**: Java / Android SDK
- **Minimum SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 37 (Android 15/16)
- **Database**: SQLite (`DatabaseHelper.java`)
- **Monetization**: Google Mobile Ads SDK (AdMob)

## Project Structure
```
WeightTracker/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/net/azterisk/weighttracker/
│   │   │   │   ├── MainActivity.java
│   │   │   │   ├── WeightListActivity.java
│   │   │   │   ├── AddWeightActivity.java
│   │   │   │   ├── SmsActivity.java
│   │   │   │   ├── DatabaseHelper.java
│   │   │   │   └── GlowUtils.java
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   └── build.gradle
└── README.md
```

## Coursework Note
This repository was created for academic coursework in CS-360.
