function setupChart(canvas) {
    const color = canvas.dataset.color;
    const label = canvas.dataset.label;

    let scales = {
        x: { type: "time", time: { unit: "second" } },
    };

    if (
        canvas.dataset.yMin != undefined ||
        canvas.dataset.yMax != undefined
    ) {
        const yMin = parseFloat(canvas.dataset.yMin);
        const yMax = parseFloat(canvas.dataset.yMax);
        scales.y = {
            min: yMin,
            max: yMax,
        };
    }

    const chart = new Chart(canvas, {
        type: "line",
        data: {
            datasets: [
                { label: label, data: [], borderColor: color, tension: 0.1 },
            ],
        },
        options: {
            scales: scales,
        },
    });

    const N = parseInt(canvas.dataset.n);
    if (N != undefined) chart.maxPoints = N;
    return chart;
}

function updateData(chart, date, value) {
    chart.data.datasets[0].data.push({ x: date, y: value });
    if (
        chart.maxPoints != undefined &&
        chart.data.datasets[0].data.length > chart.maxPoints
    ) {
        chart.data.datasets[0].data.shift();
    }

    chart.update("none");
}

export { setupChart, updateData };
