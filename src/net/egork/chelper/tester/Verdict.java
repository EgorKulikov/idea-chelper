package net.egork.chelper.tester;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class Verdict {
	public static final Verdict SKIPPED = new Verdict(Verdict.VerdictType.SKIPPED, null);
	public static final Verdict UNDECIDED = new Verdict(Verdict.VerdictType.UNDECIDED, null);
	public static final Verdict OK = new Verdict(Verdict.VerdictType.OK, null);
	public static final Verdict WA = new Verdict(Verdict.VerdictType.WA, null);

	public final VerdictType type;
	public final String message;

	public Verdict(VerdictType type, String message) {
		this.type = type;
		this.message = message;
	}

	@Override
	public String toString() {
		return type.name() + (message == null ? "" : " (" + message + ")");
	}

	public static enum VerdictType {
		OK, WA, PE, RTE, UNDECIDED, SKIPPED
	}
}
