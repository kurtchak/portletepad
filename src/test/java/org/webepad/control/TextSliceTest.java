package org.webepad.control;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webepad.exceptions.RangeOutOfDimensionsException;
import org.webepad.model.Session;
import org.webepad.utils.ExceptionHandler;

public class TextSliceTest {
	private Logger log = LoggerFactory.getLogger(TextSliceTest.class);
	
	@Test
	public void testInsertText() {
		Session s = new Session();
		
		// INSERT TEXT IN MIDDLE OF ORIGINAL TEXT
		log.info("\n1. INSERT TEXT IN MIDDLE OF ORIGINAL TEXT");
		TextSlice ts = new TextSlice(s,"abcd");
		ts.insertText("opqr", 2);
		assertEquals(ts.getPlain(), "abopqrcd");
		
		// APPEND TEXT TO THE END OF ORIGINAL TEXT
		log.info("\n2. APPEND TEXT TO THE END OF ORIGINAL TEXT");
		TextSlice ts2 = new TextSlice(s,"abcd");
		ts2.insertText("opqr", 4);
		assertEquals(ts2.getPlain(), "abcdopqr");
		
		// INSERT TEXT BEFORE THE ORIGINAL TEXT
		log.info("\n3. INSERT TEXT BEFORE THE ORIGINAL TEXT");
		TextSlice ts3 = new TextSlice(s,"abcd");
		ts3.insertText("opqr", 0);
		assertEquals(ts3.getPlain(), "opqrabcd");
		
	}
	
	@Test
	public void testAppendText() {
		Session s = new Session();
		
		// APPEND TEXT TO THE END OF ORIGINAL TEXT
		log.info("\n1. APPEND TEXT TO THE END OF ORIGINAL TEXT");
		TextSlice ts = new TextSlice(s,"abcd");
		ts.appendText("opqr");
		assertEquals(ts.getPlain(), "abcdopqr");
		
		// APPEND TEXT TO EMPTY TEXT
		log.info("\n2. APPEND TEXT TO EMPTY TEXT");
		TextSlice ts2 = new TextSlice(s,TextSlice.SPAN);
		ts2.setPlain("");
		ts2.appendText("opqr");
		assertEquals(ts2.getPlain(), "opqr");
	}

	@Test
	public void testRemoveText() {
		Session s = new Session();
		
		// REMOVE TEXT FROM THE SPECIFIED POSITION TILL THE END OF THE ORIGINAL TEXT
		log.info("\n1. REMOVE TEXT FROM THE SPECIFIED POSITION TILL THE END OF THE ORIGINAL TEXT");
		TextSlice ts = new TextSlice(s,"abcdefgh");
		try {
			ts.removeText(3);
		} catch (RangeOutOfDimensionsException e) {
			ExceptionHandler.handle(e);
		}
		assertEquals(ts.getPlain(), "abc");
		
		// REMOVE TEXT FROM THE POSITION 'FROM' TILL THE POSITION 'TO' OF THE ORIGINAL TEXT
		log.info("\n2. REMOVE TEXT FROM THE POSITION 'FROM' TILL THE POSITION 'TO' OF THE ORIGINAL TEXT");
		TextSlice ts2 = new TextSlice(s,"abcdefgh");
		try {
			ts2.removeText(0,3);
		} catch (RangeOutOfDimensionsException e) {
			ExceptionHandler.handle(e);
		}
		assertEquals(ts2.getPlain(), "defgh");
		
		// REMOVE TEXT FROM THE POSITION 'FROM' TILL THE POSITION 'TO' OF THE ORIGINAL TEXT
		log.info("\n3. REMOVE TEXT FROM THE POSITION 'FROM' TILL THE POSITION 'TO' OF THE ORIGINAL TEXT"); 
		TextSlice ts3 = new TextSlice(s,"abcdefgh");
		try {
			ts3.removeText(2,5);
		} catch (RangeOutOfDimensionsException e) {
			ExceptionHandler.handle(e);
		}
		assertEquals(ts3.getPlain(), "abfgh");
		
		// REMOVE TEXT FROM THE INVALID POSITION 'FROM' TILL THE VALID POSITION 'TO' OF THE ORIGINAL TEXT
		log.info("\n4. REMOVE TEXT FROM THE INVALID POSITION 'FROM' TILL THE VALID POSITION 'TO' OF THE ORIGINAL TEXT");
		TextSlice ts4 = new TextSlice(s,"abcdefgh");
		try {
			ts4.removeText(-2,5);
		} catch (RangeOutOfDimensionsException e) {
			ExceptionHandler.handle(e);
		}
		assertEquals(ts4.getPlain(), "abcdefgh"); // NOT CHANGED
		
		// REMOVE TEXT FROM THE VALID POSITION 'FROM' TILL THE INVALID POSITION 'TO' OF THE ORIGINAL TEXT
		log.info("\n5. REMOVE TEXT FROM THE VALID POSITION 'FROM' TILL THE INVALID POSITION 'TO' OF THE ORIGINAL TEXT");
		TextSlice ts5 = new TextSlice(s,"abcdefgh");
		try {
			ts5.removeText(2,10);
		} catch (RangeOutOfDimensionsException e) {
			ExceptionHandler.handle(e);
		}
		assertEquals(ts5.getPlain(), "abcdefgh"); // NOT CHANGED
		
		// REMOVE TEXT FROM THE INVALID POSITION 'FROM' TILL THE INVALID POSITION 'TO' OF THE ORIGINAL TEXT
		log.info("\n6. REMOVE TEXT FROM THE INVALID POSITION 'FROM' TILL THE INVALID POSITION 'TO' OF THE ORIGINAL TEXT");
		TextSlice ts6 = new TextSlice(s,"abcdefgh");
		try {
			ts6.removeText(-1,11);
		} catch (RangeOutOfDimensionsException e) {
			ExceptionHandler.handle(e);
		}
		assertEquals(ts6.getPlain(), "abcdefgh"); // NOT CHANGED
		
		// REMOVE TEXT FROM INVALID RANGE
		log.info("\n7. REMOVE TEXT FROM INVALID RANGE");
		TextSlice ts7 = new TextSlice(s,"abcdefgh");
		try {
			ts7.removeText(5,3);
		} catch (RangeOutOfDimensionsException e) {
			ExceptionHandler.handle(e);
		}
		assertEquals(ts7.getPlain(), "abcdefgh"); // NOT CHANGED
	}
}
