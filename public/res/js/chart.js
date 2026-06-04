import uPlot from 'uplot';

const ro = new ResizeObserver(entries => {
    for (let entry of entries) {
        // Access the instance we attached to the element
        const chart = entry.target._uPlotInstance;
        const newWidth = entry.contentRect.width;

        if (chart && newWidth > 0) {
            // console.log("Resizing chart");
            chart.setSize({
                width: newWidth,
                height: chart._height // Keep height constant
            });
        }
    }
});

function setupChart(container, {
    color = "red",
    label = "Label",
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

    const btn = document.createElement("span");
    btn.innerText = "Double-Click to Reset Zoom";
    btn.style.cssText = "position: absolute; top: 10px; right: 10px; z-index: 100; opacity: 0.5; user-select: none; pointer-events: none;";
    wrapper.appendChild(btn);


    const chartTarget = document.createElement("div");
    chartTarget.style.paddingTop = "30px";
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
    canvasLabel.style.cssText = "position: absolute; top: 10px; left: 40px; z-index: 100; font-size: 1.2em; user-select: none; pointer-events: none;";
    wrapper.appendChild(canvasLabel);
    chart._canvasLabel = canvasLabel;
    chart._labelLatestDataPoint = latestDataPoint;

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

function updateChartLabel(chart, seriesIndex, newLabel) {
    chart.series[seriesIndex].label = newLabel;
    if (chart._canvasLabel) {
        chart._canvasLabel.innerText = newLabel;
    }
    chart.redraw();
}

export {setupChart, updateData, updateDataBatch, clearData, updateChartLabel, isChartEmpty, setChartShown};