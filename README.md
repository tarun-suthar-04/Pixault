# Pixault

Pixault is a JavaFX desktop application for secure image steganography.
It lets users:
- Register and log in with MySQL-backed authentication
- Encrypt text or file data using AES
- Hide encrypted data inside PNG/BMP images (LSB steganography)
- Extract and decrypt hidden data from stego-images
- Log user actions in a database

## Tech Stack

- Java (JDK 24 recommended, based on project settings)
- JavaFX
- MySQL
- JDBC (MySQL Connector/J)

## Project Structure

```text
pixault/
  src/com/pixault/
    Pixault.java
    app/PixaultApp.java
    crypto/CryptoEngine.java
    db/DatabaseManager.java
    stego/StegoEngine.java
  lib/
    javafx.controls.jar
    javafx.fxml.jar
    javafx.graphics.jar
    mysql-connector-j-9.4.0.jar
```

## Prerequisites

Install the following on your machine:
- JDK 24 (or a compatible Java version used by your environment)
- JavaFX SDK (matching your JDK version)
- MySQL Server 8+

## 1. Clone the Repository

```powershell
git clone https://github.com/tarun-suthar-04/Pixault.git
cd Pixault
```

## 2. Set Up the MySQL Database

Run this SQL in MySQL:

```sql
CREATE DATABASE IF NOT EXISTS pixault_db;
USE pixault_db;

CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS activity_logs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    action VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

## 3. Configure Database Credentials

Open `src/com/pixault/db/DatabaseManager.java` and update:

```java
private static final String URL = "jdbc:mysql://localhost:3306/pixault_db";
private static final String USER = "YOUR_MYSQL_USERNAME";
private static final String PASS = "YOUR_MYSQL_PASSWORD";
```

## 4. Configure JavaFX Path

You need the JavaFX `lib` folder path on your machine.

Example path on Windows:

```text
C:\Program Files\Java\javafx\lib
```

If your path is different, replace it in commands below.

## 5. Build and Run (Terminal - PowerShell)

From the project root:

```powershell
New-Item -ItemType Directory -Force -Path bin | Out-Null

javac --module-path "C:\Program Files\Java\javafx\lib" `
  --add-modules javafx.controls,javafx.fxml `
  -cp "lib/*" `
  -d bin `
  (Get-ChildItem -Recurse -Path src -Filter *.java | ForEach-Object { $_.FullName })

java --module-path "C:\Program Files\Java\javafx\lib" `
  --add-modules javafx.controls,javafx.fxml `
  -cp "bin;lib/*" `
  com.pixault.Pixault
```

## 6. Run in VS Code (Alternative)

If you use VS Code:
- Install Java extension pack
- Ensure `.vscode/settings.json` Java runtime path matches your JDK
- Ensure JavaFX module path in `java.debug.settings.vmArgs` is valid
- Launch `Pixault (Main)` from Run and Debug

## How to Use

1. Start the app
2. Register a new account
3. Log in
4. In `Hide Data` tab:
   - Select a PNG/BMP cover image
   - Enter secret text or choose a file
   - Enter encryption password
   - Save generated stego-image
5. In `Extract Data` tab:
   - Select stego-image
   - Enter the same password
   - Extract and view text or save binary output

## Troubleshooting

- `Cannot connect to MySQL`:
  - Verify MySQL server is running
  - Check DB name, username, and password in `DatabaseManager.java`
  - Confirm `pixault_db` and required tables exist

- `JavaFX runtime components are missing`:
  - Confirm JavaFX SDK is installed
  - Check `--module-path` points to JavaFX `lib`
  - Ensure `--add-modules javafx.controls,javafx.fxml` is included

- `Image too small to hold this data`:
  - Use a larger cover image
  - Reduce message/file size

## Notes

- Use PNG/BMP for embedding and extraction to avoid lossy compression artifacts.
- Keep your encryption password safe; wrong password will fail decryption.
