# Kronos — Smart Timetable Generator

> Conflict-free class schedules, generated in seconds.

A full-stack web application built with **Java Spring Boot** that automatically generates weekly timetables using a **Backtracking Algorithm**. No gaps between lectures, lunch always protected, zero teacher or room conflicts.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2 |
| ORM | Spring Data JPA / Hibernate |
| Database | MySQL 8.0 |
| Frontend | HTML, CSS, Vanilla JS |
| Build | Maven |
| Deployment | Render |

---

## Algorithm

The core of this project is a **Backtracking + Constraint Satisfaction** algorithm in `TimetableService.java`.

For each lecture that needs scheduling, it tries every available time slot and checks:

1. Teacher is not already booked at that slot
2. Room is not already booked at that slot
3. Division doesn't already have a class at that slot
4. New slot is consecutive with existing slots on that day *(no gaps)*
5. Lunch break (1–2 PM) is never touched
6. Teacher's weekly hour limit is not exceeded
7. Lab subjects only go in lab rooms

If any check fails → try next slot. If no slot works → backtrack to previous lecture and try a different assignment.

---

## Project Structure

```
src/main/java/com/vit/timetable/
│
├── TimetableApplication.java       ← Entry point
│
├── model/                          ← JPA Entities (mapped to DB tables)
│   ├── Teacher.java
│   ├── Subject.java
│   ├── Room.java
│   ├── TimeSlot.java
│   └── TimetableEntry.java
│
├── repository/                     ← Spring Data JPA (auto-generated SQL)
│   ├── TeacherRepository.java
│   ├── SubjectRepository.java
│   ├── RoomRepository.java
│   ├── TimeSlotRepository.java
│   └── TimetableEntryRepository.java
│
├── service/
│   ├── TimetableService.java       ← Backtracking algorithm
│   └── DataInitService.java        ← Seeds DB with default data on startup
│
├── controller/                     ← REST API endpoints
│   ├── TeacherController.java
│   ├── SubjectController.java
│   ├── RoomController.java
│   └── TimetableController.java
│
└── config/
    └── WebConfig.java              ← CORS + static file serving

src/main/resources/
├── static/index.html               ← Full frontend (single file)
└── application.properties          ← DB + server config
```

---

## REST API Reference

### Subjects
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/subjects` | Get all subjects |
| `POST` | `/api/subjects` | Add a subject |
| `DELETE` | `/api/subjects/{id}` | Delete a subject |

### Teachers
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/teachers` | Get all teachers |
| `POST` | `/api/teachers` | Add a teacher |
| `DELETE` | `/api/teachers/{id}` | Delete a teacher |

### Rooms
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/rooms` | Get all rooms |
| `POST` | `/api/rooms` | Add a room |
| `DELETE` | `/api/rooms/{id}` | Delete a room |

### Timetable
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/timetable/generate?division=A` | Run the algorithm |
| `GET` | `/api/timetable?division=A` | Fetch generated timetable |
| `DELETE` | `/api/timetable?division=A` | Clear timetable |

---

## Running Locally

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.0+

### Steps

**1. Clone the repo**
```bash
git clone https://github.com/Punit1787/kronos.git
cd kronos
```

**2. Configure the database**

Open `src/main/resources/application.properties` and update:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/vit_timetable?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

**3. Run**
```bash
mvn spring-boot:run
```

**4. Open in browser**
```
http://localhost:8080
```

Default data (subjects, teachers, rooms, time slots) is seeded automatically on first run.

---

## Deploying to Render

This repo includes a `render.yaml` blueprint. To deploy:

1. Push this repo to GitHub
2. Go to [render.com](https://render.com) → New → **Blueprint**
3. Connect your GitHub repo
4. Render auto-creates the backend, frontend, and database
5. Done

---

## Constraints Enforced

| Constraint | How |
|---|---|
| No teacher double-booking | Checked per slot before assignment |
| No room double-booking | Checked per slot before assignment |
| No division overlap | Checked per slot before assignment |
| No gaps between lectures | `isConsecutiveOrFirstOnDay()` in TimetableService |
| Lunch break protected | `isLunch=true` slots filtered out entirely |
| Lab room matching | `subject.isLab == room.isLab` required |
| Teacher hour cap | Weekly count tracked during backtracking |

---

## License

MIT — free to use, modify, and submit.