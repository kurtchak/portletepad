package org.webepad.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webepad.exceptions.RangeOutOfDimensionsException;
import org.webepad.model.Changeset;
import org.webepad.model.Session;
import org.webepad.model.User;

public class TextSlice {
	private Logger log = LoggerFactory.getLogger(TextSlice.class);
	
	public static int SPAN = 0;
	public static int BR = 1;
	
	private static int lastSpanId = 0;

	private int spanId;
	private int lastActivePos;
	
	private String plain;
	private int plainLen;
	
	private String html;
	private int type;
	private int textOffset;
	
	private User author;
	private Session session;
	private String color;

	// FOR UNIT TESTING ONLY
	public TextSlice(Session session, String text) {
		this.type = text != null && text.length() > 0 ? SPAN : BR;
		this.session = session;
		this.color = session.getColorCode();
		if (type == BR) {
			setPlain("\n");
		} else {
			setPlain(text);
			spanId = nextSpanId();
		}
		this.lastActivePos = 0;
		buildRepr();
	}
	
	public TextSlice(Session session, int type) {
		this.type = type;
		this.session = session;
		this.color = session.getColorCode();
		if (type == BR) {
			setPlain("\n");
		} else {
			spanId = nextSpanId();
		}
		this.lastActivePos = 0;
		buildRepr();
	}

	public TextSlice(Changeset c) {
		this.session = c.getSession();
		this.color = session.getColorCode();
		if (c.getCharbank().length() > 0) {
			type = TextSlice.SPAN;
			if (c.hasSpanId()) {
				spanId = c.getSpanId();
				lastSpanId = spanId;
			} else {
				spanId = nextSpanId();
			}
			setPlain(c.getCharbank());
//			lastActivePos = plainLen;
			this.lastActivePos = 0;
		} else {
			type = TextSlice.BR;
			setPlain("\n");
		}
		buildRepr();
	}

	public TextSlice(int spanId) {
		type = TextSlice.SPAN;
		this.spanId = spanId;
		this.lastActivePos = 0;
	}

	public String getHtml() {
		return html;
	}

	public void setHtml(String html) {
		this.html = html;
	}

	public String getPlain() {
		return plain;
	}

	public void setPlain(String plain) {
		this.plain = plain;
		plainLen = plain.length();
		buildRepr();
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
	public int getTextOffset() {
		return textOffset;
	}
	public void setTextOffset(int textOffset) {
		this.textOffset = textOffset;
	}

	public int getPlainLen() {
		return plainLen;
	}

//	public void setPlainLen(int plainLen) {
//		this.plainLen = plainLen;
//	}
//	
	public int getSpanId() {
		return spanId;
	}

	private int nextSpanId() {
		log.debug("NEXT SPAN ID INCREMENTING: "+(lastSpanId+1)+"|"+plain);
		lastSpanId += 1;
		return lastSpanId;
	}

	/**
	 * Appends specified text to the original text of current TextSlice object
	 * @param charbank
	 */
	public void appendText(String charbank) {
		insertText(charbank, plainLen);
	}

	/**
	 * Inserts specified text into position within the original text of this TextSlice object
	 * @param string
	 * @param pos
	 */
	public void insertText(String string, int pos) {
		log.debug("insertText: "+plain.substring(0, pos) +"["+ string +"]"+ plain.substring(pos));
		setPlain(plain.substring(0, pos) + string + plain.substring(pos));
		setLastActivePos(pos);
	}

	/**
	 * Removes substring from the specified position to the end of the string 
	 * @param from
	 * @throws RangeOutOfDimensionsException 
	 */
	public void removeText(int from) throws RangeOutOfDimensionsException {
		removeText(from, plainLen);
	}

	/**
	 * Removes character from the specified position to the end of the string 
	 * @param from
	 * @throws RangeOutOfDimensionsException 
	 */
	public void removeText(int from, int to) throws RangeOutOfDimensionsException {
		log.debug("removeText: from:"+from +", to:"+ to);
		if (from < 0 || from > plainLen || to <= 0 || to > plainLen || from > to) {
			throw new RangeOutOfDimensionsException(plain,from,to);
		} else {
			log.debug(plain.substring(0,from) + "><" + plain.substring(to) + "[-"+plain.substring(from,to)+"]");
			setPlain(plain.substring(0,from) + plain.substring(to));
			setLastActivePos(from);
		}
	}
	
	public void buildRepr() {
		StringBuilder sb = new StringBuilder();
		sb.append(type);
		if (type == SPAN) {
			sb.append("_").append(color);
			sb.append("_").append(spanId);
			sb.append("_");
			textOffset = sb.length();
			sb.append(plainLen);
			sb.append(":");
			sb.append(plain);
		}
		html = sb.toString();
		log.debug(html);
	}

	public User getAuthor() {
		return author;
	}

	public void setAuthor(User author) {
		this.author = author;
	}

	public Session getSession() {
		if (session != null) {
			log.debug(session.toString());
		} else {
			log.debug("null:"+type+":"+plain);
		}
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
		color = session.getColorCode();
	}
	
	public static void resetLastSpanId() {
		lastSpanId = 0;
	}

	public int getLastActivePos() {
		return lastActivePos;
	}

	public void setLastActivePos(int lastActivePos) {
		log.debug("setLastActivePos("+lastActivePos+")");
		this.lastActivePos = lastActivePos;
	}

	public boolean hasSameTokenWith(TextSlice ts2) {
		return ts2 != null && session != null && session.equals(ts2.getSession());
	}

	public boolean hasSameTokenWith(Changeset c) {
		return c != null && session != null && session.equals(c.getSession());// TODO: comparing of session should be replaced with comparing of tokens
	}
	
	public boolean isMergeableWith(TextSlice ts2) {
		return (ts2 != null && type == TextSlice.SPAN && type == ts2.getType())
				&& hasSameTokenWith(ts2);
	}

	public boolean isTextSpan() {
		return type == SPAN;
	}

	public boolean isNewLine() {
		return type == BR;
	}
	
	public static int getNextSpanId() {
		return lastSpanId + 1;
	}
}
