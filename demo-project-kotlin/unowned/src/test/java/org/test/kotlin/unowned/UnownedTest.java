package org.test.kotlin.unowned;

import org.junit.Test;

import static io.github.gmazzo.codeowners.CodeOwnersUtils.getCodeOwners;
import static org.junit.Assert.assertNull;

public class UnownedTest {

    @Test
    public void ownerOfSelf() {
        assertNull(getCodeOwners(UnownedTest.class));
    }

    @Test
    public void ownerOfUtils() {
        assertNull(getCodeOwners(Unowned.class));
    }

}
