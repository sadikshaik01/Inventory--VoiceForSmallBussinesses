# ⚙️ Functional Requirements

## 📌 Project

**Voice-Based Inventory Management Platform for Small Businesses**

The system is a mobile and web application that allows small-business owners to manage products and inventory using **voice commands as the primary interaction method**, with manual input available as a fallback.

---

# 👤 User Management

### FR-01 — User Registration

Users can create an account using:

- Name
- Email
- Password
- Business name
- Preferred language

### FR-02 — User Login

Registered users can securely log into their account.

### FR-03 — User Profile

Users can view and update their personal and business information.

### FR-04 — Language Selection

Users can select their preferred interface and voice language.

**Supported languages:**

- English
- Telugu
- Hindi
- Mixed-language commands

---

# 📦 Product Management

### FR-05 — Product Creation

Users can create products with:

- Product name
- Category
- Primary unit
- Current/initial stock
- Minimum stock level
- Optional price

### FR-06 — Product Editing

Users can edit:

- Product name
- Category
- Unit
- Minimum stock level

### FR-07 — Product Deletion / Archiving

Users can remove or archive products while preserving historical transactions where applicable.

### FR-08 — Product Search

Users can search for products by:

- Product name
- Category

---

# 📊 Inventory Operations

### FR-09 — Manual Stock Addition

Users can manually add stock to an existing product.

```text
Rice → +20 Bags
```

### FR-10 — Manual Stock Removal

Users can manually remove stock.

```text
Rice → -5 Bags
```

### FR-11 — Voice Input

Users can record inventory commands using their device microphone.

### FR-12 — Voice Stock Addition

The system understands commands for adding stock.

**Example:**

> "Add 20 bags of rice."

The system identifies:

```text
Action: ADD
Product: Rice
Quantity: 20
Unit: Bags
```

### FR-13 — Voice Stock Removal

The system understands commands for removing stock.

**Example:**

> "Remove 5 cartons of biscuits."

The system identifies:

```text
Action: REMOVE
Product: Biscuits
Quantity: 5
Unit: Cartons
```

---

# 🎙️ Voice & AI Requirements

### FR-14 — Natural Language Understanding

The system converts natural-language voice commands into structured inventory actions.

It identifies:

- Intent / action
- Product
- Quantity
- Unit

### FR-15 — Voice Stock Query

Users can ask for their current stock using voice.

**Example:**

> "How much rice do I have?"

The system returns the current rice quantity.

### FR-16 — Voice Low-Stock Query

Users can ask:

> "Which products are running low?"

The system returns products below their configured minimum stock level.

### FR-17 — Voice Out-of-Stock Query

Users can ask:

> "Which products are out of stock?"

The system returns products with zero available stock.

### FR-18 — Regional & Mixed-Language Commands

The system supports mixed-language voice commands.

**Example:**

> "Rice 5 bags add cheyyi."

The system interprets:

```text
ADD → Rice → 5 → Bags
```

### FR-19 — Trade Unit Recognition

The system recognizes:

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

### FR-20 — Voice Command Confirmation

Before modifying inventory, the system displays the interpreted command and requests confirmation.

**Example:**

> "Add 20 bags of Rice?"

The user can:

- ✅ Confirm
- ✏️ Edit
- ❌ Cancel

### FR-21 — Voice Command Correction

Users can correct an incorrectly interpreted command before the inventory is updated.

---

# ✅ Validation Requirements

### FR-22 — Unknown Product Validation

If a command refers to a product that does not exist, the system informs the user.

```text
"Wheat is not in your inventory.
Would you like to add it?"
```

### FR-23 — Missing Information Validation

If required information is missing, the system asks the user for it.

**Example:**

```text
User:
"Add rice."

System:
"How many bags or kilograms of rice
would you like to add?"
```

### FR-24 — Ambiguous Unit Validation

If the unit is unclear, the system asks the user to specify or confirm it.

### FR-25 — Insufficient Stock Validation

The system prevents users from removing more stock than currently available.

```text
Current Stock: 20 Bags
Requested Removal: 50 Bags
```

The operation is rejected and an appropriate message is displayed.

---

# 🧾 Transaction Management

### FR-26 — Inventory Transaction Creation

Every successful stock addition or removal creates an inventory transaction.

Each transaction stores:

- Product
- Quantity
- Unit
- Transaction type
- Source
- Date/time
- User

### FR-27 — Transaction History

Users can view previous inventory transactions.

Transactions indicate whether they originated from:

- Voice
- Manual entry

### FR-28 — Current Stock Calculation

Current stock is maintained based on successful inventory transactions.

```text
Current Stock =
Total Added − Total Removed
```

---

# 🚨 Stock Alerts & Reordering

### FR-29 — Minimum Stock Level

Users can configure a minimum stock level for each product.

### FR-30 — Low-Stock Alert

The system identifies products whose stock falls below the configured minimum level.

Example:

```text
Rice
Current Stock: 8 Bags
Minimum Level: 10 Bags

Status: LOW STOCK
```

### FR-31 — Reorder Suggestion

The system provides basic reorder suggestions for products that are low or out of stock.

---

# 📊 Dashboard

### FR-32 — Dashboard

The dashboard displays important inventory information:

- Total products
- Low-stock products
- Out-of-stock products
- Today's transactions
- Recent transactions
- Voice assistant access

### FR-33 — Inventory Overview

The inventory overview displays:

- Product name
- Current stock
- Unit
- Minimum stock level
- Stock status

---

# ✍️ Manual Fallback

### FR-34 — Manual Fallback

If voice recognition fails or the user prefers manual input, the system provides manual inventory operations.

Users can:

1. Select a product
2. Select Add / Remove
3. Enter quantity
4. Select unit
5. Confirm the operation

---

# 📱 Cross-Platform Requirements

### FR-35 — Mobile Application

The system provides a mobile-friendly interface optimized for smartphones.

The mobile interface prioritizes:

- 🎙️ Voice interaction
- 🔘 Large buttons
- 🧭 Simple navigation
- ⚡ Quick inventory updates

### FR-36 — Web Application

The system provides a responsive web application for desktop and laptop users.

The web application provides the same core inventory functionality.

### FR-37 — Cross-Platform Data Synchronization

Mobile and web applications use the same backend and centralized database.

An inventory change made through one platform must be available on the other.

```text
Mobile
  ↓
Rice = 65 Bags
  ↓
Backend / Database
  ↓
Web
  ↓
Rice = 65 Bags
```

---

# 🔐 Security & Access Requirements

### FR-38 — User Data Isolation

Each user can access only their own:

- Products
- Inventory
- Transactions

### FR-39 — Authentication & Authorization

Protected inventory operations require authentication.

Requests are authorized based on the logged-in user's identity.

---

# ⚠️ Error Handling

### FR-40 — Error Handling

The system provides clear and user-friendly messages for:

- Invalid commands
- Voice recognition failures
- Unknown products
- Missing information
- Invalid quantities
- Insufficient stock
- Network failures
- Unauthorized requests
- Server errors

Technical error details should not be exposed to normal users.

---

# 🔄 Main User Journey

```text
Login
  ↓
Dashboard
  ↓
Tap Microphone
  ↓
Speak Command
  ↓
Speech-to-Text
  ↓
AI/NLP Processing
  ↓
Intent + Product + Quantity + Unit
  ↓
Validation
  ↓
User Confirmation
  ↓
Backend Processing
  ↓
Database Update
  ↓
Transaction Recorded
  ↓
Stock Status Checked
  ↓
Dashboard Updated
```

---

# 🧪 Functional Acceptance Scenarios

## Scenario 1 — Add Stock Through Voice

**Given:** Rice exists in the inventory.

**When:** User says:

> "Add 20 bags of rice."

**Then:**

1. Voice is converted to text.
2. System identifies the ADD action.
3. System identifies Rice.
4. System identifies 20 Bags.
5. Confirmation is displayed.
6. User confirms.
7. Inventory increases by 20 Bags.
8. Transaction is recorded.

---

## Scenario 2 — Remove Stock Through Voice

**Given:** Rice stock is 50 Bags.

**When:** User says:

> "Remove 10 bags of rice."

**Then:** The system confirms the command and changes the stock to **40 Bags** after confirmation.

---

## Scenario 3 — Check Stock

**When:** User says:

> "How much rice do I have?"

**Then:** The system returns the current rice quantity.

---

## Scenario 4 — Low Stock

**Given:**

```text
Current Stock = 8 Bags
Minimum Stock = 10 Bags
```

**Then:** The system marks the product as **Low Stock** and displays an alert.

---

## Scenario 5 — Insufficient Stock

**Given:**

```text
Current Stock = 20 Bags
```

**When:** User requests:

> "Remove 50 bags of rice."

**Then:** The system rejects the operation and informs the user that only **20 Bags** are available.

---

## Scenario 6 — Mixed-Language Command

**When:** User says:

> "Rice 5 bags add cheyyi."

**Then:** The system identifies:

```text
Action   = ADD
Product  = Rice
Quantity = 5
Unit     = Bags
```

The system requests confirmation before updating the inventory.

---

## Scenario 7 — Cross-Platform Synchronization

**When:** User adds 20 Bags of Rice through the mobile application.

**Then:** The updated stock is also displayed when the same user opens the web application.

---

# 📌 Functional Priority

| Priority | Features |
|---|---|
| **P0 — Critical** | Login, Product Management, Inventory Add/Remove, Voice Input, AI/NLP, Confirmation, Database |
| **P1 — Important** | Regional Language, Trade Units, Stock Queries, Alerts, Transactions, Manual Fallback |
| **P2 — Future** | Advanced Analytics, Barcode Scanning, Forecasting, Supplier Management |

---

# 🧠 Expected System Behaviour

The application follows:

```text
Speak
  ↓
Understand
  ↓
Validate
  ↓
Confirm
  ↓
Update
  ↓
Track
  ↓
Alert
```

The **AI/NLP component interprets user commands but does not directly modify the database**.

All inventory changes must pass through:

```text
Voice / Manual Input
        ↓
AI/NLP Interpretation
        ↓
Backend Validation
        ↓
Confirmation
        ↓
Transaction Processing
        ↓
Database Update
```

This ensures a simple voice-first experience while maintaining accurate and controlled inventory records.