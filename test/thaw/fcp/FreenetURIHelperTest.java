package thaw.fcp;

import org.junit.Test;
import junit.framework.TestCase;

public class FreenetURIHelperTest extends TestCase {

	private final static String[][] TEST_CLEANABLE_KEYS = {
		/* { unclean key, expected result } */
		{
			"CHK%40mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar",
			"CHK@mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar"
		},

		{
			"http://127.0.0.1:8888/CHK%40mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar",
			"CHK@mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar"
		},

		{
			"http://192.168.100.1:8888/CHK%40mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar",
			"CHK@mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar"
		},

		{
			"http://192.168.100.1:1234/CHK%40mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar",
			"CHK@mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar"
		},

		{
			"Key: http://192.168.100.1:1234/CHK%40mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar",
			"CHK@mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar"
		}
	};

	@Test
	public void testCleanURI_CleanableKeys() throws Exception {
		for(String[] pair : TEST_CLEANABLE_KEYS) {
			assertEquals("Cleanable keys", pair[1], FreenetURIHelper.cleanURI(pair[0]));
		}
	}

	private final static String[][] TEST_FILENAMED_KEYS = {
		{ /* 0 */
			"CHK@mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar",
			"Thaw-0.7.10.jar"
		},
		{ /* 1 */
			"CHK@mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/",
			null
		},
		{ /* 2 * / /* the '/' at the end was removed */
			"CHK@mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8",
			null
		},
		{ /* 3 */
			"USK%4061m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index/44/Publicly%20writable%20index.frdx",
			"Publicly writable index.frdx"
		},
		{ /* 4 */
			"USK%4061m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index/44",
			"Publicly writable index"
		},
		{ /* 5 */
			"KSK@gpl.txt",
			"gpl.txt"
		},
		{ /* 6 */
			"SSK@FoNrbtiJCeRUIorP01Vx5~Pn0aVp4tMeesVKObwbKXE,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQECAAE/toto-5/toto.frdx",
			"toto.frdx"
		},
		{ /* 7 */
			"SSK@FoNrbtiJCeRUIorP01Vx5~Pn0aVp4tMeesVKObwbKXE,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQECAAE/toto-5/",
			"toto-5" /* yes, it's the wanted behavior */
		}
	};

	@Test
	public void testGetFilenameFromKey() throws Exception {
		for(String[] pair : TEST_FILENAMED_KEYS) {
			assertEquals("Filenamed keys", pair[1], FreenetURIHelper.getFilenameFromKey(pair[0]));
		}
	}


	private final static String[] TEST_GOOD_KEYS = {
		"CHK@mmHr8ldkPL-ByTdAKL~IMua0z9nJ~dLnzoRIbbOaf2w,ORl1uXUYnutIayK~0Js5r6dnOBTYerm17OsxFq7jwpo,AAIC--8/Thaw-0.7.10.jar",
		"USK@p-uFAWUomLm37MCQLu3r67-B8e6yF1kS4q2v0liM1Vk,h0MWqM~lF0Bec-AIv445PLn06ams9-RFbnwO6Cm2Snc,AQACAAE/Thaw/7/Thaw.frdx",
		"KSK@gpl",
		"SSK@FoNrbtiJCeRUIorP01Vx5~Pn0aVp4tMeesVKObwbKXE,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQECAAE/"
	};

	private final static String[] TEST_BASIC_BAD_KEYS = {
		"CHK@mmH/Toto.jar",
		"BLEH"
	};

	@Test
	public static void testIsAKey() throws Exception {
		for (String goodKey : TEST_GOOD_KEYS) {
			assertTrue(FreenetURIHelper.isAKey(goodKey));
		}

		for (String badKey : TEST_BASIC_BAD_KEYS) {
			assertFalse(FreenetURIHelper.isAKey(badKey));
		}
	}


	private final static String[][] TEST_SSK_TO_USK = {
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index-44/Publicly writable index.frdx",
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index/44/Publicly writable index.frdx" 
		}
	};

	@Test
	public void testConvertSSKtoUSK() throws Exception {
		for (String[] test : TEST_SSK_TO_USK) {
			String usk = FreenetURIHelper.convertSSKtoUSK(test[0]);

			if ( test[1] == null )
				assertNull(usk);
			else
				assertEquals(test[1], usk);
		}
	}


	private final static String[][] TEST_USK_TO_SSK = {
		{
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index/44/Publicly writable index.frdx",
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index-44/Publicly writable index.frdx"
		},
		{
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index/-44/Publicly writable index.frdx",
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index-44/Publicly writable index.frdx"
		}
	};

	@Test
	public void testConvertUSKtoSSK() throws Exception {
		for (String[] test : TEST_USK_TO_SSK) {
			String ssk = FreenetURIHelper.convertUSKtoSSK(test[0]);

			if (test[1] == null)
				assertNull(ssk);
			else
				assertEquals(ssk, test[1]);
		}
	}


	private static final String[][] TEST_CHANGE_SSK_REV = {
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-44/Publicly writable index.frdx",
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-40/Publicly writable index.frdx"
		},
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-44/Publicly writable index.frdx",
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-48/Publicly writable index.frdx"
		},
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-44/Publicly writable index.frdx",
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-12/Publicly writable index.frdx"
		},
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-44/Publicly writable index.frdx",
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-0/Publicly writable index.frdx"
		}
	};

	private static final String[][] TEST_CHANGE_USK_REV = {
		{
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index/44/Publicly writable index.frdx",
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index/40/Publicly writable index.frdx"
		},
		{
			/* yep, it's the expected behavior */
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index/-44/Publicly writable index.frdx",
			/* rev += 4 */
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index/-40/Publicly writable index.frdx"
		},
		{
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index/44/Publicly writable index.frdx",
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index/12/Publicly writable index.frdx"
		},
		{
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index/44/Publicly writable index.frdx",
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index/0/Publicly writable index.frdx"
		}
	};

	private static final int[][] TEST_CHANGE_REV = {
		/* { rev, offset } */
		{ 0, -4 },
		{ 0,  4 },
		{ 12, 0},
		{ 0, 0}
	};


	private static void testRevisionChange(boolean ssk, String[][] TEST_SET, int[][] REV_CHANGES) throws Exception {
		for (int i = 0 ; i < REV_CHANGES.length ; i++) {
			String result = (ssk ?
					 FreenetURIHelper.changeSSKRevision(TEST_SET[i][0], REV_CHANGES[i][0], REV_CHANGES[i][1]) :
					 FreenetURIHelper.changeUSKRevision(TEST_SET[i][0], REV_CHANGES[i][0], REV_CHANGES[i][1]));

			assertEquals(TEST_SET[i][1], result);
		}
	}

	@Test
	public void testChangeSSKRev() throws Exception {
		testRevisionChange(true, TEST_CHANGE_SSK_REV, TEST_CHANGE_REV);
	}

	@Test
	public void testChangeUSKRev() throws Exception {
		testRevisionChange(false, TEST_CHANGE_USK_REV, TEST_CHANGE_REV);
	}


	private final static String[] TEST_USK_REV = {
		"USK@p-uFAWUomLm37MCQLu3r67-B8e6yF1kS4q2v0liM1Vk,h0MWqM~lF0Bec-AIv445PLn06ams9-RFbnwO6Cm2Snc,AQACAAE/Thaw/7/Thaw.frdx",
		"USK%4061m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index/44/Publicly%20writable%20index.frdx",
		"USK%4061m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index/-44/Publicly%20writable%20index.frdx"
	};

	private final static int[] TEST_USK_REV_RESULTS = {
		7,
		44,
		-44
	};

	@Test
	public static void testGetUSKRevision() throws Exception {
		for (int i = 0; i < TEST_USK_REV.length ; i++) {
			int result = FreenetURIHelper.getUSKRevision(TEST_USK_REV[i]);
			assertEquals(new Integer(result), new Integer(TEST_USK_REV_RESULTS[i]));
		}
	}


	private final static String[][] TEST_COMPARE_KEYS = {
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-40/Publicly writable index.frdx",
			"SSK@p-uFAWUomLm37MCQLu3r67-B8e6yF1kS4q2v0liM1Vk,h0MWqM~lF0Bec-AIv445PLn06ams9-RFbnwO6Cm2Snc,AQACAAE/Thaw/7/Thaw.frdx"
		},
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-40/Publicly writable index.frdx",
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-40/Publicly writable index.frdx"
		},
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-40/Publicly writable index.frdx",
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index/-3/Publicly writable index.frdx"
		},
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-40/Publicly writable index.frdx",
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index/-3/Publicly%20writable%20index.frdx"
		},
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-40/Publicly writable index.frdx",
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index/-3"
		},
		{
			"SSK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly writable index-40/Publicly writable index.frdx",
			"USK@61m2WMJEA9pyQQQ-hjGN8lIM2xToNJHyacJ8ZPB9JCQ,5aEPJBhwIV~HpGIG8YTpKSB39WCGgd0BUNWZ012745Y,AQACAAE/Publicly%20writable%20index/-3/pouet.txt"
		}
	};

	private final static boolean[] TEST_COMPARE_KEYS_RESULTS = {
		false,
		true,
		true,
		true,
		true/* yes it's a wanted behavior because of the indexes */,
		false
	};

	@Test
	public static void testCompareKeys() throws Exception {
		for (int i = 0; i < TEST_COMPARE_KEYS.length ; i++) {
			boolean result = FreenetURIHelper.compareKeys(TEST_COMPARE_KEYS[i][0],
						     TEST_COMPARE_KEYS[i][1]);
			assertEquals(new Boolean(result), new Boolean(TEST_COMPARE_KEYS_RESULTS[i]));
		}
	}
}

