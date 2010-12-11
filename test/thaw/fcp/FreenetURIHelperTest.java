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
	public void testCleanURI_CleanableKeys() throws Exception {
		for(String[] pair : TEST_CLEANABLE_KEYS) {
			assertEquals("Cleanable keys", pair[1], FreenetURIHelper.cleanURI(pair[0]));
		}
	}

	@Test
	public void testGetFilenameFromKey() throws Exception {
		for(String[] pair : TEST_FILENAMED_KEYS) {
			assertEquals("Filenamed keys", pair[1], FreenetURIHelper.getFilenameFromKey(pair[0]));
		}
	}
}
