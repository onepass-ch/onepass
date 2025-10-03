# OnePass

## Pitch

Buying and reselling tickets in Lausanne can be chaotic: every organizer uses a different system—sometimes paper, sometimes third-party vendors—making it hard for buyers to trust tickets and for organizers to stay in control.  
**OnePass** solves this by bringing everything into a single, trusted platform:

- **Organizers** can quickly create an event and publish it on the live event map.  
- **Attendees** can discover nearby events, buy or resell tickets safely, and store them digitally.  
- **Validation** is handled through QR codes that organizers scan at the entrance.  

With just a few steps—**create event → show on map → buy/sell ticket → scan QR at the door**—OnePass delivers a simple, secure ticketing experience.

---

## Core Features (MVP)

- **Event Creation**: Organizers create events directly in the app.  
- **Live Event Map**: Every event appears as a pin on the map, helping users discover what’s nearby.  
- **Ticketing**: Attendees can buy or resell tickets directly through the app.  
- **QR Codes**: Each ticket has a unique, signed QR code for validation at the entrance.  
- **Offline Mode**: Tickets are cached locally and can be scanned even without connectivity.  

*(Future extensions: NFC support, peer-to-peer ticket transfers, resale price charts.)*

---

## Tech Stack

- **Frontend**: Android app built with **Kotlin** and **Jetpack Compose**.  
- **Backend**: Firebase  
  - Authentication (Google Sign-In, account management)  
  - Cloud Firestore (events, tickets, resale listings)  
  - Cloud Functions (QR code generation, signing, validation)  
  - Firebase Storage (event images, media)  

This split-app model ensures minimal server management and smooth offline functionality.

---

## Multi-User Support

- **Authentication**: Users log in with Google Sign-In.  
- **Profiles**: Firestore links each user with their tickets, purchases, and listings.  
- **Permissions**: Security rules ensure that:
  - Only ticket owners can transfer or redeem tickets.  
  - Only organizers can manage their own events.  

---

## Sensors Used

- **GPS**: Powers the *Nearby Events* map, suggesting events within a radius (5–15 km).  
- **Camera**: Scans QR codes for ticket validation.  

*(Core sensor for MVP: **Map/GPS**; Camera is secondary for validation.)*

## Links
-**Figma** : /*figma link */
---

