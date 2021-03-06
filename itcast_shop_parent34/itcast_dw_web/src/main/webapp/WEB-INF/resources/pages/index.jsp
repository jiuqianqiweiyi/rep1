<!DOCTYPE html>
<%@ page import="java.util.Date" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="false" %>
<html lang="cn">
<head>
	<title>河南农业大学千亿级实时数据仓库可视化平台</title>
	<meta charset="UTF-8">
	<link rel="stylesheet" href="${pageContext.request.contextPath}/res/css/main.css">
	<script src="${pageContext.request.contextPath}/res/js/jquery-1.11.0.min.js"></script>
	<script src="${pageContext.request.contextPath}/res/js/echarts.js"></script>
	<script src="http://echarts.baidu.com/gallery/vendors/echarts/map/js/china.js"></script>
	<script src="${pageContext.request.contextPath}/res/js/main.js"></script>
</head>
<body>
	<div class="viewport" id="viewport">
		<!-- 顶部 -->
		<div class="header">
			<span class="logo">河南农业大学</span>
			<h2>河南农业大学千亿级实时数据仓库可视化平台</h2>
			<span class="title">xxx</span>
		</div>
		<!-- 主体部分 -->
		<div class="body">
			<div class="column">
				<!-- 用户 -->
				<div class="panel user">
					<h3>用户日活对比分析</h3>
					<div class="chart" id="userChart"></div>
				</div>
				<!-- 转化率分析 -->
				<div class="panel rate">
					<h3>漏斗转化分析</h3>
					<div class="chart" id="rateChart"></div>
					<div class="data">
						<div class="item">200</div>
						<div class="item">500</div>
						<div class="item">2000</div>
						<div class="item">10000</div>
					</div>
				</div>
				<!-- 订单完成率 -->
				<div class="panel channel">
					<h3>周销售额环比分析</h3>
					<div class="chart" id="channelChart"></div>
				</div>
			</div>
			<div class="column">
				<!-- 地图 -->
				<div class="order">
					<div class="caption">
						<span>本月订单数: <strong>846489</strong>单</span>
						<span style="margin:0 34px">本周订单数: <strong>145628</strong>单</span>
						<span>今日订单数: <strong>49874</strong>单</span>
					</div>
					<div class="chart" id="map"></div>
					<!-- 其它 -->
					<div class="extra">
						<span class="upper">高</span> <span class="lower">低</span>
					</div>
				</div>
				<!-- 销量统计 -->
				<div class="sales">
					<!-- 数据 -->
					<div class="data">
						<div class="item">
							<h5>本月总销售额</h5>
							<span>7</span><span>3</span><span>5</span><span>7</span><span>4</span>
							<span>7</span><span>3</span><span>0</span><span>0</span><span>0</span>
							<span>元</span>
						</div>
						<div class="item">
							<h5>今日总销售额</h5>
							<span>7</span><span>3</span><span>5</span><span>7</span><span>4</span>
							<span>7</span><span>3</span><span>0</span><span>0</span><span>0</span>
							<span>元</span>
						</div>
					</div>
					<h3>今日24小时销售额</h3>
					<div class="chart" id="salesChart"></div>
				</div>
			</div>
			<div class="column">
				<!-- 订单状态占比 -->
				<div class="panel status">
					<h3>订单状态占比分析</h3>
					<div class="chart" id="statusChart"></div>
					<div class="extra" data-text="全国"></div>
				</div>
				<!-- 订单完成率 -->
				<div class="panel completed">
					<h3>周订单完成趋势分析</h3>
					<div class="chart" id="completedChart"></div>
				</div>
				<!-- 销售趋势 -->
				<div class="panel trend">
					<h3>Top4地区销售排行</h3>
					<div class="chart" id="trendChart"></div>
					<div class="extra">单位: 万元</div>
				</div>
			</div>
		</div>
	</div>
	<script>
      // 调整屏幕适配
      	setFont();
      	window.addEventListener('resize', function () {
        	setFont();
      	})
		function query() {
			$.ajax({
		        type: 'POST',
		        url: 'dashboard/list',
		        data: null,
		        dataType: 'json',
		        success: function(data){
		        	userChart(data.visitor);
		          	rateChart(data.convert);
		          	channelChart(data.weekSale);
		          	salesChart(data.sales);
		          	statusChart(data.top8orderNum);
		          	completedChart(data.weekOrderFinish);
		          	trendChart(data.top4sale);
		          	orderNum(data.orderNum);
		          	sales(data.sales);
		          	orderChart(data.chinaMap)
		        },
		        error: function(data){
		        }
		    });
		}
      	var timeInterval = setInterval(function(){query()},1000)
      	//query();
  </script>
</body>
</html>