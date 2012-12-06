package org.webepad.control;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webepad.exceptions.RangeOutOfDimensionsException;
import org.webepad.model.Changeset;
import org.webepad.utils.ExceptionHandler;
import org.webepad.utils.TextSliceTuple;

public class PadContent {
	private static Logger log = LoggerFactory.getLogger(PadContent.class);
	
	private LinkedList<TextSlice> contentList = new LinkedList<TextSlice>();

	private TextSlice touchedTs; // last touched (changed/written) textslice
	
	private String text;
	private String viewRepr; // this should be DOM element, however it is not needed
							// - the render phase is on JS

	public PadContent() {
		TextSlice.resetLastSpanId();
	}
	
	public void reloadPadContent() {
		StringBuilder sb = new StringBuilder();
		for (TextSlice ts : contentList) {
			sb.append(ts.getHtml());
		}
		setViewRepr(sb.toString().trim());
	}

	/**
	 * Removes specfied TextSlice from the list and handle the neighbours left
	 * @param ts
	 */
	public PadContent removeSlice(TextSlice ts) {
		log.debug("removeSlice "+ts.getSpanId()+" ["+ts.getPlain()+"]");
		int index = contentList.indexOf(ts); // AFTER REMOVAL OF TS INDEX WILL BE POINTING ON NEXT SLICE
		contentList.remove(ts);
		if (index > 1 && index < contentList.size()) { // IF NOT THE FIRST IN LIST NOR THE END, TRY TO MERGE WITH PREVIOUS SLICE
			try {
				TextSlice mergedTs = mergeTextSlices(contentList.get(index - 1), contentList.get(index));
				if (mergedTs != null) {
					contentList.remove(index);
					contentList.set(index - 1, mergedTs);
				}
			} catch (Exception e) {
				ExceptionHandler.handle(e);
			}
		}
		setLastTouchedTs(ts, 0); // last touched must be sent to the collab users
		return this;
	}

	public TextSliceTuple<TextSlice,Integer> findTextSliceAndPosition(Changeset c) throws Exception {
		if (c.isAtTheEnd()) {
			if (contentList.isEmpty()) {
				return null;
			} else if (c.getAction() == Changeset.WRITE) {
				TextSlice ts = contentList.getLast();
				return new TextSliceTuple<TextSlice, Integer>(ts, ts.getPlainLen());
			} else if (c.getAction() == Changeset.DELETE) {
				TextSlice ts = contentList.getLast();
				return new TextSliceTuple<TextSlice, Integer>(ts, ts.getPlainLen() - c.getLengthDifference());
			}
		} else {
			int cOffset = c.getOffset();
			int length = 0, tsOffset;
			for (TextSlice ts : contentList) {
				if (length == cOffset) {
					return new TextSliceTuple<TextSlice, Integer>(ts, 0);
				} else if (length + ts.getPlainLen() > cOffset) {
					tsOffset = cOffset - length;
					return new TextSliceTuple<TextSlice, Integer>(ts, tsOffset);
				} else {
					if (length + ts.getPlainLen() == cOffset) {
						if (c.getAction() == Changeset.WRITE && ts.hasSameTokenWith(c) && ts.isTextSpan()) { // if at end of TextSlice and not have the same token, return next TextSlice with position 0
							return new TextSliceTuple<TextSlice, Integer>(ts, ts.getPlainLen());
						}
					}
					length += ts.getPlainLen();
				}
			}
			throw new Exception("Given offset is out of length of current text - offset:"+ cOffset+ ", length:"+length);
//			TextSlice ts = contentList.getLast();
//			return new TextSliceTuple<TextSlice, Integer>(ts, ts.getPlainLen()-(c.getAction() == Changeset.WRITE || c.getAction() == Changeset.ATTR_CHANGE ? 1 : 2));
		}
		return null;
	}

	public void insertSlice(TextSliceTuple<TextSlice, Integer> tsTuple, Changeset c) throws RangeOutOfDimensionsException {
		if (tsTuple == null) {
			insertSlice(null, c, 0);
		} else {
			insertSlice(tsTuple.getTextSlice(), c, tsTuple.getOffset());
		}
	}
	
	public void insertSlice(TextSlice ts, Changeset c, int pos) throws RangeOutOfDimensionsException {
		TextSlice nbts = new TextSlice(c);
		if (ts == null) {
			contentList.add(nbts);
		} else if (pos == 0) {
			contentList.add(contentList.indexOf(ts), nbts);
		} else if (pos == ts.getPlainLen()) {
			contentList.add(contentList.indexOf(ts)+1, nbts);
		} else {
			TextSlice nts = new TextSlice(ts.getSession(), TextSlice.SPAN); //TODO:POZOR
			nts.setPlain(ts.getPlain().substring(pos));
			ts.removeText(pos);
			contentList.add(contentList.indexOf(ts) + 1, nbts);
			contentList.add(contentList.indexOf(nbts) + 1, nts);
		}
		setLastTouchedTs(nbts, pos);
//		nbts.setLastActivePos(pos); // this pos is used as info where in the orig textslice was inserted the new one
	}

//	public PadContent appendSlice(Changeset c) {
//		TextSlice ts = new TextSlice(c);
//		contentList.add(ts);
//		setLastTouchedTs(ts, ts.getPlainLen()-1); // set last touched position in slice as last
//		return this;
//	}

	private void setLastTouchedTs(TextSlice ts, int pos) {
		log.debug("setLastTouchedTs("+(touchedTs != null ? touchedTs.getHtml() : null) +","+pos+")");
		ts.setLastActivePos(pos);
		setTouchedTs(ts);
	}

	/**
	 * Joines texts from two given TextSlice objects into one TextSlice
	 * @param ts1
	 * @param ts2
	 * @return
	 * @throws Exception
	 */
	public static TextSlice mergeTextSlices(TextSlice ts1, TextSlice ts2) throws Exception {
		if (ts1 == null || ts2 == null) {
			throw new Exception("One of the objects has invalid value: ts1="+ts1+", ts2="+ts2);
		} else {
			if (ts1.isMergeableWith(ts2)) {
				ts1.appendText(ts2.getPlain());
			} else {
				return null;
			}
		}
		return ts1;
	}

	public int getPlainTextLength() {
		int len = 0;
		for (TextSlice ts : contentList) {
			len += ts.getPlainLen();
		}
		return len;
	}

	// AUXILIARY METHODS FOR TESTING PURPOSES
	public void setContentList(LinkedList<TextSlice> list) {
		contentList = list; 
	}
	
	public TextSlice getPrevTextSlice(TextSlice ts) {
		if (contentList.indexOf(ts) > 0) {
			return contentList.get(contentList.indexOf(ts)-1);
		} else {
			return null;
		}
	}
	
	public TextSlice getNextTextSlice(TextSlice ts) {
		int index = contentList.indexOf(ts);
		if (index > 0 && index < contentList.size() - 1) {
			return contentList.get(contentList.indexOf(ts)+1);
		} else {
			return null;
		}
	}
	
	public void addTextSlice(TextSlice ts) {
		contentList.add(ts); 
	}

	public TextSlice getTextSlice(int index) {
		return contentList.get(index);
	}

	// GETTERS AND SETTERS
	public void setText(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public TextSlice getLast() {
		return this.contentList.peekLast();
	}

	public TextSlice getTouchedTs() {
		return touchedTs;
	}

	public void setTouchedTs(TextSlice touchedTs) {
		log.debug("setTouchedTs("+touchedTs.getHtml()+")");
		this.touchedTs = touchedTs;
	}

	public String getViewRepr() {
		return viewRepr;
	}

	public void setViewRepr(String viewRepr) {
		this.viewRepr = viewRepr;
	}

	public TextSlice findTextSlice(int spanId) {
		log.debug("findTextSlice("+spanId+")");
		if (!contentList.isEmpty()) {
			for (TextSlice ts : contentList) {
				if (ts.getSpanId() == spanId) {
					log.debug("found:("+ts.getHtml()+")");
					return ts;
				}
			}
		}
		return null;
	}

	public int indexOf(TextSlice ts) {
		log.debug("indexOf("+ts.getHtml()+")");
		return contentList.indexOf(ts);
	}

	public void insertTextSlice(TextSlice ts, int i) {
		log.debug("insertTextSlice("+ts.getHtml()+","+i+")");
		contentList.add(i, ts);
	}

	public TextSlice findTextSliceByOffset(Integer offset) {
		log.debug("findTextSliceByOffset("+offset+")");
		for (TextSlice ts : contentList) {
			if (ts.getPlainLen() > offset) {
				ts.setLastActivePos(ts.getPlainLen() - offset);
				return ts;
			} else {
				offset -= ts.getPlainLen();
			}
		}
		return null;
	}
}
