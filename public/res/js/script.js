import {  connect, PacketRegistry } from './connection.js';
import { setupChart, updateData } from './chart.js';
console.log("SCRIPT LOADED");

// --- Chart Setup ---
const canvas = document.getElementById("tempChart");
const chart = setupChart(canvas);

setInterval(() => {
    updateData(chart, new Date(), Math.random() * 100);
}, 1000);

// --- Registering your specific packets ---
PacketRegistry.register("SENSOR_UPDATE", (payload) => {
    $('.status-temp').text(Math.round(payload.temperature) + "°C");
    window.updateTempChart(new Date(payload.timestamp), payload.temperature);
});

PacketRegistry.register("ServerInfoPacket", (payload) => {
    console.log(payload);
    // document.getElementById('server-name').innerText = payload.name;
    // document.getElementById('app-version').innerText = payload.version;
});

// Start the connection
connect();