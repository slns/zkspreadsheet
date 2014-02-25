/*

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		2013/12/13 , Created by Hawk
}}IS_NOTE

Copyright (C) 2013 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
 */
package org.zkoss.zss.range.impl.imexp;

import java.io.*;
import java.util.*;

import org.zkoss.poi.hssf.usermodel.HSSFRichTextString;
import org.zkoss.poi.ss.usermodel.*;
import org.zkoss.poi.ss.util.CellRangeAddress;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFRichTextString;
import org.zkoss.zss.model.*;
import org.zkoss.zss.model.SAutoFilter.FilterOp;
import org.zkoss.zss.model.SAutoFilter.NFilterColumn;
import org.zkoss.zss.model.SCellStyle.Alignment;
import org.zkoss.zss.model.SCellStyle.BorderType;
import org.zkoss.zss.model.SCellStyle.FillPattern;
import org.zkoss.zss.model.SCellStyle.VerticalAlignment;
import org.zkoss.zss.model.SFont.TypeOffset;
import org.zkoss.zss.model.SFont.Underline;
import org.zkoss.zss.model.SHyperlink.HyperlinkType;
import org.zkoss.zss.model.SPicture.Format;

/**
 * Contains common importing behavior for both XLSX and XLS. Spreadsheet
 * {@link SBook} model including following information: Book: name Sheet: name,
 * (default) column width, (default) row height, hidden row (column), row
 * (column) style, freeze, merge, protection, named range , gridline display
 * Cell: type, value, font with color and style, type offset(normal or
 * subscript), background color, border's type and color , data format,
 * alignment, wrap, locked, fill pattern
 * 
 * We use XLSX, XLS common interface (e.g. CellStyle instead of
 * {@link XSSFCellStyle}) to get content first for that codes can be easily
 * moved to parent class.
 * 
 * @author Hawk
 * @since 3.5.0
 */
abstract public class AbstractExcelImporter extends AbstractImporter {
	/**
	 * Office Open XML Part 4: Markup Language Reference 3.3.1.12 col (Column
	 * Width & Formatting) The character width 7 is based on Calibri 11. We can
	 * get correct column width under Excel 2007, but incorrect column width in
	 * 2010
	 */
	public static final int CHRACTER_WIDTH = 7;
	/**
	 * <poi CellStyle index, {@link SCellStyle} object> Keep track of imported
	 * style during importing to avoid creating duplicated style objects.
	 */
	protected Map<Short, SCellStyle> importedStyle = new HashMap<Short, SCellStyle>();
	/** <poi Font index, {@link SFont} object> **/
	protected Map<Short, SFont> importedFont = new HashMap<Short, SFont>();
	/** target book model */
	protected SBook book;
	/** source POI book */
	protected Workbook workbook;
	/**
	 * Import the model according to reversed dependency order among model
	 * objects: book, sheet, defined name, cells, chart, pictures, validation.
	 */
	@Override
	public SBook imports(InputStream is, String bookName) throws IOException {

		workbook = createPoiBook(is);
		book = SBooks.createBook(bookName);

		SBookSeries bookSeries = book.getBookSeries();
		boolean isCacheClean = bookSeries.isAutoFormulaCacheClean();
		try {
			bookSeries.setAutoFormulaCacheClean(false);// disable it to avoid
														// unnecessary clean up
														// during importing

			importExternalBookLinks();
			int numberOfSheet = workbook.getNumberOfSheets();
			for (int i = 0; i < numberOfSheet; i++) {
				importSheet(workbook.getSheetAt(i));
			}
			importNamedRange();
			for (int i = 0; i < numberOfSheet; i++) {
				SSheet sheet = book.getSheet(i);
				Sheet poiSheet = workbook.getSheetAt(i);
				for (Row poiRow : poiSheet) {
					importRow(poiRow, sheet);
				}
				importColumn(poiSheet, sheet);
				importMergedRegions(poiSheet, sheet);
				importDrawings(poiSheet, sheet);
				importValidation(poiSheet, sheet);
				importAutoFilter(poiSheet, sheet);
			}
		} finally {
			book.getBookSeries().setAutoFormulaCacheClean(isCacheClean);
		}

		return book;
	}

	abstract protected Workbook createPoiBook(InputStream is) throws IOException;

	/**
	 * When a column is hidden with default width, we don't import the width for
	 * it's 0.
	 * 
	 * @param poiSheet
	 * @param sheet
	 */
	abstract protected void importColumn(Sheet poiSheet, SSheet sheet);

	/**
	 * If in same column: anchorWidthInFirstColumn + anchor width in
	 * inter-columns + anchorWidthInLastColumn (dx2) no in same column:
	 * anchorWidthInLastColumn - offsetInFirstColumn (dx1)
	 * 
	 */
	abstract protected int getAnchorWidthInPx(ClientAnchor anchor, Sheet poiSheet);

	abstract protected int getAnchorHeightInPx(ClientAnchor anchor, Sheet poiSheet);

	/**
	 * Name should be created after sheets created. A special defined name,
	 * _xlnm._FilterDatabase (xlsx) or _FilterDatabase (xls), stores the
	 * selected cells for auto-filter
	 */
	protected void importNamedRange() {
		for (int i = 0; i < workbook.getNumberOfNames(); i++) {
			Name definedName = workbook.getNameAt(i);
			if (definedName.isFunctionName() // ignore defined name of
												// functions, they are macro
												// functions that we don't
												// support
					|| definedName.getRefersToFormula() == null) { // ignore
																	// defined
																	// name with
																	// null
																	// formula,
																	// don't
																	// know when
																	// will have
																	// this case

				continue;
			}

			SName namedRange = null;
			if (definedName.getSheetIndex() == -1) {// workbook scope
				namedRange = book.createName(definedName.getNameName());
			} else {
				namedRange = book.createName(definedName.getNameName(), definedName.getSheetName());
			}
			namedRange.setRefersToFormula(definedName.getRefersToFormula());
		}
	}

	/**
	 * Excel uses external book links to map external book index and name. The
	 * formula contains full external book name or index only (e.g [book2.xlsx]
	 * or [1]). We needs such table for parsing and evaluating formula when
	 * necessary.
	 */
	abstract protected void importExternalBookLinks();

	/*
	 * import sheet scope content from POI Sheet.
	 */
	protected SSheet importSheet(Sheet poiSheet) {
		SSheet sheet = book.createSheet(poiSheet.getSheetName());
		sheet.setDefaultRowHeight(UnitUtil.twipToPx(poiSheet.getDefaultRowHeight()));
		// reference XUtils.getDefaultColumnWidthInPx()
		int defaultWidth = UnitUtil.defaultColumnWidthToPx(poiSheet.getDefaultColumnWidth(), CHRACTER_WIDTH);
		sheet.setDefaultColumnWidth(defaultWidth);
		// reference FreezeInfoLoaderImpl.getRowFreeze()
		sheet.getViewInfo().setNumOfRowFreeze(BookHelper.getRowFreeze(poiSheet));
		sheet.getViewInfo().setNumOfColumnFreeze(BookHelper.getColumnFreeze(poiSheet));
		sheet.getViewInfo().setDisplayGridline(poiSheet.isDisplayGridlines());
		sheet.getViewInfo().setColumnBreaks(poiSheet.getColumnBreaks());
		sheet.getViewInfo().setRowBreaks(poiSheet.getRowBreaks());

		SHeader header = sheet.getViewInfo().getHeader();
		header.setCenterText(poiSheet.getHeader().getCenter());
		header.setLeftText(poiSheet.getHeader().getLeft());
		header.setRightText(poiSheet.getHeader().getRight());

		SFooter footer = sheet.getViewInfo().getFooter();
		footer.setCenterText(poiSheet.getFooter().getCenter());
		footer.setLeftText(poiSheet.getFooter().getLeft());
		footer.setRightText(poiSheet.getFooter().getRight());

		sheet.getPrintSetup().setBottomMargin(UnitUtil.incheToPx(poiSheet.getMargin(Sheet.BottomMargin)));
		sheet.getPrintSetup().setTopMargin(UnitUtil.incheToPx(poiSheet.getMargin(Sheet.TopMargin)));
		sheet.getPrintSetup().setLeftMargin(UnitUtil.incheToPx(poiSheet.getMargin(Sheet.LeftMargin)));
		sheet.getPrintSetup().setRightMargin(UnitUtil.incheToPx(poiSheet.getMargin(Sheet.RightMargin)));

		sheet.getPrintSetup().setHeaderMargin(UnitUtil.incheToPx(poiSheet.getMargin(Sheet.HeaderMargin)));
		sheet.getPrintSetup().setFooterMargin(UnitUtil.incheToPx(poiSheet.getMargin(Sheet.FooterMargin)));
		sheet.getPrintSetup().setPaperSize(toZssPaperSize(poiSheet.getPrintSetup().getPaperSize()));
		sheet.getPrintSetup().setLandscape(poiSheet.getPrintSetup().getLandscape());

		sheet.setPassword(poiSheet.getProtect()?"":null);
		
		return sheet;
	}

	protected void importMergedRegions(Sheet poiSheet, SSheet sheet) {
		// merged cells
		// reference RangeImpl.getMergeAreas()
		int nMerged = poiSheet.getNumMergedRegions();
		for (int i = nMerged - 1; i >= 0; --i) {
			final CellRangeAddress mergedRegion = poiSheet.getMergedRegion(i);
			sheet.addMergedRegion(new CellRegion(mergedRegion.getFirstRow(), mergedRegion.getFirstColumn(), mergedRegion.getLastRow(), mergedRegion.getLastColumn()));
		}
	}

	abstract protected void importDrawings(Sheet poiSheet, SSheet sheet);

	abstract protected void importValidation(Sheet poiSheet, SSheet sheet);

	protected SRow importRow(Row poiRow, SSheet sheet) {
		SRow row = sheet.getRow(poiRow.getRowNum());
		row.setHeight(UnitUtil.twipToPx(poiRow.getHeight()));
		row.setCustomHeight(poiRow.isCustomHeight());
		row.setHidden(poiRow.getZeroHeight());
		CellStyle rowStyle = poiRow.getRowStyle();
		if (rowStyle != null) {
			row.setCellStyle(importCellStyle(rowStyle));
		}

		for (Cell poiCell : poiRow) {
			importCell(poiCell, poiRow.getRowNum(), sheet);
		}

		return row;
	}

	protected SCell importCell(Cell poiCell, int row, SSheet sheet) {

		SCell cell = sheet.getCell(row, poiCell.getColumnIndex());
		cell.setCellStyle(importCellStyle(poiCell.getCellStyle()));

		switch (poiCell.getCellType()) {
		case Cell.CELL_TYPE_NUMERIC:
			cell.setNumberValue(poiCell.getNumericCellValue());
			break;
		case Cell.CELL_TYPE_STRING:
			RichTextString poiRichTextString = poiCell.getRichStringCellValue();
			if (poiRichTextString != null && poiRichTextString.numFormattingRuns() > 0) {
				SRichText richText = cell.setupRichTextValue();
				String cellValue = poiRichTextString.getString();
				for (int i = 0; i < poiRichTextString.numFormattingRuns(); i++) {
					int nextFormattingRunIndex = (i + 1) >= poiRichTextString.numFormattingRuns() ? cellValue.length() : poiRichTextString.getIndexOfFormattingRun(i + 1);
					final String content = cellValue.substring(poiRichTextString.getIndexOfFormattingRun(i), nextFormattingRunIndex);
					richText.addSegment(content, toZssFont(getPoiFontFromRichText(workbook, poiRichTextString, i)));
				}
			} else {
				cell.setStringValue(poiCell.getStringCellValue());
			}
			break;
		case Cell.CELL_TYPE_BOOLEAN:
			cell.setBooleanValue(poiCell.getBooleanCellValue());
			break;
		case Cell.CELL_TYPE_FORMULA:
			cell.setFormulaValue(poiCell.getCellFormula());
			break;
		case Cell.CELL_TYPE_ERROR:
			cell.setErrorValue(convertErrorCode(poiCell.getErrorCellValue()));
			break;
		case Cell.CELL_TYPE_BLANK:
			// do nothing because spreadsheet model auto creates blank cells
		default:
			// TODO log: leave an unknown cell type as a blank cell.
			break;

		}
		
		Hyperlink poiHyperlink = poiCell.getHyperlink();
		if (poiHyperlink != null) {
			SHyperlink hyperlink = cell.setupHyperlink();
			hyperlink.setType(toZssHyperlinkType(poiHyperlink.getType()));
			hyperlink.setAddress(poiHyperlink.getAddress());
			hyperlink.setLabel(poiHyperlink.getLabel());
			cell.setHyperlink(hyperlink);
		}

		return cell;
	}

	/**
	 * Convert CellStyle into NCellStyle
	 * 
	 * @param poiCellStyle
	 */
	protected SCellStyle importCellStyle(CellStyle poiCellStyle) {
		SCellStyle cellStyle = null;
		short idx = poiCellStyle.getIndex();
		if ((cellStyle = importedStyle.get(idx)) == null) {
			cellStyle = book.createCellStyle(true);
			importedStyle.put(idx, cellStyle);
			String dataFormat = poiCellStyle.getRawDataFormatString();
			if(dataFormat==null){//just in case
				dataFormat = SCellStyle.FORMAT_GENERAL;
			}
			if(!poiCellStyle.isBuiltinDataFormat()){
				cellStyle.setDirectDataFormat(dataFormat);
			}else{
				cellStyle.setDataFormat(dataFormat);
			}
			cellStyle.setWrapText(poiCellStyle.getWrapText());
			cellStyle.setLocked(poiCellStyle.getLocked());
			cellStyle.setAlignment(convertAlignment(poiCellStyle.getAlignment()));
			cellStyle.setVerticalAlignment(convertVerticalAlignment(poiCellStyle.getVerticalAlignment()));
			cellStyle.setFillColor(book.createColor(BookHelper.colorToBackgroundHTML(workbook, poiCellStyle.getFillForegroundColorColor())));

			cellStyle.setBorderLeft(convertBorderType(poiCellStyle.getBorderLeft()));
			cellStyle.setBorderTop(convertBorderType(poiCellStyle.getBorderTop()));
			cellStyle.setBorderRight(convertBorderType(poiCellStyle.getBorderRight()));
			cellStyle.setBorderBottom(convertBorderType(poiCellStyle.getBorderBottom()));

			cellStyle.setBorderLeftColor(book.createColor(BookHelper.colorToBorderHTML(workbook, poiCellStyle.getLeftBorderColorColor())));
			cellStyle.setBorderTopColor(book.createColor(BookHelper.colorToBorderHTML(workbook, poiCellStyle.getTopBorderColorColor())));
			cellStyle.setBorderRightColor(book.createColor(BookHelper.colorToBorderHTML(workbook, poiCellStyle.getRightBorderColorColor())));
			cellStyle.setBorderBottomColor(book.createColor(BookHelper.colorToBorderHTML(workbook, poiCellStyle.getBottomBorderColorColor())));
			cellStyle.setHidden(poiCellStyle.getHidden());
			cellStyle.setFillPattern(convertPoiFillPattern(poiCellStyle.getFillPattern()));
			// same style always use same font
			cellStyle.setFont(importFont(poiCellStyle));
		}

		return cellStyle;
	}

	protected SFont importFont(CellStyle poiCellStyle) {
		SFont font = null;
		if (importedFont.containsKey(poiCellStyle.getFontIndex())) {
			font = importedFont.get(poiCellStyle.getFontIndex());
		} else {
			Font poiFont = workbook.getFontAt(poiCellStyle.getFontIndex());
			font = book.createFont(true);
			// font
			font.setName(poiFont.getFontName());
			if (poiFont.getBoldweight() == Font.BOLDWEIGHT_BOLD) {
				font.setBoldweight(SFont.Boldweight.BOLD);
			} else {
				font.setBoldweight(SFont.Boldweight.NORMAL);
			}
			font.setItalic(poiFont.getItalic());
			font.setStrikeout(poiFont.getStrikeout());
			font.setUnderline(convertUnderline(poiFont.getUnderline()));

			font.setHeightPoints(poiFont.getFontHeightInPoints());
			font.setTypeOffset(convertTypeOffset(poiFont.getTypeOffset()));
			font.setColor(book.createColor(BookHelper.getFontHTMLColor(workbook, poiFont)));
		}
		return font;
	}

	protected SHyperlink.HyperlinkType toZssHyperlinkType(int type) {
		switch (type) {
		case Hyperlink.LINK_DOCUMENT:
			return HyperlinkType.DOCUMENT;
		case Hyperlink.LINK_EMAIL:
			return HyperlinkType.EMAIL;
		case Hyperlink.LINK_FILE:
			return HyperlinkType.FILE;
		case Hyperlink.LINK_URL:
		default:
			return HyperlinkType.URL;
		}
	}

	protected SFont toZssFont(Font poiFont) {
		SFont font = null;
		if (importedFont.containsKey(poiFont.getIndex())) {
			font = importedFont.get(poiFont.getIndex());
		} else {
			font = book.createFont(true);
			// font
			font.setName(poiFont.getFontName());
			if (poiFont.getBoldweight() == Font.BOLDWEIGHT_BOLD) {
				font.setBoldweight(SFont.Boldweight.BOLD);
			} else {
				font.setBoldweight(SFont.Boldweight.NORMAL);
			}
			font.setItalic(poiFont.getItalic());
			font.setStrikeout(poiFont.getStrikeout());
			font.setUnderline(convertUnderline(poiFont.getUnderline()));

			font.setHeightPoints(poiFont.getFontHeightInPoints());
			font.setTypeOffset(convertTypeOffset(poiFont.getTypeOffset()));
			font.setColor(book.createColor(BookHelper.getFontHTMLColor(workbook, poiFont)));
		}
		return font;
	}

	protected SPrintSetup.PaperSize toZssPaperSize(short paperSize) {
		switch (paperSize) {
		case PrintSetup.A3_PAPERSIZE:
			return SPrintSetup.PaperSize.A3;
		case PrintSetup.A4_EXTRA_PAPERSIZE:
			return SPrintSetup.PaperSize.A4_EXTRA;
		case PrintSetup.A4_PAPERSIZE:
			return SPrintSetup.PaperSize.A4;
		case PrintSetup.A4_PLUS_PAPERSIZE:
			return SPrintSetup.PaperSize.A4_PLUS;
		case PrintSetup.A4_ROTATED_PAPERSIZE:
			return SPrintSetup.PaperSize.A4_ROTATED;
		case PrintSetup.A4_SMALL_PAPERSIZE:
			return SPrintSetup.PaperSize.A4_SMALL;
		case PrintSetup.A4_TRANSVERSE_PAPERSIZE:
			return SPrintSetup.PaperSize.A4_TRANSVERSE;
		case PrintSetup.A5_PAPERSIZE:
			return SPrintSetup.PaperSize.A5;
		case PrintSetup.B4_PAPERSIZE:
			return SPrintSetup.PaperSize.B4;
		case PrintSetup.B5_PAPERSIZE:
			return SPrintSetup.PaperSize.B5;
		case PrintSetup.ELEVEN_BY_SEVENTEEN_PAPERSIZE:
			return SPrintSetup.PaperSize.ELEVEN_BY_SEVENTEEN;
		case PrintSetup.ENVELOPE_10_PAPERSIZE:
			return SPrintSetup.PaperSize.ENVELOPE_10;
		case PrintSetup.ENVELOPE_9_PAPERSIZE:
			return SPrintSetup.PaperSize.ENVELOPE_9;
		case PrintSetup.ENVELOPE_C3_PAPERSIZE:
			return SPrintSetup.PaperSize.ENVELOPE_C3;
		case PrintSetup.ENVELOPE_C4_PAPERSIZE:
			return SPrintSetup.PaperSize.ENVELOPE_C4;
		case PrintSetup.ENVELOPE_C5_PAPERSIZE:
			return SPrintSetup.PaperSize.ENVELOPE_C5;
		case PrintSetup.ENVELOPE_C6_PAPERSIZE:
			return SPrintSetup.PaperSize.ENVELOPE_C6;
		case PrintSetup.ENVELOPE_DL_PAPERSIZE:
			return SPrintSetup.PaperSize.ENVELOPE_DL;
		case PrintSetup.ENVELOPE_MONARCH_PAPERSIZE:
			return SPrintSetup.PaperSize.ENVELOPE_MONARCH;
		case PrintSetup.EXECUTIVE_PAPERSIZE:
			return SPrintSetup.PaperSize.EXECUTIVE;
		case PrintSetup.FOLIO8_PAPERSIZE:
			return SPrintSetup.PaperSize.FOLIO8;
		case PrintSetup.LEDGER_PAPERSIZE:
			return SPrintSetup.PaperSize.LEDGER;
		case PrintSetup.LETTER_PAPERSIZE:
			return SPrintSetup.PaperSize.LETTER;
		case PrintSetup.LETTER_ROTATED_PAPERSIZE:
			return SPrintSetup.PaperSize.LETTER_ROTATED;
		case PrintSetup.LETTER_SMALL_PAGESIZE:
			return SPrintSetup.PaperSize.LETTER_SMALL;
		case PrintSetup.NOTE8_PAPERSIZE:
			return SPrintSetup.PaperSize.NOTE8;
		case PrintSetup.QUARTO_PAPERSIZE:
			return SPrintSetup.PaperSize.QUARTO;
		case PrintSetup.STATEMENT_PAPERSIZE:
			return SPrintSetup.PaperSize.STATEMENT;
		case PrintSetup.TABLOID_PAPERSIZE:
			return SPrintSetup.PaperSize.TABLOID;
		case PrintSetup.TEN_BY_FOURTEEN_PAPERSIZE:
			return SPrintSetup.PaperSize.TEN_BY_FOURTEEN;
		default:
			return SPrintSetup.PaperSize.A4;
		}
	}

	/*
	 * reference BookHelper.getFontCSSStyle()
	 */
	protected Underline convertUnderline(byte underline) {
		switch (underline) {
		case Font.U_SINGLE:
			return SFont.Underline.SINGLE;
		case Font.U_DOUBLE:
			return SFont.Underline.DOUBLE;
		case Font.U_SINGLE_ACCOUNTING:
			return SFont.Underline.SINGLE_ACCOUNTING;
		case Font.U_DOUBLE_ACCOUNTING:
			return SFont.Underline.DOUBLE_ACCOUNTING;
		case Font.U_NONE:
		default:
			return SFont.Underline.NONE;
		}
	}

	protected TypeOffset convertTypeOffset(short typeOffset) {
		switch (typeOffset) {
		case Font.SS_SUB:
			return TypeOffset.SUB;
		case Font.SS_SUPER:
			return TypeOffset.SUPER;
		case Font.SS_NONE:
		default:
			return TypeOffset.NONE;
		}
	}

	protected BorderType convertBorderType(short poiBorder) {
		switch (poiBorder) {
		case CellStyle.BORDER_THIN:
			return BorderType.THIN;
		case CellStyle.BORDER_MEDIUM:
			return BorderType.MEDIUM;
		case CellStyle.BORDER_DASHED:
			return BorderType.DASHED;
		case CellStyle.BORDER_HAIR:
			return BorderType.HAIR;
		case CellStyle.BORDER_THICK:
			return BorderType.THICK;
		case CellStyle.BORDER_DOUBLE:
			return BorderType.DOUBLE;
		case CellStyle.BORDER_DOTTED:
			return BorderType.DOTTED;
		case CellStyle.BORDER_MEDIUM_DASHED:
			return BorderType.MEDIUM_DASHED;
		case CellStyle.BORDER_DASH_DOT:
			return BorderType.DASH_DOT;
		case CellStyle.BORDER_MEDIUM_DASH_DOT:
			return BorderType.MEDIUM_DASH_DOT;
		case CellStyle.BORDER_DASH_DOT_DOT:
			return BorderType.DASH_DOT_DOT;
		case CellStyle.BORDER_MEDIUM_DASH_DOT_DOT:
			return BorderType.MEDIUM_DASH_DOT_DOT;
		case CellStyle.BORDER_SLANTED_DASH_DOT:
			return BorderType.SLANTED_DASH_DOT;
		case CellStyle.BORDER_NONE:
		default:
			return BorderType.NONE;
		}
	}

	protected Alignment convertAlignment(short poiAlignment) {
		switch (poiAlignment) {
		case CellStyle.ALIGN_LEFT:
			return Alignment.LEFT;
		case CellStyle.ALIGN_RIGHT:
			return Alignment.RIGHT;
		case CellStyle.ALIGN_CENTER:
		case CellStyle.ALIGN_CENTER_SELECTION:
			return Alignment.CENTER;
		case CellStyle.ALIGN_FILL:
			return Alignment.FILL;
		case CellStyle.ALIGN_JUSTIFY:
			return Alignment.JUSTIFY;
		case CellStyle.ALIGN_GENERAL:
		default:
			return Alignment.GENERAL;
		}
	}

	protected VerticalAlignment convertVerticalAlignment(short vAlignment) {
		switch (vAlignment) {
		case CellStyle.VERTICAL_TOP:
			return VerticalAlignment.TOP;
		case CellStyle.VERTICAL_CENTER:
			return VerticalAlignment.CENTER;
		case CellStyle.VERTICAL_JUSTIFY:
			return VerticalAlignment.JUSTIFY;
		case CellStyle.VERTICAL_BOTTOM:
		default:
			return VerticalAlignment.BOTTOM;
		}
	}

	protected ErrorValue convertErrorCode(byte errorCellValue) {
		switch (errorCellValue) {
		case ErrorConstants.ERROR_DIV_0:
			return new ErrorValue(ErrorValue.ERROR_DIV_0);
		case ErrorConstants.ERROR_NA:
			return new ErrorValue(ErrorValue.ERROR_NA);
		case ErrorConstants.ERROR_NAME:
			return new ErrorValue(ErrorValue.INVALID_NAME);
		case ErrorConstants.ERROR_NULL:
			return new ErrorValue(ErrorValue.ERROR_NULL);
		case ErrorConstants.ERROR_NUM:
			return new ErrorValue(ErrorValue.ERROR_NUM);
		case ErrorConstants.ERROR_REF:
			return new ErrorValue(ErrorValue.ERROR_REF);
		case ErrorConstants.ERROR_VALUE:
			return new ErrorValue(ErrorValue.INVALID_VALUE);
		default:
			// TODO log it
			return new ErrorValue(ErrorValue.INVALID_NAME);
		}

	}

	protected FillPattern convertPoiFillPattern(short poiFillPattern) {
		switch (poiFillPattern) {
		case CellStyle.SOLID_FOREGROUND:
			return FillPattern.SOLID_FOREGROUND;
		case CellStyle.FINE_DOTS:
			return FillPattern.FINE_DOTS;
		case CellStyle.ALT_BARS:
			return FillPattern.ALT_BARS;
		case CellStyle.SPARSE_DOTS:
			return FillPattern.SPARSE_DOTS;
		case CellStyle.THICK_HORZ_BANDS:
			return FillPattern.THICK_HORZ_BANDS;
		case CellStyle.THICK_VERT_BANDS:
			return FillPattern.THICK_VERT_BANDS;
		case CellStyle.THICK_BACKWARD_DIAG:
			return FillPattern.THICK_BACKWARD_DIAG;
		case CellStyle.THICK_FORWARD_DIAG:
			return FillPattern.THICK_FORWARD_DIAG;
		case CellStyle.BIG_SPOTS:
			return FillPattern.BIG_SPOTS;
		case CellStyle.BRICKS:
			return FillPattern.BRICKS;
		case CellStyle.THIN_HORZ_BANDS:
			return FillPattern.THIN_HORZ_BANDS;
		case CellStyle.THIN_VERT_BANDS:
			return FillPattern.THIN_VERT_BANDS;
		case CellStyle.THIN_BACKWARD_DIAG:
			return FillPattern.THIN_BACKWARD_DIAG;
		case CellStyle.THIN_FORWARD_DIAG:
			return FillPattern.THIN_FORWARD_DIAG;
		case CellStyle.SQUARES:
			return FillPattern.SQUARES;
		case CellStyle.DIAMONDS:
			return FillPattern.DIAMONDS;
		case CellStyle.LESS_DOTS:
			return FillPattern.LESS_DOTS;
		case CellStyle.LEAST_DOTS:
			return FillPattern.LEAST_DOTS;
		case CellStyle.NO_FILL:
		default:
			return FillPattern.NO_FILL;
		}
	}

	protected ViewAnchor toViewAnchor(Sheet poiSheet, ClientAnchor clientAnchor) {
		int width = getAnchorWidthInPx(clientAnchor, poiSheet);
		int height = getAnchorHeightInPx(clientAnchor, poiSheet);
		ViewAnchor viewAnchor = new ViewAnchor(clientAnchor.getRow1(), clientAnchor.getCol1(), width, height);
		viewAnchor.setXOffset(getXoffsetInPixel(clientAnchor, poiSheet));
		viewAnchor.setYOffset(getYoffsetInPixel(clientAnchor, poiSheet));
		return viewAnchor;
	}

	abstract protected int getXoffsetInPixel(ClientAnchor clientAnchor, Sheet poiSheet);

	abstract protected int getYoffsetInPixel(ClientAnchor clientAnchor, Sheet poiSheet);

	protected void importPicture(List<Picture> poiPictures, Sheet poiSheet, SSheet sheet) {
		for (Picture picture : poiPictures) {
			Format format = Format.valueOfFileExtension(picture.getPictureData().suggestFileExtension());
			if (format != null) {
				sheet.addPicture(format, picture.getPictureData().getData(), toViewAnchor(poiSheet, picture.getClientAnchor()));
			} else {
				// TODO log we ignore a picture with unsupported format
			}
		}
	}

	/**
	 * POI AutoFilter.getFilterColumn(i) sometimes returns null. A POI FilterColumn object only 
	 * exists when we have set a criteria on that column. 
	 * For example, if we enable auto filter on 2 columns, but we only set criteria on 
	 * 2nd column. Thus, the size of filter column is 1. There is only one FilterColumn 
	 * object and its column id is 1. Only getFilterColumn(1) will return a FilterColumn, 
	 * other get null.
	 * 
	 * @param poiSheet source POI sheet
	 * @param sheet destination sheet
	 */
	private void importAutoFilter(Sheet poiSheet, SSheet sheet) {
		AutoFilter poiAutoFilter = poiSheet.getAutoFilter();
		if (poiAutoFilter != null) {
			CellRangeAddress filteringRange = poiAutoFilter.getRangeAddress();
			SAutoFilter autoFilter = sheet.createAutoFilter(new CellRegion(filteringRange.formatAsString()));
			int numberOfColumn = filteringRange.getLastColumn() - filteringRange.getFirstColumn() + 1;
			for (int i = 0; i < numberOfColumn; i++) {
				FilterColumn srcColumn = poiAutoFilter.getFilterColumn(i);
				if (srcColumn == null) {
					continue;
				}
				NFilterColumn destColumn = autoFilter.getFilterColumn(i, true);
				destColumn.setProperties(toFilterOperator(srcColumn.getOperator()), srcColumn.getCriteria1(), srcColumn.getCriteria2(), srcColumn.isOn());
			}
		}
	}

	private FilterOp toFilterOperator(int operator) {
		switch (operator) {
		case AutoFilter.FILTEROP_AND:
			return FilterOp.AND;
		case AutoFilter.FILTEROP_BOTTOM10:
			return FilterOp.BOTTOM10;
		case AutoFilter.FILTEROP_BOTOOM10PERCENT:
			return FilterOp.BOTOOM10_PERCENT;
		case AutoFilter.FILTEROP_OR:
			return FilterOp.OR;
		case AutoFilter.FILTEROP_TOP10:
			return FilterOp.TOP10;
		case AutoFilter.FILTEROP_TOP10PERCENT:
			return FilterOp.TOP10_PERCENT;
		case AutoFilter.FILTEROP_VALUES:
		default:
			return FilterOp.VALUES;
		}
	}

	private org.zkoss.poi.ss.usermodel.Font getPoiFontFromRichText(Workbook book, RichTextString rstr, int run) {
		org.zkoss.poi.ss.usermodel.Font font = rstr instanceof HSSFRichTextString ? book.getFontAt(((HSSFRichTextString) rstr).getFontOfFormattingRun(run)) : ((XSSFRichTextString) rstr)
				.getFontOfFormattingRun(run);
		return font == null ? book.getFontAt((short) 0) : font;
	}
}