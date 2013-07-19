(function() {
	var gaCategory = "zssdemo";
	var time0 = jq.now();
	
	var getInterval = function(){
		return jq.now()-time0;
	};
	var resetInterval = function(){
		time0 = jq.now();
	}
	
	var ACTIONS = {
		DEMO : 'demo',
		APPMENU : 'appmenu',
	};

	var SS_EVENTS = {
		'onSheetNameChange' : {},
		'onSheetCreate' : {},
		'onSheetDelete' : {},
		'onSheetOrderChange' : {},
		'onSheetSelect' : {},
		'onStartEditing' : {},
		'onHeaderUpdate' : {},
		'onWidgetUpdate' : {}
	};
	var _act;
	var ACTION_INFOS = {
		/*zssdemo*/
		'app0': {action: _act=ACTIONS.DEMO, label: 'App Tab'},
		'app0Src': {action: _act, label: 'App Src Tab'},
		'app2': {action: _act, label: 'H1N1 Demo Tab'},
		'app2Src': {action: _act, label: 'H1N1 Demo Src'},
		/*zssapp file menu*/
		'newFile': {action: _act=ACTIONS.APPMENU}, 
		'openManageFile': {action: _act},
		'saveFile': {action: _act},
		'saveFileAs': {action: _act},
		'saveFileAndClose': {action: _act},
		'closeFile': {action: _act},
		'exportFile': {action: _act},
		'deleteFile': {action: _act},
		'exportFile': {action: _act},
		'exportPdf': {action: _act},
		'toggleFormulaBar': {action: _act},
		'freezePanel': {action: _act},
		'unfreezePanel': {action: _act},
		'freezeRow1': {action: _act},
		'freezeRow2': {action: _act},
		'freezeRow3': {action: _act},
		'freezeRow4': {action: _act},
		'freezeRow5': {action: _act},
		'freezeCol1': {action: _act},
		'freezeCol2': {action: _act},
		'freezeCol3': {action: _act},
		'freezeCol4': {action: _act},
		'freezeCol5': {action: _act}
	};
	
	var trackerEvent = function(action,label,value){
		if(window.pageTracker){
			zk.log("trackerEvent:"+gaCategory+","+action+","+label+","+value);
			pageTracker._trackEvent(gaCategory, action, label, value);
		}
		
	};
	
	var init = function() {
		var auBfSend = zAu.beforeSend;
		zAu.beforeSend = function(uri, req, dt) {
			try {
				var evtnm = req.name;
				var widget = req.target;
				var data = req.data;
				if(widget){
					if(widget.widgetName=='spreadsheet'){
						var action = null;
						var label;
						var interval = getInterval();
						if(evtnm=='onAuxAction'){
							action= "zssAuxEvent-"+data.tag;//sheet, toolbar
							label = data.action;//zss action
						}else if(evtnm=='onCtrlKey'){//only care some other
							action="zssEvent";
							if(data.wgt){
								label = evtnm+"/"+data.wgtType;
							}else{
								label = evtnm;
							}
							
						}else if(SS_EVENTS[evtnm]){//only care some other
							action="zssEvent";
							label = evtnm;
						}
						if(action){
							trackerEvent(action,label,interval);
							resetInterval();
						}
					}else if(evtnm=='onClick'){
						var id = widget.id;
						var extrainfo = null;
						if(widget.label){
							extrainfo = ",label="+widget.label;
						}
						var info = ACTION_INFOS[id];
						if(info){
							var action = info.action;
							var label = info.label;
							if(!!!label){
								label = id;
							}
							if(!!extrainfo){
								label += extrainfo;
							}
							
							var interval = getInterval();
							if(interval>1000){//skip continue events
								resetInterval();
								trackerEvent(action,label,interval);								
							}

						}
					}
				}
			} catch (e) {
			}
			return auBfSend(uri, req, dt);
		};
	};
	zk.afterLoad('zss', function() {
		var count = 0;
		var checkGa = function() {

			if (window.pageTracker) {
				init();
			} else {
				count++;
				if (count < 10) {
					setTimeout(checkGa, 1000);
				}
			}
		};
		setTimeout(checkGa, 1000);
	});
})();