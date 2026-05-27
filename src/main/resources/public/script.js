let socket;
let reconnectInterval = 1000;
const DELIMITER = "###";

window.appStartDate = new Date();
window.appInterval = 1000;
var connection_attempts = 0
var connected = false;

function connect() {
    console.log("Connecting to server");
    connection_attempts++
    $("#conn-status").text("Connecting... (attempts: " + connection_attempts + ")");
    socket = new WebSocket("ws://" + window.location.host + "/ws");


    socket.onopen = function () {
        connected = true;
        console.log("Connected to server");
        $("#conn-status").text("Connected (attempts: " + connection_attempts + ")");
        reconnectInterval = 1000; // reset on successful connection
        //Clear element #commands
        document.getElementById("commands").innerHTML = "";
    };

//If we are not connected, try to reconnect
//    if(connected == false){
//        scheduleReconnect();
//    }

    socket.onmessage = function (event) {
        //console.log("Received data:", event.data);
        onMessageData(event);
    };

    socket.onclose = function () {
        console.log("Disconnected. Attempting to reconnect...");
        connected = false;
        $("#conn-status").text("Disconnected. " + connection_attempts);
        scheduleReconnect();
    };

    socket.onerror = function (err) {
        console.error("WebSocket error:", err);
        connected = false;
        $("#conn-status").text("WebSocket error: " + err + " (attempts: " + connection_attempts + ")");
        socket.close();
    };
}

var totalReconnectAttempts = 0;

function scheduleReconnect() {
    totalReconnectAttempts++;
    if (totalReconnectAttempts > 100) {
        console.log("Failed to connect to server after 100 attempts");
        return;
    }
    setTimeout(() => {
        connect();
    }, reconnectInterval);
}

connect();

function onMessageData(event) {
    var key = event.data.split(DELIMITER)[0];
    var data = event.data.split(DELIMITER)[1];
    //console.log(key+ " : " + data);
    switch (key) {
        case "alert":
            alert(data);
            break;
        case "clear-charts":
            window.clearAllCharts();
            break;
        case "live-data":
            //Every line is prepended with history###, so we need raw data
            var lines = event.data.split("\n");
            for (var i = 0; i < lines.length; i++) {
                if (lines[i] == "") continue;
                var line = lines[i].split(DELIMITER)[1];
                var splits = line.split(",");

                var time = new Date(parseInt(splits[0]));
                var load = parseInt(splits[1]);
                var temp = parseInt(splits[2]);

                $('.status-load').html(Math.round(load));
                window.updateLoadChart(time, parseInt(load));

                $('.status-temp').html(Math.round(temp));
                window.updateTempChart(time, parseInt(temp));
            }
            break;
        //Detailed status
        case "load":
            document.getElementById('details-cpu-load').innerText = data.trim();
            break;
        //Read history
        case "history":
            //Every line is prepended with history###, so we need raw data
            var lines = event.data.split("\n");
            var tempChart = [];
            var loadChart = [];
            var times = [];

            var rerenderCounter = 0;
            var rerenderInterval = 1000;

            for (var i = 0; i < lines.length; i++) {
                if (lines[i] == "") continue;
                var line = lines[i].split(DELIMITER)[1];

                //console.log("Loading history: "+line);
                var splits = line.split(",");

                times.push(new Date(parseInt(splits[0])));
                loadChart.push(parseInt(splits[1]));
                tempChart.push(parseInt(splits[2]));

                rerenderCounter++;
                if (rerenderCounter > rerenderInterval) {
                    //Update chart so that were not here waiting
                    rerenderCounter = 0;
                    window.setTempHistoryChart(times, tempChart);
                    window.setLoadHistoryChart(times, loadChart);
                }
            }

            window.setTempHistoryChart(times, tempChart);
            window.setLoadHistoryChart(times, loadChart);
            break;
        case "history-records":
            //Every line is prepended with history###, so we need raw data
            var lines = data.split(",");
            const dropdownMenu = document.getElementById("dropdown-menu"); // your <ul> element
            dropdownMenu.innerHTML = ""; // Clear previous items if any

            lines.forEach((filename, index) => {
                const li = document.createElement("li");
                const a = document.createElement("a");
                a.className = "dropdown-item";
                a.href = "#"; // You can customize the link target

                // Add click event handler
                a.addEventListener("click", function (e) {
                    e.preventDefault(); // Prevent default link behavior
                    loadHistory(this.dataset.filename);
                });

                a.textContent = filename.trim()
                    .replace(".csv", "");
                if (index == 0) a.textContent += " (current)";

                a.dataset.filename = filename.trim(); // Optionally store raw filename
                li.appendChild(a);
                dropdownMenu.appendChild(li);
            });

            break;
        case "ram":
            document.getElementById('status-ram').innerText = data.trim();
            break;
        case "cpu-temp-history":
            document.getElementById('cpu-temp-history').innerText = data;
            break;
        /**
         * CPU Frequency
         */
        case "cpu-info":
            document.getElementById('cpu-info').innerText = data;
            break;
        /**
         * System
         */
        case "is-admin":
            document.getElementById('is-admin').innerText = data;
            break;
        case "server-name":
            document.getElementById('server-name').innerText = data;
            break;
        case "app-version":
            $('.app-version').text(data);
            break;
        case "time-since-awake":
            document.getElementById('time-since-awake').innerText = data;
            break;
        case "app-interval":
            window.appInterval = parseInt(data);
            console.log("App interval: " + window.appInterval);
            document.getElementById('app-interval').innerText = window.appInterval;
            break;
        default:
            console.log("No case found");
    }
}


function setInnerHTMLForAllClasses(className, newHTML) {
    const elements = document.querySelectorAll('.' + className);
    elements.forEach(element => {
        element.innerHTML = newHTML;
    });
}

function copyStatus() {
    const loadElement = $('.status-load').first().html();
    const tempElement = $('.status-temp').first().html();

    // Construct the text to be copied
    const textToCopy = `${tempElement}C \t\t ${loadElement}%`;

    // Use the modern Clipboard API directly
    navigator.clipboard.writeText(textToCopy)
        .then(() => {
            // Success feedback (optional, as it's often silent)
            // alert("Copied to clipboard!");
            console.log("Text successfully copied to clipboard.");
        })
        .catch(err => {
            console.error('Failed to copy text: ', err);

            // Provide more specific feedback if possible
            if (err.name === 'NotAllowedError') {
                alert("Permission to access clipboard denied. Please ensure your browser allows clipboard access for this site.");
            } else {
                alert("Failed to copy to clipboard. Please try again.");
            }
        });
}

function shutdown() {
    // Basic syntax
    let userConfirmed = confirm("Are you sure you want to shutdown?");
    if (userConfirmed) {
        socket.send("shutdown" + DELIMITER + "shutdown");
        alert("Shutdown confirmed! Proceeding...");
    }
}

function suspend() {
    let userConfirmed = confirm("Are you sure you want to suspend?");
    if (userConfirmed) {
        socket.send("shutdown" + DELIMITER + "suspend");
        alert("Suspend confirmed! Proceeding...");
    }
}

function loadHistory(filename) {
    console.log("Loading history for " + filename);
    socket.send("load-history" + DELIMITER + filename);
}

function setMaxFrequency(valueMHZ) {
    document.getElementById('frequency-policy').innerText = "";
    socket.send("set-max-frequency" + DELIMITER + valueMHZ);
}