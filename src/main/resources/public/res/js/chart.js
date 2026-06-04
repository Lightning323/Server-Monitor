import uPlot from 'uplot';

const ro = new ResizeObserver(entries => {
    for (let entry of entries) {
        // Access the instance we attached to the element
        const chart = entry.target._uPlotInstance;
        const newWidth = entry.contentRect.width;

        if (chart && newWidth > 0) {
            var height = chart._height;
            //Responsive height
            if (window.innerWidth < 900 && height > 100) {
                height *= 0.5;
            }
            // console.log("Resizing chart");
            chart.setSize({
                width: newWidth,
                height: height // Keep height constant
            });
        }
    }
});

function setupChart(container, {
    color = "red",
    label = "Value",
    yMin,
    yMax,
    maxPoints,
    height = 300,
    latestDataPoint = false
}) {
    // 1. Clear ONLY ONCE
    container.innerHTML = '';

    // 2. Create elements properly
    const wrapper = document.createElement("div");
    wrapper.style.position = "relative";
    wrapper.style.width = "100%";


    const chartTarget = document.createElement("div");
    chartTarget.classList.add("chart-target");
    wrapper.appendChild(chartTarget);
    container.appendChild(wrapper);

    // 3. Init uPlot
    const opts = {
        width: container.offsetWidth,
        height: height,
        series: [{label: "Time"}, {stroke: color, label: label, width: 2}],
        scales: {x: {time: true}, y: {min: yMin, max: yMax}},
        cursor: {
            drag: {
                x: true,
                y: false // Disabling Y-drag prevents the zoom-in interaction that causes the "broken" feeling
            }
        }
    };

    const chart = new uPlot(opts, [[], []], chartTarget);
    chart.maxPoints = maxPoints;


    const canvasLabel = document.createElement("span");
    canvasLabel.innerText = "";
    canvasLabel.classList.add("chart-label1");
    if (latestDataPoint) canvasLabel.style.cssText = " font-weight: bold;";
    wrapper.appendChild(canvasLabel);
    chart._canvasLabel = canvasLabel;
    chart._labelLatestDataPoint = latestDataPoint;

    const canvasLabel2 = document.createElement("span");
    canvasLabel2.innerText = "";
    canvasLabel2.classList.add("chart-label2");
    wrapper.appendChild(canvasLabel2);
    chart._canvasLabel2 = canvasLabel2;

    // 4. Store the reference directly on the element the Observer watches
    // We observe the 'chartTarget' div
    chartTarget._uPlotInstance = chart;
    chart._containerDiv = container;
    chart._height = height;
    ro.observe(chartTarget);
    return chart;
}

function clearData(chart) {
    chart.setData([[], []]);
    if (chart._canvasLabel) {
        chart._canvasLabel.innerText = "";
    }
}

function toNumber(value) {
    const match = String(value).match(/-?\d+(\.\d+)?/);
    // If a match is found, parse it as a float; otherwise return null or 0
    return match ? parseFloat(match[0]) : null;
}


function updateData(chart, dateMS, valueStr) {
    let [ts, vals] = chart.data;
    var date = parseFloat(dateMS / 1000);
    ts.push(date);
    vals.push(toNumber(valueStr));

    if (chart.maxPoints && ts.length > chart.maxPoints) {
        ts.shift();
        vals.shift();
    }
    if (chart._canvasLabel && chart._labelLatestDataPoint) {
        chart._canvasLabel.innerText = valueStr;
    }

    chart.setData([ts, vals]);
}

function updateDataBatch(chart, timestamps, values) {
    let [ts, vals] = chart.data;

    timestamps = timestamps.map(date => parseFloat(date.getTime() / 1000));
    values = values.map(value => toNumber(value));
    ts.push(...timestamps);
    vals.push(...values);

    if (chart.maxPoints && ts.length > chart.maxPoints) {
        const diff = ts.length - chart.maxPoints;
        ts.splice(0, diff);
        vals.splice(0, diff);
    }

    if (chart._canvasLabel && chart._labelLatestDataPoint) {
        chart._canvasLabel.innerText = values[values.length - 1];
    }

    chart.setData([ts, vals]);
    // setChartShown(chart, !isChartEmpty(chart));
}

function isChartEmpty(u) {
    // Check if u.data exists, if the X-axis array exists, and if it has length
    return !u.data || !u.data[0] || u.data[0].length === 0;
}

function setChartShown(chart, isVisible) {
    const container = chart._containerDiv; // Assuming container is the parent
    if (isVisible) {
        container.style.display = 'block';
        chart.setSize({
            width: container.offsetWidth,
            height: chart._height
        });
        chart.redraw();
    } else {
        container.style.display = 'none';
    }
}

function updateChartLabel(chart, newLabel) {
    if (newLabel === undefined || newLabel === null) return;
    chart._canvasLabel2.innerText = newLabel;
    if (!isChartEmpty(chart)) chart.redraw();
}

export {setupChart, updateData, updateDataBatch, clearData, updateChartLabel, isChartEmpty, setChartShown};