/* SimpleCellDisplayLoader.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		2013/8/7 , Created by dennis
}}IS_NOTE

Copyright (C) 2013 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package org.zkoss.zss.ui.impl;

import org.zkoss.zss.ngmodel.NSheet;
import org.zkoss.zss.ui.sys.CellDisplayLoader;

/**
 * @author dennis
 * @since 3.0.0
 */
public class SimpleCellDisplayLoader implements CellDisplayLoader {

	/* (non-Javadoc)
	 * @see org.zkoss.zss.ui.sys.RichCellContentLoader#getCellHtmlText(org.zkoss.zss.model.sys.XSheet, int, int)
	 */
	@Override
	public String getCellHtmlText(NSheet sheet, int row, int column) {
		return CellFormatHelper.getCellHtmlText(sheet, row, column);
	}

}