/* CloseBookHandler.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		2013/8/3 , Created by dennis
}}IS_NOTE

Copyright (C) 2013 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package org.zkoss.zss.ui.sys.ua.impl;

import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.UserActionContext;

/**
 * @author dennis
 * @since 3.0.0
 */
public class CloseBookHandler extends AbstractBookHandler{
	@Override
	protected boolean processAction(UserActionContext ctx) {
		Spreadsheet zss = ctx.getSpreadsheet();
		if(zss.getSrc()!=null){
			zss.setSrc(null);
		}
		if(zss.getBook()!=null){
			zss.setBook(null);
		}
		
		ctx.clearClipboard();
		return true;
	}
}
