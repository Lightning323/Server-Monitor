function setupChart(canvas,
                    {color = "red", label = "Label", yMin, yMax, maxPoints = 200}
) {

    let scales = {
        x: {type: "time", time: {unit: "second"}},
    };
    if (
        canvas.dataset.yMin != undefined ||
        canvas.dataset.yMax != undefined
    ) {
        scales.y = {
            min: yMin,
            max: yMax,
        };
    }
    const chart = new Chart(canvas, {
        type: "line",
        data: {
            datasets: [
                {label: label, data: [], borderColor: color, tension: 0.1},
            ],
        },
        options: {
            scales: scales,
        },
    });
    if (maxPoints != undefined) chart.maxPoints = maxPoints;
    return chart;
}

function clearData(chart) {
    chart.data.datasets[0].data = [];
    chart.update("none");
}

function updateData(chart, date, value) {
    chart.data.datasets[0].data.push({x: date, y: value});
    if (
        chart.maxPoints != undefined &&
        chart.data.datasets[0].data.length > chart.maxPoints
    ) {
        chart.data.datasets[0].data.shift();
    }

    chart.update("none");
}

function updateDataBatch(chart, timestamps, values) {
    // 1. Ensure the lists are the same length to prevent index errors
    const length = Math.min(timestamps.length, values.length);

    for (let i = 0; i < length; i++) {
        chart.data.datasets[0].data.push({ x: timestamps[i], y: values[i] });
    }

    // 2. Manage the size limit (shifting off old data if we exceeded capacity)
    if (chart.maxPoints !== undefined) {
        const totalPoints = chart.data.datasets[0].data.length;
        if (totalPoints > chart.maxPoints) {
            // Remove only the number of items needed to get back to maxPoints
            const diff = totalPoints - chart.maxPoints;
            chart.data.datasets[0].data.splice(0, diff);
        }
    }

    // 3. Trigger one single re-render
    chart.update("none");
}



export {setupChart, updateData, updateDataBatch, clearData};
