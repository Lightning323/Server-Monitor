window.updateTempChart;
window.updateLoadChart;
window.setLoadHistoryChart;
window.setTempHistoryChart;
window.clearAllCharts;

 window.onload = function () {

var dataLength = 400; // number of dataPoints visible at any point

//Temprature chart
    var dps = []; // dataPoints
    var tempChart = new CanvasJS.Chart("tempChart", {
        title :{
            font: "20px Arial",
            text: "CPU Temperature"
        },
        data: [{
            color: "darkred",
            type: "line",
            dataPoints: dps
        }]
    });
    tempChart.render();
        window.updateTempChart = function (date, yVal) {
            dps.push({x: date, y: yVal});
            if (dps.length > dataLength) {
                dps.shift();
            }
            tempChart.render();
        };

//Load chart
    var loadDps = [];
    var loadChart = new CanvasJS.Chart("loadChart", {
            title :{
                font: "20px Arial",
                text: "CPU Load"
            },
            data: [{
                type: "line",
                dataPoints: loadDps
            }]
        });
    loadChart.render();
    window.updateLoadChart = function (date, yVal) {
        loadDps.push({x: date, y: yVal});
        if (loadDps.length > dataLength) {
            loadDps.shift();
        }
        loadChart.render();
    };





    //Temprature history chart
    var tempHistoryDps = [];
    var tempHistoryChart = new CanvasJS.Chart("tempChart_history", {
            zoomEnabled: true,
            animationEnabled: true,
            title :{
                text: "CPU Temperature History"
            },
           data: [{
               type: "line",
               color: "darkred",
               dataPoints: tempHistoryDps
           }]
        });
    tempHistoryChart.render();
    window.setTempHistoryChart = function (times, list) {
           tempHistoryDps.length = 0;
           for(var i = 0; i < list.length; i++) {
               tempHistoryDps.push({
               x: times[i], //new Date(window.appStartDate.getTime() + (i*window.appInterval))
               y: list[i]
               });
           }
           tempHistoryChart.render();
       };

//Load history chart
    var loadHistoryDps = [];
    var loadHistoryChart = new CanvasJS.Chart("loadChart_history", {
            zoomEnabled: true,
            animationEnabled: true,
            title :{
                text: "CPU Load History"
            },
            data: [{
                type: "line",
                dataPoints: loadHistoryDps
            }]
        });
    loadHistoryChart.render();
    window.setLoadHistoryChart = function (times, list) {
        loadHistoryDps.length = 0;
        for(var i = 0; i < list.length; i++) {
            loadHistoryDps.push({
            x: times[i],//new Date(window.appStartDate.getTime() + (i*window.appInterval))
            y: list[i]
            });
        }
        loadHistoryChart.render();
    };

    //Clear all charts
    window.clearAllCharts = function () {
        dps.length = 0;
        loadDps.length = 0;
        tempHistoryDps.length = 0;
        loadHistoryDps.length = 0;
        tempChart.render();
        loadChart.render();
        tempHistoryChart.render();
        loadHistoryChart.render();
    };
}