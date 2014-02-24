package org.zkoss.zss.ngmodel.impl;

import java.io.Serializable;
import java.util.Collection;

import org.zkoss.zss.ngmodel.ErrorValue;
import org.zkoss.zss.ngmodel.NCell.CellType;
import org.zkoss.zss.ngmodel.NCellValue;
import org.zkoss.zss.ngmodel.sys.formula.EvaluationResult;
import org.zkoss.zss.ngmodel.sys.formula.EvaluationResult.ResultType;

/**
 * the formula result cell value
 * @author dennis
 *
 */
public class FormulaResultCellValue extends NCellValue implements Serializable {
	private static final long serialVersionUID = 1L;

	public FormulaResultCellValue(EvaluationResult result) {
		Object val = result.getValue();
		ResultType type = result.getType();
		if (type == ResultType.ERROR) {
			cellType = CellType.ERROR;
			value = (val instanceof ErrorValue) ? (ErrorValue) val
					: new ErrorValue(ErrorValue.INVALID_VALUE);
		} else if (type == ResultType.SUCCESS) {
			setByValue(val);
		}
	}

	private void setByValue(Object val) {
		if (val == null || "".equals(val)) {
			cellType = CellType.BLANK;
			value = null;
		} else if (val instanceof String) {
			cellType = CellType.STRING;
			value = (String) val;
		} else if (val instanceof Number) {
			cellType = CellType.NUMBER;
			value = (Number) val;
		} else if (val instanceof Boolean) {
			cellType = CellType.BOOLEAN;
			value = (Boolean) val;
		} else if (val instanceof Collection) {
			// possible a engine return a collection in cell evaluation case?
			// who should take care array formula?
			if (((Collection) val).size() > 0) {
				setByValue(((Collection) val).iterator().next());
			} else {
				cellType = CellType.BLANK;
				value = null;
			}
		} else if (val.getClass().isArray()) {
			// possible a engine return a collection in cell evaluation case?
			// who should take care array formula?
			if (((Object[]) val).length > 0) {
				setByValue(((Object[]) val)[0]);
			} else {
				cellType = CellType.BLANK;
				value = null;
			}
		} else {
			cellType = CellType.ERROR;
			value = (val instanceof ErrorValue) ? (ErrorValue) val
					: new ErrorValue(ErrorValue.INVALID_VALUE,
							"Unknow value type " + val);
		}
	}

	public CellType getCellType() {
		return cellType;
	}

	public Object getValue() {
		return value;
	}
}