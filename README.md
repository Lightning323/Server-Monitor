# Server-Monitor 🌡️

> A lightweight, robust Java web application designed to monitor system temperatures on Linux servers and dispatch real-time alerts.

---

## 🚀 Overview
**Server-Monitor** provides peace of mind by keeping a close eye on your server's thermal health. Built specifically for Linux environments, it bridges the gap between hardware sensors and a user-friendly web interface. If your server starts running hot, you’ll be the first to know.

---

## 🛠 Prerequisites

Before installing, ensure your system meets these requirements:

* **OS:** Linux distribution (Ubuntu/Debian/CentOS/RHEL recommended).
* **Java:** JDK 17 or higher.
* **Hardware Monitoring:** `lm-sensors` must be installed and configured.

---

## 📥 Installation
Note that this guide is for Ubuntu/Debian. Other Linux distributions may require different commands.

### 1. Install & Configure lm-sensors
Server-Monitor relies on `lm-sensors` to pull data from your hardware.

```bash
# Install sensors
sudo apt update && sudo apt install lm-sensors -y

# Detect hardware sensors (Answer YES to all prompts)
sudo sensors-detect

# Verify it works
sensors