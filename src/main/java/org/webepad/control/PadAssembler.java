package org.webepad.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webepad.model.Changeset;
import org.webepad.model.Pad;
import org.webepad.utils.TextSliceTuple;

public class PadAssembler {
	private static Logger log = LoggerFactory.getLogger(PadAssembler.class);

	private PadContent padContent;
	
	public PadAssembler() {
	}
	
	public void parseContent(Pad pad) throws Exception {
		if (pad != null) {
			padContent = new PadContent();
			for (Changeset c : pad.getChangesets()) {
				apply(c);
			}
			padContent.reloadPadContent();
		} else {
			throw new Exception("Parsing exception - null object was given as Pad.");
		}
	}

	/**
	 * Process and apply given changeset into built structure of text elements
	 * returning the element changed by the changeset
	 * 
	 * @param c
	 * @return
	 * @throws Exception
	 */
	private PadContent apply(Changeset c) throws Exception {
		if (c.getAction() == Changeset.WRITE) {
			return processWriteChange(c);
		} else if (c.getAction() == Changeset.DELETE) {
			return processDeleteChange(c);
		} else if (c.getAction() == Changeset.ATTR_CHANGE) {
			log.debug("CHANGE");
		} else if (c.getAction() == Changeset.NOCHANGE) {
			log.debug("NO CHANGE");
		}
		return null;
	}

	/**
	 * Process application of changeset writting chars
	 * @param c
	 * @return
	 * @throws Exception 
	 */
	private PadContent processWriteChange(Changeset c) throws Exception {
		// walk through text as a collection of TextSlice objects and find the position
		TextSliceTuple<TextSlice, Integer> tsTuple = (TextSliceTuple<TextSlice, Integer>) padContent.findTextSliceAndPosition(c);
		if (needNewSlice(tsTuple, c)) {
			padContent.insertSlice(tsTuple, c);
		} else {
			tsTuple.getTextSlice().insertText(c.getCharbank(), tsTuple.getOffset());
			padContent.setTouchedTs(tsTuple.getTextSlice()); // last position is managed within the method `insertText`
		}
		return padContent;
	}

	/**
	 * Process application of changeset deleting chars
	 * @param cc
	 * @return
	 * @throws Exception 
	 */
	private PadContent processDeleteChange(Changeset c) throws Exception {
		TextSliceTuple<TextSlice, Integer> tsTuple = (TextSliceTuple<TextSlice, Integer>) padContent.findTextSliceAndPosition(c);
		if (tsTuple.getTextSlice().getPlainLen() <= c.getLengthDifference()) {
			padContent.removeSlice(tsTuple.getTextSlice());
		} else {
			tsTuple.getTextSlice().removeText(tsTuple.getOffset(), tsTuple.getOffset() + c.getLengthDifference());
			padContent.setTouchedTs(tsTuple.getTextSlice());
		}
		return padContent;
	}

	private boolean needNewSlice(TextSliceTuple<TextSlice, Integer> tsTuple, Changeset c) {
		if (tsTuple == null) {
			log.debug("NEEDNEWSLICE ? YES");
			return true;
		} else {
			TextSlice ts = tsTuple.getTextSlice();
			log.debug("NEEDNEWSLICE ? " + ((ts == null || ts.isNewLine() || c.hasNewLine() || !c.hasSameSessionAs(ts)) ? "YES" : "NO"));
			return ts == null || ts.isNewLine() || c.hasNewLine() || !c.hasSameSessionAs(ts);
		}
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

	/**
	 * Processing the local changeset returning the actually touched element and position within it
	 * @param changeset
	 * @return
	 * @throws Exception
	 */
	public PadContent applyLocalChangeset(Changeset changeset) throws Exception {
		return apply(changeset);
	}
	
	// GETTERS AND SETTERS
	public String getTextContent() {
		return padContent.getViewRepr();
	}

	public int getPlainTextLength() {
		return padContent.getPlainTextLength();
	}

	public void refreshTextContent() {
		padContent.reloadPadContent();
	}
}
