package org.test.jvm.utils;

import org.junit.jupiter.api.Test;
import org.test.jvm.utils.more.MoreUtils;

import static io.github.gmazzo.codeowners.CodeOwnersUtils.getCodeOwners;
import static kotlin.collections.SetsKt.setOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UtilsOwnersTest {

    @Test
    public void ownerOfSelf() {
        assertEquals(setOf("jvm-test-devs"), getCodeOwners(UtilsOwnersTest.class));
    }

    @Test
    public void ownerOfUtils() {
        assertEquals(setOf("jvm-utils-devs"), getCodeOwners(Utils.class));
    }

    @Test
    public void ownerOfMoreUtils() {
        assertEquals(setOf("jvm-utils-devs"), getCodeOwners(MoreUtils.class));
        assertEquals(setOf("jvm-utils-devs"), getCodeOwners(MoreUtils.Companion.class));
    }

}
