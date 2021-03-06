/* Book.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		2013/5/1 , Created by dennis
}}IS_NOTE

Copyright (C) 2013 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package org.zkoss.zss.api.model;

import org.zkoss.poi.ss.usermodel.Workbook;

/**
 * This interface provides entry to access Spreadsheet's data model.  
 * @author dennis
 * @since 3.0.0
 */
public interface Book {
	public enum BookType {
		//EXCEL_2003, EXCEL_2007
		XLS, XLSX
	}

	/**
	 * Gets the poi book object 
	 * @return the poi book object
	 */
	public Workbook getPoiBook();
	
	/**
	 * Gets the object for synchronized a book.
	 * Note: you shouldn't synchronize a book directly, you have to get the sync object to synchronize it
	 * @return
	 */
	public Object getSync();
	
	/**
	 * Gets the book name
	 * @return tge book name
	 */
	public String getBookName();

	/**
	 * Gets the book type
	 * @return the book type
	 */
	public BookType getType();

	/**
	 * Gets the index of sheet
	 * @param sheet 
	 * @return the index of sheet or -1 if not found
	 */
	public int getSheetIndex(Sheet sheet);

	/**
	 * Gets the number of sheet
	 * @return the number of sheet
	 */
	public int getNumberOfSheets();

	/**
	 * Gets sheet by index
	 * @param index index of sheet
	 * @return
	 */
	public Sheet getSheetAt(int index);

	/**
	 * Gets sheet by sheet name
	 * @param name name of sheet
	 * @return the sheet or null if not found
	 */
	public Sheet getSheet(String name);

	/**
	 * Sets share scope of this book, the possible value is "desktop","session","application"
	 * @param scope  
	 */
	public void setShareScope(String scope);
	
	/**
	 * Gets share scope of this book
	 * @return
	 */
	public String getShareScope();
	
	
	/**
	 * check if this book has named range
	 * @param name the name to check
	 * @return true if it has a range of the name
	 */
	public boolean hasNameRange(String name);
	
	/**
	 * @return the maximum number of usable rows in each sheet
	 */
	public int getMaxRows();
	
	/**
	 * @return the maximum number of usable column in each sheet
	 */
	public int getMaxColumns();

}
