package com.cloudempiere.searchindex.test;

/**
 * Test data constants for Garden World demo client.
 *
 * Makes tests more readable by using named constants instead of magic numbers.
 * Based on org.idempiere.test.DictionaryIDs pattern.
 *
 * Usage:
 * <pre>
 * MBPartner bp = MBPartner.get(Env.getCtx(), TestConstants.BPartner.JOE_BLOCK);
 * </pre>
 *
 * @author CloudEmpiere Team
 */
public class TestConstants {

	/** Garden World Client */
	public static final int GARDEN_WORLD_CLIENT = 11;

	/** Garden World HQ Organization */
	public static final int GARDEN_WORLD_HQ_ORG = 11;

	/** Garden World Admin User */
	public static final int GARDEN_WORLD_ADMIN_USER = 101;

	/** Garden World Admin Role */
	public static final int GARDEN_WORLD_ADMIN_ROLE = 102;

	/**
	 * Common Business Partners
	 */
	public static class BPartner {
		/** Joe Block - Customer */
		public static final int JOE_BLOCK = 118;

		/** Patio - Vendor */
		public static final int PATIO = 117;
	}

	/**
	 * Common Products
	 */
	public static class Product {
		/** Azalea Bush */
		public static final int AZALEA_BUSH = 128;

		/** Oak Tree */
		public static final int OAK_TREE = 123;
	}

	/**
	 * Common Currencies
	 */
	public static class Currency {
		/** US Dollar */
		public static final int USD = 100;

		/** Euro */
		public static final int EUR = 102;
	}

	/**
	 * Common Payment Terms
	 */
	public static class PaymentTerm {
		/** Immediate Payment */
		public static final int IMMEDIATE = 105;

		/** Net 30 */
		public static final int NET_30 = 106;
	}

	/**
	 * Common Tax Categories
	 */
	public static class Tax {
		/** Exempt Tax */
		public static final int EXEMPT = 104;

		/** Standard Tax */
		public static final int STANDARD = 105;
	}

	// Add more constants as needed for search index plugin tests
}
