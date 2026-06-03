import {connect, PacketRegistry} from './connection.js';
import {setupChart, updateData, updateDataBatch, clearData} from './chart.js';

console.log("SCRIPT LOADED");

// --- Registering your specific packets ---
PacketRegistry.register("SENSOR_UPDATE", (payload) => {
    $('.status-temp').text(Math.round(payload.temperature) + "°C");
    window.updateTempChart(new Date(payload.timestamp), payload.temperature);
});

PacketRegistry.register("ServerInfoPacket", (payload) => {
    // console.log(payload);
    // console.log(payload.name);
    // console.log(payload.version);
    document.getElementById('server-name').innerText = payload.serverName;
    document.getElementById('app-version').innerText = payload.appVersion;
});

PacketRegistry.register("CpuInfoPacket", (payload) => {
    document.getElementById('cpu-info').innerText = payload.cpuInfo;
});

const canvas = document.getElementById("tempChart");
const statusTemp = document.getElementById("status-temp");
let chart;
let sensorKeys;
let selectedSensor;

function toNumber(value) {
    const match = String(value).match(/-?\d+(\.\d+)?/);
    // If a match is found, parse it as a float; otherwise return null or 0
    return match ? parseFloat(match[0]) : null;
}

function updateDropdown(items) {
    const dropdown = document.getElementById('sensor-dynamic-dropdown');
    const label = document.getElementById('dropdownMenuButton');
    dropdown.innerHTML = '';
    dropdown.innerHTML = items.map(item =>
        `<li><a class="dropdown-item" href="#">${item}</a></li>`)
        .join('');

    dropdown.querySelectorAll('.dropdown-item').forEach(item => {
        item.addEventListener('click', (event) => {
            selectedSensor = event.target.innerText;
            label.innerText = selectedSensor;
            console.log("User selected:", selectedSensor);
            if (!chart) {
                chart = setupChart(canvas,
                    {
                        color: "blue",
                        label: selectedSensor
                    });
            } else {
                chart.config.data.datasets[0].label = selectedSensor;
                clearData(chart);
                chart.update();
            }
        });
    });
}

const historyCanvas = document.getElementById("historyChart");
const historyChart = setupChart(historyCanvas,
    {
        color: "blue",
        label: "History"
    });
const dateSelector = document.getElementById("dateInput");
const historyProgress = document.getElementById("historyProgress");

dateSelector.addEventListener('change', () => {
    if (selectedSensor) {
        // 1. Get the raw date from the input
        const rawDate = new Date(dateSelector.value); // e.g., 2026-06-03
        const startDate = new Date(Date.UTC(
            rawDate.getUTCFullYear(),
            rawDate.getUTCMonth(),
            rawDate.getUTCDate(),
            0, 0, 0, 0
        ));

        const endDate = new Date(startDate);
        endDate.setUTCHours(23, 59, 59, 999);

        console.log("Start (UTC):", startDate); // 2026-06-03T00:00:00.000Z
        console.log("End (UTC):", endDate);     // 2026-06-03T23:59:59.999Z

        historyChart.config.data.datasets[0].label = selectedSensor;

        PacketRegistry.send("SensorHistoryRequestPacket", {
            sensor: selectedSensor,
            //Convert to timestamps
            startDate: startDate.getTime(),
            endDate: endDate.getTime()
        });

        historyProgress.style.display = "flex";
    } else {
        alert("Please select a sensor");
    }
});

PacketRegistry.register("SensorDumpPacket", (payload) => {
    let timestamp = new Date(payload.dump.timestamp);
    let sensors = payload.dump.sensors;

    let newSensorKeys = Object.keys(sensors);
    newSensorKeys.sort();
    if (JSON.stringify(newSensorKeys) !== JSON.stringify(sensorKeys)) {
        console.log(newSensorKeys);
        sensorKeys = newSensorKeys;
        updateDropdown(sensorKeys);
    }

    if (selectedSensor && chart) {
        statusTemp.innerText = sensors[selectedSensor];
        updateData(chart, timestamp, toNumber(sensors[selectedSensor]));
        historyProgress.style.display = "none";
    }
});

PacketRegistry.register("SensorHistoryPacket", (payload) => {
    console.log("Sensor history",payload);
    clearData(historyChart);
    let timestamps = []
    let values = []
    payload.history.forEach(entry => {
        timestamps.push(new Date(entry.t));
        values.push(toNumber(entry.v));
    });
    updateDataBatch(historyChart, timestamps, values);
});


// Start the connection
connect();