import {connect, PacketRegistry} from './connection.js';
import {
    setupChart,
    updateData,
    updateDataBatch,
    clearData,
    updateChartLabel,
    isChartEmpty,
    setChartShown
} from './chart.js';

console.log("SCRIPT LOADED");

// --- Registering your specific packets ---
PacketRegistry.register("SENSOR_UPDATE", (payload) => {
    $('.status-temp').text(Math.round(payload.temperature) + "°C");
    window.updateTempChart(new Date(payload.timestamp), payload.temperature);
});

let sensorAliases = {};

function aliasedName(sensorID) {
    return sensorAliases[sensorID] || sensorID;
}

PacketRegistry.register("SensorAliasesPacket", (payload) => {
    sensorAliases = payload.aliases;
    console.log("sensorAliases", sensorAliases);
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


let chart1 = setupChart(document.getElementById("sensorChart1"),
    {
        color: "blue", maxPoints: 200, latestDataPoint: true
    });
let chart2 = setupChart(document.getElementById("sensorChart2"),
    {
        color: "red", maxPoints: 200, latestDataPoint: true
    });
let chart3 = setupChart(document.getElementById("sensorChart3"),
    {
        color: "green", maxPoints: 200, latestDataPoint: true
    });
let chart4 = setupChart(document.getElementById("sensorChart4"),
    {
        color: "purple", maxPoints: 200, latestDataPoint: true
    });

setChartShown(chart1, false);
setChartShown(chart2, false);
setChartShown(chart3, false);
setChartShown(chart4, false);

let historyChart1 = setupChart(document.getElementById("historyChart1"),
    {
        color: "blue", label: "History", latestDataPoint: false
    });
let historyChart2 = setupChart(document.getElementById("historyChart2"),
    {
        color: "red", label: "History", latestDataPoint: false
    });
let historyChart3 = setupChart(document.getElementById("historyChart3"),
    {
        color: "green", label: "History", latestDataPoint: false
    });
let historyChart4 = setupChart(document.getElementById("historyChart4"),
    {
        color: "purple", label: "History", latestDataPoint: false
    });

setChartShown(historyChart1, false);
setChartShown(historyChart2, false);
setChartShown(historyChart3, false);
setChartShown(historyChart4, false);

let sensorKeys;

function updateDropdown(items, dropdownElement, chart, historyChart) {
    // 1. Use querySelector instead of .getClass()
    const dropdown = dropdownElement.querySelector('.dropdown-menu');
    const label = dropdownElement.querySelector('.dropdown-toggle');
    dropdown.innerHTML = '';

    dropdown.innerHTML = items.map(item =>
        `<li><a class="dropdown-item" href="#" data-sensor="${item}">${aliasedName(item)}</a></li>`)
        .join('');
    dropdown.innerHTML += `<li><a class="dropdown-item" href="#">None</a></li>`;

    dropdown.querySelectorAll('.dropdown-item').forEach(item => {

        item.addEventListener('click', (event) => {
            var key = event.target.getAttribute("data-sensor");
            console.log("User selected:", key);
            clearData(chart);
            clearData(historyChart);
            if (key === "None") {
                chart._selectedSensor = undefined;
                historyChart._selectedSensor = undefined;
                label.innerText = "None";
                return;
            }
            chart._selectedSensor = key;
            historyChart._selectedSensor = key;
            label.innerText = event.target.innerText;
            updateChartLabel(chart, 1, chart._selectedSensor);
            updateChartLabel(historyChart, 1, chart._selectedSensor);
        });

    });
}

//History seleciton
const dateSelector = document.getElementById("dateInput");
const today = new Date();
const year = today.getFullYear();
const month = String(today.getMonth() + 1).padStart(2, '0');
const day = String(today.getDate()).padStart(2, '0');
dateSelector.value = `${year}-${month}-${day}`;

const historyReloadButton = document.getElementById("historyReloadButton");
const historyProgress = document.getElementById("historyProgress");
const historyProgressText = document.getElementById("historyProgressText");

dateSelector.addEventListener('change', () => {
    requestHistory();
});

historyReloadButton.addEventListener('click', () => {
    requestHistory();
});

document.getElementById("history-tab").addEventListener('click', () => {
    if (isChartEmpty(historyChart1) && isChartEmpty(historyChart2) && isChartEmpty(historyChart3) && isChartEmpty(historyChart4)) {
        requestHistory();
    }
});

function requestHistory() {
    setChartShown(historyChart1, !isChartEmpty(historyChart1));
    setChartShown(historyChart2, !isChartEmpty(historyChart2));
    setChartShown(historyChart3, !isChartEmpty(historyChart3));
    setChartShown(historyChart4, !isChartEmpty(historyChart4));

    // 1. Get the raw date from the input
    const dateString = dateSelector.value;
    const [year, month, day] = dateString.split('-').map(Number);

// Use local date constructor (Note: Month is 0-indexed in JS)
    const targetDate = new Date(year, month - 1, day);

    const now = new Date();

// Now the rest of your logic will work correctly in local time
    const isToday = targetDate.getFullYear() === now.getFullYear() &&
        targetDate.getMonth() === now.getMonth() &&
        targetDate.getDate() === now.getDate();

    let endDate, startDate;

    if (isToday) {
        console.log("Loading history for Today");
        endDate = new Date();
        // Start date is 24 hours prior
        startDate = new Date(endDate.getTime() - (24 * 60 * 60 * 1000));
    } else {
        console.log("Loading history for "+targetDate.toLocaleDateString());
        // End date is 11:59:59.999 PM of the selected day (Local)
        endDate = new Date(targetDate);
        endDate.setHours(23, 59, 59, 999);

        // Start date is 24 hours before that local midnight
        startDate = new Date(endDate.getTime() - (24 * 60 * 60 * 1000));
    }

    const options = {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: true
    };

    console.log("Start:", startDate.toLocaleString(undefined, options));
    console.log("End:", endDate.toLocaleString(undefined, options));

    var selectedSensors = []
    sendHistoryRequest(historyChart1, startDate, endDate, selectedSensors);
    sendHistoryRequest(historyChart2, startDate, endDate, selectedSensors);
    sendHistoryRequest(historyChart3, startDate, endDate, selectedSensors);
    sendHistoryRequest(historyChart4, startDate, endDate, selectedSensors);

    if (selectedSensors.length > 0) {
        historyProgress.style.display = "flex";
    } else {
        alert("No sensors selected");
    }
}

function sendHistoryRequest(historyChart, startDate, endDate, selectedSensors) {
    if (historyChart._selectedSensor && !selectedSensors.includes(historyChart._selectedSensor)) {
        selectedSensors.push(historyChart._selectedSensor);
        PacketRegistry.send("SensorHistoryRequestPacket", {
            sensor: historyChart._selectedSensor,
            startDate: startDate.getTime(),
            endDate: endDate.getTime()
        });
    } else {
        setChartShown(historyChart, false);
    }
}

PacketRegistry.register("SensorDumpPacket", (payload) => {
    // console.log(payload);
    let timestamp = new Date(payload.dump.timestamp);
    let sensors = payload.dump.sensors;

    let newSensorKeys = Object.keys(sensors);
    newSensorKeys.sort();
    if (JSON.stringify(newSensorKeys) !== JSON.stringify(sensorKeys)) {
        console.log(newSensorKeys);
        sensorKeys = newSensorKeys;
        updateDropdown(sensorKeys, document.getElementById('sensor-dropdown-1'), chart1, historyChart1);
        updateDropdown(sensorKeys, document.getElementById('sensor-dropdown-2'), chart2, historyChart2);
        updateDropdown(sensorKeys, document.getElementById('sensor-dropdown-3'), chart3, historyChart3);
        updateDropdown(sensorKeys, document.getElementById('sensor-dropdown-4'), chart4, historyChart4);
    }
    updateLiveChart(chart1, timestamp, sensors);
    updateLiveChart(chart2, timestamp, sensors);
    updateLiveChart(chart3, timestamp, sensors);
    updateLiveChart(chart4, timestamp, sensors);
});

function updateLiveChart(chart, timestamp, sensors) {
    setChartShown(chart, !isChartEmpty(chart));
    if (chart._selectedSensor) {
        updateData(chart, timestamp, sensors[chart._selectedSensor]);
    }
}

//We recieve history here
PacketRegistry.register("SensorHistoryPacket", (payload) => {
    var sensor = payload.sensor;
    if (historyChart2._selectedSensor === sensor) updateHistoryChart(historyChart1, payload);
    else if (historyChart3._selectedSensor === sensor) updateHistoryChart(historyChart2, payload);
    else if (historyChart4._selectedSensor === sensor) updateHistoryChart(historyChart3, payload);
    else updateHistoryChart(historyChart4, payload);
});

function updateHistoryChart(chartElement, payload) {
    setChartShown(chartElement, true);
    var sensor = payload.sensor;
    var packetNumber = payload.packetNumber;

    historyProgressText.innerText = "Loading history of " + sensor + " (chunk " + packetNumber + ")";
    console.log(historyProgressText.innerText);
    if (packetNumber == 0) {
        console.log("Clearing history");
        clearData(chartElement);
    }
    let timestamps = []
    let values = []
    payload.history.forEach(entry => {
        timestamps.push(new Date(entry.t));
        values.push(entry.v);
    });
    updateChartLabel(chartElement, 1, aliasedName(sensor));
    updateDataBatch(chartElement, timestamps, values);
    if (payload.finalPacket) {
        historyProgress.style.display = "none";
        if (isChartEmpty(chartElement)) {
            setChartShown(chartElement, false);
        }
    }
}


// Start the connection
connect();