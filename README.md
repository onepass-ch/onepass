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
- [**Project Wiki**](https://github.com/onepass-ch/onepass/wiki)
- [**Figma**](https://www.figma.com/design/fwkbBN31kliusHJiytydyB/OnePass?node-id=0-1&p=f&t=JYBq32AwXtpzXLpy-0)
- [**Architecture diagram**](https://www.mermaidchart.com/app/projects/26c39244-5e0c-4374-b7fd-d7be5431add5/diagrams/6431dca0-2822-4d45-a785-70622488f07b/share/invite/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkb2N1bWVudElEIjoiNjQzMWRjYTAtMjgyMi00ZDQ1LWE3ODUtNzA2MjI0ODhmMDdiIiwiYWNjZXNzIjoiVmlldyIsImlhdCI6MTc2MDAzNTE1MX0.wm2mKxWjzvwab4ghdZsg6y5bn0FZnB_KudsAd3WhaoE) 
- [**Backend models**](https://www.mermaidchart.com/app/projects/26c39244-5e0c-4374-b7fd-d7be5431add5/diagrams/f4386989-dde2-4470-827c-3a3f447aba1c/share/invite/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkb2N1bWVudElEIjoiZjQzODY5ODktZGRlMi00NDcwLTgyN2MtM2EzZjQ0N2FiYTFjIiwiYWNjZXNzIjoiVmlldyIsImlhdCI6MTc2MDAzNTM5Mn0.l-IXK6kuI0MS4H9UF-HTH8y0NePAH1GyeIBwKtWszcA)
- [**Backend Architecture & Data Flow Overview**](https://github.com/onepass-ch/onepass/wiki/Backend-Architecture-&-Data-Flow-Overview)
---
