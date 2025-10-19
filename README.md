
# 🗨️ Distributed Group Chat Application (Java + JavaFX)

This is a fault-tolerant, distributed group chat system implemented using Java, JavaFX, and core object-oriented design principles. It supports coordinator election, real-time messaging (private & broadcast), member list sharing, heartbeat checks, and reconnection logic — all wrapped in a simple JavaFX GUI.

---

## 📦 Features

- ✅ Unique user identification with ID validation
- 🗣️ Broadcast and private messaging between users
- 🧑‍⚖️ Dynamic coordinator election (first join or on failure)
- 💓 Heartbeat mechanism to detect inactive members
- 🫱🏼‍🫲🏼 Member list sharing via coordinator approval
- 🔌 Disconnection handling and reconnection support
- 👁️ Modern Observer pattern for real-time UI updates
- 🏭 Factory pattern for consistent message creation
- 📊 JavaFX-based GUI for chat interface and member controls
- 🧪 JUnit-tested logic for client, server, and messaging

---

## 🏗️ Project Structure

```
project-root/
├── client/              # Client logic (Client.java, ChatGUI.java, ClientObserver.java)
├── server/              # Server logic (Server.java, ClientHandler.java, Coordinator.java)
├── common/              # Shared models (Message.java, Member.java, MessageType.java)
├── utils/               # Utilities (MessageFactory.java, ObservableSubject.java, LoggerUtil.java, NameValidator.java)
├── main/                # Entry points (MainGUI.java, MainServer.java)
├── JUnitTests/          # Unit tests (ClientTest, MemberTest, ServerTest, MessageTest)
└── README.md            # This file
```

---

## ⚙️ How It Works

### 🔑 Join Process
1. User opens the client setup window (`MainGUI`) and enters a unique ID.
2. Client connects to server (`MainServer`) and sends a `JOIN` message.
3. If first to join, client becomes the **coordinator**.

### 💬 Messaging
- Users can send:
  - 📣 Broadcast messages (to everyone)
  - 🔒 Private messages (to specific member)

### 📋 Member List Sharing
- Clients request member lists via the coordinator.
- Coordinator can approve or deny requests.
- Approved clients receive updated list of online users.

### 🫀 Heartbeat & Timeout
- Coordinator sends regular heartbeats.
- Clients must respond within 60 seconds.
- Non-responding clients are auto-removed, and a new coordinator is elected if needed.

### 🔁 Reconnection
- Disconnected clients can reconnect using the same ID (if available).
- GUI reflects the current connection state and disables/enables buttons accordingly.

---

## 🧩 Design Patterns

### 🔁 Observer Pattern
- Custom-built using `ObservableSubject<T>`.
- `Client` notifies `ChatGUI` of incoming messages, disconnections, and member updates.

### 🏭 Factory Pattern
- `MessageFactory` handles all `Message` object creation:
  - `JOIN`, `LEAVE`, `HEARTBEAT`, `PRIVATE`, `BROADCAST`, etc.
- Promotes clean, reusable, centralized object construction.

---

## 🛠️ How to Run

### Prerequisites
- Java 17 or higher
- JavaFX SDK installed & configured in your IDE

### 1. Start Server
```bash
Run main/MainServer.java
```

### 2. Start Clients
```bash
Run main/MainGUI.java
```

You can start multiple clients, each with a unique ID, to simulate a group chat session.

---

## 🧪 Testing

JUnit 5 tests are located in the `JUnitTests/` package.

### Test Coverage:
- `ClientTest` → connection, message sending, disconnection
- `MemberTest` → member construction, pinging, coordinator flag
- `MessageTest` → message type validation, content, payloads
- `ServerTest` → heartbeat handling, member management, election logic

### Run Tests:
In IntelliJ or compatible IDE:
```
Right-click on the test directory → Run All Tests
```

---

## 🧠 Fault Tolerance

The system is designed to gracefully handle:
- ❌ Duplicate user IDs (client gets ERROR)
- 🔌 Manual or unexpected disconnections
- 💤 Inactive clients via heartbeat timeout
- 🏃 Coordinator reassignment on failure
- 🔁 Reconnection and session restoration

---

## 🧠 Technical Highlights

- **Concurrency**: Multithreaded client-server communication
- **Serialization**: Java object streams for message transport
- **Decoupling**: Observer pattern between networking and UI
- **Safety**: Reflection used in JUnit to test internal heartbeat logic
- **Custom Design Patterns**: Avoids deprecated `java.util.Observable`

---


## 📚 Authors

- Marco Gizzi [001327359]
- Giulio Dajani [001343717]
- Hanzla Ilyas [001060407]
- Nicola Ria [001339810]
- Sebastian Andrei Carp [001289569]

---

## 📄 License

MIT License

Copyright (c) 2025 [Authors]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
copies of the Software, and to permit persons to whom the Software is 
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
SOFTWARE.
