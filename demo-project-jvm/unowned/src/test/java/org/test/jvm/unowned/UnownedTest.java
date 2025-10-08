package org.test.jvm.unowned;

import org.junit.jupiter.api.Test;

import static io.github.gmazzo.codeowners.CodeOwnersUtils.getCodeOwners;
import static kotlin.collections.SetsKt.setOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UnownedTest {

    @Test
    public void ownerOfSelf() {
        assertEquals(setOf("jvm-test-devs"), getCodeOwners(UnownedTest.class));
    }

    @Test
    public void ownerOfUtils() {
        assertNull(getCodeOwners(Unowned.class));
    }

}
