import uPlot from 'uplot';

const ro = new ResizeObserver(entries => {
    for (let entry of entries) {
        // Access the instance we attached to the element
        const chart = entry.target._uPlotInstance;
        const newWidth = entry.contentRect.width;

        if (chart && newWidth > 0) {
            console.log("Resizing chart");
            chart.setSize({
                width: newWidth,
                height: 400 // Keep height constant
            });
        }
    }
});

function setupChart(container, {color = "red", label = "Label", yMin, yMax, maxPoints}) {
    // 1. Clear ONLY ONCE
    container.innerHTML = '';

    // 2. Create elements properly
    const wrapper = document.createElement("div");
    wrapper.style.position = "relative";
    wrapper.style.width = "100%";

    const btn = document.createElement("span");
    btn.innerText = "Double-Click to Reset Zoom";
    btn.style.cssText = "position: absolute; top: 10px; right: 10px; z-index: 100; opacity: 0.5; user-select: none; pointer-events: none;";

    const chartTarget = document.createElement("div");

    wrapper.appendChild(btn);
    wrapper.appendChild(chartTarget);
    container.appendChild(wrapper);

    // 3. Init uPlot
    const opts = {
        width: container.offsetWidth,
        height: 400,
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

    // 4. Store the reference directly on the element the Observer watches
    // We observe the 'chartTarget' div
    chartTarget._uPlotInstance = chart;

    // btn.onclick = () => {
    //     console.log("X-Scale min/max:", chart.scales.x.min, chart.scales.x.max);
    //     console.log("Data range:", chart.data[0][0], chart.data[0][chart.data[0].length - 1]);
    //     const minX = chart.data[0][0];
    //     const maxX = chart.data[0][chart.data[0].length - 1];
    //     chart.setScale('x', {min: 0, max: 1000});
    //     chart.setScale('y', {min: null, max: null});
    //     chart.redraw();
    // };

    // 5. Observe the div that actually holds the canvas
    ro.observe(chartTarget);
    return chart;
}

function clearData(chart) {
    chart.setData([[], []]);
}

function updateData(chart, dateMS, value) {
    let [ts, vals] = chart.data;
    var date = parseFloat(dateMS / 1000);
    ts.push(date);
    vals.push(value);

    if (chart.maxPoints && ts.length > chart.maxPoints) {
        ts.shift();
        vals.shift();
    }

    chart.setData([ts, vals]);
}

function updateDataBatch(chart, timestamps, values) {
    let [ts, vals] = chart.data;

    timestamps = timestamps.map(date => parseFloat(date.getTime() / 1000));
    ts.push(...timestamps);
    vals.push(...values);

    if (chart.maxPoints && ts.length > chart.maxPoints) {
        const diff = ts.length - chart.maxPoints;
        ts.splice(0, diff);
        vals.splice(0, diff);
    }

    chart.setData([ts, vals]);
}

function updateChartLabel(u, seriesIndex, newLabel) {
    // 1. Update the internal series definition
    u.series[seriesIndex].label = newLabel;

    // 2. Refresh the chart so the legend/tooltip updates
    u.redraw();
}

export {setupChart, updateData, updateDataBatch, clearData, updateChartLabel};