/*

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		2013/12/01 , Created by dennis
}}IS_NOTE

Copyright (C) 2013 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package org.zkoss.zss.ngmodel;

/**
 * 
 * @author dennis
 * @since 3.5.0
 */
public interface NCellStyle {

	public static final String FORMAT_GENERAL = "General";
	
	/**
	 * The background color fill pattern
	 */
	public enum FillPattern {
		NO_FILL, SOLID_FOREGROUND, FINE_DOTS, ALT_BARS, SPARSE_DOTS, THICK_HORZ_BANDS, THICK_VERT_BANDS, THICK_BACKWARD_DIAG, THICK_FORWARD_DIAG, BIG_SPOTS, BRICKS, THIN_HORZ_BANDS, THIN_VERT_BANDS, THIN_BACKWARD_DIAG, THIN_FORWARD_DIAG, SQUARES, DIAMONDS, LESS_DOTS, LEAST_DOTS
	}

	/**
	 * The horizontal alignment
	 *
	 */
	public enum Alignment {
		GENERAL, LEFT, CENTER, RIGHT, FILL, JUSTIFY, CENTER_SELECTION
	}

	/**
	 * The vertical alignment
	 */
	public enum VerticalAlignment {
		TOP, CENTER, BOTTOM, JUSTIFY
	}

	/**
	 * The border type
	 */
	public enum BorderType {
		NONE, THIN, MEDIUM, DASHED, HAIR, THICK, DOUBLE, DOTTED, MEDIUM_DASHED, DASH_DOT, MEDIUM_DASH_DOT, DASH_DOT_DOT, MEDIUM_DASH_DOT_DOT, SLANTED_DASH_DOT;
	}
	
	
	/**
	 * @return background-color
	 */
	public NColor getFillColor();

	/**
	 * Gets the fill/background pattern <br/>
	 * @return the fill pattern
	 */
	public FillPattern getFillPattern();

	/**
	 * Gets the horizontal alignment <br/>
	 * @return the horizontal alignment
	 */
	public Alignment getAlignment();

	/**
	 * Gets vertical alignment <br/>
	 * @return
	 */
	public VerticalAlignment getVerticalAlignment();

	/**
	 * @return true if wrap-text
	 */
	public boolean isWrapText();

	/**
	 * @return left border
	 */
	public BorderType getBorderLeft();

	/**
	 * @return top border
	 */
	public BorderType getBorderTop();

	/**
	 * @return right border
	 */
	public BorderType getBorderRight();

	/**
	 * @return bottom border
	 */
	public BorderType getBorderBottom();

	/**
	 * @return top border color
	 */
	public NColor getBorderTopColor();

	/** 
	 * @return left border color
	 */
	public NColor getBorderLeftColor();

	/**
	 * 
	 * @return bottom border color
	 */
	public NColor getBorderBottomColor();

	/**
	 * @return right border color
	 */
	public NColor getBorderRightColor();
	
	/**
	 * @return data format
	 */
	public String getDataFormat();
	
	/**
	 * 
	 * @return true if the data format is direct data format, which mean it will not care Locale when formatting
	 */
	public boolean isDirectDataFormat();
	
	/**
	 * 
	 * @return true if locked
	 */
	public boolean isLocked();
	
	/**
	 * 
	 * @return true if hidden
	 */
	public boolean isHidden();
	

	public void setFillColor(NColor fillColor);

	public void setFillPattern(FillPattern fillPattern);
	
	public void setAlignment(Alignment alignment);
	
	public void setVerticalAlignment(VerticalAlignment verticalAlignment);

	public void setWrapText(boolean wrapText);

	public void setBorderLeft(BorderType borderLeft);
	public void setBorderLeft(BorderType borderLeft,NColor color);

	public void setBorderTop(BorderType borderTop);
	public void setBorderTop(BorderType borderTop,NColor color);

	public void setBorderRight(BorderType borderRight);
	public void setBorderRight(BorderType borderRight,NColor color);

	public void setBorderBottom(BorderType borderBottom);
	public void setBorderBottom(BorderType borderBottom,NColor color);

	public void setBorderTopColor(NColor borderTopColor);

	public void setBorderLeftColor(NColor borderLeftColor);

	public void setBorderBottomColor(NColor borderBottomColor);

	public void setBorderRightColor(NColor borderRightColor);

	public void setDataFormat(String dataFormat);
	
	public void setDirectDataFormat(String dataFormat);
	
	public void setLocked(boolean locked);

	public void setHidden(boolean hidden);
	
	public NFont getFont();
	
	public void setFont(NFont font);

	public void copyFrom(NCellStyle src);
}