![icon-192.png](src/main/resources/public/res/icon-192.png)
# Server-Monitor

> A lightweight, robust Java web application designed to monitor system temperatures on Linux servers and dispatch real-time alerts.

---

## Overview
**Server-Monitor** provides peace of mind by keeping a close eye on your server's thermal health. Built specifically for Linux environments, it bridges the gap between hardware sensors and a user-friendly web interface. If your server starts running hot, you’ll be the first to know.

---

## Prerequisites

Before installing, ensure your system meets these requirements:

* **OS:** Linux distribution (Ubuntu/Debian/CentOS/RHEL recommended).
* **Java:** JDK 17 or higher.
* **Hardware Monitoring:** `lm-sensors` must be installed and configured.

---

## Installation & Setup
Note that this guide is for Ubuntu/Debian. Other Linux distributions may require different commands.

### 1. Install & Configure Dependencies
Server-Monitor relies on `lm-sensors` to pull data from your hardware.

```bash
# The taskbar widget runs as a python subprocess.
# If you want to enable it, you will need to install the following packages
sudo apt install python
sudo apt install gir1.2-appindicator3-0.1 python3-gi

# Install sensors
sudo apt update
sudo apt install lm-sensors -y

# Detect hardware sensors (You will be required to answer YES or NO to detect various sensors)
sudo sensors-detect

# Verify LM-Sensors works
sensors
```

### 2. Configuration
Edit your `config.json` file. Here is an example to get started:
```yml
{
    "SERVER_NAME": "Server",
    "WEBAPP_PORT": 3000,
    "DISCORD_WEBHOOK_URL": "...", <-- Discord webhook URL for notifications
    "TEMP_NOTIFICATION_MS": 30000, <-- how often to check temperature notifications
    "SENSORS_UPDATE_MS": 1000, <-- how often to update sensor data
    "METRICS_UPDATE_MS": 60000, <-- how often to print metrics for the webapp
    "SHOW_TEMP_TIER_LABELS": false, <-- show the tier name ("Idle", "Normal load", ...) next to the coloured dot
    "SENSORS": {
        "BAT0_acpi_0__ACPI_interface__curr1": {
          "alias": "BAT0 acpi 0 (ACPI interface) curr1",
          "unit": "A",
          "showTempTiers": false, <-- enable the temperature tier dot/colouring for this sensor
          "temperature_idle": 0, <-- upper bounds for each tier (°C); 0 disables the tier
          "temperature_normal_load": 0,
          "temperature_heavy_load": 0,
          "temperature_warning": 0,
          "temperature_throttling": 0
        }
    },
    "DATABASE_RECORD_WRITE_INTERVAL_MS": 300000, <-- how often to write to database
    "TASKBAR_SENSORS": [ <-- sensor readings to display in the taskbar (optional) (These will be displayed in the order they are listed)
      {
        "alias": "CPU", <-- the alias is optional
        "sensorId": "cpu__load"
      },
      {
        "alias": "GPU",
        "sensorId": "amdgpu_pci_0300__PCI_adapter__edge"
      }
    ]
}
```
