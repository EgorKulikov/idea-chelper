package net.egork.chelper.parser.timus;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
//public class TimusTaskParser extends TimusParser implements TaskParser {
//	public static final TimusTaskParser INSTANCE = new TimusTaskParser();
//
//	public Task parse(String id, Task predefined) {
//		String[] tokens = id.split(" ");
//		String url;
//		String taskName;
//		if (tokens.length == 2) {
//			url = "http://acm.timus.ru/problem.aspx?space=" + tokens[0] + "&num=" + tokens[1] + "&contest=true";
//			taskName = "Task" + (char)('A' - 1 + Integer.parseInt(tokens[1]));
//		} else if (tokens.length == 1) {
//			url = "http://acm.timus.ru/problem.aspx?space=1&num=" + tokens[0];
//			taskName = "Task" + tokens[0];
//		} else
//			return null;
//		String text;
//		try {
//			text = FileUtilities.getWebPageContent(url);
//		} catch (IOException e) {
//			return null;
//		}
//		StringParser parser = new StringParser(text);
//		try {
//			parser.advance(true, "Memory Limit: ");
//			String heapMemory = parser.advance(false, " ") + "M";
//			List<Test> tests = new ArrayList<Test>();
//			parser.advance(false, "<TABLE CLASS=\"sample\">");
//			while (true) {
//				try {
//					parser.advance(true, "<PRE CLASS=\"intable\">");
//					String input = parser.advance(false, "</PRE>");
//					parser.advance(true, "<PRE CLASS=\"intable\">");
//					String output = parser.advance(false, "</PRE>");
//					tests.add(new Test(StringEscapeUtils.unescapeHtml(input),
//						StringEscapeUtils.unescapeHtml(output), tests.size()));
//				} catch (ParseException e) {
//					break;
//				}
//			}
////			return new Task(taskName, predefined.location, predefined.testType, StreamConfiguration.STANDARD,
////				StreamConfiguration.STANDARD, heapMemory, "64M", true,
////				tests.toArray(new Test[tests.size()]));
//            return null;
//		} catch (ParseException e) {
//			return null;
//		}
//	}
//}
