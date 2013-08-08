(function($){
    $(document).ready(function(){
		$('#TimeRangePicker_0_1_0').click(function(){
			$('div.timeRangeMenu').show();
			return false;
		});
		$("body").click(function(event){
			$('div.timeRangeMenu').hide();
		});
		$('div.timeRangeMenu > ul > div.innerMenuWrapper > li div.outerMenuWrapper li').mousemove(function(){
			$(this).addClass('splMenuMoseOver');
		});
		$('div.timeRangeMenu > ul > div.innerMenuWrapper > li').mousemove(function(){
			$('div.timeRangeMenu > ul > div.innerMenuWrapper > li.hasSubMenu').find('div').hide();
			$(this).addClass('splMenuMoseOver');
			if($(this).hasClass('hasSubMenu')) {
				$(this).find('div').show();
			}
		});

		$('div.timeRangeMenu > ul .innerMenuWrapper > li').mouseout(function(){
			$(this).removeClass('splMenuMoseOver');
		});
		$('div.timeRangeMenu > ul .innerMenuWrapper > li.hasSubMenu > a.menuItemLink').click(function(){
			return false;
		});
		//时间选择
		$('div.timeRangeMenu > ul .innerMenuWrapper > li.timeRangePreset a.menuItemLink').click(function(){
			$('#TimeRangePicker_0_1_0 span.timeRangeActivator').text($(this).text());
			//alert($(this).attr('latest_time') + '||' + $(this).attr('earliest_time'));
			$('input[name=earliest_time]').val($(this).attr('earliest_time'));
			requestData();
			return true;
		});
		//查询按钮
		$('input.searchButton').click(function(){
			//$('div.graphArea').mask('Loading...')
			//alert($('#SearchBar_0_0_0_id').val());
			requestData();
			return true;
		});

		//表格和图标切换
		$('div.ButtonSwitcher>ul>li').click(function(){
			var type = $(this).attr('_type');
			if(type != 'list') {
				if(typeof oResponseData['grouptable'] == 'undefined' || oResponseData['grouptable'].length<0)
					return;
			}
			$('div.ButtonSwitcher>ul>li').removeClass('selected');
			$(this).addClass('selected');
			buttonSwitcher();
		});
		
		function buttonSwitcher(){
			var type = $('div.ButtonSwitcher>ul>li.selected').attr('_type');
			$('div.divContent > div').hide();
			if(type == 'list') {
				$('#divContentList').show();
			} else if(type == 'table') {
				$('#divContentTable').show();
			} else {
				$('#divContentChart').show();
				requestChart();
			}
		}

		//初始化分页控件
		function initPage(totalCount, pageIndex, pageSize){
			var totalCount = totalCount || 1;  //数据总记录数
			var pageIndex = pageIndex || 0;  //页面索引初始值 
			var pageSize  = pageSize || 10; //每页显示条数初始化，修改显示条数，修改这里即可
			$("#pagination").pagination(totalCount, {
				callback: pageSelectCallback, //PageCallback() 为翻页调用次函数。
				prev_text: "上一页",
				next_text: "下一页",
				link_to: "javascript:void(0)",
				items_per_page:pageSize,
				num_edge_entries:1, //两侧首尾分页条目数   
				num_display_entries:5, //连续分页主体部分分页条目数   
				current_page: pageIndex, //当前页索引 
				noPreNextCurrentCss: true //上一页下一页去除current样式
			});
		}
		function pageSelectCallback(page_id) {
			intList(oResponseData['logtable'], page_id, 10);
			return false;
		}
		var requestColumnChart = function() {
			var data = {'test1':10,'test2':12,'test3':15,'test4':5,'test5':12,'test6':12,'test7':2,'test7':9};
			var config = ChartConfig.getConfig('column');
			var dataTmp = [],dataValue;
			var categories = new Array;
			for (var x in data) {
				dataValue = Number(data[x]);
				dataValue = dataValue == 0 ? 1 : dataValue;
				dataTmp.push([x, dataValue]);
				categories.push(x);
			}
			var finData = [];
			finData.push({name:'test',data:dataTmp});
			config.legend = false;
			config.title.text = '';
			config.series = finData;
			config.xAxis.categories = categories;
			//config.xAxis.min = 1;
			config.xAxis.title.text = '';
			//config.xAxis.type = 'logarithmic';
			config.yAxis.min = 1;
			config.yAxis.title.text = '';
			config.tooltip.headerFormat = '';
			config.tooltip.formatter = function() {
				return (this.point.category) + '总数' + this.point.y+'次';
			};
			config.plotOptions.column.pointWidth = 15;
			$("#VisualChartDiv").highcharts(config);
		};
		
		var requestChart = function() {
			var data = {
				'data1' : {'test1':10,'test2':12,'test3':15,'test4':5,'test5':12,'test6':12,'test7':2,'test8':9},
				'data2' : {'test1':5,'test2':11,'test3':10,'test4':11,'test5':13,'test6':10,'test7':21,'test8':19},
				'data3' : {'test1':2,'test2':15,'test3':6,'test4':9,'test5':23,'test6':5,'test7':17,'test8':7},
			};
			var categories = ['test1','test2','test3','test4','test5','test6','test7','test8'];
			var config = ChartConfig.getConfig('area');
			var dataTmp = [],dataValue;
			var finData = [];
			for (var x in data) {
				var dataSeries = new Array;
				for(var index in data[x]) {
					dataSeries.push(Number(data[x][index]));
				}
				finData.push({name:x,data:dataSeries});
			}
			//config.legend = false;
			config.title.text = '';
			config.series = finData;
			config.xAxis.categories = categories;
			//config.xAxis.min = 1;
			config.xAxis.title.text = '';
			//config.xAxis.type = 'logarithmic';
			//config.yAxis.min = 1;
			//config.yAxis.title.text = '';
			config.tooltip.headerFormat = '';
			config.tooltip.formatter = function() {
				return (this.point.category) + '总数' + this.point.y+'次';
			};
			//config.plotOptions.column.pointWidth = 15;
			$("#divContentChart").highcharts(config);
		};

		var intTable = function (grouptable) {
			/*
			grouptable = [
				{":time":"2013-07-18 10:00:00",":gkey":{"cmd":"add","user":"admin"},"count1":"11","sum1":"400"},
				{":time":"2013-07-18 10:00:05",":gkey":{"cmd":"add","user":"admin"},"count1":"11","sum1":"400"},
				{":time":"2013-07-18 10:00:10",":gkey":{"cmd":"add","user":"admin"},"count1":"11","sum1":"400"}
			];
			*/
			var aHead = new Array();
			if(grouptable.length>0) {
				for(key in grouptable[0]) {
					if(key == 'gKeys') {
						for(group in grouptable[0]['gKeys']) {
							aHead.push(group);
						}
					} else {
						aHead.push(key);
					}
				}
			}
			var tags = new Array();
			tags.push("<thead><tr>");
			for (var i in aHead) {
				tags.push("<th align='left'>" + aHead[i] + "</th>");
			}
			tags.push("</tr></thead>");
			tags.push("<tbody>");

			for (var i = 0; i<grouptable.length; i++) {
				var row = grouptable[i];
				tags.push("<tr>");
				for(x in row) {
					if(x == 'gKeys') {
						for(group in  row[x]) {
							tags.push("<td>" + row[x][group] + "</td>");
						}
					} else {
						tags.push("<td>" + row[x] + "</td>");
					}
				}
				tags.push("</tr>");
			}
			tags.push("</tbody>");
			$('#divContentTable table').html(tags.join(''));
		}
		var intList = function (data, page, pageSize) {
			page = page || 0;
			pageSize = pageSize || 10;
			
			var tags = new Array();
			var start = page * pageSize;
			var end = start + pageSize;
			end = end>data.length ? data.length : end;
			var aHead = new Array();
			aHead.push("<tr>");
			if(data.length>0) {
				for(key in data[0]) {
					aHead.push("<th align='left'>" + key + "</th>");
				}
			}
			aHead.push("</tr>");
			$('#divContentList thead').html(aHead.join(''));
			for (var i = start; i < end; i++) {
				rowData = data[i];
				tags.push("<tr>");
				for(var key in rowData) {
					tags.push("<td>" + rowData[key] + "</td>");
				}
				tags.push("</tr>");
			}
			$('#divContentList tbody.requestListContainer').html(tags.join(''));
		}
		var intervalid = {};
		/**
		var oResponseData = {
"query-time":"1375844188248",
"logtable":[{"bsdf":null,"timestamp":1375844102109,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_3384997383813446709_3730388"},{"bsdf":null,"timestamp":1375844093059,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-5023261333670419384_9787800"},{"bsdf":null,"timestamp":1375844093070,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_3948385012321754028_10737882"},{"bsdf":null,"timestamp":1375844093087,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_6771258316186115400_2750697"},{"bsdf":null,"timestamp":1375843976678,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-6991748565011521674_398825"},{"bsdf":null,"timestamp":1375843976701,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_711304423952522562_2891666"},{"bsdf":null,"timestamp":1375843976706,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-2404151122267017942_10636508"},{"bsdf":null,"timestamp":1375843976726,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-1567359399180177209_279233"},{"bsdf":null,"timestamp":1375844105780,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-1258945864034278822_3482578"},{"bsdf":null,"timestamp":1375844105807,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-3118765411474015533_2574415"},{"bsdf":null,"timestamp":1375844105979,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-568247406633584568_426812"},{"bsdf":null,"timestamp":1375844106580,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_6586009189497381137_5261708"},{"bsdf":null,"timestamp":1375844055520,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_6597067407398474548_5115353"},{"bsdf":null,"timestamp":1375844055544,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-5886779743604541114_2187083"},{"bsdf":null,"timestamp":1375844055576,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-6004256066975312832_2648945"},{"bsdf":null,"timestamp":1375844055587,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_6626661876712044644_2166911"},{"bsdf":null,"timestamp":1375844105582,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_8519069249156831112_5006950"},{"bsdf":null,"timestamp":1375844105609,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-7026114819740348607_2286119"},{"bsdf":null,"timestamp":1375844105781,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-5587911636528378015_2349525"},{"bsdf":null,"timestamp":1375844108249,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-9184221165888524456_9873453"},{"bsdf":null,"timestamp":1375844110915,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-5100599310655888330_5653016"},{"bsdf":null,"timestamp":1375844107843,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-8339727996335690181_2128295"},{"bsdf":null,"timestamp":1375844107885,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_3821507283986240162_825366"},{"bsdf":null,"timestamp":1375844107926,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_2394721687166620579_549747"},{"bsdf":null,"timestamp":1375844107941,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_988211684293787795_2193542"},{"bsdf":null,"timestamp":1375844092069,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-6057497244886111917_3598418"},{"bsdf":null,"timestamp":1375844092086,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-8012754677185192782_612312"},{"bsdf":null,"timestamp":1375844093259,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-121325155643726739_2767192"},{"bsdf":null,"timestamp":1375844093269,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_8841151281436866066_1908059"},{"bsdf":null,"timestamp":1375844116527,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_7622667060709077711_398768"},{"bsdf":null,"timestamp":1375844089432,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_5307494414646420327_5320574"},{"bsdf":null,"timestamp":1375844089632,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-6400968767673667893_2131260"},{"bsdf":null,"timestamp":1375844111431,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-2702482402584157332_11139958"},{"bsdf":null,"timestamp":1375844111450,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-8215132320890704411_2566539"},{"bsdf":null,"timestamp":1375844112911,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-898990120785745847_313762"},{"bsdf":null,"timestamp":1375844113111,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-3460825125617523094_4711671"},{"bsdf":null,"timestamp":1375844114987,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_7833602706451753406_2787282"},{"bsdf":null,"timestamp":1375844105847,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-946854428070094574_398696"},{"bsdf":null,"timestamp":1375844126125,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_1602402548172054551_335034"},{"bsdf":null,"timestamp":1375844126050,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-2213835902660102774_10093321"},{"bsdf":null,"timestamp":1375844126082,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_8295213791631796860_2335643"},{"bsdf":null,"timestamp":1375844085310,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_7377678383205150488_9870429"},{"bsdf":null,"timestamp":1375844109711,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_4459159047870142535_11690652"},{"bsdf":null,"timestamp":1375844109910,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-7609477638282305082_2468313"},{"bsdf":null,"timestamp":1375844109931,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_9071121877283496382_2755860"},{"bsdf":null,"timestamp":1375844109942,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-3833364964199032033_286792"},{"bsdf":null,"timestamp":1375844126580,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-2020537240318793066_2153575"},{"bsdf":null,"timestamp":1375844126603,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-6044480207071052879_554318"},{"bsdf":null,"timestamp":1375844076026,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_8892739176301087980_5320299"},{"bsdf":null,"timestamp":1375844130481,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-2891110392967260829_11220220"},{"bsdf":null,"timestamp":1375844130500,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_7185933753336133427_2191132"},{"bsdf":null,"timestamp":1375844136628,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-972956437628158701_398851"},{"bsdf":null,"timestamp":1375844139441,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-5510918068431973102_9392590"},{"bsdf":null,"timestamp":1375844139117,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_1894078151402173269_2767379"},{"bsdf":null,"timestamp":1375844135788,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-7769634321371299030_9869038"},{"bsdf":null,"timestamp":1375844135798,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_1815198863030460289_11688664"},{"bsdf":null,"timestamp":1375844135812,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_2683171391318835875_4995003"},{"bsdf":null,"timestamp":1375844135819,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-2930612185238182968_1924353"},{"bsdf":null,"timestamp":1375844135843,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_2880790257293153970_387842"},{"bsdf":null,"timestamp":1375844135860,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_1148428566650085065_2188436"},{"bsdf":null,"timestamp":1375844135886,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_6292804968983084350_557569"},{"bsdf":null,"timestamp":1375844137587,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_2524448294045270499_5020156"},{"bsdf":null,"timestamp":1375844137608,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-2075066333209247614_10485384"},{"bsdf":null,"timestamp":1375844137625,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-1190757411588812952_4783838"},{"bsdf":null,"timestamp":1375844137633,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_7362979014481918441_455808"},{"bsdf":null,"timestamp":1375844137788,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-8585305312979996190_11486101"},{"bsdf":null,"timestamp":1375844141988,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-6097157443142321814_217794"},{"bsdf":null,"timestamp":1375844141997,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-5559624395079487288_2918726"},{"bsdf":null,"timestamp":1375844142187,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-6386868342025205946_2586439"},{"bsdf":null,"timestamp":1375844142265,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-1995442786464688392_2757790"},{"bsdf":null,"timestamp":1375844142278,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_3271932134094693386_5128225"},{"bsdf":null,"timestamp":1375844112522,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_6632122888543095006_11202395"},{"bsdf":null,"timestamp":1375844136030,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-662910421154119200_1471976"},{"bsdf":null,"timestamp":1375844143150,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-3246153200761614497_398829"},{"bsdf":null,"timestamp":1375844136520,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_8619073592983161995_170902"},{"bsdf":null,"timestamp":1375844146986,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_8967528488941340146_11746838"},{"bsdf":null,"timestamp":1375844147004,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_5457907607405536714_1938108"},{"bsdf":null,"timestamp":1375844147186,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-8518925762904006264_4769711"},{"bsdf":null,"timestamp":1375844147194,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-2237560786235503815_2578140"},{"bsdf":null,"timestamp":1375844147202,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-2228301316454649727_2525514"},{"bsdf":null,"timestamp":1375844141925,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_2276331636937399228_4986843"},{"bsdf":null,"timestamp":1375844141972,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-7274344354409878551_2958782"},{"bsdf":null,"timestamp":1375844151237,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-3277158517613946464_174383"},{"bsdf":null,"timestamp":1375844122577,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_2631502133415884068_9433293"},{"bsdf":null,"timestamp":1375844133176,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-4602306663780910515_313271"},{"bsdf":null,"timestamp":1375844156669,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-4685946270940293629_3730381"},{"bsdf":null,"timestamp":1375844157268,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-1392563759770664411_5002693"},{"bsdf":null,"timestamp":1375844147650,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_8798393736108676971_9421101"},{"bsdf":null,"timestamp":1375844148665,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-5641983659916457951_343104"},{"bsdf":null,"timestamp":1375844148693,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_5299403463135372367_2751677"},{"bsdf":null,"timestamp":1375844148706,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-4353475210387995011_2224990"},{"bsdf":null,"timestamp":1375844148713,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-2770502602248199426_2192193"},{"bsdf":null,"timestamp":1375844154665,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-3106075718414139867_3778727"},{"bsdf":null,"timestamp":1375844154690,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-1343351152136961349_10868969"},{"bsdf":null,"timestamp":1375844149862,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_6183981771856168585_9876398"},{"bsdf":null,"timestamp":1375844149877,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_5603874489184217226_451020"},{"bsdf":null,"timestamp":1375844149887,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_4690928141540151444_1915614"},{"bsdf":null,"timestamp":1375844157659,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-7787005524389297818_11033429"},{"bsdf":null,"timestamp":1375844120121,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_-963776701148084468_3599018"},{"bsdf":null,"timestamp":1375844120140,"level":"INFO","location":"org.apache.hadoop.hdfs.server.datanode.BlockPoolSliceScanner","message":"Verification succeeded for BP-618800880-127.0.1.1-1340026760932:blk_3388554996604543025_3447746"}],
"grouptable":[{"count_sfd":1,"gKeys":{"bsdf":null,"groupTime":"08\/07\/2013 10:54:30"}},{"count_sfd":4,"gKeys":{"bsdf":null,"groupTime":"08\/07\/2013 10:52:50"}},{"count_sfd":16,"gKeys":{"bsdf":null,"groupTime":"08\/07\/2013 10:55:20"}},{"count_sfd":6,"gKeys":{"bsdf":null,"groupTime":"08\/07\/2013 10:56:10"}},{"count_sfd":1,"gKeys":{"bsdf":null,"groupTime":"08\/07\/2013 10:54:00"}},{"count_sfd":4,"gKeys":{"bsdf":null,"groupTime":"08\/07\/2013 10:54:10"}},{"count_sfd":18,"gKeys":{"bsdf":null,"groupTime":"08\/07\/2013 10:55:00"}},{"count_sfd":13,"gKeys":{"bsdf":null,"groupTime":"08\/07\/2013 10:55:50"}},{"count_sfd":3,"gKeys":{"bsdf":null,"groupTime":"08\/07\/2013 10:54:40"}},{"count_sfd":25,"gKeys":{"bsdf":null,"groupTime":"08\/07\/2013 10:55:30"}},{"count_sfd":7,"gKeys":{"bsdf":null,"groupTime":"08\/07\/2013 10:54:50"}},{"count_sfd":21,"gKeys":{"bsdf":null,"groupTime":"08\/07\/2013 10:55:40"}},{"count_sfd":8,"gKeys":{"bsdf":null,"groupTime":"08\/07\/2013 10:55:10"}},{"count_sfd":2,"gKeys":{"bsdf":null,"groupTime":"08\/07\/2013 10:56:00"}}],
"groupall":[{"count_sfd":129,"gKeys":{"bsdf":null}}]};
		/**/
		var oResponseData = {
			"grouptable": [
			],
			"logtable":[
			]
		};
		/**/
		var requestData = function() {
			clearInterval(intervalid);
			var time = $('input[name=earliest_time]').val();
			if(time == 60) {
				var sec = 5;
			} else {
				var sec = 10
			}
			var keywords = $('#SearchBar_0_0_0_id').val();
			var time = $('input[name=earliest_time]').val();
			var url = '/query/create';
			var postData = 'querystring=' + keywords + '&timewindow=' + time;
			$('.graphArea .events').removeClass('eventsNumOk').addClass('eventsNumLoading');
			$.ajax({
				type: "POST",
				url: url,	
				data: postData,
				dataType: "json",
				success: function(data, status) {
					if(data["query-id"]) {
						ajaxQequest(data["query-id"]);
						intervalid = setInterval(function(){ajaxQequest(data["query-id"])}, sec*1000);
					}
				},
				error:function(msg) {
					//alert("Ajax error status: "+msg.status);
					$('.graphArea .events').removeClass('eventsNumLoading').addClass('eventsNumOk');
				}
			});
		}

		var ajaxQequest = function(id) {
			$.ajax({
				type: "POST",
				url: '/query/get?query-id='+id,	
				data: '',
				dataType: "json",
				success: function(oData, status) {
					oResponseData = oData;
					requestColumnChart();
					intList(oResponseData['logtable']);
					initPage(oResponseData['logtable'].length);
					intTable(oResponseData['grouptable']);
					$('.graphArea .events').removeClass('eventsNumLoading').addClass('eventsNumOk');
				},
				error:function(msg) {
					//alert("Ajax error status: "+msg.status);
					$('.graphArea .events').removeClass('eventsNumLoading').addClass('eventsNumOk');
				}
			});
		}
		requestColumnChart();
		buttonSwitcher()
		intList(oResponseData['logtable']);
		initPage(oResponseData['logtable'].length);
		intTable(oResponseData['grouptable']);
		//requestChart();

 });
})(jQuery);
