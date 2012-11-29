var doc = window.document;
var oldText = " ";
var newText = " ";
var caretPos = 0;
var editor = {};
var caret = {};
var readOnly = false;
var pool = new AttribPool();
var userColor = "";
var nextSpanId = 0;
var userId = 0;
var padId = 0;

// remote action sync elements
var nextRemoteChangeButton = {};

function setReadOnly(ro) {
	readOnly = ro == null || ro == 'false' ? false : true;
}

function processMessage(message) {
	console.log("Message received: "+message);
	var changeset = message.match(/^@\[p(\d+)u(\d+)\](C.*)$/);
	var colorChange = message.match(/^UC:(.*)to(.*)$/);
	if (changeset != null) {
		console.log("changeset: "+changeset);
		var pId = changeset[1];
		var uId = changeset[2];
		var body = changeset[3];
		if (uId != userId || pId != padId) {
			console.log("Not intended...");
			return;
		} else {
			writeRemoteChange(body);
		}
	} else if (colorChange) {
		var fromColor = colorChange[1];
		var toColor = colorChange[2];
		adjustColor2(fromColor, toColor);
	} else {
		return;
	}
	console.log("Message processed: "+message);
}

function adjustColor2(fromColor, toColor) {
	console.log(">>"+fromColor + " to "+ toColor);
//	$('#editor').find('span[class="cl'+fromColor.substring(1)+'"]').toggleClass("cl"+fromColor.substring(1)).toggleClass("cl"+toColor.substring(1));
	$('.cl'+fromColor.substring(1)).toggleClass("cl"+fromColor.substring(1)).toggleClass("cl"+toColor.substring(1));
}

function prepareEditorContent() {
	console.log("prepare");
	editor = $('#editor');
	if (editor != null) {
		translate();
		console.log("readOnly:"+readOnly);
		if (!readOnly) {
			caret = $('#caret');
			$(editor).keypress(function(e) { processKeyPress(e); });
			$(editor).keyup(function(e) { processKeyUp(e); });
			$(editor).click(function(e) { processClick(e); });
			$(editor).focus();
			nextRemoteChangeButton = $("#anchorForChangeAction").next();
		}
	}
}

/**
 * transform encoded text posted by bean into html elements
 */
function translate() {
	editor = $('#editor');
	var text = $.trim($(editor).text()); // read the text representation sent by the server
	var spanText = "";
	newText = "";
	$(editor).text("").css("color","black");
	while (text) {
		var fields = text.match(/^(\d)(_(#[0-9a-fA-F]{6})_(\d+)_(\d+):(.*))?(.*)/);
		if (fields != null) {
			if (isTextSpanType(fields[1])) {
				spanText = fields[6].substr(0,fields[5]); // n character from the 6. field
				text = fields[6].substring(fields[5]); // all left
				$(editor).append(newTextSpan(fields[3],fields[4],spanText)); // newTextSpan(spanColor, spanNum, spanText)
				newText += spanText;
			} else if (isNewLineType(fields[1])) {
				text = fields[7]; // all left
				$(editor).append(newNewLineElem());
				newText += "\n";
			}
		} else {
			return;
		}
	}
	$(editor).append(createCaret());
	caretPos = newText.length;
	oldText = newText;
}

// PROCESSING THE KEY PRESS EVENT OCCURED WITHIN THE EDITOR
function processKeyPress(e) {
	console.log("<<KEYPRESS>> : " + keyCode(e) + " - " + e.which + " - " + getChar(e));
	e.preventDefault();
	if (isPrintable(e)) {
		console.log("::KEYPRESS EVENT::PRINTABLE");
		writeDown(e);
	} else {
		console.log("::KEYPRESS EVENT::NOT PRINTABLE");
	}
	e.stopPropagation();
}

//PROCESSING THE KEY UP EVENT OCCURED WITHIN THE EDITOR
function processKeyUp(e) {
	console.log("<<KEYUP>> : " + keyCode(e) + " - " + e.which + " - " + getChar(e));
	e.preventDefault();
	if (isBackspace(e)) {
		console.log("::KEYUP EVENT::BACKSPACE");
		deleteChar(e);
	} else if (isDeleteKey(e)) {
		console.log("::KEYUP EVENT::DELETE KEY");
		deleteChar(e);
	} else if (isArrow(e)) {
		console.log("::KEYUP EVENT::ARROW");
		moveCaret(e);
//	} else if (isPrintable(e)) {
//		console.log("::KEYUP EVENT::PRINTABLE");
//		writeDown(e);
	} else {
		console.log("::KEYUP EVENT::NOT BACKSPACE NOR ARROW");
	}
	e.stopPropagation();
}

//PROCESSING THE MOUSE CLICK EVENT OCCURED WITHIN THE EDITOR
function processClick(e) {
	console.log("::CLICK EVENT (EDITOR)");
	e.preventDefault();
	placeCaret(e);
	e.stopPropagation();
}

function writeRemoteChange(changeInfo) {
	console.log("writeRemoteChange: "+changeInfo);
	var fields = changeInfo.match(/^C:([WDA]):(\d+):(\d+):(.*):(\d*):(\d*):(\d*):(#[0-9a-fA-F]{6})/);
	if (fields == null) {
		return;
	}
	logRemoteNotifier(fields);
	
	var remoteAction = fields[1];
	var remoteSpanId = fields[2];
	var remoteSpanPos = fields[3];
	var remoteCharbank = fields[4].length <= 0 ? "" : fields[4];
	var remoteLeftId = fields[5];
	var remoteRightId = fields[6];
	var remoteOffset = fields[7];
	var remoteFgColor = fields[8];

	var elem = getTextSlice(remoteSpanId);

	if (remoteAction == "W") {
		newText = newText.substring(0, remoteOffset) + remoteCharbank + newText.substring(remoteOffset);
	} else if (remoteAction == "D") {
		newText = newText.substring(0, remoteOffset) + newText.substring(remoteOffset+1);
	}
	
	if (!isNull(elem)) { console.log('TARGET ELEMENT('+($(elem).length)+'): '+getSpanIdNumber(elem)+":"+$(elem).text()); }
	else { console.log('TARGET ELEMENT =>> NOT FOUND'); }

	if (!isNull(elem)) { // found element with such id
		var part = getCurrentTextSlicePart(elem, remoteSpanPos);
		var elem = part.elem;
		remoteSpanPos = part.pos;
		console.log('TARGET ELEMENT:'+$(elem).prop('id')+" => "+$(elem).text() + "["+remoteSpanPos+"]");
		if (remoteSpanPos >= 0) { //TODO
			var str = $(elem).text();
			if (remoteAction == 'W') {
				if (remoteSpanId == 0) { // newLine was entered
					$(elem).text(str.substring(0,remoteSpanPos));
					var br = createNewTextSlice('br');
					var span = createNewTextSlice('span', getFgColor(elem), getNextSpanId(), str.substring(remoteSpanPos));
					$(br).insertAfter(elem);
					$(span).insertAfter(br);
				} else { console.log("$(elem).text(str.substring(0,"+(remoteSpanPos)+") + " + remoteCharbank + " + str.substring("+(remoteSpanPos)+") => "+(str.substring(0,remoteSpanPos) + remoteCharbank + str.substring(remoteSpanPos)));
					$(elem).text(str.substring(0,remoteSpanPos) + remoteCharbank + str.substring(remoteSpanPos));
				}
			} else if (remoteAction == 'D') { console.log("DELETING - NOW IMPLEMENTED");
				if (remoteSpanPos < str.length) { console.log("remoteSpanPos < str.length: "+remoteSpanPos +":"+ str.length);
					$(elem).text(str.substring(0,remoteSpanPos) + str.substring(parseInt(remoteSpanPos)+1)); console.log("str.substring(0,"+remoteSpanPos+") = " + str.substring(0,remoteSpanPos)); console.log("str.substring("+(parseInt(remoteSpanPos)+1)+") = " + str.substring(parseInt(remoteSpanPos)+1));
					if ($(elem).text().length == 0) { console.log("str.length == 0 => DELETING WHOLE SPAN");
						var prev = $(elem).prev();
						var next = $(elem).next();
						$(elem).remove();
						if (hasSameToken(prev, next)) {
							mergeSlices(prev, next);
						}
					}
				} else { // synchronize .. rebuild + translate + placeCaret
					alert('synchronize .. rebuild + translate + placeCaret ?');
				}
				return;
			} else if (remoteAction == 'A') {
				alert("ATTRIBUTE CHANGE - NOT IMPLEMENTED");
			} else {
				alert("UNKNOWN ACTION: "+ remoteAction);
				return;
			}
		} //console.log("(after): "+ $(elem) + " | text() -> " +$(elem).text());
	} else if (remoteLeftId > 0) { console.log('REMOTE LEFT TAKEN');
		var elem = getTextSlice(remoteLeftId);
		var leftAtom = getCurrentTextSlicePart(elem, remoteSpanPos);
		var left = leftAtom.elem;
		remoteSpanPos = leftAtom.pos;
		if (!isNull(left)) { // found neighbour
			if (remoteSpanPos <= $(left).text().length) {
				if (remoteSpanPos > 0) {
					var rightStr = $(left).text().substring(remoteSpanPos);
					$(left).text($(left).text().substring(0,remoteSpanPos));
					var right = createNewTextSlice('span', getFgColor(left), remoteRightId, rightStr);
					$(right).insertAfter(left); // TODO: insertIntoElem(elem, pos, elemInserted)
				}
				updateNextSpanId(remoteSpanId);
				var n = createNewTextSlice('', remoteFgColor, remoteSpanId, remoteCharbank); console.log("inserting after left neighbour...");
				$(n).insertAfter(left); // TODO: insertIntoElem(elem, pos, elemInserted)
			} else {
				alert('RemoteSpanPos('+remoteSpanPos+') exceedes length('+$(left).text().length+') of element.');
			}
		} else {
			alert('NOT FOUND LEFT SIBLING');
		}
	} else if (remoteRightId > 0) { console.log('REMOTE RIGHT TAKEN');
		var right = getTextSlice(remoteRightId);
		console.log(remoteRightId);
		console.log($(right));
		if (!isNull(right)) { // found neighbour
			if (remoteAction == "W") {
				updateNextSpanId(remoteSpanId);
				var n = createNewTextSlice('', remoteFgColor, remoteSpanId, remoteCharbank); console.log("inserting before right neighbour...");
				$(n).insertBefore($(right).get(0)); // TODO: insertIntoElem(elem, pos, elemInserted)
			} else if (remoteAction == "D") {
//				alert("Action D yet implemented");
				console.log($(right));
				console.log($(right).prev());
				if (!isNull($(right).prev())) {
					alert("deleting:"+$(right).prev());
					$($(right).prev()).remove();
				}
			} else if (remoteAction == "A") {
				alert("Action A not implemented yet");
			} else {
				alert("Unknown action: "+remoteAction);
			}

		} else {
			alert('NOT FOUND RIGHT SIBLING');
		}
		
	} else if ($('#editor').find('span').length <= 1) { console.log('EMPTY EDITOR - FIRST ELEMENT IN IT');
		updateNextSpanId(remoteSpanId);
		var n = createNewTextSlice('', remoteFgColor, remoteSpanId, remoteCharbank); console.log("inserting the first element...");
		$(n).insertBefore($('#caret')); // TODO: insertIntoElem(elem, pos, elemInserted)
	} else if (remoteOffset >= 0) {
		console.log('WITHOUT SPAN NEIGHBOURS '+ (remoteCharbank == null) + ","+ (remoteCharbank == "") +","+ (remoteCharbank != null));
		var elem = $('#editor').find(':first');
		while (textLength(elem) < remoteOffset) {
			remoteOffset -= textLength(elem);
			elem = $(elem).next().prop('id') != 'caret' ? $(elem).next() : $(elem).next().next();
		}
		if (remoteAction == "W") {
			updateNextSpanId(remoteSpanId);
			var n = createNewTextSlice('', remoteFgColor, remoteSpanId, remoteCharbank); console.log("inserting after left (br) neighbour...");
			console.log($(n));
			console.log('insertAfter');
			console.log($(elem));
			$(n).insertAfter(elem); // TODO:suppossing it is located between another two newLines
		} else if (remoteAction == "D") {
			$(elem).remove();
		} else if (remoteAction == "A") {
			alert("action A not implemented");
		} else {
			alert("unknown action: "+remoteAction);
		}
	} else {
		alert('dajaka chyba');
	}
	updateCaretPos();
	processNext();
}

function getCurrentTextSlicePart(elem, pos) {
	var elemPart = $(elem).get(0);
	console.log('FOUND MORE ELEMENTS:'); for (var i=0; i<$(elem).length; i++) console.log('SPAN: '+$($(elem).get(i)).prop('id')+" => "+$($(elem).get(i)).text());
	for (var i=0; i<$(elem).length && ($($(elem).get(i)).text().length < pos); i++) {
		pos -= $($(elem).get(i)).text().length;
		elemPart = $(elem).get(i+1);
	}
	return { 'elem':elemPart, 'pos': pos };
}

function textSliceExists(spanIdNumber) {
	return $("#t_s_"+spanIdNumber).length > 0 ? true : $("#t_s_"+spanIdNumber+"a").length > 0;
}

function getTextSlice(spanIdNumber) {
	console.log("getTextSlice:"+spanIdNumber);
	var elem = null;
	if (textSliceExists(spanIdNumber)) {
		elem = $("#t_s_"+spanIdNumber);
		console.log("elem1:"+(isNull(elem) ? "null" : $(elem).text()));
		if (isNull(elem)) {
			elem = $("#t_s_"+spanIdNumber+"a,#t_s_"+spanIdNumber+"b");
			console.log("elem2:"+(isNull(elem) ? "null" : $(elem).text()));
		}
	}
	return elem;
}

function logRemoteNotifier(fields) {
//	console.log("logRemoteNotifier()");
	console.log(
			"REMOTE CHANGE OCCURED: " + fields[1]
			+ "-" + fields[2]
			+ "-" + fields[3]
			+ "-" + (fields[4].length <= 0 ? "" : fields[4])
			+ "-" + fields[5]
			+ "-" + fields[6]
			+ "-" + fields[7]
			+ "-" + fields[8]);
}

function processNext() {
	console.log("processNext()");
	$(nextRemoteChangeButton).click();
}

// types of text to be parsed
function isTextSpanType(type) {
	return type == "0";
}

function isNewLineType(type) {
	return type == "1";
}

function isCaretType(type) {
	return type == "3";
}

// creating elements from the parsed text
function newNewLineElem() {
	console.log('<br>');
	return createNewTextSlice("br");
}

function newTextSpan(spanColor, spanNum, spanText) {
	console.log("<"+spanColor+":"+spanNum+":"+spanText+">");
	return createNewTextSlice('span', spanColor, spanNum, spanText);
}
function createCaret() {
	var e = document.createElement("SPAN");
	e.setAttribute("id", "caret");
	e.setAttribute("style", 'background-color:'+userColor+'; border-style:solid; border-color:black; border-width:0 0 0 1px; width:1px;');
	return e;
}

// determine position of window selection and places the caret element into
// position
function placeCaret(e) {
	var elem = getSelectedElement(e); //console.log('found - elem coords: ' + elem.$(elem).text() + " - " + elem.offset);
	if (isCaret(elem.elem)) {
		console.log('IS CARET');
		return;
	} else {
		try {
			placeCaretWithinElement(elem.elem, elem.offset);
		} catch (err) {
			$("#editor").append($("#caret"));
			updateCaretPos();
		}
	}
}

function getSelectedElement(e) {
	var selection = getTextSelection();
	if (selection == null) {
		return;
	} else {
		var node = !window.opera ? getSelectedNode(selection) : getTextSliceAndPosByClick(e);
		var offset = !window.opera ? getSelectionOffset(selection) : 0;
		console.log("node:" + node);
		console.log("offset:" + offset);
		var elem = !window.opera ? node.parentNode : $(node).get(0);
		console.log("node:" + node + " | elem:" + elem + " offset:" + offset);
		if (isTextSpan(elem)) {
			return { 'elem' : elem, 'offset' : offset };
		} else if (isEmptySpace(node)) {
			console.log('is EMPTY AREA');
			elem = getLastTextSpanOnLine(e);
			console.log('elem: '+elem.get(0));
			return { 'elem' : elem.get(0), 'offset' : elem.text().length };
		} else if (isCaret(elem)) {
			return { 'elem' : elem, 'offset' : 1 };
		} else {
			console.log('is UNKNOWN ELEMENT');
			return;
		}
	}
}

function getTextSelection() {
	if (window.getSelection) {
		console.log("window.getSelection: "+ window.getSelection);
		return window.getSelection();
	} else if (document.getSelection) {
		console.log("document.getSelection: "+ document.getSelection);
		return document.getSelection();
	} else if (document.selection) {
		console.log("document.selection: "+ document.selection);
		return document.selection.createRange();
	} else {
		console.log("NO SELECTION???");
		return null;
	}
}

function getSelectedNode(selection) {
	if (window.opera) {
		if (selection.getRangeAt && selection.rangeCount > 0) {
			return selection.getRangeAt(0).startContainer;
		} else {
		 	var range = selection.createRange();
		 	return range.startContainer;
	 	}
	} else {
		return selection.anchorNode;
	}
}

function getSelectionOffset(selection) {
	if (window.opera) {
		if (selection.getRangeAt && selection.rangeCount > 0) {
			return selection.getRangeAt(0).startOffset;
		} else {
		 	var range = selection.createRange();
		 	return range.startOffset;
	 	}
	} else {
		return selection.anchorOffset;
	}
}

function isCaret(elem) {
	return !isNull(elem) && $(elem).prop('id') == 'caret';
}

function isTextSpan(elem) {
	console.log("IS TEXT SPAN ? "+($(elem).is('span') && $(elem).prop('id').substring(0,4) == 't_s_'));
	return $(elem).is('span') && $(elem).prop('id').substring(0,4) == 't_s_';
}

function isEmptySpace(elem) {
	return elem.tagName == 'DIV';
}

function getLastTextSpanOnLine(e) {
	console.log("selection: "+ e.clientX + ":"+e.clientY);
	var lastLineElem = findLineEnd(e.clientY);
	console.log("RETURNED ELEMENT: "+ lastLineElem.text());
	return lastLineElem;
}

function getFirstTextSpanOnLine(e) {
	console.log("selection: "+ e.clientX + ":"+e.clientY);
	var tail = findLineEnd(e.clientY);
	var head = $(tail).prevAll("br");
	if ($(head).length > 0) {
		head = $(head).last().next(); // closest br's right neighbour
	} else {
		head = $("#editor").first();
	}
	return head;
}

function getTextSliceAndPosByClick(e) {
	var x = e.clientX;
	var tail = getLastTextSpanOnLine(e);
	while (!isNewLine(tail)) {
		console.log("x:"+x+" ?== $(tail).offset().left:"+$(tail).offset().left);
		if (x >= $(tail).offset().left) {
			return tail;
		}
		tail = $(tail).prev();
	}
}

function findLineEnd(coordY) {
	var brs = getAllLineEnds();
	console.log($(brs).length+": "+$(brs).get());
	var elem = null;
	// SKAREDY HACK - offset sa po case zmensil o 165 bodov od povodneho
	// NEFUNGUJE posun offsetu je rozny
//	if (coordY < $(brs[0]).offset().top) {
//		coordY += 165;
//	}
	for (var i = 0; i < $(brs).length; i++) {
		console.log("("+i+")");
		if (coordY > $(brs[i]).offset().top) {
			console.log(i+" from "+$(brs).length+ " >> "+coordY +" : "+ $(brs[i]).offset().top);
			elem = $(brs[i]);
		} else {
			console.log("END "+ coordY +" : "+ $(brs[i>0?i-1:i]).offset().top);
			break;
		}
	}
	if (elem == null) {
		console.log("ERROR: END RETURN elem is null");
		return;
	} else {
		if ($(elem).prop('tagName') == "BR") {
			console.log("BR ELEM "+ elem.get() +"->prev:"+$(elem).prev().get());
			elem = $(elem).prev();
		}
		console.log("END RETURN "+ coordY +" : "+ $(elem).offset().top);
		return elem;
	}
}

function getAllLineEnds() {
	return $('#editor').find('br,span:not("#caret"):last').get(); // last element of the editor
}

// return length of the specified node
function textLength(e) {
	return (e == null ? 0 : (isNewLine(e) ? 1
			: ($(e).prop('tagName') == 'SPAN' ? $(e).text().length : 0)));
}

function createNewLine() {
	return document.createElement('br');
}

// creates new element for written text
function createNewTextSlice(tag, fgColor, spanIdNumber, text) {
	// e =
	// $('<span>').appendTo(editor).html(text.substring(11,text.indexOf('|')-1)).css({
	// 'backgroundColor': text.substring(2,9)
	// });
	console.log("2_createNewTextSlice: " + tag + ":" + fgColor + ":" + spanIdNumber + ":" + text);
	if (tag == 'br' || (tag == '' && spanIdNumber == 0)) {
		return document.createElement('br');
	} else {
		var n = document.createElement('span');
		if (fgColor == null) {
			fgColor = userColor;
		}
//		n.setAttribute("style", "background-color: "+fgColor);
		n.setAttribute("class", "cl"+fgColor.substring(1));
		setSpanId(n, spanIdNumber);
		$(n).text(text);
		return n;
	}
}

// within the elements
// node is in this case textNode subelement of span element, therefore im using
// editor reference instead of parentNode
function placeCaretWithinElement(elem, offset) {
	console.log('placeCaretWithinElement:'+elem+'|'+elem.tagName+'|'+$(elem).text()+'|'+offset);
	editor = $("#editor");
	caret = !isNull($("#caret")) ? $("#caret") : createCaret();
	// component to clarifies walk-through the
	// editor elements text
	// selected end of node's text -> just moving the caret behind the node
	var oldLeft = $(caret).prev(); // caret's previous left neighbour
	console.log("IS EMPTY LEFT OBJECT ? " + isNull(oldLeft) + ":"+(!isNull(oldLeft) ? $(oldLeft).text() : ""));
	var oldRight = $(caret).next();// caret's previous right neighbour
	console.log("IS EMPTY RIGHT OBJECT ? " + isNull(oldRight) + ":"+(!isNull(oldRight) ? $(oldRight).text() : ""));
	if (offset == $(elem).text().length) {
		console.log('#1 offset == $(elem).text().length');
		if (isNewLine(elem) || getSpanId(oldLeft) != getSpanId(elem)) {
			$(caret).insertAfter(elem);
			rejoinSplittedSlices(editor, oldLeft, oldRight);
		}
		// selected start of node's text -> just moving the caret before the
		// node
	} else if (offset == 0) {
		console.log('#2 offset == 0');
		if (getSpanId(oldRight) != getSpanId(elem)) {
			$(caret).insertBefore(elem);
			rejoinSplittedSlices(editor, oldLeft, oldRight);
		}
		// selected somewhere in the middle of node's text -> need splitting the
		// node into two text slices and putting the caret between them
	} else {
		console.log('#3 else');
		var a = createNewTextSlice("SPAN", getFgColor(elem), getSpanIdNumber(elem), ""); // TODO:TODO
		var b = elem;
		$(a).text($(b).text().substring(0, offset));
		$(b).text($(b).text().substring(offset));
		$(a).insertBefore(b);
		$(caret).remove(); // removing the caret from within the editor
		var leftMerge = null, rightMerge = null;
		if (!isNull(oldLeft) && !isNull(oldRight))  {
			if (getSpanIdNumber(b) != getSpanIdNumber(oldLeft)) { // previous caret pos was within the same textSlice
				console.log("1 -> REJOINSPLITTEDSLICES(editor, oldLeft, oldRight): "+$(oldLeft)+","+$(oldRight));
				rejoinSplittedSlices(editor, oldLeft, oldRight);
			} else {
				console.log("2a -> REJOINSPLITTEDSLICES(editor, $(left).prev(), left): "+$(a).prev().text()+","+$(a).text());
				leftMerge = rejoinSplittedSlices(editor, $(a).prev(), a);
				console.log("LEFTMERGE: "+leftMerge+" => "+$(leftMerge).text());
				console.log("2b -> REJOINSPLITTEDSLICES(editor, right, $(right).next()): "+$(b).text()+","+$(b).next().text());
				rightMerge = rejoinSplittedSlices(editor, b, $(b).next());
				console.log("RIGHTMERGE: "+rightMerge+" => "+$(rightMerge).text());
			}
		}
		if (leftMerge == null) {
			leftMerge = a;
		}
		if (rightMerge == null) {
			rightMerge = b;
		}
		setSpanId(leftMerge, getSpanIdNumber(leftMerge)+"a");
		console.log("leftMerge.getAttribute(\"id\") = "+ $(leftMerge).prop("id")+ " : "+$(leftMerge).text());
		setSpanId(rightMerge, getSpanIdNumber(rightMerge)+"b");
		console.log("rightMerge.getAttribute(\"id\") = "+ $(rightMerge).prop("id")+ " : "+$(rightMerge).text());
		$(caret).insertBefore(rightMerge);
	}
	updateCaretPos();
}

function isDefined(attr) {
	return (typeof attr !== 'undefined' && attr !== false);
}

function getFgColor(elem) {
	var cls = $(elem).attr("class");
	if (isDefined(cls)) {
		return "#"+cls.replace("cl","");
	}
}

function isNull(a) {
	return a == null || $(a).length == 0;
}

// a must precedes b
function mergeSlices(a, b) {
	console.log("MERGESLICES("+a+","+b+")");
	if (!hasSameToken(a, b)) {
		alert("Given slices: "+$(a)+", "+$(b)+" cannot be merged, because they doesn't have same token.");
	} else if (isCaret($(a).next()) && isCaret($(b).prev())) {
		console.log("splitted by caret");
		setSpanId(a, getSpanIdNumber(a)+"a");
		setSpanId(b, getSpanIdNumber(a)+"b");
	} else if (areNeighbours(a, b)) {
		console.log("areNeighbours");
		$(a).text($(a).text()+$(b).text());
		$(b).remove();
		console.log("NEW MERGED SLICE:"+getSpanIdNumber(a)+":"+$(a).text());
	} else {
		alert("Given slices: "+$(a)+", "+$(b)+" cannot be merged, because they doesn't neigbour.");
	}
}

function areNeighbours(a, b) {
	console.log("ARE A AND B NEIGHBOURS ? "+(!isNull(a) && !isNull(b) && getSpanId($(a).next()) == getSpanId(b) ? "YES" : "NO"));
	return !isNull(a) && !isNull(b) && getSpanId($(a).next()) == getSpanId(b);
}
function rejoinSplittedSlices(parent, a, b) {
	console.log("REJOINSPLITTEDSLICES("+parent+","+a+","+b+")");
	if (!isNull(a) && !isNull(b)) {console.log("a => "+$(a).text()+"\tb => "+$(b).text());}
	if (isNull(b) || isNewLine(b)) {
		console.log("1.rejoinSplittedSlices("+parent+", b:"+b+", ...)");
		setSpanId(a, getSpanIdNumber(a)); // clear trailing 'b'
		return a;
	} else if (isStartBound(a) || isNewLine(a)) {
		console.log("2.rejoinSplittedSlices("+parent+", a:"+a+", ...)");
		setSpanId(b, getSpanIdNumber(b)); // clear trailing 'a'
		return b;
	} else if (!wasSplitted(a, b)) {
		console.log("3.rejoinSplittedSlices("+parent+","+$(a).prop("id")+":"+$(a).text()+","+$(b).prop("id")+":"+$(b).text()+")");
		setSpanId(a, getSpanIdNumber(a)); // clear trailing 'b'
		setSpanId(b, getSpanIdNumber(b)); // clear trailing 'a'
		return;
	} else {
		console.log("4.rejoinSplittedSlices("+parent+","+$(a).prop("id")+":"+$(a).text()+","+$(b).prop("id")+":"+$(b).text()+")");
		setSpanId(a, getSpanIdNumber(a)); // clear trailing 'a'
		$(a).text($(a).text() + $(b).text());
		$(b).remove(); // after joining of right part's text remove this part
		console.log('REJOINED SPAN: ' + $(a).prop("id") + ':' + $(a).text());
		return a;
	}
}

function hasSameToken(a, b) {
	// console.log(a.getAttribute("style")+"<>"+b.getAttribute("style"));
	console.log("HAS SAME TOKEN ? "+ (!isNull(a) && !isNull(b) && getFgColor(a) == getFgColor(b) ? "YES" : "NO"));
	return !isNull(a) && !isNull(b) && getFgColor(a) == getFgColor(b);
}

function wasSplitted(a, b) {
	console.log("WAS SPLITTED ? "+ (!isNull(a) && !isNull(b) && getSpanIdNumber(a) == getSpanIdNumber(b) ? "YES" : "NO"));
	return !isNull(a) && !isNull(b) && getSpanIdNumber(a) == getSpanIdNumber(b);
}

function getSpanId(a) {
	console.log("spanId '"+$(a).prop("id"));
	return $(a).prop("id");
}

function getSpanIdNumber(a) {
	console.log("spanIdNumber of spanId '"+$(a).prop("id")+"' is "+$(a).prop("id").replace("t_s_","").replace("a","").replace("b",""));
	return $(a).prop("id").replace("t_s_","").replace("a","").replace("b","");
}
// ???
function isStartBound(e) {
	return (isNull(e) || (!isTextSpan(e) && !isNewLine(e)));
}
// ???
function isEndBound(e) {
	return isNull(e);
}

// updating the current position of caret within the plain text
function updateCaretPos() {
	caretPos = $('#caret').prevAll("span").text().length + $('#caret').prevAll("br").length;
	console.log("NEW UPDATE CARET POS: " + caretPos);
}

function generateChangeset(evn) {
	var data;
	if (evn.ctrlKey && !(pasting(evn) || cutting(evn))) {
		return;
	} else if (pasting(evn)) {
		data = window.clipboardData.getData('Text');
		console.log('pasting: ' + data);
	} else if (cutting(evn)) {
		window.clipboardData.setData('Text', 'SOME TEMPORARY TEXT');
		data = window.clipboardData.getData('Text');
		console.log("cutting: " + data);
	} else {
		data = getChar(evn);
		console.log("printable char: " + data);
	}
	var c;
	console.log("OLDTEXT:" + oldText + "| CARETPOS:" + caretPos + " | LENGTH: " + oldText.length);
	console.log("NEWTEXT:" + newText + "| DATA:" + data + " | LENGTH: " + newText.length);
	if (isPrintable(evn)) {
		c = Changeset.makeSplice(oldText, caretPos, 0, data, null, pool);
		caretPos += 1;
	} else if (isBackspace(evn)) {
		c = Changeset.makeSplice(oldText, caretPos - 1, 1, '', null, pool);
		caretPos -= 1;
	} else if (isDeleteKey(evn)) {
		c = Changeset.makeSplice(oldText, caretPos, 1, '', null, pool);
	} else {
		console.log("NOT PRINTABLE NOR REMOVAL");
		return;
	}
	oldText = newText;
	// dirty hack for working out the JSF - JS identification issues
	$(editor).next().val(c);
	$(editor).next().next().click();
	console.log(c);
}

// TODO: TAB, multi-SHIFT
function writeDown(evn) {
	console.log("writedown caretPos:" + caretPos);
	caret = $('#caret');
	var c = !isEnter(evn) ? getChar(evn) : '\n';
	console.log("CHAR: " + c);
	var tag = (isEnter(evn) ? 'br' : (isPrintable(evn) ? 'span' : ''));
	var sibl = 'prev';
	var needSlice;
	if (!isNull(caret) && !isNull($(caret).prev()) && getFgColor($(caret).prev()) == userColor) {
		needSlice = false;
	} else if (!isNull($(caret).next()) && getFgColor($(caret).next()) == userColor) {
		sibl = 'next';
		needSlice = false;
	} else {
		needSlice = true;
	}
	if (isPrintable(evn)) {
		var n;
		console.log("printable:" + c);
		if (needSlice) {
			console.log("newSlice:" + tag);
			n = (!isEnter(evn)) ? createNewTextSlice(tag, userColor, getNextSpanId(), c)
								: createNewLine();
			// position
			newText = newText.substring(0, caretPos) + c + newText.substring(caretPos); // ?? insert into
			console.log("newText: " + newText);
			$(n).insertBefore(caret);
			if (caretPos > 1) {
				var prev = $(n).prev();
				console.log("prev:" + $(prev).text());
				if ($(prev).prop("id").match("a")) {
					setSpanId(prev, getSpanIdNumber(prev));
					console.log("next:" + $(caret).next().text());
					setSpanId($(caret).next(), getNextSpanId());
				}
			}
		} else {
			console.log("caretPos = " + caretPos);
			if (caretPos == 0) {
				console.log(caretPos + ':new elem');
				n = createNewTextSlice();
				$(n).insertBefore(caret);
				$(n).text(c);
			} else {
				if (sibl == 'next') {
					$(caret).next().text(c + $(caret).next().text());
					moveCaretRight(1);
				} else {
					$(caret).prev().text($(caret).prev().text() + c);
				}
			}
		}
		// position
		newText = newText.substring(0, caretPos) + c + newText.substring(caretPos); // ?? insert into
		console.log("newText: " + newText);
		generateChangeset(evn);
	}
	$(editor).focus();
}

function moveCaretLeft(step) {
	moveCaretHorizontaly('left', step);
}

function moveCaretRight(step) {
	moveCaretHorizontaly('right', step);
}

function moveCaretHorizontaly(dir, step) {
	var caret = $('#caret');
	if (!isNull(caret)) {
		moveCaretWithinNode($('#editor'), dir, caret, $(caret).prev(), $(caret).next());
	} else {
		alert('Error: moveCaret: Caret wasn\'t found.');
	}
}

function getNextSpanId() {
	nextSpanId++;
	return nextSpanId - 1;
}

function updateNextSpanId(spanId) {
	console.log('updateNextSpanId('+spanId+')');
	if (spanId == 0) {
		return;
	} else if (spanId >= nextSpanId) {
		spanId++;
		nextSpanId = spanId;
		console.log('nextSpanId is now '+nextSpanId+'');
	} else {
		alert('Given SpanId('+spanId+') is smaller then local NextSpanId('+nextSpanId+')');
	}
}

function deleteChar(evn) {
	caret = $("#caret");
	editor = $("#editor");
	var prev = $(caret).prev();
	var next = $(caret).next();
	if (isBackspace(evn) && !isNull(prev)) {
		var txt = $(prev).text();
		console.log("TXT: " + txt + "|");
		console.log("REM: " + txt.charAt(txt.length - 1) + "|");
		if (txt.length > 1) {
			$(prev).text(txt.substring(0, txt.length - 1));
		} else {
			console.log("REMOVE WHOLE ELEMENT: " + prev);
			var pprev = $(prev).prev();
			var pnext = next;
			$(prev).remove();
			if (hasSameToken(pprev, pnext)) {
				mergeSlices(pprev, pnext);
			}
		}
		console.log(newText.substring(0, caretPos - 1) + "|" + newText.substring(caretPos));
		console.log("REMOVED..." + newText.substring(caretPos - 1,caretPos));
		newText = newText.substring(0, caretPos - 1) + newText.substring(caretPos);
		console.log("NEWTEXT:" + newText + ":" + newText.length);
//		$(editor).focus();
		generateChangeset(evn);
	} else if (isDeleteKey(evn) && !isNull(next)) {
		var txt = $(next).text();
		console.log("TXT: " + txt + "|");
		console.log("REM: " + txt.charAt(0) + "|");
		if (txt.length > 1) {
			$(next).text(txt.substring(1));
		} else {
			console.log("REMOVE WHOLE ELEMENT: " + next);
			var nprev = prev;
			var nnext = $(next).next();
			$(next).remove();
			if (hasSameToken(nprev, nnext)) {
				mergeSlices(nprev, nnext);
			}
		}
		console.log(newText.substring(0, caretPos) + "|" + newText.substring(caretPos+1));
		console.log("REMOVED..." + newText.substring(caretPos, caretPos+1));
		newText = newText.substring(0, caretPos) + newText.substring(caretPos+1);
		console.log("NEWTEXT:" + newText + ":" + newText.length);
//		$(editor).focus();
		generateChangeset(evn);
	}
}

// insert caret into the text on the specified position
function moveCaretWithinNode(parent, dir, caret, prev, next) {
	if (dir == "left") {
		if (!hasSameToken(prev, next)) {
			next = createNewTextSlice('SPAN', getFgColor(prev), "", "");
			$(next).insertAfter(caret);
		}
		// <span>ab</span><caret><span>cd</span>
		// <= caret
		// b =>
		// <span>a</span><caret><span>bcd</span>
		console.log("PSTR: " + prev.innerHTML);
		$(next).text($(prev).text().charAt($(prev).text().length > 0 ? $(prev).text().length - 1 : 0) + $(next).text());
		$(prev).text($(prev).text().substring(0, $(prev).text().length > 0 ? $(prev).text().length - 1 : 0));
		if ($(prev).text().length == 0) {
			$(prev).remove();
			console.log("getAttribute => "+$(prev).prop('id'));
			setSpanId(next,getSpanIdNumber(prev));
		} else {
			setSpanId(prev,getSpanIdNumber(prev)+"a");
			setSpanId(next,getSpanIdNumber(prev)+"b");
		}
	} else if (dir == "right") {
		if (!hasSameToken(prev, next)) {
			prev = createNewTextSlice('SPAN', getFgColor(next), "", "");
			$(prev).insertBefore(caret);
		}
		// <span>ab</span><caret><span>cd</span>
		// caret =>
		// <= c
		// <span>abc</span><caret><span>d</span>
		$(prev).text($(prev).text() + $(next).text().charAt(0));
		$(next).text($(next).text().substring(1));
		if ($(next).text().length == 0) {
			$(next).remove();
			console.log("getProp => "+$(next).prop('id'));
			setSpanId(prev,getSpanIdNumber(next));
		} else {
			setSpanId(prev,getSpanIdNumber(next)+"a");
			setSpanId(next,getSpanIdNumber(next)+"b");
		}
	}
}

function setSpanId(e,idNumber) {
	$(e).prop('id','t_s_'+idNumber);
}

function getLineStart(e) {
	console.log('.finding line start: e:' + $(e).prop('tagName'));
	while (!isStartBound($(e).prev()) && !isNewLine($(e).prev())) {
		e = $(e).prev();
		console.log('finding line start: e:' + $(e).prop('tagName'));
	}
	return e;
}

function getLineEnd(e) {
	console.log('.finding line end: e:' + $(e).prop('tagName'));
	while (!isEndBound($(e).next()) && !isNewLine($(e).next())) {
		e = $(e).next();
		console.log('finding line end: e:' + $(e).prop('tagName'));
	}
	return e;
}

// TODO:
// shifting the caret down off 'step' positions
// eg: step = 1 => 1 line down
// step = -1 => 1 line up
function shiftCaret(dir, hpos) {
	var e;
	if (dir == "UP") { console.log("SHIFTING UP");
		e = $("#caret").prev();
		if (isNewLine(e) && isNewLine($(e).prev())) {
			e = $(e).prev();
		} else {
			e = isNewLine(e) ? getLineStart(e) : getLineStart(getLineStart($(e)).prev());
		}
		placeCaretIntoElem(e, hpos);
	} else if (dir == "DOWN") { console.log("SHIFTING DOWN");
		e = $("#caret").next();
		e = isNewLine(e) ? $(e).next() : getLineEnd(e).next();
		placeCaretIntoElem(e, hpos);
	} else {
		return;
	}
}

// finds the current element and places the caret into it
function placeCaretIntoElem(e, offset) {
	console.log('placeCaretIntoElem(e,offset) : ' + $(e).prop('tagName') + "|"	+ textLength(e) + "|" + offset);
	while (!isNull(e) && textLength(e) < offset) {
		if (isEndBound($(e).next()) || isNewLine($(e).next())) {
			offset = $(e).text().length;
			break;
		} else {
			console.log('>> placeCaretIntoElem(e,offset) : ' + $(e).prop('tagName') + "|"	+ textLength(e) + "|" + offset);
			offset -= $(e).text().length;
			e = $(e).next();
		}
	}
	console.log('END: placeCaretIntoElem(e,offset) : ' + $(e).prop('tagName') + "|" + textLength(e) + "|" + offset);
	placeCaretWithinElement(e, offset);
}

// returns the horizontal offset - offset within the actual line
function horizCaretPos(e) {
	var hpos = 0;
	// supposing that the only text elements are <br> and <span>
	e = $(e).prev();
	while (!isStartBound(e) && !isNewLine(e)) {
		console.log(e);
		hpos += $(e).text().length;
		e = $(e).prev();
	}
	return hpos;
}

function onFirstLine(elem) {
	console.log("IS ON FIRST LINE ? "+($(elem).prevAll("br").length <= 0 ? "YES" : "NO"));
	return $(elem).prevAll("br").length <= 0;
}

function onLastLine(elem) {
	console.log("IS ON LAST LINE ? "+($(elem).nextAll("br").length <= 0 ? "YES" : "NO"));
	return $(elem).nextAll("br").length <= 0;
}

// need a workout
function moveCaret(e) {
	console.log("moveCaret..."+keyCode(e));
	caret = $("#caret");
	dir = keyCode(e);
//	console.log("M:" + dir);
	var prev = $(caret).prev();
	var next = $(caret).next();
//	console.log("PREVIOUSSIBLING = "+prev+" : "+$(prev).text());
//	console.log("NEXTSIBLING = "+next+" : "+$(next).text());
	if (dir == 39) {
		if (!isNull(next)) {
			caretPos += 1;
			if (isTextSpan(next)) {
				moveCaretWithinNode(editor, "right", caret, prev, next);
			} else {
				$(next).insertBefore(caret);
			}
		}
	} else if (dir == 37) {
		if (!isStartBound(prev)) {
			caretPos -= 1;
			console.log($(prev).prop('tagName') + ":" + $(prev).text().length);
			if (!isNewLine(prev)) {
				moveCaretWithinNode(editor, "left", caret, prev, next);
			} else {
				$(prev).insertAfter(caret);
			}
		}
	} else if (dir == 38) {
		if (!onFirstLine(caret)) {
			shiftCaret("UP", horizCaretPos(caret));
		}
	} else if (dir == 40) {
		if (!onLastLine(caret)) {
			shiftCaret("DOWN", horizCaretPos(caret));
		}
	}
	console.log(newText.substring(0,caretPos)+"<CARET>"+newText.substring(caretPos));
//	e.stopPropagation();
}

// TEST ALSO OTHER BROWSERS THEN FIREFOX
function isArrow(e) {
//	console.log("keyCode:"+keyCode(e));
	return keyCode(e) >= 37 && keyCode(e) <= 40;
}

function isEnter(e) {
	return keyCode(e) == 13;
}

function isSpace(e) {
	return keyCode(e) == 32;
}

function isTab(e) {
	return keyCode(e) == 9;
}

function isWhiteSpace(e) {
	return isEnter(e) || isSpace(e) || isTab(e);
}

function isBackspace(e) {
	return keyCode(e) == 8;
}

function isDeleteKey(e) {
	return keyCode(e) == 46; // TODO: see -> http://www.quirksmode.org/js/keys.html
}

function getChar(e) {
	return String.fromCharCode(keyCode(e));
}

function isPrintable(e) {
	if (e.type == "keypress") {
		if (e.which == "undefined") {
			return true;
		} else if (typeof e.which == "number" && e.which > 0) {
			return !e.ctrlKey && !e.metaKey && !e.altKey && !isBackspace(e);
		}
	}
	return false;
}

function keyCode(e) {
	return e.keyCode > 0 ? e.keyCode : e.which;
}

function isNewLine(e) {
//	console.log("IS NEW LINE ? "+(e != null && $(e).is('br')));
	return e != null && $(e).is('br');
}

// determine "Paste" action
function pasting(e) {
	return (e.ctrlKey && (keyCode(e) == 86));
}
// determine "Cut" action
function cutting(e) {
	return (e.ctrlKey && (keyCode(e) == 88));
}

function isEmptyLine(elem) {
	return isNewLine(elem.nextSibling);
}
