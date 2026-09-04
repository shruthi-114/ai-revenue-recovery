# AI Revenue Recovery — Smart Payment Retry Engine

Built for the **Razorpay Build Challenge** — Track: *AI Revenue Recovery*

---

## 1. Project Objectives — What does it solve?

Every payment gateway loses real revenue to failed transactions — a card
declines, a network blips, a customer's OTP times out, or their account
is simply low on funds at that moment. Today, most systems either:

- Retry blindly on a fixed schedule (wastes money on payments that will
  never succeed, and annoys customers with repeated attempts), or
- Don't retry at all and just lose the sale.

**This project solves that by asking one question for every failed
payment: "Is this actually worth retrying, and if so, when?"**

It's a lightweight decision engine that:
1. Looks at *why* a payment failed, the customer's payment history, how
   many retries have already been attempted, and what time it failed.
2. Converts those signals into a transparent **0–100 recovery score**.
3. Turns that score into a concrete action — retry immediately, retry
   later, send a reminder (e.g. for insufficient funds, wait for salary
   day), or don't retry at all.
4. Simulates running those retries and shows how much revenue was
   actually recovered, live, on a dashboard.

The goal isn't to blindly retry everything — it's to recover revenue
**without wasting attempts or annoying customers**, which is exactly
the trade-off a payments company like Razorpay has to make at scale.

---

## 2. Architecture

```
                        ┌───────────────────────────┐
                        │        Web Browser         │
                        │  (index.html + style.css   │
                        │   + app.js, plain vanilla) │
                        └──────────────┬─────────────┘
                                       │  fetch() calls
                                       │  GET /api/transactions
                                       │  POST /api/retry
                                       ▼
                        ┌───────────────────────────┐
                        │      ApiServer.java        │
                        │  (built on JDK's own       │
                        │  com.sun.net.httpserver)   │
                        │  - serves static files      │
                        │  - exposes JSON API          │
                        └──────────────┬─────────────┘
                                       │
                     ┌─────────────────┼─────────────────┐
                     ▼                                    ▼
        ┌─────────────────────────┐        ┌───────────────────────────┐
        │      DataStore.java      │        │     RecoveryEngine.java    │
        │  in-memory list of        │        │  - calculateScore()        │
        │  failed Transaction        │◄──────┤  - decideAction()          │
        │  objects (sample data)     │        │  - simulateRetries()       │
        └─────────────────────────┘        └───────────────────────────┘
                     │
                     ▼
        ┌─────────────────────────┐
        │      Transaction.java     │
        │  plain data model +        │
        │  hand-written toJson()      │
        └─────────────────────────┘
```

**Why it's built this way:**

- **No frameworks (no Spring Boot, no React/Vite).** Everything runs off
  the plain JDK. This was a deliberate choice — the logic is the
  interesting part of this project, not framework configuration. Anyone
  can clone this, run two commands, and see it work with zero setup.
- **Rule-based / explainable scoring instead of a black-box ML model.**
  With no historical training dataset available, a trained model would
  either be fake or overfit on invented numbers. A transparent, weighted
  rule engine is honest about what it's doing, is fully explainable to a
  judge or a compliance team, and mirrors the same signals a trained
  model would end up weighting anyway.
- **In-memory data store.** Keeps the demo instant to run. Swapping it
  for a real database is a small, isolated change (see "Next Steps").

### Tech stack
| Layer | Technology |
|---|---|
| Backend | Plain Java 17+ (`com.sun.net.httpserver`), zero external dependencies |
| Frontend | Vanilla HTML, CSS, JavaScript (no build step, no npm) |
| Data | In-memory Java objects (swap-in-ready for a real DB) |

---

## 3. How to Run

You only need a JDK installed (Java 17 or newer). No Maven, no Gradle, no npm.

```bash
# 1. Compile
javac -d out $(find src -name "*.java")

# 2. Run
java -cp out com.recovery.Main

# 3. Open the dashboard
# Visit http://localhost:8080 in your browser
```

Click **"Load Failed Payments"** to see the scored transactions, then
**"Run Smart Retry"** to simulate the recovery attempts and watch the
revenue-recovered stats update.

---

## 4. Build Challenges & Technical Obstacles

**1. Deciding how to represent "AI" honestly.**
The track is called AI Revenue Recovery, and it would have been easy to
just call a rule engine "AI" without explaining it. Instead I treated it
as an explainable scoring model and documented exactly which signals it
weighs and why — the same approach real fraud/risk teams use before they
trust a black-box model in production. This also solved the practical
problem of not having a labeled training dataset to work with.

**2. Keeping it dependency-free while still returning JSON.**
Without Maven/Gradle, I didn't have access to a JSON library like Gson
or Jackson. I solved this by hand-writing a small `toJson()` method on
the `Transaction` model itself, since the data shape is small and fixed.
It keeps the whole project buildable with just `javac`, no internet
access needed to fetch dependencies.

**3. Avoiding a naive "retry everything immediately" approach.**
An early version of the scoring logic retried every failure right away.
That's unrealistic — instantly retrying an "insufficient funds" failure
almost never works and just annoys the customer. I added a specific rule
so that insufficient-funds failures get a "send reminder" decision
instead of an instant retry, closer to how a real recovery system
would behave.

**4. Making the retry outcome feel real without a live payment gateway.**
Since this is a hackathon build without a production Razorpay
integration, retry outcomes are simulated. I tied the simulated success
probability directly to the recovery score itself (higher score → higher
chance of a successful simulated retry), so the dashboard numbers stay
consistent with the logic instead of being random noise.

---

## 5. Next Steps (if this became a real product)

- Swap `DataStore` for a real database (Postgres) and connect to actual
  webhook events from a payment gateway instead of sample data.
- Replace the rule-based engine with a trained model once enough labeled
  retry-outcome data exists, while keeping the same score → decision
  interface so nothing else in the system has to change.
- Add a scheduler (e.g. a simple cron-style Java `ScheduledExecutorService`)
  to actually fire retries at the suggested time instead of simulating
  them instantly.
- Add authentication and multi-merchant support for a real dashboard.

---

## 6. Repository Structure

```
ai-revenue-recovery/
├── src/com/recovery/
│   ├── Main.java                  # entry point
│   ├── model/Transaction.java     # data model
│   ├── service/RecoveryEngine.java # scoring + decision logic
│   ├── service/DataStore.java     # sample data
│   └── server/ApiServer.java      # HTTP server + routes
├── webapp/
│   ├── index.html
│   ├── style.css
│   └── app.js
└── README.md
```
