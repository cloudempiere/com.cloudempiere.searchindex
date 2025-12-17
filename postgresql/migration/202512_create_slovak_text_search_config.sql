-- Migration: Create Slovak/Czech text search configuration with unaccent support
-- Issue: ADR-005 - SearchType Migration & Slovak Language Support
-- Date: 2025-12-17
-- Description: Creates sk_unaccent text search configuration for proper handling of
--              Slovak and Czech diacritics (č, š, ž, á, ú, ý, etc.)
--
-- This configuration enables searches without diacritics to match text with diacritics
-- Example: searching "ruza" will match "ruža", "růža", "rúža"
--
-- IMPORTANT: Requires unaccent extension to be installed

DO $$
BEGIN
    RAISE NOTICE 'Starting migration: Create Slovak text search configuration';
    RAISE NOTICE 'Date: %', NOW();
    RAISE NOTICE '========================================';

    -- Check if unaccent extension exists
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'unaccent') THEN
        RAISE NOTICE 'Installing unaccent extension...';
        CREATE EXTENSION IF NOT EXISTS unaccent;
        RAISE NOTICE '  ✓ Unaccent extension installed';
    ELSE
        RAISE NOTICE '  ✓ Unaccent extension already installed';
    END IF;

    -- Create Slovak/Czech text search configuration
    IF NOT EXISTS (SELECT 1 FROM pg_ts_config WHERE cfgname = 'sk_unaccent') THEN
        RAISE NOTICE 'Creating sk_unaccent text search configuration...';

        -- Copy from 'simple' configuration (language-independent)
        CREATE TEXT SEARCH CONFIGURATION sk_unaccent (COPY = simple);

        -- Configure unaccent for word mappings
        -- This removes diacritics: č→c, š→s, ž→z, á→a, é→e, etc.
        ALTER TEXT SEARCH CONFIGURATION sk_unaccent
          ALTER MAPPING FOR word, asciiword, hword, hword_asciipart, asciihword
          WITH unaccent, simple;

        RAISE NOTICE '  ✓ Created sk_unaccent configuration';
        RAISE NOTICE '  ✓ Configured word mappings with unaccent';
    ELSE
        RAISE NOTICE '  ⚠ sk_unaccent configuration already exists, skipping creation';
    END IF;

    -- Test the configuration
    RAISE NOTICE 'Testing sk_unaccent configuration...';

    -- Test 1: Slovak text with diacritics
    DECLARE
        v_test_vector tsvector;
        v_test_query tsquery;
    BEGIN
        -- Create tsvector from Slovak text with diacritics
        v_test_vector := to_tsvector('sk_unaccent', 'červená ruža môže');

        -- Expected result: diacritics removed
        -- 'cervena':1 'moze':3 'ruza':2
        RAISE NOTICE '  Test 1 - Slovak text: %', v_test_vector;

        -- Create tsquery without diacritics
        v_test_query := to_tsquery('sk_unaccent', 'cervena & ruza');

        -- Test if query matches vector
        IF v_test_vector @@ v_test_query THEN
            RAISE NOTICE '  ✓ Test 1 PASSED: Query without diacritics matches text with diacritics';
        ELSE
            RAISE WARNING '  ✗ Test 1 FAILED: Query should match';
        END IF;
    END;

    -- Test 2: Czech text with diacritics
    DECLARE
        v_test_vector tsvector;
        v_test_query tsquery;
    BEGIN
        v_test_vector := to_tsvector('sk_unaccent', 'žluťoučký kůň');
        v_test_query := to_tsquery('sk_unaccent', 'zlutoucky & kun');

        IF v_test_vector @@ v_test_query THEN
            RAISE NOTICE '  ✓ Test 2 PASSED: Czech diacritics handled correctly';
        ELSE
            RAISE WARNING '  ✗ Test 2 FAILED: Czech query should match';
        END IF;
    END;

    RAISE NOTICE '========================================';
    RAISE NOTICE 'Migration completed successfully!';
    RAISE NOTICE '';
    RAISE NOTICE 'Slovak and Czech text search now properly handles diacritics.';
    RAISE NOTICE 'After re-indexing, searches without diacritics will match text with diacritics.';
    RAISE NOTICE '';
    RAISE NOTICE 'Next steps:';
    RAISE NOTICE '1. Mark all Slovak/Czech search indexes as invalid: UPDATE AD_SearchIndex SET IsValid=''N''';
    RAISE NOTICE '2. Re-run CreateSearchIndex process for affected indexes';
END $$;
