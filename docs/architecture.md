# Voice-Based Inventory Management Platform for Small Businesses

## 🏗️ Technical Design & Solution Architecture

A cross-platform, voice-first inventory management system designed for small businesses. The platform allows users to manage inventory through **voice commands or manual input**, with AI/NLP handling natural-language commands and a Spring Boot backend controlling all inventory operations.

---

## 📋 Table of Contents

- [1. Technology Stack](#1-technology-stack)
- [2. Application Architecture](#2-application-architecture)
- [3. Frontend Architecture](#3-frontend-architecture)
- [4. Backend Architecture](#4-backend-architecture)
- [5. Database Design](#5-database-design)
- [6. Database Relationships](#6-database-relationships)
- [7. API Design](#7-api-design)
- [8. Voice & AI Processing](#8-voice--ai-processing)
- [9. Natural Language Processing](#9-natural-language-processing)
- [10. Multilingual Voice Processing](#10-multilingual-voice-processing)
- [11. Voice Command Confirmation](#11-voice-command-confirmation)
- [12. Inventory Processing](#12-inventory-processing)
- [13. Stock Calculation](#13-stock-calculation)
- [14. Trade Unit Support](#14-trade-unit-support)
- [15. Low-Stock & Reorder Logic](#15-low-stock--reorder-logic)
- [16. Authentication & Security](#16-authentication--security)
- [17. User Data Isolation](#17-user-data-isolation)
- [18. Mobile & Web Data Flow](#18-mobile--web-data-flow)
- [19. Error Handling](#19-error-handling)
- [20. External Integrations](#20-external-integrations)
- [21. Deployment Architecture](#21-deployment-architecture)
- [22. Frontend Project Structure](#22-frontend-project-structure)
- [23. Backend Project Structure](#23-backend-project-structure)
- [24. Complete Data Flow](#24-complete-data-flow)
- [25. Scalability & Future Extensions](#25-scalability--future-extensions)
- [26. Technical Constraints](#26-technical-constraints)
- [27. Architecture Summary](#27-architecture-summary)

---

# 1. Technology Stack

| Layer | Technology |
|---|---|
| Mobile & Web Frontend | React.js, Vite |
| UI/UX | Tailwind CSS |
| Mobile Approach | Responsive PWA / Mobile-first |
| Backend | Java Spring Boot |
| API | RESTful APIs |
| Database | PostgreSQL |
| Database Hosting | Supabase |
| Authentication | JWT |
| Speech-to-Text | Speech Recognition API |
| AI/NLP | LLM-based Natural Language Processing |
| Version Control | Git, GitHub |
| API Testing | Postman |
| Frontend Deployment | Vercel |
| Backend Deployment | Render |

---

# 2. Application Architecture

The system follows a **client-server architecture** with separate frontend clients and a centralized backend.

Both mobile and web applications communicate with the same Spring Boot REST API and PostgreSQL database.

```text
                         ┌──────────────────────┐
                         │        USER          │
                         │  Small Business User │
                         └──────────┬───────────┘
                                    │
                       Voice / Manual Input
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
          ┌─────────▼─────────┐           ┌────────▼─────────┐
          │   MOBILE CLIENT   │           │    WEB CLIENT    │
          │ React / PWA       │           │ React / Vite     │
          └─────────┬─────────┘           └────────┬─────────┘
                    │                              │
                    └──────────────┬───────────────┘
                                   │
                              HTTPS / REST
                                   │
                     ┌─────────────▼─────────────┐
                     │      SPRING BOOT          │
                     │       BACKEND             │
                     │                           │
                     │ Authentication            │
                     │ Product Management         │
                     │ Inventory Management       │
                     │ Voice Processing           │
                     │ AI/NLP Processing          │
                     │ Validation                 │
                     │ Alerts                     │
                     │ Transactions               │
                     └───────┬───────────┬───────┘
                             │           │
                ┌────────────▼───┐   ┌──▼─────────────────┐
                │   PostgreSQL   │   │  Voice / AI APIs   │
                │   Database     │   │                   │
                │               │   │ Speech-to-Text     │
                │ Users         │   │ AI/NLP             │
                │ Products      │   └────────────────────┘
                │ Transactions  │
                └───────────────┘
```

### Core Architectural Principle

> **AI interprets the command; the backend validates and controls the inventory.**

AI/NLP never directly modifies the database.

---

# 3. Frontend Architecture

The frontend is developed using **React.js and Vite**.

The application provides two interfaces:

## 📱 Mobile Interface

A mobile-first responsive/PWA interface optimized for:

- Voice interaction
- Quick stock updates
- Large buttons
- Simple navigation
- Touch interaction

## 💻 Web Interface

A desktop-friendly interface optimized for:

- Inventory tables
- Dashboard
- Product management
- Transaction history
- Alerts
- Voice interaction

Both interfaces communicate with the same backend APIs.

```text
Mobile PWA ────────┐
                   ├──→ Spring Boot REST API
Web Application ───┘
```

---

# 4. Backend Architecture

The backend uses **Java Spring Boot** and follows a layered architecture.

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
PostgreSQL
```

## Controller Layer

Responsible for:

- Handling HTTP requests
- Exposing REST APIs
- Request/response processing

## Service Layer

Contains business logic such as:

- Stock calculations
- Validation
- Product operations
- Voice command processing
- Alert generation

## Repository Layer

Responsible for communicating with PostgreSQL.

## Security Layer

Handles:

- JWT authentication
- Authorization
- Protected endpoints
- User access control

---

# 5. Database Design

PostgreSQL is used for persistent application data, with **Supabase** providing managed database hosting.

## Users

```text
users
-------------------------
id
name
email
password
business_name
preferred_language
created_at
updated_at
```

## Products

```text
products
-------------------------
id
user_id
name
category
unit
current_stock
minimum_stock
price
created_at
updated_at
```

## Inventory Transactions

```text
inventory_transactions
-------------------------
id
user_id
product_id
transaction_type
quantity
unit
source
created_at
```

### Transaction Types

```text
ADD
REMOVE
```

### Transaction Sources

```text
VOICE
MANUAL
```

---

# 6. Database Relationships

The database follows a user → products → transactions relationship.

```text
┌──────────────┐
│    USERS     │
├──────────────┤
│ id           │
│ name         │
│ email        │
└──────┬───────┘
       │
       │ 1 : N
       ▼
┌──────────────┐
│   PRODUCTS   │
├──────────────┤
│ id           │
│ user_id      │
│ name         │
│ unit         │
│ current_stock│
│ minimum_stock│
└──────┬───────┘
       │
       │ 1 : N
       ▼
┌────────────────────────┐
│ INVENTORY_TRANSACTIONS │
├────────────────────────┤
│ id                     │
│ user_id                │
│ product_id             │
│ transaction_type       │
│ quantity               │
│ unit                   │
│ source                 │
│ created_at             │
└────────────────────────┘
```

---

# 7. API Design

The backend exposes RESTful APIs for authentication, products, inventory, transactions, alerts, and voice processing.

## Authentication

```http
POST /api/auth/register
POST /api/auth/login
```

## User

```http
GET /api/users/profile
PUT /api/users/profile
```

## Products

```http
GET    /api/products
POST   /api/products
GET    /api/products/{id}
PUT    /api/products/{id}
DELETE /api/products/{id}
```

## Inventory

```http
GET  /api/inventory
POST /api/inventory/add
POST /api/inventory/remove
```

## Transactions

```http
GET /api/transactions
```

## Alerts

```http
GET /api/inventory/low-stock
```

## Voice Processing

```http
POST /api/voice/process
```

## AI Assistant

```http
POST /api/assistant/query
```

---

# 8. Voice & AI Processing

Voice interaction is the core feature of the platform.

The processing pipeline is:

```text
User Speech
     ↓
Device Microphone
     ↓
Speech-to-Text API
     ↓
Recognized Text
     ↓
AI/NLP Processing
     ↓
Intent + Entities
     ↓
Validation
     ↓
User Confirmation
     ↓
Backend API
     ↓
Database
```

The AI layer converts natural speech into a structured command that can be validated by the backend.

---

# 9. Natural Language Processing

The AI/NLP component identifies:

- Intent
- Product
- Quantity
- Unit

### Example: Add Stock

**Input:**

> "Add 20 bags of rice."

**AI Output:**

```json
{
  "intent": "ADD_STOCK",
  "product": "rice",
  "quantity": 20,
  "unit": "bags"
}
```

### Example: Check Stock

**Input:**

> "How much rice do I have?"

**AI Output:**

```json
{
  "intent": "CHECK_STOCK",
  "product": "rice"
}
```

The structured command is then passed through validation before any inventory modification occurs.

---

# 10. Multilingual Voice Processing

The system supports:

- English
- Telugu
- Hindi
- Mixed English + regional language

### Example

> "Rice 5 bags add cheyyi."

The AI converts the command into:

```text
Intent: ADD_STOCK
Product: Rice
Quantity: 5
Unit: Bags
```

The backend operates on the standardized command regardless of the language used by the user.

---

# 11. Voice Command Confirmation

The AI **does not directly update the database**.

Instead, every inventory modification follows this flow:

```text
Voice
 ↓
Speech-to-Text
 ↓
AI/NLP
 ↓
Structured Command
 ↓
Validation
 ↓
Confirmation
 ↓
User Confirms
 ↓
Backend
 ↓
Database
```

### Example Confirmation

```text
AI understood:

ADD STOCK
Rice
20 Bags

"Add 20 bags of Rice?"

[ CONFIRM ] [ EDIT ] [ CANCEL ]
```

Only after user confirmation does the backend execute the inventory transaction.

---

# 12. Inventory Processing

For every stock operation, the backend performs the following:

1. Authenticate the user.
2. Identify the product.
3. Validate the quantity.
4. Validate the unit.
5. Check available stock for removals.
6. Update the inventory.
7. Create a transaction record.
8. Check the minimum stock level.
9. Generate an alert if necessary.
10. Return updated inventory information.

This ensures that AI-generated commands cannot bypass business rules.

---

# 13. Stock Calculation

The system maintains inventory using stock transactions.

```text
Current Stock =
Total Added Quantity
-
Total Removed Quantity
```

### Example

```text
Initial Stock = 50 Bags
Added         = 20 Bags
Removed       = 5 Bags

Current Stock = 65 Bags
```

---

# 14. Trade Unit Support

The system supports the following units:

- Pieces
- Kg
- Grams
- Litres
- Millilitres
- Bags
- Cartons
- Boxes
- Dozens
- Quintals

Each product has a **primary unit**.

### Examples

```text
Rice       → Bags
Sugar      → Kg
Oil        → Litres
Biscuits   → Cartons
Soap       → Pieces
```

The system does not assume universal conversions because units such as **bags** may have different meanings depending on the product.

---

# 15. Low-Stock & Reorder Logic

Each product has a configured minimum stock level.

### Example

```text
Product: Rice
Current Stock: 8 Bags
Minimum Level: 10 Bags
```

The system checks:

```text
Current Stock < Minimum Stock
```

If the condition is true:

```text
Status = LOW STOCK
```

The product is displayed in the alert section.

The MVP can provide a basic reorder suggestion based on the configured minimum stock level.

---

# 16. Authentication & Security

The platform uses **JWT authentication**.

## Authentication Flow

```text
User Login
    ↓
Spring Boot
    ↓
Validate Credentials
    ↓
Generate JWT
    ↓
Send Token
    ↓
Client Stores Token
    ↓
Token Included in API Requests
    ↓
Backend Validates Token
```

## Security Measures

- Password hashing
- JWT authentication
- Protected REST APIs
- Authorization checks
- HTTPS
- Input validation
- User-specific data access
- Secure database access
- Transaction logging

---

# 17. User Data Isolation

Every product and transaction is associated with a user.

```text
User A
 ├── Rice
 ├── Sugar
 └── Biscuits

User B
 ├── Rice
 └── Oil
```

The backend uses the authenticated user's ID to ensure that one user cannot access another user's inventory.

---

# 18. Mobile & Web Data Flow

Both mobile and web applications communicate with the same centralized backend.

```text
                ┌──────────────┐
                │ Mobile App   │
                └──────┬───────┘
                       │
                       │
                ┌──────▼───────┐
                │ Spring Boot  │
                │ REST API     │
                └──────┬───────┘
                       │
                ┌──────▼───────┐
                │ PostgreSQL   │
                └──────┬───────┘
                       │
                ┌──────▼───────┐
                │ Spring Boot  │
                │ REST API     │
                └──────┬───────┘
                       │
                ┌──────▼───────┐
                │  Web App     │
                └──────────────┘
```

An inventory update made through the mobile application becomes available to the web application through the centralized database.

---

# 19. Error Handling

The system provides user-friendly errors for:

- Invalid login
- Unknown product
- Missing quantity
- Missing unit
- Invalid quantity
- Insufficient stock
- Voice recognition failure
- AI interpretation failure
- Network failure
- Unauthorized requests
- Server errors

### Example: Insufficient Stock

> "You currently have only 20 bags of rice. You cannot remove 50 bags."

### Example: Voice Recognition Failure

> "I couldn't understand the request. Please try again or enter it manually."

Technical implementation details should not be exposed to end users.

---

# 20. External Integrations

The application integrates with external services for specific functionality.

## Speech-to-Text

Converts spoken commands into text.

## AI/NLP

Interprets recognized text and extracts structured inventory information.

## Supabase

Provides managed PostgreSQL database hosting.

External APIs should be accessed through secure credentials and must not expose sensitive API keys directly in the frontend.

---

# 21. Deployment Architecture

```text
                         INTERNET
                            │
             ┌──────────────┴──────────────┐
             │                             │
             ▼                             ▼
          VERCEL                         RENDER
             │                             │
       React Frontend                Spring Boot API
                                           │
                                           │
                                           ▼
                                      SUPABASE
                                           │
                                           ▼
                                      PostgreSQL

                            ┌─────────────────────┐
                            │ External Services   │
                            │                     │
                            │ Speech-to-Text      │
                            │ AI/NLP              │
                            └─────────────────────┘
```

### Deployment Components

| Component | Platform |
|---|---|
| Frontend | Vercel |
| Backend | Render |
| Database | Supabase PostgreSQL |
| Version Control | GitHub |
| External AI/Voice | Speech-to-Text & AI/NLP APIs |

---

# 22. Frontend Project Structure

```text
src/
│
├── components/
│   ├── Navbar
│   ├── Sidebar
│   ├── VoiceButton
│   ├── ProductCard
│   ├── AlertCard
│   └── TransactionList
│
├── pages/
│   ├── Login
│   ├── Register
│   ├── Dashboard
│   ├── Inventory
│   ├── ProductDetails
│   ├── VoiceAssistant
│   ├── Alerts
│   ├── Transactions
│   └── Settings
│
├── services/
│   ├── authService
│   ├── productService
│   ├── inventoryService
│   └── voiceService
│
└── App.jsx
```

The frontend structure separates reusable UI components, pages, and API service logic.

---

# 23. Backend Project Structure

```text
src/main/java/
│
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
├── security/
├── exception/
└── config/
```

The layered structure separates:

- API handling
- Business logic
- Database access
- Entity models
- Data transfer objects
- Security
- Exception handling
- Application configuration

---

# 24. Complete Data Flow

```text
                       USER
                         │
                         ▼
              Mobile / Web Application
                         │
                  Voice / Manual
                         │
            ┌────────────┴────────────┐
            │                         │
        Manual Input              Voice Input
            │                         │
            │                  Speech-to-Text
            │                         │
            │                         ▼
            │                     AI / NLP
            │                         │
            └────────────┬────────────┘
                         ▼
                    Structured
                      Command
                         │
                         ▼
                     Validation
                         │
                         ▼
                    Confirmation
                         │
                         ▼
                  Spring Boot API
                         │
             ┌───────────┴───────────┐
             │                       │
        Authentication          Business Logic
             │                       │
             └───────────┬───────────┘
                         ▼
                     PostgreSQL
                         │
                         ▼
                Inventory Updated
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
          Dashboard              Alerts
              │
              ▼
       Transaction History
```

---

# 25. Scalability & Future Extensions

The architecture can support additional modules without replacing the core architecture.

Potential future extensions include:

- Barcode scanning
- Supplier management
- Sales management
- Purchase management
- WhatsApp integration
- Automated purchase orders
- Advanced analytics
- Demand forecasting
- Employee accounts
- Multiple business branches
- Native Android/iOS applications

These capabilities can be implemented as additional modules around the existing architecture.

---

# 26. Technical Constraints

The MVP has the following constraints:

1. Internet connectivity is required for cloud-based AI and speech services.
2. Speech recognition accuracy may vary by language and pronunciation.
3. Mixed-language commands may require confirmation.
4. External AI APIs may have usage and rate limits.
5. The hackathon MVP focuses on inventory rather than complete ERP functionality.
6. A responsive PWA approach is preferred for rapid mobile and web delivery.

---

# 27. Architecture Summary

## Core Architecture

```text
React Mobile/Web
       │
       ▼
REST API
       │
       ▼
Spring Boot
       │
       ▼
PostgreSQL / Supabase
```

## Voice Processing Architecture

```text
Voice
  ↓
Speech-to-Text
  ↓
AI/NLP
  ↓
Structured Command
  ↓
Validation
  ↓
Confirmation
  ↓
Backend
  ↓
Database
```

## Key Architectural Features

- 📱 Cross-platform mobile and web access
- 🎙️ Voice-first interaction
- 🤖 AI-assisted command understanding
- 🔐 JWT-based authentication
- 🗄️ Centralized PostgreSQL database
- 🔄 Mobile/web synchronization
- 📦 Inventory and transaction management
- ⚠️ Low-stock alerts
- ✍️ Manual input fallback
- 🌐 Multilingual and mixed-language support
- 📈 Extensible architecture for future modules

---

## 🔑 Core Design Principle

> **AI interprets the command; the backend validates and controls the inventory.**

This separation ensures that natural-language processing remains responsible for understanding user intent, while the backend remains responsible for authentication, validation, business rules, inventory updates, and transaction persistence.
