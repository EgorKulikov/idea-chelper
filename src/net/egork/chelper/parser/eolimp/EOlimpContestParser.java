package net.egork.chelper.parser.eolimp;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
//public class EOlimpContestParser extends EOlimpParser implements ContestParser {
//	public static final EOlimpContestParser INSTANCE = new EOlimpContestParser();
//
//	public Collection<String> parse(String id) {
//		String mainPage;
//		try {
//			mainPage = FileUtilities.getWebPageContent("http://www.e-olimp.com/en/competitions/" + id);
//		} catch (IOException e) {
//			return Collections.emptyList();
//		}
//		List<String> tasks = new ArrayList<String>();
//		StringParser parser = new StringParser(mainPage);
//		char letter = 'A';
//		while (true) {
//			try {
//				parser.advance(true, "<a href='problems/");
//				tasks.add(parser.advance(false, "'>") + " " + letter++);
//			} catch (ParseException e) {
//				break;
//			}
//		}
//		return tasks;
//	}
//
//	public TaskParser getTaskParser() {
//		return EOlimpTaskParser.INSTANCE;
//	}
//}
