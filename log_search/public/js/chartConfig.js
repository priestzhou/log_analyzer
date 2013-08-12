var ChartConfig = {};
ChartConfig.getConfig = function(configName) {
    var newObject = $.extend(true, {}, this[configName]);
    return newObject;
};
ChartConfig.area = {
    chart: {
        type: 'area'
        , zoomType: 'x',
    }
	, credits:{
		enabled: false
	}
    , title: { text:'Dashboard Charts' }
    , subtitle: { text:'' }
    , legend: {
        layout: 'vertical',
        align: 'left',
        verticalAlign: 'top',
        x: 100,
        y: 0,
        floating: true,
        borderWidth: 1,
        backgroundColor: '#FFFFFF'
    }
    , xAxis: {
        title: {
            text: ''
        }
    }
    , yAxis: { 
        min: 1,
        title: {
            text: ''
        }
    }
    , plotOptions: {
        area: {
            fillColor: {
                linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1},
                stops: []
            },
            lineWidth: 1,
            marker: {
                enabled: false
            },
            shadow: false,
            states: {
                hover: {
                    lineWidth: 1
                }
            },
        }
    }
    , tooltip: {
        pointFormat: '{point.y:,.0f}'
    }
    , series: []
};
ChartConfig.column = {
    chart: {
        type: 'column',
        height: '100'
    }
	, credits:{
		enabled: false
	}
    , title: { text:'Dashboard Charts' }
    , subtitle: { text:'' }
    , legend: {
        layout: 'vertical',
        align: 'left',
        verticalAlign: 'top',
        x: 100,
        y: 0,
        floating: true,
        borderWidth: 1,
        backgroundColor: '#FFFFFF'
    }
    , xAxis: { 
        categories: [],
        title: {
            text: ''
        }
    }
    , yAxis: {
        min: 0,
        title: {
            text: ''
        }
    }
    , tooltip: {
        pointFormat: '{point.y:,.0f}'
    }
    , plotOptions: {
        column: {
            pointPadding: 0.2
        }
    }
    , series: []
};
ChartConfig.pie = {
    chart: {
        plotBackgroundColor: null,
        plotBorderWidth: null,
        plotShadow: false
    },
    title: {
        text: ''
    },
    tooltip: {
        formatter: function() {
            return this.point.name + ":<b>"+this.point.percentage.toFixed(2)+"%</b>"
        },
    	percentageDecimals: 2
    },
    plotOptions: {
        pie: {
            allowPointSelect: true,
            cursor: 'pointer',
            dataLabels: {
                enabled: true,
                color: '#000000',
                connectorColor: '#000000',
                formatter: function() {
                    return '<b>'+ this.point.name +'</b>: '+ this.percentage.toFixed(2) +' %';
                }
            }
        }
    },
    series: []
};
