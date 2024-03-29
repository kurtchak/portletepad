package org.webepad.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.webepad.control.TextSlice;
import org.webepad.dao.hibernate.HibernateDAOFactory;
import org.webepad.exceptions.MalformedMessage;
import org.webepad.exceptions.NothingChangedException;
import org.webepad.exceptions.RegExpException;
import org.webepad.utils.ExceptionHandler;

/**
 * The persistent class for the Changeset database table.
 * 
 */
public class Changeset extends TemporalEntity {
	private static final long serialVersionUID = 8568943006690012442L;
	
	private static String RULE_REGEXP = "^Z:([0-9a-zA-Z]+)([><])([0-9a-zA-Z]+)(((\\*[0-9a-zA-Z]+)*(\\|?[0-9a-zA-Z]+)?([+-=])([0-9a-zA-Z]+))+)?$";
	private static String INNER_RULE_REGEXP = "(\\*[0-9a-zA-Z]+)*((\\|)?([0-9a-zA-Z]+))?=([0-9a-zA-Z]+)";
	private static String RULE_LEN_DIFF_REGEXP = "Z:([0-9a-zA-Z]+)[<>]([0-9a-zA-Z]+)";
			
	private static Pattern RULE_PATTERN = Pattern.compile(RULE_REGEXP);
	private static Pattern INNER_RULE_PATTERN = Pattern.compile(INNER_RULE_REGEXP);
	private static Pattern RULE_LEN_DIFF_PATTERN = Pattern.compile(RULE_LEN_DIFF_REGEXP);

	// action flags
	public static String WRITE = "W";
	public static String DELETE = "D";
	public static String ATTR_CHANGE = "A";
	public static String NOCHANGE = "X";

	// persisted
	private String rule;
	private String charbank;
	private int number;
	private Pad pad;
	private AttributePool attributePool;

	// not persisted
	private String action;
	private int offset;
	private Integer oldLength;
	private Session session;

	// set when received from other user
	private int spanId = 0;
	
	private int lengthDifference;

	public String getRule() {
		return rule;
	}

	public void setRule(String rule) {
		this.rule = rule;
		parseRule();
	}

	private void parseRule() {
		try {
			Matcher m = RULE_LEN_DIFF_PATTERN.matcher(rule);
			if (m.find()) {
				oldLength = Integer.parseInt(m.group(1), 36);
				setLengthDifference(Integer.parseInt(m.group(2), 36));
			}
			if (Pattern.matches("Z:[0-9a-zA-Z]+>[1-9a-zA-Z][0-9a-zA-Z]*.*", rule)) {
				action = Changeset.WRITE; // COULD BE ALSO OF TYPE 'ATTR_CHANGE'
			} else if (Pattern.matches("Z:[0-9a-zA-Z]+<.*", rule)) {
				action = Changeset.DELETE;
			} else if (Pattern.matches("Z:[0-9a-zA-Z]+>0.+", rule)) {
				action = Changeset.ATTR_CHANGE;
			} else if (Pattern.matches("Z:[0-9a-zA-Z]+>0$", rule)) {
				action = Changeset.NOCHANGE;
			} else {
				throw new MalformedMessage(rule);
			}
			computeOffset();
		} catch (MalformedMessage e) {
			ExceptionHandler.handle(e);
		} catch (RegExpException e) {
			ExceptionHandler.handle(e);
		} catch (NothingChangedException e) {
			ExceptionHandler.handle(e);
		}
	}

	public String getCharbank() {
		return this.charbank;
	}

	public void setCharbank(String charbank) {
		this.charbank = charbank;
	}

	public int getNumber() {
		return this.number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public Pad getPad() {
		return this.pad;
	}

	public void setPad(Pad pad) {
		this.pad = pad;
	}

	public AttributePool getAttributePool() {
		return this.attributePool;
	}

	public void setAttributePool(AttributePool attributePool) {
		this.attributePool = attributePool;
	}

	public User getAuthor() {
		return getCreator();
	}

	public void setAuthor(User author) {
		setCreator(author);
		if (pad != null && pad.getUserSessions() != null) {
			session = pad.getUserSessions().get(author.getId());
		}
	}

	public void save() {
		HibernateDAOFactory.getInstance().getChangesetDAO().insert(this);
	}

	public void update() {
		HibernateDAOFactory.getInstance().getChangesetDAO().update(this);
	}

	public String toString() {
		return toString(true);
	}
	
	public String toString(boolean compact) {
		StringBuilder sb = new StringBuilder();
		if (!compact) {
			sb.append("CHANGESET").append("\n------------");
			sb.append("\nID:" + getId());
			if (pad != null) {
				sb.append("\nPAD:" + pad.getId() + " - " + pad.getName());
			}
			if (number <= 0) {
				sb.append("\nNUMBER:" + number);
			}
			if (rule != null) {
				sb.append("\nRULE:" + rule);
			}
			if (charbank != null) {
				sb.append("\nCHARBANK:" + charbank+"|");
			}
			if (getCreator() != null) {
				sb.append("\nAUTHOR:" + getCreator().getId() + " - " + getCreator().getName());
			}
			sb.append("\nCREATED:" + getCreated());
		} else {
			sb.append("C:[" + getId());
			sb.append("|" + pad.getName()+"("+pad.getId()+")");
			sb.append("|" + number);
			sb.append("|" + rule);
			sb.append("|" + charbank);
			sb.append(":" + getCreator().getId() + "(" + getCreator().getName()+")");
			sb.append(":" + getCreated() + "]");
		}
		return sb.toString();
	}

	public void print() {
		log.info(toString(true));
	}

	public String toCompactString() {
		return toString(true);
	}
	
	public void setAction(String action) {
		this.action = action;
	}
	
	public String getAction() {
		return action;
	}

	public boolean hasNewLine() {
		return action == WRITE && charbank.length() == 0;		
	}
	
	/**
	 * Computes position of current text change from given rule
	 * @param rule
	 * @return
	 * @throws RegExpException
	 * @throws NothingChangedException
	 */
	private void computeOffset() throws RegExpException, NothingChangedException {
		Matcher m = RULE_PATTERN.matcher(rule);
		if (!m.find()) {
			throw new RegExpException(rule, RULE_REGEXP);
		} else if (m.group(4) == null) {
			throw new NothingChangedException("rule: " + rule);
		}
		Matcher m3 = INNER_RULE_PATTERN.matcher(m.group(4));
		offset = 0;
		while (m3.find()) {
			if (m3.group(5) != null) {
				offset += Integer.parseInt(m3.group(5), 36);
			}
		}
	}

	public int getOffset() {
		return offset;
	}

	public Session getSession() { // TODO: zmenit asoc Changeset-Pad, Changeset-U -> Changeset-Session
		if (session == null) {
			session = HibernateDAOFactory.getInstance().getSessionDAO().findSession(getPad().getId(), getUser().getId());
		}
		return session;
	}

	public boolean hasSameSessionAs(TextSlice ts) {
		return ts != null && session != null && session.equals(ts.getSession());
	}

	public Integer getOldLength() {
		return oldLength;
	}

	public int getNewLength() {
		if (action == WRITE) {
			return oldLength + lengthDifference;
		} else if (action == DELETE) {
			return oldLength - lengthDifference;
		} else {
			return oldLength;
		}
	}
	
	public boolean isAtTheEnd() {
//		log.info("isAtEnd ? "+(action == DELETE && offset == getNewLength() || offset == oldLength ? "YES" : "NO")
//				+"| "+rule
//				+"| action:" + action + " ol:" + oldLength + " [" + offset + "] nl:" + getNewLength());
		if (action == DELETE) {
			return offset == getNewLength(); // if it is here => 'ab|x'
		} else {
			return offset == oldLength; // if it is here => 'ab|c'
		}
	}

	public void setLengthDifference(int lengthDifference) {
		this.lengthDifference = lengthDifference;
	}
	
	public int getLengthDifference() {
		return lengthDifference;
	}

	public boolean hasSpanId() {
		return spanId > 0;
	}
	
	public int getSpanId() {
		return spanId;
	}
	
	public void setSpanId(int spanId) {
		this.spanId= spanId;
	}
}