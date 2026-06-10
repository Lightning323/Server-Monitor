import uPlot from 'uplot';

const gapMarkersPlugin = {
    hooks: {
        draw: [
            u => {
                if (!u._breaks?.length)
                    return;

                const { ctx } = u;

                ctx.save();
                ctx.strokeStyle = "#888";
                ctx.setLineDash([4, 4]);

                for (const idx of u._breaks) {

                    const x = Math.round(u.valToPos(idx, "x", true));

                    ctx.beginPath();
                    ctx.moveTo(x, u.bbox.top);
                    ctx.lineTo(x, u.bbox.top + u.bbox.height);
                    ctx.stroke();
                }

                ctx.restore();
            }
        ]
    }
};

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
        scales: {
            x: {},
            y: { min: yMin, max: yMax }
        },
        plugins: [gapMarkersPlugin],
        axes: [{
            values: (u, splits) =>
                splits.map(i => {
                    const ts = u._realTimes[Math.round(i)];
                    if (ts == null) return "";

                    return new Date(ts * 1000).toLocaleTimeString();
                })
        }],
        series: [
            {
                label: "Time",
                value: (u, xVal) => {
                    const idx = Math.round(xVal);

                    const ts = u._realTimes?.[idx];
                    if (ts == null)
                        return "";

                    return new Date(ts * 1000).toLocaleString();
                }
            },
            {
                stroke: color,
                label: label,
                width: 2
            }
        ],
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


// function updateData(chart, dateMS, valueStr) {
//     let [ts, vals] = chart.data;
//     var date = parseFloat(dateMS / 1000);
//     ts.push(date);
//     vals.push(toNumber(valueStr));
//
//     if (chart.maxPoints && ts.length > chart.maxPoints) {
//         ts.shift();
//         vals.shift();
//     }
//     if (chart._canvasLabel && chart._labelLatestDataPoint) {
//         chart._canvasLabel.innerText = valueStr;
//     }
//
//     chart.setData([ts, vals]);
// }

function updateDataBatch(chart, timestamps, values) {
    // console.log("OUTPUT____",timestamps);
    let [indices, vals] = chart.data;
    if (chart._canvasLabel && chart._labelLatestDataPoint) {
        chart._canvasLabel.innerText = values[values.length - 1];
    }
    // Initialize timestamp storage if needed
    if (!chart._realTimes) {
        chart._realTimes = [];
    }

    // Convert timestamps to unix seconds and values to numbers
    const newTimes = timestamps.map(date => date.getTime() / 1000);
    const newVals = values.map(value => toNumber(value));

    //Add gaps property
    const GAP_THRESHOLD = 60 * 60; // 1 hour
    if (!chart._breaks)
        chart._breaks = [];
    for (let i = 1; i < newTimes.length; i++) {
        if (newTimes[i] - newTimes[i - 1] > GAP_THRESHOLD) {
            chart._breaks.push(chart._realTimes.length + i);
            // chart._breaks.push(300);
        }
    }

    // Append real timestamps
    chart._realTimes.push(...newTimes);

    // Append values
    vals.push(...newVals);

    // Rebuild index axis (0,1,2,3...)
    const totalPoints = vals.length;
    indices = Array.from({ length: totalPoints }, (_, i) => i);

    // Trim if maxPoints exceeded
    if (chart.maxPoints && totalPoints > chart.maxPoints) {
        const diff = totalPoints - chart.maxPoints;

        chart._realTimes.splice(0, diff);
        vals.splice(0, diff);

        // Rebuild indices after trimming
        indices = Array.from({ length: vals.length }, (_, i) => i);
    }

    chart.setData([indices, vals]);

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

export {setupChart, updateDataBatch, clearData, updateChartLabel, isChartEmpty, setChartShown};