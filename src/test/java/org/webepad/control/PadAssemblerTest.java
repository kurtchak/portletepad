package org.webepad.control;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webepad.model.Session;
import org.webepad.utils.ExceptionHandler;

public class PadAssemblerTest {
	private Logger log = LoggerFactory.getLogger(PadAssemblerTest.class);
	
	@Test
	public void testMergeTextSlices() {
		Session s = new Session();
		s.setId(1L);
		Session s2 = new Session();
		s2.setId(2L);
		
		// MERGE TWO NEIGHBOUR TEXTSLICES
		log.info("\n1. MERGE TWO NEIGHBOUR TEXTSLICES");
		TextSlice ts1 = new TextSlice(s,"abcd");
		TextSlice ts2 = new TextSlice(s,"opqr");
		TextSlice newTs = null;
		try {
			newTs = PadAssembler.mergeTextSlices(ts1, ts2);
		} catch (Exception e) {
			ExceptionHandler.handle(e);
		}
		assertEquals(newTs != null, true);
		assertEquals(newTs.getPlain(), "abcdopqr");

		// MERGE TWO NEIGHBOUR TEXTSLICES WITH DIFFERENT TOKEN (SESSION)
		log.info("\n2. MERGE TWO NEIGHBOUR TEXTSLICES WITH DIFFERENT TOKEN (SESSION)");
		TextSlice ts3 = new TextSlice(s,"abcd");
		TextSlice ts4 = new TextSlice(s2,"opqr");
		TextSlice newTs2 = null;
		try {
			newTs2 = PadAssembler.mergeTextSlices(ts3, ts4);
		} catch (Exception e) {
			ExceptionHandler.handle(e);
		}
		assertEquals(newTs2 == null, true);

		// MERGE TWO NEIGHBOUR TEXTSLICES WHERE THE FIRST IS NEWLINE SLICE
		log.info("\n3. MERGE TWO NEIGHBOUR TEXTSLICES WHERE THE FIRST IS NEWLINE SLICE");
		TextSlice ts5 = new TextSlice(s,TextSlice.BR);
		TextSlice ts6 = new TextSlice(s,"opqr");
		TextSlice newTs3 = null;
		try {
			newTs3 = PadAssembler.mergeTextSlices(ts5, ts6);
		} catch (Exception e) {
			ExceptionHandler.handle(e);
		}
		assertEquals(newTs3 == null, true);

		// MERGE TWO NEIGHBOUR TEXTSLICES WHERE THE SECOND IS NEWLINE SLICE
		log.info("\n4. MERGE TWO NEIGHBOUR TEXTSLICES WHERE THE SECOND IS NEWLINE SLICE");
		TextSlice ts7 = new TextSlice(s,"abcd");
		TextSlice ts8 = new TextSlice(s,TextSlice.BR);
		TextSlice newTs4 = null;
		try {
			newTs4 = PadAssembler.mergeTextSlices(ts7, ts8);
		} catch (Exception e) {
			ExceptionHandler.handle(e);
		}
		assertEquals(newTs4 == null, true);

		// MERGE TWO NEIGHBOUR TEXTSLICES WHERE THE FIRST IS NULL
		log.info("\n5. MERGE TWO NEIGHBOUR TEXTSLICES WHERE THE FIRST IS NULL");
		TextSlice ts9 = new TextSlice(s,"abcd");
		TextSlice ts10 = null;
		TextSlice newTs5 = null;
		try {
			newTs5 = PadAssembler.mergeTextSlices(ts9, ts10);
		} catch (Exception e) {
			ExceptionHandler.handle(e);
		}
		assertEquals(newTs5 == null, true);

		// MERGE TWO NEIGHBOUR TEXTSLICES WHERE THE SECOND IS NULL
		log.info("\n6. MERGE TWO NEIGHBOUR TEXTSLICES WHERE THE SECOND IS NULL");
		TextSlice ts11 = null;
		TextSlice ts12 = new TextSlice(s,"opqr");
		TextSlice newTs6 = null;
		try {
			newTs6 = PadAssembler.mergeTextSlices(ts11, ts12);
		} catch (Exception e) {
			ExceptionHandler.handle(e);
		}

		assertEquals(newTs6 == null, true);
		// MERGE TWO NEIGHBOUR TEXTSLICES WHERE THE FIRST IS NULL AND SECOND IS NEW LINE SLICE
		log.info("\n7. MERGE TWO NEIGHBOUR TEXTSLICES WHERE THE FIRST IS NULL AND SECOND IS NEW LINE SLICE");
		TextSlice ts13 = new TextSlice(s,TextSlice.BR);
		TextSlice ts14 = null;
		TextSlice newTs7 = null;
		try {
			newTs7 = PadAssembler.mergeTextSlices(ts13, ts14);
		} catch (Exception e) {
			ExceptionHandler.handle(e);
		}
		assertEquals(newTs7 == null, true);

		// MERGE TWO NEIGHBOUR TEXTSLICES WHERE THE SECOND IS NULL AND FIRST IS NEW LINE SLICE
		log.info("\n8. MERGE TWO NEIGHBOUR TEXTSLICES WHERE THE SECOND IS NULL AND FIRST IS NEW LINE SLICE");
		TextSlice ts15 = null;
		TextSlice ts16 = new TextSlice(s,TextSlice.BR);
		TextSlice newTs8 = null;
		try {
			newTs8 = PadAssembler.mergeTextSlices(ts15, ts16);
		} catch (Exception e) {
			ExceptionHandler.handle(e);
		}
		assertEquals(newTs8 == null, true);
	}
}
