package net.parasec.ob;

public final class Impact {

	public static StringBuilder toCsv(final int[] impacts) {
		final StringBuilder sb = new StringBuilder();
		if (impacts == null)
			sb.append(",,,,,,,,,,");
		else {
			for (int i = 0; i < 10; i++)
				sb.append(Util.asUSD(impacts[i])).append(",");
			sb.append(Util.asUSD(impacts[10]));
		}
		return sb;
	}

}
