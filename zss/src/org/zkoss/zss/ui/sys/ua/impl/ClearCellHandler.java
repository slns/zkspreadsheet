/* FontFamilyAction.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		2013/8/5 , Created by dennis
}}IS_NOTE

Copyright (C) 2013 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
 */
package org.zkoss.zss.ui.sys.ua.impl;

import org.zkoss.util.resource.Labels;
import org.zkoss.zss.api.CellOperationUtil;
import org.zkoss.zss.api.Range;
import org.zkoss.zss.api.Ranges;
import org.zkoss.zss.api.UnitUtil;
import org.zkoss.zss.api.model.Sheet;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.UserActionContext;
import org.zkoss.zss.undo.ClearCellAction;
import org.zkoss.zss.undo.FontStyleAction;
import org.zkoss.zss.undo.UndoableActionManager;
import org.zkoss.zss.undo.ClearCellAction.Type;

/**
 * @author dennis
 * 
 */
public class ClearCellHandler extends AbstractProtectedHandler {
	ClearCellAction.Type _type;

	public ClearCellHandler(Type type) {
		this._type = type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.zkoss.zss.ui.sys.ua.impl.AbstractHandler#processAction(org.zkoss.
	 * zss.ui.UserActionContext)
	 */
	@Override
	protected boolean processAction(UserActionContext ctx) {
		Sheet sheet = ctx.getSheet();
		Rect selection = ctx.getSelection();
		Range range = Ranges.range(sheet, selection);
		String label = null;
		switch (_type) {
		case ALL:
			label = Labels.getLabel("zss.undo.clearContents");
			break;
		case CONTENT:
			label = Labels.getLabel("zss.undo.clearAll");
			break;
		case STYLE:
			label = Labels.getLabel("zss.undo.clearStyles");
			break;
		}
		UndoableActionManager uam = ctx.getSpreadsheet()
				.getUndoableActionManager();
		uam.doAction(new ClearCellAction(label, sheet, selection.getRow(),
				selection.getColumn(), selection.getLastRow(), selection
						.getLastColumn(), _type));
		return true;
	}

}
