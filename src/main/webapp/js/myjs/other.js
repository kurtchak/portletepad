
// METHOD UNDERTOOK from 
// [http://stackoverflow.com/questions/1335252/how-can-i-get-the-dom-element-which-contains-the-current-selection]
function getSelectionBoundaryElement(isStart) {
    var range, sel, container;
    if (document.selection) {
        range = document.selection.createRange();
        range.collapse(isStart);
        return range.parentElement();
    } else {
        sel = window.getSelection();
        if (sel.getRangeAt) {
            if (sel.rangeCount > 0) {
                range = sel.getRangeAt(0);
            }
        } else {
            // Old WebKit
            range = document.createRange();
            range.setStart(sel.anchorNode, sel.anchorOffset);
            range.setEnd(sel.focusNode, sel.focusOffset);

            // Handle the case when the selection was selected backwards (from the end to the start in the document)
            if (range.collapsed !== sel.isCollapsed) {
                range.setStart(sel.focusNode, sel.focusOffset);
                range.setEnd(sel.anchorNode, sel.anchorOffset);
            }
       }

        if (range) {
           container = range[isStart ? "startContainer" : "endContainer"];

           // Check if the container is a text node and return its parent if so
           return container.nodeType === 3 ? container.parentNode : container;
        }   
    }
}
//METHOD UNDERTOOK


//NOT SAFE 
//		function updatePlainText() {
//			var e = doc.getElementById('editor');
//			var nodes = e.childNodes;
//			var str = "";
//			for (var i=0; i<nodes.length; i++) {
//				if (nodes[i].nodeType == 1) {
//					if (nodes[i].tagName == "SPAN" && nodes[i].getAttribute("id") != "caret") {
//						str += nodes[i].innerHTML;
//					} else if (nodes[i].tagName == "BR") {
//						str += '\n';
//					}
//				}
//			}
//			console.log(str);
//		}
