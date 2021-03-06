

public class SS_173_Test extends SSAbstractTestCase {

    @Override
    protected void executeTest() {
    	setTimeout("30000");
    	selectCells(1,5,5,8);
    	click("jq('$fileMenu button.z-menu-btn')");
    	waitResponse();
    	click("jq('@menu[label=\"Export\"] a.z-menu-cnt-img')");
    	waitResponse();
    	click("jq('$exportPdf a.z-menu-item-cnt')");
    	waitResponse();
    	
    	// TODO verify if open file window is opened
    	verifyTrue(widget(jq("@window[title=\"Export to PDF\"]")).exists());
    	
    	click("jq('$currSelection label.z-radio-cnt')");
    	waitResponse();
    	click("jq('$noHeader label.z-checkbox-cnt')");
    	waitResponse();
    	click("jq('$export td.z-button-cm')");
    	waitResponse();
    }
}
