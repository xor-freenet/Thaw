package thaw.fcp;

import thaw.core.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * would be better called "FreenetKeyHelper" ... but too late :p
 */
public class FreenetURIHelper {

	private FreenetURIHelper() {

	}

	/**
	 * Quick test to see if the string could be a key
	 * only check the head, not the content (this property is used in FetchPlugin,
	 * please keep it)
	 */
	public static boolean isAKey(String key) {
		if (key == null)
		    return false;

		if (key.startsWith("CHK@")
		    || key.startsWith("SSK@")
		    || key.startsWith("USK@")) {
			return (key.length() > 20);
		}

		return key.startsWith("KSK@");
	}

	/* Match any of the following key formats (case insensitive):
	 *    SSK, USK, and CHK have two 43 character Base64 components followed by a 7 character
	 *       Base64 component, where each component is separated by a comma.  The base64 character
	 *       set includes the set [A-Za-z0-9-~].  URI keys are not padded.
	 *    KSK just begins with "KSK@" followed by a name.
	 */
	private static final Pattern FREENET_KEY_REGEX
		= Pattern.compile("(?i)((CHK|SSK|USK)@[a-z0-9~-]{43},[a-z0-9~-]{43},[a-z0-9~-]{7}.*)|(KSK@.+)");

	public static String cleanURI(String uri) {
		if (uri == null) {
			return uri;
        }

		try {
			uri = java.net.URLDecoder.decode(uri, "UTF-8");
		} catch (final java.io.UnsupportedEncodingException e) {
			Logger.warning(new FreenetURIHelper(), "UnsupportedEncodingException (UTF-8): "+e.toString());
		}

        Matcher regexMatcher = FREENET_KEY_REGEX.matcher(uri);
        if (regexMatcher.find()) {
            uri = regexMatcher.group();
            if (isAKey(uri)) {
                return uri;
            } else {
                Logger.notice(new FreenetURIHelper(), "Not a valid key: "+uri);
                return null;
		    }
        } else {
			Logger.notice(new FreenetURIHelper(), "Not a valid key: "+uri);
			return null;
        }
	}

	public static String getFilenameFromKey(final String key) {
		String filename = null;
		final String cutcut[];

		if (key == null)
			return null;


		if (key.startsWith("KSK")) {
			filename = key.substring(4);
		} else {
			cutcut = key.split("/");

			if (key.startsWith("CHK")) {

				if (cutcut.length >= 2)
					filename = cutcut[1];

			} else if (key.startsWith("SSK")) {

				filename = cutcut[cutcut.length-1];

			} else if (key.startsWith("USK")) {

				if (cutcut.length >= 4 || cutcut.length == 2)
					filename = cutcut[cutcut.length-1];
				else if (cutcut.length == 3)
					filename = cutcut[cutcut.length-2];
			}

		}


		if (filename != null) {
			try {
				filename = java.net.URLDecoder.decode(filename, "UTF-8");
			} catch (final java.io.UnsupportedEncodingException e) {
				Logger.warning(filename, "UnsupportedEncodingException (UTF-8): "+e.toString());
			} catch (final java.lang.IllegalArgumentException e) {
				Logger.warning(filename, "IllegalArgumentException: " + e.toString());
			}
		}

		return filename;
	}

	public static String convertSSKtoUSK(String SSK) {
		if ((SSK == null) || SSK.startsWith("USK@"))
			return SSK;

		SSK = SSK.replaceFirst("SSK@", "USK@");

		final String[] split = SSK.split("/");

		SSK = "";

		for (int i = 0 ; i < split.length ; i++) {
			switch (i) {
			case(0):
				SSK = split[i];
				break;
			case(1):
				final String subsplit[] = split[i].split("-");

				SSK = SSK + "/";

				for (int j = 0 ; j < subsplit.length-1 ; j++) {
					if (j == 0)
						SSK = SSK + subsplit[j];
					else
						SSK = SSK + "-" + subsplit[j];
				}

				SSK = SSK + "/" + subsplit[subsplit.length-1];

				break;
			default:
				SSK = SSK + "/" + split[i];
			}
		}

		return SSK;
	}

	private static String abs(final String val) {
		try {
			final java.math.BigDecimal bd = new java.math.BigDecimal(val);
			return bd.abs().toString();
		} catch(final java.lang.NumberFormatException e) {
			Logger.warning(new FreenetURIHelper(), "NumberFormatException while parsing '"+val+"'");
			return "0";
		}
	}

	public static String convertUSKtoSSK(String USK) {
		if ((USK == null) || USK.startsWith("SSK@"))
			return USK;

		USK = USK.replaceFirst("USK@", "SSK@");

		final String[] split = USK.split("/");

		USK = "";

		for (int i = 0 ; i < split.length ; i++) {
			switch (i) {
			case(0):
				USK = split[i];
				break;
			case(2):
				USK += "-" + FreenetURIHelper.abs(split[i]);
				break;
			default:
				USK += "/" + split[i];
				break;
			}
		}

		return USK;
	}

	public static String getPublicInsertionSSK(String key) {
		key = FreenetURIHelper.convertUSKtoSSK(key);

		final String split[] = key.split("/");

		key = "";

		for (int i = 0 ; i < split.length-1 ; i++) {
			if (i == 0)
				key = key + split[i];
			else
				key = key + "/" + split[i];
		}

		return key;
	}


	private static String changeRev(final String revStr, final int rev, final int offset) {
		if (offset == 0)
			return Integer.toString(rev);

		return Integer.toString(Integer.parseInt(revStr) + offset);
	}

	/**
	 * @param offset if == 0, then rev is changed according to the given offset
	 */
	public static String changeSSKRevision(String key, final int rev, final int offset) {

		if (key == null)
			return null;

		final String[] split = key.split("/");

		key = "";

		for (int i = 0 ; i < split.length ; i++) {
			switch(i) {
			case(0):
				key = key + split[i];
				break;
			case(1):
				final String[] subsplit = split[i].split("-");

				for (int j = 0 ; j < subsplit.length-1 ; j++) {
					if (j == 0)
						key = key + "/" + subsplit[j];
					else
						key = key + "-" + subsplit[j];
				}

				key = key + "-" + FreenetURIHelper.changeRev(subsplit[subsplit.length-1], rev, offset);
				break;
			default:
				key = key + "/" + split[i];
			}
		}

		return key;
	}

	public static String changeUSKRevision(String key, int rev, int offset) {
		if (key == null)
			return null;

		final String[] split = key.split("/");

		key = "";

		for (int i = 0 ; i < split.length ; i++) {
			switch(i) {
			case(0):
				key = key + split[i];
				break;
			case(2):
				key = key + "/" + FreenetURIHelper.changeRev(split[2], rev, offset);
				break;
			default:
				key = key + "/" + split[i];
			}
		}

		return key;

	}

	public static int getUSKRevision(final String key) {
		String[] split;

		if (key == null)
			return -1;

		split = key.split("/");

		if (split.length < 3)
			return -1;

		try {
			return Integer.parseInt(split[2]);
		} catch(NumberFormatException e) {
			Logger.warning(new FreenetURIHelper(), "Unable to parse '"+key +"'");
			return -1;
		}
	}

	/**
	 * will lower the case !
	 * will return the begining of the key.
	 */
	public static String getComparablePart(String key) {
		if (key == null)
			return null;

		if (key.startsWith("KSK@")) {
			return key.toLowerCase();
		}

		if (key.length() <= 70)
			return key.toLowerCase();

		return key.substring(0, 70).toLowerCase();
	}

	/**
	 * this process is not costless.
	 * Ignore the revisions
	 * @return true if they match
	 */
	public static boolean compareKeys(String keyA, String keyB) {
		if (keyA == keyB)
			return true;

		if (keyA == null || keyB == null) {
			Logger.notice(new FreenetURIHelper(), "compareKeys : null argument ?!");
			return false;
		}

		keyA = cleanURI(keyA);
		keyB = cleanURI(keyB);

		if (keyA.startsWith("USK@"))
			keyA = convertUSKtoSSK(keyA);

		if (keyB.startsWith("USK@"))
			keyB = convertUSKtoSSK(keyB);

		if (!keyA.substring(0, 3).equals(keyB.substring(0, 3))) {
			Logger.notice(new FreenetURIHelper(), "Not the same kind of key : "+
				      keyA.substring(0, 3) + " vs " + keyB.substring(0, 3));
			return false;
		}

		if (keyA.startsWith("CHK@")) {
			return getComparablePart(keyA).equals(getComparablePart(keyB));
		}

		if (keyA.startsWith("SSK@")) {
			keyA = changeSSKRevision(keyA, 0, 0);
			keyB = changeSSKRevision(keyB, 0, 0);

			String[] splitA = keyA.split("/");
			String[] splitB = keyB.split("/");

			if (splitA.length != splitB.length) {
				/* we shorten the keys because one has less elements than the other */
				keyA = splitA[0]+splitA[1];
				keyB = splitB[0]+splitB[1];
			}

			keyA = keyA.replaceAll(".frdx", ".xml"); /* we consider .frdx equivalent to .xml */
			keyB = keyB.replaceAll(".frdx", ".xml"); /* we consider .frdx equivalent to .xml */
		}

		if ( keyA.equals(keyB) )
			return true;

		return false;
	}

	public static boolean isObsolete(String key) {

		if (key.startsWith("KSK"))
			return false;

		if (key.startsWith("SSK") || key.startsWith("USK")) {
			if (key.indexOf("AQABAAE") > 0)
				return true;

			return false;
		}

		if (key.startsWith("CHK")) {
			if (key.indexOf(",AAE") > 0)
				return true;

			return false;
		}

		return true;
	}
}

