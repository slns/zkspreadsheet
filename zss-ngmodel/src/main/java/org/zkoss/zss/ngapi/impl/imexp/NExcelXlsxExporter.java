/*

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		2013/12/01 , Created by Hawk
}}IS_NOTE

Copyright (C) 2013 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package org.zkoss.zss.ngapi.impl.imexp;

import org.openxmlformats.schemas.spreadsheetml.x2006.main.*;
import org.zkoss.poi.ss.usermodel.*;
import org.zkoss.poi.ss.usermodel.charts.*;
import org.zkoss.poi.xssf.usermodel.*;
import org.zkoss.poi.xssf.usermodel.charts.*;
import org.zkoss.zss.ngmodel.*;
import org.zkoss.zss.ngmodel.NChart.NBarDirection;
import org.zkoss.zss.ngmodel.NChart.NChartGrouping;
import org.zkoss.zss.ngmodel.NChart.NChartLegendPosition;
import org.zkoss.zss.ngmodel.NChart.NChartType;
import org.zkoss.zss.ngmodel.chart.*;
/**
 * 
 * @author dennis, kuro
 * @since 3.5.0
 */
public class NExcelXlsxExporter extends AbstractExcelExporter {
	
	protected void exportColumnArray(NSheet sheet, Sheet poiSheet, NColumnArray columnArr) {
		XSSFSheet xssfSheet = (XSSFSheet) poiSheet;
		
        CTWorksheet ctSheet = xssfSheet.getCTWorksheet();
    	if(xssfSheet.getCTWorksheet().sizeOfColsArray() == 0) {
    		xssfSheet.getCTWorksheet().addNewCols();
    	}
    		
    	CTCol col = ctSheet.getColsArray(0).addNewCol();
        col.setMin(columnArr.getIndex()+1);
        col.setMax(columnArr.getLastIndex()+1);
    	col.setStyle(toPOICellStyle(columnArr.getCellStyle()).getIndex());
    	col.setCustomWidth(true);
    	col.setWidth(XUtils.pxToCTChar(columnArr.getWidth(), AbstractExcelImporter.CHRACTER_WIDTH));
    	col.setHidden(columnArr.isHidden());
	}

	@Override
	protected Workbook createPoiBook() {
		return new XSSFWorkbook();
	}

	/**
	 * reference DrawingManagerImpl.addChartX()
	 */
	@Override
	protected void exportChart(NSheet sheet, Sheet poiSheet) {
		for (NChart chart: sheet.getCharts()){
			CategoryData categoryData = null;
			switch(chart.getType()){
				case AREA:
					if (chart.isThreeD()){
						categoryData = new XSSFArea3DChartData();
						((XSSFArea3DChartData)categoryData).setGrouping(toPoiGrouping(chart.getGrouping()));
					}else{
						categoryData = new XSSFAreaChartData();
						((XSSFAreaChartData)categoryData).setGrouping(toPoiGrouping(chart.getGrouping()));
					}
					break;
				case BAR:
					if (chart.isThreeD()){
						categoryData = new XSSFBar3DChartData();				
						((XSSFBar3DChartData)categoryData).setGrouping(toPoiGrouping(chart.getGrouping()));
						((XSSFBar3DChartData)categoryData).setBarDirection(toPoiBarDirection(chart.getBarDirection()));
					}else{
						categoryData = new XSSFBarChartData();
						((XSSFBarChartData)categoryData).setGrouping(toPoiGrouping(chart.getGrouping()));
						((XSSFBarChartData)categoryData).setBarDirection(toPoiBarDirection(chart.getBarDirection()));
					}
					break;
//				case BUBBLE:
//					categoryData = XSSFBubbleChartData();
//					break;
				case COLUMN:
					if (chart.isThreeD()){
						categoryData = new XSSFColumn3DChartData();
						((XSSFColumn3DChartData)categoryData).setGrouping(toPoiGrouping(chart.getGrouping()));
						((XSSFColumn3DChartData)categoryData).setBarDirection(toPoiBarDirection(chart.getBarDirection()));
					}else{
						categoryData = new XSSFColumnChartData();
						((XSSFColumnChartData)categoryData).setGrouping(toPoiGrouping(chart.getGrouping()));
						((XSSFColumnChartData)categoryData).setBarDirection(toPoiBarDirection(chart.getBarDirection()));
					}
					break;
				case DOUGHNUT:
					categoryData = new XSSFDoughnutChartData();
					break;
				case LINE:
					if (chart.isThreeD()){
						categoryData = new XSSFLine3DChartData();
					}else{
						categoryData = new XSSFLineChartData();
					}
					break;
				case PIE:
					if (chart.isThreeD()){
						categoryData = new XSSFPie3DChartData();
					}else{
						categoryData = new XSSFPieChartData();
					}
					break;
//				case SCATTER:
//					XYData xyData =  new XSSFScatChartData(xssfChart);
//					break;
//				case STOCK: TODO contains errors.
//					categoryData = new XSSFStockChartData();
//					break;
				default:
					//ignore unsupported chart
					continue;
			}
			if (categoryData != null){
				fillCategoryData(chart, categoryData);
			}
			
			// cannot create drawing before determining chart type
			final Drawing drawing = poiSheet.createDrawingPatriarch();
			ClientAnchor anchor = toClientAnchor(chart.getAnchor(), poiSheet);
			final Chart poiChart = drawing.createChart(anchor);
			if (chart.isThreeD()){
				poiChart.getOrCreateView3D(); //will create View3D
			}
			if (chart.getLegendPosition() != null) {
				ChartLegend legend = poiChart.getOrCreateLegend();
				legend.setPosition(toPoiLegendPosition(chart.getLegendPosition()));
			}
		
			ChartAxis bottomAxis = createChartAxis(poiChart, chart.getType(), AxisPosition.BOTTOM);
			if (bottomAxis != null) {
				bottomAxis.setCrosses(AxisCrosses.AUTO_ZERO);
				final ValueAxis leftAxis = poiChart.getChartAxisFactory().createValueAxis(AxisPosition.LEFT);
				leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);
				poiChart.plot(categoryData, bottomAxis, leftAxis);
			} else {
				poiChart.plot(categoryData);
			}
//			final XSSFClientAnchor xanchor = (XSSFClientAnchor) anchor;
//			final XSSFGraphicFrame frame = ((XSSFChart)poiChart).getGraphicFrame();
//			final CTGraphicalObjectFrame gfrm = frame.getCTGraphicalObjectFrame();
//			ZssChartX chartX = createXSSFChartX((XSSFDrawing)drawing, gfrm , xanchor);
//			if (_charts == null) {
//				_charts = new ArrayList<ZssChartX>();
//				_chartXMap = new HashMap<Chart, ZssChartX>();
//			}
//			_charts.add(chartX);
//			_chartXMap.put(chartX.getChart(), chartX);
			//        return chartX;
		}
	}
	
	private ClientAnchor toClientAnchor(NViewAnchor viewAnchor, Sheet poiSheet){
		
		//calculate last column index
		int chartWidth = viewAnchor.getWidth();
		int lastColumn = viewAnchor.getColumnIndex();
		int lastXOffset = 0;
		//TODO same column?
		for (int column = viewAnchor.getColumnIndex(); ;column++){
			int interColumnWidth = XUtils.getWidthAny(poiSheet,column, AbstractExcelImporter.CHRACTER_WIDTH); 
			if (chartWidth - interColumnWidth < 0){ 
				lastColumn = column;
				lastXOffset = chartWidth;
				break;
			}else{
				chartWidth -= interColumnWidth;
			}
		}
		
		//calculate last row index
		int chartHeight = viewAnchor.getHeight();
		int lastRow = viewAnchor.getRowIndex();
		int lastYOffset = 0;
		for (int row = viewAnchor.getRowIndex(); ;row++){
			int interRowHeight = XUtils.getHeightAny(poiSheet, row);
			if (chartHeight - interRowHeight < 0){
				lastRow = row;
				lastYOffset = chartHeight;
				break;
			}else{
				chartHeight -= interRowHeight;
			}
		}
		
		ClientAnchor clientAnchor = new XSSFClientAnchor(UnitUtil.pxToEmu(viewAnchor.getXOffset()),UnitUtil.pxToEmu(viewAnchor.getYOffset()),
				UnitUtil.pxToEmu(lastXOffset),UnitUtil.pxToEmu(lastYOffset),
				viewAnchor.getColumnIndex(),viewAnchor.getRowIndex(),
				lastColumn,lastRow);
		return clientAnchor;
	}
	
	private void fillCategoryData(NChart chart, CategoryData categoryData){
		NGeneralChartData chartData = (NGeneralChartData)chart.getData();
		ChartDataSource<?> categories = createFormulaChartDataSource(chartData);
		for (int i=0 ; i < chartData.getNumOfSeries() ; i++){
			NSeries series = chartData.getSeries(i);
			ChartTextSource title = createFormulaChartTextSource(series.getNameFormula());
			ChartDataSource<? extends Number> values = createFormulaChartDataSource(series);
			categoryData.addSerie(title, categories, values);
		}
	}
	
	private void fillXYData(NChart chart, XYData xyData){
		
	}
	
	private ChartTextSource createFormulaChartTextSource(final String formula){
		return new ChartTextSource() {
			
			@Override
			public void renameSheet(String oldname, String newname) {
			}
			
			@Override
			public boolean isReference() {
				return true;
			}
			
			@Override
			public String getTextString() {
				return null;
			}
			
			@Override
			public String getFormulaString() {
				return formula;
			}
		};
		
	}
	private ChartDataSource<? extends Number> createFormulaChartDataSource(final NSeries series){
		return new ChartDataSource<Number>() {

			@Override
			public int getPointCount() {
				return series.getNumOfValue();
			}

			@Override
			public Number getPointAt(int index) {
				return Double.parseDouble(series.getValue(index).toString());
			}

			@Override
			public boolean isReference() {
				return true;
			}

			@Override
			public boolean isNumeric() {
				return true;
			}

			@Override
			public String getFormulaString() {
				return series.getValuesFormula();
			}

			@Override
			public void renameSheet(String oldname, String newname) {
			}
		};
	}
	private ChartDataSource<?> createFormulaChartDataSource(final NGeneralChartData chartData){
		return new ChartDataSource<String>() {

			@Override
			public int getPointCount() {
				return chartData.getNumOfCategory();
			}

			@Override
			public String getPointAt(int index) {
				return chartData.getCategory(index).toString();
			}

			@Override
			public boolean isReference() {
				return true;
			}

			@Override
			public boolean isNumeric() {
				return false;
			}

			@Override
			public String getFormulaString() {
				return chartData.getCategoriesFormula();
			}

			@Override
			public void renameSheet(String oldname, String newname) {
			}
		};
	}
	
	private ChartGrouping toPoiGrouping(NChartGrouping grouping){
		switch(grouping){
			case CLUSTERED:
				return ChartGrouping.CLUSTERED;
			case PERCENT_STACKED:
				return ChartGrouping.PERCENT_STACKED;
			case STACKED:
				return ChartGrouping.STACKED;
			case STANDARD:
			default:
				return ChartGrouping.STANDARD;
		}
	}
	
	private ChartDirection toPoiBarDirection(NBarDirection direction){
		switch(direction){
			case VERTICAL:
				return ChartDirection.VERTICAL;
			case HORIZONTAL:
			default:
				return ChartDirection.HORIZONTAL;
		}
		
	}
	
	private LegendPosition toPoiLegendPosition(NChartLegendPosition position){
		switch(position){
			case BOTTOM:
				return LegendPosition.BOTTOM;
			case TOP:
				return LegendPosition.TOP;
			case TOP_RIGHT:
				return LegendPosition.TOP_RIGHT;
			case LEFT:
				return LegendPosition.LEFT;
			case RIGHT:
			default:
				return LegendPosition.RIGHT;
			
		}
	}
	
	/**
	 * 
	 * @param chart
	 * @param type
	 * @param pos
	 * @return
	 */
	private ChartAxis createChartAxis(Chart chart, NChartType type, AxisPosition pos) {
		switch(type) {
		case DOUGHNUT:
		case PIE:
			return null; //no axis
		case AREA:
		case BAR:
		case COLUMN:
		case LINE:
			return chart.getChartAxisFactory().createCategoryAxis(pos);
		case SCATTER:
			return chart.getChartAxisFactory().createValueAxis(pos);
			//TODO other chart types
		case BUBBLE:
		case STOCK:
		case OF_PIE:
		case RADAR:
		case SURFACE:
		default:
			return null;
		}
	}

}
